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

package teamdash.wbs.columns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowNumPeopleColumn extends AbstractDataColumn {

    private WBSModel wbsModel;

    public WorkflowNumPeopleColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Workflow.Num_People.Name");
        this.preferredWidth = 100;
        setConflictAttributeName(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        if (!isCellEditable(node)) return "";

        int numPeople = getNumPeopleAt(node);

        if (numPeople == 1)
            return resources.getString("Workflow.Num_People.Person");
        else
            return resources.format("Workflow.Num_People.People_FMT", numPeople);
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

        Matcher m = NUM_PAT.matcher(s);
        if (m.find()) {
            int numPeople = Integer.parseInt(m.group());
            if (numPeople > 0)
                node.setNumericAttribute(ATTR_NAME, numPeople);
        }
    }


    private static final Pattern NUM_PAT = Pattern.compile("\\d+");
    public static final String ATTR_NAME = TeamTimeColumn.NUM_PEOPLE_ATTR;
    static final String COLUMN_ID = "# People";
}
