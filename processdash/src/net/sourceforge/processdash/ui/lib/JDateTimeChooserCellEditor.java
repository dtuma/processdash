// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

/**
 * This class is actually a simple JDateChooserCellEditor (from the JCalendar library)
 * that uses a dateChooser which can use any format to display a date. Furthermore,
 * it allows the use of the "Enter" key to validate changes made in a field.
 */
public class JDateTimeChooserCellEditor extends AbstractCellEditor
                                        implements TableCellEditor {

    private JDateChooser dateChooser;
    private boolean changingInternally;

    public JDateTimeChooserCellEditor(String format) {
        this(format, true);
    }

    public JDateTimeChooserCellEditor(String format, boolean autoSave) {
        dateChooser = new JDateChooser(null, null, format,
            new JTextFieldDateTimeEditor());
        changingInternally = false;

        dateChooser.getDateEditor().getUiComponent().setBorder(null);

        if (autoSave)
            dateChooser.addPropertyChangeListener(new AutoSaver());
    }

    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        Date date = null;
        if (value instanceof Date)
            date = (Date) value;

        changingInternally = true;
        dateChooser.setDate(date);
        changingInternally = false;

        return dateChooser;
    }

    public Object getCellEditorValue() {
        return dateChooser.getDate();
    }


    private class AutoSaver implements PropertyChangeListener, Runnable {

        public void propertyChange(PropertyChangeEvent evt) {
            if (!changingInternally && evt.getPropertyName().equals("date"))
                SwingUtilities.invokeLater(this);
        }

        public void run() {
            stopCellEditing();
        }

    }


    public static class JTextFieldDateTimeEditor extends JTextFieldDateEditor {
        public Date getDate() {
            try {
                date = dateFormatter.parse(getText());
            } catch (Exception e) {
                date = null;
            }
            return date;
        }
    }
}
