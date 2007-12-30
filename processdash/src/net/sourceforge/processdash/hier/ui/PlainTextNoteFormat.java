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

package net.sourceforge.processdash.hier.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.EventHandler;
import java.util.Date;

import javax.swing.JEditorPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

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

        String text = HTMLUtils.escapeEntities(note.getContent());
        text = StringUtils.findAndReplace(text, "\n", "<br>");
        text = StringUtils.findAndReplace(text, "  ", "&nbsp;&nbsp;");
        html.append(text);

        Date when = note.getTimestamp();
        String who = note.getAuthor();
        if (when != null && who != null) {
            html.append("<hr><div " + BYLINE_CSS + ">");
            html.append(HTMLUtils.escapeEntities(resources.format("ByLine_FMT",
                when, who)));
            html.append("</div>");
        }

        return html.toString();
    }

    public HierarchyNoteEditor getEditor(HierarchyNote note,
            HierarchyNote conflict, HierarchyNote base) {
        if (conflict != null)
            throw new RuntimeException("Not yet implemented!");

        Editor result = new Editor();
        result.setText(note.getContent());
        result.setDirty(false);
        return result;
    }

    public class Editor extends JEditorPane implements HierarchyNoteEditor {

        private boolean dirty;

        private EventListenerList ell;

        private DocumentListener docListener;

        Editor() {
            this.dirty = false;
            this.ell = new EventListenerList();
            this.docListener = EventHandler.create(DocumentListener.class,
                this, "markDirty");

            setEditorKit(new StyledEditorKit());
            setEditable(true);
            setBackground(new Color(255, 255, 200));
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
                }
            }
        }

        public String getContent() {
            return getText();
        }

        public Component getNoteEditorComponent() {
            return this;
        }

        public void markDirty() {
            setDirty(true);
        }

        void setDirty(boolean dirty) {
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

    }

    private static final String BYLINE_CSS = "style='text-align:right; color:#808080; font-style:italic'";

}
