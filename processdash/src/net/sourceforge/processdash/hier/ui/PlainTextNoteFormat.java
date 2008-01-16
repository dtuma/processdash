// Copyright (C) 2007-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;

import javax.swing.JEditorPane;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.ThreeWayDiff;
import net.sourceforge.processdash.util.ThreeWayTextDiff;
import net.sourceforge.processdash.util.ThreeWayDiff.ResultItem;

public class PlainTextNoteFormat implements HierarchyNoteFormat {

    /** Format constant indicating that note content is in plain text */
    public static final String FORMAT_ID = "text";

    private static final Resources resources = Resources.getDashBundle("Notes");

    public String getID() {
        return FORMAT_ID;
    }

    public String getAsHTML(HierarchyNote note) {
        if (note == null)
            return null;

        StringBuffer html = new StringBuffer();
        html.append(HTMLUtils.escapeEntities(note.getContent()));
        StringUtils.findAndReplace(html, "\n", "<br>");
        StringUtils.findAndReplace(html, "  ", "&nbsp;&nbsp;");
        return html.toString();
    }

    public HierarchyNoteEditor getEditor(HierarchyNote note,
            HierarchyNote conflict, HierarchyNote base) {

        Editor result = new Editor();
        Document doc = result.getDocument();

        if (conflict != null) {
            String baseText = base.getContent();
            String conflictText = conflict.getContent();
            String localText = note.getContent();

            ThreeWayDiff.ResultItem<String>[] diffResults =
                ThreeWayTextDiff.compareTextByWords(baseText, conflictText, localText);

            result.generateDocument(doc, diffResults);
            result.conflictAuthor = conflict.getAuthor();

            result.setDirty(true);
        }
        else if (note != null){
            result.setText(note.getContent());
            result.setDirty(false);
        }

        AttributedInsertDocument aDoc = (AttributedInsertDocument) doc;
        aDoc.setInsertAttrs(result.attrAddedLocally);

        return result;
    }

    public class Editor extends JEditorPane implements HierarchyNoteEditor {

        /** The styles names used to display a note's content in various state */
        private static final String STYLE_ADDED_LOCALLY = "AddedLocally";
        private static final String STYLE_ADDED_BY_OTHER = "AddedByOther";
        private static final String STYLE_REMOVED_LOCALLY = "RemovedLocally";
        private static final String STYLE_REMOVED_BY_OTHER = "RemovedByOther";

        /** The attributes used to display a note's content in various state */
        private Style attrNormal = null;
        private Style attrAddedByOther = null;
        private Style attrAddedLocally = null;
        private Style attrRemovedByOther = null;
        private Style attrRemovedLocally = null;

        /** If the note has conflicts, we need to know know made them */
        private String conflictAuthor;

        private boolean dirty;

        private EventListenerList ell;

        private DocumentListener docListener;

        Editor() {
            this.dirty = false;
            this.ell = new EventListenerList();
            this.docListener = EventHandler.create(DocumentListener.class,
                this, "markDirty");

            setEditorKit(new AttributedEditorKit());
            setEditable(true);
            setBackground(new Color(255, 255, 200));
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        private void generateDocument(Document doc, ResultItem<String>[] diffResults) {
            for (int i = 0; i < diffResults.length; ++i) {
                ResultItem<String> currentResult = diffResults[i];
                SimpleAttributeSet attrToUse = null;

                if (currentResult.isInsertedByA())
                    attrToUse = new SimpleAttributeSet(attrAddedByOther);
                else if (currentResult.isInsertedByB())
                    attrToUse = new SimpleAttributeSet(attrAddedLocally);
                else if (currentResult.isDeletedByA())
                    attrToUse = new SimpleAttributeSet(attrRemovedByOther);
                else if (currentResult.isDeletedByB())
                    attrToUse = new SimpleAttributeSet(attrRemovedLocally);
                else
                    attrToUse = new SimpleAttributeSet(attrNormal);

                try {
                    doc.insertString(doc.getLength(), currentResult.item, attrToUse);
                } catch (BadLocationException e) { /* Should not happen */ }
            }
        }

        @Override
        public void setDocument(Document doc) {
            Document oldDoc = getDocument();
            if (oldDoc != null)
                oldDoc.removeDocumentListener(docListener);

            super.setDocument(doc);

            if (doc != null) {
                doc.addDocumentListener(docListener);
                if (doc instanceof StyledDocument) {
                    StyledDocument sd = (StyledDocument) doc;
                    Style style = sd.getStyle(StyleContext.DEFAULT_STYLE);
                    StyleConstants.setFontFamily(style, Font.SANS_SERIF);

                    attrNormal = style;

                    attrAddedLocally = sd.addStyle(STYLE_ADDED_LOCALLY, attrNormal);
                    StyleConstants.setForeground(attrAddedLocally, Color.BLUE);
                    StyleConstants.setBold(attrAddedLocally, true);

                    attrAddedByOther = sd.addStyle(STYLE_ADDED_BY_OTHER, attrNormal);
                    StyleConstants.setForeground(attrAddedByOther, Color.RED);
                    StyleConstants.setBold(attrAddedByOther, true);

                    attrRemovedByOther = sd.addStyle(STYLE_REMOVED_BY_OTHER, attrNormal);
                    StyleConstants.setForeground(attrRemovedByOther, Color.RED);
                    StyleConstants.setStrikeThrough(attrRemovedByOther, true);
                    StyleConstants.setBold(attrRemovedByOther, true);

                    attrRemovedLocally = sd.addStyle(STYLE_ADDED_LOCALLY, attrNormal);
                    StyleConstants.setForeground(attrRemovedLocally, Color.BLUE);
                    StyleConstants.setStrikeThrough(attrRemovedLocally, true);
                    StyleConstants.setBold(attrRemovedLocally, true);
                }
            }
        }

        public String getContent() {
            StringBuffer result = new StringBuffer();
            try {
                getContent(result, getDocument().getDefaultRootElement());
            } catch (BadLocationException e1) { /* shouldn't happen */}
            return result.toString().trim();
        }

        private void getContent(StringBuffer result, Element e) throws BadLocationException {
            if (e.isLeaf()) {
                if (!StyleConstants.isStrikeThrough(e.getAttributes())) {
                    int len = e.getEndOffset() - e.getStartOffset();
                    result.append(getDocument().getText(e.getStartOffset(), len));
                }
            } else {
                for (int i = 0;  i < e.getElementCount();  i++)
                    getContent(result, e.getElement(i));
            }

        }

        public Component getNoteEditorComponent() {
            return this;
        }

        public void markDirty() {
            setDirty(true);
        }

        public void setDirty(boolean dirty) {
            if (this.dirty != dirty) {
                this.dirty = dirty;
                ChangeEvent e = new ChangeEvent(this);
                for (ChangeListener l : ell.getListeners(ChangeListener.class)) {
                    l.stateChanged(e);
                }
            }
        }

        public boolean isDirty() {
            return dirty;
        }

        public void addDirtyListener(ChangeListener l) {
            ell.add(ChangeListener.class, l);
            if (dirty)
                l.stateChanged(new ChangeEvent(this));
        }

        public void removeDirtyListener(ChangeListener l) {
            ell.remove(ChangeListener.class, l);
        }

        public String getFormatID() {
            return PlainTextNoteFormat.FORMAT_ID;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            Point pt = event.getPoint();

            if (pt == null) return null;

            int docLocation = viewToModel(pt);

            if (!textPresentUnderCursor(docLocation)) return null;

            AttributeSet attrs = getStyleAt(getDocument().getDefaultRootElement(), docLocation);
            String styleNameBeingHovered = (String)attrs.getAttribute(StyleConstants.NameAttribute);

            if (styleNameBeingHovered == null) return null;

            if (styleNameBeingHovered.equals(STYLE_ADDED_LOCALLY))
                return resources.format("Editor.Tooltip.Added_FMT",
                                        resources.getString("Editor.Tooltip.You"));

            if (styleNameBeingHovered.equals(STYLE_ADDED_BY_OTHER))
                return resources.format("Editor.Tooltip.Added_FMT", conflictAuthor);

            if (styleNameBeingHovered.equals(STYLE_REMOVED_LOCALLY))
                return resources.format("Editor.Tooltip.Removed_FMT",
                                        resources.getString("Editor.Tooltip.You"));

            if (styleNameBeingHovered.equals(STYLE_REMOVED_BY_OTHER))
                return resources.format("Editor.Tooltip.Removed_FMT", conflictAuthor);

            return null;
        }

        private boolean textPresentUnderCursor(int pos) {
            try {
                return getDocument().getText(pos, 1).matches(".");
            }
            catch (BadLocationException e) {/* Should not happen*/}

            return false;
        }

        private AttributeSet getStyleAt(Element e, int pos) {
            if (pos < e.getStartOffset() || pos >= e.getEndOffset())
                return null;
            else if (e.isLeaf())
                return e.getAttributes();
            else {
                for (int i = 0;  i < e.getElementCount();  i++) {
                    AttributeSet result = getStyleAt(e.getElement(i), pos);

                    if (result != null)
                        return result;
                }
            }
            return null;
        }
    }

    private class AttributedInsertDocument extends DefaultStyledDocument {

        private AttributeSet insertAttrs;

        public AttributeSet getInsertAttrs() {
            return insertAttrs;
        }

        public void setInsertAttrs(AttributeSet insertAttrs) {
            this.insertAttrs = insertAttrs;
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (insertAttrs != null)
                a = insertAttrs;

            super.insertString(offs, str, a);
        }

    }

    private class AttributedEditorKit extends StyledEditorKit {

        @Override
        public Document createDefaultDocument() {
            return new AttributedInsertDocument();
        }

    }

}
