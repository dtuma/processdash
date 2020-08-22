// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

import static teamdash.wbs.WorkflowUtil.WORKFLOW_SOURCE_IDS_ATTR;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.CheckboxTree;
import net.sourceforge.processdash.ui.lib.CheckboxTree.CheckboxNodeRenderer;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.lib.JOptionPaneActionHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.ZoomManager;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.ActionCategoryComparator;
import teamdash.hist.BlameData;
import teamdash.hist.BlameModelData;
import teamdash.hist.BlameModelDataListener;
import teamdash.hist.BlameNodeData;
import teamdash.hist.ui.BlameAnnotationBorder;
import teamdash.hist.ui.BlameComponentRepainter;
import teamdash.hist.ui.BlameSelectionListener;
import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;


/** Displays the nodes of a WBSModel in a JTable, and installs custom
 * editing functionality.
 */
public class WBSJTable extends JTable {

    /** The wbs model to display */
    WBSModel wbsModel;
    /** An associated data model, if applicable */
    DataTableModel dataModel;
    /** Our custom cell renderer */
    WBSNodeRenderer renderer;
    /** Our custom cell editor */
    WBSNodeEditor editor;
    /** The map translating node types to corresponding icons */
    Map iconMap;
    /** Information about project workflows */
    WorkflowWBSModel workflows;
    /** Our source for task identification information */
    TaskIDSource taskIDSource;
    /** A collection of blame data for drawing annotations */
    BlameModelData blameData;
    /** An object for updating the blame caret position */
    BlameSelectionListener blameSelectionListener;

    /** a list of keystroke/action mappings to install */
    private Set customActions = new HashSet();
    /** a scrollable JComponent that we can delegate our scrolling events to */
    private JComponent scrollableDelegate = null;
    /** The list of nodes that has been cut */
    private List cutList = null;
    /** A safe default task type */
    String safeTaskType = "Planning Task";

    /** Should editing of WBS nodes be disabled? */
    private boolean disableEditing = false;
    /** Is indentation disabled for this WBS? */
    private boolean disableIndentation = false;
    /** Should the enter key insert a new line into the WBS? */
    private ButtonModel enterInsertsLine = new DefaultButtonModel();
    /** The initials of a team member for whom editing operations should
     *  be optimized */
    private String optimizeForIndiv = null;

    private static final Resources resources = WBSEditor.resources;


    /** Create a JTable to display a WBS. Construct a default icon menu. */
    public WBSJTable(WBSModel model, Map iconMap) {
        this(model, iconMap, null, null, null);
    }
    /** Create a JTable to display a WBS */
    public WBSJTable(WBSModel model, Map iconMap, JMenu iconMenu) {
        this(model, iconMap, iconMenu, null, null);
    }
    /** Create a JTable to display a WBS */
    public WBSJTable(WBSModel model, Map iconMap, JMenu iconMenu,
            WorkflowWBSModel workflows, TaskIDSource idSource) {
        super(model);
        wbsModel = model;
        this.iconMap = iconMap;
        this.workflows = workflows;
        taskIDSource = idSource;

        if (getRowHeight() < 1)
            setRowHeight(19);
        else
            setRowHeight(getRowHeight() + 3);
        buildCustomActionMaps();

        WBSZoom.get().manage(this, "font", "rowHeight");
        WBSZoom.get().manage(getTableHeader(), "font");

        renderer = new WBSNodeRenderer(model, iconMap, workflows);
        setDefaultRenderer(WBSNode.class, renderer);
        setForeground(Color.black);
        setBackground(Color.white);
        setSelectionForeground(Color.black);
        setSelectionBackground(new Color(0xb8cfe5));

        editor = new WBSNodeEditor(this, model, iconMap, iconMenu, workflows);
        setDefaultEditor(WBSNode.class, editor);
        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        installCustomActions(this);
        installTableActions();
        getSelectionModel().addListSelectionListener(SELECTION_LISTENER);
        addMouseMotionListener(MOTION_LISTENER);
        MacGUIUtils.tweakTable(this);
    }
    /** Create a WBSJTable to display a WBS along with additional data columns,
     * as provided by a data table model. */
    public WBSJTable(DataTableModel model, Map iconMap, JMenu iconMenu) {
        // create the table normally
        this(model.getWBSModel(), iconMap, iconMenu);
        // change the table model, so we will display additional columns
        setModel(dataModel = model);
        // the WBS node should always stay in column 0. Enforce this by
        // disabling the reordering of table columns
        getTableHeader().setReorderingAllowed(false);
        // arrange for WBS node editing to begin when the user presses the
        // left arrow key while on column 0
        new MoveLeftOrStartEditing().install();
    }


    public void setEditingEnabled(boolean enable) {
        disableEditing = !enable;
        editor.setEditingEnabled(enable);
        recalculateEnablement();
    }

    public boolean isEditingEnabled() {
        return !disableEditing;
    }

    public void setIndentationDisabled(boolean disabled) {
        disableIndentation = disabled;
        recalculateEnablement();
    }

    public void loadGuiPrefs(GuiPrefs prefs) {
        prefs.load("insertOnEnter", enterInsertsLine);
    }

    public String getOptimizeForIndiv() {
        return optimizeForIndiv;
    }

    public void setOptimizeForIndiv(String initials) {
        this.optimizeForIndiv = initials;
    }

    public String getSafeTaskType() {
        return safeTaskType;
    }

    public void setSafeTaskType(String safeTaskType) {
        if (safeTaskType != null)
            this.safeTaskType = safeTaskType;
    }


    /** Return true if the given object is in the list of currently
     * cut nodes */
    public boolean isCutNode(Object node) {
        return (cutList != null && cutList.contains(node));
    }


    /** Cancel the current cut operation, if there is one. */
    public void cancelCut() {
        if (cutList == null || cutList.size() == 0) return;

        int[] rowBounds = getVisibleRowBounds(cutList);
        // An undo/redo operation will cause the cut list to become invalid,
        // and will cause firstRow/lastRow to be -1.  In addition, these
        // numbers will be -1 if the cut items are all hidden under a
        // collapsed node.  Don't fire a table event in those situations.
        if (rowBounds[0] >= 0 && rowBounds[1] >= 0)
            wbsModel.fireNodeAppearanceChanged(rowBounds[0], rowBounds[1]);
        cutList = null;
    }


    /** Override method to forward "scroll" events to our delegate, if one
     * is set. */
    public void scrollRectToVisible(Rectangle aRect) {
        if (scrollableDelegate == null)
            super.scrollRectToVisible(aRect);
        else
            scrollableDelegate.scrollRectToVisible(aRect);
    }

    /** Set the component which should handle scroll events on our behalf. */
    public void setScrollableDelegate(JComponent c) {
        scrollableDelegate = c;
        if (c != null)
            putClientProperty(ZoomManager.DISABLE_TABLE_SCROLL_LOCK, true);
    }


    /** Change the current selection so it contains the given list of rows */
    public void selectRows(int[] rowsToSelect) {
        selectRows(rowsToSelect, false);
    }
    public void selectRows(int[] rowsToSelect, boolean scroll) {
        if (rowsToSelect != null) {
            if (rowsToSelect.length == 0)
                clearSelection();
            else {
                getSelectionModel().setValueIsAdjusting(true);
                clearSelection();
                int minRow, maxRow;
                minRow = maxRow = rowsToSelect[0];
                for (int i=rowsToSelect.length;   i-- > 0; ) {
                    int row = rowsToSelect[i];
                    addRowSelectionInterval(row, row);
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                }
                getSelectionModel().setValueIsAdjusting(false);

                if (scroll) {
                    minRow = Math.max(0, minRow-1);
                    scrollRectToVisible(getCellRect(maxRow, 0, true));
                    scrollRectToVisible(getCellRect(minRow, 0, true));
                }
            }
        }
    }

    public boolean displayHyperlinkedItem(String item) {
        String[] parts = item.split("/", 2);
        int wbsNodeId = Integer.parseInt(parts[0]);
        return selectAndShowNode(wbsNodeId) //
                && selectColumn(parts.length > 1 ? parts[1] : null);
    }

    public boolean selectAndShowNode(int wbsNodeId) {
        // find the node in the model with the specified ID
        WBSNode node = wbsModel.getNodeMap().get(wbsNodeId);
        return selectAndShowNode(node);
    }

    public boolean selectAndShowNode(WBSNode node) {
        if (node == null)
            return false;

        // stop any editing session that might be in progress
        stopCellEditing();

        // ensure that the node in question is visible, and retrieve its row
        int row = wbsModel.makeVisible(node);
        if (row == -1)
            return false;

        // select the row for the node in question, and scroll to it
        selectRows(new int[] { row }, true);
        return true;
    }

    public boolean selectColumn(String columnID) {
        try {
            int columnPos = 0;
            if (columnID != null)
                columnPos = getColumnModel().getColumnIndex(columnID);
            getColumnModel().getSelectionModel().clearSelection();
            addColumnSelectionInterval(columnPos, columnPos);
            return true;
        } catch (IllegalArgumentException noSuchColumn) {
            return false;
        }
    }

    /**
     * Stop any editing session that might be in progress, saving the changes.
     * If no editing session is in progress, this method will have no effect.
     */
    public void stopCellEditing() {
        if (isEditing())
            getCellEditor().stopCellEditing();
        UndoList.stopCellEditing(this);
    }


    public void setBlameData(BlameData blame) {
        BlameModelData blameData = BlameData.getModel(blame,
            wbsModel.getModelType());
        if (this.blameData != blameData) {
            if (this.blameData != null)
                this.blameData.removeBlameModelDataListener(BLAME_LISTENER);
            if (this.blameSelectionListener != null) {
                this.blameSelectionListener.dispose();
                this.blameSelectionListener = null;
            }

            this.blameData = blameData;
            FILTER_ACTION.setBlameData(blameData);

            if (this.blameData != null) {
                this.blameData.addBlameModelDataListener(BLAME_LISTENER);
                this.blameSelectionListener = new BlameSelectionListener(this,
                        blame);
            }

            repaint();
        }
    }

    private BlameNodeData getBlameDataForRow(int row) {
        if (blameData == null)
            return null;
        WBSNode node = wbsModel.getNodeForRow(row);
        if (node == null)
            return null;
        else
            return blameData.get(node.getTreeNodeID());
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        Component editor = getEditorComponent();
        if (editor != null)
            editor.setFont(font);
    }

    @Override
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);

        // row height changes typically indicate a zooming operation. Notify the
        // WBS node renderer/editor so they can recalculate their geometry.
        if (renderer != null)
            renderer.updateGeometry();
        if (editor != null)
            editor.updateGeometry();
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row,
            int column) {
        Component result = super.prepareEditor(editor, row, column);
        result.setFont(getFont());
        return result;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableCellRenderer result = super.getCellRenderer(row, column);
        if (result instanceof Component)
            ((Component) result).setBackground(null);
        return result;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component result = super.prepareRenderer(renderer, row, column);

        boolean needsBlameAnnotation = false;
        BlameNodeData blame = getBlameDataForRow(row);
        if (blame != null) {
            column = convertColumnIndexToModel(column);
            if (column == 0) {
                needsBlameAnnotation = blame.hasStructuralChange();
            } else if (dataModel != null) {
                String columnID = dataModel.getColumn(column).getColumnID();
                needsBlameAnnotation = blame.isColumnAffected(columnID);
            }
        }
        if (needsBlameAnnotation)
            BlameAnnotationBorder.annotate(result);

        return result;
    }

    private final BlameModelDataListener BLAME_LISTENER = //
            new BlameComponentRepainter(this);



    /** Return a list of actions useful for editing the wbs */
    public Action[] getEditingActions() {
        return new Action[] { CUT_ACTION, COPY_ACTION, PASTE_ACTION,
                INSERT_ACTION, INSERT_AFTER_ACTION, DELETE_ACTION,
                MOVEUP_ACTION, MOVEDOWN_ACTION, PROMOTE_ACTION, DEMOTE_ACTION,
                EXPAND_ACTION, COLLAPSE_ACTION, EXPAND_ALL_ACTION,
                COLLAPSE_ALL_ACTION };
    }

    public void tweakClipboardActions(Resources res, Icon copyIcon,
            Icon pasteIcon) {
        CUT_ACTION.putValue(Action.NAME, res.getString("Cut"));

        COPY_ACTION.putValue(Action.NAME, res.getString("Copy"));
        COPY_ACTION.putValue(Action.SMALL_ICON, copyIcon);

        PASTE_ACTION.putValue(Action.NAME, res.getString("Paste"));
        PASTE_ACTION.putValue(Action.SMALL_ICON, pasteIcon);
    }

    /** Return a list of workflow-related actions */
    public Action[] getWorkflowActions(WorkflowWBSModel workflows) {
        Action apply = new SelectAndApplyWorkflowAction(workflows);
        Action reapply = new ReapplyWorkflowAction(workflows);
        return new Action[] { new ApplyOrReapplyWorkflowAction(workflows,
                apply, reapply) };
    }

    /** Return an action capable of inserting a workflow */
    public Action getInsertWorkflowAction(WorkflowWBSModel workflows) {
        if (INSERT_WORKFLOW_ACTION == null)
            INSERT_WORKFLOW_ACTION = new InsertWorkflowAction(workflows);
        return INSERT_WORKFLOW_ACTION;
    }

    public Action[] getMasterActions(TeamProject proj) {
        return new Action[] { new MergeMasterWBSAction(proj) };
    }



    /** */
    public void setSelectionModel(ListSelectionModel newModel) {
        ListSelectionModel oldModel = getSelectionModel();
        if (oldModel != null)
            oldModel.removeListSelectionListener(SELECTION_LISTENER);
        newModel.addListSelectionListener(SELECTION_LISTENER);
        super.setSelectionModel(newModel);
    }


    /** Populate the <tt>customActions</tt> list with actions for editing
     * the work breakdown structure. */
    private void buildCustomActionMaps() {

        // Map "Tab" and "Shift-Tab" to the demote/promote actions
        customActions.add(new WbsActionMapping(KeyEvent.VK_TAB, 0, "Demote",
                DEMOTE_ACTION));
        customActions.add(new WbsActionMapping(KeyEvent.VK_TAB, SHIFT, "Promote",
                PROMOTE_ACTION));
        customActions.add(new ActionMapping(KeyEvent.VK_INSERT, 0, "Insert",
                INSERT_ACTION));
        customActions.add(new ActionMapping(KeyEvent.VK_ENTER, 0, "Enter",
                ENTER_ACTION));
        customActions.add(new ActionMapping(KeyEvent.VK_ENTER, SHIFT,
                "InsertAfter", INSERT_AFTER_ACTION));
        customActions.add(new ActionMapping("collapseTree", COLLAPSE_ACTION));
        customActions.add(new ActionMapping("expandTree", EXPAND_ACTION));
        customActions.add(new ActionMapping("moveTreeUp", MOVEUP_ACTION));
        customActions.add(new ActionMapping("moveTreeDown", MOVEDOWN_ACTION));

        // Find the default row navigation actions for the table, and replace
        // them with new actions which (1) perform the original action, then
        // (2) restart editing on the newly selected cell.
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        List filter = Arrays.asList(new String[]
            { "selectPreviousRow", "selectNextRow",
                "selectFirstRow", "selectLastRow",
                "scrollUpChangeSelection", "scrollDownChangeSelection" });

        KeyStroke[] keys = inputMap.allKeys();
        for (int i = 0;   i < keys.length;   i++) {
            Object actionKey = inputMap.get(keys[i]);
            if (! filter.contains(actionKey)) continue;
            Action action = actionMap.get(actionKey);
            if (action == null) continue;
            customActions.add(new ActionMapping
                (keys[i], actionKey + "-WBS",
                 new DelegateActionRestartEditing(action,actionKey)));
        }
    }


    /** Install our list of custom actions into the input and action maps for a
     * given component */
    void installCustomActions(JComponent component) {
        Iterator i = customActions.iterator();
        while (i.hasNext())
            ((ActionMapping) i.next()).install(component);
    }


    /** Install actions that are appropriate only for the table (not for any
     * other component). */
    void installTableActions() {
        InputMap inputMap = getInputMap();
        InputMap inputMap2 = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        InputMap inputMap3 = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        ClipboardBridge clipboardBridge = new ClipboardBridge(this);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, CTRL), "Cut");
        actionMap.put("Cut", new WbsNodeKeystrokeDelegatingAction(CUT_ACTION,
                null));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, CTRL), "Copy");
        actionMap.put("Copy", new WbsNodeKeystrokeDelegatingAction(COPY_ACTION,
                clipboardBridge.getCopyAction()));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, CTRL), "Paste");
        inputMap2.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, CTRL), "Paste");
        actionMap.put("Paste", new WbsNodeKeystrokeDelegatingAction(
                PASTE_ACTION, clipboardBridge.getPasteAction()));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
        actionMap.put("Delete", new WbsNodeKeystrokeDelegatingAction(
                DELETE_ACTION, TABLE_DELETE_ACTION));

        // the expand/collapse icons have accelerators assigned for the plus
        // and minus keys.  Register some additional accelerator keys based on
        // other places where plus and minus appear on the keyboard.
        inputMap3.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ALT), "collapseTree");
        inputMap3.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ALT), "expandTree");
        inputMap3.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ALT), "expandTree");
        inputMap3.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ALT | SHIFT), "expandTree");
    }


    /** Look at each of our actions to determine if it should be enabled. */
    private void recalculateEnablement() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows != null && selectedRows.length > 1)
            Arrays.sort(selectedRows);
        for (Iterator i = enablementCalculations.iterator(); i.hasNext();) {
            EnablementCalculation calc = (EnablementCalculation) i.next();
            calc.recalculateEnablement(selectedRows);
        }
    }
    private List enablementCalculations = new LinkedList();
    protected void addEnablementCalculation(EnablementCalculation e) {
        enablementCalculations.add(e);
    }


    /** Return true if the list of rows contains at least one row. */
    private boolean notEmpty(int[] selectedRows) {
        return (selectedRows != null && selectedRows.length > 0);
    }

    /** Return true if the list of rows contains at least one row other than
     * row 0. */
    private boolean notJustRoot(int[] selectedRows) {
        if (selectedRows == null || selectedRows.length == 0) return false;
        if (selectedRows.length == 1 && selectedRows[0] == 0) return false;
        return true;
    }

    private boolean containsReadOnlyNode(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            WBSNode node = wbsModel.getNodeForRow(rows[i]);
            if (node == null)
                continue;
            if (node.isReadOnly())
                return true;
            if (node.isExpanded() == false) {
                for (WBSNode desc : wbsModel.getDescendants(node))
                    if (desc.isReadOnly())
                        return true;
            }
        }
        return false;
    }

    private boolean containsReadOnlyNode(List nodes) {
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            if (node.isReadOnly())
                return true;
        }
        return false;
    }

    /** test whether the given list of rows represent a parent immediately
     * followed by some number of its children. */
    private boolean isParentAndChildren(int[] rows) {
        if (rows == null || rows.length == 0)
            return false;
        int row = rows[0];
        WBSNode node = wbsModel.getNodeForRow(row);
        if (node == null)
            return false;
        int indent = node.getIndentLevel();
        for (int i = 1;  i < rows.length;  i++) {
            int nextRow = rows[i];
            if (nextRow - row != 1)
                return false;
            row = nextRow;
            node = wbsModel.getNodeForRow(row);
            if (node == null)
                return false;
            int subindent = node.getIndentLevel();
            if (subindent <= indent)
                return false;
        }
        return true;
    }

    private boolean containsMasterNode(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            WBSNode node = wbsModel.getNodeForRow(rows[i]);
            if (node == null)
                continue;
            if (MasterWBSUtil.isMasterNode(node))
                return true;
            if (node.isExpanded() == false) {
                for (WBSNode desc : wbsModel.getDescendants(node))
                    if (MasterWBSUtil.isMasterNode(desc))
                        return true;
            }
        }
        return false;
    }

    private void setSourceIDsForCopyOperation(List nodeList) {
        for (Iterator i = nodeList.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            if (taskIDSource != null)
                node.setAttribute(MasterWBSUtil.SOURCE_NODE_ID,
                        taskIDSource.getNodeID(node));
            node.setAttribute(MasterWBSUtil.MASTER_PARENT_ID, null);
            node.setAttribute(MasterWBSUtil.MASTER_NODE_ID, null);
            node.setAttribute(RelaunchWorkerNew.RELAUNCH_SOURCE_ID, null);
        }
    }

    private Set getNodeIDs(List nodes) {
        Set nodeIDs = new HashSet();
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            nodeIDs.add(Integer.toString(node.getUniqueID()));
        }

        return nodeIDs;
    }

    private Map getInsertWorkflowExtraAttrs(WBSNode node) {
        String autoZeroUser = getAutoZeroUserString(node);
        if (autoZeroUser == null)
            autoZeroUser = optimizeForIndiv;

        return (autoZeroUser == null ? null : Collections.singletonMap(
            TeamTimeColumn.AUTO_ZERO_USER_ATTR_TRANSIENT, autoZeroUser));
    }

    private String getAutoZeroUserString(WBSNode node) {
        if (dataModel == null)
            return null;
        int pos = dataModel.findColumn(TeamTimeColumn.COLUMN_ID);
        if (pos == -1)
            return null;

        TeamTimeColumn ttc = (TeamTimeColumn) dataModel.getColumn(pos);
        return ttc.getAutoZeroUserString(node);
    }

    /**
     * Find the first and last visible rows for a particular list of nodes.
     * 
     * @param nodes a list of nodes from the WBS. The nodes in this array must
     *    be in the same order that they appear in the WBS.
     * @return an array of size 2. The first element will be the first visible
     *    row number, and the second element will be the last visible row
     *    number.  If none of the rows are visible, both elements of the
     *    array will be -1
     */
    private int[] getVisibleRowBounds(List nodes) {
        int[] result = new int[2];
        result[0] = result[1] = -1;
        for (int i = 0;  i < nodes.size();  i++) {
            int row = wbsModel.getRowForNode((WBSNode) nodes.get(i));
            if (row != -1) {
                result[0] = row;
                break;
            }
        }
        for (int i = nodes.size();  i-- > 0; ) {
            int row = wbsModel.getRowForNode((WBSNode) nodes.get(i));
            if (row != -1) {
                result[1] = row;
                break;
            }
        }
        return result;
    }

    protected boolean isWbsNodeColumnSelected() {
        int currentCol = getColumnModel().getSelectionModel()
                .getAnchorSelectionIndex();
        if (currentCol == -1)
            currentCol = getEditingColumn();
        return (currentCol == -1 || getColumnClass(currentCol) == WBSNode.class);
    }

    private void delegateKeyAction(Action action, ActionEvent e) {
        if (action != null && action.isEnabled()) {
            ActionEvent event = new ActionEvent(WBSJTable.this, e.getID(), e
                    .getActionCommand(), e.getModifiers());
            action.actionPerformed(event);
        }
    }

    protected interface EnablementCalculation {
        public void recalculateEnablement(int[] selectedRows);
    }


    /** Class that will perform a WBS-specific action if the cursor selection
     * is in the WBS node column, but will delegate to a regular table action
     * otherwise. */
    private class WbsNodeKeystrokeDelegatingAction extends AbstractAction {
        private Action wbsAction;
        private Action tableAction;

        WbsNodeKeystrokeDelegatingAction(Action wbsAction, Action tableAction) {
            this.wbsAction = wbsAction;
            this.tableAction = tableAction;
        }

        public void actionPerformed(ActionEvent e) {
            if (isWbsNodeColumnSelected() && wbsAction.isEnabled())
                delegateKeyAction(wbsAction, e);
            else
                delegateKeyAction(tableAction, e);
        }
    }


    /** An ActionMapping object holds information about an Action and the
     * keystrokes that should be used to invoke it.  An ActionMapping object
     * knows how to install itself into a component.  This class takes things
     * one step farther, to support Actions which are WBS-node-specific: i.e.,
     * actions that should only be triggered when the WBS node column is
     * selected.  During the installation process, it will automatically
     * look up the current binding for the keystroke.  If one exists, it
     * will create and install a {@link WbsNodeKeystrokeDelegatingAction} instead. */
    private class WbsActionMapping extends ActionMapping {

        public WbsActionMapping(int keyCode, int modifiers, Object actionKey,
                Action action) {
            super(keyCode, modifiers, actionKey, action);
        }

        @Override
        public void install(JComponent component, int condition) {
            Action actionToInstall = this.action;

            if (component instanceof JTable) {
                Action originalAction = getOriginalAction(component);
                if (originalAction != null)
                    actionToInstall = new WbsNodeKeystrokeDelegatingAction(
                            this.action, originalAction);
            }

            super.install(component, condition, actionToInstall);
        }

        private Action getOriginalAction(JComponent component) {
            return getOriginalAction(component, JComponent.WHEN_FOCUSED,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        }

        private Action getOriginalAction(JComponent comp, int... conditions) {
            for (int condition : conditions) {
                Action result = getOriginalAction(comp, condition);
                if (result != null)
                    return result;
            }
            return null;
        }

        private Action getOriginalAction(JComponent comp, int condition) {
            Object origActionKey = comp.getInputMap(condition).get(keyStroke);
            if (origActionKey == null)
                return null;
            else
                return comp.getActionMap().get(origActionKey);
        }
    }


    /** Abstract class for an action that (1) stops editing, (2) does
     * something, then (3) restarts editing */
    private class RestartEditingAction extends AbstractAction {
        /** Policy for editing restart.  If you want to resume editing
         * the node that was being edited when our action was triggered,
         * set this to false.  If you want to begin editing whatever cell is
         * highlighted after our action completes, set this to true. */
        protected boolean editingFollowsSelection = false;

        protected int editingRow, editingColumn;
        protected EventObject restartEvent;
        protected WBSNode editingNode;

        public RestartEditingAction() { }
        public RestartEditingAction(String name) { super(name); }
        public RestartEditingAction(String name, Icon icon) {
            super(name, icon);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // Get a "restart editing" event from the cell editor.
            restartEvent = editor.getRestartEditingEvent();

            // ask what column we're editing. (for our purposes, it's always
            // going to be column 0, but lets be more robust.)
            editingColumn = WBSJTable.this.editingColumn;
            editingRow    = WBSJTable.this.editingRow;

            // find out what node is currently being edited.
            editingNode = (WBSNode) editor.getCellEditorValue();

            // stop the current editing session.
            if (editingNode != null) editor.stopCellEditing();
            UndoList.stopCellEditing(WBSJTable.this);

            // do something.
            doAction(e);

            // restart editing.
            restartEditing(editingColumn, editingNode, restartEvent);
        }

        public void doAction(ActionEvent e) {}

        /** Start up an editing session. */
        public void restartEditing(int editingColumn, WBSNode editingNode,
                                   EventObject restartEvent) {
            if (disableEditing)
                return;

            if (editingFollowsSelection) {
                // Begin editing the newly selected node.
                WBSJTable.this.grabFocus();
                int rowToEdit = getSelectedRow();
                int columnToEdit = editingColumn; //getSelectedColumn();
                editCellAt(rowToEdit, columnToEdit, restartEvent);

            } else if (editingNode != null) {
                // Resume editing the cell we used to be editing.
                int editingRow = wbsModel.getRowForNode(editingNode);
                if (editingRow > 0)
                    // start editing the node
                    editCellAt(editingRow, editingColumn, restartEvent);
            }
        }

        /** Convenience method to get the row selection. some actions we
         * take may clear the selection;  to solve this, we generate a list
         * containing the editing row. */
        protected int[] getSelectedRows() {
            int[] result = WBSJTable.this.getSelectedRows();

            if ((result == null || result.length == 0) && editingRow != -1) {
                result = new int[1];
                result[0] = editingRow;
            }

            return result;
        }

        protected String getActionName() { return (String) getValue(NAME); }
    }



    /** An action to demote the selected rows. */
    private class DemoteAction extends RestartEditingAction implements
            EnablementCalculation {

        public DemoteAction() {
            super(resources.getString("Edit.Demote"), IconFactory.getDemoteIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_INDENT);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ALT));
            enablementCalculations.add(this);
        }
        public void doAction(ActionEvent e) {
            if (containsMasterNode(getSelectedRows()))
                return;
            selectRows(wbsModel.indentNodes(getSelectedRows(), 1));
            UndoList.madeChange(WBSJTable.this, getActionName());
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !disableIndentation
                    && !isFiltered()
                    && notJustRoot(selectedRows)
                    && !containsMasterNode(selectedRows));
        }
    }
    final DemoteAction DEMOTE_ACTION = new DemoteAction();


    /** An action to promote the selected rows. */
    private class PromoteAction extends RestartEditingAction implements
            EnablementCalculation {

        public PromoteAction() {
            super(resources.getString("Edit.Promote"), IconFactory.getPromoteIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_INDENT);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ALT));
            enablementCalculations.add(this);
        }
        public void doAction(ActionEvent e) {
            if (containsMasterNode(getSelectedRows()))
                return;
            selectRows(wbsModel.indentNodes(getSelectedRows(), -1));
            UndoList.madeChange(WBSJTable.this, getActionName());
        }
        public void recalculateEnablement(int[] selectedRows) {
            if (disableEditing
                    || disableIndentation
                    || isFiltered()
                    || notJustRoot(selectedRows) == false
                    || containsMasterNode(selectedRows))
                setEnabled(false);
            else {
                for (int i = selectedRows.length;   i-- > 0; ) {
                    WBSNode n = wbsModel.getNodeForRow(selectedRows[i]);
                    if (n != null && n.getIndentLevel() < 2) {
                        setEnabled(false);
                        return;
                    }
                }
                setEnabled(true);
            }
        }
    }
    final PromoteAction PROMOTE_ACTION = new PromoteAction();


    /** An action which (1) stops the current editing session, (2) triggers
     * some other action, then (3) restarts editing. */
    private class DelegateActionRestartEditing extends RestartEditingAction {
        Action realAction;

        public DelegateActionRestartEditing(Action realAction, Object key) {
            super();
            putValue(NAME, key);
            this.realAction = realAction;
            this.editingFollowsSelection = true;
        }

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                doAction(e);
            else
                super.actionPerformed(e);
        }

        public void doAction(ActionEvent e) {
            delegateKeyAction(realAction, e);
        }
    }


    /** An action to perform a "cut" operation */
    private class CutAction extends AbstractAction implements ClipboardOwner,
            EnablementCalculation {

        public CutAction() {
            super(resources.getString("Edit.WBS.Cut"), IconFactory.getCutIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_CLIPBOARD);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            rows = wbsModel.getAtomicRowList(rows);
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // record the cut nodes.
            cutList = wbsModel.getNodesForRows(rows, true);
            WorkflowUtil.storeWorkflowStepNames(cutList, workflows);
            WBSClipSelection.putNodeListOnClipboard(cutList, this);
            WorkflowUtil.clearWorkflowStepNames(cutList);

            // update the appearance of newly cut cells (they will be
            // displaying phantom icons).
            wbsModel.fireNodeAppearanceChanged(rows[0], rows[rows.length-1]);
            if (isEditing()) editor.updateIconAppearance();
            WBSJTable.this.recalculateEnablement();
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !isFiltered()
                    && notJustRoot(selectedRows));
        }
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            cancelCut();
        }
    }
    final CutAction CUT_ACTION = new CutAction();


    /** An action to perform a "copy" operation */
    private class CopyAction extends AbstractAction implements
            EnablementCalculation {

        public CopyAction() {
            super(resources.getString("Edit.WBS.Copy"), IconFactory.getCopyIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_CLIPBOARD);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            rows = wbsModel.getAtomicRowList(rows);
            if (rows == null || rows.length == 0) return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            // make a list of the copied nodes
            List copyList = WBSNode.cloneNodeList
                (wbsModel.getNodesForRows(rows, true));
            setSourceIDsForCopyOperation(copyList);
            WorkflowUtil.storeWorkflowStepNames(copyList, workflows);
            WBSClipSelection.putNodeListOnClipboard(copyList, null);
            WorkflowUtil.clearWorkflowStepNames(copyList);

            WBSJTable.this.recalculateEnablement();
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!isFiltered() && notJustRoot(selectedRows));
        }
    }
    final CopyAction COPY_ACTION = new CopyAction();


    /** An action to perform a "paste" operation */
    private class PasteAction extends AbstractAction implements
            EnablementCalculation {

        public PasteAction() {
            super(resources.getString("Edit.WBS.Paste"),
                    IconFactory.getPasteIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_CLIPBOARD);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            WBSNode beforeNode = getLocation();
            if (beforeNode == null) return;

            // stop the current editing session.
            editor.stopCellEditing();
            UndoList.stopCellEditing(WBSJTable.this);

            // paste the nodes.
            insertCopiedNodes(beforeNode);
        }

        WBSNode getLocation() {
            int rowNum = getSelectedRow();
            if (rowNum == -1) return null;
            if (rowNum == 0) rowNum++;
            WBSNode result = wbsModel.getNodeForRow(rowNum);
            if (cutList != null && cutList.contains(result))
                return null;
            return result;
        }

        void insertCopiedNodes(WBSNode beforeNode) {
            List<WBSNode> nodesToInsert = WBSClipSelection
                    .getNodeListFromClipboard(beforeNode);
            if (nodesToInsert == null || nodesToInsert.size() == 0) return;

            if (nodesToInsert == cutList) {
                if (wbsModel.deleteNodes(cutList, false) == false) {
                    // We asked the WBS model to delete the nodes, but nothing
                    // was deleted, because the specified nodes were not found.
                    // This indicates that the cut list is invalid - probably
                    // because of an intervening undo/redo operation.  Clear
                    // the cut list and abort.
                    cutList = null;
                    return;
                }
            } else {
                nodesToInsert = WBSNode.cloneNodeList(nodesToInsert);
                for (WBSNode n : nodesToInsert) {
                    MasterWBSUtil.removeMasterNodeAttrs(n);
                    ExtSyncUtil.removeExtNodeAttributes(n);
                    if (dataModel != null)
                        dataModel.cleanForeignNodeAttributes(n);
                }
                WorkflowUtil.resolveWorkflowStepNames(nodesToInsert, workflows);
            }
            cutList = null;

            // Generally, we will insert the pasted nodes immediately before
            // the designated node.  Exception: if the designated node was
            // one of nodes originally copied to begin this copy/paste
            // operation, insert the new nodes after the originally copied
            // ones.  This behavior is easier for a user to follow.
            int pos = wbsModel.getRowForNode(beforeNode);
            pos = wbsModel.getPasteBeforePos(pos, nodesToInsert);
            Set copiedIDs = getNodeIDs(nodesToInsert);
            while (rowContainsCopiedNode(pos, copiedIDs))
                pos++;

            int[] rowsInserted = wbsModel.insertNodes(nodesToInsert, pos);
            selectRows(rowsInserted);
            if (rowsInserted != null && rowsInserted.length > 0) {
                int firstRow = rowsInserted[0];
                int lastRow = rowsInserted[rowsInserted.length-1];
                if(maybeRenameCopiedNodes(nodesToInsert))
                    wbsModel.fireNodeAppearanceChanged(firstRow, lastRow);
                scrollRectToVisible(getCellRect(lastRow, 0, true));
                scrollRectToVisible(getCellRect(firstRow-1, 0, true));
            }

            UndoList.madeChange(WBSJTable.this, (String) getValue(NAME));
        }
        boolean rowContainsCopiedNode(int row, Set copiedIDs) {
            WBSNode destNode = wbsModel.getNodeForRow(row);
            if (destNode == null)
                return false;
            String destNodeID = Integer.toString(destNode.getUniqueID());
            return copiedIDs.contains(destNodeID);
        }
        boolean maybeRenameCopiedNodes(List insertedNodes) {
            boolean renameOccurred = false;
            for (Iterator i = insertedNodes.iterator(); i.hasNext();) {
                if (maybeRenameCopiedNode((WBSNode) i.next()))
                    renameOccurred = true;
            }
            return renameOccurred;
        }
        boolean maybeRenameCopiedNode(WBSNode node) {
            String nodeName = node.getName();
            WBSNode parent = wbsModel.getParent(node);
            WBSNode[] children = wbsModel.getChildren(parent);
            Set childNames = new HashSet();
            for (int i = 0; i < children.length; i++) {
                WBSNode child = children[i];
                if (child != node)
                    childNames.add(child.getName());
            }
            String finalName = nodeName;
            int n = 2;
            while (childNames.contains(finalName)) {
                finalName = nodeName + " (" + (n++) + ")";
            }
            if (finalName == nodeName) {
                return false;
            } else {
                node.setName(finalName);
                return true;
            }
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !isFiltered()
                    && notEmpty(selectedRows)
                    && getLocation() != null);
        }
    }
    final PasteAction PASTE_ACTION = new PasteAction();


    /** An action to perform an "insert node before" operation */
    private class InsertAction extends RestartEditingAction implements
            EnablementCalculation {

        public InsertAction() {
            this(resources.getString("Edit.Insert"));
        }
        public InsertAction(String name) {
            super(name);
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_STRUCTURE);
            editingFollowsSelection = true;
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (isWbsNodeColumnSelected())
                super.actionPerformed(e);
            else
                performAlternateAction(e);
        }
        protected void performAlternateAction(ActionEvent e) {
        }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            insertRowBefore(currentRow, currentRow);
        }
        protected void insertRowBefore(int row, int rowToCopy) {
            if (row == -1) return;
            if (row == 0) row = 1;
            if (rowToCopy == 0) rowToCopy = 1;

            String type = TeamProcess.COMPONENT_TYPE;
            Object workflowType = null;
            int indentLevel = 1;
            boolean expanded = false;

            WBSNode nodeToCopy = wbsModel.getNodeForRow(rowToCopy);
            if (nodeToCopy != null) {
                type = nodeToCopy.getType();
                workflowType = nodeToCopy.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
                if (TeamProcess.isProbeTask(type)) {
                    type = getSafeTaskType();
                    workflowType = null;
                }
                indentLevel = nodeToCopy.getIndentLevel();
                expanded = nodeToCopy.isExpanded();
            }
            if (cutList != null && cutList.contains(nodeToCopy)) cancelCut();
            WBSNode newNode = new WBSNode(wbsModel, "", type, indentLevel,
                    expanded);
            newNode.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, workflowType);

            newNode.setAttribute(AUTO_ZERO_ATTR_1, optimizeForIndiv);

            row = wbsModel.add(row, newNode);

            if (newNode.removeAttribute(AUTO_ZERO_ATTR_1) != null)
                newNode.setAttribute(AUTO_ZERO_ATTR_2, optimizeForIndiv);

            setRowSelectionInterval(row, row);
            scrollRectToVisible(getCellRect(row, 0, true));

            UndoList.madeChange(WBSJTable.this, getActionName());
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !isFiltered()
                    && notEmpty(selectedRows));
        }
        // Normally we would like to use a transient attribute to register an
        // "auto zero user".  However, when the node is first added to the WBS,
        // the WBS will discard all of the transient attributes.  So we store
        // the value in a persistent attribute, then add the node to the WBS.
        // The act of adding will cause recalculations to occur, which could
        // cause the "auto zero user" to be applied.  But if the auto zero
        // user was not applied during that addition/recalc, we move the
        // registration from a persistent attribute to a transient one, so it
        // will not be saved along with other WBS attributes.
        private static final String AUTO_ZERO_ATTR_1 = TeamTimeColumn.AUTO_ZERO_USER_ATTR_PERSISTENT;
        private static final String AUTO_ZERO_ATTR_2 = TeamTimeColumn.AUTO_ZERO_USER_ATTR_TRANSIENT;
    }
    final InsertAction INSERT_ACTION = new InsertAction();


    /** An action to perform an "insert node after" operation */
    private class InsertAfterAction extends InsertAction {
        private Action alternateAction;
        public InsertAfterAction() {
            super(resources.getString("Edit.Insert_After"));
            putValue(SMALL_ICON, IconFactory.getAddRowIcon());
            putValue(SHORT_DESCRIPTION, resources.getString("Edit.Add_Row"));
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_STRUCTURE);

            try {
                Object actionKey = getInputMap(
                        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
                alternateAction = getActionMap().get(actionKey);
            } catch (Exception e) {
            }
        }
        public void doAction(ActionEvent e) {
            int currentRow = getSelectedRow();
            int targetRow = wbsModel.getInsertAfterPos(currentRow);
            insertRowBefore(targetRow, currentRow);
        }
        protected void performAlternateAction(ActionEvent e) {
            if (alternateAction != null)
                alternateAction.actionPerformed(e);
        }
    }
    final InsertAfterAction INSERT_AFTER_ACTION = new InsertAfterAction();


    /** An action to selectively perform an "insert node after" operation,
     * if the enterInsertsLine property is true */
    private class EnterAction extends InsertAfterAction {
        @Override
        protected void insertRowBefore(int row, int rowToCopy) {
            if (enterInsertsLine.isSelected()) {
                super.insertRowBefore(row, rowToCopy);
            } else if (row > 0 && row < getRowCount()) {
                setRowSelectionInterval(row, row);
                scrollRectToVisible(getCellRect(row, 0, true));
            }
        }
    }
    final InsertAfterAction ENTER_ACTION = new EnterAction();


    /** An action to configure the enterInsertsLine property */
    private class ToggleEnterBehaviorAction extends AbstractAction implements
            ChangeListener {
        private String enabledTip, disabledTip;
        public ToggleEnterBehaviorAction() {
            enterInsertsLine.setSelected(true);
            enterInsertsLine.addChangeListener(this);
            enabledTip = getHTML("Edit.Enter_Inserts");
            disabledTip = getHTML("Edit.Enter_Next_Row");
            stateChanged(null);
        }
        private String getHTML(String resKey) {
            String html = resources.getHTML(resKey);
            html = StringUtils.findAndReplace(html, "\n", "<br>");
            html = StringUtils.findAndReplace(html, "[", "<b>");
            html = StringUtils.findAndReplace(html, "]", "</b>");
            html = StringUtils.findAndReplace(html, "{", "<i>");
            html = StringUtils.findAndReplace(html, "}", "</i>");
            return "<html>" + html + "</html>";
        }
        public void stateChanged(ChangeEvent e) {
            if (enterInsertsLine.isSelected()) {
                putValue(Action.SMALL_ICON, IconFactory.getInsertOnEnterIcon());
                putValue(Action.SHORT_DESCRIPTION, enabledTip);
            } else {
                putValue(Action.SMALL_ICON, IconFactory
                        .getNoInsertOnEnterIcon());
                putValue(Action.SHORT_DESCRIPTION, disabledTip);
            }
        }
        public void actionPerformed(ActionEvent e) {
            enterInsertsLine.setSelected(!enterInsertsLine.isSelected());
        }
    }
    final ToggleEnterBehaviorAction TOGGLE_ENTER_BEHAVIOR_ACTION =
            new ToggleEnterBehaviorAction();


    /** An action to perform a "delete" operation */
    private class DeleteAction extends AbstractAction implements
            EnablementCalculation {

        public DeleteAction() {
            super(resources.getString("Edit.Delete"), IconFactory.getDeleteIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_STRUCTURE);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            rows = wbsModel.getAtomicRowList(rows);
            if (rows == null || rows.length == 0) return;

            // verify that the user wants to cut the nodes.
            List nodesToDelete = wbsModel.getNodesForRows(rows, true);
            if (nodesToDelete == null || nodesToDelete.size() == 0
                    || containsReadOnlyNode(nodesToDelete)) return;
            if (maybeAskUserToConfirmDeletion(nodesToDelete) == false)
                return;

            // cancel the previous cut operation, if there was one.
            cancelCut();

            if (isEditing()) editor.stopCellEditing();
            UndoList.stopCellEditing(WBSJTable.this);

            // create an object to reallocate time from deleted workflow tasks
            WorkflowDeletedTimeSpreader wdts = new WorkflowDeletedTimeSpreader(
                    wbsModel, dataModel, workflows, nodesToDelete);

            // delete the nodes.
            wbsModel.deleteNodes(nodesToDelete);

            // reallocate time from deleted workflow tasks into sibling tasks
            // nearby. If this table is editing a secondary model (like
            // milestones, proxies, etc), this will do nothing.
            wdts.respreadDeletedTime();

            int rowToSelect = Math.min(rows[0], wbsModel.getRowCount()-1);
            setRowSelectionInterval(rowToSelect, rowToSelect);
            scrollRectToVisible(getCellRect(rowToSelect, 0, true));

            UndoList.madeChange(WBSJTable.this, (String) getValue(NAME));
        }
        private boolean maybeAskUserToConfirmDeletion(
                List<WBSNode> nodesToDelete) {
            if (checkForActualDataOnDelete == false
                    || containsActualData(nodesToDelete) == false)
                return true;

            Window window = SwingUtilities.getWindowAncestor(WBSJTable.this);
            Object message = (nodesToDelete.size() == 1 ? DELETE_ONE_MESSAGE
                    : DELETE_MANY_MESSAGE);
            int userChoice = JOptionPane.showConfirmDialog(window, message,
                "Confirm Deletion", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            return userChoice == JOptionPane.OK_OPTION;
        }
        private boolean containsActualData(List<WBSNode> nodeList) {
            for (WBSNode node : nodeList)
                if (TeamActualTimeColumn.hasActualTime(node))
                    return true;
            return false;
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !isFiltered()
                    && notJustRoot(selectedRows)
                    && !containsReadOnlyNode(selectedRows));
        }
    }
    final DeleteAction DELETE_ACTION = new DeleteAction();
    boolean checkForActualDataOnDelete = false;
    private static final String[] DELETE_ONE_MESSAGE = {
        "Actual data has been collected against the WBS item you are",
        "about to delete.  Are you certain you want to delete it?"
    };
    private static final String[] DELETE_MANY_MESSAGE = {
        "Some of the WBS items you are about to delete have actual data",
        "associated with them. Are you certain you want to delete them?"
    };


    /** An action to delete the value in the currently selected table cell. */
    private class TableDeleteAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            int selRow = getSelectedRow();
            int selCol = getSelectedColumn();
            if (selRow != -1 && selCol != -1)
                setValueAt(null, selRow, selCol);
        }
    }
    final TableDeleteAction TABLE_DELETE_ACTION = new TableDeleteAction();


    /** An action to insert information from a workflow */
    private class InsertWorkflowAction extends AbstractAction implements
            EnablementCalculation {

        private WorkflowWBSModel workflows;

        public InsertWorkflowAction(WorkflowWBSModel workflows) {
            this.workflows = workflows;
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_WORKFLOW);
            setEnabled(false);
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            UndoList.stopCellEditing(WBSJTable.this);
            editor.stopCellEditing();

            // get the name of the workflow to insert.
            String workflowName = e.getActionCommand();
            if (workflowName == null || workflowName.length() == 0) return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // make the change
            List insertedNodes = new ArrayList();
            Arrays.sort(rows);
            for (int i = rows.length; i-- > 0; ) {
                WBSNode node = wbsModel.getNodeForRow(rows[i]);
                List<WBSNode> inserted = WorkflowUtil.insertWorkflow(wbsModel,
                    node, workflowName, workflows,
                    WorkflowDataModel.WORKFLOW_ATTRS,
                    getInsertWorkflowExtraAttrs(node), true);
                if (inserted != null)
                    insertedNodes.addAll(inserted);
            }

            if (!insertedNodes.isEmpty()) {
                int[] newRowsToSelect = wbsModel.getRowsForNodes(insertedNodes);
                Arrays.sort(newRowsToSelect);
                selectRows(newRowsToSelect);
                WBSJTable.this.requestFocusInWindow();
                UndoList.madeChange(WBSJTable.this, "Insert workflow");
            }
        }

        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                    && !isFiltered()
                    && notEmpty(selectedRows));
        }
    }
    private InsertWorkflowAction INSERT_WORKFLOW_ACTION = null;


    /** Let the user select a workflow to apply, using autocompletion */
    private class SelectAndApplyWorkflowAction extends AbstractAction implements
            PropertyChangeListener {

        private WorkflowWBSModel workflows;
        private Action insertAction;
        private String lastWorkflow;

        public SelectAndApplyWorkflowAction(WorkflowWBSModel workflows) {
            super(resources.getString("Workflow.Apply.Menu"));
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_WORKFLOW);
            setEnabled(false);
            this.workflows = workflows;
            this.insertAction = getInsertWorkflowAction(workflows);
            insertAction.addPropertyChangeListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            // abort if workflow insertion is not currently allowed
            if (insertAction.isEnabled() == false)
                return;

            // create user interface components that the user can use to
            // select a workflow, using autocompletion
            JTextField text = new JTextField();
            new JOptionPaneActionHandler().install(text);

            Vector options = new Vector();
            for (WBSNode wkflw : workflows.getWorkflowNodes()) {
                String name = wkflw.getName();
                if (name.trim().length() > 0 && !options.contains(name))
                    options.add(name);
            }
            JList optionList = new JList(options);
            installKeystrokeForWindow(optionList, "UP", "DOWN", "PAGE_UP",
                "PAGE_DOWN");
            new JOptionPaneClickHandler().install(optionList);
            JScrollPane sp = new JScrollPane(optionList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            AutoCompleteDecorator.decorate(optionList, text);
            text.setText(lastWorkflow);

            Object[] message = new Object[] {
                    resources.getString("Workflow.Apply.Window_Prompt"),
                    BoxUtils.vbox(text, sp),
                    new JOptionPaneTweaker.GrabFocus(text) };
            String title = resources.getString("Workflow.Apply.Window_Title");

            // display the options to the user and let them make a selection
            int userChoice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(WBSJTable.this), message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            lastWorkflow = text.getText();
            insertAction.actionPerformed(new ActionEvent(this,
                    ActionEvent.ACTION_PERFORMED, lastWorkflow));
        }

        private void installKeystrokeForWindow(JComponent c, String... keys) {
            for (String oneKey : keys) {
                KeyStroke keyStroke = KeyStroke.getKeyStroke(oneKey);
                Object action = c.getInputMap().get(keyStroke);
                c.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, action);
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setEnabled(insertAction.isEnabled());
        }

    }


    /** Let the user reapply workflows to one or more existing enactments */
    private class ReapplyWorkflowAction extends AbstractAction implements
            EnablementCalculation {

        private WorkflowWBSModel workflows;
        private AbstractButton useSelection, useEntireWbs;
        private JCheckBox updateTimes;
        private DefaultTreeModel workflowTreeModel;
        private DefaultMutableTreeNode workflowTreeRoot;
        private CheckboxTree workflowTree;
        private JPanel listPanel;
        private CardLayout listPanelLayout;
        private JOptionPane pane;
        private JDialog dialog;

        public ReapplyWorkflowAction(WorkflowWBSModel workflows) {
            super(resources.getString("Workflow.Reapply.Menu"));
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_WORKFLOW);
            putValue(MNEMONIC_KEY, new Integer('R'));
            this.workflows = workflows;
            recalculateEnablement(null);
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            if (dialog == null)
                buildDialog();
            Object src = e.getSource();
            if (src == useSelection || src == useEntireWbs) {
                refreshWorkflowEnactments();
            } else {
                stopCellEditing();
                prepareAndShowDialog();
            }
        }

        private void buildDialog() {
            ButtonGroup g = new ButtonGroup();
            useSelection = makeRadioButton(g, "Scope_Selection", KeyEvent.VK_S);
            useEntireWbs = makeRadioButton(g, "Scope_WBS", KeyEvent.VK_W);

            JCheckBox updateTasks = makeCheckBox("Change_Tasks");
            Box updateTasksRow = BoxUtils.hbox(updateTasks, updateTasks.getText());
            updateTasksRow.setToolTipText(updateTasks.getToolTipText());
            updateTasks.setEnabled(false);
            updateTasks.setText(null);

            updateTimes = makeCheckBox("Change_Times");
            updateTimes.setMnemonic(KeyEvent.VK_P);

            workflowTreeRoot = new DefaultMutableTreeNode();
            workflowTreeModel = new DefaultTreeModel(workflowTreeRoot);
            workflowTree = new CheckboxTree(workflowTreeModel);
            CheckboxNodeRenderer r = (CheckboxNodeRenderer) workflowTree
                    .getCellRenderer();
            r.changeOpenIcon(IconFactory.getWorkflowIcon());
            r.changeClosedIcon(IconFactory.getWorkflowIcon());
            r.changeLeafIcon(IconFactory.getEmptyIcon(10, 1));

            listPanel = new JPanel(listPanelLayout = new CardLayout());
            JLabel noneLabel = new JLabel(res("No_Workflows_Found"),
                    SwingConstants.CENTER);
            noneLabel.setFont(noneLabel.getFont().deriveFont(Font.ITALIC));
            listPanel.add(noneLabel, "none");
            listPanel.add(workflowTree, "data");
            JScrollPane sp = new JScrollPane(listPanel);
            sp.setPreferredSize(new Dimension(300, 100));

            Object content = new Object[] { //
                    res("Scope_Prompt"), useSelection, useEntireWbs, //
                    Box.createVerticalStrut(2), //
                    res("Change_Prompt"), updateTasksRow, updateTimes, //
                    Box.createVerticalStrut(2), //
                    res("Workflows_List_Prompt"), sp };

            Frame f = (Frame) SwingUtilities.getWindowAncestor(WBSJTable.this);
            pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            dialog = pane.createDialog(f, res("Window_Title"));
            dialog.setModal(true);
            dialog.setResizable(true);
            dialog.pack();
            dialog.setLocationRelativeTo(f);
        }

        private JRadioButton makeRadioButton(ButtonGroup g, String resKey, int m) {
            JRadioButton result = new JRadioButton(res(resKey));
            setToolTip(result, resKey);
            result.setMnemonic(m);
            indent(result);
            g.add(result);
            result.addActionListener(this);
            return result;
        }

        private JCheckBox makeCheckBox(String resKey) {
            JCheckBox result = new JCheckBox(res(resKey), true);
            setToolTip(result, resKey);
            indent(result);
            return result;
        }

        private void setToolTip(JComponent comp, String resKey) {
            String tipText = res(resKey + "_Tooltip");
            String tipHtml = "<html><div style='width:200px'>"
                    + HTMLUtils.escapeEntities(tipText) + "</div></html>";
            comp.setToolTipText(tipHtml);
        }

        private void indent(JComponent comp) {
            comp.setBorder(BorderFactory.createCompoundBorder(comp.getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
        }

        private String res(String key) {
            return resources.getString("Workflow.Reapply." + key);
        }

        private void prepareAndShowDialog() {
            // update the radio buttons based on whether table rows are selected
            if (getSelectedRow() == -1) {
                useSelection.setEnabled(false);
                useEntireWbs.setSelected(true);
            } else {
                useSelection.setEnabled(true);
                useSelection.setSelected(true);
            }

            // recalculate the list of enactments in the selection/WBS
            refreshWorkflowEnactments();

            // display the dialog and wait for a user response
            pane.selectInitialValue();
            dialog.setVisible(true);

            // if the user clicked OK, apply the changes
            if (Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
                applyChanges();
        }

        private void refreshWorkflowEnactments() {
            // identify the appropriate scope, and scan for workflow enactments
            List<WBSNode> nodes;
            if (useEntireWbs.isSelected()) {
                nodes = Collections.singletonList(wbsModel.getRoot());
            } else {
                int[] selectedRows = getSelectedRows();
                Arrays.sort(selectedRows);
                nodes = wbsModel.getNodesForRows(selectedRows, false);
            }
            Map<String, Set<WBSNode>> enactments = WorkflowUtil
                    .getWorkflowEnactmentRoots(wbsModel, nodes, workflows);

            // rebuild the tree based on the new list of enactments
            workflowTreeRoot.removeAllChildren();
            if (enactments.isEmpty()) {
                listPanelLayout.show(listPanel, "none");
                workflowTreeModel.nodeStructureChanged(workflowTreeRoot);

            } else {
                for (Entry<String, Set<WBSNode>> en : enactments.entrySet()) {
                    String workflowName = en.getKey();
                    DefaultMutableTreeNode workflowNode = //
                            new DefaultMutableTreeNode(workflowName);
                    workflowTreeRoot.add(workflowNode);
                    for (WBSNode node : en.getValue()) {
                        Enactment e = new Enactment(workflowName, node);
                        workflowNode.add(new DefaultMutableTreeNode(e));
                    }
                }
                workflowTreeModel.nodeStructureChanged(workflowTreeRoot);
                if (useEntireWbs.isSelected())
                    workflowTree.setCheckedStatusRecursive(workflowTreeRoot,
                        false);
                for (int i = workflowTree.getRowCount(); i-- > 0;)
                    workflowTree.collapseRow(i);
                if (enactments.size() == 1)
                    workflowTree.expandRow(0);
                listPanelLayout.show(listPanel, "data");
            }
        }

        private class Enactment {
            public String workflowName;
            public WBSNode wbsNode;
            public String display;
            protected Enactment(String workflow, WBSNode node) {
                this.workflowName = workflow;
                this.wbsNode = node;
                this.display = wbsModel.getFullName(node);
            }
            public String toString() { return display; }
        }

        private void applyChanges() {
            Set<WBSNode> affectedNodes = new HashSet();
            for (Object obj : workflowTree.getCheckedNodes()) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) obj;
                if (treeNode.getUserObject() instanceof Enactment) {
                    Enactment e = (Enactment) treeNode.getUserObject();
                    System.out.println("Reapplying " + e.workflowName + " to "
                            + e.display);

                    // update the tasks in the WBS based on the workflow
                    List<WBSNode> workflowSteps = WorkflowUtil.insertWorkflow(
                        wbsModel, e.wbsNode, e.workflowName, workflows,
                        WorkflowDataModel.WORKFLOW_ATTRS,
                        getInsertWorkflowExtraAttrs(e.wbsNode), false);
                    affectedNodes.add(e.wbsNode);
                    affectedNodes.addAll(workflowSteps);

                    // if requested, reapply the rates and percentages to produce
                    // adjusted time estimates for each task
                    if (updateTimes.isSelected())
                        WorkflowUtil.reapplyRatesAndPercentages(dataModel,
                            wbsModel, e.wbsNode, workflowSteps, e.workflowName,
                            workflows);
                }
            }

            if (!affectedNodes.isEmpty()) {
                selectRows(wbsModel.getRowsForNodes(new ArrayList(affectedNodes)));
                WBSJTable.this.requestFocusInWindow();
                UndoList.madeChange(WBSJTable.this, (String) getValue(NAME));
            }
        }

        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing && !isFiltered());
        }
    }


    /** Dynamically apply or reapply workflows, based on the current selection */
    private class ApplyOrReapplyWorkflowAction extends AbstractAction implements
            PropertyChangeListener {

        private WorkflowWBSModel workflows;
        private Action applyAction, reapplyAction;

        public ApplyOrReapplyWorkflowAction(WorkflowWBSModel workflows,
                Action applyAction, Action reapplyAction) {
            super(resources.getString("Workflow.Apply_Or_Reapply.Menu"));
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_WORKFLOW);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, //
                MacGUIUtils.getCtrlModifier()));
            this.workflows = workflows;
            this.applyAction = applyAction;
            this.reapplyAction = reapplyAction;
            applyAction.addPropertyChangeListener(this);
            reapplyAction.addPropertyChangeListener(this);
            propertyChange(null);
        }

        public void actionPerformed(ActionEvent e) {
            // determine the right action to perform, and delegate the event
            Action action = getAppropriateAction();
            if (action.isEnabled())
                action.actionPerformed(e);
        }

        private Action getAppropriateAction() {
            // if nothing is selected (-1) or if the root row is selected (0),
            // trigger the reapply action.
            if (getSelectedRow() < 1 || !applyAction.isEnabled())
                return reapplyAction;

            // look at the current selection to see if it implies any contained
            // or enclosing workflow enactment roots.
            int[] rows = getSelectedRows();
            Arrays.sort(rows);
            List<WBSNode> selectedNodes = wbsModel.getNodesForRows(rows, false);
            Map<String, Set<WBSNode>> enactments = WorkflowUtil
                    .getWorkflowEnactmentRoots(wbsModel, selectedNodes,
                        workflows);

            // if no enactments were found, reapply would have nothing to do.
            if (enactments.isEmpty())
                return applyAction;
            else
                // if enactments were found, trigger the reapply action.
                return reapplyAction;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setEnabled(applyAction.isEnabled() || reapplyAction.isEnabled());
        }
    }


    private class MergeMasterWBSAction extends AbstractAction implements
            EnablementCalculation {

        private TeamProject project;

        public MergeMasterWBSAction(TeamProject project) {
            super("Copy Core Work Items from Master Project");
            this.project = project;
            setEnabled(!disableEditing);
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            UndoList.stopCellEditing(WBSJTable.this);

            int[] newRowsToSelect = MasterWBSUtil.mergeFromMaster(project);
            if (newRowsToSelect != null) {
                editor.stopCellEditing();
                selectRows(newRowsToSelect);
                UndoList.madeChange(WBSJTable.this, "Copy Work from Master");
            }
        }

        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing && !isFiltered());
        }
    }


    /** An action to perform an "expand" operation */
    private class ExpandAction extends AbstractAction implements
            EnablementCalculation {

        public ExpandAction() {
            super(resources.getString("Edit.Expand"), IconFactory.getExpandIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_EXPANSION);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ALT));
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // stop editing the current cell.
            stopCellEditing();

            // expand selected nodes
            List selectedNodes = new ArrayList();
            for (int i = 0; i < rows.length; i++) {
                selectedNodes.add(wbsModel.getNodeForRow(rows[i]));
            }
            Set nodeIDs = getNodeIDs(selectedNodes);
            Set expandedNodeIDs = wbsModel.getExpandedNodeIDs();
            expandedNodeIDs.addAll(nodeIDs);
            wbsModel.setExpandedNodeIDs(expandedNodeIDs);

            // update selection
            List nodesToSelect = new ArrayList(selectedNodes);
            for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
                WBSNode node = (WBSNode) i.next();
                nodesToSelect.addAll(Arrays.asList(wbsModel.getDescendants(node)));
            }

            selectRows(wbsModel.getRowsForNodes(nodesToSelect));
         }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(notJustRoot(selectedRows));
        }
    }
    final ExpandAction EXPAND_ACTION = new ExpandAction();


    /** An action to perform an "expand all" operation */
    private class ExpandAllAction extends AbstractAction implements
            EnablementCalculation {

        public ExpandAllAction() {
            super(resources.getString("Edit.Expand_All"),
                    IconFactory.getExpandAllIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_EXPANSION);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;

            // stop editing the current cell.
            stopCellEditing();

            // expand selected nodes and all descendants
            List selectedNodes = wbsModel.getNodesForRows(rows, false);
            List nodesToExpand = new ArrayList(selectedNodes);
            for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
                WBSNode node = (WBSNode) i.next();
                WBSNode[] descendants = wbsModel.getDescendants(node);
                nodesToExpand.addAll(Arrays.asList(descendants));
            }

            Set nodeIDs = getNodeIDs(nodesToExpand);
            Set expandedNodeIDs = wbsModel.getExpandedNodeIDs();
            expandedNodeIDs.addAll(nodeIDs);
            wbsModel.setExpandedNodeIDs(expandedNodeIDs);

            // update selection
            selectRows(wbsModel.getRowsForNodes(nodesToExpand));
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(notEmpty(selectedRows));
        }
    }
    final ExpandAllAction EXPAND_ALL_ACTION = new ExpandAllAction();


    /** An action to perform a "collapse" operation */
    private class CollapseAction extends AbstractAction implements
            EnablementCalculation {

        public CollapseAction() {
            super(resources.getString("Edit.Collapse"),
                    IconFactory.getCollapseIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_EXPANSION);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ALT));
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0) return;
            List<WBSNode> selectedNodes = wbsModel.getNodesForRows(rows, true);

            // stop editing the current cell.
            stopCellEditing();

            // if only one row is selected, and it doesn't need collapsing,
            // transfer the selection to the parent instead and exit.
            if (rows.length == 1 //
                    && (wbsModel.isLeaf(selectedNodes.get(0)) //
                    || !selectedNodes.get(0).isExpanded())) {
                WBSNode parentNode = wbsModel.getParent(selectedNodes.get(0));
                selectAndShowNode(parentNode);
                return;
            }

            // collapse selected nodes
            Set nodeIDs = getNodeIDs(selectedNodes);
            Set expandedNodeIDs = wbsModel.getExpandedNodeIDs();
            expandedNodeIDs.removeAll(nodeIDs);
            wbsModel.setExpandedNodeIDs(expandedNodeIDs);

            // update selection
            selectRows(wbsModel.getRowsForNodes(selectedNodes));
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(notJustRoot(selectedRows));
        }
    }
    final CollapseAction COLLAPSE_ACTION = new CollapseAction();


    /** An action to perform a "collapse all" operation */
    private class CollapseAllAction extends AbstractAction implements
            EnablementCalculation {

        public CollapseAllAction() {
            super(resources.getString("Edit.Collapse_All"),
                    IconFactory.getCollapseAllIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_EXPANSION);
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (rows == null || rows.length == 0)
                return;

            // stop editing the current cell.
            stopCellEditing();

            // collapse selected nodes and all descendants
            List selectedNodes = wbsModel.getNodesForRows(rows, false);
            List nodesToCollapse = new ArrayList(selectedNodes);
            for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
                WBSNode node = (WBSNode) i.next();
                WBSNode[] descendants = wbsModel.getDescendants(node);
                nodesToCollapse.addAll(Arrays.asList(descendants));
            }

            Set nodeIDs = getNodeIDs(nodesToCollapse);
            Set expandedNodeIDs = wbsModel.getExpandedNodeIDs();
            expandedNodeIDs.removeAll(nodeIDs);
            wbsModel.setExpandedNodeIDs(expandedNodeIDs);

            // update selection
            selectRows(wbsModel.getRowsForNodes(nodesToCollapse));
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(selectedRows != null && selectedRows.length > 0);
        }
    }
    final CollapseAllAction COLLAPSE_ALL_ACTION = new CollapseAllAction();


    /** An action to perform a "move up" operation */
    private class MoveUpAction extends AbstractAction implements
            EnablementCalculation {

        public MoveUpAction() {
            super(resources.getString("Edit.Move_Up"), IconFactory.getMoveUpIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_STRUCTURE);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_UP, ALT));
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (!isParentAndChildren(rows)) return;

            // stop editing the current cell.
            stopCellEditing();

            WBSNode node = wbsModel.getNodeForRow(rows[0]);
            int[] finalRows = wbsModel.moveNodeUp(node);
            if (finalRows != null) {
                UndoList.madeChange(WBSJTable.this, (String) getValue(NAME));
                selectRows(finalRows, true);
            }
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                && !isFiltered()
                && selectedRows != null
                && isParentAndChildren(selectedRows)
                && selectedRows[0] > 1);
        }
    }
    final MoveUpAction MOVEUP_ACTION = new MoveUpAction();


    /** An action to perform a "move down" operation */
    private class MoveDownAction extends AbstractAction implements
            EnablementCalculation {

        public MoveDownAction() {
            super(resources.getString("Edit.Move_Down"),
                    IconFactory.getMoveDownIcon());
            putValue(WBS_ACTION_CATEGORY, WBS_ACTION_CATEGORY_STRUCTURE);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ALT));
            enablementCalculations.add(this);
        }
        public void actionPerformed(ActionEvent e) {
            if (disableEditing)
                return;

            // get a list of the currently selected rows.
            int[] rows = getSelectedRows();
            if (!isParentAndChildren(rows)) return;

            // stop editing the current cell.
            stopCellEditing();

            WBSNode node = wbsModel.getNodeForRow(rows[0]);
            int[] finalRows = wbsModel.moveNodeDown(node);
            if (finalRows != null) {
                UndoList.madeChange(WBSJTable.this, (String) getValue(NAME));
                selectRows(finalRows, true);
            }
        }
        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(!disableEditing
                && !isFiltered()
                && selectedRows != null
                && isParentAndChildren(selectedRows)
                && selectedRows[0] > 0
                && selectedRows[selectedRows.length-1] + 1 < getRowCount());
        }
    }
    final MoveDownAction MOVEDOWN_ACTION = new MoveDownAction();


    /**
     * Handle a "left arrow" keystroke in a WBS data table. This will move left
     * one cell unless we are already in column 0; then it will start editing of
     * the WBS node.
     */
    private class MoveLeftOrStartEditing extends AbstractAction {

        private Action startEditingAction;

        private ActionListener originalMoveLeftAction;

        void install() {
            KeyStroke leftKeyStroke = KeyStroke.getKeyStroke("LEFT");
            originalMoveLeftAction = getActionForKeyStroke(leftKeyStroke);
            startEditingAction = getActionMap().get("startEditing");

            getInputMap().put(leftKeyStroke, "moveLeftOrStartEditing");
            getActionMap().put("moveLeftOrStartEditing", this);
        }

        public void actionPerformed(ActionEvent e) {
            if (getSelectedColumn() == 0)
                startEditingAction.actionPerformed(e);
            else
                originalMoveLeftAction.actionPerformed(e);
        }

    }


    public boolean isFiltered() {
        return FILTER_ACTION.isActive();
    }

    final WBSFilterAction FILTER_ACTION = new WBSFilterAction(this);



    /** Recalculate enablement following changes in selection */
    private final class SelectionListener implements ListSelectionListener {
        public SelectionListener() {
            recalculateEnablement();
        }
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting())
                recalculateEnablement();
        }
    }
    final SelectionListener SELECTION_LISTENER = new SelectionListener();



    /** Display a hand cursor when the mouse is over a node icon */
    private final class MotionListener implements MouseMotionListener {
        private Cursor handCursor =
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        private Cursor textCursor =
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        public void mouseDragged(MouseEvent e) {}
        public void mouseMoved(MouseEvent e) {
            setCursor(getCursorToDisplay(e.getPoint()));
        }
        private Cursor getCursorToDisplay(Point p) {
            if (disableEditing)
                return null;

            int column = columnAtPoint(p);
            if (column != 0) return null;
            int row = rowAtPoint(p);
            if (!wbsModel.isCellEditable(row, column)) return null;

            // ask the WBSNodeEditor to tell us which item the mouse is over
            switch (editor.getMouseOverRegion(p)) {
            case WBSNodeEditor.CLICKED_TEXT:
                return textCursor;

            case WBSNodeEditor.CLICKED_ICON:
                WBSNode overNode = wbsModel.getNodeForRow(row);
                if (overNode != null && wbsModel.isNodeTypeEditable(overNode))
                    return handCursor;
            }

            return null;
        }
    }
    final MotionListener MOTION_LISTENER = new MotionListener();



    // convenience declarations
    private static final int SHIFT = Event.SHIFT_MASK;
    private static final int CTRL  = (MacGUIUtils.isMacOSX()
            ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
    private static final int ALT = (MacGUIUtils.isMacOSX()
            ? InputEvent.META_DOWN_MASK : InputEvent.ALT_DOWN_MASK);
    public static final String WBS_ACTION_CATEGORY = ActionCategoryComparator.ACTION_CATEGORY;
    public static final String WBS_ACTION_CATEGORY_CLIPBOARD = "WBSClipboard";
    public static final String WBS_ACTION_CATEGORY_INDENT = "indent";
    public static final String WBS_ACTION_CATEGORY_EXPANSION = "expansion";
    public static final String WBS_ACTION_CATEGORY_STRUCTURE = "structure";
    public static final String WBS_ACTION_CATEGORY_WORKFLOW = "workflow";

}
