// Copyright (C) 2013 Tuma Solutions, LLC
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

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.util.StringUtils;

public class TeamProjectBrowser extends JSplitPane {

    private DashboardContext ctx;

    private ActiveTaskModel taskModel;

    private PropTreeModel treeModel;

    private DashHierarchy treeProps;

    private JTree tree;

    private DefaultListModel scripts;

    private JList scriptList;

    private ExternalEventHandler handler;


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

        initSplitterLocationFromSettings();
        updateTreeSelectionFromActiveTask();
    }

    private void buildTree() {
        treeModel = new PropTreeModel(new DefaultMutableTreeNode("root"), null);
        tree = new JTree(treeModel);
        reloadHierarchy();

        tree.setEditable(false);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setExpandsSelectedPaths(true);
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(handler);

        setLeftComponent(new JScrollPane(tree));
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
        // TODO: if pruning produces an empty hierarchy, add something back.
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

        for (int i = p.getNumChildren(); i-- > 0;) {
            PropertyKey child = p.getChild(i);
            boolean shouldPrune = isProject || pruneForDisplay(hier, child);
            if (shouldPrune)
                p.removeChild(i);
        }

        return isProject == false && p.getNumChildren() == 0;
    }

    private void updateTreeSelectionFromActiveTask() {
        PropertyKey activeTask = taskModel.getNode();
        setSelectedTreeNode(activeTask);
    }

    private void setSelectedTreeNode(PropertyKey newNode) {
        PropertyKey nodeToSelect = treeProps.findClosestKey(newNode.path());
        PropertyKey currentlySelectedNode = getSelectedTreeNode();
        if (!nodeToSelect.equals(currentlySelectedNode)) {
            Object[] path = treeModel.getPathToKey(treeProps, nodeToSelect);
            handler.changeTreeSelection(path);
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
        scriptList = new JList(scripts);

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
        if (newScripts == null || newScripts.size() == 0)
            // TODO: display some default when there are no scripts
            return;

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
        id.display();

        if (clearSelection)
            scriptList.clearSelection();
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


    /**
     * Class that listens for events from other application sources, and takes
     * the appropriate action. Also assists with the synchronization between the
     * selected node in the tree and the currently active dashboard task.
     */
    private class ExternalEventHandler implements PropertyChangeListener,
            DashHierarchy.Listener, TreeSelectionListener,
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
                tree.setSelectionPath(new TreePath(path));
            }
            currentlyChangingTreeSelection = false;
        }

        /** Respond to an external change in the active task */
        public void propertyChange(PropertyChangeEvent evt) {
            if (!currentlyChangingActiveTask)
                updateTreeSelectionFromActiveTask();
        }

        public void hierarchyChanged(Event e) {
            reloadHierarchy();
            updateTreeSelectionFromActiveTask();
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
            if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand()))
                saveSplitterLocationToSettings();
        }

    }


    /** Listener to handle mouse clicks on the script list */
    private class ListClickHandler extends MouseAdapter implements Runnable {

        private MouseEvent mouseEvent;

        public void mouseClicked(MouseEvent e) {
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

}
