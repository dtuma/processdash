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

package teamdash.wbs.columns;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.Timer;
import javax.swing.table.TableCellEditor;

import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.AbstractWBSModelMerger;
import teamdash.wbs.AnnotatedValue;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.ExternalSystemManager;
import teamdash.wbs.ExternalSystemManager.ExtNodeType;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSDataModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowUtil;
import teamdash.wbs.WorkflowWBSModel;

public class PhaseColumn extends AbstractDataColumn
    implements CalculatedDataColumn, CustomEditedColumn
{
    public static String COLUMN_ID = "Phase";

    private WBSDataModel dataModel;
    private WBSModel wbsModel;
    private WorkflowWBSModel workflows;
    private Set allowedTypes;
    private TableCellEditor cellEditor;

    public PhaseColumn(WBSDataModel dataModel, TeamProcess teamProcess,
            WorkflowWBSModel workflows) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.workflows = workflows;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Phase.Name");
        this.preferredWidth = 100;
        this.allowedTypes = new LinkedHashSet(
                teamProcess.getChoosableNodeTypes());
        setConflictAttributeName(AbstractWBSModelMerger.NODE_TYPE);
    }

    public Object getValueAt(WBSNode node) {
        Object result = getValueAtImpl(node);
        if (result != null && !isCellEditable(node))
            result = new ReadOnlyValue(result);
        return result;
    }
    
    private Object getValueAtImpl(WBSNode node) {
        Object extNodeType = getExtNodeType(node);
        if (extNodeType != null)
            return extNodeType;

        Object workflowType = getWorkflowType(node);
        if (workflowType != null)
            return workflowType;

        String type = node.getType();
        if (allowedTypes.contains(type))
            return type;
        else
            return null;
    }

    private Object getExtNodeType(WBSNode node) {
        Object extSysID = node.getAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR);
        Object extType = node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR);
        if (extSysID == null || extType == null)
            return null;

        String fullType = extSysID + ":" + extType;
        return new AnnotatedValue((String) extType, fullType);
    }

    private Object getWorkflowType(WBSNode node) {
        String wfType = WorkflowUtil.getWorkflowStepName(node, workflows, "/");
        if (wfType == null)
            return null;

        int slashPos = wfType.indexOf('/');
        String stepName = wfType.substring(slashPos + 1);
        return new AnnotatedValue(stepName, wfType);
    }


    public boolean isCellEditable(WBSNode node) {
        return node != wbsModel.getRoot() && node.isReadOnly() == false;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue instanceof String) {
            String s = (String) aValue;
            if (allowedTypes.contains(s))
                changeNodeType(node, s);
            else if (allowedTypes.contains(s + " Task"))
                changeNodeType(node, s + " Task");
            else if (s.indexOf(':') > 0)
                setExtNodeType(node, s);
            else if (s.indexOf('/') > 0)
                setWorkflowNodeType(node, s);
        }
    }

    private void changeNodeType(WBSNode node, String type) {
        // if the name of the node matches the old node type, update the
        // name of the task to keep it in sync.
        String oldType = node.getType();
        String nodeName = node.getName();
        nodeName = (nodeName == null ? "" : nodeName.trim());
        if (nodeName.length() == 0 || oldType.equals(nodeName) ||
            oldType.equals(nodeName + " Task")) {
            nodeName = type;
            if (nodeName.endsWith(" Task"))
                nodeName = nodeName.substring(0, nodeName.length() - 5);
            node.setName(nodeName);
        }

        // save the new type of the node.
        node.setType(type);
        node.setAttribute(WorkflowUtil.WORKFLOW_SOURCE_IDS_ATTR, null);
        ExternalNodeTypeColumn.storeType(node, null);
        EVENT_CONSOLIDATOR.needEvent(node);
    }

    private void setExtNodeType(WBSNode node, String typeFullName) {
        // parse the value as System:Type, then look up the resulting type
        int colonPos = typeFullName.indexOf(':');
        String extSysID = typeFullName.substring(0, colonPos).trim();
        String typeName = typeFullName.substring(colonPos + 1).trim();
        ExtNodeType type = lookupExtNodeType(extSysID, typeName);

        // if we find a matching external node type, set it on this node
        if (type != null && type.isCreatable()) {
            node.setType(TeamProcess.COMPONENT_TYPE);
            node.setAttribute(WorkflowUtil.WORKFLOW_SOURCE_IDS_ATTR, null);
            ExternalNodeTypeColumn.storeType(node, type);
            EVENT_CONSOLIDATOR.needEvent(node);
        }
    }

    private ExtNodeType lookupExtNodeType(String extSysID, String typeName) {
        ExternalSystemManager mgr = dataModel.getExternalSystemManager();
        for (ExtNodeType type : mgr.getNodeTypes(extSysID)) {
            if (type.getName().equalsIgnoreCase(typeName))
                return type;
        }
        return null;
    }

    private void setWorkflowNodeType(WBSNode node, String stepFullName) {
        // Look for a workflow step that has the given (slash-separated) name
        WBSNode step = workflows.getDescendantByName(workflows.getRoot(),
            stepFullName);

        // if we find a matching workflow step, set this node that that
        // workflow step type
        if (step != null && step.getIndentLevel() > 1) {
            node.setType(step.getType());
            node.setAttribute(WorkflowUtil.WORKFLOW_SOURCE_IDS_ATTR,
                Integer.toString(step.getUniqueID()));
            ExternalNodeTypeColumn.storeType(node, null);
            EVENT_CONSOLIDATOR.needEvent(node);
        }
    }

    public boolean recalculate() { return true; }
    public void storeDependentColumn(String ID, int columnNumber) {}

    public TableCellEditor getCellEditor() {
        if (cellEditor == null) {
            JComboBox cb = new JComboBox(allowedTypes.toArray());
            cellEditor = new AutocompletingDataTableCellEditor(cb);
        }
        return cellEditor;
    }

    /** If the user pastes a long string of values into this column, each
     * one will change the type of a different WBS node.  Rather than firing
     * change events on the WBS for each pasted value, send one event at the
     * end of the entire operation.  Since WBS change events trigger broad
     * recalculation operations, consolidating these events can save a lot
     * of churn downstream.
     */
    private class EventConsolidator implements ActionListener {
        private int row;
        private Timer timer;
        public EventConsolidator() {
            this.timer = new Timer(20, this);
            this.timer.setRepeats(false);
            this.row = Integer.MAX_VALUE;
        }
        public synchronized void needEvent(WBSNode node) {
            int row = wbsModel.getRowForNode(node);
            if (row != -1) {
                this.row = Math.min(this.row, row);
                timer.restart();
            }
        }
        public void actionPerformed(ActionEvent e) {
            int rowNumber;
            synchronized (this) {
                rowNumber = row;
                row = Integer.MAX_VALUE;
            }

            // we must update not just the given row, but also the rows
            // below it in the table (since they may now be invalid
            // because of the change).  Although only descendants of
            // this node could be affected, just repaint all the
            // remaining rows (lazy).
            int rowCount = wbsModel.getRowCount();
            if (rowNumber < rowCount)
                wbsModel.fireTableRowsUpdated(rowNumber, rowCount-1);
        }
    }
    private EventConsolidator EVENT_CONSOLIDATOR = new EventConsolidator();
}
