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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.IDSource;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FormatUtil;

public class OldStyleTimeLogReader implements EnumerIterator {

    private BufferedReader in;

    private List entriesRead;

    private IDSource idSource;

    public OldStyleTimeLogReader(File file, IDSource idSource) throws IOException {
        this(file.isFile() ? new FileReader(file) : null, idSource);
    }

    public OldStyleTimeLogReader(InputStream in) throws IOException {
        this(new InputStreamReader(in), new DummyIDSource());
    }

    private OldStyleTimeLogReader(Reader in, IDSource idSource) throws IOException {
        this.in = (in == null ? null : new BufferedReader(in));
        this.idSource = idSource;
        this.entriesRead = new LinkedList();
        fill();
    }

    private void fill() throws IOException {
        if (in == null)
            return;

        String line;
        while (entriesRead.size() < 50) {
            line = readOneLine();
            if (line == null)
                break;

            if (line.startsWith(CONTINUATION_FLAG)) {
                line = line.substring(CONTINUATION_FLAG.length());
                entriesRead.remove(entriesRead.size() - 1);
            }

            try {
                TimeLogEntryVO tle = parseOldStyleEntry(-1, line);
                entriesRead.add(tle);
            } catch (IllegalArgumentException iae) {
            }
        }
    }

    private String readOneLine() throws IOException {
        if (in == null)
            return null;

        String line = in.readLine();
        if (line == null) {
            try {
                in.close();
            } catch (IOException ioe) {
            }
            in = null;
        }
        return line;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return !entriesRead.isEmpty();
    }

    public Object next() {
        if (entriesRead.size() < 5) {
            try {
                fill();
            } catch (IOException e) {
                throw new IONoSuchElementException(e);
            }
        }

        if (entriesRead.isEmpty())
            throw new NoSuchElementException();

        TimeLogEntryVO result = (TimeLogEntryVO) entriesRead.remove(0);
        result.ID = idSource.getNextID();
        return result;
    }

    public boolean hasMoreElements() {
        return hasNext();
    }

    public Object nextElement() {
        return next();
    }

    /**
     * When a line in the time log file begins with this flag, it is considered
     * to be a replacement for the preceeding line in the file.
     */
    private static final String CONTINUATION_FLAG = "*";

    private static final String TAB = "\t";

    private static final String START = "Start Time: ";

    private static final String ELAPSED = "Elapsed Time: ";

    private static final String INTERRUPT = "Interruption Time: ";

    static TimeLogEntryVO parseOldStyleEntry(long entryID, String s) {
        int startPosition = 0;
        int endPosition = s.indexOf(TAB, startPosition);
        String path = null;
        if (startPosition < endPosition) {
            PropertyKey key = PropertyKey.valueOf(s.substring(startPosition,
                    endPosition));
            if (key == null)
                key = PropertyKey.fromKey(s.substring(startPosition,
                        endPosition));
            if (key != null)
                path = key.path();
        }
        if (path == null)
            throw new IllegalArgumentException("Invalid Key");

        startPosition = s.indexOf(START, endPosition) + START.length();
        endPosition = s.indexOf(TAB, startPosition);
        Date startTime = FormatUtil.parseDateTime(s.substring(startPosition,
                endPosition));
        if (startTime == null)
            throw new IllegalArgumentException("Invalid Start Time");

        startPosition = s.indexOf(ELAPSED, endPosition) + ELAPSED.length();
        endPosition = s.indexOf(TAB, startPosition);
        long minutesElapsed = 0;
        try {
            minutesElapsed = Long.valueOf(
                    s.substring(startPosition, endPosition)).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Elapsed Time");
        }

        startPosition = s.indexOf(INTERRUPT, endPosition) + INTERRUPT.length();
        endPosition = s.indexOf(TAB, startPosition);
        long minutesInterrupt = 0;
        try {
            String minInterrupt;
            if (endPosition == -1)
                minInterrupt = s.substring(startPosition);
            else
                minInterrupt = s.substring(startPosition, endPosition);
            minutesInterrupt = Long.parseLong(minInterrupt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Interrupt Time");
        }

        String comment = null;
        if (endPosition != -1)
            comment = s.substring(endPosition + 1).replace('\u0001', '\n');

        return new TimeLogEntryVO(entryID, path, startTime, minutesElapsed,
                minutesInterrupt, comment);
    }

    private static class DummyIDSource implements IDSource {
        private long id = 0;
        public long getNextID() {
            return ++id;
        }
    }

}
