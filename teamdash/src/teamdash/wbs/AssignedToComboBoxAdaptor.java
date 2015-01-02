/*
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Modifications Copyright (C) 2014-2015 Tuma Solutions, LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package teamdash.wbs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import teamdash.wbs.AssignedToDocument.Word;

public class AssignedToComboBoxAdaptor implements ActionListener {

    /** the combobox being adapted */
    private JComboBox comboBox;

    /** the string representation in use for the currently selected item */
    private String selectedItemAsString;

    public AssignedToComboBoxAdaptor(JComboBox comboBox) {
        this.comboBox = comboBox;
        comboBox.addActionListener(this);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        // mark the current word when a new item is selected
        markCurrentWord();
    }

    public void setSelectedItem(String item) {
        comboBox.setSelectedItem(item);
    }

    public String getSelectedItem() {
        return (String) comboBox.getModel().getSelectedItem();
    }

    public String getSelectedItemAsString() {
        return this.selectedItemAsString;
    }

    public void setSelectedItemAsString(String itemAsString) {
        this.selectedItemAsString = itemAsString;
    }

    public int getItemCount() {
        return comboBox.getItemCount();
    }

    public String getItem(int index) {
        return (String) comboBox.getItemAt(index);
    }

    public boolean listContainsSelectedItem() {
        Object selectedItem = getSelectedItem();
        if (selectedItem != null) {
            for (int i = getItemCount(); i-- > 0;) {
                if (selectedItem.equals(getItem(i)))
                    return true;
            }
        }
        return false;
    }

    public JTextComponent getTextComponent() {
        return (JTextComponent) comboBox.getEditor().getEditorComponent();
    }

    public void markCurrentWord() {
        JTextComponent text = getTextComponent();
        AssignedToDocument doc = (AssignedToDocument) text.getDocument();
        int caretPos = text.getCaretPosition();
        Word w = doc.getWord(caretPos);

        if (w != null) {
            markWord(w);
            if (w.isNumber())
                w = w.prev;
            if (w != null && w.isLetters()) {
                String val = doc.getWordText(w);
                if (!val.equals(getSelectedItemAsString()))
                    doc.setSelectedItem(val);
            }
        }
    }

    public void markWord(Word w) {
        if (w != null) {
            markText(w.beg, w.end);
            if (w.isNumber())
                comboBox.hidePopup();
            else if (comboBox.isDisplayable() && !comboBox.isPopupVisible())
                comboBox.showPopup();
        }
    }

    public void markText(int caret, int end) {
        getTextComponent().setCaretPosition(end);
        getTextComponent().moveCaretPosition(caret);
    }

    public void setCaret(int caret) {
        getTextComponent().setCaretPosition(caret);
    }

}
