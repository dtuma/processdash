// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


package net.sourceforge.processdash.tool.export;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.processdash.FileBackupManager;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataImporter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListXML;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.SelectableHierarchyTree;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.TimeLog;
import net.sourceforge.processdash.log.TimeLogEntry;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.ProgressDialog;
import net.sourceforge.processdash.util.RobustFileWriter;


public class ImportExport extends JDialog implements ActionListener {

    static final Resources resource = Resources.getDashBundle("ImportExport");

    static final int X_DATA = 0;
    static final int X_LIST = 1;

    static String FILE_SEP = null;

    ProcessDashboard  parent;
    DashHierarchy props;
    SelectableHierarchyTree tree;
    int           operation = X_DATA;



    public ImportExport (ProcessDashboard dash) {
        super (dash, resource.getString("Export"));
        PCSH.enableHelpKey(this, "ExportingData");

        parent = dash;
        props = parent.getHierarchy();

        /* Create the tree. */
        tree = new SelectableHierarchyTree(props);

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.getViewport().add(tree);

        getContentPane().add(sp, "Center");

        ButtonGroup bg = new ButtonGroup();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JRadioButton button;
        button = new JRadioButton (resource.getString("ExportData"));
        button.setActionCommand("XData");
        button.addActionListener(this);
        button.setSelected(true);
        button.setAlignmentX(0.5f);
        bg.add (button);
        buttonBox.add (button);

        button = new JRadioButton (resource.getString("ExportHierarchy"));
        button.setActionCommand("XList");
        button.addActionListener(this);
        button.setAlignmentX(0.5f);
        bg.add (button);
        buttonBox.add (button);

        buttonBox.add (Box.createVerticalStrut(4));
        buttonBox.add (Box.createVerticalGlue());

        Box btnBox = new Box(BoxLayout.X_AXIS);
        btnBox.add(Box.createHorizontalStrut(4));
        JButton btn = new JButton (resource.getString("Export"));
        btn.setActionCommand("Apply");
        btn.addActionListener(this);
        btnBox.add(btn);
        btn = new JButton (resource.getString("Close"));
        btn.setActionCommand("Close");
        btn.addActionListener(this);
        btnBox.add(btn);
        btnBox.add(Box.createHorizontalStrut(4));
        buttonBox.add (btnBox);
        buttonBox.add (Box.createVerticalStrut(2));

        getContentPane().add(buttonBox, "East");
        pack();
        show();

                                    // get needed system properties
        Properties prop = System.getProperties ();
        FILE_SEP = prop.getProperty ("file.separator");
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        FileDialog fd;
        String lastFile, lastDir;
        boolean fail = false;

        if (cmd.equals("XData")) {
            operation = X_DATA;
        } else if (cmd.equals("XList")) {
            operation = X_LIST;
        } else if (cmd.equals("Close")) {
            setVisible(false);
        } else if (cmd.equals("Apply")) {
            DefaultMutableTreeNode dmn;
            Vector v = tree.getSelectedPaths();
            switch (operation) {
            case X_DATA:
                // Perform operation (filter TBD)
                //export the data
                // use file dialog to get file name/loc?
                //  (extend file dialog class to add more functionality/options?)
                fd = new FileDialog (parent,
                                     resource.getString("ExportDataTo"),
                                     FileDialog.SAVE);
                //fd.setDirectory ("");
                fd.setFile ("dash.txt");
                fd.show();
                lastDir = fd.getDirectory();
                lastFile = fd.getFile ();
                if (lastFile != null)
                    exportInteractively(v, new File(lastDir, lastFile));
                break;
            case X_LIST:
                // Perform operation (filter TBD)
                //export the hierarchy
                // use file dialog to get file name/loc?
                //  (extend file dialog class to add more functionality/options?)
                fd = new FileDialog (parent,
                                     resource.getString("ExportHierarchyTo"),
                                     FileDialog.SAVE);
                //fd.setDirectory ("");
                fd.setFile ("hierarch.txt");
                fd.show();
                lastFile = fd.getFile ();
                if (lastFile != null) {
                    JDialog working;
                    working = new JDialog (parent, resource.getString("ExportExportingDots"));
                    working.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    JLabel lab = new JLabel (resource.getString("ExportExportingMessage"));
                    working.getContentPane().add(lab, "Center");
                    working.pack();
                    working.show();
                    Thread.yield();

                    lastDir  = fd.getDirectory ();
                    try {
                        PrintWriter out =
                            new PrintWriter (new BufferedWriter
                                             (new FileWriter(lastDir + FILE_SEP + lastFile)));
                        parent.getHierarchy().orderedDump(out, v);
                        out.close();
                    } catch (IOException ioe) {
                        fail = true; System.out.println("IOException: " + e);
                    };
                    lab.setText (resource.getString("ExportComplete"));
                    working.invalidate();
                }
                break;
            }
        }
    }

    public void exportInteractively(Vector filter, File dest) {
        exportInteractively(this, parent, filter, dest);
    }

    public static void exportInteractively(Object window, ProcessDashboard parent,
                                           Vector filter, File dest) {
        ProgressDialog p = null;
        if (window instanceof Dialog)
            p = new ProgressDialog((Dialog)window,
                              resource.getString("ExportExporting"),
                              resource.getString("ExportExportingDataDots") );
        else if (window instanceof Frame)
            p = new ProgressDialog((Frame)window,
                              resource.getString("ExportExporting"),
                              resource.getString("ExportExportingDataDots") );

        p.addTask(new ExportTask(parent, filter, dest));
        p.setCompletionMessage(resource.getString("ExportDone"));
        p.run();
    }

    public static class ExportTask implements Runnable {
        ProcessDashboard parent; Vector filter;  File dest;
        public ExportTask(ProcessDashboard p, Vector f, File d) {
            parent = p; filter = f;  dest = d; }
        public void run() { export(parent, filter, dest); }
    }

    /** If the named schedule were to be exported to a file, what would
     * the data element be named?
     */
    public static String exportedScheduleName(DataRepository data,
                                              String scheduleName) {
        SimpleData o = data.getSimpleValue("/Owner");
        String owner = (o == null ? "?????" : safeName(o.format()));
        String name = safeName(scheduleName) + " (" + owner + ")";
        return EVTaskList.MAIN_DATA_PREFIX + name;
    }

    public static void export(ProcessDashboard parent, Vector filter, File dest) {
        boolean fail = false;
        PrintWriter out = null;
        try {
            out = new PrintWriter(new RobustFileWriter(dest));

            // Find and print any applicable task lists.
            Iterator i = parent.getData().getKeys();
            Set taskListNames = new HashSet();
            String name;
            int pos;
            while (i.hasNext()) {
                name = (String) i.next();
                pos = name.indexOf(TASK_ORD_PREF);
                if (pos != -1 && Filter.matchesFilter(filter, name))
                    taskListNames.add(name.substring(pos+TASK_ORD_PREF.length()));
            }
            i = taskListNames.iterator();
            SimpleData o = parent.getData().getSimpleValue("/Owner");
            String owner = (o == null ? "?????" : safeName(o.format()));
            while (i.hasNext()) {
                name = (String) i.next();
                EVTaskList tl = EVTaskList.openExisting
                    (name, parent.getData(), parent.getHierarchy(), parent.getCache(), false);
                if (tl == null) continue;

                tl.recalc();
                String xml = tl.getAsXML();
                xml = new String(xml.getBytes("UTF-8"));
                name = safeName(name) + " (" + owner + ")";
                out.write(EVTaskList.MAIN_DATA_PREFIX + name + "/" +
                          EVTaskListXML.XML_DATA_NAME + ",");
                out.write(StringData.escapeString(xml));
                out.println();
            }

            parent.getData().dumpRepository(out, filter);

            TimeLog tl = new TimeLog();
            TimeLogEntry tle;
            tl.read (parent.getTimeLog());
            Enumeration keys = tl.filter(PropertyKey.ROOT, null, null);
            while (keys.hasMoreElements()) {
                tle = (TimeLogEntry)keys.nextElement();
                if (Filter.matchesFilter (filter, tle.key.path()))
                    out.println(tle.toAbbrevString());
            }

        } catch (IOException ioe) {
            fail = true; System.out.println("IOException: " + ioe);
        }
        out.close();
        if (fail) dest.delete();
    }
    private static String TASK_ORD_PREF = "/" + EVTaskListData.TASK_ORDINAL_PREFIX;
    private static String safeName(String n) {
        return n.replace('/', '_').replace(',', '_');
    }

    public static void exportAll(ProcessDashboard parent) {
        exportAll(parent, Settings.getVal("export.data"));
    }
    public static void exportAll(ProcessDashboard parent, String userSetting) {

        boolean foundWork = false;
        ProgressDialog p = new ProgressDialog(parent,
                                  resource.getString("ExportAutoExporting"),
                                  resource.getString("ExportExportingDataDots"));

        if (userSetting != null && userSetting.length() > 0) {
            // parse the user setting to find data export instructions
            StringTokenizer exportTaskTokens = new StringTokenizer(userSetting, "|");
            while (exportTaskTokens.hasMoreTokens()) {
                String exportTaskStr = exportTaskTokens.nextToken();
                int pos = exportTaskStr.indexOf("=>");
                if (pos == -1) continue;
                String filename =
                    Settings.translateFile(exportTaskStr.substring(0,pos));
                Vector filter = new Vector();
                StringTokenizer filterItems = new StringTokenizer
                    (exportTaskStr.substring(pos+2), ";");
                while (filterItems.hasMoreTokens())
                    filter.add(filterItems.nextToken());
                p.addTask(new ExportTask(parent, filter, new File(filename)));
                foundWork = true;
            }
        }

        // Look for data export instructions in the data repository.
        Iterator i = parent.getData().getKeys();
        String name;
        SimpleData filename;
        int pos;
        while (i.hasNext()) {
            name = (String) i.next();
            pos = name.indexOf(EXPORT_DATANAME);
            if (pos < 1) continue;
            filename = parent.getData().getSimpleValue(name);
            if (filename == null || !filename.test()) continue;
            Vector filter = new Vector();
            filter.add(name.substring(0, pos-1));
            String file_name = Settings.translateFile(filename.format());
            p.addTask(new ExportTask(parent, filter, new File(file_name)));
            foundWork = true;
        }

        if (!foundWork) return;
        p.run();
        System.out.println("Completed user-scheduled data export.");
    }
    public static final String EXPORT_DATANAME = DataImporter.EXPORT_DATANAME;

    private static class DailyExporterThread extends Thread {
        private ProcessDashboard parent;
        private String propDirectory;
        public DailyExporterThread(ProcessDashboard p, String dir) {
            parent = p;
            propDirectory = dir;
            setDaemon(true);
            start();
        }
        public void run() { while (true) runOnce(); }

        public void runOnce() {
            try {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                long timeToSleep = (24 - hour) * MILLIS_PER_HOUR;
                sleep(timeToSleep);

                // wake up sometime during the hour between midnight and 1AM,
                // and export data.
                exportAll(parent);

                FileBackupManager.maybeRun(propDirectory, FileBackupManager.RUNNING);

            } catch (InterruptedException ie) {}
        }
    }
    private static final long MILLIS_PER_HOUR =
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;

    public static void startAutoExporter(ProcessDashboard parent, String propDir) {
        new DailyExporterThread(parent, propDir);
    }
}
