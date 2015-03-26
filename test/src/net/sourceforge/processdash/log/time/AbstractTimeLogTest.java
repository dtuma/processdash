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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FileUtils;
import junit.framework.TestCase;

public abstract class AbstractTimeLogTest extends TestCase {

    protected static final String TIMELOG1_TXT = "timelog1.txt";

    protected static final String TIMELOG1_XML = "timelog1.xml";

    protected static final int[] TIMELOG1_CONTENTS = new int[] { 1225400571,
            2069514918, 2074526288, 2078817088, 101785258, 155466329,
            1957308220, 428501950, 158859350, 522396266, 266905970, 1652144080,
            1644758857, 535096198, 476409759, 209763889, 475511305, 212795867,
            101080806, 1471923804, 101962688, 2109850198, 1454495281,
            401827852, 614196439, 702464752, 964248345, 1613384715, 1612398922,
            363280927, 1610852916, 1064308226, 961773795, 1611565321,
            1753751422, 1708492472, 1471204255, 179740148, 519722822,
            1150074722, 700882325, 552381924, 604401767, 1902450623, 783420499,
            834059658, 1921986750, 1764410631, 1764832726, 1768694021,
            1772803500, 1906942603, 477602408, 25229565, 793894178, 1055928971,
            664617907, 1211726274, 1837364682, 870169763, 1348197747,
            1510033581, 199953043, 1787399982, 1739510143, 2080016054,
            1286841808, 1255462971, 1474465588, 1019125924, 2081251185,
            186318732, 434560231, 1724957478, 296763437, 1965249631,
            1853269041, 882169579, 1871709995, 1389804236, 1398311941,
            1421164526, 1484451847, 1226598577, 552666800, 1706998071,
            637901691, 1697024105, 544274900, 1901695216, 672974548,
            1698654101, 1570048740, 674113081, 822128412, 1903844682,
            1695642048, 641095246, 1235227534, 541582393, 2010282670,
            2109468472, 831446858, 738550659, 1896882275, 898034188,
            1447660647, 1074729205, 1706651549, 169783300, 324763980,
            1245257738, 1449720861, 553681268, 1631232232, 1166764839,
            49187656, 1013697526, 88176573, 1211190380, 850323226, 603401943,
            1945449606, 729830683, 885950528, 1944988531, 1208466343,
            1637391585, 1978472579, 697900486, 743888413, 317618981, 793003995,
            922514317, 318194870, 1582546672, 1743168630, 1286871078 };

    protected static final String TIMELOG2_TXT = "timelog2.txt";

    protected static final String TIMELOG2_XML = "timelog2.xml";

    protected static final int[] TIMELOG2_CONTENTS = new int[] { 736406057,
            737480994, 714674773, 102754108, 124285629, 420453418, 132170719,
            50564272, 1091168677, 1257303567, 23256572, 1441022929, 2022645217,
            1526836306, 2024221947, 1995237927, 412283105, 1989406301,
            1886989733, 1988906115, 414789501, 1990546553, 413656999,
            1607758862, 1536224277, 1373547855, 1602024023, 1605174552,
            230518777, 1200258351, 1436401520, 1344646135, 1344647126,
            1071859770, 1064712971, 1020735868, 1022218973, 1007076854,
            1059533401, 1059533350, 855760278, 1303999473, 1549010668,
            1225068271, 2109982117, 1147612440, 1559818776, 1144607670,
            1269460279, 1260661756, 950451611, 948285108, 943571045,
            1933087748, 1604612089, 745705983, 1582933871, 1658740628,
            1655422781, 1635967594, 2069795151, 2078921208, 1754998518,
            2049719882, 479355227, 269972936, 1671337447, 1088543692,
            1088543149, 940452600, 1871047577, 1686549924, 399965939,
            1681987950, 1680487607, 1859172138, 1763124514, 1597904433,
            1597899056, 1261339786, 1374053854, 1222076560, 1222082441,
            1206262205, 1176081460, 1177858511, 1187394078, 1161388041,
            1588314665, 1768736231, 1119102540, 759939393, 1886776383,
            1010400583, 1548832960, 2052684490, 1448205630, 685493748,
            755759309, 762595162, 752222731, 797362396, 797361353, 284349448,
            278061521, 767910154, 1973710065, 2057978838, 2050496279,
            2053744152, 2074379245, 2076998458, 2068426075, 1728856896,
            1735578697, 1034570484, 1026207277, 968635622, 1857644621,
            1860859618, 1841682791, 1779787984, 236227140, 166371673,
            145385632, 149849059, 212956082, 261404429, 263098436, 255799583,
            786173692, 1785794422, 997522064, 1965198394, 323088225,
            1382994753, 1960160517, 853437303, 247461460, 1964127720,
            1857983633, 1826887110, 1218462998, 1393553796, 1479946901,
            2036209859, 1919899011, 822330366, 1559951913, 820805028,
            1591594043, 1284921596 };

    protected void assertTimeLogHashcodes(int[] expectedHashcodes, Iterator iter) {
        for (int i = 0; i < expectedHashcodes.length; i++) {
            assertTrue(iter.hasNext());
            assertEquals(expectedHashcodes[i], iter.next().hashCode());
        }
        assertFalse(iter.hasNext());
    }

    protected static final String TIMELOG3_TXT = "timelog3.txt";

    protected static final String TIMELOG3_XML = "timelog3.xml";

    protected static final Object[][] TIMELOG3_CONTENTS = {
            { "/Project/Dy4-181 BSP Build/Test Task/Test",
                    new Date(1094229310000L), new Integer(900), new Integer(0),
                    "comment number one" },
            { "/Project/Gen2 MPA Build for VxWorks/Test Task/Test",
                    new Date(1094229457000L), new Integer(720),
                    new Integer(20), null },
            { "/Project/Requirements/Serial IO/RRW/Reqts",
                    new Date(1094238593000L), new Integer(44), new Integer(0),
                    "comment number two" },
            { "/Project/Requirements/FDDI bus/Inspect/Reqts Inspect",
                    new Date(1094244165000L), new Integer(14), new Integer(0),
                    "comment number three" },
            { "/Project/Requirements/Utility bus/RRW/Reqts",
                    new Date(1094757707000L), new Integer(151), new Integer(0),
                    "comment\nnumber four" } };

    protected static final String TIMELOG3_MOD_XML = "timediff3.xml";

    protected static final Object[][] TIMELOG3_MOD_CONTENTS = {
            { "/Non Project/Dy4-181 BSP Build/Test Task/Test",
                    new Date(1094229310000L), new Integer(800), new Integer(0),
                    "comment number one", new Integer(1) },
            { "/Project/Requirements/Serial IO/RRW/Reqts",
                    new Date(1094238594000L), new Integer(44), new Integer(1),
                    "comment number two, modified", new Integer(3) },
            { "/Project/Requirements/FDDI bus/Inspect/Reqts Inspect",
                    new Date(1094244165000L), new Integer(14), new Integer(0),
                    "comment number three", new Integer(4) },
            { "/Project/Requirements/Utility bus/RRW/Reqts",
                    new Date(1094757707000L), new Integer(151), new Integer(0),
                    "comment\nnumber four", new Integer(5) },
            { "/Project/Requirements/FDDI bus/Inspect/Reqts Inspect",
                    new Date(1094244165000L), new Integer(14), new Integer(0),
                    "comment number three", new Integer(6) } };

    protected void assertTimeLogContents(Object[][] expectedContents, TimeLog timeLog) throws IOException {
        assertTimeLogContents(expectedContents, timeLog.filter(null, null, null));
    }
    protected void assertTimeLogContents(Object[][] expectedContents,
            Iterator iter) {
        assertTimeLogContents(expectedContents, iter, null);
    }
    protected void assertTimeLogContents(Object[][] expectedContents,
            Iterator iter, TimeLogEntry extraEntry) {
        for (int i = 0; i < expectedContents.length; i++) {
            assertTrue(iter.hasNext());
            assertTimeLogEntryContents(expectedContents[i],
                    (TimeLogEntry) iter.next());
        }
        if (extraEntry != null) {
            assertTrue(iter.hasNext());
            assertTimeLogEntryContents(extraEntry, (TimeLogEntry) iter.next());
        }
        assertFalse(iter.hasNext());
    }

    protected void assertTimeLogEntryContents(Object[] data, TimeLogEntry tle) {
        assertEquals(data[0], tle.getPath());
        assertEquals(data[1], tle.getStartTime());
        assertEquals(((Integer) data[2]).intValue(), tle.getElapsedTime());
        assertEquals(((Integer) data[3]).intValue(), tle.getInterruptTime());
        assertEquals(data[4], tle.getComment());
        assertEquals(ChangeFlagged.NO_CHANGE, ((ChangeFlagged) tle)
                .getChangeFlag());
    }

    protected void assertTimeLogEntryContents(TimeLogEntry tle1, TimeLogEntry tle2) {
        assertEquals(tle1.getID(), tle2.getID());
        assertEquals(tle1.getPath(), tle2.getPath());
        assertEquals(tle1.getStartTime(), tle2.getStartTime());
        assertEquals(tle1.getElapsedTime(), tle2.getElapsedTime());
        assertEquals(tle1.getInterruptTime(), tle2.getInterruptTime());
        assertEquals(tle1.getComment(), tle2.getComment());
    }


    protected static void copyFile(File dir, String src, String dest)
            throws IOException {
        FileUtils.copyFile(openFile(src), new File(dir, dest));
    }

    protected static boolean compareFile(InputStream expected, File actual) throws IOException {
        return compareStreams(expected, new FileInputStream(actual));
    }

    protected static boolean compareStreams(InputStream a, InputStream b) throws IOException {
        try {
            BufferedInputStream aa = new BufferedInputStream(a);
            BufferedInputStream bb = new BufferedInputStream(b);
            while (true) {
                int ca = aa.read();
                int cb = bb.read();
                if (ca == -1 && cb == -1)
                    return true;
                else if (ca != cb)
                    return false;
            }
        } finally {
            a.close(); b.close();
        }
    }

    protected File createTempDir() throws IOException {
        File tempDir = File.createTempFile("test", ".tmp");
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }

    protected void assertFilteredContents(Iterator iter, int[] expectedIds, int[] hashCodes) {
        for (int i = 0; i < expectedIds.length; i++) {
            assertTrue(iter.hasNext());
            int pos = expectedIds[i] - 1;
            int expectedHashcode = hashCodes[pos];
            assertEquals(expectedHashcode, iter.next().hashCode());
        }
        assertFalse(iter.hasNext());
    }
    protected static InputStream openFile(String filename) throws IOException {
        return AbstractTimeLogTest.class.getResourceAsStream(filename);
    }
    protected static void assertIsEmptyTimeLogFile(File f) throws IOException {
        assertFalse(new TimeLogReader(f).hasMoreElements());
    }

    protected static final class MockBaseTimeLog implements TimeLog {
        public String filename = TIMELOG3_XML;
        public int numBytes = Integer.MAX_VALUE;

        public MockBaseTimeLog() {
            this(TIMELOG3_XML);
        }

        public MockBaseTimeLog(String filename) {
            this.filename = filename;
        }

        public EnumerIterator filter(String path, Date from, Date to)
                throws IOException {
            EnumerIterator result = new TimeLogReader(new IOExceptionInputStream(
                    openFile(filename), numBytes));
            if (path != null || from != null || to != null)
                result = new TimeLogIteratorFilter(result, path, from, to);
            return result;
        }
    }

    protected static final class EmptyBaseTimeLog implements TimeLog, EnumerIterator {
        public EnumerIterator filter(String path, Date from, Date to) throws IOException {
            return this;
        }

        public void remove() {}
        public boolean hasNext() { return false; }
        public boolean hasMoreElements() { return false; }
        public Object next() { return null; }
        public Object nextElement() { return null; }
    }
}
