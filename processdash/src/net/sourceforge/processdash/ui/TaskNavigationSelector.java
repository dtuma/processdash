// Copyright (C) 2006-2007 Tuma Solutions, LLC
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

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ui.TaskListNavigator;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyMenu;
import net.sourceforge.processdash.i18n.Resources;
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
        currentNavigator = new HierarchyMenu(dash, menuBar, activeTaskModel);
        menu.setIcon(HierarchyMenu.HIER_ICON);
        menu.setToolTipText(currentNavigator.getNavMenuDisplayName());
    }

    private void createTaskListNavigator() {
        try {
            currentNavigator = new TaskListNavigator(dash, menuBar,
                    activeTaskModel);
            menu.setIcon(TaskListNavigator.TASK_LIST_ICON);
            menu.setToolTipText(currentNavigator.getNavMenuDisplayName());
        } catch (IllegalArgumentException e) {
            createHierarchyNavigator();
        }
    }

    public void chooseHierarchyNavigator() {
        if (HierarchyMenu.chooseHierarchyNavigator(menu, dash, resources)) {
            InternalSettings.set(NAVIGATOR_TYPE_SETTING, HIERARCHY_TYPE);
            createNavigator();
        }
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

}
