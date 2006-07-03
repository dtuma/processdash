// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.Timer;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskDependencyResolver;
import net.sourceforge.processdash.ev.ui.*;
import net.sourceforge.processdash.hier.*;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.ui.*;
import net.sourceforge.processdash.i18n.*;
import net.sourceforge.processdash.log.*;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.WorkingTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.ui.*;
import net.sourceforge.processdash.net.cache.*;
import net.sourceforge.processdash.net.cms.CmsDefaultConfig;
import net.sourceforge.processdash.net.http.*;
import net.sourceforge.processdash.process.*;
import net.sourceforge.processdash.process.ui.*;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.DashboardSecurity;
import net.sourceforge.processdash.templates.*;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;
import net.sourceforge.processdash.ui.*;
import net.sourceforge.processdash.ui.help.*;
import net.sourceforge.processdash.ui.lib.*;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.StringUtils;


public class ProcessDashboard extends JFrame implements WindowListener, DashboardContext {

    public static final String HTTP_PORT_SETTING = "http.port";

    ConfigureButton configure_button = null;
    PauseButton pause_button = null;
    ScriptButton script_button = null;
    DefectButton defect_button = null;
    TaskNavigationSelector taskNav = null;
    CompletionButton completion_button = null;
    JMenuBar hierarchy_menubar = null;

    DashHierarchy props;
    DashHierarchy templates = null;
    DashboardTimeLog timeLog = null;
    DataRepository data = null;
    WebServer webServer = null;
    ConcurrencyLock concurrencyLock = null;
    AutoUpdateManager aum = null;
    ConsoleWindow consoleWindow = new ConsoleWindow();
    ObjectCache objectCache;
    private ErrorReporter brokenData;
    Resources resources;

    boolean paused = true;
    static final String DEFAULT_PROP_FILE = "state";
    String starting_dir       = "";
    String property_directory;
    static String default_directory = null;
    String propertiesFile     = DEFAULT_PROP_FILE;
    static final String TEMPLATES_FILE = "state";

    private ActiveTaskModel activeTaskModel;

    private static final Logger logger = Logger
            .getLogger(ProcessDashboard.class.getName());

    private static String versionNumber;

    public static final int DEFAULT_WEB_PORT = 2468;

    public ProcessDashboard(String title) {
        super();
        setIconImage(DashboardIconFactory.getWindowIconImage());
        getContentPane().setLayout(new GridBagLayout());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        // load app defaults and user settings.
        try {
            InternalSettings.initialize("");
        } catch (Exception e) {
            logErr("Unable to read settings file", e);
            displayStartupIOError("Errors.Read_File_Error.Settings_File",
                    e.getMessage());
            System.exit(0);
        }
        propertiesFile = Settings.getFile("stateFile");
        File prop_file = new File(propertiesFile);
        try {
            prop_file = prop_file.getCanonicalFile();
        } catch (IOException ioe) {}
        propertiesFile = prop_file.getPath();
        property_directory = prop_file.getParent() + Settings.sep;
        DefectAnalyzer.setDataDirectory(property_directory);
        CmsDefaultConfig.setPersistenceDirectory(prop_file.getParentFile());
        try {
            default_directory = prop_file.getParentFile().getCanonicalPath();
        } catch (IOException ioe) {
            default_directory = prop_file.getParentFile().getAbsolutePath();
        }

        // start the http server.
        try {
            int httpPort = Settings.getInt(HTTP_PORT_SETTING, DEFAULT_WEB_PORT);
            webServer = new WebServer(httpPort);
            webServer.setDashboardContext(this);
            InternalSettings.addPropertyChangeListener
                (HTTP_PORT_SETTING, new HttpPortSettingListener());
            ScriptID.setNameResolver(new ScriptNameResolver(webServer));
        } catch (IOException ioe) {
            logErr("Couldn't start web server", ioe);
        }

        maybeEnableReadOnlyMode();

        // ensure that we have exclusive control of the data in the
        // property_directory
        //
        if (!Settings.isReadOnly())
            concurrencyLock = new ConcurrencyLock(property_directory,
                                                  webServer.getPort(),
                                                  webServer.getTimestamp());

        // run the backup process as soon as possible
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.STARTUP, null);


        // create the data repository.
        data = new DataRepository();
        if ("true".equalsIgnoreCase(Settings.getVal("dataFreezing.disabled")))
            data.disableFreezing();
        templates = TemplateLoader.loadTemplates(data);
        aum = new AutoUpdateManager(TemplateLoader.getPackages());
        resources = Resources.getDashBundle("ProcessDashboard");
        InternalSettings.loadLocaleSpecificDefaults(resources);
        FormatUtil.setDateFormats(Settings.getVal("dateFormat"),
                Settings.getVal("dateTimeFormat"));
        Translator.init();
        LookAndFeelSettings.loadLocalizedSettings();
        data.setDatafileSearchURLs(TemplateLoader.getTemplateURLs());
        versionNumber = TemplateLoader.getPackageVersion("pspdash"); // legacy
        logger.info("Process Dashboard version " + versionNumber);

        if (title == null)
            title = resources.getString("Window_Title");
        if (Settings.isReadOnly())
            title = resources.format("ReadOnly.Title_FMT", title);
        setTitle(title);

        // initialize the content roots for the http server.
        webServer.setRoots(TemplateLoader.getTemplateURLs());
        WebServer.setOutputCharset(getWebCharset());

        BetaVersionSetup.runSetup(property_directory);

        // determine if Lost Data Files are present in the pspdata directory
        // and take steps to repair them.
        LostDataFiles lostPSPFiles = new LostDataFiles();
        lostPSPFiles.findLostFiles(property_directory);
        if (lostPSPFiles.repair(this)==false) {

            // if the lost data files could not be repaired, exit the dashboard
            logger.severe
            ("Dashboard was terminated due to user request. " +
                    "The following bad data files were found in the "+
                    "psp data directory:\n" + lostPSPFiles.printOut());
            System.exit(0);
        }

        // open and load the the user's work breakdown structure
        props = new DashHierarchy(property_directory);
        Vector v = null;
        Exception saxException = null;
        if (prop_file.exists()) {
            try {
                // try to load the user's existing properties file.
                try {
                    v = props.loadXML(propertiesFile, templates);
                } catch (Exception se1) {
                    if (!safeInstanceof(se1, "org.xml.sax.SAXException"))
                        throw se1;
                    saxException = se1;
                    props.load(propertiesFile);
                    LegacySupport.fixupV13ScriptIDs(props);
                    props.saveXML(propertiesFile, null);
                    props.clear();
                    v = props.loadXML(propertiesFile, templates);
                }
            } catch (Exception e) {
                // this is a serious problem, indicating a corrupt
                // state file.  Display a warning to the user, then exit.
                if (saxException != null) e = saxException;
                propertiesFile = prop_file.getAbsolutePath();
                try {
                    propertiesFile = prop_file.getCanonicalPath();
                } catch (Exception e2) {}
                DashHierarchy.displayCorruptStateFileWarning
                    (resources, propertiesFile, e);
                System.exit(0);
            }
        } else {
            try {
                // apparently, the user doesn't already have a properties
                // file.  read the default properties file, which simply
                // contains nodes for "Project" and "Non Project".
                String state = new String
                    (FileUtils.slurpContents(getClass().getResourceAsStream
                                                 (DEFAULT_PROP_FILE), true),
                     "ISO-8859-1");
                // localize the strings "Project" and "Non Project"
                state = StringUtils.findAndReplace
                    (state, "<Project>",
                     "<"+resources.getString("Project")+">");
                state = StringUtils.findAndReplace
                    (state, "<Non Project>",
                     "<"+resources.getString("Non_Project")+">");

                v = props.load(new StringReader(state), true);
                displayFirstTimeUserHelp();
            } catch (Exception e) {
                logErr("Couldn't read default state file", e);
            }
        }
        data.setNodeComparator(props);
        activeTaskModel = new DefaultActiveTaskModel(props);

        // create the time log
        try {
            this.timeLog = new DashboardTimeLog(new File(property_directory), data, props);
            this.timeLog.getTimeLoggingModel().setActiveTaskModel(activeTaskModel);
            DashboardTimeLog.setDefault(this.timeLog);
        } catch (IOException e1) {
            displayStartupIOError("Errors.Read_File_Error.Time_Log",
                    property_directory + WorkingTimeLog.TIME_LOG_FILENAME);
            System.exit(0);
        }

        // possibly reload cached data definitions.
        File serializedDefinitions = new File(property_directory, "defns.ser");
        if (serializedDefinitions.exists() &&
            (serializedDefinitions.lastModified() >
             TemplateLoader.getTemplateTimestamp()))
            try {
                data.loadDefinitions(new FileInputStream
                    (serializedDefinitions));
            } catch (Exception e) {}

        // open all the datafiles that were specified in the properties file.
        brokenData = new ErrorReporter
            (resources.getString("Errors.Broken_Data_Title"),
             resources.getStrings("Errors.Broken_Data_Header"),
             resources.getStrings("Errors.Broken_Data_Footer"));
        data.startInconsistency();
        try {
            if (v != null) {
                String a[];
                for (int i = 0; i < v.size(); i++) {
                    a = (String[])v.elementAt (i);
                    openDatafile(a[0], a[1]);
                }
            }
        }catch (Exception e) { logErr("open datafiles failed!", e); };

        // open the global data file.
        try {
            data.openDatafile("", property_directory + "global.dat");
        } catch (FileNotFoundException exc) {
            // if the user doesn't have a global data file, create one
            // for them from the default template.
            HierarchyEditor.createDataFile (property_directory + "global.dat",
                                          "dataFile.txt");
            openDatafile("", "global.dat");

        } catch (Exception exc) {
            logErr("when generating default datafile, caught exception", exc);
        }

        configure_button = new ConfigureButton(this);
        addToMainWindow(configure_button, 0);
        PCSH.enableHelpKey(this, "QuickOverview");
        pause_button = new PauseButton(timeLog.getTimeLoggingModel());
        if (!Settings.isReadOnly())
            addToMainWindow(pause_button, 0);
        defect_button = new DefectButton(this);
        if (!Settings.isReadOnly())
            addToMainWindow(defect_button, 0);
        script_button = new ScriptButton(this);
        addToMainWindow(script_button, 0);
        hierarchy_menubar = new JMenuBar();
        addToMainWindow(hierarchy_menubar, 1.0);
        DependencyIndicator dependencyIndicator = new DependencyIndicator(this,
                activeTaskModel);
        addToMainWindow(dependencyIndicator, 0);
        completion_button = new CompletionButton(this, activeTaskModel);
        addToMainWindow(completion_button, 0);

        ImportManager.init(data);
        data.finishInconsistency();
        ExportManager.init(data, this);
        try {
            data.maybeSaveDefinitions(serializedDefinitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        startAutoExporter();
        LegacySupport.configureRemoteListeningCapability(data);
        timeLog.refreshMetrics();

        try {
            objectCache =
                new FileObjectCache(new File(property_directory), ".obj");
        } catch (IOException ioe) {
            // not possible?
            ioe.printStackTrace();
        }

        webServer.setData(data);
        webServer.setProps(props);
        webServer.setCache(objectCache);
        EVTaskDependencyResolver.init(this);

        taskNav = new TaskNavigationSelector
            (this, hierarchy_menubar, activeTaskModel);
        completion_button.setNavSelector(taskNav);
        dependencyIndicator.update();
        props.addHierarchyListener(new DashHierarchy.Listener() {
                public void hierarchyChanged(Event e) {
                    saveHierarchy();
                    refreshHierarchy();
                }});

        brokenData.done();
        TemplateLoader.showTemplateErrors();
        addComponentListener(new ResizeWatcher());
    }
    private Component addToMainWindow(Component component, double weight) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.gridx = getContentPane().getComponentCount();
        g.insets = new Insets(2, g.gridx == 0 ? 2 : 0, 2, 2);
        g.weightx = weight;
        if (weight > 0)
            g.fill = GridBagConstraints.HORIZONTAL;

        GridBagLayout layout = (GridBagLayout) getContentPane().getLayout();
        layout.setConstraints(component, g);
        return getContentPane().add(component);
    }

    private static final String FIRST_TIME_HELP_URL = "/help/first-use.htm";
    private void displayFirstTimeUserHelp() {
        new AboutDialog(null, resources.getString("Welcome_Dialog_Title"),
                        FIRST_TIME_HELP_URL);
    }
    private boolean safeInstanceof(Object c, String className) {
        Class clz = c.getClass();
        while (clz != null) {
            if (clz.getName().equals(className)) return true;
            clz = clz.getSuperclass();
        }
        return false;
    }

    private class HttpPortSettingListener implements PropertyChangeListener {

        public HttpPortSettingListener() {
            setBrowserDefaults();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (HTTP_PORT_SETTING.equalsIgnoreCase(evt.getPropertyName()))
                try {
                    int newPort = Integer.parseInt((String) evt.getNewValue());
                    webServer.addPort(newPort);
                    setBrowserDefaults();
                } catch (Exception e) {}
        }

        private void setBrowserDefaults() {
            Browser.setDefaults(
                    webServer.getHostName(false),
                    webServer.getPort());
        }
    }

    private String getWebCharset() {
        String charsetName = Settings.getVal("http.charset");

        if (charsetName == null)
            charsetName = HTTPUtils.DEFAULT_CHARSET;
        else if ("auto".equals(charsetName))
            charsetName = (Translator.isTranslating() ? "UTF-8"
                    : HTTPUtils.DEFAULT_CHARSET);

        return charsetName;
    }

    private void maybeEnableReadOnlyMode() {
        if (Settings.isReadOnly())
            return;

        if (Settings.getBool("readOnly", false)) {
            // if the user has a "readOnly=true" setting, turn on the
            // read only flag.
            InternalSettings.setReadOnly(true);
            return;
        }

        String setting = Settings.getVal("readOnly");
        if (setting == null) return;
        if (setting.toLowerCase().startsWith("recommend")) {
            // Must manually use ResourceBundles until after the
            // TemplateLoader is initialized.
            ResourceBundle res = ResourceBundle.getBundle(
                    "Templates.resources.ProcessDashboard");
            JRadioButton readOnlyOption = new JRadioButton(res
                    .getString("ReadOnly.Recommended.Read_Only_Option"));
            readOnlyOption.setSelected(true);
            JRadioButton readWriteOption = new JRadioButton(res
                    .getString("ReadOnly.Recommended.Read_Write_Option"));
            ButtonGroup group = new ButtonGroup();
            group.add(readOnlyOption);
            group.add(readWriteOption);
            String title = res.getString("ReadOnly.Recommended.Title");
            Object[] message = new Object[] {
                    res.getString("ReadOnly.Recommended.Message").split("\n"),
                    readOnlyOption, readWriteOption };
            JOptionPane.showMessageDialog(null, message, title,
                    JOptionPane.QUESTION_MESSAGE);
            if (readOnlyOption.isSelected())
                InternalSettings.setReadOnly(true);
        }
    }

    private void displayStartupIOError(String resourceKey, String filename) {
        if (resources == null)
            resources = Resources.getDashBundle("ProcessDashboard");

        try {
            File f = new File(filename);
            filename = f.getAbsolutePath();
            filename = f.getCanonicalPath();
        } catch (Exception e) {}

        JOptionPane.showMessageDialog
            (null,
             resources.formatStrings("Errors.Read_File_Error.Message_FMT",
                                     resources.getString(resourceKey),
                                     filename),
             resources.getString("Errors.Read_File_Error.Title"),
             JOptionPane.ERROR_MESSAGE);
    }

    private static final String BULLET = "\u2022 ";

    public void openDatafile (String prefix, String dataFile) {
        try {
            data.openDatafile (prefix, property_directory + dataFile);
        } catch (FileNotFoundException fnfe) {
            brokenData.logError(prefix);
        } catch (Exception exc) {
            logErr("when opening datafile, '" + dataFile + "' for path '"
                    + prefix + "', caught exception:", exc);
        }
    }

    private static void ensureJRE13() {
        String versionNum = System.getProperty("java.version");
        String req = "1.4";
        if (DashPackage.compareVersions(versionNum, req) < 0) {
            Resources res = Resources.getDashBundle("ProcessDashboard.Errors");
            String vendorURL = System.getProperty("java.vendor.url");
            JOptionPane.showMessageDialog
                (null,
                 res.formatStrings("JRE_Requirement_Message_FMT",
                                   versionNum, vendorURL, req),
                 res.format("JRE_Requirement_Title_FMT", req),
                 JOptionPane.ERROR_MESSAGE);
            Browser.launch(vendorURL);
            System.exit(0);
        }
    }

    public static String getOwnerName(DataRepository data) {
        return getOwnerName((DataContext) data);
    }
    public static String getOwnerName(DataContext data) {
        SimpleData val = data.getSimpleValue("/Owner");
        if (val == null) return null;
        String result = val.format();

        String defaultVal = Resources.getDashBundle("ProcessDashboard")
            .getString("Enter_your_name");
        if (result.equals(defaultVal))
            return null;
        else
            return result;
    }

    boolean addTemplateJar(String jarfileName) {
        if (!TemplateLoader.addTemplateJar(data, templates, jarfileName))
            return false;

        URL [] templateRoots = TemplateLoader.getTemplateURLs();
        data.setDatafileSearchURLs(templateRoots);
        webServer.setRoots(templateRoots);
        return true;
    }

    private void refreshHierarchy() {
        if (SwingUtilities.isEventDispatchThread())
            refreshHierarchyImpl();
        else try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { refreshHierarchyImpl(); }});
        } catch (Exception e) { }
    }
    private void refreshHierarchyImpl() {
        logger.finer("ProcessDashboard.refreshHierarchyImpl starting");
        taskNav.hierarchyChanged();
        logger.finer("ProcessDashboard.refreshHierarchyImpl finished");
    }

    public ActiveTaskModel getActiveTaskModel() {
        return activeTaskModel;
    }


    public void pauseTimer() {
        timeLog.getTimeLoggingModel().stopTiming();
    }

    public PropertyKey getCurrentPhase() { return activeTaskModel.getNode(); }

    public boolean isHierarchyEditorOpen() {
        return configure_button.isHierarchyEditorOpen();
    }


    public TimeLog getTimeLog() {
        return timeLog;
    }

    public String getDirectory() {
        return (property_directory != null) ? property_directory : "";
    }

    public static String getDefaultDirectory() { return default_directory; }

    public DashHierarchy getProperties() {
        return props;
    }

    public DashHierarchy getTemplateProperties() {
        return templates;
    }

    public DataRepository getDataRepository() {
        return data;
    }

    public Dimension getPreferredSize() {
        Dimension minSize = super.getMinimumSize();
        Dimension result = super.getPreferredSize();
        int maxWidth = Settings.getInt("window.maxWidth", -1);
        if (maxWidth != -1)
            result.width = Math.max(
                    Math.min(result.width, maxWidth),
                    minSize.width);
        return result;
    }

    private class ResizeWatcher extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            Dimension minSize = getMinimumSize();
            Dimension preferredSize = getPreferredSize();
            Dimension currentSize = getSize();

            if (currentSize.height != preferredSize.height
                    || currentSize.width < minSize.width) {
                currentSize.height = preferredSize.height;
                currentSize.width = Math.max(currentSize.width, minSize.width);
                setSize(currentSize);

            } else if (currentSize.width != preferredSize.width) {
                // this is (generally) an indication that the user has resized
                // the window manually.  Remember their decision and don't
                // grow past this width in the future.
                InternalSettings.set("window.maxWidth",
                        Integer.toString(currentSize.width));
            }

        }
    }


    public void windowOpened(WindowEvent w) {}
    public void windowClosed(WindowEvent w) {}
    public void windowIconified(WindowEvent w) {}
    public void windowDeiconified(WindowEvent w) {}
    public void windowActivated(WindowEvent w) {}
    public void windowDeactivated(WindowEvent w) {}

    public void windowClosing(WindowEvent w) {
        exitProgram();
    }

    public void exitProgram() {
        new DashboardPermission("exitProgram").checkPermission();

        String owner = getOwnerName(data);
        try {
            if (quit() == false)
                return;
        } catch (Throwable t) {
            // if the shutdown sequence encounters an uncaught exception,
            // display an error message, but still exit.
            logErr("When shutting down, encountered the exception:", t);
        }
        logger.fine("Backing up data directory");
        FileBackupManager.maybeRun
            (property_directory, FileBackupManager.SHUTDOWN, owner);

        logger.fine("Shutdown complete");
        System.exit(0);
    }

    protected boolean quit() {
        List unsavedData = saveAllData();
        if (unsavedData.isEmpty() == false
                && warnUserAboutUnsavedData(unsavedData) == false) {
            InternalSettings.setDisableChanges(false);
            return false;
        }

        logger.fine("Performing auto exports");
        ExportManager.getInstance().exportAll(this, this);
        if (webServer != null) {
            logger.fine("Shutting down web server");
            webServer.quit();
            webServer = null;
        }
        if (data != null) {
            logger.fine("Finalizing data repository");
            data.finalize();
            data = null;
        }

        if (concurrencyLock != null) {
            logger.fine("Removing concurrency lock");
            concurrencyLock.unlock();
            concurrencyLock = null;
        }

        return true;
    }

    private boolean warnUserAboutUnsavedData(List unsavedData) {
        if (unsavedData.isEmpty())
            return true;

        StringBuffer dataItems = new StringBuffer();
        for (Iterator i = unsavedData.iterator(); i.hasNext();) {
            String dataDescr = (String) i.next();
            dataItems.append(BULLET).append(dataDescr).append("\n");
        }

        String title = resources.getString("Errors.Save_Error.Title");
        String[] message = resources.formatStrings(
                "Errors.Save_Error.Message_FMT", dataItems.toString());
        int userChoice = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION);
        return userChoice == JOptionPane.YES_OPTION;
    }

    public List saveAllData() {
        List unsavedData = new LinkedList();
        if (Settings.isReadOnly())
            return unsavedData;

        TaskScheduleChooser.closeAll();

        if (pause_button != null)
            pause_button.saveData();
        if (configure_button != null)
            configure_button.saveData();

        if (saveHierarchy() == false)
            recordUnsavedItem(unsavedData, "Hierarchy");

        if (saveTimeLogData() == false)
            recordUnsavedItem(unsavedData, "Time_Log");

        if (saveMetricsData() == false)
            recordUnsavedItem(unsavedData, "Metrics_Data");

        if (saveSettingsData() == false)
            recordUnsavedItem(unsavedData, "Settings_Data");

        return unsavedData;
    }

    private boolean saveMetricsData() {
        if (data == null)
            return true;

        data.saveAllDatafiles();

        return ! data.areDatafilesDirty();
    }

    private void recordUnsavedItem(List unsavedData, String dataType) {
        unsavedData.add(resources.getString("Errors.Save_Error." + dataType));
    }

    protected boolean saveTimeLogData() {
        timeLog.getTimeLoggingModel().setPaused(true);
        timeLog.getTimeLoggingModel().saveData();
        timeLog.saveData();
        return ! timeLog.isDirty();
    }

    public boolean saveHierarchy() {
        try {
            props.save(propertiesFile, "hierarchical work breakdown structure");
            return true;
        } catch (Exception e) {
            logErr("prop write failed.", e);
            return false;
        }
    }

    public boolean saveSettingsData() {
        InternalSettings.setDisableChanges(true);
        if (InternalSettings.isDirty())
            InternalSettings.saveSettings();
        return InternalSettings.isDirty() == false;
    }


    private void startAutoExporter() {
        int millisPerHour = 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*milliseconds*/;

        ActionListener periodicTaskInitiator = (ActionListener) EventHandler
                .create(ActionListener.class, this, "startPeriodicTasks");
        Timer t = new Timer(millisPerHour, periodicTaskInitiator);
        t.start();
    }


    public void startPeriodicTasks() {
        int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        final boolean doExport = hourMatchesSetting(currentHourOfDay,
                ExportManager.EXPORT_TIMES_SETTING);
        final boolean doBackup = hourMatchesSetting(currentHourOfDay,
                FileBackupManager.BACKUP_TIMES_SETTING);
        if (doExport == false && doBackup == false)
            // nothing to do
            return;

        Thread t = new Thread("Process Dashboard Periodic Task Executor") {
            public void run() {
                performPeriodicTasks(doExport, doBackup);
            }
        };
        t.start();
    }

    private boolean hourMatchesSetting(int hour, String settingName) {
        String settingVal = Settings.getVal(settingName);
        if (settingVal == null || settingVal.trim().length() == 0)
            return false;
        if ("*".equals(settingVal.trim()))
            return true;

        String[] times = settingVal.split("\\D+");  // split on non-digits
        for (int i = 0; i < times.length; i++)
            try {
                if (hour == Integer.parseInt(times[i]))
                    return true;
            } catch (NumberFormatException nfe) {}

        return false;
    }

    private void performPeriodicTasks(boolean doExport, boolean doBackup) {
        if (doExport)
            ExportManager.getInstance().exportAll(this, this);
        if (doBackup)
            FileBackupManager.maybeRun(property_directory,
                    FileBackupManager.RUNNING, getOwnerName(data));
    }

    public static String getVersionNumber() { return versionNumber; }

    static SplashScreen ss = null;

    public static void dropSplashScreen() {
        if (ss != null) ss.okayToDispose();
        ss = null;
    }

    private static void logErr(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void main(String args[]) {
        DashboardSecurity.setupSecurityManager();

        ss = new SplashScreen(ProcessDashboard.class.getResource("ui/splash.png"));
        ss.displayFor(3000);      // show for at least 3 seconds.

        ensureJRE13();

        if (Boolean.getBoolean("readOnly"))
            InternalSettings.setReadOnly(true);

        int pos = 0;
        if (args.length > 0 && "readOnly".equalsIgnoreCase(args[0])) {
            InternalSettings.setReadOnly(true);
            pos++;
        }

        ProcessDashboard dash = new ProcessDashboard
            (args.length > pos ? args[pos] : null);
        DashController.setDashboard(dash);

        dash.pack();
        dash.show();

        dropSplashScreen();
        dash.aum.maybePerformCheck(dash);
    }

    public DashHierarchy getHierarchy() { return props; }
    public DataRepository getData() { return data; }
    public ObjectCache getCache() { return objectCache; }
    public WebServer getWebServer() { return webServer; }

}
