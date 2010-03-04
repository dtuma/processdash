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

import javax.swing.JTable;

/** This simple renderer appends a fixed string to the end of
 * displayed values.
 */
public class DataTableStringSuffixRenderer extends DataTableCellRenderer {

    private String suffix;

    /** Create a renderer to append the given suffix to the end of
     * displayed values */
    public DataTableStringSuffixRenderer(String suffix) {
        this.suffix = suffix;
    }

    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        if (value != null)
            value = String.valueOf(value) + suffix;

        return super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);
    }

}
