// Copyright (C) 2006-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util.glob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class GlobEngineTest extends TestCase {

    private static final String TEXT = "The quick brown fox jumped over the lazy dogs";

    public void testGlobMatch() {
        assertTrue(GlobEngine.test("fox", TEXT));
        assertTrue(GlobEngine.test("f?x", TEXT));
        assertTrue(GlobEngine.test("LAZY", TEXT));
        assertTrue(GlobEngine.test("jum*", TEXT));
        assertTrue(GlobEngine.test("*ick", TEXT));
        assertFalse(GlobEngine.test("bar", TEXT));
        assertTrue(GlobEngine.test("q*k", TEXT));
        assertTrue(GlobEngine.test("fox the", TEXT));
        assertFalse(GlobEngine.test("fox -the", TEXT));
        assertTrue(GlobEngine.test("-qux", TEXT));
        assertFalse(GlobEngine.test("-q*", TEXT));
        assertFalse(GlobEngine.test("*ing", TEXT));

        assertTrue(GlobEngine.test("foo | fox | blah", TEXT));
        assertFalse(GlobEngine.test("(foo fox) | blah", TEXT));
        assertFalse(GlobEngine.test("foo | (fox blah)", TEXT));
        assertTrue(GlobEngine.test("foo | (fox -blah)", TEXT));
        assertTrue(GlobEngine.test("foo | -(fox blah)", TEXT));
        assertTrue(GlobEngine.test("foo | not (fox blah)", TEXT));
        assertTrue(GlobEngine.test("foo | (fox not blah)", TEXT));

        assertTrue(GlobEngine.test("baz", "foo bar baz"));
        assertTrue(GlobEngine.test("f*", "foo bar baz"));
        assertFalse(GlobEngine.test("qux", "foo bar baz"));
        assertFalse(GlobEngine.test("z*", "foo bar baz"));
        assertTrue(GlobEngine.test("-z*", "foo bar baz"));
        assertTrue(GlobEngine.test("foo bar", "foo bar baz"));
        assertTrue(GlobEngine.test("bar foo", "foo bar baz"));
        assertFalse(GlobEngine.test("bar -foo", "foo bar baz"));
    }

    private static final String[] TAGGED_DATA = {
        "orphan",
        "label:foo", "a", "b",
        "label:bar", "a", "d",
        "label:", "orphan2",
        "label:qux", "e",
        "label:FoO", "c",
    };
    private static final List data = Arrays.asList(TAGGED_DATA);

    public void testGlobSearch() {
        assertSearch("foo", "a,b,c" );
        assertSearch("f*", "a,b,c" );
        assertSearch("foo bar", "a" );
        assertSearch("foo -bar", "b,c" );
        assertSearch("bar", "a,d" );
        assertSearch("foo | bar", "a,b,c,d" );
        assertSearch("foo bar | qux", "a,e" );
        assertEmpty("(foo | bar) qux");
        assertEmpty("z");
        assertEmpty("orphan");
        assertEmpty("a*");
        assertSearch("-blah", "a,b,c,d,e");
    }

    private void assertEmpty(String expr) {
        assertEquals(0, GlobEngine.search(expr, "label:", data).size());
    }

    private void assertSearch(String expr, String results) {
        assertSearch(expr, results.split(","));
    }

    private void assertSearch(String expr, String[] values) {
        assertList(GlobEngine.search(expr, "label:", data), values);
    }

    private void assertList(Set s, String[] values) {
        assertEquals(values.length, s.size());
        List l = new ArrayList(s);
        Collections.sort(l);
        for (int i = 0; i < values.length; i++)
            assertEquals(values[i], l.get(i));
    }
}
