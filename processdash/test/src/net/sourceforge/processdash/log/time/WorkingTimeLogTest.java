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

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import net.sourceforge.processdash.util.FileUtils;

public class WorkingTimeLogTest extends AbstractTimeLogTest implements TimeLogListener {

    File tempDir, baseFile, modFile, oldFile;

    private boolean eventReceived = false;

    protected void setUp() throws Exception {
        super.setUp();
        tempDir = createTempDir();
        baseFile = new File(tempDir, WorkingTimeLog.TIME_LOG_FILENAME);
        modFile = new File(tempDir, WorkingTimeLog.TIME_LOG_MOD_FILENAME);
        oldFile = new File(tempDir, WorkingTimeLog.OLD_TIME_LOG_FILENAME);
    }

    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDir);
        super.tearDown();
    }

    public void testOldStyleConversion() throws Exception {
        copyFile(tempDir, TIMELOG3_TXT, WorkingTimeLog.OLD_TIME_LOG_FILENAME);
        WorkingTimeLog tl = new WorkingTimeLog(tempDir);

        assertTrue(baseFile.isFile());
        assertTrue(modFile.isFile());
        assertIsEmptyTimeLogFile(modFile);
        assertFalse(oldFile.isFile());
        assertTrue(compareFile(openFile(TIMELOG3_XML), baseFile));

        assertTimeLogContents(TIMELOG3_CONTENTS, tl);
        assertEquals(TIMELOG3_CONTENTS.length + 1, tl.getNextID());
    }

    public void testNewStyleCleanup() throws Exception {
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);
        copyFile(tempDir, TIMELOG3_MOD_XML,
                WorkingTimeLog.TIME_LOG_MOD_FILENAME);
        WorkingTimeLog tl = new WorkingTimeLog(tempDir);

        assertTrue(baseFile.isFile());
        assertTrue(modFile.isFile());
        assertIsEmptyTimeLogFile(modFile);
        assertFalse(oldFile.isFile());

        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, tl);
        assertEquals(TIMELOG3_CONTENTS.length + 2, tl.getNextID());
    }

    public void testNewStylePlain() throws Exception {
        copyFile(tempDir, TIMELOG1_XML, WorkingTimeLog.TIME_LOG_FILENAME);
        WorkingTimeLog tl = new WorkingTimeLog(tempDir);

        assertTrue(baseFile.isFile());
        assertTrue(modFile.isFile());
        assertIsEmptyTimeLogFile(modFile);
        assertFalse(oldFile.isFile());

        Iterator entries = tl.filter(null, null, null);
        assertTimeLogHashcodes(TIMELOG1_CONTENTS, entries);
        assertEquals(TIMELOG1_CONTENTS.length + 1, tl.getNextID());

        // Make certain that filtering is working correctly
        entries = tl.filter(null, new Date(1100217158000L), new Date(1100217379000L));
        int[] expectedIds = new int[] { 124, 125, 126 };
        assertFilteredContents(entries, expectedIds, TIMELOG1_CONTENTS);
    }

    public void testNewStyleWithOld() throws Exception {
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);
        copyFile(tempDir, TIMELOG3_MOD_XML,
                WorkingTimeLog.TIME_LOG_MOD_FILENAME);
        copyFile(tempDir, TIMELOG3_TXT, WorkingTimeLog.OLD_TIME_LOG_FILENAME);
        WorkingTimeLog tl = new WorkingTimeLog(tempDir);

        assertTrue(baseFile.isFile());
        assertTrue(modFile.isFile());
        assertIsEmptyTimeLogFile(modFile);
        assertFalse(oldFile.isFile());

        Iterator entries = tl.filter(null, null, null);

        for (int i = 0; i < TIMELOG3_MOD_CONTENTS.length; i++) {
            assertTrue(entries.hasNext());
            TimeLogEntry tle = (TimeLogEntry) entries.next();
            assertTimeLogEntryContents(TIMELOG3_MOD_CONTENTS[i], tle);
            assertEquals(TIMELOG3_MOD_CONTENTS[i][5], new Integer((int) tle
                    .getID()));
        }
        for (int i = 0; i < TIMELOG3_CONTENTS.length; i++) {
            assertTrue(entries.hasNext());
            TimeLogEntry tle = (TimeLogEntry) entries.next();
            assertEquals(i + 7, tle.getID());
            assertTimeLogEntryContents(TIMELOG3_CONTENTS[i], tle);
        }
        assertFalse(entries.hasNext());
    }

    public void testModificationPassthrough() throws Exception {
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);
        WorkingTimeLog tl = new WorkingTimeLog(tempDir);
        tl.updateCurrentId(null);  // no-op
        tl.addTimeLogListener(this);

        assertTrue(baseFile.isFile());
        assertTrue(modFile.isFile());
        assertIsEmptyTimeLogFile(modFile);
        assertFalse(oldFile.isFile());

        assertTimeLogContents(TIMELOG3_CONTENTS, tl);

        // add entries manually (like they would be added during dashboard use)
        Iterator mods = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        assertFalse(eventReceived);
        tl.addModification((ChangeFlaggedTimeLogEntry) mods.next());
        assertTrue(eventReceived);
        tl.removeTimeLogListener(this);
        eventReceived = false;
        tl.addModifications(mods);
        assertFalse(eventReceived);
        // verify correct contents
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, tl);
        // verify that modifications were saved properly
        assertTrue(compareFile(openFile(TIMELOG3_XML), baseFile));
        assertTrue(compareFile(openFile(TIMELOG3_MOD_XML), modFile));
        assertFalse(oldFile.isFile());

    }

    public void timeLogChanged(TimeLogEvent e) {
        eventReceived = true;
    }

}
