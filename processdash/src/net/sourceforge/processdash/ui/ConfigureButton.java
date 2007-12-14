// Copyright (C) 1999-2007 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ui.TaskScheduleChooser;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingApprover;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.log.ui.TimeLogEditor;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.diff.ui.OpenLOCDiffAction;
import net.sourceforge.processdash.tool.export.ui.SaveBackupAction;
import net.sourceforge.processdash.tool.export.ui.wizard.ShowExportWizardAction;
import net.sourceforge.processdash.tool.export.ui.wizard.ShowImportWizardAction;
import net.sourceforge.processdash.tool.probe.ProbeDialog;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.util.HTMLUtils;


public class ConfigureButton extends JMenuBar implements ActionListener, HierarchyEditor.Listener {
    ProcessDashboard   parent       = null;
    JMenu menu = null;
    HierarchyEditor  prop_frame   = null;
    //TaskScheduleDialog   task_frame   = null;
    TimeLogEditor  time_frame   = null;
    DefectLogEditor   defect_frame = null;
    ProbeDialog    probe_dialog = null;
    TaskScheduleChooser task_chooser = null;


    static String FILE_SEP = null;
    static final String ANALYSIS_URL =
        "/To+Date/PSP/All//reports/analysis/index.htm";
    static final String ABOUT_URL    = "/help/Topics/Overview/about.htm";
    static final String PRINT_URL    = "/help/book.html";
    static final String BUG_URL =
        "http://sourceforge.net/tracker/?group_id=9858&atid=109858";
    static final String FORUM_URL =
        "http://sourceforge.net/forum/forum.php?forum_id=30753";

    Resources resources = Resources.getDashBundle("ProcessDashboard.Menu");
    Logger logger = Logger.getLogger(ConfigureButton.class.getName());

                                  // menu labels
    static final String HIERARCHY_FRAME  = "Hierarchy";
    static final String TIME_LOG_FRAME   = "Time_Log";
    static final String DEFECT_LOG_FRAME = "Defect_Log";
    static final String PROBE_DIALOG     = "PROBE";
    static final String TASK_DIALOG      = "Task_&_Schedule";
    static final String DATA_ANALYSIS    = "Data_Analysis";
    static final String TOOL_MENU        = "Tools";
    static final String IMPORT           = "Import";
    static final String EXPORT           = "Export";
    static final String HELP_MENU        = "Help.Help";
    static final String EXIT_PROGRAM     = "Exit";
    static final String L10N_TOOL        = "l10nTool";

    static final String HELP_FRAME     = "Help.Help_Topics";
    static final String HELP_SEARCH    = "Help.Search";
    static final String HELP_PRINT     = "Help.Printable_Users_Manual";
    static final String HELP_ABOUT     = "Help.About_Process_Dashboard";
    static final String HELP_BUG       = "Help.Submit_bug_report";
    static final String HELP_FORUM     = "Help.Online_help_forum";
    static final String HELP_CONSOLE   = "Help.View_debugging_output";

                                  // menu labels & cmd text (see above)
    static final String [][] menuItems = {
        { HIERARCHY_FRAME,  "UsingHierarchyEditor" },
        { TIME_LOG_FRAME,   "UsingTimeLogEditor" },
        { DEFECT_LOG_FRAME, "UsingDefectLogEditor" },
//    { PROBE_DIALOG,     "UsingProbeTool" },
        { TASK_DIALOG,      "TaskAndSchedule???" },
        { DATA_ANALYSIS,    "DataChartsAndReports" },
//    { IMPORT_EXPORT,    "ExportingData" },
        { TOOL_MENU,        null },
        { HELP_MENU,        null },
        { EXIT_PROGRAM,     null } };


    public ConfigureButton(ProcessDashboard dash) {
        super();
        parent = dash;

        String    s;
        menu = new JMenu(resources.getString("Main_Menu_Name"));
        add (menu);
        setMinimumSize(getPreferredSize());

        /*
         * I'm commenting out the following line for now.  Although eventually
         * we will want context-sensitive help for the Configure Menu, the
         * following line unfortunately causes F1, pressed from the main
         * dashboard window, to bring up ConfigureMenu help.
         */
        //PCSH.enableHelpKey(this, "ConfigureMenu");

        BetaVersionSetup.addSubmenu(menu);

        for (int ii = 0; ii < menuItems.length; ii++) {
            s = menuItems[ii][0];
            if (HELP_MENU.equals(s))
                addHelpMenu(menu);
            else if (TOOL_MENU.equals(s))
                addToolMenu(menu);
            else
                menu.add(makeMenuItem(s));

            // Can't get this to work, don't know why
            // if (menuItems[ii][1] != null)
            //   PCSH.enableHelp(menuItem, menuItems[ii][1]);
        }

                                    // get needed system properties
        Properties prop = System.getProperties ();
        FILE_SEP = prop.getProperty ("file.separator");
    }

    public JMenu getMainMenu() {
        return menu;
    }

    private JMenuItem makeMenuItem(String text) {
        JMenuItem result = new JMenuItem(resources.getString(text));
        result.setActionCommand(text);
        result.addActionListener(this);
        return result;
    }

    private void addToolMenu(JMenu menu) {
        JMenu toolMenu = new JMenu(resources.getString(TOOL_MENU));
        menu.add(toolMenu);

        // workaround jre 1.3 bug...reference http://developer.java.sun.com/developer/bugParade/bugs/4280243.html
        toolMenu.enableInputMethods(false);

        toolMenu.add(makeMenuItem(PROBE_DIALOG));
        if (Settings.isReadWrite()) {
            toolMenu.add(new ShowImportWizardAction(resources.getString(IMPORT)));
            toolMenu.add(new ShowExportWizardAction(resources.getString(EXPORT)));
        }
        toolMenu.add(new SaveBackupAction(parent.getData()));
        toolMenu.add(new OpenLOCDiffAction());
        addToolExtensions(toolMenu);
    }

    private void addToolExtensions(JMenu toolMenu) {
        List items = ExtensionManager.getExecutableExtensions("toolsMenuItem",
                parent);
        for (Iterator i = items.iterator(); i.hasNext();) {
            Object item = (Object) i.next();
            if (item instanceof Action)
                toolMenu.add((Action) item);
            else if (item instanceof JMenuItem)
                toolMenu.add((JMenuItem) item);
            else
                logger.warning("Could not add item to tools menu - "
                        + "unrecognized class " + item.getClass().getName());
        }
    }

    private String getDashboardJarFileName() {
        String myURL = ConfigureButton.class.getResource
            ("ConfigureButton.class").toString();
        // we are expecting a URL like (Windows)
        // jar:file:/D:/path/to/pspdash.jar!/net/.../ConfigureButton.class
        // or (Unix)
        // jar:file:/usr/path/to/pspdash.jar!/net/.../ConfigureButton.class
        if (myURL.startsWith("jar:file:")) {
            String jarFileName = myURL.substring(9,myURL.indexOf("!/net/"));
            jarFileName = HTMLUtils.urlDecode(jarFileName);
            File jarFile = new File(jarFileName);
            return jarFile.getPath();
        }

        return null;
    }

    private void addHelpMenu(JMenu menu) {
        JMenu helpMenu = new JMenu(resources.getString(HELP_MENU));
        JMenuItem search;
        menu.add(helpMenu);

        // workaround jre 1.3 bug...reference http://developer.java.sun.com/developer/bugParade/bugs/4280243.html
        helpMenu.enableInputMethods(false);

        helpMenu.add(makeMenuItem(HELP_FRAME));
        helpMenu.add(search = makeMenuItem(HELP_SEARCH));
        helpMenu.add(makeMenuItem(HELP_PRINT));
        helpMenu.add(makeMenuItem(HELP_ABOUT));
        helpMenu.addSeparator();
        helpMenu.add(makeMenuItem(HELP_BUG));
        helpMenu.add(makeMenuItem(HELP_FORUM));
        if ("true".equalsIgnoreCase(Settings.getVal("console.showMenuOption")))
            helpMenu.add(makeMenuItem(HELP_CONSOLE));
        if (!PCSH.isSearchSupported())
            search.setEnabled(false);
    }


    public void saveData () {
        if (time_frame != null) {
            time_frame.confirmClose(false);
            time_frame = null;
        }
        if (prop_frame != null) {
            prop_frame.confirmClose(false);
            prop_frame = null;
        }
        if (defect_frame != null) {
            defect_frame.quit();
            defect_frame = null;
        }
    }

    public void saveOrRevertTimeLog() {
        if (time_frame != null)
            time_frame.saveRevertOrCancel(false);
    }

    protected void startPropertyFrame () {
        if (parent.getProperties() != null) {
            if (prop_frame != null)
                prop_frame.show();
            else {
                prop_frame = new HierarchyEditor(parent,
                                               this,
                                               parent.getProperties(),
                                               parent.getTemplateProperties());
                prop_frame.addHierarchyEditorListener(this);
            }
        }
    }

    public void hierarchyEditorClosed(HierarchyEditor editor) {
        prop_frame = null;
        editor.removeHierarchyEditorListener(this);
    }

    public boolean isHierarchyEditorOpen() {
        return prop_frame != null;
    }

    protected void startProbeDialog () {
        if (probe_dialog != null)
            probe_dialog.setVisible(true);
        else
            probe_dialog = new ProbeDialog(parent.getHierarchy(), parent.getData());
    }

    public void startTimeLog() {
        startTimeLog(null);
    }

    public void startTimeLog(PropertyKey phase) {
        DashHierarchy hier = parent.getProperties();
        if (phase == null)
            phase = parent.getCurrentPhase();

        if (hier != null) {
            if (time_frame != null) {
                time_frame.setSelectedNode(phase);
                time_frame.show();
            } else {
                DashboardTimeLog timeLog =
                    (DashboardTimeLog) parent.getTimeLog();
                TimeLoggingApprover approver = timeLog;
                time_frame = new TimeLogEditor(timeLog, hier, approver, phase);
            }
        }
    }

    public void startDefectLog() {
        if (parent.getProperties() != null) {
            if (defect_frame != null) {
                defect_frame.setSelectedPhase(parent.getCurrentPhase());
                defect_frame.showIt();
            } else
                defect_frame = new DefectLogEditor(parent,
                                                this,
                                                parent.getProperties());
        }
    }


    public void startTaskDialog() {
        if (parent.getProperties() != null) {
            if (task_chooser != null && task_chooser.isDisplayable())
                task_chooser.toFront();
            else
                task_chooser = new TaskScheduleChooser(parent);
        }
    }

    public void startDataAnalysis() { Browser.launch(ANALYSIS_URL); }

    public void startHelp() { PCSH.displayHelpTopic("QuickOverview"); }

    public void startHelpSearch() { PCSH.displaySearchTab(); }

    public void showPrintableManual() { Browser.launch(PRINT_URL); }

    public void startAboutDialog() {
        new AboutDialog(parent, resources.getString(HELP_ABOUT), ABOUT_URL);
    }

    public void submitBug () { Browser.launch(BUG_URL); }

    public void launchForum () { Browser.launch(FORUM_URL); }

    public void showConsole () { ConsoleWindow.showInstalledConsole(); }

    public void exitProgram() { parent.exitProgram(); }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals(HIERARCHY_FRAME)) {
            startPropertyFrame ();
        } else if (cmd.equals(TIME_LOG_FRAME)) {
            startTimeLog();
        } else if (cmd.equals(DEFECT_LOG_FRAME)) {
            startDefectLog();
        } else if (cmd.equals(PROBE_DIALOG)) {
            startProbeDialog ();
        } else if (cmd.equals(TASK_DIALOG)) {
            startTaskDialog ();
        } else if (cmd.equals(DATA_ANALYSIS)) {
            startDataAnalysis ();
        } else if (cmd.equals(HELP_FRAME)) {
            startHelp ();
        } else if (cmd.equals(HELP_SEARCH)) {
            startHelpSearch ();
        } else if (cmd.equals(HELP_PRINT)) {
            showPrintableManual ();
        } else if (cmd.equals(HELP_ABOUT)) {
            startAboutDialog ();
        } else if (cmd.equals(HELP_BUG)) {
            submitBug ();
        } else if (cmd.equals(HELP_FORUM)) {
            launchForum ();
        } else if (cmd.equals(HELP_CONSOLE)) {
            showConsole ();
        } else if (cmd.equals(EXIT_PROGRAM)) {
            exitProgram ();
        }
    }

}
