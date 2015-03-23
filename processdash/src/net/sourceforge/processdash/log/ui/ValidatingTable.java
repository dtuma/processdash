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

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;


/*
 * A "generic" table generator class
 *
 * headers and data are required parameters, and MUST have the same number of
 * columns (this is NOT checked).  Other parameters are optional, with the
 * following defaults:
 *  widths		- 100
 *  colHdrToolTips	- none
 *  colDataToolTips	- none
 *  validator		- a class which implements TableValidator
 *  table_id		- the table id, to allow a single validator to handle
 *			  multiple tables
 *
 * Note: Currently validation is ONLY called when editing is "completed".  That
 *  is, when the user clicks return
 */
public class ValidatingTable extends JPanel {


    DefaultCellEditor editor;
    DefaultTableCellRenderer renderers[];
    ValidatingJTable table;
    TableColumn newColumn;
    int idx, width;

 /** Constructor #1 - no colDataToolTips passed - set to same as colHdrToolTips */
    public ValidatingTable(Object[] headers,
                           Object[][] data,
                           int[] widths,
                           String[] colHdrToolTips,
                           TableValidator validator,
                           int table_id,
                           boolean showScroll,
                           boolean[] editable) {
        this(headers,data,widths,colHdrToolTips,colHdrToolTips,
             validator,table_id,showScroll,null, editable);
    }
    /** Constructor #2 - main constructor */
    public ValidatingTable(Object[] headers,
                           Object[][] data,
                           int[] widths,
                           String[] colHdrToolTips,
                           String[] colDataToolTips,
                           TableValidator validator,
                           int table_id,
                           boolean showScroll,
                           Object[] colIDsForBoolean,
                           boolean[] editable) {

        super();
        renderers = new DefaultTableCellRenderer [headers.length];
        setLayout(new BorderLayout());

        // Create the table;
        table = new ValidatingJTable();
        table.setModel (new VTableModel(editable));
        table.vParent = validator;
        table.id = table_id;
        table.setAutoCreateColumnsFromModel(false);
        if(widths != null){table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);}

        boolean isColEditable;

        // Add in the columns
        for(idx = 0; idx < headers.length; idx++) {
            if ((widths != null) && (widths.length > idx)) {width = widths[idx];}
            isColEditable = (editable == null) ? true : editable[idx];
            if (isColEditable) {
                editor = new DefaultCellEditor(new JTextField(""));
                editor.setClickCountToStart (1);
            } else
                editor = null;
            renderers[idx] = new DefaultTableCellRenderer();

            if(colIDsForBoolean != null){
                for(int j=0; j<colIDsForBoolean.length; j++){
                    if(headers[idx].equals(colIDsForBoolean[j])){
//	    if (isColEditable)
//	      editor = new DefaultCellEditor(new JCheckBox());
//	    renderers[idx] = new DefaultCellRenderer(new JCheckBox());
                    }
                }
            }

            if ((colDataToolTips != null) && (colDataToolTips.length > idx)) {
                renderers[idx].setToolTipText(colDataToolTips [idx]);
            }
            newColumn = new TableColumn(idx, width, renderers[idx], editor);
//      newColumn = new TableColumn(idx, width);
//System.out.println("editor:"+editor.toString());
//      newColumn.setCellEditor (editor);
            table.addColumn(newColumn);
            ((VTableModel)table.getModel()).addColumn("" + idx, null);
            newColumn.setHeaderValue (headers[idx]);
            newColumn.setHeaderRenderer (createHeaderRenderer (headers[idx]));

            TableCellRenderer renderer2 = newColumn.getHeaderRenderer();
            if (renderer2 instanceof DefaultTableCellRenderer) {
                // Set a tool tip for the column header cell
                if ((colHdrToolTips != null) && (colHdrToolTips.length > idx)){
                    ((DefaultTableCellRenderer)renderer2).setToolTipText(colHdrToolTips[idx]);
                }
            }
        }
        if (data != null)
            ((VTableModel)table.getModel()).setDataVector(data,null);
        //table.setMultipleSelectionAllowed (false);
        table.setColumnSelectionAllowed (false);
        table.setRowSelectionAllowed (false);

        // Put the table and header into a scrollPane
        JScrollPane scrollpane = new JScrollPane(table);
        //scrollpane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        scrollpane.setBorder(BorderFactory.createEmptyBorder());
        if(!showScroll){
            scrollpane.setHorizontalScrollBarPolicy
                (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollpane.setVerticalScrollBarPolicy
                (ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        } else {
            scrollpane.setVerticalScrollBarPolicy
                (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        add(scrollpane, BorderLayout.CENTER);

        // Adjust preferred size if specific column widths were passed
        if(widths != null){
            Dimension d1 = table.getSize();
            Dimension d2 = table.getTableHeader().getSize();
            setPreferredSize(new Dimension (d1.width + ((showScroll) ? 20 : 0),
                                            (d1.height+d2.height)));
        }

        // Note: You can speed up resizing repaints by turning off live cell
        // updates like this:
        // table.getTableHeader().setUpdateTableInRealTime(false);
    }
    //********************************/
    //** end 'ValidatingTable' Constructor */
    //********************************/

    protected TableCellRenderer createHeaderRenderer (Object aObject) {
//    JButton jButton = new JButton();
//    jButton.setBackground(Color.blue);
//    jButton.setHorizontalAlignment(0);
//    jButton.setBorder(BorderFactory.createRaisedBevelBorder());
//    jButton.setFocusPainted(false);
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
//    tcr.setBackground(Color.blue);
        return tcr;
    }


    class FocusCheck extends Object implements FocusListener {
        public void focusGained (FocusEvent e) {
            System.err.println ("focusGained listener");
        }
        public void focusLost (FocusEvent e) {
            System.err.println ("focusLost listener");
        }
    }

    class ValidatingJTable extends JTable {
        public TableValidator vParent;
        public int id;
        public void tableChanged(TableModelEvent e) {
            super.tableChanged (e);
            if ((vParent != null) && (editingRow >= 0) && (editingColumn >= 0)){
                vParent.validate(id,editingRow,editingColumn,
                                 (String)getValueAt(editingRow, editingColumn));
            }
        }

        public void doResizeRepaint() { resizeAndRepaint(); }
    }

    public boolean isEditing() {return table.isEditing();}
    public int getEditingRow() {return table.getEditingRow ();}
    public int getEditingColumn() {return table.getEditingColumn ();}
    public void appendRow() {
        ((VTableModel)table.getModel()).setNumRows (table.getRowCount () + 1);
    }

    public void setColor(int row, int column, Color color) {
        Component comp = (renderers[column]).getTableCellRendererComponent
            (table,table.getValueAt(row, column),false, false, row, column);
        comp.setBackground(color);
        //sets background color for column
        //    (renderers [column]).setBackgroundColor(color);
    }

    public Color getColor(int row, int column) {
        Component comp = (renderers [column]).getTableCellRendererComponent
            (table,table.getValueAt(row, column), false, false, row, column);
        return comp.getBackground();
    }

    public void stopEdit () {//TBD
    }

    public void doResizeRepaint() { table.doResizeRepaint(); }

}
