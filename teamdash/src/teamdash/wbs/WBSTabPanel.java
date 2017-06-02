// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ColumnSelectorIcon;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.RobustFileWriter;

import teamdash.ActionCategoryComparator;
import teamdash.XMLUtils;
import teamdash.hist.BlameCaretPos;
import teamdash.hist.BlameData;
import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.CustomColumnListener;
import teamdash.wbs.columns.CustomColumnManager;
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.CustomColumnsAction;
import teamdash.wbs.columns.WBSNodeColumn;

/** Class to display the WBS editor panel
 */
public class WBSTabPanel extends JLayeredPane implements
        TeamMemberList.InitialsListener, CustomColumnListener,
        MergeConflictHyperlinkHandler
{

    private static final String TABXML_EXTENSION = ".tabxml";
    private static final String ID_ATTRIBUTE = "id";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String COLUMN_ELEMENT = "column";
    private static final String TAB_ELEMENT = "tab";
    private static final String WBS_TABS_ELEMENT = "wbstabs";

    /** The order in which the Actions should appear in the Edit menu. */
    private static final List<String> editMenuActionOrder =
        Arrays.asList(UndoList.UNDO_ACTION_CATEGORY_UNDO_REDO,
                      WBSJTable.WBS_ACTION_CATEGORY_CLIPBOARD,
                      DataJTable.DATA_ACTION_CATEGORY_CLIPBOARD,
                      WBSJTable.WBS_ACTION_CATEGORY_INDENT,
                      WBSJTable.WBS_ACTION_CATEGORY_EXPANSION,
                      WBSJTable.WBS_ACTION_CATEGORY_STRUCTURE);

    public static final String TEAM_MEMBER_PLAN_TIMES_ID = "TeamMemberTimes";
    public static final String TEAM_MEMBER_ACTUAL_TIMES_ID = "TeamMemberActualTimes";
    public static final String CUSTOM_COLUMNS_ID = "CustomColumnSet";

    public static final String CUSTOM_TABS_SYS_PROP = "teamdash.wbs.customTabURLs";
    private static final Resources resources = Resources.getDashBundle("WBSEditor.CustomTabs");

    WBSJTable wbsTable;
    DataJTable dataTable;
    JScrollPane scrollPane;
    JTabbedPane tabbedPane;
    JSplitPane splitPane;
    JToolBar toolBar;
    WBSFindAction findAction;
    JFileChooser fileChooser;
    UndoList undoList;
    ArrayList<TableColumnModel> tableColumnModels = new ArrayList();
    TableColumnModel customColumnsTab;
    int customColumnInsertPosDelta;
    boolean customTabsDirty = false;
    GridBagLayout layout;
    ArrayList tabProperties = new ArrayList();
    List enablementCalculations = new LinkedList();

    /** Create a WBSTabPanel */
    public WBSTabPanel(WBSModel wbs, DataTableModel data,
            TeamProcess teamProcess, WorkflowWBSModel workflows,
            TaskIDSource idSource) {
        this(wbs, data, teamProcess.getIconMap(),
             teamProcess.getNodeTypeMenu(), workflows, idSource);
    }

    /** Create a WBSTabPanel */
    public WBSTabPanel(WBSModel wbs, DataTableModel data, Map iconMap,
            JMenu iconMenu, WorkflowWBSModel workflows, TaskIDSource idSource) {
        setLayout(layout = new GridBagLayout());

        undoList = new UndoList(wbs);
        undoList.setForComponent(this);
        undoList.setMemorySensitive(true);

        // build the components to display in this panel
        makeTables(wbs, data, iconMap, iconMenu, workflows, idSource);
        makeSplitter();
        makeScrollPane();
        makeTabbedPane();
        makeToolBar();
        installKeyboardShortcuts();

        // manually set the initial divider location, to trigger the
        // size coordination logic.
        splitPane.setDividerLocation((int) toolBar.getPreferredSize().getWidth() + 10);
    }

    public void setReadOnly(boolean readOnly) {
        boolean editable = !readOnly;
        dataTable.setEditingEnabled(editable);
        wbsTable.setEditingEnabled(editable);
    }

    public void stopCellEditing() {
        UndoList.stopCellEditing(this);
    }

    protected boolean isTabEditable(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabProperties.size())
            return false;
        else
            return ((TabProperties) tabProperties.get(tabIndex)).isEditable();
    }

    protected boolean isTabProtected(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabProperties.size())
            return false;
        else
            return ((TabProperties) tabProperties.get(tabIndex)).isProtected();
    }

    private boolean editableTabsExist() {
        for (Iterator iter = tabProperties.iterator(); iter.hasNext();) {
            TabProperties properties = (TabProperties) iter.next();
            if (properties.isEditable())
                return true;
        }

        return false;
    }

    public int addTab(
            String tabName,
            String columnIDs[],
            String[] columnNames) {
        return addTab(tabName, columnIDs, columnNames, false, false);
    }

    /** Add a tab to the tab panel
     * @param tabName The name to display on the tab
     * @param columnNames The columns to display when this tab is selected
     */
    public int addTab(
        String tabName,
        String columnIDs[],
        String[] columnNames,
        boolean isEditable,
        boolean isProtected) {

        DataTableModel tableModel = (DataTableModel) dataTable.getModel();
        TableColumnModel columnModel = new DefaultTableColumnModel();

        for (int i = 0; i < columnIDs.length; i++) {
            if (columnIDs[i] == null)
                continue;
            else if (TEAM_MEMBER_PLAN_TIMES_ID.equals(columnIDs[i])) {
                tableModel.addTeamMemberPlanTimes(columnModel);
                isProtected = true;
            } else if (TEAM_MEMBER_ACTUAL_TIMES_ID.equals(columnIDs[i])) {
                tableModel.addTeamMemberActualTimes(columnModel);
                isProtected = true;
            } else if (CUSTOM_COLUMNS_ID.equals(columnIDs[i])) {
                customColumnsTab = columnModel;
                customColumnInsertPosDelta = columnIDs.length - i - 1;
                tableModel.addCustomColumns(columnModel);
            } else {
                try {
                    TableColumn tableColumn =
                        new DataTableColumn(tableModel, columnIDs[i]);
                    if (columnNames != null && columnNames[i] != null)
                        // maybe change the name of the column
                        tableColumn.setHeaderValue(columnNames[i]);
                    columnModel.addColumn(tableColumn);
                } catch (IllegalArgumentException e) {
                    //ignore columns not found
                }
            }
        }
        int tabIndex = addTab(tabName, columnModel, new TabProperties(isEditable, isProtected));

        return tabIndex;
    }

    protected int addTab(String tabName, TableColumnModel columnModel, TabProperties properties) {
        // add the newly created table model to the tableColumnModels list
        tableColumnModels.add(columnModel);

        // add tab properties
        tabProperties.add(properties);

        // insert the new tab immediately before the "Add Tab" pseudotab.
        int tabIndex = tableColumnModels.size() - 1;
        tabbedPane.insertTab(tabName, null,
            new EmptyComponent(new Dimension(10, 10)), null, tabIndex);

        if (properties.isEditable())
            EXPORT_TABS_ACTION.setEnabled(true);

        // If this is the first tab, trigger a tab selection event to install
        // the tableColumnModel we just created.
        if (tabIndex == 0)
            tabbedPane.setSelectedIndex(0);

        return tabIndex;
    }

    protected void removeTab(int index) {
        tabProperties.remove(index);
        tableColumnModels.remove(index);
        if (tabbedPane.getSelectedIndex() == index)
            tabbedPane.setSelectedIndex(index-1);
        tabbedPane.remove(index);
    }

    public LinkedHashMap<String, TableColumnModel> getTabData() {
        LinkedHashMap<String, TableColumnModel> result =
            new LinkedHashMap<String, TableColumnModel>();

        for (int i = 0; i < tabbedPane.getTabCount()-1; i++) {
            String tabName = tabbedPane.getTitleAt(i);
            String key = tabName;
            int j = 0;
            while (result.containsKey(key)) {
                key = tabName + " (" + (++j) + ")";
            }

            TableColumnModel colModel = (TableColumnModel) tableColumnModels
                    .get(i);
            result.put(key, colModel);
        }
        return result;
    }

    public List<String> getSelectedColumnIDs() {
        int[] cols = dataTable.getSelectedColumns();
        List<String> result = new ArrayList<String>(cols.length);
        if (cols.length == 0 || cols.length == dataTable.getColumnCount())
            result.add(WBSNodeColumn.COLUMN_ID);
        for (int oneCol : cols) {
            result.add((String) dataTable.getColumnModel().getColumn(oneCol)
                    .getIdentifier());
        }
        return result;
    }

    public List<String> getActiveTabColumnIDs() {
        List<String> result = new ArrayList(dataTable.getColumnCount() + 1);
        result.add(WBSNodeColumn.COLUMN_ID);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            result.add((String) dataTable.getColumnModel().getColumn(i)
                    .getIdentifier());
        }
        return result;
    }

    public enum FindResult { NotFound, Found, Wrapped }

    public FindResult findNextMatch(StringTest pattern, boolean searchForward,
            boolean searchByColumns, List<String> columns) {
        // find the node position where the search should begin
        WBSNode selectedNode = getSelectedNode();
        List<WBSNode> wbsNodes = (wbsTable.FILTER_ACTION.isActive()
                        ? wbsTable.wbsModel.getNonHiddenWbsNodes()
                        : wbsTable.wbsModel.getWbsNodes());
        int selectedNodePos = Math.max(0, wbsNodes.indexOf(selectedNode));

        // find the column where the search should begin
        String selectedColumnID = getSelectedColumnID();
        if (columns == null)
            columns = getBlameColumnIDs();
        int selectedColumnPos = Math.max(0, columns.indexOf(selectedColumnID));

        // cache column indexes for better performance
        DataTableModel dataModel = (DataTableModel) dataTable.getModel();
        int[] columnIdx = new int[columns.size()];
        for (int i = columns.size(); i-- > 0;)
            columnIdx[i] = dataModel.findColumn(columns.get(i));

        // prepare info needed for the search
        int nodePos = selectedNodePos;
        int columnPos = selectedColumnPos;
        int increment = searchForward ? +1 : -1;
        boolean wrapped = false;

        while (true) {
            if (searchByColumns) {
                // search forward/backward for the next node.
                nodePos += increment;

                // if we run off the end of the node list, wrap and then move
                // to another column.
                if (outOfRange(wbsNodes, nodePos)) {
                    nodePos = wrapPos(wbsNodes, nodePos);
                    columnPos += increment;

                    // If we run off the end of the column list, wrap columns
                    if (outOfRange(columns, columnPos)) {
                        columnPos = wrapPos(columns, columnPos);
                        wrapped = true;
                    }
                }

            } else {
                // search forward/backward for the next column.
                columnPos += increment;

                // if we run off the end of the column list, wrap and then move
                // to another node.
                if (outOfRange(columns, columnPos)) {
                    columnPos = wrapPos(columns, columnPos);
                    nodePos += increment;

                    // if we run off the end of the node list, wrap nodes
                    if (outOfRange(wbsNodes, nodePos)) {
                        nodePos = wrapPos(wbsNodes, nodePos);
                        wrapped = true;
                    }
                }
            }

            // see if the given cell is a match
            WBSNode oneNode = wbsNodes.get(nodePos);
            int oneColIdx = columnIdx[columnPos];
            Object cellValue = dataModel.getValueAt(oneNode, oneColIdx);
            if (cellValue != null && pattern.test(cellValue.toString())) {
                wbsTable.selectAndShowNode(oneNode);
                selectColumn(columns.get(columnPos));
                return wrapped ? FindResult.Wrapped : FindResult.Found;
            }

            // if we wrapped all the way around to the starting location,
            // return false to indicate that no match was found
            if (nodePos == selectedNodePos && columnPos == selectedColumnPos)
                return FindResult.NotFound;
        }
    }

    private WBSNode getSelectedNode() {
        int selRow = Math.max(0, wbsTable.getSelectedRow());
        WBSNode selNode = wbsTable.wbsModel.getNodeForRow(selRow);
        return selNode;
    }

    private String getSelectedColumnID() {
        int[] cols = dataTable.getSelectedColumns();
        if (cols.length == 0 || cols.length == dataTable.getColumnCount()) {
            return WBSNodeColumn.COLUMN_ID;
        } else {
            return (String) dataTable.getColumnModel().getColumn(cols[0])
                    .getIdentifier();
        }
    }

    private boolean outOfRange(List list, int pos) {
        return pos < 0 || pos >= list.size();
    }

    private int wrapPos(List list, int pos) {
        return (pos + list.size()) % list.size();
    }

    public boolean displayHyperlinkedItem(String item) {
        String[] parts = item.split("/", 2);
        int wbsId = Integer.parseInt(parts[0]);
        return wbsTable.selectAndShowNode(wbsId) //
                && selectColumn(parts.length > 1 ? parts[1] : null);
    }

    public boolean selectColumn(String columnID) {
        // if the "node" column was requested, transfer focus to the WBS tree
        if (columnID == null || WBSNodeColumn.COLUMN_ID.equals(columnID)) {
            dataTable.selectColumn(0);
            dataTable.getActionMap().get("focusTree").actionPerformed(null);
            dataTable.getColumnModel().getSelectionModel().clearSelection();
            return true;
        }

        // otherwise, try selecting the given column in the data table. Look
        // for the column on the current tab first, then on the next tab to the
        // right, and so on.
        int numTabs = tableColumnModels.size();
        for (int i = 0; i < numTabs; i++) {
            int tabPos = (tabbedPane.getSelectedIndex() + i) % numTabs;
            TableColumnModel columns = tableColumnModels.get(tabPos);
            try {
                int columnPos = columns.getColumnIndex(columnID);

                if (i != 0)
                    tabbedPane.setSelectedIndex(tabPos);
                dataTable.requestFocusInWindow();
                dataTable.selectColumn(columnPos);
                return true;
            } catch (IllegalArgumentException noSuchColumn) {
                // if the column was not found on this tab, the getColumnIndex
                // method will throw an IllegalArgumentException. Catch the
                // exception so we can try the column model for the next tab.
            }
        }

        return false;
    }

    public void setBlameData(BlameData blameData) {
        wbsTable.setBlameData(blameData);
        dataTable.setBlameData(blameData);
    }

    public BlameCaretPos findNextAnnotation(BlameCaretPos currentCaret,
            boolean searchForward, boolean searchByColumns) {
        if (wbsTable.blameData == null)
            return null;
        else
            return wbsTable.blameData.findNextAnnotation(
                wbsTable.wbsModel.getWbsNodes(), getBlameColumnIDs(),
                currentCaret, searchForward, searchByColumns);
    }

    private List<String> getBlameColumnIDs() {
        if (blameColumnIDs == null) {
            Set<String> result = new LinkedHashSet<String>();
            result.add(WBSNodeColumn.COLUMN_ID);
            for (TableColumnModel tcm : tableColumnModels) {
                for (int i = 0; i < tcm.getColumnCount(); i++) {
                    result.add(tcm.getColumn(i).getIdentifier().toString());
                }
            }
            blameColumnIDs = new ArrayList<String>(result);
        }
        return blameColumnIDs;
    }
    private List<String> blameColumnIDs;


    /** Get a list of file-related actions for the work breakdown structure */
    public Action[] getFileActions() {
        List<Action> result = new ArrayList<Action>();
        try {
            Class clazz = Class.forName("teamdash.wbs.excel.SaveAsExcelAction");
            Action saveAsExcelAction = (Action) clazz.newInstance();
            saveAsExcelAction.putValue(DataJTable.class.getName(), dataTable);
            saveAsExcelAction.putValue(WBSTabPanel.class.getName(), this);
            result.add(saveAsExcelAction);
        } catch (Throwable t) {
            // a class not found error will be thrown if the apache libraries
            // are not available - degrade gracefully
        }
        return result.toArray(new Action[result.size()]);
    }

    /** Get a list of actions for editing the work breakdown structure */
    public Action[] getEditingActions() {
        List<Action> result = new ArrayList<Action>();

        result.add(undoList.getUndoAction());
        result.add(undoList.getRedoAction());
        result.addAll(Arrays.asList(wbsTable.getEditingActions()));
        result.addAll(Arrays.asList(dataTable.getEditingActions()));
        result.add(findAction);
        result.add(findAction.replaceAction);
        result.add(wbsTable.FILTER_ACTION);

        Comparator<Action> comparator = new ActionCategoryComparator(editMenuActionOrder);
        Collections.sort(result, comparator);

        return result.toArray(new Action[0]);
    }


    /** Return a list of workflow-related actions */
    public Action[] getWorkflowActions(WBSModel workflows) {
        return wbsTable.getWorkflowActions(workflows);
    }

    /** Get an action capable of inserting a workflow into the work breakdown
     *  structure */
    public Action getInsertWorkflowAction(WBSModel workflows) {
        return wbsTable.getInsertWorkflowAction(workflows);
    }


    /** Get an action capable of inserting a workflow into the work breakdown
     *  structure */
    public Action[] getMasterActions(TeamProject project) {
        return wbsTable.getMasterActions(project);
    }

    public void addChangeListener(ChangeListener l) {
        undoList.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        undoList.removeChangeListener(l);
    }

    /**
     * We only keep one Listeners list and we keep it in the UndoList. The
     *  UndoList will automatically notify listeners when a change is made to
     *  the model. However, if we want to notify the listeners when a change
     *  related to the view occurs, we have to "force" the notification.
     */
    private void notifyAllListeners() {
        undoList.notifyAllChangeListeners();
    }

    private interface EnablementCalculation {
        public void recalculateEnablement(int selectedTabIndex);
    }

    private TableColumnModel copyColumnsDeep(TableColumnModel tableColumnModel) {
        TableColumnModel newTableColumnModel = new DefaultTableColumnModel();
        for (Enumeration columns = tableColumnModel.getColumns(); columns.hasMoreElements();) {
            DataTableColumn existingColumn = (DataTableColumn) columns.nextElement();
            newTableColumnModel.addColumn(new DataTableColumn(existingColumn));
        }
        return newTableColumnModel;
    }

    public Action[] getTabActions() {
        return new Action[] {NEW_TAB_ACTION, DUPLICATE_TAB_ACTION,
                DELETE_TAB_ACTION, IMPORT_TABS_ACTION, EXPORT_TABS_ACTION,
                CHANGE_TAB_COLUMNS_ACTION, makeCustomColumnsAction(),
                RENAME_TAB_ACTION};
    }

    private CustomColumnsAction makeCustomColumnsAction() {
        CustomColumnManager colMgr = wbsTable.dataModel.getCustomColumnManager();
        colMgr.addCustomColumnListener(this);
        if (!WBSPermissionManager.hasPerm("wbs.columns", "2.3.1.2"))
            return null;
	else
            return new CustomColumnsAction(this, wbsTable.dataModel, colMgr);
    }

    private class NewTabAction extends AbstractAction {
        public NewTabAction() {
            super(resources.getString("New_Tab.Menu"),
                    IconFactory.getNewTabIcon());
        }

        public void actionPerformed(ActionEvent e) {
            String tabName = JOptionPane.showInputDialog(tabbedPane,
                resources.getString("New_Tab.Prompt"),
                resources.getString("New_Tab.Title"),
                JOptionPane.QUESTION_MESSAGE);
            if (null == tabName)
                return;

            customTabsDirty = true;
            int tabIndex = addTab(tabName, new DefaultTableColumnModel(), new TabProperties(true, false));
            tabbedPane.setSelectedIndex(tabIndex);
            showColumnSelector();
            notifyAllListeners();
        }
    }
    final NewTabAction NEW_TAB_ACTION = new NewTabAction();

    private class RenameTabAction extends AbstractAction implements EnablementCalculation {
        public RenameTabAction() {
            super(resources.getString("Rename_Tab.Menu"));
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            String tabName = JOptionPane.showInputDialog(tabbedPane,
                resources.format("Rename_Tab.Prompt_FMT",
                    tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())),
                resources.getString("Rename_Tab.Title"),
                JOptionPane.QUESTION_MESSAGE);
            if (null == tabName)
                return;

            customTabsDirty = true;
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), tabName);
            notifyAllListeners();
        }

        public void recalculateEnablement(int selectedTabIndex) {
            setEnabled(isTabEditable(selectedTabIndex));
        }
    }
    final RenameTabAction RENAME_TAB_ACTION = new RenameTabAction();

    private class DuplicateTabAction extends AbstractAction implements EnablementCalculation {
        public DuplicateTabAction() {
            super(resources.getString("Duplicate_Tab.Menu"),
                    IconFactory.getDuplicateTabIcon());
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            String tabName = JOptionPane.showInputDialog(tabbedPane,
                resources.getString("Duplicate_Tab.Prompt"),
                resources.getString("Duplicate_Tab.Title"),
                JOptionPane.QUESTION_MESSAGE);
            if (null == tabName)
                return;

            customTabsDirty = true;
            TableColumnModel columns = copyColumnsDeep(
                    (TableColumnModel) tableColumnModels.get(tabbedPane.getSelectedIndex()));
            int tabIndex = addTab(tabName, columns, new TabProperties(true, false));
            tabbedPane.setSelectedIndex(tabIndex);
            showColumnSelector();
            notifyAllListeners();
        }

        public void recalculateEnablement(int selectedTabIndex) {
            setEnabled(!isTabProtected(selectedTabIndex));
        }
    }
    final DuplicateTabAction DUPLICATE_TAB_ACTION = new DuplicateTabAction();

    private class DeleteTabAction extends AbstractAction implements EnablementCalculation {
        public DeleteTabAction() {
            super(resources.getString("Delete_Tab.Menu"),
                    IconFactory.getRemoveTabIcon());
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            int confirm = JOptionPane.showConfirmDialog(tabbedPane,
                resources.format("Delete_Tab.Prompt_FMT",
                    tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())),
                resources.getString("Delete_Tab.Title"),
                JOptionPane.OK_CANCEL_OPTION);
            if (confirm == JOptionPane.OK_OPTION) {
                removeTab(tabbedPane.getSelectedIndex());
                customTabsDirty = true;

                if (!editableTabsExist())
                    EXPORT_TABS_ACTION.setEnabled(false);

                notifyAllListeners();
            }
        }

        public void recalculateEnablement(int selectedTabIndex) {
            setEnabled(isTabEditable(selectedTabIndex));
        }
    }
    final DeleteTabAction DELETE_TAB_ACTION = new DeleteTabAction();

    private class ChangeTabColumnsAction extends AbstractAction implements EnablementCalculation {
        public ChangeTabColumnsAction() {
            super(resources.getString("Change_Columns.Menu"),
                    IconFactory.getAddColumnIcon());
            enablementCalculations.add(this);
        }

        public void actionPerformed(ActionEvent e) {
            customTabsDirty = true;
            showColumnSelector();
            notifyAllListeners();
        }

        public void recalculateEnablement(int selectedTabIndex) {
            setEnabled(isTabEditable(selectedTabIndex));
        }
    }
    final ChangeTabColumnsAction CHANGE_TAB_COLUMNS_ACTION = new ChangeTabColumnsAction();

    private class ExportTabsAction extends AbstractAction {
        public ExportTabsAction() {
            super(resources.getString("Export_Tabs.Menu"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                File file = getFile();
                if (null != file)
                    saveTabs(file);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(tabbedPane,
                    exception.getMessage(),
                    resources.getString("Export_Tabs.Dialog_Title"),
                    JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
        }

        private File getFile() {
            File file = null;
            JFileChooser chooser = getFileChooser();
            int returnValue = chooser.showSaveDialog(tabbedPane);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();

                // check for an extension and add it if it is missing
                if (file.getName().indexOf('.') == -1)
                    file = new File(file.getParentFile(), file.getName() + TABXML_EXTENSION);

                if (file.exists()) {
                    if (file.canWrite()) {
                        int option = JOptionPane.showConfirmDialog(tabbedPane,
                            resources.formatStrings("Export_Tabs.Replace_FMT",
                                file.getName()),
                            resources.getString("Export_Tabs.Dialog_Title"),
                            JOptionPane.YES_NO_OPTION);

                        if (option == JOptionPane.NO_OPTION)
                            file = getFile();
                    } else {
                        JOptionPane.showMessageDialog(tabbedPane,
                            resources.formatStrings(
                                "Export_Tabs.Cannot_Write_FMT", file.getName()),
                            resources.getString("Export_Tabs.Dialog_Title"),
                            JOptionPane.ERROR_MESSAGE);
                        file = getFile();
                    }
                }
            }

            return file;
        }
    }

    final ExportTabsAction EXPORT_TABS_ACTION = new ExportTabsAction();

    private class ImportTabsAction extends AbstractAction {

        public ImportTabsAction() {
            super(resources.getString("Import_Tabs.Menu"));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                File file = getFile();
                if (null != file) {
                    loadTabs(file);
                    customTabsDirty = true;
                    notifyAllListeners();
                }
            } catch (LoadTabsException exception) {
                JOptionPane.showMessageDialog(tabbedPane,
                    exception.getMessage(),
                    resources.getString("Import_Tabs.Error_Title"),
                    JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
        }

        private File getFile() {
            File file = null;
            JFileChooser chooser = getFileChooser();
            int returnValue = chooser.showOpenDialog(tabbedPane);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (file.exists()) {
                    if (!file.canRead()) {
                        JOptionPane.showMessageDialog(tabbedPane,
                            resources.formatStrings(
                                "Import_Tabs.Cannot_Read_FMT", file.getName()),
                            resources.getString("Import_Tabs.Error_Title"),
                            JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(tabbedPane,
                        resources.formatStrings("Import_Tabs.No_Such_File_FMT",
                            file.getName()),
                        resources.getString("Import_Tabs.Error_Title"),
                        JOptionPane.ERROR_MESSAGE);
                    file = getFile();
                }
            }

            return file;
        }
    }
    final ImportTabsAction IMPORT_TABS_ACTION = new ImportTabsAction();

    /** Listen for changes in team member initials, and disable undo. */
    public void initialsChanged(String oldInitials, String newInitials) {
        // other objects also listen for changes to team member initials,
        // including the object that applies those changes to the WBS data.
        // We don't have any insight into whether we are being notified
        // first or last, but we must wait until those other clients have
        // also received this message and done their work.  After that has
        // happened, we can tell the undo list to clear (it will take a new
        // data snapshot in the process).
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                undoList.clear();
            }});
    }



    public void columnAdded(String id, DataColumn newColumn) {
        // insert the column near the end of the Task Details tab
        if (customColumnsTab != null) {
            int pos = customColumnsTab.getColumnCount();
            customColumnsTab.addColumn(new DataTableColumn(wbsTable.dataModel,
                newColumn));
            customColumnsTab.moveColumn(pos, pos - customColumnInsertPosDelta);
            // switch to the custom columns tab so the user can see the change
            selectCustomColumnsTab();
            // fire a dirty change event (without recording an undoable action)
            undoList.notifyAllChangeListeners();
        }
    }

    public void columnChanged(String id, DataColumn oldColumn,
            DataColumn newColumn) {
        // Find columns with the given ID and replace them.
        for (TableColumnModel tcm : tableColumnModels) {
            for (int col = tcm.getColumnCount(); col-- > 0;) {
                TableColumn column = tcm.getColumn(col);
                if (id.equals(column.getIdentifier())) {
                    tcm.removeColumn(column);
                    tcm.addColumn(new DataTableColumn(wbsTable.dataModel,
                            newColumn));
                    tcm.moveColumn(tcm.getColumnCount() - 1, col);
                }
            }
        }
        // switch to the custom columns tab so the user can see the change
        selectCustomColumnsTab();
        // fire a dirty change event (but without recording an undoable action)
        undoList.notifyAllChangeListeners();
    }

    public void columnDeleted(String id, DataColumn oldColumn) {
        // Find columns with the given ID and delete them.
        for (TableColumnModel tcm : tableColumnModels) {
            for (int col = tcm.getColumnCount(); col-- > 0;) {
                TableColumn column = tcm.getColumn(col);
                if (id.equals(column.getIdentifier()))
                    tcm.removeColumn(column);
            }
        }
        // fire a dirty change event (but without recording an undoable action)
        undoList.notifyAllChangeListeners();
    }

    private void selectCustomColumnsTab() {
        if (customColumnsTab != null && !currentlyReplacingCustomColumns) {
            int pos = tableColumnModels.indexOf(customColumnsTab);
            tabbedPane.setSelectedIndex(pos);
        }
    }


    public void replaceCustomColumns(CustomColumnSpecs columns) {
        CustomColumnManager colMgr = wbsTable.dataModel.getCustomColumnManager();
        try {
            currentlyReplacingCustomColumns = true;
            colMgr.replaceProjectSpecificColumns(columns);
        } finally {
            currentlyReplacingCustomColumns = false;
        }

        // rearrange the columns on the custom column tab so they appear in the
        // same order they were presented in the new set of specs
        if (customColumnsTab != null && !columns.isEmpty()) {
            int destPos = customColumnsTab.getColumnCount()
                    - customColumnInsertPosDelta - columns.size();
            for (String oneID : columns.keySet()) {
                int pos = customColumnsTab.getColumnIndex(oneID);
                if (pos != -1 && pos != destPos)
                    customColumnsTab.moveColumn(pos, destPos);
                destPos++;
            }
        }
    }

    private boolean currentlyReplacingCustomColumns = false;


    /**
     * Update the project-specific custom column model so its contents match the
     * order the columns appear in the UI
     */
    public void saveOrderOfProjectSpecificCustomColumns() {
        if (customColumnsTab == null)
            return;

        // get the IDs of project-specific custom columns
        CustomColumnManager colMgr = wbsTable.dataModel.getCustomColumnManager();
        Set<String> customColumnIDs = colMgr.getProjectSpecificColumnIDs();
        if (customColumnIDs.isEmpty())
            return;

        // find the order that columns appear on the custom columns tab
        List<String> columnOrder = new ArrayList<String>();
        for (int i = 0; i < customColumnsTab.getColumnCount(); i++) {
            Object oneID = customColumnsTab.getColumn(i).getIdentifier();
            if (customColumnIDs.contains(oneID))
                columnOrder.add((String) oneID);
        }

        // tell the custom column manager to rearrange the columns in its model
        colMgr.setOrderOfProjectSpecificColumns(columnOrder);
    }



    /** Create the JTables and perform necessary setup */
    private void makeTables(WBSModel wbs, DataTableModel data, Map iconMap,
            JMenu iconMenu, WorkflowWBSModel workflows, TaskIDSource idSource) {
        // create the WBS table to display the hierarchy
        wbsTable = new WBSJTable(wbs, iconMap, iconMenu, workflows, idSource);
        wbsTable.checkForActualDataOnDelete = true;
        wbsTable.dataModel = data;
        // create the table to display hierarchy data
        dataTable = new DataJTable(data);
        // link the tables together so they have the same scrolling behavior,
        // selection model, and row height.
        wbsTable.setScrollableDelegate(dataTable);
        wbsTable.setSelectionModel(dataTable.getSelectionModel());
        dataTable.setRowHeight(wbsTable.getRowHeight());
    }

    /** Create and install the splitter component. */
    private void makeSplitter() {
        splitPane =
            new MagicSplitter(JSplitPane.HORIZONTAL_SPLIT, false, 70, 70);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setDividerLocation(205);
        splitPane.addPropertyChangeListener(
            JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new DividerListener());
        //splitPane.setDividerSize(3);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = 10;
        c.insets.top = 25;
        c.insets.bottom = 1;
        add(splitPane);
        setLayer(splitPane, 30);
        layout.setConstraints(splitPane, c);
    }

    /** Create and install the scroll pane component. */
    private void makeScrollPane() {
        // create a vertical scroll bar
        scrollPane =
            new JScrollPane(
                dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // remove the borders from the scroll pane
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // make the WBS table the "row header view" of the scroll pane.
        scrollPane.setRowHeaderView(wbsTable);
        // don't paint over the splitter bar when we repaint.
        scrollPane.setOpaque(false);
        wbsTable.setOpaque(false);
        scrollPane.getRowHeader().setOpaque(false);

        // place a button in the top-right corner for column editing
        JButton colButton = new JButton(CHANGE_TAB_COLUMNS_ACTION);
        colButton.setBorder(new ChoppedEtchedBorder());
        colButton.setFocusable(false);
        colButton.setIcon(new ColumnSelectorIcon(true));
        colButton.setDisabledIcon(new ColumnSelectorIcon(false));
        colButton.setToolTipText(colButton.getText());
        colButton.setText(null);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, colButton);

        // add the scroll pane to the panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.left = c.insets.right = c.insets.bottom = 10;
        c.insets.top = 30;
        add(scrollPane);
        setLayer(scrollPane, 20);
        layout.setConstraints(scrollPane, c);
    }

    /** Create and install the tabbed pane component. */
    private void makeTabbedPane() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        // Create a "pseudo-tab" that displays the "Add Tab" icon.
        tabbedPane.addTab(null, IconFactory.getAddTabIcon(),
            new EmptyComponent(new Dimension(10, 10)),
            resources.getString("New_Tab.Tooltip"));

        tabbedPane.addChangeListener(new TabListener());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets.top = c.insets.bottom = c.insets.right = 0;
        c.insets.left = 215;
        add(tabbedPane);
        setLayer(tabbedPane, 10);
        layout.setConstraints(tabbedPane, c);
    }

    /** Create and install the tool bar component. */
    private void makeToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0,0,0,0));
        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());

        Action[] editingActions = wbsTable.getEditingActions();
        for (int i = 0;   i < editingActions.length;   i++)
            if (editingActions[i].getValue(Action.SMALL_ICON) != null)
                addToolbarButton(editingActions[i]);
        addToolbarButton(findAction = new WBSFindAction(this));
        addToolbarButton(wbsTable.FILTER_ACTION);
        wbsTable.FILTER_ACTION.setWbsTabPanel(this);
        addToolbarButton(wbsTable.TOGGLE_ENTER_BEHAVIOR_ACTION);

        // add the tool bar to the panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets.left = 10;
        c.insets.right = c.insets.bottom = c.insets.top = 0;
        add(toolBar);
        setLayer(toolBar, 0);
        layout.setConstraints(toolBar, c);
    }

    /** Add a button to the beginning of the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        int p = (MacGUIUtils.isMacOSX() ? 2 : 1);
        button.setMargin(new Insets(p,p,p,p));
        button.setFocusPainted(false);
        button.putClientProperty("hideActionText", Boolean.TRUE);
        String toolTip = (String) a.getValue(Action.NAME);
        if (toolTip == null || toolTip.length() == 0)
            toolTip = (String) a.getValue(Action.SHORT_DESCRIPTION);
        button.setToolTipText(toolTip);
        button.setText(null);
        IconFactory.setDisabledIcon(button);
        toolBar.add(button);
    }

    /** Listen for changes to the tab selection, and install the corresponding
     * table column model. */
    private final class TabListener implements ChangeListener {
        private int mostRecentlySelectedTab = -1;
        public void stateChanged(ChangeEvent e) {
            TableCellEditor editor = wbsTable.getCellEditor();
            if (editor != null) editor.stopCellEditing();
            editor = dataTable.getCellEditor();
            if (editor != null) editor.stopCellEditing();

            int whichTab = tabbedPane.getSelectedIndex();

            // Has the user just clicked on the "Add Tab" pseudo-tab?
            // if so, restore the most recently selected tab (because the
            // pseudo-tab can't really be activated), then run the "Add
            // New Tab" action.
            if (whichTab == tabbedPane.getTabCount()-1) {
                if (mostRecentlySelectedTab != -1) {
                    tabbedPane.setSelectedIndex(mostRecentlySelectedTab);
                    NEW_TAB_ACTION.actionPerformed(null);
                }
                return;
            }

            TableColumnModel newModel =
                (TableColumnModel) tableColumnModels.get(whichTab);
            dataTable.setColumnModel(newModel);
            mostRecentlySelectedTab = whichTab;

            for (Iterator i = enablementCalculations.iterator(); i.hasNext();) {
                EnablementCalculation calc = (EnablementCalculation) i.next();
                calc.recalculateEnablement(whichTab);
            }
        }
    }

    /** This component displays a splitter bar (along the lines of JSplitPane)
     * but doesn't display anything on either side of the bar. Instead, these
     * areas are transparent, allowing other components to show through.
     */
    private final class MagicSplitter extends JSplitPane {
        public MagicSplitter(
            int newOrientation,
            boolean newContinuousLayout,
            int firstCompMinSize,
            int secondCompMinSize) {
            super(
                newOrientation,
                newContinuousLayout,
                new EmptyComponent(
                    new Dimension(firstCompMinSize, firstCompMinSize)),
                new EmptyComponent(
                    new Dimension(secondCompMinSize, secondCompMinSize)));
            setOpaque(false);
        }
        /** Limit contains() to the area owned by the splitter bar.  This
         * allows mouse events (e.g. clicks, mouseovers) to "pass through"
         * our invisible component areas, to the real components underneath.
         */
        public boolean contains(int x, int y) {
            int l = getDividerLocation();
            int diff = x - l;
            return (diff > 0 && diff < getDividerSize());
        }
    }

    /** Listen for changes in the position of the divider, and resize other
     * objects in this panel appropriately
     */
    private final class DividerListener implements PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            // get the new location of the divider.
            int dividerLocation = ((Number) evt.getNewValue()).intValue();

            // resize the wbsTable to fit on the left side of the divider
            TableColumn col = wbsTable.getColumnModel().getColumn(0);
            col.setMinWidth(dividerLocation - 5);
            col.setMaxWidth(dividerLocation - 5);
            col.setPreferredWidth(dividerLocation - 5);
            // add an extra 20 pixels to the width of the JScrollPane's
            // row header, to allow space for the splitter bar.
            Dimension d = new Dimension(dividerLocation + 15, 100);
            wbsTable.setPreferredScrollableViewportSize(d);

            // resize the tabbed pane to fit on the right side of the divider.
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.weightx = c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets.left = dividerLocation + 10;
            c.insets.top = c.insets.bottom = c.insets.right = 0;
            layout.setConstraints(tabbedPane, c);

            // revalidate the layout of the tabbed panel.
            WBSTabPanel.this.revalidate();
        }
    }

    /** Display an invisible component with a certain minimum/preferred size.
    */
    private final class EmptyComponent extends JComponent {
        private Dimension d, m;
        public EmptyComponent(Dimension d) {
            this(d, new Dimension(3000, 3000));
        }
        public EmptyComponent(Dimension d, Dimension m) {
            this.d = d;
            this.m = m;
        }
        public Dimension getMaximumSize() {
            return m;
        }
        public Dimension getMinimumSize() {
            return d;
        }
        public Dimension getPreferredSize() {
            return d;
        }
        public boolean isOpaque() {
            return false;
        }
        public void paint(Graphics g) {
        }
    }

    private class ChoppedEtchedBorder extends EtchedBorder {

        public void paintBorder(Component c, Graphics g, int x, int y,
                int width, int height) {
            Shape clipping = g.getClip();
            g.setClip(x, y, width, height);
            super.paintBorder(c, g, x, y, width, height+1);
            g.setClip(clipping);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2,2,1,2);
        }
    }

    /**
     * Configure a number of keyboard shortcuts for navigating within the
     * contents of the WBS tab panel
     */
    private void installKeyboardShortcuts() {
        InputMap inputMap = dataTable.getInputMap();
        ActionMap actionMap = dataTable.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("HOME"), "focusTree");
        inputMap.put(KeyStroke.getKeyStroke("shift SPACE"), "focusTree");
        actionMap.put("focusTree", new TransferFocusToWbsNode());
        new MoveLeftFromDataCell().install(dataTable);

        inputMap = wbsTable.getInputMap();
        actionMap = wbsTable.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("END"), "lastDataCol");
        actionMap.put("lastDataCol", new TransferFocusToDataTable(true));
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "firstDataCol");
        actionMap.put("firstDataCol", new TransferFocusToDataTable(false));
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "startEditing");

        inputMap = this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, //
            MacGUIUtils.getCtrlModifier()), "focusTabs");
        this.getActionMap().put("focusTabs", new TransferFocusToTabs());
    }

    /** Move the keyboard focus to the WBS tree on the left of the splitter */
    private class TransferFocusToWbsNode extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            wbsTable.addColumnSelectionInterval(0, 0);
            wbsTable.requestFocusInWindow();
            if (wbsTable.blameSelectionListener != null)
                wbsTable.blameSelectionListener.valueChanged(null);
        }

    }

    /**
     * Handle a "left arrow" keystroke in the data table. This will move left
     * one cell unless we are already in column 0; then it will transfer focus
     * to the tree.
     */
    private class MoveLeftFromDataCell extends AbstractAction {

        private Action transferFocusToWbsNode;

        private ActionListener originalMoveLeftAction;

        void install(JTable t) {
            KeyStroke leftKeyStroke = KeyStroke.getKeyStroke("LEFT");
            originalMoveLeftAction = t.getActionForKeyStroke(leftKeyStroke);
            transferFocusToWbsNode = t.getActionMap().get("focusTree");

            t.getInputMap().put(leftKeyStroke, "moveLeftFromDataCell");
            t.getActionMap().put("moveLeftFromDataCell", this);
        }

        public void actionPerformed(ActionEvent e) {
            if (dataTable.getSelectedColumn() == 0)
                transferFocusToWbsNode.actionPerformed(e);
            else
                originalMoveLeftAction.actionPerformed(e);
        }

    }

    /** Move keyboard focus to the first or last column of the data table */
    private class TransferFocusToDataTable extends AbstractAction {
        boolean lastCol;

        public TransferFocusToDataTable(boolean lastCol) {
            this.lastCol = lastCol;
        }

        public void actionPerformed(ActionEvent e) {
            int col = (lastCol ? dataTable.getColumnCount() - 1 : 0);
            dataTable.getColumnModel().getSelectionModel().clearSelection();
            dataTable.addColumnSelectionInterval(col, col);
            dataTable.requestFocusInWindow();
        }

    }

    /** Transfer focus to the tabbed pane's selector tabs */
    private class TransferFocusToTabs extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            tabbedPane.requestFocusInWindow();
        }

    }

    /**
     * Display dialog to allow user to select columns to display on the current tab.
     */
    private void showColumnSelector() {
        WBSColumnSelectorDialog columnSelectorDialog = new WBSColumnSelectorDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                resources.getString("Change_Columns.Dialog_Title"),
                getAvailableTabColumns());
        columnSelectorDialog.setTableColumnModel((TableColumnModel) tableColumnModels.get(tabbedPane.getSelectedIndex()));
        columnSelectorDialog.setDialogMessage(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
        columnSelectorDialog.setLocationRelativeTo(this);
        columnSelectorDialog.setVisible(true);
    }

    /**
     * Build map of table column models for non-editable and non-protected tabs
     * @return Map
     */
    private Map getAvailableTabColumns() {
        Map tabColumnsMap = new LinkedHashMap();
        for (int i = 0; i < tabbedPane.getTabCount()-1; i++) {
            if (!isTabEditable(i) && !isTabProtected(i))
                tabColumnsMap.put(tabbedPane.getTitleAt(i), tableColumnModels.get(i));
        }
        return tabColumnsMap;
    }

    public void loadDefaultCustomTabs() {
        String sysProp = System.getProperty(CUSTOM_TABS_SYS_PROP);
        if (sysProp != null) {
            for (String url : sysProp.trim().split("\\s+")) {
                try {
                    loadTabs(new URL(url));
                } catch (Exception e) {
                    System.out.println("Could not load custom tabs from " + url);
                }
            }
        }
    }

    /**
     * Load custom tab definitions from file.
     * @param file
     * @throws LoadTabsException
     */
    public void loadTabs(File file) throws LoadTabsException {
        try {
            loadTabs(file.toURI().toURL());
        } catch (MalformedURLException exception) {
            LoadTabsException e = new LoadTabsException(
                    resources.getString("Import_Tabs.Error_Message"));
            e.initCause(exception);
            throw e;
        }
    }

    private void loadTabs(URL url) throws LoadTabsException {
        try {
            Document document = XMLUtils.parse(url.openStream());
            if (!WBS_TABS_ELEMENT.equals(document.getDocumentElement().getTagName()))
                throw new LoadTabsException(
                        resources.getString("Import_Tabs.Invalid_File"));

            // read xml
            NodeList tabNodes = document.getElementsByTagName(TAB_ELEMENT);
            if (tabNodes.getLength() == 0)
                throw new LoadTabsException(
                        resources.getString("Import_Tabs.Empty_File"));

            for (int i = 0; i < tabNodes.getLength(); i++) {
                Element tab = (Element) tabNodes.item(i);
                String tabTitle = tab.getAttribute(NAME_ATTRIBUTE);
                NodeList columnNodes = tab.getElementsByTagName(COLUMN_ELEMENT);
                String[] columnHeaders = new String[columnNodes.getLength()];
                String[] columnIDs = new String[columnNodes.getLength()];
                for (int j = 0; j < columnNodes.getLength(); j++) {
                    Element column = (Element) columnNodes.item(j);
                    columnHeaders[j] = column.getAttribute(NAME_ATTRIBUTE);
                    columnIDs[j] = column.getAttribute(ID_ATTRIBUTE);
                }
                addTab(tabTitle, columnIDs, columnHeaders, true, false);
            }

        } catch (LoadTabsException exception) {
            throw exception;
        } catch (SAXException saxe) {
            LoadTabsException e = new LoadTabsException(
                    resources.getString("Import_Tabs.Invalid_File"));
            e.initCause(saxe);
            throw e;
        } catch (Exception exception) {
            LoadTabsException e = new LoadTabsException(
                    resources.getString("Import_Tabs.Error_Message"));
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Delete all of our custom tabs, and load replacement tabs from a file.
     */
    public void replaceCustomTabs(File file) throws LoadTabsException {
        if (isTabEditable(tabbedPane.getSelectedIndex()))
            tabbedPane.setSelectedIndex(0);
        for (int i = tabbedPane.getTabCount()-1; i-- > 0; ) {
            if (isTabEditable(i))
                removeTab(i);
        }
        loadTabs(file);
        customTabsDirty = true;
    }

    /**
     * Save custom tab definitions to file.
     * @param file
     * @throws IOException
     */
    public void saveTabs(File file) throws IOException {
        try {
            RobustFileWriter out = new RobustFileWriter(file, "UTF-8");

            // write out xml
            out.write("<?xml version='1.0' encoding='utf-8'?>\n");
            out.write("<" + WBS_TABS_ELEMENT + " " + VERSION_ATTRIBUTE + "='" + getVersionNumber() + "'>\n");
            for (int i = 0; i < tabbedPane.getTabCount()-1; i++) {
                if (isTabEditable(i)) {
                    out.write("\t<" + TAB_ELEMENT + " " + NAME_ATTRIBUTE + "='"
                            + XMLUtils.escapeAttribute(tabbedPane.getTitleAt(i)) + "'>\n");
                    TableColumnModel tableColumnModel = (TableColumnModel)tableColumnModels.get(i);
                    for (Enumeration e = tableColumnModel.getColumns(); e.hasMoreElements();) {
                        TableColumn column = (TableColumn) e.nextElement();
                        out.write("\t\t<" + COLUMN_ELEMENT
                                + " " + NAME_ATTRIBUTE + "='" + XMLUtils.escapeAttribute(column.getHeaderValue().toString()) + "'"
                                + " " + ID_ATTRIBUTE + "='" + XMLUtils.escapeAttribute(column.getIdentifier().toString()) + "'/>\n");
                    }
                    out.write("\t</" + TAB_ELEMENT + ">\n");
                }
            }
            out.write("</" + WBS_TABS_ELEMENT + ">\n");
            out.close();
        } catch (Exception e) {
            IOException exception = new IOException(
                    resources.getString("Export_Tabs.Error_Message"));
            exception.initCause(e);
            throw exception;
        }
    }

    public void saveCustomTabsIfChanged(File file) throws IOException {
        if (customTabsDirty) {
            saveTabs(file);
            customTabsDirty = false;
        }
    }

    private static String getVersionNumber() {
        String result = null;
        try {
            result = WBSTabPanel.class.getPackage()
                    .getImplementationVersion();
        } catch (Exception e) {
        }
        return (result == null ? "####" : result);
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(TABFILE_FILTER);
            fileChooser.setMultiSelectionEnabled(false);
        }

        return fileChooser;
    }

    static FileFilter TABFILE_FILTER = new TabFileFilter();

    private static class TabFileFilter extends FileFilter {

        public boolean accept(File f) {
            return (f.isDirectory() ||
                    f.getName().endsWith(TABXML_EXTENSION));
        }

        public String getDescription() {
            return resources.getString("File_Type");
        }

    }

    public class LoadTabsException extends Exception {
        public LoadTabsException (String message) {
            super(message);
        }
    }

    private class TabProperties {
        private static final String TAB_PROTECTED_PROPERTY = "protected";
        private static final String TAB_EDITABLE_PROPERTY = "editable";

        private HashMap properties = new HashMap();

        public TabProperties(boolean editable, boolean protect) {
            setEditable(editable);
            setProtected(protect);
        }

        public boolean isEditable() {
            return ((Boolean) properties.get(TAB_EDITABLE_PROPERTY)).booleanValue();
        }

        public void setEditable(boolean editable) {
            properties.put(TAB_EDITABLE_PROPERTY, new Boolean(editable));
        }

        public boolean isProtected() {
            return ((Boolean) properties.get(TAB_PROTECTED_PROPERTY)).booleanValue();
        }

        public void setProtected(boolean protect) {
            properties.put(TAB_PROTECTED_PROPERTY, new Boolean(protect));
        }
    }
}
