// Copyright (C) 2013-2016 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.BrokenDataFileHandler;
import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.UserGroupUtil;
import net.sourceforge.processdash.team.group.ui.GroupFilterMenu;
import net.sourceforge.processdash.team.setup.TeamStartBootstrap;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.VersionUtils;

public class TeamProjectBrowser extends JSplitPane {

    public interface Filter {

        boolean shouldShow(String projectRootPath);

    }

    private DashboardContext ctx;

    private ActiveTaskModel taskModel;

    private PropTreeModel treeModel;

    private DashHierarchy treeProps;

    private JTree tree;

    private JProgressBar filterReloadIndicator;

    private Filter filter;

    private DefaultListModel scripts;

    private ScriptList scriptList;

    private ExternalEventHandler handler;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.TeamProject");


    public TeamProjectBrowser(ProcessDashboard dash) {
        super(HORIZONTAL_SPLIT);
        this.ctx = dash;
        this.taskModel = dash.getActiveTaskModel();

        this.handler = new ExternalEventHandler();
        dash.addApplicationEventListener(handler);
        taskModel.addPropertyChangeListener(handler);
        ctx.getHierarchy().addHierarchyListener(handler);

        buildTree();
        buildScriptList();
        augmentTeamDashboardFileMenu(dash);

        initSplitterLocationFromSettings();
        updateTreeSelectionFromActiveTask();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        reloadHierarchy();
        updateTreeSelectionFromActiveTask();
        reloadScripts(getSelectedTreeNode());
    }

    private void buildTree() {
        treeModel = new PropTreeModel(new DefaultMutableTreeNode("root"), null);
        tree = new JTree(treeModel);
        reloadHierarchy();

        tree.setEditable(false);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setExpandsSelectedPaths(true);
        tree.setCellRenderer(new ProjectTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(handler);

        filterReloadIndicator = new JProgressBar();
        filterReloadIndicator.setIndeterminate(true);
        filterReloadIndicator.setVisible(false);
        ToolTipTimingCustomizer.INSTANCE.install(filterReloadIndicator);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        leftPanel.add(filterReloadIndicator, BorderLayout.SOUTH);

        setLeftComponent(leftPanel);
    }

    private void reloadHierarchy() {
        treeProps = getHierarchyToDisplay();
        treeModel.reload(treeProps);
        int row = 0;
        while (row < tree.getRowCount())
            tree.expandRow(row++);
    }

    private DashHierarchy getHierarchyToDisplay() {
        DashHierarchy result = new DashHierarchy("");
        result.copy(ctx.getHierarchy());
        pruneForDisplay(result, PropertyKey.ROOT);
        maybeCreateDefaultHierarchy(result);
        return result;
    }

    /**
     * Recursively prune items so the leaves of the tree are all team project
     * roots.
     * 
     * @param hier
     *            the hierarchy to prune
     * @param node
     *            one node to examine
     * @return true if this node should be pruned from the tree, false otherwise
     */
    private boolean pruneForDisplay(DashHierarchy hier, PropertyKey node) {
        Prop p = hier.pget(node);
        String templateId = p.getID();
        boolean isProject = StringUtils.hasValue(templateId);

        if (isProject && filter != null && !filter.shouldShow(node.path())) {
            return true;
        }

        for (int i = p.getNumChildren(); i-- > 0;) {
            PropertyKey child = p.getChild(i);
            boolean shouldPrune = isProject || pruneForDisplay(hier, child);
            if (shouldPrune)
                p.removeChild(i);
        }

        return isProject == false && p.getNumChildren() == 0;
    }

    private void maybeCreateDefaultHierarchy(DashHierarchy hier) {
        if (hier.getNumChildren(PropertyKey.ROOT) == 0) {
            PropertyKey projectNode = addHierChild(hier, PropertyKey.ROOT,
                resources.getString("Project"));
            PropertyKey noneFound = addHierChild(hier, projectNode,
                resources.getString("None_Found"));
            getOrCreateProp(hier, noneFound).setID(NO_PROJECT_TEMPLATE_ID);
        }
    }

    private PropertyKey addHierChild(DashHierarchy hier, PropertyKey parent,
            String childName) {
        PropertyKey child = new PropertyKey(parent, childName);
        getOrCreateProp(hier, parent).addChild(child, 0);
        return child;
    }

    private Prop getOrCreateProp(DashHierarchy hier, PropertyKey key) {
        Prop prop = hier.pget(key);
        hier.put(key, prop);
        return prop;
    }


    private void updateTreeSelectionFromActiveTask() {
        PropertyKey activeTask = taskModel.getNode();
        setSelectedTreeNode(activeTask);
    }

    private void setSelectedTreeNode(PropertyKey newNode) {
        PropertyKey nodeToSelect = treeProps.findExistingKey(newNode.path());
        if (nodeToSelect == null)
            nodeToSelect = getBestNodeToSelect(newNode);
        PropertyKey currentlySelectedNode = getSelectedTreeNode();
        if (!nodeToSelect.equals(currentlySelectedNode)) {
            Object[] path = treeModel.getPathToKey(treeProps, nodeToSelect);
            handler.changeTreeSelection(path);
        }
    }

    private PropertyKey getBestNodeToSelect(PropertyKey newNode) {
        // if the active task has been filtered out of the tree, find the
        // closest parent of that target task.
        PropertyKey node = treeProps.findClosestKey(newNode.path());
        while (true) {
            // next, start walking down the tree. If we find a child that is
            // a leaf, return it. If not, look at its first child and try again.
            Prop prop = treeProps.pget(node);
            if (prop.getNumChildren() == 0)
                return node;
            else
                node = prop.getChild(0);
        }
    }

    private PropertyKey getSelectedTreeNode() {
        PropertyKey selectedNode = null;

        TreePath tp = tree.getSelectionPath();
        if (tp != null) {
            Object[] path = tp.getPath();
            selectedNode = treeModel.getPropKey(treeProps, path);
        }

        if (selectedNode == null)
            selectedNode = PropertyKey.ROOT;
        return selectedNode;
    }



    private void buildScriptList() {
        scripts = new DefaultListModel();
        scriptList = new ScriptList(scripts);

        scriptList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scriptList.addMouseListener(new ListClickHandler());
        scriptList.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showScript");
        scriptList.getActionMap().put("showScript", new ListEnterAction());

        setRightComponent(new JScrollPane(scriptList));
    }



    /**
     * Reload the list of scripts based on the currently active task.
     */
    private void reloadScripts(PropertyKey key) {
        scripts.clear();

        List<ScriptID> newScripts = null;
        if (key != null)
            newScripts = ScriptEnumerator.getScripts(ctx, key);
        if (newScripts == null || newScripts.size() == 0) {
            // if no scripts were found, create an appropriate default entry.
            newScripts = new ArrayList<ScriptID>();
            ScriptID defaultScript = createDefaultScript(key);
            newScripts.add(defaultScript);
            newScripts.add(defaultScript);
        }

        ScriptID defaultScript = newScripts.get(0);
        String dataPath = defaultScript.getDataPath();
        for (int i = 1; i < newScripts.size(); i++) {
            ScriptID script = newScripts.get(i);
            if (dataPath != null && !dataPath.equals(script.getDataPath()))
                break;
            scripts.addElement(script);
            dataPath = script.getDataPath();
            if (defaultScript.scriptEquals(script))
                scriptList.getSelectionModel().setLeadSelectionIndex(i - 1);
        }

        if (Settings.getBool("userPref.teamScripts.useWipe", true))
            scriptList.showScriptsWithWipe();
    }

    private ScriptID createDefaultScript(PropertyKey key) {
        if (isBrokenTeamProject(key.path())) {
            String templateID = ctx.getHierarchy().pget(key).getID();
            int slashPos = templateID.indexOf('/');
            String pid = (slashPos == -1 ? "" : templateID.substring(0, slashPos));
            return new ScriptID(BrokenDataFileHandler.SHARE_MCF_URL, "",
                    resources.format("Missing_MCF.Title_FMT", pid));
        } else {
            if (filter != null) {
                String templateID = treeProps.getID(key);
                if (NO_PROJECT_TEMPLATE_ID.equals(templateID))
                    return new ScriptID(SELECT_GROUP_FILTER_URI, "ignored",
                            resources.getString("None_Matching.Title"));
            }
            return new ScriptID(getNewProjectCreationUri(), "",
                    resources.getString("New.Title"));
        }
    }

    private boolean isBrokenTeamProject(String path) {
        if (ctx instanceof ProcessDashboard) {
            ProcessDashboard dash = (ProcessDashboard) ctx;
            return dash.getBrokenDataPaths().contains(path);
        } else {
            return false;
        }
    }

    /**
     * Display the script that is currently selected
     * 
     * @param clearSelection
     *            if true, clear the selection after displaying the script
     * @param evt
     *            the mouse click event that triggered this action; can be null
     */
    private void showSelectedScript(boolean clearSelection, MouseEvent evt) {
        // find out which item is currently selected
        int selectedIndex = scriptList.getMinSelectionIndex();
        if (selectedIndex == -1)
            return;

        // if this action was triggered by a mouse click, double-check to
        // make certain that the click occurred on the selected item, and not
        // somewhere within the empty space of the JList.
        if (evt != null && evt.getPoint() != null) {
            Rectangle selectedCellBounds = scriptList.getCellBounds(
                selectedIndex, selectedIndex);
            if (selectedCellBounds != null
                    && !selectedCellBounds.contains(evt.getPoint()))
                return;
        }

        ScriptID id = (ScriptID) scripts.elementAt(selectedIndex);
        UserFilter f = UserGroupManager.getInstance().getGlobalFilter();
        UserGroupManager.getInstance().setLocalFilter(id.getDataPath(), f);
        id.display();

        if (clearSelection)
            scriptList.clearSelection();
    }



    /**
     * Add team-project-related items to the File menu.
     */
    private void augmentTeamDashboardFileMenu(ProcessDashboard dash) {
        JMenuBar menuBar = dash.getConfigurationMenus();

        if (Settings.isReadWrite()) {
            JMenu fileMenu = menuBar.getMenu(0);
            fileMenu.insert(new NewProjectAction(), 0);
            fileMenu.insert(new AlterTeamProjectMenu(), 1);
        }

        if (UserGroupManager.getInstance().isEnabled()) {
            GroupFilterMenu groupFilterMenu = new GroupFilterMenu(
                    UserGroup.EVERYONE);
            groupFilterMenu.addChangeListener(handler);

            menuBar.add(Box.createHorizontalGlue());
            menuBar.add(groupFilterMenu);
        }
    }


    private String getNewProjectCreationUri() {
        String uri = TeamStartBootstrap.TEAM_START_URI;

        String targetParent = getNewProjectTargetParent();
        if (StringUtils.hasValue(targetParent))
            uri = HTMLUtils.appendQuery(uri,
                TeamStartBootstrap.TARGET_PARENT_PARAM, targetParent);

        return uri;
    }

    private String getNewProjectTargetParent() {
        PropertyKey node = getSelectedTreeNode();

        // When no tree node is selected, the root node is reported as the
        // "effective selection." In this case, return null to indicate that
        // we don't have a target parent.
        if (PropertyKey.ROOT.equals(node))
            return null;

        // Find the first at or above the selection which has no template ID.
        // that is our target parent.
        while (node != null) {
            String templateID = treeProps.getID(node);
            if (StringUtils.hasValue(templateID))
                node = node.getParent();
            else if (PropertyKey.ROOT.equals(node))
                return "/";
            else
                return node.path();
        }
        return null;
    }


    private void maybeRenameSelectedProject() {
        // determine which team project is selected. If none, abort.
        PropertyKey projectNode = getSelectedTreeNode();
        String projectPath = projectNode.path();
        String templateID = ctx.getHierarchy().getID(projectNode);
        if (!StringUtils.hasValue(templateID))
            return;

        // Create objects we will use in a dialog
        String title = resources.getString("Rename.Title");
        JTextField newPathField = new JTextField(projectPath);
        Object message = resources.formatStrings("Rename.Message_Header_FMT",
            projectPath);
        message = new Object[] { message, " ", newPathField,
                new JOptionPaneTweaker.GrabFocus(newPathField) };

        while (true) {
            // Show the user a dialog asking for the new path and name.
            int userChoice = JOptionPane.showConfirmDialog(this, message,
                title, JOptionPane.OK_CANCEL_OPTION);

            // if the user didn't press the OK button, abort.
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            // get the new path in canonical form. If it was not absolute,
            // interpret it relative to the current parent of the project.
            String newPath = newPathField.getText().trim();
            if (newPath.indexOf('/') == -1)
                newPath = projectNode.getParent().path() + "/" + newPath;
            newPath = DashHierarchy.scrubPath(newPath);
            newPathField.setText(newPath);

            // if the user didn't change the name, abort.
            if (newPath.length() < 2 || newPath.equals(projectPath))
                return;

            // check for various error conditions.
            if (projectAlreadyExists(newPath)) {
                showInvalidRenameMessage("Rename.Duplicate_Name");

            } else if (projectParentIsInvalid(newPath)) {
                showInvalidRenameMessage("Rename.Invalid_Parent");

            } else {
                try {
                    // try performing the renaming operation.
                    HierarchyAlterer hierarchyAlterer = DashController
                            .getHierarchyAlterer();
                    hierarchyAlterer.renameNode(projectPath, newPath);

                    // if this caused the old parent of the project to become
                    // childless, delete that parent (and grandparent, etc)
                    PropertyKey oldParent = projectNode.getParent();
                    while (ctx.getHierarchy().getNumChildren(oldParent) == 0) {
                        hierarchyAlterer.deleteNode(oldParent.path());
                        oldParent = oldParent.getParent();
                    }

                    // point the "active task" at the renamed project.
                    PropertyKey newNode = ctx.getHierarchy().findExistingKey(
                        newPath);
                    if (newNode != null)
                        taskModel.setNode(newNode);

                } catch (HierarchyAlterationException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private boolean projectAlreadyExists(String path) {
        PropertyKey key = ctx.getHierarchy().findExistingKey(path);
        return (key != null);
    }

    private boolean projectParentIsInvalid(String path) {
        PropertyKey key = ctx.getHierarchy().findClosestKey(path);
        while (key != null) {
            String templateID = ctx.getHierarchy().getID(key);
            if (StringUtils.hasValue(templateID))
                return true;
            key = key.getParent();
        }
        return false;
    }

    private void showInvalidRenameMessage(String resKey) {
        JOptionPane.showMessageDialog(this, resources.getStrings(resKey),
            resources.getString("Rename.Error_Title"),
            JOptionPane.ERROR_MESSAGE);
    }



    private void maybeDeleteSelectedProject() {
        PropertyKey selectedNode = getSelectedTreeNode();
        String projectPath = selectedNode.path();
        String templateID = ctx.getHierarchy().getID(selectedNode);
        if (okToDeleteProject(projectPath, templateID)) {
            try {
                DashController.getHierarchyAlterer().deleteNode(projectPath);
            } catch (HierarchyAlterationException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean okToDeleteProject(String projectPath, String templateID) {
        // do not delete plain nodes (which act as folders full of projects)
        if (!StringUtils.hasValue(templateID))
            return false;

        // it is harmless to delete a team project stub.
        if (TeamStartBootstrap.TEAM_STUB_ID.equals(templateID))
            return true;

        // the user is deleting a real project. Display a dialog to confirm.
        String title = resources.getString("Delete.Title");
        Object message = getDeleteProjectWarningMessage(projectPath, templateID);
        int userChoice = JOptionPane.showConfirmDialog(this, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return userChoice == JOptionPane.YES_OPTION;
    }

    private Object getDeleteProjectWarningMessage(String projectPath,
            String templateID) {
        List message = new ArrayList();

        // add a header to the message.
        message.add(resources.formatStrings("Delete.Message_Header_FMT",
            projectPath));
        message.add(" ");

        // if WBS planning has been done, add a warning.
        if (hasWbsData(projectPath)) {
            message.add(resources.getStrings("Delete.WBS_Data_Warning"));
            message.add(" ");
        }

        // if team members have joined the project, add a warning.
        if (membersHaveJoined(projectPath, templateID)) {
            message.add(resources.getStrings("Delete.Members_Joined"));
            message.add(" ");
        }

        // add a footer to the message.
        message.add(resources.getString("Delete.Message_Footer"));
        return message.toArray();
    }

    private boolean hasWbsData(String projectPath) {
        return listLongerThan(projectPath, "Synchronized_Task_ID_WBS_Order", 2);
    }

    private boolean membersHaveJoined(String projectPath, String templateID) {
        return (templateID.endsWith("/TeamRoot") && listLongerThan(projectPath,
            "Corresponding_Project_Nodes", 0));
    }

    private boolean listLongerThan(String projectPath, String listName,
            int length) {
        String name = projectPath + "/" + listName;
        ListData l = ListData.asListData(ctx.getData().getSimpleValue(name));
        return l != null && l.size() > length;
    }


    private static final String SPLITTER_SETTING = "mainWindow.splitterPos";

    private void initSplitterLocationFromSettings() {
        int location = Settings.getInt(SPLITTER_SETTING, 200);
        setDividerLocation(location);
    }

    private void saveSplitterLocationToSettings() {
        int location = getDividerLocation();
        InternalSettings.set(SPLITTER_SETTING, Integer.toString(location));
    }

    private static final String ACTIVE_TASK_SETTING = "userPref.dataset.activeTask";

    private void initActiveTaskFromPreferences() {
        String activeTask = Settings.getVal(ACTIVE_TASK_SETTING);
        PropertyKey key = ctx.getHierarchy().findExistingKey(activeTask);
        if (key != null && ctx.getHierarchy().getNumChildren(key) == 0)
            taskModel.setNode(key);
    }

    private void saveActiveTaskPreference() {
        String activeTask = taskModel.getPath();
        InternalSettings.set(ACTIVE_TASK_SETTING, activeTask);
    }


    public Filter buildGroupFilter(UserFilter filter) {
        Set<String> projectIDsToInclude = UserGroupUtil.getProjectIDsForFilter(
            filter, ctx);
        if (projectIDsToInclude == null)
            return null;
        else
            return new TeamProjectGroupFilter(projectIDsToInclude);
    }


    private class ProjectTreeCellRenderer extends DefaultTreeCellRenderer {

        public ProjectTreeCellRenderer() {
            setLeafIcon(DashboardIconFactory.getProjectIcon());
        }

    }


    /**
     * Class that listens for events from other application sources, and takes
     * the appropriate action. Also assists with the synchronization between the
     * selected node in the tree and the currently active dashboard task.
     */
    private class ExternalEventHandler implements PropertyChangeListener,
            DashHierarchy.Listener, TreeSelectionListener, ChangeListener,
            ApplicationEventListener {

        boolean currentlyChangingActiveTask, currentlyChangingTreeSelection;

        public void changeActiveTask(PropertyKey newTask) {
            currentlyChangingActiveTask = true;
            taskModel.setNode(newTask);
            currentlyChangingActiveTask = false;
        }

        public void changeTreeSelection(Object[] path) {
            currentlyChangingTreeSelection = true;
            if (path == null || path.length < 2) {
                tree.clearSelection();
            } else {
                TreePath treePath = new TreePath(path);
                tree.setSelectionPath(treePath);
                tree.scrollPathToVisible(treePath);
            }
            currentlyChangingTreeSelection = false;
        }

        /** Respond to an external change in the active task */
        public void propertyChange(PropertyChangeEvent evt) {
            if (!currentlyChangingActiveTask)
                updateTreeSelectionFromActiveTask();
        }

        public void hierarchyChanged(Event e) {
            // reload the contents of our tree based on the new hierarchy.
            reloadHierarchy();
            // tell the active task model to recalculate the current task;
            // then make sure our tree is synced to it.
            taskModel.setNode(PropertyKey.ROOT);
            updateTreeSelectionFromActiveTask();
            // reload the scripts for the current task in case they changed.
            reloadScripts(getSelectedTreeNode());
        }

        /** Respond to a change in the selected tree node */
        public void valueChanged(TreeSelectionEvent e) {
            PropertyKey selectedNode = getSelectedTreeNode();
            if (!currentlyChangingTreeSelection)
                changeActiveTask(selectedNode);
            reloadScripts(selectedNode);
        }

        /** Respond to a "Save All" request from the dashboard */
        public void handleApplicationEvent(ActionEvent e) {
            if (APP_EVENT_STARTED.equals(e.getActionCommand())) {
                initActiveTaskFromPreferences();
            } else if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
                saveSplitterLocationToSettings();
                saveActiveTaskPreference();
            }
        }

        /** respond to a change in the global group filter */
        public void stateChanged(ChangeEvent e) {
            GroupFilterMenu menu = (GroupFilterMenu) e.getSource();
            UserFilter selection = menu.getSelectedItem();
            if (selection != null) {
                UserGroupManager.getInstance().setGlobalFilter(selection);
                new GroupFilterLoader(selection).execute();
            }
        }

    }


    /** component to display the list of scripts */
    private class ScriptList extends JList implements ActionListener {

        private Timer wipeTimer;

        private int wipeWidth;

        private Rectangle rect;

        protected ScriptList(ListModel dataModel) {
            super(dataModel);
            wipeTimer = new Timer(10, this);
            wipeWidth = Integer.MAX_VALUE;
            rect = new Rectangle();
        }

        public void showScriptsWithWipe() {
            wipeWidth = 0;
            repaint();
            wipeTimer.restart();
        }

        public void actionPerformed(ActionEvent e) {
            getBounds(rect);
            wipeWidth = (int) (wipeWidth * 1.2 + 5);
            if (wipeWidth > rect.width) {
                wipeWidth = Integer.MAX_VALUE;
                wipeTimer.stop();
            }
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            getBounds(rect);
            if (wipeWidth > rect.width) {
                super.paint(g);
            } else {
                g.setColor(getBackground());
                g.fillRect(rect.x, rect.y, rect.width, rect.height);

                Shape originalClip = g.getClip();
                rect.width = wipeWidth;
                g.setClip(rect);
                super.paint(g);
                g.setClip(originalClip);
            }
        }
    }


    /** Listener to handle mouse clicks on the script list */
    private class ListClickHandler extends MouseAdapter implements Runnable {

        private MouseEvent mouseEvent;

        public void mouseReleased(MouseEvent e) {
            // First, allow the current mouse event to be delivered to ALL
            // listeners. (This will ensure that the selection gets updated
            // appropriately.) AFTER that has occurred, run a task to display
            // the currently selected script.
            this.mouseEvent = e;
            SwingUtilities.invokeLater(this);
        }

        public void run() {
            showSelectedScript(true, mouseEvent);
            mouseEvent = null;
        }

    }


    /** Action to perform when a user presses Enter on the script list */
    private class ListEnterAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            showSelectedScript(false, null);
        }

    }

    private class NewProjectAction extends AbstractAction {

        public NewProjectAction() {
            super(resources.getString("Menu.File.New_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            Browser.launch(getNewProjectCreationUri());
        }

    }

    private class AlterTeamProjectMenu extends JMenu implements
            TreeSelectionListener {

        private RelaunchProjectAction relaunchAction;

        public AlterTeamProjectMenu() {
            super(resources.getString("Menu.File.Alter_Team_Project"));
            setEnabled(false);
            tree.getSelectionModel().addTreeSelectionListener(this);

            add(new RenameProjectAction());
            add(new MoveProjectUpAction());
            add(new MoveProjectDownAction());
            relaunchAction = new RelaunchProjectAction();
            if (relaunchAction.isSupported())
                add(relaunchAction);
            add(new DeleteProjectAction());
        }

        public void valueChanged(TreeSelectionEvent e) {
            PropertyKey projectKey = getSelectedTreeNode();
            String templateID = ctx.getHierarchy().getID(projectKey);
            setEnabled(StringUtils.hasValue(templateID));
            relaunchAction.checkEnabledForProcess(templateID);
        }

    }

    private class RenameProjectAction extends AbstractAction {

        public RenameProjectAction() {
            super(resources.getString("Menu.File.Rename_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            maybeRenameSelectedProject();
        }

    }

    private class DeleteProjectAction extends AbstractAction {

        public DeleteProjectAction() {
            super(resources.getString("Menu.File.Delete_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            maybeDeleteSelectedProject();
        }

    }

    private class MoveProjectUpAction extends AbstractAction implements
            TreeSelectionListener {

        private int nodeOffset;

        private Prop nodeParent;

        private int childPosToMoveUp;

        public MoveProjectUpAction() {
            this("Move_Team_Project_Up", 0);
        }

        public MoveProjectUpAction(String resKey, int nodeOffset) {
            super(resources.getString("Menu.File." + resKey));
            this.nodeOffset = nodeOffset;
            tree.getSelectionModel().addTreeSelectionListener(this);
        }

        public void valueChanged(TreeSelectionEvent e) {
            findNodeToMoveUp();
            setEnabled(nodeParent != null);
        }

        private void findNodeToMoveUp() {
            nodeParent = null;

            DashHierarchy hier = ctx.getHierarchy();
            PropertyKey selectedNode = getSelectedTreeNode();
            if (!StringUtils.hasValue(hier.getID(selectedNode)))
                return;

            Prop parent = hier.pget(selectedNode.getParent());
            int pos = parent.getChildPos(selectedNode) + nodeOffset;
            if (pos > 0 && pos < parent.getNumChildren()) {
                nodeParent = parent;
                childPosToMoveUp = pos;
            }
        }

        public void actionPerformed(ActionEvent e) {
            findNodeToMoveUp();
            if (nodeParent != null) {
                nodeParent.moveChildUp(childPosToMoveUp);
                valueChanged(null);
                ctx.getHierarchy().fireHierarchyChanged();
            }
        }

    }

    private class MoveProjectDownAction extends MoveProjectUpAction {

        public MoveProjectDownAction() {
            super("Move_Team_Project_Down", 1);
        }

    }

    private class RelaunchProjectAction extends AbstractAction {

        public RelaunchProjectAction() {
            super(resources.getString("Menu.File.Relaunch_Team_Project"));
        }

        public boolean isSupported() {
            return isRelaunchSupported("teamTools");
        }

        public void checkEnabledForProcess(String templateID) {
            setEnabled(templateID != null && templateID.endsWith("/TeamRoot"));
        }

        public void actionPerformed(ActionEvent e) {
            PropertyKey projectKey = getSelectedTreeNode();
            String templateID = ctx.getHierarchy().getID(projectKey);
            checkEnabledForProcess(templateID);
            if (!isEnabled())
                return;

            StringBuilder uri = new StringBuilder();
            uri.append(HTMLUtils.urlEncodePath(projectKey.path()));

            int slashPos = templateID.indexOf('/');
            String processID = templateID.substring(0, slashPos);
            if (isRelaunchSupported(processID))
                uri.append("//").append(processID)
                        .append("/setup/wizard.class?page=relaunch");
            else
                uri.append("//dash/relaunchUpgradeMCF.shtm");

            Browser.launch(uri.toString());
        }

        private boolean isRelaunchSupported(String packageID) {
            String version = TemplateLoader.getPackageVersion(packageID);
            return (version != null && VersionUtils.compareVersions(version,
                "4.1") >= 0);
        }

    }

    private GroupFilterLoader currentGroupFilterLoader;

    private class GroupFilterLoader extends SwingWorker<Filter, Object>
            implements ActionListener {

        private UserFilter userFilter;

        public GroupFilterLoader(UserFilter userFilter) {
            this.userFilter = userFilter;
            currentGroupFilterLoader = this;
            filterReloadIndicator.setToolTipText(resources.format(
                "Filter_Wait_Tooltip_FMT", userFilter.toString()));

            // most filter calculations will occur very quickly (the exception
            // being the first calculation in a large team dashboard, which
            // must wait for all projects to load). If the calc takes longer
            // than 50ms, display a progress bar.
            Timer t = new Timer(50, this);
            t.setRepeats(false);
            t.start();
        }

        @Override
        protected Filter doInBackground() throws Exception {
            return buildGroupFilter(userFilter);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentGroupFilterLoader == this)
                filterReloadIndicator.setVisible(true);
        }

        @Override
        protected void done() {
            if (currentGroupFilterLoader == this) {
                try {
                    currentGroupFilterLoader = null;
                    filterReloadIndicator.setToolTipText(null);
                    filterReloadIndicator.setVisible(false);
                    setFilter(get());
                } catch (Exception e) {
                }
            }
        }

    }

    private class TeamProjectGroupFilter implements Filter {

        private Set<String> projectIDs;

        TeamProjectGroupFilter(Set<String> projectIDs) {
            this.projectIDs = projectIDs;
        }

        @Override
        public boolean shouldShow(String projectRootPath) {
            // return true if this project's ID is in the list
            String dataName = DataRepository.createDataName(projectRootPath,
                TeamDataConstants.PROJECT_ID);
            SimpleData sd = ctx.getData().getSimpleValue(dataName);
            if (sd != null && projectIDs.contains(sd.format()))
                return true;

            // if this is a master project, return true if any of its
            // subprojects are in the list
            dataName = DataRepository.createDataName(projectRootPath,
                "Subproject_WBS_ID_List");
            ListData l = ListData.asListData(ctx.getData().getValue(dataName));
            if (l != null) {
                for (int i = l.size(); i-- > 0;) {
                    String oneID = l.get(i).toString();
                    if (projectIDs.contains(oneID))
                        return true;
                }
            }

            // this project does not appear to be in the list.
            return false;
        }

    }


    private static final String NO_PROJECT_TEMPLATE_ID = "Nonexistent Project";

    private static final String SELECT_GROUP_FILTER_URI = "/control/showGroupFilterSelector?trigger";

}
