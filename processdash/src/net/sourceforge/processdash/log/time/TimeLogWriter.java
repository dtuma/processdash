// Copyright (C) 2005-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;

import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class TimeLogWriter implements TimeLogIOConstants {

    private static final String NEWLINE = System.getProperty("line.separator");

    public static void write(File f, Iterator timeLogEntries)
            throws IOException {
        write(new RobustFileOutputStream(f), timeLogEntries, true);
    }

    public static void write(OutputStream out, Iterator timeLogEntries)
            throws IOException {
        write(out, timeLogEntries, true);
    }

    public static void write(OutputStream out, Iterator timeLogEntries,
            boolean close) throws IOException {
        if (!(out instanceof BufferedOutputStream))
            out = new BufferedOutputStream(out);
        write(new OutputStreamWriter(out, ENCODING), timeLogEntries, close);
    }

    public static void write(Writer out, Iterator timeLogEntries)
            throws IOException {
        write(out, timeLogEntries, true);
    }

    public static void write(Writer out, Iterator timeLogEntries, boolean close)
            throws IOException {
        XmlSerializer ser = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            ser = factory.newSerializer();
        } catch (XmlPullParserException xppe) {
            throw new RuntimeException("Couldn't obtain xml serializer", xppe);
        }

        ser.setOutput(out);
        ser.startDocument(ENCODING, null);
        ser.ignorableWhitespace(NEWLINE);
        ser.startTag(null, DOC_ROOT_ELEM);
        ser.ignorableWhitespace(NEWLINE);

        try {
            while (timeLogEntries.hasNext())
                writeTimeLogEntry(ser, (TimeLogEntry) timeLogEntries.next());

        } catch (IONoSuchElementException ionsee) {
            throw ionsee.getIOException();
        }

        ser.endTag(null, DOC_ROOT_ELEM);
        ser.ignorableWhitespace(NEWLINE);
        ser.endDocument();

        if (close)
            out.close();
        else
            out.flush();
    }

    private static void writeTimeLogEntry(XmlSerializer ser, TimeLogEntry entry)
            throws IOException {
        ser.ignorableWhitespace("  ");
        ser.startTag(null, TIME_ELEM);

        writeAttr(ser, ID_ATTR, entry.getID());
        writeAttr(ser, PATH_ATTR, entry.getPath());
        writeAttr(ser, START_ATTR, entry.getStartTime());
        writeAttr(ser, DELTA_ATTR, entry.getElapsedTime());
        writeAttr(ser, INTERRUPT_ATTR, entry.getInterruptTime());
        writeAttr(ser, COMMENT_ATTR, entry.getComment());
        if (entry instanceof ChangeFlagged) {
            int changeFlag = ((ChangeFlagged) entry).getChangeFlag();
            if (changeFlag != 0)
                writeAttr(ser, FLAG_ATTR,
                          Character.toString(FLAG_CHARS.charAt(changeFlag)));
        }

        ser.endTag(null, TIME_ELEM);
        ser.ignorableWhitespace(NEWLINE);
    }

    private static void writeAttr(XmlSerializer ser, String name, Date value)
            throws IOException {
        if (value != null)
            writeAttr(ser, name, XMLUtils.saveDate(value));
    }

    private static void writeAttr(XmlSerializer ser, String name, long value)
            throws IOException {
        if (value != 0)
            writeAttr(ser, name, Long.toString(value));
    }

    private static void writeAttr(XmlSerializer ser, String name, String value)
            throws IOException {
        if (value != null)
            ser.attribute(null, name, value);
    }
}
