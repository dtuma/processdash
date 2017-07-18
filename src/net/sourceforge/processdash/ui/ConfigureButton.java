// Copyright (C) 1999-2017 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ev.ui.TaskScheduleChooser;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingApprover;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.log.ui.TimeLogEditor;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.ui.ShowUserGroupEditorAction;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.diff.ui.OpenLOCDiffAction;
import net.sourceforge.processdash.tool.export.ui.SaveBackupAction;
import net.sourceforge.processdash.tool.export.ui.wizard.ShowExportWizardAction;
import net.sourceforge.processdash.tool.export.ui.wizard.ShowImportWizardAction;
import net.sourceforge.processdash.tool.mobile.OpenMobileSetupWizard;
import net.sourceforge.processdash.tool.perm.ui.ShowRolesEditorAction;
import net.sourceforge.processdash.tool.perm.ui.ShowUserEditorAction;
import net.sourceforge.processdash.tool.prefs.OpenPreferencesDialogAction;
import net.sourceforge.processdash.tool.probe.ProbeDialog;
import net.sourceforge.processdash.tool.quicklauncher.OpenDatasetAction;
import net.sourceforge.processdash.ui.help.DashHelpBroker;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.WindowsFlatMenuBar;


public class ConfigureButton extends WindowsFlatMenuBar
        implements ActionListener, HierarchyEditor.Listener {

    ProcessDashboard   parent       = null;
    HierarchyEditor  prop_frame   = null;
    //TaskScheduleDialog   task_frame   = null;
    TimeLogEditor  time_frame   = null;
    DefectLogEditor   defect_frame = null;
    ProbeDialog    probe_dialog = null;
    TaskScheduleChooser task_chooser = null;


    static final String ANALYSIS_URL =
        "/To+Date/PSP/All//reports/analysis/index.htm";
    static final String PRINT_URL    = "/help/book.html";
    static final String BUG_URL =   "http://www.processdash.com/bugTracker";
    static final String FORUM_URL = "http://www.processdash.com/helpForum";

    static Resources resources = Resources.getDashBundle("ProcessDashboard.Menu");
    Logger logger = Logger.getLogger(ConfigureButton.class.getName());

                                  // menu labels
    static final String HIERARCHY_FRAME  = "Hierarchy";
    static final String TIME_LOG_FRAME   = "Time_Log";
    static final String DEFECT_LOG_FRAME = "Defect_Log";
    static final String PROBE_DIALOG     = "PROBE";
    static final String TASK_DIALOG      = "Task_&_Schedule";
    static final String DATA_ANALYSIS    = "PSP_Data_Analysis";
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


    public ConfigureButton(ProcessDashboard dash) {
        super();
        parent = dash;

        if (Settings.isPersonalMode())
            buildPersonalMenu();
        else
            buildTeamMenus();
    }

    private void buildPersonalMenu() {
        JMenu menu = new JMenu(resources.getString("Main_Menu_Name"));
        add(menu);
        setMinimumSize(getPreferredSize());

        BetaVersionSetup.addSubmenu(menu);

        menu.add(makeMenuItem(HIERARCHY_FRAME));
        menu.add(makeMenuItem(TIME_LOG_FRAME));
        menu.add(makeMenuItem(DEFECT_LOG_FRAME));
        menu.add(makeMenuItem(TASK_DIALOG));
        menu.add(new DataAnalysisWatcher(makeMenuItem(DATA_ANALYSIS)).menuItem);
        menu.add(buildToolMenu(true));
        menu.add(buildHelpMenu());
        menu.add(makeMenuItem(EXIT_PROGRAM));
    }

    private void buildTeamMenus() {
        add(buildTeamFileMenu());
        add(buildToolMenu(false));
        add(buildHelpMenu());
    }

    private JMenu buildTeamFileMenu() {
        JMenu fileMenu = new JMenu(resources.getString("File"));

        BetaVersionSetup.addSubmenu(fileMenu);

        fileMenu.addSeparator();

        fileMenu.add(makeMenuItem(TASK_DIALOG));
        fileMenu.addSeparator();

        SaveBackupAction saveBackupAction = new SaveBackupAction(parent);
        fileMenu.add(saveBackupAction);
        fileMenu.add(new OpenDatasetAction(parent, saveBackupAction));
        fileMenu.addSeparator();

        fileMenu.add(makeMenuItem(EXIT_PROGRAM));

        return fileMenu;
    }

    private JMenuItem makeMenuItem(String text) {
        JMenuItem result = new JMenuItem(resources.getString(text));
        result.setActionCommand(text);
        result.addActionListener(this);
        return result;
    }

    private JMenu buildToolMenu(boolean personalMode) {
        JMenu toolMenu = new JMenu(resources.getString(TOOL_MENU));
        List extensionItems = ExtensionManager.getExecutableExtensions(
            "toolsMenuItem", parent);
        Collections.sort(extensionItems, new ToolMenuItemComparator());

        toolMenu.add(new OpenPreferencesDialogAction(parent));
        toolMenu.add(new OfflineModeToggleMenuItem(parent.getWorkingDirectory()));
        addToolExtensions(toolMenu, extensionItems, "prefs");
        if (!personalMode) {
            toolMenu.add(new ShowRolesEditorAction(parent));
            toolMenu.add(new ShowUserEditorAction(parent));
            if (UserGroupManager.getInstance().isEnabled())
                toolMenu.add(new ShowUserGroupEditorAction(parent));
        }
        toolMenu.addSeparator();

        if (personalMode)
            toolMenu.add(makeMenuItem(PROBE_DIALOG));
        if (Settings.isReadWrite()) {
            toolMenu.add(new ShowImportWizardAction(resources.getString(IMPORT)));
            toolMenu.add(new ShowExportWizardAction(resources.getString(EXPORT)));
        }
        addToolExtensions(toolMenu, extensionItems, "data");
        if (personalMode)
            toolMenu.add(new OpenMobileSetupWizard(resources
                    .getString("Mobile_App")));
        toolMenu.addSeparator();
        int sepCount = toolMenu.getMenuComponentCount();

        if (personalMode) {
            SaveBackupAction saveBackupAction = new SaveBackupAction(parent);
            toolMenu.add(saveBackupAction);
            toolMenu.add(new OpenDatasetAction(parent, saveBackupAction));
            addToolExtensions(toolMenu, extensionItems, "backups");
            toolMenu.addSeparator();
            toolMenu.add(new OpenLOCDiffAction());
        }

        // add uncategorized tool menu items to the bottom of the menu.
        addToolExtensions(toolMenu, extensionItems, null);

        // if the menu ends with a separator, remove it.
        if (sepCount == toolMenu.getMenuComponentCount())
            toolMenu.remove(sepCount - 1);

        return toolMenu;
    }

    private void addToolExtensions(JMenu toolMenu, List items, String category) {
        for (Iterator i = items.iterator(); i.hasNext();) {
            Object item = (Object) i.next();
            if (item instanceof Action) {
                Action action = (Action) item;
                if (matchesCategory(action, category)) {
                    toolMenu.add(action);
                    i.remove();
                }

            } else if (item instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) item;
                if (matchesCategory(menuItem.getAction(), category)) {
                    toolMenu.add(menuItem);
                    i.remove();
                }

            } else {
                logger.warning("Could not add item to tools menu - "
                        + "unrecognized class " + item.getClass().getName());
                i.remove();
            }
        }
    }

    private boolean matchesCategory(Action action, String category) {
        if (category == null)
            return true;
        else if (action == null)
            return false;
        else
            return category.equals(action.getValue("toolsMenuCategory"));
    }

    private static class ToolMenuItemComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return getName(o1).compareToIgnoreCase(getName(o2));
        }
        private String getName(Object obj) {
            String result = null;
            if (obj instanceof Action) {
                result = String.valueOf(((Action) obj).getValue(Action.NAME));
            } else if (obj instanceof JMenuItem) {
                result = ((JMenuItem) obj).getText();
            }
            return (result == null ? "" : result);
        }
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu(resources.getString(HELP_MENU));
        JMenuItem search;

        helpMenu.add(makeMenuItem(HELP_FRAME));
        helpMenu.add(search = makeMenuItem(HELP_SEARCH));
        helpMenu.add(makeMenuItem(HELP_PRINT));
        addExternalPrintableManuals(helpMenu);
        helpMenu.add(makeMenuItem(HELP_ABOUT));
        helpMenu.addSeparator();
        helpMenu.add(makeMenuItem(HELP_BUG));
        helpMenu.add(makeMenuItem(HELP_FORUM));
        if ("true".equalsIgnoreCase(Settings.getVal("console.showMenuOption")))
            helpMenu.add(makeMenuItem(HELP_CONSOLE));
        if (!PCSH.isSearchSupported())
            search.setEnabled(false);

        return helpMenu;
    }


    private void addExternalPrintableManuals(JMenu helpMenu) {
        Map<String, String> manuals = DashHelpBroker.getExternalPrintableManuals();
        for (Map.Entry<String, String> e : manuals.entrySet()) {
            String title = e.getKey();
            String url = e.getValue();
            helpMenu.add(new ShowExternalManualAction(title, url));
        }
    }

    private class ShowExternalManualAction extends AbstractAction {
        private String url;

        private ShowExternalManualAction(String title, String url) {
            super(title);
            if (url.startsWith("/"))
                this.url = url;
            else
                this.url = "/" + url;
        }

        public void actionPerformed(ActionEvent e) {
            Browser.launch(url);
        }
    }

    public void saveAndCloseHierarchyEditor () {
        if (prop_frame != null) {
            prop_frame.confirmClose(false);
            prop_frame = null;
        }
    }

    public void startPropertyFrame () {
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
                parent.addApplicationEventListener(time_frame);
            }
        }
    }

    public void startDefectLog() {
        startDefectLog(null);
    }

    public void startDefectLog(PropertyKey phase) {
        if (parent.getProperties() != null) {
            if (phase == null)
                phase = parent.getCurrentPhase();

            if (defect_frame != null) {
                defect_frame.setSelectedPhase(phase);
                defect_frame.showIt();
            } else {
                defect_frame = new DefectLogEditor(parent, this,
                    parent.getProperties(), phase);
                parent.addApplicationEventListener(defect_frame);
            }
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

    public void startHelp() {
          PCSH.displayHelpTopic(Settings.isPersonalMode() ? "QuickOverview"
                  : "TeamUseOverview");
    }

    public void startHelpSearch() { PCSH.displaySearchTab(); }

    public void showPrintableManual() { Browser.launch(PRINT_URL); }

    public void startAboutDialog() {
        new AboutDialog(parent, resources.getString(HELP_ABOUT));
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

    private class DataAnalysisWatcher implements DashHierarchy.Listener {
        private JMenuItem menuItem;
        protected DataAnalysisWatcher(JMenuItem pspDataAnalysisMenuItem) {
            this.menuItem = pspDataAnalysisMenuItem;
            updatePSPDataAnalysisVisibility();
            if (!menuItem.isVisible())
                parent.getHierarchy().addHierarchyListener(this);
        }
        public void hierarchyChanged(Event e) {
            updatePSPDataAnalysisVisibility();
        }
        public void updatePSPDataAnalysisVisibility() {
            SimpleData sd = parent.getData().getSimpleValue("/PSP/Project List");
            boolean hasPspTasks = (sd != null && sd.test());
            menuItem.setVisible(hasPspTasks);
        }
    }

}
