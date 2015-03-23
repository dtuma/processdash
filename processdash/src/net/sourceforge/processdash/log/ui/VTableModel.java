// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.ui;

/*
 * ValidatingTable.java
 *
 */

import javax.swing.table.*;
import javax.swing.event.*;
import java.util.Vector;
import java.util.Enumeration;

public class VTableModel extends AbstractTableModel {
    /** The Vector of Vector of Object values */
    protected Vector    dataVector;
    /** The Vector column identifiers */
    protected Vector    columnIdentifiers;
    /** The number of columns in the table */
    int                 numColumns;
    /** The number of rows in the table */
    int                 numRows;
    /** The editable columns (null => all editable) */
    boolean             editableCols[];


    public VTableModel(boolean[] editable) {
        super();
        numColumns = 0;
        columnIdentifiers = new Vector();
        dataVector = new Vector();
        this.numRows = 0;
        editableCols = editable;
    }


    public boolean isCellEditable(int row, int col) {
        if ((editableCols != null) && (editableCols.length > col))
            return editableCols [col];
        return true;
    }


    public void setDataVector(Vector newData, Vector columnIds) {
        Vector oldIds = columnIdentifiers;

        if (newData == null)
            throw new IllegalArgumentException("setDataVector() - Null parameter");

        if (columnIds != null) {
            columnIdentifiers = columnIds;
        }
        dataVector = newData;
            if (dataVector.size() > 0)
                numColumns = ((Vector)dataVector.elementAt(0)).size();
            else
                numColumns = 0;
        this.newDataAvailable(null);
    }

    public void setDataVector(Object[][] newData, Object[] columnIds) {
        Vector ids = null;
        if (columnIds != null)
            ids = convertToVector(columnIds);

        setDataVector(convertToVector(newData), ids);
    }

    public void setNumRows(int newSize) {
        if ((newSize < 0) || (newSize == numRows))
            return;

        int oldNumRows = numRows;
        if (newSize <= numRows) {
            // newSize is smaller than our current size, so we can just
            // let Vector discard the extra rows
            numRows = newSize;
            dataVector.setSize(numRows);

            // Generate notification
            rowsRemoved(new TableModelEvent(this, numRows, oldNumRows-1,
                                            TableModelEvent.ALL_COLUMNS,
                                            TableModelEvent.DELETE));
        }
        else {
            // We are adding rows to the model
            while(numRows < newSize) {
                Vector newRow = new Vector(numColumns);
                dataVector.addElement(newRow);
                numRows++;
            }

            // Generate notification
            newRowsAdded(new TableModelEvent(this, oldNumRows, numRows-1,
                                             TableModelEvent.ALL_COLUMNS,
                                             TableModelEvent.INSERT));
        }
    }

    public void newDataAvailable(TableModelEvent event) {
        if ((event == null) || (dataVector.size() != numRows)) {
            // Assume all changed
            numRows = dataVector.size();

            event = new TableModelEvent(this, TableModelEvent.HEADER_ROW);

            // Because the new data rows might not have the correct number
            // of columns we need to do this to make sure dataVector is not
            // malformed.
            this.setColumnIdentifiers(columnIdentifiers);
        }

        // Now we sent the notification
        fireTableChanged(event);
    }

    public void rowsRemoved(TableModelEvent event) {
        if (event == null)
            throw new IllegalArgumentException("rowsRemoved() - null parameter");

        numRows = dataVector.size();

        // Now we send the notification
        fireTableChanged(event);
    }

    public void newRowsAdded(TableModelEvent event) {
        numRows = dataVector.size();
        int start = event.getFirstRow();
        int end = event.getLastRow();
        if (start < 0) start = 0;
        if (end < 0) end = numRows-1;

        // Have to make sure all the new columns have the correct
        // number of columns
        for (int i=start; i <= end; i++)
            ((Vector)dataVector.elementAt(i)).setSize(numColumns);

        // Now we send the notification
        fireTableChanged(event);
    }

    public void setColumnIdentifiers(Vector newIdentifiers) {
        if (newIdentifiers != null) {
            columnIdentifiers = newIdentifiers;
            numColumns = newIdentifiers.size();
        }
        else {
            columnIdentifiers = new Vector();
            numColumns = 0;
        }

        // Generate notification
        fireTableStructureChanged();
    }

    public int getColumnCount() { return numColumns; }

    public int getRowCount() { return numRows; }

    public Object getValueAt(int row, int column) {
        Vector rowVector = (Vector)dataVector.elementAt(row);
        return rowVector.elementAt(column);
    }

    protected Vector convertToVector(Object[] anArray) {
        if (anArray == null)
            return new Vector();

        Vector v = new Vector(anArray.length);
        for (int i=0; i < anArray.length; i++) {
            v.addElement(anArray[i]);
        }
        return v;
    }

    public void addRow(Vector rowData) {
        Vector newRow = rowData;

        if (newRow == null)
            newRow = new Vector(numColumns);

        dataVector.addElement(newRow);
        numRows++;

        // Generate notification
        newRowsAdded(new TableModelEvent(this, numRows-1, numRows-1,
                                         TableModelEvent.ALL_COLUMNS,
                                         TableModelEvent.INSERT));
    }

    public void addRow(Object[] rowData) {
        Vector newVector = null;

        if (rowData != null)
            newVector = convertToVector(rowData);
        this.addRow(newVector);
    }

    public void removeRow(int row) {
        dataVector.removeElementAt(row);
        numRows--;

        // Generate notification
        rowsRemoved(new TableModelEvent(this, row, numRows,
                                        TableModelEvent.ALL_COLUMNS,
                                        TableModelEvent.DELETE));
    }

    public void setValueAt(Object aValue, int row, int column) {
        Vector rowVector = (Vector)dataVector.elementAt(row);
        rowVector.setElementAt(aValue, column);

        // generate notification
        fireTableChanged(new TableModelEvent(this, row, row, column));
    }

    public void addColumn(Object columnIdentifier, Vector columnData) {
        if (columnIdentifier == null)
            throw new IllegalArgumentException("addColumn() - null parameter");

        columnIdentifiers.addElement(columnIdentifier);
        numColumns++;

        // Fill in the new column, with nulls or with columnData
        int index = 0;
        Enumeration enumeration = dataVector.elements();
        while (enumeration.hasMoreElements()) {
            Object value;

            if ((columnData != null) && (index < columnData.size()))
                value = columnData.elementAt(index);
            else
                value = null;

            ((Vector)enumeration.nextElement()).addElement(value);
            index++;
        }

        // Generate notification
        fireTableChanged(null);
    }
};

