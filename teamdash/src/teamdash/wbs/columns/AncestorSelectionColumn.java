// Copyright (C) 2011-2013 Tuma Solutions, LLC
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

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

/**
 * This column displays a set of checkboxes for the nodes in the hierarchy.
 * The user is allowed to select and unselect boxes, and the column enforces
 * these constraints:
 * 
 * <ul>
 *     <li>if a box is checked, all of its ancestors must be checked too.</li>
 *     <li>if a box is unchecked, none of its descendants can be checked.</li>
 * </ul>
 * 
 * These constraints are appropriate when the desire is to indicate a set of
 * top-level nodes that should be included in report summarizations.
 * 
 * @author Tuma
 */
public class AncestorSelectionColumn extends AbstractDataColumn implements
        CalculatedDataColumn, LabelSource, WbsNodeAttributeSource {

    private WBSModel wbsModel;

    private String explicitAttrName;

    private String calculatedAttrName;

    private String labelName;

    private static final boolean MAKE_ANCESTORS_EXPLICIT = true;

    public AncestorSelectionColumn(DataTableModel dataModel, String columnID,
            String columnName, int columnWidth, String labelName) {
        this.wbsModel = dataModel.getWBSModel();
        this.columnID = columnID;
        this.columnName = columnName;
        this.preferredWidth = columnWidth;
        this.explicitAttrName = columnID + "-Checked";
        this.calculatedAttrName = "_" + columnID + "_Calculated";
        this.labelName = TaskLabelColumn.convertToLabel(labelName);
    }

    public AncestorSelectionColumn(DataTableModel dataModel, String id,
            String name, Element e) {
        this(dataModel, id, name, XMLUtils.getXMLInt(e, "width"),
                e.getAttribute("syncAsLabel"));
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public Class getColumnClass() {
        return Boolean.class;
    }

    public boolean isCellEditable(WBSNode node) {
        return true;
    }

    public String getLabels(WBSNode node) {
        if (node.getAttribute(calculatedAttrName) != null)
            return this.labelName;
        else
            return null;
    }

    public Object getValueAt(WBSNode node) {
        return node.getAttribute(calculatedAttrName) != null;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue == Boolean.TRUE)
            checkNode(node);
        else
            uncheckNodeAndDescendants(node);
    }

    private void checkNode(WBSNode node) {
        node.setAttribute(explicitAttrName, "t");
    }

    private void uncheckNodeAndDescendants(WBSNode node) {
        node.setAttribute(explicitAttrName, null);
        for (WBSNode d : wbsModel.getDescendants(node))
            d.setAttribute(explicitAttrName, null);
    }

    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    private boolean recalc(WBSNode node) {
        boolean isChecked = node.getAttribute(explicitAttrName) != null;
        for (WBSNode child : wbsModel.getChildren(node))
            if (recalc(child))
                isChecked = true;
        node.setAttribute(calculatedAttrName, isChecked ? "t" : null);
        if (isChecked && MAKE_ANCESTORS_EXPLICIT)
            node.setAttribute(explicitAttrName, "t");
        return isChecked;
    }

    public String getAttributeId() {
        return columnID;
    }

    public String getAttributeName() {
        return columnName;
    }

    public boolean isAttributeAutoInherited() {
        return false;
    }

    public List<String> getAttributeValues(WBSNode node) {
        if (getValueAt(node) == Boolean.TRUE)
            return TRUE_ATTR_VALUE;
        else
            return null;
    }

    private static final List<String> TRUE_ATTR_VALUE = Collections
            .singletonList("true");

}
