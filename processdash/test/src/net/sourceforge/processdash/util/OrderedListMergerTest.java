// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class OrderedListMergerTest extends TestCase {

    public void testEmpty() {
        assertEquals(Collections.EMPTY_LIST,
                OrderedListMerger.merge(null));

        assertEquals(Collections.EMPTY_LIST,
                OrderedListMerger.merge(Collections.EMPTY_LIST));

        List foo = Arrays.asList("foo bar baz".split(" "));
        List l = new LinkedList();
        l.add(Collections.EMPTY_LIST);
        l.add(foo);
        l.add(Collections.EMPTY_LIST);
        assertEquals(foo, OrderedListMerger.merge(l));
    }


    public void testIdentical() {
        runTest(new String[] {
                "foo bar baz",
                "foo bar baz",
                },
            "foo bar baz");
    }

    public void testSubset() {
        runTest(new String[] {
                "foo bar baz",
                "bar baz",
                },
        "foo bar baz");
        runTest(new String[] {
                "bar baz",
                "foo bar baz",
                },
        "foo bar baz");
    }

    public void testGeneric() {
        runTest(new String[] {
                "foo bar baz",
                "bar qux",
                },
            "foo bar baz qux" );
        runTest(new String[] {
                "foo bar baz",
                "qux bar baz",
                },
            "foo qux bar baz" );
        runTest(new String[] {
                "the fox jumped",
                "suddenly the dogs",
                "quick brown fox",
                "over three dogs",
                "lazy dogs",
                },
                "suddenly the quick brown fox jumped over three lazy dogs");
    }

    private void runTest(String[] data, String results) {
        Set toMerge = new LinkedHashSet();
        for (int i = 0; i < data.length; i++)
            toMerge.add(Arrays.asList(data[i].split(" ")));
        List merged = OrderedListMerger.merge(toMerge);
        assertEquals(Arrays.asList(results.split(" ")), merged);
    }
}
