// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

import java.text.DateFormat;
import java.util.Date;

import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellRenderer;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class TeamCompletionDateColumn extends AbstractPrecomputedColumn
        implements CustomRenderedColumn {

    public static final String COLUMN_ID = "Actual_Completion_Date";

    public static final String ATTR_NAME = "Actual_Completion_Date";

    public TeamCompletionDateColumn() {
        super(COLUMN_ID, resources.getString("Completion_Date.Name"), ATTR_NAME,
                TeamActualTimeColumn.COLUMN_ID);
        this.preferredWidth = 70;
    }

    @Override
    public Class getColumnClass() {
        return Date.class;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        return new ReadOnlyValue(node.getAttribute(ATTR_NAME));
    }

    public static final String getMemberNodeDataAttrName(TeamMember m) {
        return getMemberNodeDataAttrName(m.getInitials());
    }

    protected static String getMemberNodeDataAttrName(String initials) {
        return initials + "@Actual_Node_Completion_Date";
    }

    public TableCellRenderer getCellRenderer() {
        return new DateCellRenderer();
    }

    private class DateCellRenderer extends DataTableCellRenderer {

        @Override
        protected Object format(Object value) {
            if (value instanceof Date) {
                return FORMATTER.format((Date) value);
            } else {
                return value;
            }
        }

    }

    private static final DateFormat FORMATTER = DateFormat
            .getDateInstance(DateFormat.SHORT);
}
