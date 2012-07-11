// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.team;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.MergeWarning;
import teamdash.merge.MergeWarning.Severity;

public class TeamMemberListMergerTest extends TestCase {

    public void testStartDateChanges() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        // make a change to start date in one schedule.
        a.setStartOnDayOfWeek(5);

        // confirm that the merge (in either direction) occurs without
        // conflict, and that it observes the change.
        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        TeamMemberList m = merge.getMerged();
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(a.getStartOnDayOfWeek(), m.getStartOnDayOfWeek());
        assertEquals(a.getZeroDay(), m.getZeroDay());

        merge = new TeamMemberListMerger(base, b, a);
        m = merge.getMerged();
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(a.getStartOnDayOfWeek(), m.getStartOnDayOfWeek());
        assertEquals(a.getZeroDay(), m.getZeroDay());

        // make a conflicting change to start date in the other schedule.
        b.setStartOnDayOfWeek(3);

        // confirm that the merge reports a conflict, and that the date from
        // the main branch is preferred.
        merge = new TeamMemberListMerger(base, a, b);
        m = merge.getMerged();
        assertWarnings(merge.getMergeWarnings(),
            conflict(-100, TeamMemberListMerger.ZERO_DAY));
        assertEquals(a.getStartOnDayOfWeek(), m.getStartOnDayOfWeek());
        assertEquals(a.getZeroDay(), m.getZeroDay());

        merge = new TeamMemberListMerger(base, b, a);
        m = merge.getMerged();
        assertWarnings(merge.getMergeWarnings(),
            conflict(-100, TeamMemberListMerger.ZERO_DAY));
        assertEquals(b.getStartOnDayOfWeek(), m.getStartOnDayOfWeek());
        assertEquals(b.getZeroDay(), m.getZeroDay());
    }

    public void testNonconflictingEdits() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);
        for (int i = 0;  i < a.getRowCount(); i++) {
            TeamMember m = ((i & 1) == 1 ? a : b).get(i);
            TeamMember n = ((i & 1) == 1 ? b : a).get(i);

            m.setName(m.getName() + "'");
            n.setInitials(n.getInitials() + n.getInitials());
            m.setServerIdentityInfo("serverUser" + m.getInitials());
            n.setColor(Color.BLACK);
            m.setHoursPerWeek(m.getHoursPerWeek() * 11);
            n.getSchedule().setStartWeek(i+1);
            m.getSchedule().setEndWeek(i+5);
            n.getSchedule().addException(i+3, i * 2.0);
        }

        TeamMemberList[] merges = new TeamMemberList[2];
        Map initialsChanges1 = map("aa", "aaaa", "cc", "cccc", "ee", "eeee");
        Map initialsChanges2 = map("bb", "bbbb", "dd", "dddd", "ff", "ffff");

        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(initialsChanges1, merge.getChangesNeededToIncomingInitials());
        assertEquals(initialsChanges2, merge.getChangesNeededToMainInitials());
        merges[0] = merge.getMerged();

        merge = new TeamMemberListMerger(base, b, a);
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(initialsChanges2, merge.getChangesNeededToIncomingInitials());
        assertEquals(initialsChanges1, merge.getChangesNeededToMainInitials());
        merges[1] = merge.getMerged();

        for (int i = 0;  i < base.getRowCount(); i++) {
            for (int j = 0;  j < 2;  j++) {
                TeamMember btm = base.get(i);
                TeamMember mtm = merges[j].get(i);

                assertEquals(btm.getName() + "'", mtm.getName());
                assertEquals(btm.getInitials() + btm.getInitials(), mtm.getInitials());
                assertEquals("serverUser" + btm.getInitials(), mtm.getServerIdentityInfo());
                assertEquals(Color.BLACK, mtm.getColor());
                assertEquals(btm.getHoursPerWeek() * 11, mtm.getHoursPerWeek(), 0.000001);
                assertEquals(i+1, mtm.getSchedule().getStartWeek());
                assertEquals(i+5, mtm.getSchedule().getEndWeek());
                WeekData weekData = mtm.getSchedule().getWeekData(i+3);
                assertEquals(WeekData.TYPE_EXCEPTION, weekData.getType());
                assertEquals(i * 2.0, weekData.getHours(), 0.00001);
            }
        }
    }

    public void testConflictingEdits() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        TeamMember atm = a.get(0);
        TeamMember btm = b.get(0);

        atm.setName("New A Name");
        btm.setName("New B Name");
        atm.setServerIdentityInfo("serverInfoA");
        btm.setServerIdentityInfo("serverInfoB");
        atm.setInitials("aaa");
        btm.setInitials("bbb");
        atm.setHoursPerWeek(11.0);
        btm.setHoursPerWeek(22.0);
        WeeklySchedule atms = atm.getSchedule();
        WeeklySchedule btms = btm.getSchedule();
        atms.setStartWeek(1);
        btms.setStartWeek(2);
        atms.setEndWeek(11);
        btms.setEndWeek(22);
        atms.addException(5, 111.0);
        btms.addException(5, 222.0);
        atms.addException(7, 111.0);
        btms.addException(7, 222.0);

        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        assertWarnings(merge.getMergeWarnings(),
            conflict(10, TeamMember.NAME_ATTR),
            conflict(10, TeamMember.INITIALS_ATTR),
            conflict(10, WeeklySchedule.HOURS_PER_WEEK_ATTR),
            conflict(10, WeeklySchedule.START_WEEK_ATTR),
            conflict(10, WeeklySchedule.END_WEEK_ATTR),
            conflict(10, WeeklySchedule.EXCEPTION_TAG));
        assertEquals(atm, merge.getMerged().get(0));
        assertEquals(map("bbb", "aaa"),
            merge.getChangesNeededToIncomingInitials());
        assertTrue(merge.getChangesNeededToMainInitials().isEmpty());
        for (int i = 1;  i < base.getRowCount();  i++)
            assertEquals(base.get(i), merge.getMerged().get(i));

        merge = new TeamMemberListMerger(base, b, a);
        assertWarnings(merge.getMergeWarnings(),
            conflict(10, TeamMember.NAME_ATTR),
            conflict(10, TeamMember.INITIALS_ATTR),
            conflict(10, WeeklySchedule.HOURS_PER_WEEK_ATTR),
            conflict(10, WeeklySchedule.START_WEEK_ATTR),
            conflict(10, WeeklySchedule.END_WEEK_ATTR),
            conflict(10, WeeklySchedule.EXCEPTION_TAG));
        assertEquals(btm, merge.getMerged().get(0));
        assertEquals(map("aaa", "bbb"),
            merge.getChangesNeededToIncomingInitials());
        assertTrue(merge.getChangesNeededToMainInitials().isEmpty());
        for (int i = 1;  i < base.getRowCount();  i++)
            assertEquals(base.get(i), merge.getMerged().get(i));
    }

    public void testSpecialHandlingForColor() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        TeamMember atm = a.get(0);
        TeamMember btm = b.get(0);

        atm.setColor(Color.YELLOW);
        btm.setColor(Color.GREEN);

        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(btm, merge.getMerged().get(0));
        for (int i = 1;  i < base.getRowCount();  i++)
            assertEquals(base.get(i), merge.getMerged().get(i));

        merge = new TeamMemberListMerger(base, b, a);
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertEquals(atm, merge.getMerged().get(0));
        for (int i = 1;  i < base.getRowCount();  i++)
            assertEquals(base.get(i), merge.getMerged().get(i));
    }

    public void testConflictingTeamMemberNames() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        // in both branches, edit different people to have the same name
        // or initials
        a.get(0).setName("New Name");
        b.get(1).setName("New Name");

        a.get(2).setInitials("abc");
        b.get(3).setInitials("abc");

        // perform merges in both directions and assert our expectstions
        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        assertWarnings(merge.getMergeWarnings(),
            conflict(10, 20, "Attribute.Name_Conflict"),
            conflict(30, 40, "Attribute.Initials_Conflict"));
        assertEquals("New Name", merge.getMerged().get(0).getName());
        assertEquals("New Namex", merge.getMerged().get(1).getName());
        assertEquals("abc", merge.getMerged().get(2).getInitials());
        assertEquals("abcx", merge.getMerged().get(3).getInitials());
        assertEquals(map("cc", "abc", "abc", "abcx"),
            merge.getChangesNeededToIncomingInitials());
        assertEquals(map("dd", "abcx"),
            merge.getChangesNeededToMainInitials());

        merge = new TeamMemberListMerger(base, b, a);
        assertWarnings(merge.getMergeWarnings(),
            conflict(20, 10, "Attribute.Name_Conflict"),
            conflict(40, 30, "Attribute.Initials_Conflict"));
        assertEquals("New Namex", merge.getMerged().get(0).getName());
        assertEquals("New Name", merge.getMerged().get(1).getName());
        assertEquals("abcx", merge.getMerged().get(2).getInitials());
        assertEquals("abc", merge.getMerged().get(3).getInitials());
        assertEquals(map("dd", "abc", "abc", "abcx"),
            merge.getChangesNeededToIncomingInitials());
        assertEquals(map("cc", "abcx"),
            merge.getChangesNeededToMainInitials());

        // add a team member in one branch that has the same name as
        // an edit in the other branch
        TeamMemberList c = new TeamMemberList(base);
        c.maybeAddEmptyRow();
        TeamMember tmc = c.get(6);
        tmc.setName("New Name");
        tmc.setInitials("xyz");
        tmc.setId(111);

        merge = new TeamMemberListMerger(base, a, c);
        assertWarnings(merge.getMergeWarnings(),
            conflict(10, 111, "Attribute.Name_Conflict"));
        assertEquals("New Name", merge.getMerged().get(0).getName());
        assertEquals("New Namex", merge.getMerged().get(6).getName());

        merge = new TeamMemberListMerger(base, c, a);
        assertWarnings(merge.getMergeWarnings(),
            conflict(111, 10, "Attribute.Name_Conflict"));
        assertEquals("New Namex", merge.getMerged().get(0).getName());
        assertEquals("New Name", merge.getMerged().get(6).getName());

        // add a team member in one branch that has the same initials as
        // an edit in the other branch
        TeamMemberList d = new TeamMemberList(base);
        d.maybeAddEmptyRow();
        TeamMember tmd = d.get(6);
        tmd.setName("Some Name");
        tmd.setInitials("abc");
        tmd.setId(222);

        merge = new TeamMemberListMerger(base, a, d);
        assertWarnings(merge.getMergeWarnings(),
            conflict(30, 222, "Attribute.Initials_Conflict"));
        assertEquals("abc", merge.getMerged().get(2).getInitials());
        assertEquals("abcx", merge.getMerged().get(6).getInitials());
        assertEquals(map("cc", "abc", "abc", "abcx"),
            merge.getChangesNeededToIncomingInitials());
        assertTrue(merge.getChangesNeededToMainInitials().isEmpty());

        merge = new TeamMemberListMerger(base, d, a);
        assertWarnings(merge.getMergeWarnings(),
            conflict(222, 30, "Attribute.Initials_Conflict"));
        assertEquals("abcx", merge.getMerged().get(2).getInitials());
        assertEquals("abc", merge.getMerged().get(6).getInitials());
        assertEquals(map("abc", "abcx"),
            merge.getChangesNeededToIncomingInitials());
        assertEquals(map("cc", "abcx"), merge.getChangesNeededToMainInitials());
    }

    public void testDuplicatedAddition() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        // add two individuals with matching name/initials or serverInfo to each
        // of A and B.
        a.maybeAddEmptyRow();
        TeamMember tmas = a.get(6);
        tmas.setName("New Nonmatching Name");
        tmas.setInitials("abc");
        tmas.setServerIdentityInfo("newlyAddedPerson");
        tmas.setId(111);
        a.maybeAddEmptyRow();
        TeamMember tman = a.get(7);
        tman.setName("New Matching Name");
        tman.setInitials("pdq");
        tman.setId(222);

        b.maybeAddEmptyRow();
        TeamMember tmbs = b.get(6);
        tmbs.setName("New Nonmatching Name 2");
        tmbs.setInitials("abcd");
        tmbs.setServerIdentityInfo("newlyAddedPerson");
        tmbs.setId(333);
        b.maybeAddEmptyRow();
        TeamMember tmbn = b.get(7);
        tmbn.setName("New Matching Name");
        tmbn.setInitials("pdq");
        tmbn.setId(444);

        // perform merges in both directions and assert our expectations
        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        assertEquals(8, merge.getMerged().getRowCount());
        assertWarnings(merge.getMergeWarnings(),
            conflict(111, TeamMember.NAME_ATTR),
            conflict(111, TeamMember.INITIALS_ATTR));
        assertEquals(tmas, merge.getMerged().get(6));
        assertEquals(tman, merge.getMerged().get(7));
        assertEquals(map(333, 111, 444, 222),
            merge.getChangesMadeToIncomingIDs());
        assertEquals(map("abcd", "abc"),
            merge.getChangesNeededToIncomingInitials());
        assertTrue(merge.getChangesNeededToMainInitials().isEmpty());

        merge = new TeamMemberListMerger(base, b, a);
        assertEquals(8, merge.getMerged().getRowCount());
        assertWarnings(merge.getMergeWarnings(),
            conflict(333, TeamMember.NAME_ATTR),
            conflict(333, TeamMember.INITIALS_ATTR));
        assertEquals(tmbs, merge.getMerged().get(6));
        assertEquals(tmbn, merge.getMerged().get(7));
        assertEquals(map(111, 333, 222, 444),
            merge.getChangesMadeToIncomingIDs());
        assertEquals(map("abc", "abcd"),
            merge.getChangesNeededToIncomingInitials());
        assertTrue(merge.getChangesNeededToMainInitials().isEmpty());
    }

    public void testExceptionRemoval() {
        TeamMemberList base = loadTeam("mergeTestData1");
        TeamMemberList a = new TeamMemberList(base);
        TeamMemberList b = new TeamMemberList(base);

        TeamMember atm = a.get(0);
        TeamMember btm = b.get(0);
        WeeklySchedule btms = btm.getSchedule();

        atm.setName("New A Name");
        btms.removeException(4);

        TeamMemberListMerger merge = new TeamMemberListMerger(base, a, b);
        TeamMember mtm = merge.getMerged().get(0);
        assertEquals("New A Name", mtm.getName());
        WeekData weekData = mtm.getSchedule().getWeekData(4);
        assertEquals(WeekData.TYPE_DEFAULT, weekData.getType());
    }


    private static void assertEquals(TeamMember expected, TeamMember actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getInitials(), actual.getInitials());
        assertEquals(expected.getServerIdentityInfo(), actual.getServerIdentityInfo());
        assertEquals(expected.getColor(), actual.getColor());
        assertEquals(expected.getExtraAttributes(), actual.getExtraAttributes());
        assertEquals(expected.getSchedule().getStartWeek(), actual.getSchedule().getStartWeek());
        assertEquals(expected.getSchedule().getEndWeek(), actual.getSchedule().getEndWeek());
        int maxWeek = Math.max(
                expected.getSchedule().getMaintenanceStartWeek(), //
                actual.getSchedule().getMaintenanceStartWeek());
        for (int i = expected.getSchedule().getStartWeek();  i < maxWeek; i++) {
            WeekData expectedWeek = expected.getSchedule().getWeekData(i);
            WeekData actualWeek = actual.getSchedule().getWeekData(i);
            assertEquals(expectedWeek.getType(), actualWeek.getType());
            assertEquals(expectedWeek.getHours(), actualWeek.getHours(), 0.0001);
        }
    }

    private static void assertWarnings(Collection actual,
            MergeWarning... expected) {
        assertSetEquals(Arrays.asList(expected), actual);
    }

    private static void assertSetEquals(Collection expected, Collection actual) {
        assertEquals(expected.size(), actual.size());
        for (Object e : expected)
            assertTrue("Set contains " + e, actual.contains(e));
    }

    private static Map map(Object... data) {
        Map result = new HashMap();
        for (int i = 0; i < data.length; i += 2)
            result.put(data[i], data[i + 1]);
        return result;
    }

    private static MergeWarning<Integer> conflict(int id, String attrName) {
        return conflict(id, attrName, "Attribute." + attrName);
    }

    private static MergeWarning<Integer> conflict(int id, String attrName,
            String key) {
        return new AttributeMergeWarning<Integer>(Severity.CONFLICT, key, id,
                attrName, null, null, null);
    }

    private static MergeWarning<Integer> conflict(int idA, int idB, String key) {
        return new MergeWarning<Integer>(Severity.CONFLICT, key, idA, idB);
    }

    private TeamMemberList loadTeam(String name) {
        try {
            String filename = name + ".xml";
            return new TeamMemberList(XMLUtils.parse(
                getClass().getResourceAsStream(filename)).getDocumentElement());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Got exception");
            return null;
        }
    }
}
