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

import java.util.Date;
import java.util.Iterator;

public class IteratorFilterTest extends AbstractTimeLogTest {

    public void testNullIteratorFilter() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG1_XML));
        iter = new TimeLogIteratorFilter(iter, null, null, null);
        assertTimeLogHashcodes(TIMELOG1_CONTENTS, iter);
    }

    public void testIteratorFilter1() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG1_XML));
        iter = new TimeLogIteratorFilter(iter, "/Non Project/BCS-M", null, null);
        int[] expectedIds = new int[] { 3, 7, 12, 13 };
        assertFilteredContents(iter, expectedIds, TIMELOG1_CONTENTS);
    }

    public void testIteratorFilter2() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG1_XML));
        iter = new TimeLogIteratorFilter(iter, "/Project/EFV", null, null);
        assertFalse(iter.hasNext());
    }

    public void testIteratorFilter3() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG1_XML));
        iter = new TimeLogIteratorFilter(iter, "/Project/EFV Gap Analysis",
                new Date(1093377260000L), new Date(1093907142000L));
        int[] expectedIds = new int[] { 19, 20, 21, 22, 23, 24 };
        assertFilteredContents(iter, expectedIds, TIMELOG1_CONTENTS);
    }

    public void testIteratorFilter4() throws Exception {
        Iterator iter = new TimeLogReader(openFile(TIMELOG1_XML));
        iter = new TimeLogIteratorFilter(iter, null,
                new Date(1093377260000L), new Date(1093907142000L));
        int[] expectedIds = new int[] { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        assertFilteredContents(iter, expectedIds, TIMELOG1_CONTENTS);
    }

    public void testEmptyStartDate() throws Exception {
        TimeLogEntry tle = new TimeLogEntryVO(42, "/path", null, 0, 0, null);
        assertTrue(TimeLogIteratorFilter.matches(tle, null, new Date(), null, true));
        assertTrue(TimeLogIteratorFilter.matches(tle, null, null, new Date(), true));
    }

}
