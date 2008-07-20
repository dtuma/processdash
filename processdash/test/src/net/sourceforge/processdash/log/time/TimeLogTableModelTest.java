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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.log.IDSource;
import net.sourceforge.processdash.log.time.AbstractTimeLogTest.EmptyBaseTimeLog;
import net.sourceforge.processdash.util.FormatUtil;
import junit.framework.TestCase;


public class TimeLogTableModelTest extends AbstractTimeLogTest implements TimeLogListener, TableModelListener {

    private static final int ROW_INSERTED = TableModelEvent.INSERT;

    private static final int ROW_UPDATED = TableModelEvent.UPDATE;

    private static final int ROW_DELETED = TableModelEvent.DELETE;

    private static final Date FILTER_DATE = new Date(1100128007000L);

    private static final String FILTER_PATH = "/Project/EFV Gap Analysis/Gap Analysis/Complete Gap Analysis Report";

    protected MockBaseTimeLog baseTimeLog;

    protected CommittableModifiableTimeLog realTimeMods;

    protected CommittableModifiableTimeLog timeLog;

    protected TimeLogTableModel tableModel;

    protected List timeLogEventsReceived, tableModelEventsReceived;

    protected void setUp() throws Exception {
        super.setUp();
        createModels();
        tableModel = new TimeLogTableModel();
        tableModel.setTimeLog(timeLog);
        timeLogEventsReceived = new LinkedList();
        tableModelEventsReceived = new LinkedList();
        tableModel.addTableModelListener(this);
    }

    protected void createModels() {
        baseTimeLog = new MockBaseTimeLog(TIMELOG1_XML);
        realTimeMods = new TimeLogModifications(baseTimeLog);
        timeLog = new TimeLogModifications(realTimeMods, new DummyIDSource(138));
    }

    public void testNullTimeLog() {
        tableModel.setTimeLog(timeLog);  // no-op
        tableModel.setTimeLog(null);
        assertEquals(0, tableModel.getRowCount());
        assertEquals(5, tableModel.getColumnCount());
        assertNull(tableModel.getValueAt(12, 1));

        tableModel.addRow(PATH1);
        assertEquals(0, tableModel.getRowCount());
    }

    public void testColumnNames() {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            assertNotNull(tableModel.getColumnName(i));
        }
    }

    private static final String PATH1 = "/Project/EFV Gap Analysis/Gap Analysis/Complete Gap Analysis Report/Produce Final Report/Reqts";
    private static final String ZERO = "0:00";
    private static final DateFormat DFMT = DateFormat.getDateTimeInstance();

    private static final String[][] TABLE_CONTENTS_1 = {
        { PATH1, DFMT.format(FILTER_DATE), "0:12", ZERO, null},
        { PATH1, DFMT.format(new Date(1100190252000L)), "2:01", ZERO, null},
        { PATH1, DFMT.format(new Date(1100289389000L)), "0:57", "0:05", null},
        { PATH1, DFMT.format(new Date(1100296281000L)), "0:57", ZERO, "long comment with\nembedded newlines\nfor entry near end" },
        { PATH1, DFMT.format(new Date(1100530587000L)), "0:31", ZERO, null},
    };
    private static final long[] TABLE_IDS_1 = {
        82, 83, 136, 137, 138
    };

    private static final String[][] SUMMARIZED_CONTENT = {
        { PATH1, DFMT.format(FILTER_DATE), "4:28", "0:15", "extra comment\nlong comment with\nembedded newlines\nfor entry near end" }
    };

    private static final long[] SUMMARIZED_IDS = { TABLE_IDS_1[0] };

    public void testFiltering() {
        tableModel.setFilter(FILTER_PATH, FILTER_DATE, null);
        assertTableContents(TABLE_IDS_1, TABLE_CONTENTS_1, tableModel);
    }

    public void testTableModelModifications() throws Exception {
        doTestTableModelModifications();
    }

    public Date doTestTableModelModifications() throws Exception {
        setupFilter();

        String[][] expectedContents = deepCopyArray(TABLE_CONTENTS_1);

        // setValue(tableModel, 0, 0, null, false, null);
        setValue(tableModel, 99, 0, PATH1, false, expectedContents);
        setValue(tableModel, 0, 99, PATH1, false, expectedContents);
        setValue(tableModel, 0, 0, PATH1, false, expectedContents);
        setValue(tableModel, 0, 0, FILTER_PATH+"/new Path", true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[0], FILTER_PATH+"/new Path", null, 0, 0, null);


        setValue(tableModel, 4, 1, "invalid date", false, null);
        Date now = DFMT.parse(DFMT.format(new Date()));
        setValue(tableModel, 4, 1, DFMT.format(now), true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[4], null, now, 0, 0, null);

        setValue(tableModel, 2, 2, "invalid time", false, null);
        setValue(tableModel, 2, 2, "00:57", false, expectedContents);
        setValue(tableModel, 2, 2, "2:00", true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[2], null, null, 63, 0, null);

        setValue(tableModel, 3, 3, "invalid time", false, null);
        setValue(tableModel, 3, 3, "00:00", false, expectedContents);
        setValue(tableModel, 3, 3, "0:10", true, expectedContents);
        expectedContents[3][2] = "0:47";
        assertModification(timeLog, TABLE_IDS_1[3], null, null, -10, 10, null);

        setValue(tableModel, 1, 4, "new comment", true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[1], null, null, 0, 0, "new comment");
        setValue(tableModel, 3, 4, null, true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[3], null, null, -10, 10, "");
        setValue(tableModel, 3, 4, "foo", true, expectedContents);
        assertModification(timeLog, TABLE_IDS_1[3], null, null, -10, 10, "foo");
        tableModel.setValueAt("", 3, 4);
        assertEquals(null, tableModel.getValueAt(3, 4));
        expectedContents[3][4] = null;
        assertModification(timeLog, TABLE_IDS_1[3], null, null, -10, 10, "");

        assertTableContents(TABLE_IDS_1, expectedContents, tableModel);

        tableModel.setFilter(null, null, null);
        tableModel.setFilter(FILTER_PATH, FILTER_DATE, null);
        assertTableContents(TABLE_IDS_1, expectedContents, tableModel);
        return now;
    }

    private void setupFilter() {
        tableModel.setFilter(FILTER_PATH, FILTER_DATE, null);
        assertTableModelEvent(ROW_UPDATED, 0, Integer.MAX_VALUE, -1);
        assertTableContents(TABLE_IDS_1, TABLE_CONTENTS_1, tableModel);
    }

    public void testTimeLogEventsFromTableModelModifications() throws Exception {
        timeLog.addTimeLogListener(this);
        Date now = doTestTableModelModifications();

        long[] IDs = TABLE_IDS_1;
        assertTimeLogEvent(timeLog, IDs[0], FILTER_PATH+"/new Path", null, 0, 0, null);
        assertTimeLogEvent(timeLog, IDs[4], null, now, 0, 0, null);
        assertTimeLogEvent(timeLog, IDs[2], null, null, 63, 0, null);
        assertTimeLogEvent(timeLog, IDs[3], null, null, -10, 10, null);
        assertTimeLogEvent(timeLog, IDs[1], null, null, 0, 0, "new comment");
        assertTimeLogEvent(timeLog, IDs[3], null, null, 0, 0, "");
        assertTimeLogEvent(timeLog, IDs[3], null, null, 0, 0, "foo");
        assertTimeLogEvent(timeLog, IDs[3], null, null, 0, 0, "");
        assertTrue(timeLogEventsReceived.isEmpty());
    }

    public void testExternalEvents() throws Exception {
        setupFilter();
        List tableIDs = asList(TABLE_IDS_1);
        List tableRows = new ArrayList(Arrays.asList(deepCopyArray(TABLE_CONTENTS_1)));

        // add a new time log entry that matches the filter, and check to
        // see that it appears at the bottom of the list.
        Date now = new Date();
        TimeLogEntryVO addition = new TimeLogEntryVO(139, PATH1, now, 12, 1, "comment", ChangeFlagged.ADDED);
        realTimeMods.addModification(addition);
        assertTableModelEvent(ROW_INSERTED, 5, 5, -1);
        String[] rowContents = new String[] { PATH1, DFMT.format(now), "0:12", "0:01", "comment" };
        tableIDs.add(new Long(139));
        tableRows.add(rowContents);
        assertTableContents(tableIDs, tableRows, tableModel);

        // alter that new time log entry and add it again.  Make certain the
        // contents of the table model update appropriately.
        addition = new TimeLogEntryVO(139, PATH1, now, 15, 1, "comment", ChangeFlagged.ADDED);
        realTimeMods.addModification(addition);
        assertTableModelEvent(ROW_UPDATED, 5, 5, -1);
        rowContents[2] = "0:15";
        assertTableContents(tableIDs, tableRows, tableModel);

        // add a new time log entry that doesn't pass the filter.  It should have no effect.
        addition = new TimeLogEntryVO(140, "/foo"+PATH1, now, 12, 1, "comment", ChangeFlagged.ADDED);
        realTimeMods.addModification(addition);
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(tableIDs, tableRows, tableModel);

        // modify a time log entry that isn't present in the filtered list.  It
        // should have no effect.
        TimeLogEntryVO mod = new TimeLogEntryVO(1, null, null, 123, 0, null, ChangeFlagged.MODIFIED);
        realTimeMods.addModification(mod);
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(tableIDs, tableRows, tableModel);

        // modify a time log entry that is in the filtered list, and watch the
        // change appear.
        mod = new TimeLogEntryVO(136, null, null, 0, 0, "new comment", ChangeFlagged.MODIFIED);
        realTimeMods.addModification(mod);
        assertTableModelEvent(ROW_UPDATED, 2, 2, -1);
        ((String[]) tableRows.get(2))[4] = "new comment";
        assertTableContents(tableIDs, tableRows, tableModel);

        // externally delete one of the entries in the filtered list.
        TimeLogEntryVO del = new TimeLogEntryVO(83, null, null, 0, 0, null, ChangeFlagged.DELETED);
        realTimeMods.addModification(del);
        assertTableModelEvent(ROW_DELETED, 1, 1, -1);
        tableIDs.remove(1);
        tableRows.remove(1);
        assertTableContents(tableIDs, tableRows, tableModel);

        // externally delete an entry that isn't in the filtered list.
        del = new TimeLogEntryVO(5, null, null, 0, 0, null, ChangeFlagged.DELETED);
        realTimeMods.addModification(del);
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(tableIDs, tableRows, tableModel);

        // test a no-op boundary conditions.
        TimeLogEntryVO noop = new TimeLogEntryVO(82, null, null, 0, 0, null, ChangeFlagged.NO_CHANGE);
        tableModel.timeLogChanged(new TimeLogEvent(realTimeMods, noop));
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(tableIDs, tableRows, tableModel);

        // now clear the real time modifications and watch everything go back
        // to the way it was before.
        realTimeMods.clearUncommittedData();
        assertTableModelEvent(ROW_UPDATED, 0, Integer.MAX_VALUE, -1);
        assertTableContents(TABLE_IDS_1, TABLE_CONTENTS_1, tableModel);
    }

    public void testDeleteRow() {
        setupFilter();
        List tableIDs = asList(TABLE_IDS_1);
        List tableRows = new ArrayList(Arrays.asList(deepCopyArray(TABLE_CONTENTS_1)));
        assertTableContents(tableIDs, tableRows, tableModel);

        // deleting nonexistent row should do nothing.
        tableModel.deleteRow(99);
        assertTableContents(tableIDs, tableRows, tableModel);

        // now delete all the rows, one at a time.
        timeLog.addTimeLogListener(this);
        while (!tableIDs.isEmpty()) {
            tableModel.deleteRow(0);
            long id = ((Long) tableIDs.remove(0)).longValue();
            tableRows.remove(0);
            assertTableModelEvent(ROW_DELETED, 0, 0, -1);
            TimeLogEntryVO del = new TimeLogEntryVO(id, null, null, 0, 0, null, ChangeFlagged.DELETED);
            assertTimeLogEvent(del);
            assertTableContents(tableIDs, tableRows, tableModel);
        }
        timeLog.removeTimeLogListener(this);
    }

    public void testDuplicateRow() throws Exception {
        setupFilter();
        List tableIDs = asList(TABLE_IDS_1);
        List tableRows = new ArrayList(Arrays.asList(deepCopyArray(TABLE_CONTENTS_1)));
        assertTableContents(tableIDs, tableRows, tableModel);

        // duplicating nonexistent row should do nothing.
        tableModel.duplicateRow(99);
        assertTableContents(tableIDs, tableRows, tableModel);

        // duplicate an existing row, check for correct behavior
        timeLog.addTimeLogListener(this);
        int tableSize = tableIDs.size();
        String[] rowData = (String[]) tableRows.get(3);
        rowData = (String[]) rowData.clone();
        rowData[4] = null;  // comments are not duplicated.
        tableModel.duplicateRow(3);
        tableIDs.add(new Long(139));
        tableRows.add(rowData);
        assertTableModelEvent(ROW_INSERTED, tableSize, tableSize, -1);
        TimeLogEntryVO addition = new TimeLogEntryVO(139, rowData[0], DFMT.parse(rowData[1]), FormatUtil.parseTime(rowData[2]), FormatUtil.parseTime(rowData[3]), rowData[4], ChangeFlagged.ADDED);
        assertTimeLogEvent(addition);
        assertTableContents(tableIDs, tableRows, tableModel);
        timeLog.removeTimeLogListener(this);
    }

    public void testAddRow() throws Exception {
        while (true) {
            try {
                doTestAddRow();
                break;
            } catch (RetryNeededException rne) {}
        }
    }

    private void doTestAddRow() throws Exception {
        setupFilter();
        List tableIDs = asList(TABLE_IDS_1);
        List tableRows = new ArrayList(Arrays.asList(deepCopyArray(TABLE_CONTENTS_1)));
        assertTableContents(tableIDs, tableRows, tableModel);
        int tableSize = tableIDs.size();

        tableModel.addRow("/foo");
        assertTrue(tableModelEventsReceived.isEmpty());

        timeLog.addTimeLogListener(this);
        Date now = new Date();
        tableModel.addRow(PATH1);
        Date post = new Date();
        if (!post.equals(now))
            throw new RetryNeededException();
        assertTableModelEvent(ROW_INSERTED, tableSize, tableSize, -1);

        String[] rowData = new String[] { PATH1, DFMT.format(now), "0:00", "0:00", null };
        tableIDs.add(new Long(140));
        tableRows.add(rowData);
        assertTableContents(tableIDs, tableRows, tableModel);

        TimeLogEntryVO addition = new TimeLogEntryVO(140, PATH1, now, 0, 0, null, ChangeFlagged.ADDED);
        assertTimeLogEvent(addition);

        timeLog.removeTimeLogListener(this);
    }

    public void testSummarize() throws Exception {
        setupFilter();
        tableModel.setValueAt("extra comment", 1, 4);
        tableModel.setValueAt("0:10", 3, 3);

        tableModel.summarizeRows();
        assertTableContents(SUMMARIZED_IDS, SUMMARIZED_CONTENT, tableModel);

        // perform a second summarization - should be a no-op.
        tableModelEventsReceived.clear();
        tableModel.summarizeRows();
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(SUMMARIZED_IDS, SUMMARIZED_CONTENT, tableModel);

        // now setup a different filter (to create a list of entries that
        // won't get modified at all) and test that summarization is a no-op.
        tableModel.setFilter(null, null, new Date(1092928667100L));
        long[] ids = getIDs(tableModel);
        String[][] content = getContents(tableModel);
        tableModelEventsReceived.clear();
        tableModel.summarizeRows();
        assertTrue(tableModelEventsReceived.isEmpty());
        assertTableContents(ids, content, tableModel);
    }

    private List asList(long[] array) {
        List result = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            result.add(new Long(array[i]));
        }
        return result;
    }

    private long[] getIDs(TableModel m) {
        long[] result = new long[m.getRowCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Long) m.getValueAt(i, -1)).longValue();
        }
        return result;
    }
    private String[][] getContents(TableModel m) {
        String[][] result = new String[m.getRowCount()][m.getColumnCount()];
        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = (String) m.getValueAt(row, col);
            }
        }
        return result;
    }


    private void assertTimeLogEvent(Object tl, long id, String path, Date date, int elapsed, int interrupt, String comment) {
        TimeLogEntryVO expectedTle = new TimeLogEntryVO(id, path, date, elapsed, interrupt, comment, ChangeFlagged.MODIFIED);
        assertTimeLogEvent(expectedTle);
    }

    private void assertModification(Object tl, long id, String path, Date date, int elapsed, int interrupt, String comment) {
        TimeLogModifications timeLog = (TimeLogModifications) tl;
        TimeLogEntryVO expectedTle = new TimeLogEntryVO(id, path, date, elapsed, interrupt, comment, ChangeFlagged.MODIFIED);
        assertEquals(expectedTle, timeLog.getModification(id));
    }

    private String[][] deepCopyArray(String[][] array) {
        String[][] result = new String[array.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = (String[]) TABLE_CONTENTS_1[i].clone();
        }
        return result;
    }

    private void assertTableContents(List expectedIDs, List expectedContents, TimeLogTableModel tableModel) {
        long[] idArray = new long[expectedIDs.size()];
        for (int i = 0; i < idArray.length; i++)
            idArray[i] = ((Long) expectedIDs.get(i)).longValue();

        String[][] contentArray = new String[expectedContents.size()][];
        for (int i = 0; i < contentArray.length; i++)
            contentArray[i] = (String[]) expectedContents.get(i);

        assertTableContents(idArray, contentArray, tableModel);
    }

    private void assertTableContents(long[] expectedIDs, String[][] expectedContents, TimeLogTableModel tableModel) {
        assertEquals(expectedContents.length, tableModel.getRowCount());
        if (expectedContents.length == 0) return;
        assertEquals(expectedContents[0].length, tableModel.getColumnCount());
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            assertEquals(expectedIDs[row], ((Long) tableModel.getValueAt(row, -1)).longValue());
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                assertEquals("incorrect value at (" + row + "," + col + ")",
                        expectedContents[row][col],
                        tableModel.getValueAt(row, col));
            }
        }
        assertNull(tableModel.getValueAt(1, 99));
    }

    private void setValue(TableModel tableModel, int row, int col, String value, boolean shouldSucceed, String[][] expectedContents) {
        Object oldValue = tableModel.getValueAt(row, col);
        assertTrue(tableModel.isCellEditable(row, col));
        tableModel.setValueAt(value, row, col);
        if (shouldSucceed) {
            assertTableModelEvent(ROW_UPDATED, row, row, -1);
            assertEquals(value, tableModel.getValueAt(row, col));
            expectedContents[row][col] = value;
        } else {
            assertEquals(oldValue, tableModel.getValueAt(row, col));
        }
    }

    private void assertTimeLogEvent(Object expectedEntry) {
        assertFalse("expected to find an event", timeLogEventsReceived.isEmpty());
        Object evt = timeLogEventsReceived.remove(0);
        assertTrue("event should be a TimeLogEvent",
                evt instanceof TimeLogEvent);
        TimeLogEvent e = (TimeLogEvent) evt;
        assertSame(timeLog, e.getSource());
        assertEquals(expectedEntry, e.getTimeLogEntry());
    }

    public void timeLogChanged(TimeLogEvent e) {
        timeLogEventsReceived.add(e);
    }

    public void assertTableModelEvent(int type, int firstRow, int lastRow, int col) {
        assertFalse("expected to find an event", tableModelEventsReceived.isEmpty());
        Object evt = tableModelEventsReceived.remove(0);
        assertTrue("event should be a TableModelEvent",
                evt instanceof TableModelEvent);
        TableModelEvent e = (TableModelEvent) evt;
        assertSame(tableModel, e.getSource());
        assertEquals(type, e.getType());
        assertEquals(firstRow, e.getFirstRow());
        assertEquals(lastRow, e.getLastRow());
        assertEquals(col, e.getColumn());
    }

    public void tableChanged(TableModelEvent e) {
        tableModelEventsReceived.add(e);
    }

    private class RetryNeededException extends Exception {}

    private class DummyIDSource implements IDSource {
        private long id;
        public DummyIDSource(long maxId) {
            id = maxId;
        }
        public long getNextID() {
            return ++id;
        }

    }
}
