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

package teamdash.wbs;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JTable;

import teamdash.XMLUtils;

/**
 * Table cell renderer for task dependency data.
 */
public class TaskDependencyCellRenderer extends DataTableCellRenderer {

    public TaskDependencyCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        boolean hasError = false;
        setToolTipText(null);
        String overflowToolTipText = null;

        // Note: this logic will not flag errors in read-only dependencies
        if (value instanceof TaskDependency)
            hasError = ((TaskDependency) value).hasError;
        else if (value instanceof TaskDependencyList) {
            TaskDependencyList list = (TaskDependencyList) value;
            hasError = list.hasError();
            overflowToolTipText = computeOverflowToolTipText(list);
        }

        if (hasError)
            value = new ErrorValue(value, "Dependent task not found",
                    ErrorValue.ERROR);

        Component result = super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        if (overflowToolTipText != null)
            setToolTipText(overflowToolTipText);
        return result;
    }

    private String computeOverflowToolTipText(TaskDependencyList list) {
        if (list.size() < 2)
            return null;

        StringBuffer result = new StringBuffer();
        result.append("<html><body><table border='0'>");
        for (Iterator i = list.iterator(); i.hasNext();) {
            TaskDependency d = (TaskDependency) i.next();
            if (d.hasError)
                result.append("<tr><td color='red'>");
            else
                result.append("<tr><td>");
            result.append(XMLUtils.escapeAttribute(d.displayName));
            result.append("</td></tr>");
        }
        result.append("</table></body></html>");
        return result.toString();
    }

}
