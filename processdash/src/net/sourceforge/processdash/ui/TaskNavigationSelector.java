// Copyright (C) 2006-2014 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ui.TaskListNavigator;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyNavigator;
import net.sourceforge.processdash.hier.ui.HierarchyTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.util.FallbackObjectFactory;

public class TaskNavigationSelector implements DashHierarchy.Listener,
        PropertyChangeListener {

    private static final String WIDTH_SETTING = "mainWindow.taskSelectorWidth";

    public interface NavMenuUI {
        public void hierarchyChanged();
        public void activeTaskChanged();
        public boolean selectNext();
        public void delete();
    }

    public interface QuickSelectTaskProvider {
        public TreeTableModel getTaskSelectionChoices();
        public Object getTreeNodeForPath(String path);
        public String getPathForTreeNode(Object node);
    }

    static final String TASK_PROVIDER_KEY = "QuickSelectTaskProvider";
    static final String ACTIVE_TASK_MODEL_KEY = "QuickSelectTaskModel";
    static final String PARENT_COMPONENT_KEY = "QuickSelectParentComponent";

    ProcessDashboard dash;
    ActiveTaskModel activeTaskModel;
    JMenuBar menuBar;
    TaskNavMenu menu;
    ActionListener chooseTaskListAction;
    Action changeTaskAction;
    NavMenuUI currentNavigator;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.NavSelector");

    public TaskNavigationSelector(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model) {
        this.dash = dash;
        this.menuBar = menuBar;
        this.activeTaskModel = model;

        buildMenu();
        createNavigator();

        dash.getHierarchy().addHierarchyListener(this);
        activeTaskModel.addPropertyChangeListener(this);
    }

    public boolean selectNext() {
        return currentNavigator.selectNext();
    }

    public Action getChangeTaskAction() {
        return changeTaskAction;
    }

    public void storePrefs() {
        int width = menuBar.getWidth();
        InternalSettings.set(WIDTH_SETTING, Integer.toString(width));
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        if (SwingUtilities.isEventDispatchThread()) {
            fireHierarchyChanged();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { fireHierarchyChanged(); }});
            } catch (Exception ex) {}
        }
    }

    private void fireHierarchyChanged() {
        if (currentNavigator != null)
            currentNavigator.hierarchyChanged();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == activeTaskModel) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (currentNavigator != null)
                        currentNavigator.activeTaskChanged();
                }
            });
        }
    }

    private void buildMenu() {
        menu = new TaskNavMenu();
        ToolTipTimingCustomizer.INSTANCE.install(menu);
        menuBar.add(menu);

        menuBar.add(new JMenu("I"));
        Dimension d = menuBar.getPreferredSize();
        menuBar.setMinimumSize(d);
        d = new Dimension(d);
        d.width = Settings.getInt(WIDTH_SETTING, 500);
        menuBar.setPreferredSize(d);
        menuBar.remove(1);

        JMenuItem useHierarchy = new JMenuItem();
        useHierarchy.setText(resources.getString("All_Tasks.Menu_Name"));
        useHierarchy.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "chooseHierarchyNavigator"));
        menu.add(useHierarchy);

        JMenuItem useTaskList = new JMenuItem();
        useTaskList.setText(resources.getDlgString("Task_List.Menu_Name"));
        chooseTaskListAction = (ActionListener) EventHandler.create(
                ActionListener.class, this, "chooseTaskListNavigator");
        useTaskList.addActionListener(chooseTaskListAction);
        menu.add(useTaskList);

        changeTaskAction = new FallbackObjectFactory<Action>(Action.class)
                .add("net.sourceforge.processdash.ui.QuickSelectTaskAction")
                .get(false);
        if (changeTaskAction != null) {
            changeTaskAction.putValue(ACTIVE_TASK_MODEL_KEY, activeTaskModel);
            changeTaskAction.putValue(PARENT_COMPONENT_KEY, dash);
            menu.add(new JMenuItem(changeTaskAction));
        }
    }

    private void createNavigator() {
        if (currentNavigator != null) {
            currentNavigator.delete();
            menuBar.removeAll();
            menuBar.add(menu);
            menu.cleanup();
        }

        String navigatorType = Settings.getVal(NAVIGATOR_TYPE_SETTING);
        if (TASK_LIST_TYPE.equalsIgnoreCase(navigatorType))
            createTaskListNavigator();
        else
            // default navigator
            createHierarchyNavigator();

        if (changeTaskAction != null) {
            QuickSelectTaskProvider taskProvider;
            if (currentNavigator instanceof QuickSelectTaskProvider)
                taskProvider = (QuickSelectTaskProvider) currentNavigator;
            else
                taskProvider = new DefaultTaskProvider();
            changeTaskAction.putValue(TASK_PROVIDER_KEY, taskProvider);
        }
    }

    private void createHierarchyNavigator() {
        currentNavigator = new HierarchyNavigator(dash, menuBar,
                activeTaskModel);
    }

    private void createTaskListNavigator() {
        try {
            currentNavigator = new TaskListNavigator(dash, menuBar,
                    chooseTaskListAction, activeTaskModel);
        } catch (IllegalArgumentException e) {
            createHierarchyNavigator();
        }
    }

    public void chooseHierarchyNavigator() {
        InternalSettings.set(NAVIGATOR_TYPE_SETTING, HIERARCHY_TYPE);
        createNavigator();
    }

    public void chooseTaskListNavigator() {
        if (TaskListNavigator.chooseTaskListNavigator(menu, dash, resources)) {
            InternalSettings.set(NAVIGATOR_TYPE_SETTING, TASK_LIST_TYPE);
            createNavigator();
        }
    }

    public static String prettifyPath(PropertyKey node) {
        if (node == null)
            return null;
        else
            return prettifyPath(node.path());
    }

    public static String prettifyPath(String path) {
        if (path == null)
            return null;
        else
            return path.replaceAll("/", " / ").trim();
    }

    private static final String NAVIGATOR_TYPE_SETTING = "navigator.type";
    private static final String HIERARCHY_TYPE = "hierarchy";
    private static final String TASK_LIST_TYPE = "taskList";

    private class DefaultTaskProvider extends HierarchyTreeModel implements
            QuickSelectTaskProvider {

        public DefaultTaskProvider() {
            super(dash.getHierarchy(), resources.getString("Task"));
        }

        public String getPathForTreeNode(Object node) {
            return key(node).path();
        }

        public TreeTableModel getTaskSelectionChoices() {
            return this;
        }

        public Object getTreeNodeForPath(String path) {
            PropertyKey key = dash.getHierarchy().findExistingKey(path);
            return getNodeForKey(key);
        }

    }

    private class TaskNavMenu extends JMenu {

        public TaskNavMenu() {
            setIcon(new PaddedIcon(DashboardIconFactory.getTaskOverflowIcon(),
                    0, 6, 0, 0));
            setIconTextGap(0);
            add(new TaskNavSeparator());
        }

        private void cleanup() {
            while (!(getMenuComponent(0) instanceof TaskNavSeparator))
                remove(0);
            setToolTipText(null);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            return NarrowJMenu.TOOLTIP_LOCATION;
        }
    }

    private class TaskNavSeparator extends JSeparator {}

}
