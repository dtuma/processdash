/*
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Modifications Copyright (C) 2014-2017 Tuma Solutions, LLC
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
package net.sourceforge.processdash.ui.lib.autocomplete;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * A document that can be used to edit the value in an "assigned to" field,
 * enforcing syntax rules and providing autocompletion.
 */
public class AssignedToDocument extends PlainDocument {

    public static final boolean DECIMAL_IS_COMMA = localeUsesCommaForDecimal();

    public static final char SEPARATOR_CHAR = (DECIMAL_IS_COMMA ? ';' : ',');

    public static final String SEPARATOR = String.valueOf(SEPARATOR_CHAR);

    public static final String SEPARATOR_SPACE = SEPARATOR + " ";


    /**
     * Flag to indicate if adaptor.setSelectedItem has been called. Subsequent
     * calls to remove/insertString should be ignored as they are likely have
     * been caused by the adapted Component that is trying to set the text for
     * the selected component.
     */
    private boolean selecting = false;

    /**
     * Flag indicating that the last edit was the insertion of a separator char
     */
    private boolean justInsertedSeparator = false;

    /**
     * The adaptor that is used to find and select items.
     */
    private AssignedToComboBoxAdaptor adaptor;

    /**
     * Flag to indicate whether textual words should be limited to the items
     * provided by the adaptor
     */
    private boolean strictMatching;

    /**
     * Flag indicating whether numbers can be entered in this document
     */
    private boolean numbersAllowed = true;

    /**
     * The default time to list for an individual, if we need to insert a number
     * as a placeholder
     */
    private String defaultTime = "0";

    /**
     * The pattern to use to identify editable words in the document.
     */
    private Pattern wordPattern = DEFAULT_WORD_PAT;

    /**
     * Flag indicating whether we should use enhanced logic (checking the
     * wordPattern) to determine if a character is a letter.
     */
    private boolean testLettersAgainstPattern = false;

    // local versions of the well-known separator constants
    private char separatorChar = SEPARATOR_CHAR;
    private String separator = SEPARATOR;
    private String separatorSpace = SEPARATOR_SPACE;


    /**
     * Creates a new AssignedToDocument.
     * 
     * @param adaptor
     *            The adaptor that will be used to find and select matching
     *            items.
     * @param strict
     *            true if selections should be limited to the items provided by
     *            the adaptor; false if the user can type other values
     */
    public AssignedToDocument(AssignedToComboBoxAdaptor adaptor, boolean strict) {
        this.adaptor = adaptor;
        this.strictMatching = strict;
    }

    /**
     * Returns if only items from the adaptor's list should be allowed to be
     * entered.
     * 
     * @return if only items from the adaptor's list should be allowed to be
     *         entered
     */
    public boolean isStrictMatching() {
        return strictMatching;
    }

    public boolean isNumbersAllowed() {
        return numbersAllowed;
    }

    public void setNumbersAllowed(boolean numbersAllowed) {
        this.numbersAllowed = numbersAllowed;
    }

    public void setFullText(String value) {
        try {
            super.remove(0, getLength());
            super.insertString(0, value, null);
        } catch (Exception e) {
        }
    }

    public void setDefaultTime(String defaultTime) {
        this.defaultTime = defaultTime;
    }

    public void setWordPattern(Pattern wordPattern) {
        this.wordPattern = wordPattern;
        this.testLettersAgainstPattern = true;
    }

    public char getSeparatorChar() {
        return separatorChar;
    }

    public void setSeparatorChar(char sep) {
        this.separatorChar = sep;
        this.separator = String.valueOf(sep);
        this.separatorSpace = separator + " ";
    }

    @Override
    public void replace(int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        // return immediately when selecting an item
        if (selecting)
            return;

        try {
            if (length <= 0) {
                // nothing to delete? perform a plain insertion
                insertString(offset, text, attrs);

            } else if (text == null || text.length() == 0) {
                // nothing to insert? perform a plain deletion
                remove(offset, length);

            } else {
                // don't allow the user to replace multiple words
                Word w = getWord(offset);
                if (w != null && offset + length > w.end)
                    length = w.end - offset;
                insertOrReplaceString(offset, length, text, attrs);
            }
        } catch (BadInputException bie) {
        }
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        // return immediately when selecting an item
        if (selecting)
            return;

        try {
            int end = offs + len;
            boolean deletedWord = false;
            justInsertedSeparator = false;

            // iterate over the words, finding ones the user wants to remove
            List<Word> words = getWords();
            for (int i = words.size(); i-- > 0;) {
                Word w = words.get(i);
                if (w.beg >= end || w.end <= offs)
                    // this word isn't in the deletion range
                    continue;

                int delBeg = Math.max(offs, w.beg);
                int delEnd = Math.min(end, w.end);
                int delLen = delEnd - delBeg;

                if (w.isNumber()) {
                    deletedWord = true;
                    if (delLen < w.len())
                        // delete a portion of a number
                        super.remove(delBeg, delLen);
                    else
                        // delete entire number, and the surrounding parens
                        super.remove(w.beg - 1, w.len() + 2);

                } else if (delLen == w.len()) {
                    // delete an entire set of initials, plus the number that
                    // follows, and any relevant punctuation.
                    Word next = w.next;
                    if (next != null && next.isNumber())
                        next = next.next;
                    delEnd = (next != null ? next.beg : getLength());
                    adaptor.setCaret(delEnd);
                    if (delBeg > 0) {
                        delBeg = skipPriorSeparation(delBeg);
                        delEnd = skipPriorSeparation(delEnd);
                    }
                    super.remove(delBeg, delEnd - delBeg);
                    deletedWord = true;

                } else if (!strictMatching) {
                    // delete a portion of some letters
                    super.remove(delBeg, delLen);
                    deletedWord = true;

                    // if we just altered the current word, update the selected
                    // item in the combo box model
                    if (w.beg <= offs && offs <= w.end) {
                        w = getWord(offs);
                        setSelectedItem(getWordText(w));
                        adaptor.setCaret(offs);
                    }
                }

                // recalculate the positions of all remaining words, as they
                // may have shifted as a result of deletions just performed
                words = getWords();
            }

            // if we didn't find anything valid to delete, signal  an error
            if (deletedWord == false)
                badInput();

        } catch (BadInputException bie) {
        }
    }

    private int skipPriorSeparation(int pos) throws BadLocationException {
        while (pos > 0) {
            char ch = getText(pos - 1, 1).charAt(0);
            if (ch == ' ' || ch == separatorChar)
                pos--;
            else
                break;
        }
        return pos;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet a)
            throws BadLocationException {
        insertOrReplaceString(offset, 0, str, a);
    }

    private void insertOrReplaceString(int offset, int len, String str,
            AttributeSet a) throws BadLocationException {
        // return immediately when selecting an item
        if (selecting)
            return;

        // nothing to insert? abort
        if (str == null || str.length() == 0)
            return;
        char ch = str.charAt(0);

        try {
            // branch based on what the user is trying to insert
            if (isLetter(ch)) {
                tryInsertInitials(offset, len, str);
            } else if (isNumber(ch)) {
                tryInsertNumber(offset, len, str);
            } else if (isPunctuation(ch)) {
                tryInsertPunctuation(offset, ch);
            } else {
                badInput();
            }
            justInsertedSeparator = (ch == separatorChar);
        } catch (BadInputException bie) {
        }
    }

    private void tryInsertInitials(int offset, int len, String str)
            throws BadLocationException {

        // find out what word the user is currently editing
        Word w = getWord(offset);

        // if the highlight is currently on a number, edit the next word
        // instead, or append to the document
        if (w != null && w.isNumber()) {
            w = w.next;
            if (w != null) {
                offset = w.beg;
                len = w.len();
                adaptor.markWord(w);
            } else {
                offset = getLength();
                len = 0;
                adaptor.setCaret(offset);
            }
        }

        // determine the prefix of the intitials that have been typed so far
        StringBuilder initials = new StringBuilder();
        if (w != null && offset > w.beg)
            initials.append(getText(w.beg, offset - w.beg));
        initials.append(str);

        // lookup and select a matching item.
        String lookupResult = lookupItem(initials.toString());
        if (lookupResult == null) {
            if (strictMatching) {
                badInput();
            } else {
                int pos = offset + len;
                if (w != null && pos >= w.beg && pos < w.end)
                    initials.append(getText(pos, w.end - pos));
                lookupResult = initials.toString();
            }
        }

        setSelectedItem(lookupResult);
        if (w != null) {
            // editing/replacing an existing word
            int pos = w.beg;
            super.insertString(pos, lookupResult, null);
            super.remove(pos + lookupResult.length(), w.len());
            adaptor.markText(pos + initials.length(), //
                pos + lookupResult.length());

        } else {
            // appending new match to the end
            setSelectedItem(lookupResult);
            appendInitials(lookupResult, initials.length());
        }
    }

    private void appendInitials(String initials, int numUnselectedChars)
            throws BadLocationException {
        // appending initials to the end. Add a separator if necessary
        ensureFinalSeparator();
        int offset = getLength();
        super.insertString(offset, initials, null);
        adaptor.markText(offset + numUnselectedChars, getLength());
    }

    private void ensureFinalSeparator() throws BadLocationException {
        // ensure that the document ends with a final separator and space
        int endPos = getLength();
        String currentText = getText(0, endPos);
        if (currentText.endsWith(separatorSpace)) {
            // separator already present
        } else if (currentText.endsWith(separator)) {
            // insert space after final separator
            super.insertString(endPos, " ", null);
        } else if (currentText.endsWith(" ")) {
            // insert separator before final space
            super.insertString(endPos - 1, separator, null);
        } else if (currentText.length() > 0) {
            // append separator and space char
            super.insertString(endPos, separatorSpace, null);
        }
    }

    private void tryInsertNumber(int offset, int len, String str)
            throws BadLocationException {

        // make certain the inserted value only contains number chars
        for (int i = str.length(); i-- > 0;)
            if (!isNumber(str.charAt(i)))
                badInput();

        // find out what word the user is currently editing
        List<Word> words = getWords();
        Word w = getWord(words, offset);

        // if we are not in any word, decide what to do next
        if (w == null) {
            // if we are within the document, but not in a word (e.g., between
            // punctuation chars), abort
            if (offset != getLength())
                badInput();
            // we are at the end of the document. if the doc is empty, abort
            if (words.isEmpty())
                badInput();
            // get the final word. If it is a number, abort.
            w = words.get(words.size() - 1);
            if (w.isNumber())
                badInput();
            // the final word (just preceding the insertion point) is initials.
            // continue with the block below for proper handling.
        }

        // if the user just typed a comma, treat it as word separator
        // punctuation (even if a comma is used for the decimal point in the
        // current locale) under certain conditions:
        // * if the cursor is on letters. This will avoid a very predictable
        //   source of user confusion, since legacy users are accustomed to
        //   separating numberless initials with a comma.
        // * if the cursor is at the beginning of the number. For parsing
        //   purposes we don't allow numbers to begin with a comma.
        if (str.charAt(0) == ',' && (w.isLetters() || offset == w.beg)) {
            tryInsertPunctuation(offset, separatorChar);
            return;
        }

        // should the user be forbidden from entering numbers?
        if (numbersAllowed == false)
            badInput();

        // if the cursor is currently on initials, edit a number instead; either
        // one that already follows these initials, or a newly inserted number
        if (w.isLetters()) {
            Word next = w.next;
            if (next != null && next.isNumber()) {
                // edit the number that follows this word.
                w = next;
                offset = next.beg;
                len = w.len();
            } else {
                // remove any trailing spaces after these initials, then insert
                // a new number for the user to edit.
                int pos = w.end;
                while (pos < getLength() && " ".equals(getText(pos, 1)))
                    super.remove(pos, 1);
                super.insertString(pos, "()", null);
                if (str.charAt(0) == '.')
                    str = "0" + str;
                offset = pos + 1;
                len = 0;
            }
        }

        // insert the numbers into the document.
        if (len > 0)
            super.remove(offset, len);
        super.insertString(offset, str, null);
        adaptor.setCaret(offset + str.length());
    }

    private void tryInsertPunctuation(int offset, char ch)
            throws BadLocationException {

        if (ch == ' ') {
            // when the user types a space, try moving a word to the right
            List<Word> words = getWords();
            if (words.isEmpty())
                badInput();
            if (justInsertedSeparator)
                // separator followed by space should not cause a double-move
                return;
            for (Word w : words) {
                if (w.beg > offset) {
                    adaptor.markWord(w);
                    return;
                }
            }
            // if we can't move a word to the right, move to the end of the
            // document, adding a space there if necessary.
            int len = getLength();
            if (!" ".equals(getText(len - 1, 1)))
                super.insertString(len++, " ", null);
            adaptor.setCaret(len);

        } else if (ch == separatorChar) {
            // when the user types a separator, try moving the caret to the
            // next set of initials
            List<Word> words = getWords();
            if (words.isEmpty())
                badInput();
            for (Word w : words) {
                if (w.isLetters() && w.beg > offset) {
                    adaptor.markWord(w);
                    return;
                }
            }

            // if we were already on the last set of initials, go to the end of
            // the document, appending a separator there if necessary.
            ensureFinalSeparator();
            int endPos = getLength();
            adaptor.setCaret(endPos);

        } else if (ch == '(') {
            // If the user isn't currently in a word of letters, abort.
            Word w = getWord(offset);
            if (w == null || w.isNumber() || !numbersAllowed)
                badInput();

            Word next = w.next;
            if (next != null && next.isNumber()) {
                // if these letters are followed by a number, select it.
                adaptor.markWord(next);
            } else {
                // otherwise, insert the default time and select that.
                super.insertString(w.end, "(" + defaultTime + ")", null);
                adaptor.markText(w.end + 1, w.end + 1 + defaultTime.length());
            }

        } else if (ch == ')') {
            Word w = getWord(offset);
            if (w == null || w.isLetters())
                badInput();

            Word next = w.next;
            if (next != null) {
                // select the word following these numbers.
                adaptor.markWord(next);
            } else {
                // otherwise, move to the end of the document.
                adaptor.setCaret(getLength());
            }
        }

    }

    private boolean isLetter(char c) {
        if (Character.isLetter(c))
            return true;
        else if (testLettersAgainstPattern)
            return wordPattern.matcher("A" + c).matches();
        else
            return false;
    }

    private boolean isNumber(char c) {
        return Character.isDigit(c) || c == (DECIMAL_IS_COMMA ? ',' : '.');
    }

    private boolean isPunctuation(char c) {
        return c == ' ' || c == '(' || c == ')' || c == separatorChar;
    }

    private void badInput() {
        UIManager.getLookAndFeel().provideErrorFeedback(
            adaptor.getTextComponent());
        throw new BadInputException();
    }

    private class BadInputException extends RuntimeException {
    }

    /**
     * Selects the given item using the AbstractAutoCompleteAdaptor.
     * 
     * @param itemAsString
     *            string representation of the item to be selected
     * @param item
     *            the item that is to be selected
     */
    void setSelectedItem(String item) {
        selecting = true;
        adaptor.setSelectedItem(item);
        adaptor.setSelectedItemAsString(item);
        selecting = false;
    }

    /**
     * Searches for an item that matches the given pattern. The
     * AbstractAutoCompleteAdaptor is used to access the candidate items. The
     * match is not case-sensitive and will only match at the beginning of each
     * item's string representation.
     * 
     * @param pattern
     *            the pattern that should be matched
     * @return the first item that matches the pattern or <code>null</code> if
     *         no item matches
     */
    private String lookupItem(String pattern) {

        // iterate over all items to find an exact match
        for (int i = 0, n = adaptor.getItemCount(); i < n; i++) {
            String oneItem = adaptor.getItem(i);
            if (pattern.equalsIgnoreCase(oneItem))
                return oneItem;
        }

        // check if the currently selected item matches
        String selectedItem = adaptor.getSelectedItem();
        if (startsWithIgnoreCase(selectedItem, pattern))
            return selectedItem;

        // search for any matching item, if the currently selected does not
        // match
        for (int i = 0, n = adaptor.getItemCount(); i < n; i++) {
            String oneItem = adaptor.getItem(i);
            if (startsWithIgnoreCase(oneItem, pattern))
                return oneItem;
        }

        // no item starts with the pattern => return null
        return null;
    }

    /**
     * Returns true if <code>base</code> starts with <code>prefix</code>
     * (ignoring case).
     * 
     * @param base
     *            the string to be checked
     * @param prefix
     *            the prefix to check for
     * @return true if <code>base</code> starts with <code>prefix</code>; false
     *         otherwise
     */
    private static boolean startsWithIgnoreCase(String base, String prefix) {
        if (base == null || prefix == null || base.length() < prefix.length())
            return false;
        return base.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean localeUsesCommaForDecimal() {
        String num = NumberFormat.getNumberInstance().format(1.1);
        return num.indexOf(',') != -1;
    }


    public List<Word> getWords() {
        List<Word> result = new ArrayList();
        try {
            Word prev = null;
            Matcher m = wordPattern.matcher(getText(0, getLength()));
            while (m.find()) {
                Word w = new Word();
                w.beg = m.start();
                w.end = m.end();
                w.letters = m.start(1) != -1;
                w.prev = prev;
                if (prev != null)
                    prev.next = w;
                result.add(w);
                prev = w;
            }
        } catch (BadLocationException ble) {
        }
        return result;
    }

    private static final Pattern DEFAULT_WORD_PAT = Pattern
            .compile("(\u00AB?\\p{L}+\u00BB?)|([0-9.][0-9.,]*)");

    public Word getWord(int pos) {
        return getWord(getWords(), pos);
    }

    private Word getWord(List<Word> words, int pos) {
        for (Word w : words) {
            if (w.beg <= pos && pos <= w.end)
                return w;
        }
        return null;
    }

    public String getWordText(Word w) {
        try {
            return getText(w.beg, w.len());
        } catch (BadLocationException ble) {
            return null;
        }
    }

    public void setTargetInitials(int pos, String word, boolean append) {
        if (selecting || word == null || !wordPattern.matcher(word).matches())
            return;

        Word w = getWord(pos);
        try {
            if (w != null && w.isNumber())
                w = w.prev;
            if (w != null && w.isLetters()) {
                super.insertString(w.beg, word, null);
                super.remove(w.beg + word.length(), w.len());
                w = getWord(w.beg);
            } else if (append) {
                appendInitials(word, 0);
                w = getWord(getLength());
            }
        } catch (BadLocationException ble) {
        }
        adaptor.markWord(w);
    }

    public String getTargetInitials(int pos) {
        Word w = getWord(pos);
        if (w != null && w.isNumber())
            w = w.prev;
        return (w == null ? null : getWordText(w));
    }

    public static class Word {

        public int beg, end;

        public boolean letters;

        public Word next, prev;

        public int len() {
            return end - beg;
        }

        public boolean isLetters() {
            return letters;
        }

        public boolean isNumber() {
            return !letters;
        }
    }

}
