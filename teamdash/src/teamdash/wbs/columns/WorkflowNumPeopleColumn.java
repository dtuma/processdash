// Copyright (C) 2002-2012 Tuma Solutions, LLC
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

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowNumPeopleColumn extends AbstractDataColumn implements
        CustomRenderedColumn {

    private WBSModel wbsModel;

    public WorkflowNumPeopleColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = this.columnID = COLUMN_ID;
        this.preferredWidth = 60;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        if (!isCellEditable(node)) return "";

        int numPeople = getNumPeopleAt(node);

        if (numPeople == 1)
            return "1 person";
        else
            return numPeople + " people";
    }

    static int getNumPeopleAt(WBSNode node) {
        double value = node.getNumericAttribute(ATTR_NAME);
        int numPeople = 1;
        if (value > 0) numPeople = (int) value;
        return numPeople;
    }


    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue == null) return;
        String s = String.valueOf(aValue).trim();

        int pos = s.indexOf(' ');
        if (pos != -1) s = s.substring(0, pos).trim();
        pos = s.indexOf('p');
        if (pos != -1) s = s.substring(0, pos).trim();

        try {
            int numPeople = (int) Double.parseDouble(s);
            if (numPeople > 0)
                node.setNumericAttribute(ATTR_NAME, numPeople);
        } catch (NumberFormatException nfe) { }
    }


    public TableCellRenderer getCellRenderer() {
        return WorkflowTableCellRenderer.INSTANCE;
    }

    public static final String ATTR_NAME = TeamTimeColumn.NUM_PEOPLE_ATTR;
    static final String COLUMN_ID = "# People";
}
