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

import javax.swing.table.TableColumn;


/** Encapsulates the logic necessary to create a TableColumn object for a
 * DataColumn
 */
public class DataTableColumn extends TableColumn {

    /** Create a DataTableColumn if we know the String ID of the column */
    public DataTableColumn(DataTableModel model, String columnID) {
        super();

        // locate the named column
        int columnIndex = model.findColumn(columnID);
        if (columnIndex == -1)
            throw new IllegalArgumentException(
                "No column with ID " + columnID);

        init(model, model.getColumn(columnIndex), columnIndex);
    }

    /** Create a DataTableColumn for a given DataColumn */
    public DataTableColumn(DataTableModel model, DataColumn c) {
        super();

        // locate the named column
        int columnIndex = model.findColumn(c.getColumnID());
        if (columnIndex == -1)
            columnIndex = model.findColumn(c.getColumnName());
        if (columnIndex == -1)
            throw new IllegalArgumentException(
                "No column with ID " + c.getColumnID());

        init(model, c, columnIndex);
    }

    /** 
     * Create a copy of an existing DataTableColumn
     * @param orig
     */
    public DataTableColumn(DataTableColumn orig) {
        setModelIndex(orig.getModelIndex());
        setHeaderValue(orig.getHeaderValue());
        setIdentifier(orig.getIdentifier());
        setPreferredWidth(orig.getPreferredWidth ());
        setCellRenderer(orig.getCellRenderer());
        setCellEditor(orig.getCellEditor());
    }


    private void init(DataTableModel model, DataColumn c, int columnIndex) {
        // set the index, header value, and identifier.
        setModelIndex(columnIndex);
        setHeaderValue(c.getColumnName());
        setIdentifier(c.getColumnID());

        // install the column's preferred width
        int width = c.getPreferredWidth();
        if (width > 0)
            setPreferredWidth(width);

        if (c instanceof CustomRenderedColumn)
            // install the column's preferred renderer.
            setCellRenderer(((CustomRenderedColumn) c).getCellRenderer());

        if (c instanceof CustomEditedColumn)
            // install the column's preferred editor.
            setCellEditor(((CustomEditedColumn) c).getCellEditor());
    }

    public String toString() {
        return getHeaderValue().toString();
    }
}
