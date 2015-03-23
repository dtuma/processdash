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
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.hier.PathRenamingInstruction;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.FileUtils;

public class TimeLogRenamingTest extends AbstractTimeLogTest {

    private static final String RENAME_XML = "timeRename3.xml";

    private File tempDir;

    // private List eventsReceived;

    protected void setUp() throws Exception {
        super.setUp();
        // eventsReceived = new LinkedList();
        tempDir = null;
    }

    protected void tearDown() throws Exception {
        if (tempDir != null)
            FileUtils.deleteDirectory(tempDir);

        super.tearDown();
    }

    // private static final String RENAME1 = "/Project\n/Foo/Bar/Baz";

    // /*
    // * Things to test:
    // * - create renaming operations that swap two paths.
    // * - test event delivery
    // */

    public void testPathRenamer() {
        List renames = createDefaultRenameList();

        // test regular renaming operations
        assertRename("/Foo/Bar/Baz", "/Project/Baz", renames);
        assertRename("/Projects/Baz", "/Projects/Baz", renames);
        assertRename("/Foo/Bar/Dy4-181 BSP Builds/Test Task/Test",
                "/Project/Dy4-181 BSP Build/Test Task/Test", renames);
        assertRename("/Project X/Not Changed", "/Project X/Not Changed",
                renames);

        // test some boundary cases
        assertRename(null, null, renames);
        try {
            PathRenamer.toInstruction(new TimeLogEntryVO(1, "/foo", null, 0, 0,
                    null));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            assertEquals(
                    "Time log entry does not describe a batch modification.",
                    iae.getMessage());
        }
    }

    private void assertRename(String expected, String path, List renames) {
        assertEquals(expected, PathRenamingInstruction.renamePath(renames, path));
    }

    public void testIsRenamingOperation() {
        PathRenamingInstruction r = new PathRenamingInstruction("/Foo", "Bar");
        TimeLogEntry tle = PathRenamer.toTimeLogEntry(r);
        MutableTimeLogEntry m = new MutableTimeLogEntryVO(tle);
        assertTrue(PathRenamer.isRenamingOperation(m));
        m.setPath("null");
        assertFalse(PathRenamer.isRenamingOperation(m));
        m.setPath("/Foo/Bar");
        assertFalse(PathRenamer.isRenamingOperation(m));
        m = new MutableTimeLogEntryVO(tle);
        m.setStartTime(new Date());
        assertFalse(PathRenamer.isRenamingOperation(m));
        m.setStartTime(null);
        m.setInterruptTime(1);
        assertFalse(PathRenamer.isRenamingOperation(m));
        m.setInterruptTime(0);
        m.setElapsedTime(1);
        assertFalse(PathRenamer.isRenamingOperation(m));
        m.setElapsedTime(0);
        m.setComment("comment");
        assertFalse(PathRenamer.isRenamingOperation(m));
        m.setComment(null);
        for (int i = ChangeFlagged.MODIFIED; i <= ChangeFlagged.DELETED; i++) {
            m.setChangeFlag(i);
            assertFalse(PathRenamer.isRenamingOperation(m));
        }
        m.setChangeFlag(ChangeFlagged.BATCH_MODIFICATION);
        assertTrue(PathRenamer.isRenamingOperation(m));
        assertFalse(PathRenamer.isRenamingOperation(new MockExternalTimeLogEntry(m)));
    }

    public void testSimpleRenaming() throws Exception {
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null,
                null);
        TimeLogReader renames = new TimeLogReader(openFile(RENAME_XML));
        timeLogMods.addModification((ChangeFlaggedTimeLogEntry) renames.next());
        timeLogMods.addModifications(renames);

        Object[][] expectedContents = applyRenames(TIMELOG3_CONTENTS,
                createDefaultRenameList());
        assertTimeLogContents(expectedContents, timeLogMods);
    }

    public void testRenamingWithOtherMods() throws Exception {
        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, null,
                null);

        // apply several normal modifications, followed by renaming operations.
        timeLogMods.addModifications(new TimeLogReader(
                openFile(TIMELOG3_MOD_XML)));
        timeLogMods.addModifications(new TimeLogReader(openFile(RENAME_XML)));

        // check the expected results.
        Object[][] expectedContents = applyRenames(TIMELOG3_MOD_CONTENTS,
                createDefaultRenameList());
        assertTimeLogContents(expectedContents, timeLogMods);

        // now make another modification that changes one of the renamed paths
        // back to its original (unrenamed) value. Make certain the modification
        // succeeds in spite of the earlier rename operation.
        String path = "/Project/Requirements/Serial IO/RRW/Reqts";
        TimeLogEntryVO mod = new TimeLogEntryVO(3, path, null, 0, 0, null,
                ChangeFlagged.MODIFIED);
        timeLogMods.addModification(mod);
        expectedContents[1][0] = path;
        assertTimeLogContents(expectedContents, timeLogMods);
    }

    public void testIO() throws Exception {
        // create a mod list and read the rename operations from a file.
        tempDir = createTempDir();
        copyFile(tempDir, RENAME_XML, RENAME_XML);
        File file = new File(tempDir, RENAME_XML);

        MockBaseTimeLog base = new MockBaseTimeLog();
        TimeLogModifications timeLogMods = new TimeLogModifications(base, file,
                null);

        // now programatically reconstruct our expectations for the contents
        // (using no file IO). Assert that they are the same.
        Object[][] expectedContents = applyRenames(TIMELOG3_CONTENTS,
                createDefaultRenameList());
        assertTimeLogContents(expectedContents, timeLogMods);

        // save the modifications out to the file and make certain they are
        // written exactly as we expect.
        file.delete();
        assertFalse(file.exists());
        timeLogMods.save();
        assertTrue(file.exists());
        assertTrue(compareFile(openFile(RENAME_XML), file));
    }

    public void testOperationWithWorkingTimeLog() throws Exception {
        // create a working time log with starting contents of timelog3.xml
        tempDir = createTempDir();
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);
        WorkingTimeLog timeLog = new WorkingTimeLog(tempDir);
        // ask for a deferred modifications instance, and add the standard
        // timelog3 mods to it.
        CommittableModifiableTimeLog deferred = timeLog.getDeferredTimeLogModifications();
        deferred.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        // make certain the contents match our expectations so far.
        assertTimeLogContents(TIMELOG3_CONTENTS, timeLog);
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, deferred);

        // now add the renaming operations to the "real time" mod list.
        timeLog.addModifications(new TimeLogReader(openFile(RENAME_XML)));

        // at this point, our "real time" list should look like a renamed
        // version of the standard timelog 3 contents.
        List renames = createDefaultRenameList();
        Object[][] expectedContents = applyRenames(TIMELOG3_CONTENTS, renames);
        assertTimeLogContents(expectedContents, timeLog);
        // meanwhile, our "deferred" list should look like a renamed version of
        // the modified timelog 3 contents.
        expectedContents = applyRenames(TIMELOG3_MOD_CONTENTS, renames);
        assertTimeLogContents(expectedContents, deferred);

        // now commit the deferred time log. The real time list should look like
        // both the modifications and the renames have taken effect.
        deferred.commitData();
        assertTimeLogContents(expectedContents, timeLog);
        // changes should have gotten saved into the modifications file
        assertTrue(new File(tempDir, WorkingTimeLog.TIME_LOG_FILENAME).exists());
        File modFile = new File(tempDir, WorkingTimeLog.TIME_LOG_MOD_FILENAME);
        assertTrue(modFile.exists());
        long modSizeBeforeCleanup = modFile.length();

        // now get the working time log to clean up and reread itself (by
        // creating a new one)
        timeLog = new WorkingTimeLog(tempDir);
        assertTrue(new File(tempDir, WorkingTimeLog.TIME_LOG_FILENAME).exists());
        assertTrue(modFile.exists());
        assertIsEmptyTimeLogFile(modFile);
        assertTrue(modFile.length() < modSizeBeforeCleanup);

        // after the cleanup, contents should still match our expectations.
        assertTimeLogContents(expectedContents, timeLog);
    }

    protected Object[][] applyRenames(Object[][] originalContent, List renames) {
        List instructions = createInstructionList(renames);
        Object[][] result = new Object[originalContent.length][];
        for (int i = 0; i < result.length; i++) {
            Object[] row = (Object[]) originalContent[i].clone();
            row[0] = PathRenamingInstruction.renamePath(instructions, (String) row[0]);
            result[i] = row;
        }
        return result;
    }

    private List createDefaultRenameList() {
        List renames = new LinkedList();
        renames.add("/Project/Dy4-181 BSP Build\n/Project/Dy4-181 BSP Builds");
        renames.add("/Project\n/Foo/Bar");
        renames.add("/Blah\n/Qwerty");
        renames = createInstructionList(renames);
        return renames;
    }

    private List createInstructionList(List renames) {
        List instructions = new LinkedList();
        for (Iterator iter = renames.iterator(); iter.hasNext();) {
            Object o = (Object) iter.next();
            if (o instanceof PathRenamingInstruction)
                instructions.add(o);
            else if (o instanceof TimeLogEntry)
                instructions.add(PathRenamer.toInstruction((TimeLogEntry) o));
            else if (o instanceof String)
                instructions.add(PathRenamer.toInstruction((String) o));
        }
        return instructions;
    }
}
