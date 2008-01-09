// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

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
        assertMergeResults(MERGE1_RESULTS, results);
    }

    private static final Object[][] MERGE1_RESULTS = {
        {"The", 0, 0, 0 },
        {"quick", 1, -1, 1 },
        {"brown", 2, -1, 2 },
        {"QUICK", -1, 1, -1 },
        {"fox", 3, 2, 3 },
        {"jumped", 4, 3, 4 },
        {"over", 5, 4, 5 },
        {"the", 6, -1, 6 },
        {"three", -1, 5, -1 },
        {"lazy", 7, 6, -1 },
        {"dogs.", 8, 7, -1 },
        {"dog.", -1,-1, 7 },
    };


    public void testMerge2() {
        ThreeWayDiff<String> diff = new ThreeWayDiff<String>(BASE, B, B);
        ResultItem<String>[] results = diff.getMergedResult();
        assertMergeResults(MERGE2_RESULTS, results);
    }

    private static final Object[][] MERGE2_RESULTS = {
        { "The", 0, 0, 0 },
        { "quick", 1, 1, 1 },
        { "brown", 2, 2, 2 },
        { "fox", 3, 3, 3 },
        { "jumped", 4, 4, 4 },
        { "over", 5, 5, 5 },
        { "the", 6, 6, 6 },
        // here we detect that mutual deletion is detected correctly.
        { "lazy", 7, -1, -1 },
        { "dogs.", 8, -1, -1 },
        // note: we do not currently provide support for detecting that both
        // people inserted the same thing...so the insertion is listed twice.
        { "dog.", -1, 7, -1 },
        { "dog.", -1, -1, 7 },
    };


    public void testMergeText() {
        ResultItem<String>[] results = ThreeWayTextDiff.compareTextByWords(
            "The quick brown fox jumped over the lazy dogs.",
            "The    quick  brown fox jumped over three lazy dogs.",
            "The quick brown FOX jumped    over the lazy dogs.");
        assertMergeResults(MERGE_TEXT_RESULTS, results, true);
    }

    private static final Object[][] MERGE_TEXT_RESULTS = {
        { "The", 0, 0, 0 },
        { "    quick", 1, 1, 1 },
        { "  brown", 2, 2, 2 },
        { " fox", 3, 3, -1 },
        { " FOX", -1, -1, 3 },
        { " jumped", 4, 4, 4 },
        { "    over", 5, 5, 5 },
        { " the", 6, -1, 6 },
        { " three", -1, 6, -1 },
        { " lazy", 7, 7, 7 },
        { " dogs.", 8, 8, 8 },
    };


    private void assertMergeResults(Object[][] expected,
            ResultItem<String>[] actual) {
        assertMergeResults(expected, actual, false);
    }

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
            }
        }
    }

    static void debugPrint(ResultItem ri) {
        System.out.println(ri.item + "; basePos=" + ri.basePos + ", aPos="
                + ri.aPos + ", bPos=" + ri.bPos);
    }
}
