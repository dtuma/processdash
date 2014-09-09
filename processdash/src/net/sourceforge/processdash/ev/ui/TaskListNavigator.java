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

package net.sourceforge.processdash.ev.ui;


import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.TaskNavigationSelector.QuickSelectTaskProvider;
import net.sourceforge.processdash.ui.lib.AbstractTreeTableModel;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;
import net.sourceforge.processdash.ui.lib.NarrowJMenuItem;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.util.StringUtils;


public class TaskListNavigator implements TaskNavigationSelector.NavMenuUI,
        QuickSelectTaskProvider, ActionListener, DataListener {

    private JMenuBar menuBar;

    private DashboardContext context;

    private ActiveTaskModel activeTaskModel;

    private String taskListName;

    private String taskListPath;

    private String tooltipPrefix;

    private Set listeningToData;

    private JMenu overflowMenu;

    private ChooseTaskList truncatedPrefixMenu;

    Font plainFont, boldFont;

    private TaskListTopMenu menu;

    private JMenu completedTasksMenu;

    private TaskJMenuItem selectedItem;

    private List<TaskJMenuItem> allItems;

    private int numTodoItems;

    private EVTaskList taskList;

    private TreeTableModel quickTasks;

    private static Resources resources = Resources.getDashBundle("EV");

    private static Logger logger = Logger.getLogger(TaskListNavigator.class
            .getName());


    public TaskListNavigator(DashboardContext context, JMenuBar menuBar,
            ActionListener chooseTaskListAction, ActiveTaskModel activeTaskModel)
            throws IllegalArgumentException {
        this.menuBar = menuBar;
        this.context = context;
        this.activeTaskModel = activeTaskModel;

        this.taskListName = Settings.getVal(TASK_LIST_NAME_SETTING);
        this.taskListPath = Settings.getVal(TASK_LIST_PATH_SETTING);
        if (!StringUtils.hasValue(this.taskListPath))
            this.taskListPath = null;
        else
            this.tooltipPrefix = TaskNavigationSelector.prettifyPath( //
                    taskListPath.substring(1));

        this.listeningToData = new HashSet();
        this.allItems = new ArrayList<TaskJMenuItem>();

        createMenus(chooseTaskListAction);
        syncTaskToModel();

        new TaskListOpener().start();
    }

    private class TaskListOpener extends SwingWorker {

        public TaskListOpener() {
            menuBar.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        public Object construct() {
            if (taskListName == null || taskListName.trim().length() == 0)
                return resources.getString("Navigator.Errors.None_Selected");

            String displayName = EVTaskList.cleanupName(taskListName);

            for (int i = 5;   i-- > 0; ) {
                try {
                    EVTaskList tl = EVTaskList.openExisting(taskListName,
                            context.getData(), context.getHierarchy(),
                            context.getCache(), false);
                    if (tl == null)
                        return resources.format(
                                "Navigator.Errors.Not_Found_FMT", displayName);

                    if (!(tl instanceof EVTaskListData))
                        return resources.format("Navigator.Errors.Invalid_FMT",
                                displayName);

                    ((EVTaskListData) tl).recalcLeavesOnly();
                    tl.recalc();

                    return tl;
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "When opening task list, caught exception:", e);
                }
            }

            return resources.format("Navigator.Errors.Unexpected_FMT",
                    displayName);
        }

        public void finished() {
            menuBar.setCursor(null);

            if (get() instanceof EVTaskList)
                installTaskList((EVTaskList) get());
            else
                installMessage(String.valueOf(get()));
        }

    }


    public void hierarchyChanged() {
        reloadTaskList();
    }

    public void activeTaskChanged() {
        syncTaskToModel();
    }

    private void reloadTaskList() {
        new TaskListOpener().start();
    }

    public void delete() {
        installTaskList(null);
        ToolTipTimingCustomizer.INSTANCE.uninstall(menu);
    }

    public String getType() { return "taskList"; }
    public TreeTableModel getTaskSelectionChoices() { return quickTasks; }
    public String getPathForTreeNode(Object node) { return node.toString(); }
    public Object getTreeNodeForPath(String path) { return path; }

    private void createMenus(ActionListener chooseAction) {
        overflowMenu = menuBar.getMenu(0);
        String taskListMenuText = resources.format(
            "/ProcessDashboard:NavSelector.Task_List.Tooltip_FMT",
            EVTaskList.cleanupName(taskListName));
        overflowMenu.add(new ChooseTaskList(taskListMenuText, chooseAction), 0);
        truncatedPrefixMenu = new ChooseTaskList(null, chooseAction);
        overflowMenu.add(truncatedPrefixMenu, 0);

        boldFont = overflowMenu.getFont();
        plainFont = boldFont.deriveFont(Font.PLAIN);
        completedTasksMenu = new JMenu(resources
                .getString("Navigator.Completed_Items"));
        completedTasksMenu.setFont(plainFont);

        menu = new TaskListTopMenu();
        menu.setIconTextGap(1);
        ToolTipTimingCustomizer.INSTANCE.install(menu);
        menuBar.add(menu);
        menuBar.add(Box.createHorizontalStrut(4));
        menuBar.validate();
        menuBar.repaint();
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

        if (newTaskList != null)
            maybeSelectFirstTask();
    }

    private void maybeSelectFirstTask() {
        if (findTask(activeTaskModel.getPath()) == null)
            selectNext();
    }

    public boolean selectNext() {
        String currentTask = menu.getActionCommand();
        for (int i = 0; i < numTodoItems; i++) {
            TaskJMenuItem item = allItems.get(i);
            if (!item.getPath().equals(currentTask)) {
                item.doClick();
                return true;
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
                if (taskListPath != null && !Filter.pathMatches(path, taskListPath))
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

            quickTasks = new TaskListFlatTreeTableModel(todoTasks,
                completedTasks);

            int maxItemsPerMenu = Settings.getInt("hierarchyMenu.maxItems", 20);
            String currentPath = activeTaskModel.getPath();
            numTodoItems = todoTasks.size();

            menu.removeAll();
            allItems.clear();
            addMenuItems(menu, todoTasks, maxItemsPerMenu, currentPath);

            if (!completedTasks.isEmpty()) {
                // this makes the most recently completed tasks the easiest to
                // navigate to, and the oldest tasks are buried deep in the menu.
                Collections.reverse(completedTasks);
                completedTasksMenu.removeAll();
                addMenuItems(completedTasksMenu, completedTasks,
                        maxItemsPerMenu, currentPath);
                if (menu.getItemCount() > 0
                        && menu.getItem(0) instanceof TaskJMenuItem)
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

            // We only want to show the "Open Task & Schedule Window" menu item if there is
            // an active task
            if (StringUtils.hasValue(taskListName)) {
                JMenuItem openTaskAndSchedule = new JMenuItem();
                openTaskAndSchedule.setText(
                    resources.getString("Navigator.Open_Task_And_Schedule"));
                openTaskAndSchedule.setFont(plainFont);

                openTaskAndSchedule.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.hasValue(taskListName))
                            TaskScheduleChooser.open(context, taskListName);
                    }
                });

                menu.insertSeparator(menu.getItemCount());
                menu.add(openTaskAndSchedule);
            }
        }

        for (Iterator i = subscriptionsToDelete.iterator(); i.hasNext();) {
            String dataName = (String) i.next();
            context.getData().removeDataListener(dataName, this);
        }

    }

    private void addMenuItems(JMenu menu, List paths, int maxItemsPerMenu,
            String currentPath) {
        for (Iterator i = paths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            if (menu.getItemCount() + 1 >= maxItemsPerMenu) {
                JMenu moreMenu = new JMenu(Resources.getGlobalBundle()
                        .getDlgString("More"));
                moreMenu.setFont(plainFont);
                menu.insert(moreMenu, 0);
                menu.insertSeparator(1);
                menu = moreMenu;
            }
            TaskJMenuItem item = new TaskJMenuItem(path, currentPath);
            menu.add(item);
            allItems.add(item);
        }
    }

    private class ChooseTaskList extends JMenuItem {

        protected ChooseTaskList(String text, ActionListener a) {
            setText(text);
            addActionListener(a);
        }

        @Override
        public void setText(String text) {
            super.setText(text);
            setVisible(text != null && text.length() > 0);
        }
    }

    private class TaskJMenuItem extends NarrowJMenuItem {

        private String path;

        public TaskJMenuItem(String path, String current) {
            this.path = path;
            if (path.equals(current)) {
                setFont(boldFont);
                selectedItem = this;
            } else {
                setFont(plainFont);
            }
            setDisplayTextForPath(this, path);
            setActionCommand(path);
            addActionListener(TaskListNavigator.this);
        }

        public String getPath() {
            return path;
        }

        @Override
        protected String getTextToLayout(String menuText) {
            return reversePathSegments(menuText);
        }

        @Override
        protected String getTextToDisplay(String menuText, String fitLayoutText) {
            String[] parts = getFitPathSplit(menuText, fitLayoutText);
            setToolTipText(parts[0]);
            return parts[2];
        }

        public void setToolTipText(String text) {
            String toolTip = text;
            if (toolTip == null)
                toolTip = tooltipPrefix;
            else if (tooltipPrefix != null)
                toolTip = tooltipPrefix + " / " + text;
            if (toolTip != null)
                toolTip = toolTip + " / ...";
            super.setToolTipText(toolTip);
        }
    }

    private void setDisplayTextForPath(AbstractButton target, String path) {
        if (taskListPath != null && Filter.pathMatches(path, taskListPath)) {
            if (path.length() > taskListPath.length())
                path = path.substring(taskListPath.length()+1);
            else {
                // the path and the task path are equal!  Just use the final
                // part of the path in this case.
                int slashPos = path.lastIndexOf('/');
                path = path.substring(slashPos + 1);
            }
        }
        if (path.startsWith("/"))
            path = path.substring(1);
        target.setText(TaskNavigationSelector.prettifyPath(path));
    }

    private void syncTaskToModel() {
        String currentPath = activeTaskModel.getPath();
        if (currentPath != null && !currentPath.equals(menu.getActionCommand())) {
            menu.setActionCommand(currentPath);
            setDisplayTextForPath(menu, currentPath);
            Window window = SwingUtilities.getWindowAncestor(menu);
            if (window != null) {
                if (window instanceof ProcessDashboard)
                    ((ProcessDashboard)window).windowSizeRequirementsChanged();
                else
                    window.pack();
            }
        }

        if (selectedItem != null)
            selectedItem.setFont(plainFont);
        selectedItem = findTask(currentPath);
        if (selectedItem != null)
            selectedItem.setFont(boldFont);
    }

    private TaskJMenuItem findTask(String path) {
        for (TaskJMenuItem item : allItems) {
            if (item.getPath().equals(path))
                return item;
        }
        return null;
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

    public static boolean chooseTaskListNavigator(Component parent,
            DashboardContext context, Resources resources) {

        TaskListTreeModel model;
        try {
            model = new TaskListTreeModel(context);
        } catch (NoTaskListsFoundException ntlfe) {
            String title = resources.getString("Task_List.No_Lists.Title");
            String[] msg = resources
                    .getStrings("Task_List.No_Lists.Message");
            JOptionPane.showMessageDialog(parent, msg, title,
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        JTree taskLists = new JTree(model);
        taskLists.addTreeWillExpandListener(model);

        taskLists.setRootVisible(false);
        taskLists.setShowsRootHandles(true);
        taskLists.setToggleClickCount(4);
        taskLists.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        new JOptionPaneClickHandler().install(taskLists);

        JScrollPane sp = new JScrollPane(taskLists);
        sp.setPreferredSize(new Dimension(500, 300));
        String title = resources.getString("Task_List.Dialog.Title");
        Object message = new Object[] {
                resources.getString("Task_List.Dialog.Prompt"), sp,
                new JOptionPaneTweaker.MakeResizable() };
        if (JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            TreePath path = taskLists.getSelectionPath();
            if (path == null || path.getPathCount() < 2)
                return false;
            String taskListName = model.getValueFromNode(path
                    .getPathComponent(1));
            String fullPath = null;
            if (path.getPathCount() > 2)
                fullPath = model.getValueFromNode(path.getLastPathComponent());
            InternalSettings.set(TASK_LIST_NAME_SETTING, taskListName);
            InternalSettings.set(TASK_LIST_PATH_SETTING, fullPath);
            return true;
        }

        return false;
    }


    private static String reversePathSegments(String fullPath) {
        String[] parts = fullPath.split(" / ");
        StringBuilder result = new StringBuilder();
        for (int i = parts.length; i-- > 0;)
            result.append(" / ").append(parts[i]);
        return result.substring(3);
    }

    private static String[] getFitPathSplit(String fullPath, String fitText) {
        int len = fitText.length();
        if (fitText.endsWith(" /..."))
            fitText = fitText.substring(0, len - 4) + "...";
        else if (fitText.endsWith(" / ..."))
            fitText = fitText.substring(0, len - 5) + "...";

        int pos = fitText.lastIndexOf(" / ");
        if (pos == -1)
            return new String[] { fullPath, fitText, fitText };

        String truncPart = fitText.substring(pos + 3);
        String finalPart = fullPath.substring(fullPath.length() - pos);
        String initialPart = fullPath.substring(0, fullPath.length() - pos - 3);
        return new String[] { initialPart, truncPart, //
                truncPart + " / " + finalPart };
    }

    private static class NoTaskListsFoundException extends Exception {}


    private class TaskListTopMenu extends NarrowJMenu {

        private int truncatedPathWidth;

        @Override
        protected String getTextToLayout(String menuText) {
            truncatedPathWidth = 0;
            return reversePathSegments(menuText);
        }

        @Override
        protected String getTextToDisplay(String menuText, String fitLayoutText) {
            String[] parts = getFitPathSplit(menuText, fitLayoutText);
            setToolTipText(parts[0]);
            truncatedPathWidth = new JLabel(parts[1]).getPreferredSize().width + 3;
            return parts[2];
        }

        public void setToolTipText(String text) {
            String toolTip = text;
            if (text != null && tooltipPrefix != null)
                toolTip = tooltipPrefix + " / " + text;
            super.setToolTipText(toolTip);

            int pos = -1;
            if (text != null)
                pos = text.lastIndexOf(" / ");
            String overflowTip = (pos == -1 ? null : text.substring(0, pos));
            if (overflowTip == null)
                overflowTip = tooltipPrefix;
            else if (tooltipPrefix != null)
                overflowTip = tooltipPrefix + " / " + overflowTip;
            overflowMenu.setToolTipText(overflowTip);
            truncatedPrefixMenu.setText(overflowTip);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (event.getX() < truncatedPathWidth)
                return super.getToolTipText(event);
            else
                return null;
        }
    }


    private static class TaskListTreeModel extends DefaultTreeModel implements
            TreeWillExpandListener {

        private DashboardContext context;

        public TaskListTreeModel(DashboardContext context)
                throws NoTaskListsFoundException {
            super(new DefaultMutableTreeNode("root"));
            this.context = context;
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) getRoot();

            String[] taskListNames = EVTaskList.findTaskLists(
                    context.getData(), true, false);
            if (taskListNames == null || taskListNames.length == 0)
                throw new NoTaskListsFoundException();

            String[] displayNames = EVTaskList.getDisplayNames(taskListNames);
            for (int i = 0; i < displayNames.length; i++) {
                NodeObject obj = new NodeObject(displayNames[i],
                        taskListNames[i]);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(obj);
                node.add(new DefaultMutableTreeNode(PLACEHOLDER));
                root.add(node);
            }
        }

        public void treeWillCollapse(TreeExpansionEvent event)
                throws ExpandVetoException {}

        public void treeWillExpand(TreeExpansionEvent event)
                throws ExpandVetoException {
            TreePath path = event.getPath();
            if (path.getPathCount() != 2)
                return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
                    .getLastPathComponent();
            if (node.getChildCount() != 1)
                return;
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node
                    .getChildAt(0);
            if (PLACEHOLDER != child.getUserObject())
                return;

            node.removeAllChildren();
            NodeObject obj = (NodeObject) node.getUserObject();
            String taskListName = obj.getValue();
            EVTaskList tl = EVTaskList.open(taskListName, context.getData(),
                    context.getHierarchy(), context.getCache(), false);
            if (tl != null)
                copyNodes(node, (EVTask) tl.getRoot());
            nodeStructureChanged(node);
        }

        private void copyNodes(DefaultMutableTreeNode dest, EVTask src) {
            for (int i = 0; i < src.getNumChildren(); i++) {
                EVTask srcChild = src.getChild(i);
                NodeObject obj = new NodeObject(srcChild.getName(), srcChild
                        .getFullName());
                DefaultMutableTreeNode destChild = new DefaultMutableTreeNode(
                        obj);
                dest.add(destChild);
                copyNodes(destChild, srcChild);
            }
        }

        public String getValueFromNode(Object node) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            NodeObject obj = (NodeObject) treeNode.getUserObject();
            return obj.getValue();
        }

        private class NodeObject {
            private String display;

            private String value;

            public NodeObject(String display, String value) {
                this.display = display;
                this.value = value;
            }

            public String getValue() {
                return value;
            }

            public String toString() {
                return display;
            }
        }

        private static final String PLACEHOLDER = "placeholder";

    }

    private class TaskListFlatTreeTableModel extends AbstractTreeTableModel {

        private static final String ROOT_OBJECT = "ROOT";

        private List todoTasks;
        private List completedTasks;
        private String completed;

        public TaskListFlatTreeTableModel(List todoTasks,
                List completedTasks) {
            super(ROOT_OBJECT);
            this.todoTasks = todoTasks;
            this.completedTasks = completedTasks;
            this.completed = resources.getString("Navigator.Completed_Items");
        }

        public int getColumnCount() {
            return 1;
        }

        public Class getColumnClass(int column) {
            return TreeTableModel.class;
        }

        public String getColumnName(int column) {
            return resources.getString("Task");
        }

        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        public Object getValueAt(Object node, int column) {
            return node;
        }

        public Object getChild(Object parent, int index) {
            if (parent == ROOT_OBJECT) {
                if (index < todoTasks.size())
                    return todoTasks.get(index);
                else
                    return completed;
            } else if (parent == completed)
                return completedTasks.get(index);
            else
                return null;
        }

        public int getChildCount(Object parent) {
            if (parent == ROOT_OBJECT)
                return todoTasks.size() + (completedTasks.isEmpty() ? 0 : 1);
            else if (parent == completed)
                return completedTasks.size();
            else
                return 0;
        }

    }


    private static final String TASK_LIST_NAME_SETTING = "navigator.taskListName";

    private static final String TASK_LIST_PATH_SETTING = "navigator.taskListPath";

}
