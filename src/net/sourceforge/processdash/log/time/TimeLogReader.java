// Copyright (C) 2005 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.NoSuchElementException;

import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class TimeLogReader implements EnumerIterator, TimeLogIOConstants {

    private static final String FEATURE_RELAXED =
        "http://xmlpull.org/v1/doc/features.html#relaxed";

    private File logFile;
    private InputStream in;
    private boolean close;
    private XmlPullParser parser;
    private TimeLogEntry nextEntry;


    public TimeLogReader(File logFile) throws IOException {
        this(logFile.isFile() ? new FileInputStream(logFile) : null);
        this.logFile = logFile;
    }

    public TimeLogReader(InputStream in) throws IOException {
        this(in, true);
    }

    public TimeLogReader(InputStream in, boolean close) throws IOException {
        if (in != null)
            try {
                this.in = in;
                this.close = close;
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                parser = factory.newPullParser();
                try {
                    parser.setFeature(FEATURE_RELAXED, true);
                } catch (Exception e) {}
                parser.setInput(in, ENCODING);
                fill();
            } catch (XmlPullParserException xppe) {
                throw new RuntimeException("Couldn't obtain xml parser", xppe);
            } catch (IONoSuchElementException ionsee) {
                throw ionsee.getIOException();
            }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return nextEntry != null;
    }

    public Object next() {
        if (nextEntry == null)
            throw new NoSuchElementException();

        Object result = nextEntry;
        fill();
        return result;
    }

    public boolean hasMoreElements() {
        return hasNext();
    }

    public Object nextElement() {
        return next();
    }

    private void close() {
        if (close && in != null)
            try {
                in.close();
            } catch (IOException ioe) {}
        in = null;
        nextEntry = null;
    }

    private void fill() {
        while (true) {
            try {
                switch (parser.next()) {

                case XmlPullParser.END_DOCUMENT:
                    close();
                    return;

                case XmlPullParser.START_TAG:
                    if (TIME_ELEM.equals(parser.getName()))
                        nextEntry = parseTimeElement();
                    if (nextEntry != null)
                        return;

                }
            } catch (IOException ioe) {
                close();
                throw new IONoSuchElementException(ioe);
            } catch (XmlPullParserException xppe) {
                close();
                throw new IONoSuchElementException(xppe);
            }
        }
    }

    private TimeLogEntry parseTimeElement() {
        try {
            long id = getLong(ID_ATTR);
            String path = getAttr(PATH_ATTR);
            Date startTime = XMLUtils.parseDate(getAttr(START_ATTR));
            long elapsedTime = getLong(DELTA_ATTR);
            long interruptTime = getLong(INTERRUPT_ATTR);
            String comment = getAttr(COMMENT_ATTR);
            int flag = parseFlagChar(getAttr(FLAG_ATTR));
            if (id == 0 && flag != ChangeFlagged.BATCH_MODIFICATION)
                throw new IllegalArgumentException();

            TimeLogEntryVO result = new TimeLogEntryVO(id, path, startTime,
                    elapsedTime, interruptTime, comment, flag);
            return result;

        } catch (Exception e) {
            System.out.println
                ("WARNING: discarding garbled time log entry on line "
                            + parser.getLineNumber() + " of file " + logFile);
            return null;
        }
    }

    private long getLong(String name) {
        String value = getAttr(name);
        if (value == null)
            return 0;
        else
            return Long.parseLong(value);
    }

    private String getAttr(String name) {
        return parser.getAttributeValue(null, name);
    }

    private int parseFlagChar(String flagStr) {
        if (flagStr == null)
            return 0;
        else
            return Math.max(0, FLAG_CHARS.indexOf(flagStr));
    }
}
