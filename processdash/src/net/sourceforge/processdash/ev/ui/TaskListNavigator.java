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

package net.sourceforge.processdash.ev.ui;


import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;
import net.sourceforge.processdash.ui.lib.SwingWorker;


public class TaskListNavigator implements TaskNavigationSelector.NavMenuUI,
        ActionListener, DataListener {

    private JMenuBar menuBar;

    private DashboardContext context;

    private ActiveTaskModel activeTaskModel;

    private String taskListName;

    private Set listeningToData;

    private JMenu menu;

    private JMenu completedTasksMenu;

    private EVTaskList taskList;

    private static Resources resources = Resources.getDashBundle("EV");


    public TaskListNavigator(JMenuBar menuBar, DashboardContext context,
            ActiveTaskModel activeTaskModel, String taskListName)
            throws IllegalArgumentException {
        this.menuBar = menuBar;
        this.context = context;
        this.activeTaskModel = activeTaskModel;
        this.taskListName = taskListName;
        this.listeningToData = new HashSet();

        createMenus();
        syncTaskToModel();

        new TaskListOpener().start();
    }

    private class TaskListOpener extends SwingWorker {

        public Object construct() {
            menuBar.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            if (taskListName == null || taskListName.trim().length() == 0)
                return resources.getString("Navigator.Errors.None_Selected");

            String displayName = EVTaskList.cleanupName(taskListName);

            EVTaskList tl = EVTaskList.openExisting(taskListName, context
                    .getData(), context.getHierarchy(), context.getCache(),
                    false);
            if (tl == null)
                return resources.format("Navigator.Errors.Not_Found_FMT",
                        displayName);

            if (!(tl instanceof EVTaskListData))
                return resources.format("Navigator.Errors.Invalid_FMT",
                        displayName);

            ((EVTaskListData) tl).recalcLeavesOnly();
            tl.recalc();
            menuBar.setCursor(null);

            return tl;
        }

        public void finished() {
            if (get() instanceof EVTaskList)
                installTaskList((EVTaskList) get());
            else
                installMessage(String.valueOf(get()));
        }

    }


    private void reloadTaskList() {
        new TaskListOpener().start();
    }

    public String getNavMenuDisplayName() {
        return EVTaskList.cleanupName(taskListName);
    }

    public void delete() {
        if (menuBar != null)
            menuBar.remove(menu);
        installTaskList(null);
    }

    private void createMenus() {
        completedTasksMenu = new JMenu(resources
                .getString("Navigator.Completed_Items"));

        menu = new NarrowJMenu();
        menuBar.add(menu);
        installMessage(resources.getString("Navigator.Loading_Message"));
    }

    private void installMessage(String message) {
        menu.removeAll();

        JMenuItem menuItem = new JMenuItem(message);
        menuItem.setEnabled(false);
        menu.add(menuItem);
        installTaskList(null);
    }

    private void installTaskList(EVTaskList newTaskList) {
        if (this.taskList != null)
            this.taskList.dispose();
        else if (newTaskList == null)
            EVTaskList.removeTaskListSaveListener(this);
        else
            EVTaskList.addTaskListSaveListener(this);

        this.taskList = newTaskList;
        reloadMenus();
    }

    /*
    private void maybeSelectFirstTask() {
        String currentTask = activeTaskModel.getPath();
        JMenuItem firstItem = null;
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            if (menu.getMenuComponent(i) instanceof TaskJMenuItem) {
                TaskJMenuItem item = (TaskJMenuItem) menu.getMenuComponent(i);
                if (item.getPath().equals(currentTask))
                    // if we find the active task in our list, abort.
                    return;
                if (firstItem == null)
                    firstItem = item;
            }
        }
        // The currently active task doesn't appear in this task list.  Select
        // the first uncompleted task in our list as the new active task.
        if (firstItem != null)
            firstItem.doClick();
    }
    */

    public boolean selectNext() {
        String currentTask = menu.getActionCommand();
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            if (menu.getMenuComponent(i) instanceof TaskJMenuItem) {
                TaskJMenuItem item = (TaskJMenuItem) menu.getMenuComponent(i);
                if (!item.getPath().equals(currentTask)) {
                    item.doClick();
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized void reloadMenus() {
        Set subscriptionsToDelete = new HashSet(listeningToData);

        if (taskList != null) {
            TableModel table = taskList.getSimpleTableModel();
            List todoTasks = new ArrayList();
            List completedTasks = new ArrayList();
            for (int i = 0; i < table.getRowCount(); i++) {
                String path = (String) table.getValueAt(i,
                        EVTaskList.TASK_COLUMN);
                if (path == null || path.trim().length() == 0)
                    continue;
                if (table.getValueAt(i, EVTaskList.DATE_COMPLETE_COLUMN) == null)
                    todoTasks.add(path);
                else
                    completedTasks.add(path);

                String dataName = DataRepository.createDataName(path,
                        "Completed");
                subscriptionsToDelete.remove(dataName);
                if (listeningToData.contains(dataName) == false) {
                    context.getData().addDataListener(dataName, this, false);
                    listeningToData.add(dataName);
                }
            }

            int maxItemsPerMenu = Settings.getInt("hierarchyMenu.maxItems", 20);

            menu.removeAll();
            addMenuItems(menu, todoTasks, maxItemsPerMenu);

            if (!completedTasks.isEmpty()) {
                // this makes the most recently completed tasks the easiest to
                // navigate to, and the oldest tasks are buried deep in the menu.
                Collections.reverse(completedTasks);
                completedTasksMenu.removeAll();
                addMenuItems(completedTasksMenu, completedTasks,
                        maxItemsPerMenu);
                if (menu.getItem(0) instanceof TaskJMenuItem)
                    menu.insertSeparator(0);
                menu.insert(completedTasksMenu, 0);
            }

            if (menu.getMenuComponentCount() == 0) {
                JMenuItem emptyList = new JMenuItem();
                emptyList.setText(resources.format("Navigator.Empty_List_FMT",
                        taskList.getDisplayName()));
                emptyList.setEnabled(false);
                menu.add(emptyList);
            }
        }

        for (Iterator i = subscriptionsToDelete.iterator(); i.hasNext();) {
            String dataName = (String) i.next();
            context.getData().removeDataListener(dataName, this);
        }
    }

    private void addMenuItems(JMenu menu, List paths, int maxItemsPerMenu) {
        for (Iterator i = paths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            if (menu.getItemCount() + 1 >= maxItemsPerMenu) {
                JMenu moreMenu = new JMenu(Resources.getGlobalBundle()
                        .getDlgString("More"));
                menu.insert(moreMenu, 0);
                menu.insertSeparator(1);
                menu = moreMenu;
            }
            menu.add(new TaskJMenuItem(path));
        }
    }

    private class TaskJMenuItem extends JMenuItem {

        private String path;

        public TaskJMenuItem(String text) {
            super(TaskNavigationSelector.prettifyPath(text));
            this.path = text;
            setActionCommand(text);
            addActionListener(TaskListNavigator.this);
        }

        public String getPath() {
            return path;
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        syncTaskToModel();
    }

    private void syncTaskToModel() {
        String currentPath = activeTaskModel.getPath();
        if (currentPath != null && !currentPath.equals(menu.getActionCommand())) {
            menu.setActionCommand(currentPath);
            menu.setText(TaskNavigationSelector.prettifyPath(currentPath));
            Window window = SwingUtilities.getWindowAncestor(menu);
            if (window != null)
                window.pack();
        }
    }

    public void dataValueChanged(DataEvent e) throws RemoteException {
        if (taskList != null)
            taskList.recalc();
        doReloadMenus();
    }

    public void dataValuesChanged(Vector v) throws RemoteException {
        if (taskList != null)
            taskList.recalc();
        doReloadMenus();
    }

    private void doReloadMenus() {
        if (SwingUtilities.isEventDispatchThread())
            reloadMenus();
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    reloadMenus();
                }
            });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == EVTaskList.class && e.getActionCommand() != null
                && e.getActionCommand().equals(taskListName))
            reloadTaskList();
        else if (e.getActionCommand() != null)
            activeTaskModel.setPath(e.getActionCommand());
    }

    public static final Icon TASK_LIST_ICON = new ImageIcon(
            TaskListNavigator.class.getResource("listicon.png"));

}
