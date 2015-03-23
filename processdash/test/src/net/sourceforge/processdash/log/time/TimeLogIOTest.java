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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import sun.awt.image.ByteArrayImageSource;

import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.IDSource;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.IteratorFilter;
import net.sourceforge.processdash.util.StringUtils;

import junit.framework.TestCase;

public class TimeLogIOTest extends AbstractTimeLogTest {

    public void testOldStyleTimeLog1() throws Exception {
        compareOldStyleTimeLog(TIMELOG1_TXT, TIMELOG1_CONTENTS);
    }

    public void testOldStyleTimeLog2() throws Exception {
        compareOldStyleTimeLog(TIMELOG2_TXT, TIMELOG2_CONTENTS);
    }

    private void compareOldStyleTimeLog(String filename, int[] expectedHashcodes)
            throws Exception {
        Iterator iter = new OldStyleTimeLogReader(openFile(filename));
        assertTimeLogHashcodes(expectedHashcodes, iter);
    }

    public void testOldStyleBoundaryCases() throws Exception {
        // nonexistent file
        EnumerIterator iter = new OldStyleTimeLogReader(new File("foo"),
                new DummyIDSource());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasMoreElements());
        try {
            iter.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }
        try {
            iter.nextElement();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }
        try {
            iter.remove();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException nsee) {
        }

        try {
            iter = new OldStyleTimeLogReader(new IOExceptionInputStream(
                    openFile(TIMELOG1_TXT), 200));
            fail("Expected IOException");
        } catch (IOException ioe) {
        }

        try {
            iter = new OldStyleTimeLogReader(new IOExceptionInputStream(
                    openFile(TIMELOG1_TXT), 10000));
            while (iter.hasNext())
                iter.next();
            fail("Expected IONoSuchElementException");
        } catch (IONoSuchElementException ionsee) {
            // expected behavior
        } catch (IOException ioe) {
            fail("Expected IONoSuchElementException");
        }
    }

    public void testNewStyleTimeLog1() throws Exception {
        compareNewStyleTimeLog(TIMELOG1_XML, TIMELOG1_CONTENTS);
    }

    private static final String[] TIMELOG2_ERROR_TEXT = {
            "WARNING: discarding garbled time log entry on line 13 of file null",
            "WARNING: discarding garbled time log entry on line 14 of file null",
            "WARNING: discarding garbled time log entry on line 15 of file null",
            "WARNING: discarding garbled time log entry on line 16 of file null",
            "WARNING: discarding garbled time log entry on line 20 of file null" };

    public void testNewStyleTimeLog2() throws Exception {
        PrintStream origOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out));
            compareNewStyleTimeLog(TIMELOG2_XML, TIMELOG2_CONTENTS);

            BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
            for (int i = 0; i < TIMELOG2_ERROR_TEXT.length; i++) {
                assertEquals(TIMELOG2_ERROR_TEXT[i], in.readLine());
            }
            assertNull(in.readLine());

        } finally {
            System.setOut(origOut);
        }
    }

    private void compareNewStyleTimeLog(String filename, int[] expectedHashcodes)
            throws Exception {
        Iterator iter = new TimeLogReader(openFile(filename));
        assertTimeLogHashcodes(expectedHashcodes, iter);
    }

    public void testWritingExternalEntries() throws Exception {
        final TimeLogReader entries = new TimeLogReader(openFile(TIMELOG3_XML), false);
        Iterator extEntries = new Iterator() {
            public Object next() {
                TimeLogEntry tle = (TimeLogEntry) entries.next();
                return new MockExternalTimeLogEntry(tle);
            }
            public void remove() {
            }
            public boolean hasNext() {
                return entries.hasNext();
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TimeLogWriter.write(new OutputStreamWriter(out), extEntries, false);
        ByteArrayInputStream written = new ByteArrayInputStream(out.toByteArray());
        compareStreams(openFile(TIMELOG3_XML), written);
    }

    public void testNewStyleIOExceptions() throws Exception {
        MockBaseTimeLog timeLog = new MockBaseTimeLog();
        NullWriter out = new NullWriter();

        try {
            // cause the timeLog to fail immediately, even before it can
            // read its first time log entry
            timeLog.numBytes = 50;
            timeLog.filter(null, null, null);
            fail("Expected IOException");
        } catch (IOException ioe) {
            assertEquals("IOExceptionInputStream faking IOException", ioe.getMessage());
        }

        // this should allow the initial "filter()" call to succeed, but the
        // resulting iterator should fail on a subsequent call to next().
        timeLog.numBytes = 700;
        Iterator i = timeLog.filter(null, null, null);
        try {
            while (i.hasNext())
                i.next();
            fail("Expected IONoSuchElementException");
        } catch (IONoSuchElementException ionsee) {
            assertEquals("IOExceptionInputStream faking IOException", ionsee.getIOException()
                    .getMessage());
        }

        // this should allow the initial "filter()" call to succeed, but the
        // resulting iterator should fail on a subsequent call to next().
        i = timeLog.filter(null, null, null);
        try {
            TimeLogWriter.write(out, i);
            fail("Expected IOException");
        } catch (IOException ioe) {
            // The TimeLogWriter should unwrap the IONoSuchElementException and
            // throw the underlying IOException
            assertEquals("IOExceptionInputStream faking IOException", ioe.getMessage());
        }

        // Configure the reader to behave, and the writer to throw an exception
        // before completion
        timeLog.numBytes = Integer.MAX_VALUE;
        out.numBytes = 500;
        try {
            TimeLogWriter.write(out, timeLog.filter(null, null, null));
            fail("Expected IOException");
        } catch (IOException ioe) {
            // The TimeLogWriter should unwrap the IONoSuchElementException and
            // throw the underlying IOException
            assertEquals("NullWriter faking IOException", ioe.getMessage());
        }

//        out.numBytes = Integer.MAX_VALUE;
//        List entries = Collections.list(timeLog.filter(null, null, null));
//        try {
//            System.setProperty(XmlPullParserFactory.PROPERTY_NAME, "foo");
//            XmlPullParserFactory.
//            try {
//                timeLog.filter(null, null, null);
//                fail("Expected IOException");
//            } catch (IOException ioe) {
//                assertSame(XmlPullParserException.class, ioe.getCause().getClass());
//            }
//
//        } finally {
//            System.setProperty(XmlPullParserFactory.PROPERTY_NAME, "");
//        }
    }

    public void testNewStyleBoundaryCases() throws Exception {
        // nonexistent file
        EnumerIterator iter = new TimeLogReader(new File("foo"));
        assertFalse(iter.hasNext());
        assertFalse(iter.hasMoreElements());
        try {
            iter.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }
        try {
            iter.nextElement();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException nsee) {
        }
        try {
            iter.remove();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException nsee) {
        }
    }

    public void testActualContentsOldStyle() throws Exception {
        Iterator iter = new OldStyleTimeLogReader(openFile(TIMELOG3_TXT));
        assertTimeLogContents(TIMELOG3_CONTENTS, iter);
    }

    public void testActualContentsNewStyle() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG3_XML));
        assertTimeLogContents(TIMELOG3_CONTENTS, iter);
    }

    private static class NullWriter extends Writer {
        public int numBytes = Integer.MAX_VALUE;
        public void close() throws IOException {
        }

        public void flush() throws IOException {
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            numBytes -= len;
            if (numBytes < 0)
                throw new IOException("NullWriter faking IOException");
        }
    }

    private static class DummyIDSource implements IDSource {
        private long id = 0;

        public long getNextID() {
            return ++id;
        }
    }

    public static void main(String[] args) {
        try {
            final OldStyleTimeLogReader in = new OldStyleTimeLogReader(
                    openFile("timelog3.txt"));
            String path = TimeLogIOTest.class.getName().replace('.', '/');
            String filename = "test/src/"
                    + StringUtils.findAndReplace(path, "TimeLogIOTest",
                            "timelog3.xml");
            FileWriter fileWriter = new FileWriter(filename);
            TimeLogWriter.write(fileWriter, in);
            fileWriter.close();
            // while (in.hasNext())
            // System.out.print(in.next().hashCode() + ", ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
