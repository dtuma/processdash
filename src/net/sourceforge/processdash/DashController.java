// Copyright (C) 2001-2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.TaskScheduleChooser;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.team.setup.TeamProjectSetupWizard;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.templates.ui.ImportTemplatePermissionDialog;
import net.sourceforge.processdash.tool.export.mgr.CompletionStatus;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.tool.export.mgr.RepairImportDirInstruction;
import net.sourceforge.processdash.ui.WindowTracker;
import net.sourceforge.processdash.util.UUIDFile;


public class DashController {

    static ProcessDashboard dash = null;
    private static File dataDirectory_ = null;
    private static Set localAddresses = Collections.singleton("127.0.0.1");
    private static DashboardPermission PERMISSION =
        new DashboardPermission("dashController");
    private static final Logger logger = Logger.getLogger(DashController.class
            .getName());

    public static void setDashboard(ProcessDashboard dashboard) {
        PERMISSION.checkPermission();
        dash = dashboard;
        Set addrs = new HashSet();
        try {
            addrs.add("127.0.0.1");
            addrs.add("::1");
            addrs.add("0:0:0:0:0:0:0:1");
            addrs.add("0:0:0:0:0:0:0:1%0");
            addLocalAddresses(addrs, "localhost");
            addLocalAddresses(addrs, InetAddress.getLocalHost().getHostName());
        } catch (IOException ioe) {}
        localAddresses = Collections.unmodifiableSet(addrs);
    }
    private static void addLocalAddresses(Set dest, String host) {
        try {
            dest.add(host);
            for (InetAddress addr : InetAddress.getAllByName(host))
                dest.add(addr.getHostAddress());
        } catch (Exception e) {
        }
    }

    public static void setDataDirectory(File directory) {
        PERMISSION.checkPermission();
        dataDirectory_ = directory;
    }

    private static File getDataDirectory() {
        if (dataDirectory_ != null)
            return dataDirectory_;
        else
            return new File(dash.getDirectory());
    }

    public static void checkIP(Object remoteAddress) throws IOException {
        PERMISSION.checkPermission();

        if (remoteAddress instanceof InetAddress)
            remoteAddress = ((InetAddress) remoteAddress).getHostAddress();

        if (localAddresses.contains(remoteAddress))
            return;

        throw new IOException("Connection not accepted from: " + remoteAddress);
    }

    public static void raiseWindow() {
        if (dash != null)
            new WindowRaiser();
    }

    /** In Gnome/Linux, a single call to raiseWindowImpl doesn't seem to do the
     * trick.  It has to be called a couple of times, with a delay in between.
     * This class performs that task.
     */
    private static class WindowRaiser implements ActionListener {

        Timer t;
        private int retryCount = 10;

        public WindowRaiser() {
            t = new Timer(100, this);
            t.setInitialDelay(0);
            t.start();
        }

        public void actionPerformed(ActionEvent e) {
            boolean isIconified = (dash.getExtendedState() & Frame.ICONIFIED) > 0;
            if (retryCount-- < 0 || (dash.isActive() && !isIconified))
                t.stop();
            else
                raiseWindowImpl();
        }

    }

    private static void raiseWindowImpl() {
        if ((dash.getExtendedState() & Frame.ICONIFIED) > 0)
            dash.setState(Frame.NORMAL);
        dash.setVisible(true);
        dash.windowSizeRequirementsChanged();
        dash.toFront();
    }

    public static Window showTimeLogEditor(final String path) {
        return WindowTracker.openWindow(new Runnable() {
            public void run() { showTimeLogEditorImpl(path); }});
    }
    private static void showTimeLogEditorImpl(String path) {
        PropertyKey key = dash.props.findExistingKey(path);
        dash.configure_button.startTimeLog(key);
    }

    public static Window showDefectLogEditor(final String path) {
        return WindowTracker.openWindow(new Runnable() {
            public void run() { showDefectLogEditorImpl(path); }});
    }
    private static void showDefectLogEditorImpl(String path) {
        PropertyKey key = dash.props.findExistingKey(path);
        dash.configure_button.startDefectLog(key);
    }

    public static Window showTaskSchedule(final String path) {
        return WindowTracker.openWindow(new Runnable() {
                public void run() { showTaskScheduleImpl(path); } } );
    }
    private static void showTaskScheduleImpl(String path) {

        // if no path was given, just display a chooser dialog to the user.
        if (path == null || path.length() == 0) {
            new TaskScheduleChooser(dash);
            return;
        }

        List taskLists = EVTaskList.getPreferredTaskListsForPath(dash.data,
                path);

        if (taskLists.size() == 1)
            TaskScheduleChooser.open(dash, (String) taskLists.get(0));
        else
            new TaskScheduleChooser
                (dash, (String[]) taskLists.toArray(new String[0]));
    }

    public static void exportData(String prefix) {
        exportDataForPrefix(prefix);
    }
    public static CompletionStatus exportDataForPrefix(String prefix) {
        CompletionStatus result = ExportManager.getInstance()
                .exportDataForPrefix(prefix);

        if (result != null && result.getException() != null)
            logger.log(Level.WARNING, "Error exporting data for '" + prefix
                    + "'", result.getException());

        return result;
    }

    public static void exportAllData() {
        ExportManager.getInstance().exportAll(dash, dash);
    }

    public static File backupData() {
        if (saveAllDataWithFeedback() == false)
            return null;

        return dash.fileBackupManager.run();
    }

    /** @since 2.2.6 */
    public static void saveDirtyGuiData() {
        PERMISSION.checkPermission();
        if (SwingUtilities.isEventDispatchThread()) {
            saveDirtyGuiDataImpl();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        saveDirtyGuiDataImpl();
                    }
                });
            } catch (Exception e) {}
        }
    }

    private static void saveDirtyGuiDataImpl() {
        dash.fireApplicationEvent(ApplicationEventListener.APP_EVENT_SAVE_ALL_DATA);
    }


    public static void saveAllData() {
        PERMISSION.checkPermission();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    dash.saveAllData();
                }
            });
        } catch (Exception e) {
        }
    }

    public static boolean saveAllDataWithFeedback() {
        PERMISSION.checkPermission();
        final boolean[] result = { false };
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if (dash.saveAllData().isEmpty())
                        result[0] = true;
                }
            });
        } catch (Exception e) {
        }
        return result[0];
    }

    /**
     * Write dirty application data to disk
     * @since 1.15.7
     */
    public static void flushDirtyData() {
        if (dash != null) {
            dash.saveMetricsData();
            dash.saveSettingsData();
            dash.flushWorkingData();
        }
    }

    /**
     * @see BackgroundTaskManager#suspend(long, long)
     * @since 1.14.1
     */
    public static void suspendBackroundTasks(long suspendMillis,
            long waitForCurrentTaskMillis) {
        BackgroundTaskManager.getInstance().suspend(suspendMillis,
            waitForCurrentTaskMillis);
    }

    public static void startTiming() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { startTimingImpl(); } } );
    }
    private static void startTimingImpl() {
        DashboardTimeLog tl = (DashboardTimeLog) dash.getTimeLog();
        tl.getTimeLoggingModel().startTiming();
    }

    public static void stopTiming()  {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { stopTimingImpl(); } } );
    }
    private static void stopTimingImpl()  {
        DashboardTimeLog tl = (DashboardTimeLog) dash.getTimeLog();
        tl.getTimeLoggingModel().stopTiming();
    }


    private static boolean setPathSuccessful;
    public static boolean setPath(final String path) {
        if (SwingUtilities.isEventDispatchThread())
            return setPathImpl(path);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { setPathSuccessful = setPathImpl(path); }});
        } catch (Exception e) {
            setPathSuccessful = false;
        }
        return setPathSuccessful;
    }
    private static boolean setPathImpl(String path) {
        return dash.pause_button.setPath(path);
    }

    private static boolean setPhaseSuccessful;
    public static boolean setPhase(final String phase) {
        if (SwingUtilities.isEventDispatchThread())
            return setPhaseImpl(phase);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { setPhaseSuccessful = setPhaseImpl(phase); }});
        } catch (Exception e) {
            setPhaseSuccessful = false;
        }
        return setPhaseSuccessful;
    }
    private static boolean setPhaseImpl(String phase) {
        return dash.pause_button.setPhase(phase);
    }

    public static void printNullDocument(PrintWriter out) {
        out.println(TriggerURI.NULL_DOCUMENT_MARKER);
        out.println("<HTML><HEAD><SCRIPT>");
        out.println("history.back();");
        out.println("</SCRIPT></HEAD><BODY></BODY></HTML>");
    }

    public static Map getTemplates() {
        Prop templates = dash.templates.pget(PropertyKey.ROOT);
        TreeMap result = new TreeMap();
        for (int i = templates.getNumChildren();   i-- > 0; ) {
            PropertyKey childKey = templates.getChild(i);
            Prop child = dash.templates.pget(childKey);
            result.put(child.getID(), Prop.unqualifiedName(childKey.name()));
        }
        return result;
    }

    /** @since 1.15.8 */
    public static void showHierarchyEditor() {
        dash.configure_button.startPropertyFrame();
    }

    public static boolean isHierarchyEditorOpen() {
        return dash.configure_button.isHierarchyEditorOpen();
    }

    public static void addTemplateDirToPath(String templateDir,
                                            boolean create) {
        if (ImportTemplatePermissionDialog.askUserForPermission
            (dash, null, templateDir, create) == true)
            addTemplateDirToTemplatePath(templateDir);
    }

    private static void addTemplateDirToTemplatePath(String templateDir) {
        if (templateDir == null) return;
        templateDir = templateDir.replace('\\', '/');

        String templatePath = Settings.getVal(TEMPLATE_PATH);
        if (templatePath == null || templatePath.trim().length() == 0)
            InternalSettings.set(TEMPLATE_PATH, templateDir);

        else if (!templateSettingContainsDir(templatePath, templateDir)) {
            templatePath = templateDir + ";" + templatePath;
            InternalSettings.set(TEMPLATE_PATH, templatePath);
        }
    }
    private static final String TEMPLATE_PATH = "templates.directory";
    private static boolean templateSettingContainsDir(String setting,
                                                      String dir)
    {
        setting = ";" + setting.replace('\\', '/') + ";";
        dir     = ";" + dir.replace('\\', '/')     + ";";
        return setting.indexOf(dir) != -1;
    }

    public static boolean alterTemplateID(String prefix,
                                          String oldID,
                                          String newID) {
        if (Settings.isReadOnly())
            return false;

        PropertyKey key = dash.props.findExistingKey(prefix);
        String actualID = dash.props.getID(key);
        if (oldID == actualID || // handles "null == null" case
            (oldID != null && oldID.equals(actualID))) try {
            HierarchyAlterer a = new HierarchyAlterer(dash);
            a.addTemplate(prefix, newID);
            return true;
        } catch (Exception e) {}

        return false;
    }

    public static boolean loadNewTemplate(String jarfileName,
                                          String templateDir,
                                          boolean create)
    {
        if (ImportTemplatePermissionDialog.askUserForPermission
            (dash, jarfileName, templateDir, create) == false)
            return false;

        if (dash.addTemplateJar(jarfileName) == false)
            return false;

        addTemplateDirToTemplatePath(templateDir);
        return true;
    }

    public static HierarchyAlterer getHierarchyAlterer() {
        return new HierarchyAlterer(dash);
    }
    public static String getSettingsFileName() {
        return InternalSettings.getSettingsFileName();
    }
    public static void addImportSetting(String prefix, String location) {
        ImportDirectoryInstruction instr = new ImportDirectoryInstruction(
                location, prefix);
        ImportManager.getInstance().addInstruction(instr);
    }
    public static void repairImportSetting(String projectID, String prefix,
            String[] locations) {
        new RepairImportDirInstruction(projectID, prefix, locations).run();
    }

    public static void enableTeamSettings() {
        if (Settings.isReadOnly())
            return;

        // enable earned value rollups.
        InternalSettings.set(EV_ROLLUP, "true");

        // export more often.
        InternalSettings.set(EXPORT_TIMES, "*");

        // listen on any address if we aren't already, and if the feature
        // hasn't been blocked
        String httpRemote = Settings.getVal(WebServer.HTTP_ALLOWREMOTE_SETTING);
        if (!"blocked".equalsIgnoreCase(httpRemote))
            InternalSettings.set(WebServer.HTTP_ALLOWREMOTE_SETTING, "true");

        // listen on a repeatable port.
        String port = Settings.getVal(HTTP_PORT);
        if (port == null) {
            int portNum = getAvailablePort();
            InternalSettings.set(HTTP_PORT, Integer.toString(portNum));
        }

        // save settings and flush them to the server.
        flushDirtyData();
    }

    private static int getAvailablePort() {
        for (int i = 0;   i < PORT_PATTERNS.length;   i++)
            for (int j = 3;   j < 10;   j++)
                if (isPortAvailable(PORT_PATTERNS[i]*j))
                    return PORT_PATTERNS[i]*j;
        return 3000;
    }

    private static boolean isPortAvailable(int port) {
        if (port < 1024) return false;
        boolean successful = false;
        try {
            ServerSocket a = new ServerSocket(port-1);
            ServerSocket b = new ServerSocket(port);
            successful = true;
            a.close();
            b.close();
        } catch (IOException ioe) {}
        return successful;
    }

    private static final String EV_ROLLUP = "ev.enableRollup";
    private static final String EXPORT_TIMES =
        ExportManager.EXPORT_TIMES_SETTING;
    private static final String HTTP_PORT = ProcessDashboard.HTTP_PORT_SETTING;
    private static final int[] PORT_PATTERNS = {
        1000, 1111, 1001, 1010, 1100, 1011, 1101, 1110 };

    public static void scrubDataDirectory() {
        PERMISSION.checkPermission();
        Set<String> filesInUse = new HashSet<String>();
        getDataAndDefectFilesInUse(filesInUse, PropertyKey.ROOT);
        File[] files = getDataDirectory().listFiles();
        if (files == null)
            return;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (isNumberedDataOrDefectFile(name))
                if (!filesInUse.contains(name))
                    f.delete();
        }
    }

    private static void getDataAndDefectFilesInUse(Set<String> filesInUse,
            PropertyKey node) {
        Prop p = dash.getHierarchy().pget(node);
        filesInUse.add(lower(p.getDataFile()));
        filesInUse.add(lower(p.getDefectLog()));
        for (int i = dash.getHierarchy().getNumChildren(node);  i-- > 0; ) {
            PropertyKey child = dash.getHierarchy().getChildKey(node, i);
            getDataAndDefectFilesInUse(filesInUse, child);
        }
    }

    private static String lower(String s) {
        return (s == null ? null : s.toLowerCase());
    }

    private static boolean isNumberedDataOrDefectFile(String name) {
        return ("0123456789".indexOf(name.charAt(0)) != -1)
            && (name.endsWith(".dat") || name.endsWith(".def"));
    }

    public static String getDatasetID() {
        return getDatasetID(true);
    }

    public static String getDatasetID(boolean assignIfMissing) {
        String result;
        synchronized (DashController.class) {
            result = datasetID;
        }

        if (result == null) {
            File dataDir = getDataDirectory();
            File idFile = new File(dataDir, "datasetID.dat");
            try {
                result = UUIDFile.getIdentifier(idFile, assignIfMissing);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                    "Unable to retrieve dataset identifier", e);
            }
            synchronized (DashController.class) {
                datasetID = result;
            }
        }

        return result;
    }
    private static String datasetID = null;

    /** @since 2.2.3.2 */
    public static boolean assignHierarchyNodeIDs() throws IllegalStateException {
        // PERMISSION.checkPermission(); // no longer checked in PD version 2.4
        if (dash.isHierarchyEditorOpen())
            throw new IllegalStateException(
                    "The hierarchy editor is currently open.");
        boolean madeChange = false;
        if (TeamProjectSetupWizard.copyNodeIDsToHierarchy(dash.getData(),
            dash.getHierarchy()))
            madeChange = true;
        if (dash.getHierarchy().assignMissingNodeIDs())
            madeChange = true;
        if (madeChange) {
            dash.saveHierarchy();
            DataVersionChecker.registerDataRequirement("pspdash", "2.2.3.2");
        }
        return madeChange;
    }

    /** @since 1.15.2 */
    public static void addApplicationEventListener(ApplicationEventListener l) {
        dash.addApplicationEventListener(l);
    }

    /** @since 1.15.2 */
    public static void removeApplicationEventListener(ApplicationEventListener l) {
        dash.removeApplicationEventListener(l);
    }

}
