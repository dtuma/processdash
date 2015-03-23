// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;


/** The SwingX library provides an autocompleting combo box table cell editor.
 * But it has some undesirable usability issues when you start an editing
 * session with a keypress.  In particular, pressing a key causes the editing
 * session to start, but does not place the focus on the text field - you have
 * to click to make that happen (unacceptable).  Also, the first keypress
 * starts the editing session but is discarded instead of being propagated to
 * the text field;  the result is that when you type a cell value the first
 * letter of your entry is lost.  This class corrects both of those problems.
 */
public class AutocompletingDataTableCellEditor extends ComboBoxCellEditor {

    private JComboBox comboBox;

    private KeyEvent keyEvent;

    private Timer setupEditor;

    private Timer dispatchKeyEvent;

    public AutocompletingDataTableCellEditor() {
        this(new JComboBox());
    }

    public AutocompletingDataTableCellEditor(JComboBox combo) {
        super(combo);
        this.comboBox = combo;
        if (!(combo instanceof AssignedToComboBox)) {
            this.comboBox.setEditable(true);
            AutoCompleteDecorator.decorate(comboBox);
        }

        this.setupEditor = new Timer(10, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setupEditor();
            }
        });
        this.setupEditor.setRepeats(false);

        this.dispatchKeyEvent = new Timer(10, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispatchKeyEvent();
            }
        });
        this.dispatchKeyEvent.setRepeats(false);
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    protected void setupEditor() {
        comboBox.getEditor().getEditorComponent().requestFocus();
        if (comboBox.isShowing()) {
            comboBox.showPopup();
            dispatchKeyEvent.restart();
        }
    }

    protected void dispatchKeyEvent() {
        if (keyEvent != null) {

            JTextField tf = (JTextField) comboBox.getEditor()
                    .getEditorComponent();

            char c = keyEvent.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED)
                return;
            else if (isValidCharacter(c))
                tf.setText(String.valueOf(c));
            else
                tf.setText("");
        }
    }

    /** 
     * The keypress that starts the editing session could represent a valid
     * character that needs adding to the field, or it could represent
     * something like "delete" which signals the desire to clear the
     * current text in the field. The default implementation of this method
     * recognizes letters and digits as valid;  subclasses can override this
     * to indicate their particular rules.
     */
    protected boolean isValidCharacter(int c) {
        return Character.isLetterOrDigit(c);
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        keyEvent = null;
        if (anEvent instanceof MouseEvent) {
            return ((MouseEvent) anEvent).getClickCount() >= 2;
        } else if (anEvent instanceof KeyEvent) {
            keyEvent = (KeyEvent) anEvent;
        }
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        if (column != -1)
            setupEditor.restart();

        return super.getTableCellEditorComponent(table, value, isSelected, row,
            column);
    }

}
