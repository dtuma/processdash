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

/** Interface for a column in a DataTableModel
 */
public interface DataColumn {

    /** Get the ID of this column. */
    public String getColumnID();

    /** Get the name that should be displayed for this column */
    public String getColumnName();

    /** Get the type of data displayed by this column. */
    public Class getColumnClass();

    /** Return true if this column is editable for the given WBSNode */
    public boolean isCellEditable(WBSNode node);

    /** Get the value of this column for the given WBSNode */
    public Object getValueAt(WBSNode node);

    /** Set a new value of this column for the given WBSNode */
    public void setValueAt(Object aValue, WBSNode node);

    /** Return the preferred width of this column, or -1 if this column
     * has no preferred width. */
    public int getPreferredWidth();

}
