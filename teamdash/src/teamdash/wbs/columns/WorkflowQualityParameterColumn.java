// Copyright (C) 2018 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowModel;
import teamdash.wbs.WorkflowWBSModel;

public abstract class WorkflowQualityParameterColumn
        extends AbstractNumericColumn implements WorkflowOptionalColumn {

    protected WorkflowWBSModel wbsModel;

    protected TeamProcess teamProcess;

    protected String attrName;


    public WorkflowQualityParameterColumn(WBSModel workflowWbsModel,
            TeamProcess teamProcess, String attrName) {
        this.wbsModel = (WorkflowWBSModel) workflowWbsModel;
        this.teamProcess = teamProcess;
        this.attrName = attrName;
        this.preferredWidth = 50;
        setConflictAttributeName(attrName);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    @Override
    public boolean shouldHideColumn(WorkflowModel model) {
        // quality parameters are expert-level columns. Most team members won't
        // need to see them...just coaches and quality managers. So we hide
        // the column by default unless a user explicitly enables it.
        return true;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        // display "null" for non-leaf-tasks
        if (!isCellEditable(node))
            return null;

        // see if the user has an explicit value saved on this node
        NumericDataValue value = (NumericDataValue) super.getValueAt(node);
        value.isInvisible = (value.value == 0);
        if (value.value >= 0)
            return value;

        // if there is no explicit value, look for a process default
        double processDefault = getProcessDefaultForNode(node);
        value = new NumericDataValue(processDefault);
        if (processDefault == 0)
            value.isInvisible = true;
        else
            value.errorMessage = DEFAULT_TOOLTIP;
        return value;
    }

    @Override
    protected double getValueForNode(WBSNode node) {
        return node.getNumericAttribute(attrName);
    }

    protected double getProcessDefaultForNode(WBSNode node) {
        String nodeType = node.getType();
        String phase = nodeType.substring(0, nodeType.length() - 5);
        return getProcessDefaultForPhase(phase);
    }

    protected abstract double getProcessDefaultForPhase(String phase);

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        // see if the user has deleted the value from the cell
        if (isEmptyValue(aValue)) {
            if (node.getAttribute(attrName) != null)
                // if this node has a custom value, handle the deletion by
                // reverting back to the default value from the process. Signal
                // this with a bad value, which will parse to NaN.
                aValue = "*";
            else
                // if this node was already using the default value from the
                // process, handle the deletion by changing to zero.
                aValue = "0";
        }
        super.setValueAt(aValue, node);
    }

    private boolean isEmptyValue(Object val) {
        if (val == null)
            return true;
        else if (val instanceof String)
            return ((String) val).trim().length() == 0;
        else
            return false;
    }

    @Override
    protected void setValueForNode(double value, WBSNode node) {
        if (Double.isNaN(value) || equal(value, getProcessDefaultForNode(node)))
            node.removeAttribute(attrName);
        else if (value >= 0)
            node.setNumericAttribute(attrName, value);
    }


    protected static final String DEFAULT_TOOLTIP = resources
            .getString("Default_Tooltip");

}
