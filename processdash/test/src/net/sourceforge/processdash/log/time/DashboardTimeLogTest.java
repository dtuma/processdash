// Copyright (C) 2005-2009 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.FileUtils;

public class DashboardTimeLogTest extends DefaultTimeLoggingModelTest {

    private File tempDir;

    protected void createModels() throws Exception {
        tempDir = createTempDir();
        DashboardTimeLog dashboardTimeLog = new DashboardTimeLog(tempDir, data,
                hier);

        String dataName = DataRepository.createDataName(PATH4, "Time");
        data.putValue(dataName, new StringData("\"foo"));

        approver = dashboardTimeLog;
        timeLog = dashboardTimeLog;
        timeLoggingModel = (DefaultTimeLoggingModel) dashboardTimeLog
                .getTimeLoggingModel();
    }


    public void testApproverFunctionality() {
        assertFalse(DashboardTimeLog.timeLoggingAllowed(null, null, null, null));
        assertFalse(approver.isTimeLoggingAllowed(DataRepository.chopPath(PATH1)));

        String dataName = DataRepository.createDataName(PATH4, "Time");
        data.putValue(dataName, new MockNumberFunction());
        assertFalse(approver.isTimeLoggingAllowed(PATH4));
        data.putValue(dataName, null);
        assertTrue(approver.isTimeLoggingAllowed(PATH4));
    }

    public void testPassthrough() throws Exception {
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);

        assertTimeLogContents(TIMELOG3_CONTENTS, timeLog);

        // add entries manually (like they would be added during dashboard use)
        Iterator mods = new TimeLogReader(openFile(TIMELOG3_MOD_XML));
        timeLog.addModifications(mods);
        // verify correct contents
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLog);

        // make changes to the deferred time log and ensure that they don't
        // take effect
        ModifiableTimeLog deferred = ((DashboardTimeLog) timeLog)
                .getDeferredTimeLogModifications();
        deferred.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLog);
    }

    public void testSaveableDataSourceSupport() throws Exception {
        PrintStream origErr = System.err;
        try {
            ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errorCapture));

            doTestSaveableDataSupportImpl();

            System.err.flush();
            String errorMessage = errorCapture.toString();
            assertTrue(errorMessage.startsWith(
                    "Unable to save time log modifications to file"));
        } finally {
            System.setErr(origErr);
        }
    }

    private void doTestSaveableDataSupportImpl() throws IOException {
        copyFile(tempDir, TIMELOG3_XML, WorkingTimeLog.TIME_LOG_FILENAME);

        // at the start, our time log is not "dirty".
        DashboardTimeLog dashboardTimeLog = (DashboardTimeLog) timeLog;
        assertFalse(dashboardTimeLog.isDirty());

        // create a bogus directory in the way of the time log mods file, so
        // that attempts to save it will fail.
        File modFile = new File(tempDir, WorkingTimeLog.TIME_LOG_MOD_FILENAME);
        modFile.delete();
        modFile.mkdir();

        // add some entries.  The contents should appear correct, but the
        // time log itself should be dirty.
        timeLog.addModifications(new TimeLogReader(openFile(TIMELOG3_MOD_XML)));
        assertTimeLogContents(TIMELOG3_MOD_CONTENTS, timeLog);
        assertTrue(dashboardTimeLog.isDirty());

        // attempt a manual save, and ensure that we're still dirty.
        dashboardTimeLog.saveData();
        assertTrue(dashboardTimeLog.isDirty());

        // now remove the source of the save problem, save, and check not dirty.
        modFile.delete();
        dashboardTimeLog.saveData();
        assertFalse(dashboardTimeLog.isDirty());
    }

    public void testSetDefault() {
        DashboardTimeLog.setDefault(timeLog);
        assertSame(timeLog, DashboardTimeLog.getDefault());
    }

    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDir);
        super.tearDown();
    }

    private static class MockNumberFunction extends DoubleData implements NumberFunction {

        public void recalc() {
        }

        public String name() {
            return null;
        }

    }
}
