// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.io.File;

class ConfigureButton extends JMenuBar implements ActionListener {
    PSPDashboard   parent       = null;
    PropertyFrame  prop_frame   = null;
    //TaskScheduleDialog   task_frame   = null;
    TimeLogEditor  time_frame   = null;
    DefectEditor   defect_frame = null;
    ImportExport   impexp_frame = null;
    ProbeDialog    probe_dialog = null;
    TaskScheduleChooser task_chooser = null;


    static String FILE_SEP = null;
    static final String ANALYSIS_URL = "/To+Date/PSP/All//reports/index.htm";
    static final String ABOUT_URL    = "/help/Topics/Overview/about.htm";
    static final String PRINT_URL    = "/help/book.html";
    static final String BUG_URL =
        "http://sourceforge.net/tracker/?group_id=9858&atid=109858";
    static final String FORUM_URL =
        "http://sourceforge.net/forum/forum.php?forum_id=30753";

                                    // menu labels
    static final String HIERARCHY_FRAME  = "Hierarchy";
    static final String TIME_LOG_FRAME   = "Time Log";
    static final String DEFECT_LOG_FRAME = "Defect Log";
    static final String PROBE_DIALOG     = "PROBE";
    static final String TASK_DIALOG      = "Task & Schedule";
    static final String DATA_ANALYSIS    = "Data Analysis";
    static final String IMPORT_EXPORT    = "Export";
    static final String HELP_MENU        = "Help";
    static final String EXIT_PROGRAM     = "Exit";

    static final String HELP_FRAME     = "Help Topics";
    static final String HELP_SEARCH    = "Search";
    static final String HELP_PRINT     = "Printable Users Manual";
    static final String HELP_ABOUT     = "About Process Dashboard";
    static final String HELP_BUG       = "Submit bug report";
    static final String HELP_FORUM     = "Online help forum";
    static final String HELP_CONSOLE   = "View debugging output";

                                  // menu labels & cmd text (see above)
    static final String [][] menuItems = {
        { HIERARCHY_FRAME,  "UsingHierarchyEditor" },
        { TIME_LOG_FRAME,   "UsingTimeLogEditor" },
        { DEFECT_LOG_FRAME, "UsingDefectLogEditor" },
        { PROBE_DIALOG,     "UsingProbeTool" },
        { TASK_DIALOG,      "TaskAndSchedule???" },
        { DATA_ANALYSIS,    "DataChartsAndReports" },
        { IMPORT_EXPORT,    "ExportingData" },
        { HELP_MENU,        null },
        { EXIT_PROGRAM,     null } };


    ConfigureButton(PSPDashboard dash) {
        super();
        parent = dash;

        String    s;
        JMenu     menu = new JMenu("C");
        JMenuItem menuItem;
        JMenuItem helpItem = null;
        add (menu);

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
            else
                menu.add(makeMenuItem(s));

            // Can't get this to work, don't know why
            // if (menuItems[ii][1] != null)
            //   PCSH.enableHelp(menuItem, menuItems[ii][1]);
        }

        dash.getContentPane().add(this);

                                    // get needed system properties
        Properties prop = System.getProperties ();
        FILE_SEP = prop.getProperty ("file.separator");
    }

    private JMenuItem makeMenuItem(String text) {
        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(text);
        result.addActionListener(this);
        return result;
    }

    private void addHelpMenu(JMenu menu) {
        JMenu helpMenu = new JMenu(HELP_MENU);
        menu.add(helpMenu);

        // workaround jre 1.3 bug...reference http://developer.java.sun.com/developer/bugParade/bugs/4280243.html
        helpMenu.enableInputMethods(false);

        helpMenu.add(makeMenuItem(HELP_FRAME));
        helpMenu.add(makeMenuItem(HELP_SEARCH));
        helpMenu.add(makeMenuItem(HELP_PRINT));
        helpMenu.add(makeMenuItem(HELP_ABOUT));
        helpMenu.addSeparator();
        helpMenu.add(makeMenuItem(HELP_BUG));
        helpMenu.add(makeMenuItem(HELP_FORUM));
        if ("true".equalsIgnoreCase(Settings.getVal("console.showMenuOption")))
            helpMenu.add(makeMenuItem(HELP_CONSOLE));
    }


    public void quit () {
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

    public void reloadHierarchy(PSPProperties newProps) {
        if (time_frame != null)
            time_frame.reloadAll(newProps);
        if (defect_frame != null)
            defect_frame.reloadAll(newProps);
    }

    protected void startPropertyFrame () {
        if (parent.getProperties() != null) {
            if (prop_frame != null)
                prop_frame.show();
            else
                prop_frame = new PropertyFrame(parent,
                                               this,
                                               parent.getProperties(),
                                               parent.getTemplateProperties());
        }
    }

    protected void removePropertyFrame () {
        prop_frame = null;
    }

    protected void startProbeDialog () {
        if (probe_dialog != null)
            probe_dialog.show();
        else
            probe_dialog = new ProbeDialog(parent);
    }

    public void startTimeLog() {
        if (parent.getProperties() != null) {
            if (time_frame != null) {
                time_frame.setSelectedNode(parent.getCurrentPhase());
                time_frame.show();
            } else
                time_frame = new TimeLogEditor(parent,
                                               this,
                                               parent.getProperties());
        }
    }

    public void startDefectLog() {
        if (parent.getProperties() != null) {
            if (defect_frame != null) {
                defect_frame.setSelectedPhase(parent.getCurrentPhase());
                defect_frame.showIt();
            } else
                defect_frame = new DefectEditor(parent,
                                                this,
                                                parent.getProperties());
        }
    }

    public void startImportExport() {
        if (parent.getProperties() != null) {
            if (impexp_frame != null)
                impexp_frame.show();
            else
                impexp_frame = new ImportExport(parent);
        }
    }

    public void addToTimeLogEditor (TimeLogEntry tle) {
        if (time_frame != null)
            time_frame.addRow (tle);
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

    public void startAboutDialog() { new AboutDialog(parent, ABOUT_URL); }

    public void submitBug () { Browser.launch(BUG_URL); }

    public void launchForum () { Browser.launch(FORUM_URL); }

    public void showConsole () { ConsoleWindow.showInstalledConsole(); }

    public void exitProgram() { parent.exitProgram(); }

    public void save() {
        //    if (task_frame != null)
        //task_frame.save();
    }

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
        } else if (cmd.equals(IMPORT_EXPORT)) {
            startImportExport ();
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
