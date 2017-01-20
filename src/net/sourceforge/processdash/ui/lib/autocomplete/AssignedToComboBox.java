/*
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Modifications Copyright (C) 2014 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.TextAction;

import org.jdesktop.swingx.autocomplete.workarounds.MacOSXPopupLocationFix;

import teamdash.wbs.AssignedToDocument.Word;


public class AssignedToComboBox extends JComboBox {

    private JTextComponent textComponent;

    private AssignedToComboBoxAdaptor adaptor;

    private AssignedToDocument document;

    public AssignedToComboBox(boolean strict) {
        this(new DefaultComboBoxModel(), strict);
    }

    public AssignedToComboBox(ComboBoxModel aModel, boolean strict) {
        super(aModel);

        // the combobox has to be editable
        setEditable(true);
        // fix the popup location
        MacOSXPopupLocationFix.install(this);

        // install an autocompleting document on the text component
        textComponent = (JTextComponent) getEditor().getEditorComponent();
        adaptor = new AssignedToComboBoxAdaptor(this);
        document = new AssignedToDocument(adaptor, strict);
        textComponent.setDocument(document);

        // add special event handlers, etc
        textComponent.addFocusListener(new FocusHandler());
        textComponent.addCaretListener(new CaretHandler());
        textComponent.addMouseListener(new ClickHandler());
        textComponent.addKeyListener(new KeyHandler());
        tweakKeyBindings(textComponent);
        setFont(getFont().deriveFont(Font.PLAIN));

        setEditor(new AutoCompleteComboBoxEditor(getEditor()));
        setFullText("");
    }

    public void setInitialsList(List<String> acceptableInitials) {
        String fullText = getFullText();
        DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
        model.removeAllElements();
        for (String initial : acceptableInitials)
            model.addElement(initial);
        setFullText(fullText);
    }

    public void setNumbersAllowed(boolean numbersAllowed) {
        document.setNumbersAllowed(numbersAllowed);
    }

    public String getFullText() {
        return textComponent.getText();
    }

    public void setFullText(String text) {
        document.setFullText(text);
        textComponent.setCaretPosition(0);
        adaptor.markCurrentWord();
        discardTrackingState();
    }

    public void setDefaultTime(String time) {
        document.setDefaultTime(time);
    }

    private class FocusHandler extends FocusAdapter {
        // mark current word when the text component gains focus
        public void focusGained(FocusEvent e) {
            adaptor.markCurrentWord();
        }
    }

    private class CaretHandler implements CaretListener {
        // select entire words as appropriate when the caret moves
        public void caretUpdate(final CaretEvent e) {
            if (e.getDot() == e.getMark()) {
                Word word = document.getWord(e.getDot());
                if (word != null && word.beg == e.getDot()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            int caret = textComponent.getCaretPosition();
                            int mark = textComponent.getCaret().getMark();
                            if (e.getDot() == caret && mark == caret)
                                adaptor.markCurrentWord();
                        }
                    });
                }
            }
        }
    }

    private class ClickHandler extends MouseAdapter implements Runnable {
        // select entire words when the user clicks on a new word
        public void mouseClicked(MouseEvent e) {
            SwingUtilities.invokeLater(this);
        }
        public void run() {
            int caretPos = textComponent.getCaretPosition();
            int textLen = textComponent.getText().length();
            if (caretPos != textLen) {
                adaptor.markCurrentWord();
                Word word = document.getWord(caretPos);
                if (word != null && word.isLetters())
                    showPopup();
            }
        }
    }

    private class KeyHandler extends KeyAdapter {
        // show the popup list when the user presses a letter
        public void keyPressed(KeyEvent keyEvent) {
            // don't popup on action keys (cursor movements, etc...)
            if (keyEvent.isActionKey())
                return;
            // don't popup if the combobox isn't visible anyway
            if (isDisplayable() && !isPopupVisible()) {
                char c = keyEvent.getKeyChar();
                if (Character.isLetter(c))
                    setPopupVisible(true);
            }
        }
    }

    private void tweakKeyBindings(JTextComponent textComponent) {
        InputMap inputMap = textComponent.getInputMap();
        // special handling for backspace
        inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), BACKSPACE_ACTION);
        // set left and right to move whole words
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), MOVE_LEFT_ACTION);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), MOVE_RIGHT_ACTION);
        // ignore CTRL-X and beep instead
        inputMap.put(KeyStroke.getKeyStroke("ctrl X"), ERROR_ACTION);
    }

    private final TextAction BACKSPACE_ACTION = new TextAction(
            "autocomplete-backspace") {
        public void actionPerformed(ActionEvent e) {
            int caretPos = textComponent.getCaretPosition();
            int mark = textComponent.getCaret().getMark();
            Word w = document.getWord(caretPos);
            if (w == null) {
                // no word active? move left
                MOVE_LEFT_ACTION.actionPerformed(e);
            } else if (w.beg == caretPos && w.end == mark //
                    || w.end == caretPos && w.beg == mark) {
                // entire word selected? delete it
                regularBackspace().actionPerformed(e);
            } else if (w.isNumber()) {
                // delete a digit from within a number
                regularBackspace().actionPerformed(e);
            } else if (!document.isStrictMatching()
                    && !adaptor.listContainsSelectedItem()) {
                // delete a letter from a nonstrict word
                regularBackspace().actionPerformed(e);
            } else if (caretPos > 0) {
                // move selection backward
                textComponent.setCaretPosition(w.end);
                textComponent.moveCaretPosition(caretPos - 1);
            }
        }

        Action regularBackspace_;

        private Action regularBackspace() {
            if (regularBackspace_ == null)
                regularBackspace_ = textComponent.getActionMap().get(
                    DefaultEditorKit.deletePrevCharAction);
            return regularBackspace_;
        }
    };

    private final TextAction MOVE_LEFT_ACTION = new TextAction(
            "autocomplete-word-left") {
        public void actionPerformed(ActionEvent e) {
            int caretPos = textComponent.getCaretPosition();
            int left = 0;
            for (Word w : document.getWords()) {
                if (w.end < caretPos)
                    left = w.beg;
            }
            textComponent.setCaretPosition(left);
        }
    };

    private final TextAction MOVE_RIGHT_ACTION = new TextAction(
            "autocomplete-word-right") {
        public void actionPerformed(ActionEvent e) {
            int caretPos = textComponent.getCaretPosition();
            int right = textComponent.getText().length();
            for (Word w : document.getWords()) {
                if (w.beg > caretPos) {
                    right = w.beg;
                    break;
                }
            }
            textComponent.setCaretPosition(right);
        }
    };

    private static Object ERROR_ACTION = new TextAction(
            "provide-error-feedback") {
        public void actionPerformed(ActionEvent e) {
            UIManager.getLookAndFeel()
                    .provideErrorFeedback(getTextComponent(e));
        }
    };

    private class AutoCompleteComboBoxEditor implements ComboBoxEditor {
        private final ComboBoxEditor delegate;

        public AutoCompleteComboBoxEditor(ComboBoxEditor delegate) {
            this.delegate = delegate;
        }

        public void setItem(Object anObject) {
            document.setTargetInitials(textComponent.getCaretPosition(),
                (String) anObject, true);
        }

        public Object getItem() {
            return document.getTargetInitials(textComponent.getCaretPosition());
        }

        public void selectAll() {
            // don't select the entire text; just select the current word
            // (the one that will be replaced by the combo box selection)
            adaptor.markCurrentWord();
        }

        public Component getEditorComponent() {
            return textComponent;
        }

        public void addActionListener(ActionListener l) {
            delegate.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            delegate.removeActionListener(l);
        }
    }

    private class TrackableItem {
        Position end;
        String origInitials;
        String origTime;
    }

    private List<TrackableItem> initialTrackedState;

    public void startTrackingChanges() {
        initialTrackedState = new ArrayList();

        List<Word> words = document.getWords();
        Word w = (words.isEmpty() ? null : words.get(0));

        while (w != null) {
            // build trackable items for each set of initials
            TrackableItem item = new TrackableItem();
            item.origInitials = document.getWordText(w);
            int itemEndPos = w.end;
            w = w.next;

            // if the initials are followed by a number, add it to our item info
            if (w != null && w.isNumber()) {
                item.origTime = document.getWordText(w);
                itemEndPos = w.end + 1;
                w = w.next;
            }

            // create a document Position to track the end of this item
            try {
                item.end = document.createPosition(itemEndPos);
                initialTrackedState.add(item);
            } catch (BadLocationException ble) {
            }
        }
    }

    public void discardTrackingState() {
        initialTrackedState = null;
    }

    public AssignedToEditList getTrackedChanges() {
        if (initialTrackedState == null)
            throw new IllegalStateException("No tracked changes");

        AssignedToEditList result = new AssignedToEditList();
        List<Word> words = document.getWords();
        Word w = (words.isEmpty() ? null : words.get(0));

        for (TrackableItem item : initialTrackedState) {
            // create a change representing this original tracked item
            AssignedToEditList.Change change = new AssignedToEditList.Change();
            change.origInitials = item.origInitials;
            change.origTime = item.origTime;
            result.add(change);

            if (w != null && w.end <= item.end.getOffset()) {
                // if a matching set of initials is present in the document,
                // store those as the new initials for this change
                change.newInitials = document.getWordText(w);
                w = w.next;

                // if the initials are followed by a number, add that info to
                // our tracked change too
                if (w != null && w.isNumber()) {
                    change.newTime = document.getWordText(w);
                    w = w.next;
                }
            }
        }

        // if any words are left in the document, they represent additions
        while (w != null) {
            AssignedToEditList.Change change = new AssignedToEditList.Change();
            change.newInitials = document.getWordText(w);
            result.add(change);
            w = w.next;

            // if the initials are followed by a number, add that info to
            // our tracked change too
            if (w != null && w.isNumber()) {
                change.newTime = document.getWordText(w);
                w = w.next;
            }
        }

        return result;
    }

}
