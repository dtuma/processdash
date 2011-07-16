// Copyright (C) 2007-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.util.Date;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.hier.ui.PlainTextNoteFormat;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class HierarchyNote {

    public static class InvalidNoteSpecification extends Exception {
        private InvalidNoteSpecification(String msg) {
            super(msg);
        }

        private InvalidNoteSpecification(Exception e) {
            super(e);
        }
    }

    /** The textual content of the note */
    private String content;

    /**
     * The format used for the content.
     * 
     * @see #FORMAT_PLAIN_TEXT
     */
    private String format = PlainTextNoteFormat.FORMAT_ID;

    /** What type of note does this represent */
    private String flavor;

    /** The name of the person who last modified this note */
    private String author;

    /** The time the note was last modified */
    private Date timestamp;

    public HierarchyNote() {}

    public HierarchyNote(SimpleData val) throws InvalidNoteSpecification {
        this(val.format());
    }

    public HierarchyNote(String xml) throws InvalidNoteSpecification {
        this(parseXML(xml));
    }

    public HierarchyNote(Element xml) throws InvalidNoteSpecification {
        if (!NOTE_TAG.equals(xml.getTagName()))
            throw new InvalidNoteSpecification("Incorrect XML tag - expected "
                    + NOTE_TAG);
        try {
            setContent(XMLUtils.getTextContents(xml), xml
                    .getAttribute(FORMAT_ATTR));
            setAuthor(xml.getAttribute(AUTHOR_ATTR));
            setTimestamp(XMLUtils.getXMLDate(xml, TIMESTAMP_ATTR));
        } catch (IllegalArgumentException iae) {
            throw new InvalidNoteSpecification(iae);
        }
    }

    public String getContent() {
        return content;
    }

    public String getFormat() {
        return format;
    }

    public void setContent(String content, String format) {
        if (!XMLUtils.hasValue(format))
            throw new IllegalArgumentException("Format must be specified");

        this.content = (content == null ? "" : content);
        this.format = format;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        if (XMLUtils.hasValue(author))
            this.author = author;
        else
            this.author = null;
    }

    public String getAsXML() {
        StringBuffer result = new StringBuffer();
        result.append("<").append(NOTE_TAG);

        if (XMLUtils.hasValue(author))
            result.append(" " + AUTHOR_ATTR + "='").append(
                XMLUtils.escapeAttribute(author)).append("'");

        if (timestamp != null)
            result.append(" " + TIMESTAMP_ATTR + "='").append(
                XMLUtils.saveDate(timestamp)).append("'");

        if (XMLUtils.hasValue(format))
            result.append(" " + FORMAT_ATTR + "='").append(
                XMLUtils.escapeAttribute(format)).append("'");

        result.append(">");
        if (content != null)
            result.append(HTMLUtils.escapeEntities(content));
        result.append("</" + NOTE_TAG + ">");

        return result.toString();
    }

    public String getAsHTML() {
        return HierarchyNoteManager.getNoteFormat(format).getAsHTML(this);
    }

    public SimpleData getAsData() {
        return StringData.create(getAsXML());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HierarchyNote) {
            HierarchyNote that = (HierarchyNote) obj;
            return eq(this.content, that.content)
                    && eq(this.author, that.author)
                    && eq(this.format, that.format)
                    && eq(this.timestamp, that.timestamp);
        }
        return false;
    }

    private boolean eq(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null)
            return false;
        return a.equals(b);
    }

    @Override
    public int hashCode() {
        int result = hc(author);
        result = (result << 4) ^ hc(timestamp);
        result = (result << 4) ^ hc(format);
        result = (result << 4) ^ hc(content);
        return result;
    }

    private int hc(Object o) {
        return (o == null ? 0 : o.hashCode());
    }

    private static Element parseXML(String xml) throws InvalidNoteSpecification {
        try {
            return XMLUtils.parse(xml).getDocumentElement();
        } catch (Exception e) {
            throw new InvalidNoteSpecification(e);
        }
    }

    public static final String NOTE_TAG = "note";

    private static final String AUTHOR_ATTR = "author";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String FORMAT_ATTR = "format";

}
