// Copyright (C) 2007-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.EventObject;

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
    private int clickCountToStart;

    public JDateTimeChooserCellEditor(String format) {
        this(format, true);
    }

    public JDateTimeChooserCellEditor(String format, boolean autoSave) {
        dateChooser = new JDateChooser(null, null, format,
            new JTextFieldDateTimeEditor());
        changingInternally = false;
        clickCountToStart = 1;

        dateChooser.getDateEditor().getUiComponent().setBorder(null);

        if (autoSave)
            dateChooser.addPropertyChangeListener(new AutoSaver());
    }

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    public void setClickCountToStart(int clickCountToStart) {
        this.clickCountToStart = clickCountToStart;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent)
            return ((MouseEvent) e).getClickCount() >= clickCountToStart;
        else
            return true;
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
