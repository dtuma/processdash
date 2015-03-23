// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import net.sourceforge.processdash.util.ThreeWayDiff.ResultItem;
import junit.framework.TestCase;

public class ThreeWayDiffTest extends TestCase {

    private String[] BASE = "The quick brown fox jumped over the lazy dogs.".split(" ");
    private String[] A = "The QUICK fox jumped over three lazy dogs.".split(" ");
    private String[] B = "The quick brown fox jumped over the dog.".split(" ");

    public void testNull() {
        ThreeWayDiff<String> diff = new ThreeWayDiff<String>(BASE, BASE, BASE);
        ResultItem<String>[] results = diff.getMergedResult();
        assertEquals(BASE.length, results.length);
        for (int i = 0; i < results.length; i++) {
            assertEquals(BASE[i], results[i].item);
            assertEquals(i, results[i].basePos);
            assertEquals(i, results[i].aPos);
            assertEquals(i, results[i].bPos);
        }
    }

    public void testMerge1() {
        ThreeWayDiff<String> diff = new ThreeWayDiff<String>(BASE, A, B);
        ResultItem<String>[] results = diff.getMergedResult();
        assertMergeResults(MERGE1_RESULTS, results, false);
        assertTrue(results[0].isUnchanged());

    }
    private static final int UNCHANGED = 0;
    private static final int DELETED_BY_A = 1;
    private static final int DELETED_BY_B = 2;
    private static final int DELETED_BY_BOTH = 3;
    private static final int INSERTED_BY_A = 4;
    private static final int INSERTED_BY_B = 5;

    private static final Object[][] MERGE1_RESULTS = {
        {"The", 0, 0, 0, UNCHANGED },
        {"quick", 1, -1, 1, DELETED_BY_A },
        {"brown", 2, -1, 2, DELETED_BY_A },
        {"QUICK", -1, 1, -1, INSERTED_BY_A },
        {"fox", 3, 2, 3, UNCHANGED },
        {"jumped", 4, 3, 4, UNCHANGED },
        {"over", 5, 4, 5, UNCHANGED },
        {"the", 6, -1, 6, DELETED_BY_A },
        {"three", -1, 5, -1, INSERTED_BY_A },
        {"lazy", 7, 6, -1, DELETED_BY_B },
        {"dogs.", 8, 7, -1, DELETED_BY_B },
        {"dog.", -1, -1, 7, INSERTED_BY_B },
    };



    public void testMerge2() {
        ThreeWayDiff<String> diff = new ThreeWayDiff<String>(BASE, B, B);
        ResultItem<String>[] results = diff.getMergedResult();
        assertMergeResults(MERGE2_RESULTS, results, false);
    }

    private static final Object[][] MERGE2_RESULTS = {
        { "The", 0, 0, 0, UNCHANGED },
        { "quick", 1, 1, 1, UNCHANGED },
        { "brown", 2, 2, 2, UNCHANGED },
        { "fox", 3, 3, 3, UNCHANGED },
        { "jumped", 4, 4, 4, UNCHANGED },
        { "over", 5, 5, 5, UNCHANGED },
        { "the", 6, 6, 6, UNCHANGED },
        // here we detect that mutual deletion is detected correctly.
        { "lazy", 7, -1, -1, DELETED_BY_BOTH },
        { "dogs.", 8, -1, -1, DELETED_BY_BOTH },
        // note: we do not currently provide support for detecting that both
        // people inserted the same thing...so the insertion is listed twice.
        { "dog.", -1, 7, -1, INSERTED_BY_A },
        { "dog.", -1, -1, 7, INSERTED_BY_B },
    };


    public void testMergeText() {
        ResultItem<String>[] results = ThreeWayTextDiff.compareTextByWords(
            "The quick brown fox jumped over the lazy dogs.",
            "The    quick  brown fox jumped over three lazy\n\tdogs.",
            "The quick brown FOX jumped    over the lazy dogs.");
        assertMergeResults(MERGE_TEXT_RESULTS, results, false);
    }

    private static final Object[][] MERGE_TEXT_RESULTS = {
        { "The", 0, 0, 0, UNCHANGED },
        { "    quick", 1, 1, 1, UNCHANGED },
        { "  brown", 2, 2, 2, UNCHANGED },
        { " fox", 3, 3, -1, DELETED_BY_B },
        { " FOX", -1, -1, 3, INSERTED_BY_B },
        { " jumped", 4, 4, 4, UNCHANGED },
        { "    over", 5, 5, 5, UNCHANGED },
        { " the", 6, -1, 6, DELETED_BY_A },
        { " three", -1, 6, -1, INSERTED_BY_A },
        { " lazy", 7, 7, 7, UNCHANGED },
        { "\n\tdogs.", 8, 8, 8, UNCHANGED },
    };


    private void assertMergeResults(Object[][] expected,
            ResultItem<String>[] actual, boolean print) {

        for (int i = 0; i < actual.length; i++) {
            if (print)
                debugPrint(actual[i]);
            if (expected != null) {
                assertEquals(expected[i][0], actual[i].item);
                assertEquals(expected[i][1], actual[i].basePos);
                assertEquals(expected[i][2], actual[i].aPos);
                assertEquals(expected[i][3], actual[i].bPos);
                assertChangeFlags(expected[i][4], actual[i]);
            }
        }
    }

    private void assertChangeFlags(Object expectedFlag, ResultItem item) {
        boolean[] expected = EXPECTED_FLAGS[((Integer)expectedFlag).intValue()];
        assertEquals(expected[0], item.isUnchanged());
        assertEquals(expected[1], item.isDeleted());
        assertEquals(expected[2], item.isDeletedByA());
        assertEquals(expected[3], item.isDeletedByB());
        assertEquals(expected[4], item.isInserted());
        assertEquals(expected[5], item.isInsertedByA());
        assertEquals(expected[6], item.isInsertedByB());
    }

    private static final boolean[][] EXPECTED_FLAGS = {
        // UNCHANGED
        { true, false, false, false, false, false, false },
        // DELETED_BY_A
        { false, true, true, false, false, false, false },
        // DELETED_BY_B
        { false, true, false, true, false, false, false },
        // DELETED_BY_BOTH
        { false, true, true, true, false, false, false },
        // INSERTED_BY_A
        { false, false, false, false, true, true, false },
        // INSERTED_BY_B
        { false, false, false, false, true, false, true},
    };

    static void debugPrint(ResultItem ri) {
        System.out.println("{ \"" + ri.item + "\", " + ri.basePos + ", "
                + ri.aPos + ", " + ri.bPos + ", ? },");
    }

}
