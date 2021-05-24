// Copyright (C) 2008-2021 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.columns.WorkflowPercentageColumn;

public class WorkflowJTable extends WBSJTable {

    private static final Resources resources = WorkflowEditor.resources;


    public WorkflowJTable(WorkflowDataModel workflowModel, TeamProcess process,
            boolean hasProbeTaskType, ActionListener probeListener) {
        super(workflowModel, getWorkflowIcons(process.getIconMap()),
                tweakIconMenu(process.getNodeTypeMenu(),
                    process.isPspCompatible(), hasProbeTaskType,
                    probeListener));
        setSafeTaskType(process.getPhases().get(0) + TeamProcess.TASK_SUFFIX);

        // configure the renderer for the table
        this.renderer.setRootNodeName(resources.getString("Root_Name"));
        this.renderer.setTypeNameMap(Collections.singletonMap(
            TeamProcess.PROBE_TASK_TYPE, resources.getString("PROBE")));

        // configure the editor for WBS nodes
        this.editor.setTypeNameMap(this.renderer.getTypeNameMap());
        this.editor.setTypeTaskNameMap(Collections.singletonMap( //
            TeamProcess.PROBE_TASK_TYPE, "Planning"));

        // install the default editor for table data.
        this.setDefaultEditor(Object.class, new WorkflowCellEditor());

        // customize the behavior and appearance of the columns.
        DataTableModel.installColumnCustomizations(this);
    }


    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component result = super.prepareRenderer(renderer, row, column);
        if (column > 0 && !isCellEditable(row, column))
            result.setBackground(UNEDITABLE);
        return result;
    }


    private static Map getWorkflowIcons(Map<String, Icon> processIconMap) {
        // make a copy of the icon map.
        Map result = new HashMap(processIconMap);
        // change the "project" icon to a special value for common workflows
        result.put(TeamProcess.PROJECT_TYPE,
            WBSZoom.icon(IconFactory.getCommonWorkflowsIcon()));
        // replace each of the "task" icons with the correponsponding
        // "workflow task" icon.
        for (Map.Entry<String, Icon> e : processIconMap.entrySet()) {
            String key = e.getKey();
            if (key != null && key.endsWith(TeamProcess.WORKFLOW_TASK_SUFFIX)) {
                String taskType = StringUtils.findAndReplace(key,
                    TeamProcess.WORKFLOW_TASK_SUFFIX, TeamProcess.TASK_SUFFIX);
                Icon icon = e.getValue();
                result.put(taskType, icon);
            }
        }
        return result;
    }


    private static JMenu tweakIconMenu(JMenu iconMenu, boolean hasPspTaskType,
            boolean hasProbeTaskType, ActionListener probeListener) {
        JMenu taskMenu = (JMenu) iconMenu.getMenuComponent(0);

        if (hasProbeTaskType) {
            // create a new menu item for the PROBE task type.
            JMenuItem probeItem = new JMenuItem(resources.getString("PROBE"));
            probeItem.setFont(iconMenu.getFont());
            probeItem.setActionCommand(TeamProcess.PROBE_TASK_TYPE);
            if (probeListener != null)
                probeItem.addActionListener(probeListener);
    
            // insert the PROBE item after the PSP task item (if it is present).
            // The PSP item is first in the list unless a "More..." submenu
            // precedes it.
            int pos = (hasPspTaskType ? 1 : 0)
                    + (taskMenu.getMenuComponent(0) instanceof JMenu ? 1 : 0);
            taskMenu.add(probeItem, pos);
        }

        // if the number of task types is small, embed them directly in the node
        // menu (rather than using a fly-out)
        int numTaskTypes = taskMenu.getMenuComponentCount();
        int numComponentTypes = iconMenu.getMenuComponentCount() - 2;
        if (numTaskTypes + numComponentTypes < 15) {
            for (int i = numTaskTypes; i-- > 0;)
                iconMenu.add(taskMenu.getMenuComponent(i), 1);
            iconMenu.remove(0);
        }

        return iconMenu;
    }



    @Override
    public Action[] getEditingActions() {
        Action[] actions = super.getEditingActions();
        Action[] result = new Action[actions.length + 1];
        Object normalizeBefore = WBS_ACTION_CATEGORY_STRUCTURE;
        for (int a = 0, r = 0; a < actions.length; a++, r++) {
            if (normalizeBefore != null && normalizeBefore
                    .equals(actions[a].getValue(WBS_ACTION_CATEGORY))) {
                result[r++] = NORMALIZE_ACTION;
                normalizeBefore = null;
            }
            result[r] = actions[a];
        }
        return result;
    }



    private static class WorkflowCellEditor extends DefaultCellEditor {

        public WorkflowCellEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            Component result = super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);

            if (result instanceof JTextField)
                ((JTextField) result).selectAll();

            return result;
        }

    }


    private class NormalizeAction extends AbstractAction
            implements EnablementCalculation {

        public NormalizeAction() {
            super(resources.getString("Normalize_Tooltip"), //
                    IconFactory.getPercentIcon());
            addEnablementCalculation(this);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEditingEnabled())
                return;

            // get the currently selected workflow nodes
            Set<WBSNode> workflowNodes = getSelectedWorkflows();
            if (workflowNodes.isEmpty())
                return;

            // stop the current editing session.
            editor.stopCellEditing();
            UndoList.stopCellEditing(WorkflowJTable.this);

            // ask the percentage column to normalize the selected workflow
            DataColumn pctCol = dataModel.getColumn(
                dataModel.findColumn(WorkflowPercentageColumn.COLUMN_ID));
            for (WBSNode node : workflowNodes)
                ((WorkflowPercentageColumn) pctCol).normalizeWorkflow(node);
            dataModel.columnChanged(pctCol);

            UndoList.madeChange(WorkflowJTable.this, (String) getValue(NAME));
        }

        private Set<WBSNode> getSelectedWorkflows() {
            Set<WBSNode> result = new HashSet<WBSNode>();
            for (int row : getSelectedRows()) {
                WBSNode node = wbsModel.getNodeForRow(row);
                WBSNode workflow = wbsModel.getBaseParent(node);
                if (workflow != null)
                    result.add(workflow);
            }
            return result;
        }

        public void recalculateEnablement(int[] selectedRows) {
            setEnabled(isEditingEnabled() && notJustRoot(selectedRows));
        }
    }
    final NormalizeAction NORMALIZE_ACTION = new NormalizeAction();


    public static final Color UNEDITABLE = new Color(220, 220, 220);

}
