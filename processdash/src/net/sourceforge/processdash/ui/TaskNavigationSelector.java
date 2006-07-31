// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.TaskListNavigator;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyMenu;
import net.sourceforge.processdash.hier.ui.HierarchyTreeModel;
import net.sourceforge.processdash.hier.ui.HierarchyTreeModel.HierarchyTreeNode;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;

public class TaskNavigationSelector {

    public interface NavMenuUI extends PropertyChangeListener {
        public String getNavMenuDisplayName();
        public boolean selectNext();
        public void delete();
    }

    ProcessDashboard dash;
    ActiveTaskModel activeTaskModel;
    JMenuBar menuBar;
    JMenu menu;
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
    }

    public boolean selectNext() {
        return currentNavigator.selectNext();
    }

    public void hierarchyChanged() {
        createNavigator();
    }


    private void buildMenu() {
        this.menu = new JMenu();
        new ToolTipTimingCustomizer().install(this.menu);
        menuBar.add(menu);

        JMenuItem useHierarchy = new JMenuItem();
        useHierarchy.setText(resources.getDlgString("Hierarchy.Menu_Name"));
        useHierarchy.setIcon(HierarchyMenu.HIER_ICON);
        useHierarchy.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "chooseHierarchyNavigator"));
        menu.add(useHierarchy);

        JMenuItem useTaskList = new JMenuItem();
        useTaskList.setText(resources.getDlgString("Task_List.Menu_Name"));
        useTaskList.setIcon(TaskListNavigator.TASK_LIST_ICON);
        useTaskList.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "chooseTaskListNavigator"));
        menu.add(useTaskList);
    }

    private void createNavigator() {
        if (currentNavigator != null) {
            activeTaskModel.removePropertyChangeListener(currentNavigator);
            currentNavigator.delete();
        }

        String navigatorType = Settings.getVal(NAVIGATOR_TYPE_SETTING);
        if (TASK_LIST_TYPE.equalsIgnoreCase(navigatorType))
            createTaskListNavigator();
        else
            // default navigator
            createHierarchyNavigator();

        activeTaskModel.addPropertyChangeListener(currentNavigator);
    }

    private void createHierarchyNavigator() {
        String path = Settings.getVal(HIERARCHY_ROOT_PATH_SETTING, "/");
        PropertyKey key = dash.getProperties().findExistingKey(path);
        if (key == null)
            key = PropertyKey.ROOT;

        currentNavigator = new HierarchyMenu(dash, menuBar, activeTaskModel,
                key);
        menu.setIcon(HierarchyMenu.HIER_ICON);
        menu.setToolTipText(currentNavigator.getNavMenuDisplayName());
    }


    private void createTaskListNavigator() {
        String taskListName = Settings.getVal(TASK_LIST_NAME_SETTING);
        try {
            currentNavigator = new TaskListNavigator(menuBar, dash,
                    activeTaskModel, taskListName);
            menu.setIcon(TaskListNavigator.TASK_LIST_ICON);
            menu.setToolTipText(currentNavigator.getNavMenuDisplayName());
        } catch (IllegalArgumentException e) {
            createHierarchyNavigator();
        }
    }



    public void chooseHierarchyNavigator() {
        HierarchyTreeModel model = new HierarchyTreeModel(dash.getProperties());
        model.setRootName(resources.getString("Hierarchy.Root_Node_Name"));
        JTree tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setSelectionInterval(0, 0);
        tree.setToggleClickCount(4);
        new JOptionPaneClickHandler().install(tree);

        JScrollPane sp = new JScrollPane(tree);
        sp.setPreferredSize(new Dimension(200, 200));

        String title = resources.getString("Hierarchy.Dialog.Title");
        Object[] message = new Object[] {
                resources.getStrings("Hierarchy.Dialog.Prompt"), sp };
        if (JOptionPane.showConfirmDialog(menu, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String path = getSelectedPathForHierarchyNavigator(tree);
            if (path != null) {
                InternalSettings.set(NAVIGATOR_TYPE_SETTING, HIERARCHY_TYPE);
                InternalSettings.set(HIERARCHY_ROOT_PATH_SETTING, path);
                createNavigator();
            }
        }

        tree.setModel(null);
    }

    private String getSelectedPathForHierarchyNavigator(JTree tree) {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath == null)
            // no node was selected
            return null;

        HierarchyTreeNode node = (HierarchyTreeNode) selectedPath
                .getLastPathComponent();
        if (node == null)
            // not sure if this can happen, but be safe.
            return null;

        if (node.getChildCount() == 0)
            // don't allow the user to select leaf nodes.  The HierarchyMenu
            // class can't handle that very well.
            node = (HierarchyTreeNode) node.getParent();

        if (node == null)
            return null;
        else
            return node.getPath();
    }

    public void chooseTaskListNavigator() {
        String[] taskListNames = EVTaskList.findTaskLists(dash.getData(), true,
                false);

        if (taskListNames == null || taskListNames.length == 0) {
            String title = resources.getString("Task_List.No_Lists.Title");
            String[] msg = resources.getStrings("Task_List.No_Lists.Message");
            JOptionPane.showMessageDialog(menu, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] displayNames = EVTaskList.getDisplayNames(taskListNames);
        JList taskLists = new JList(displayNames);
        new JOptionPaneClickHandler().install(taskLists);
        JScrollPane sp = new JScrollPane(taskLists);
        sp.setPreferredSize(new Dimension(200, 200));
        String title = resources.getString("Task_List.Dialog.Title");
        Object message = new Object[] {
                resources.getString("Task_List.Dialog.Prompt"), sp };
        if (JOptionPane.showConfirmDialog(menu, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int selIndex = taskLists.getSelectedIndex();
            if (selIndex != -1) {
                String name = taskListNames[selIndex];
                InternalSettings.set(NAVIGATOR_TYPE_SETTING, TASK_LIST_TYPE);
                InternalSettings.set(TASK_LIST_NAME_SETTING, name);
                createNavigator();
            }
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
    private static final String HIERARCHY_ROOT_PATH_SETTING = "navigator.hierarchyPath";
    private static final String TASK_LIST_TYPE = "taskList";
    private static final String TASK_LIST_NAME_SETTING = "navigator.taskListName";

}
