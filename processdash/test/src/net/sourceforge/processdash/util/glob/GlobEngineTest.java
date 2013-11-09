// Copyright (C) 2006-2013 Tuma Solutions, LLC
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
        "label:lazy", GlobEngineConstants.DEFERRED_DATA_MARKER,
            GlobEngineConstants.DEFERRED_TOKEN_PREFIX + "lazy", "d"
    };
    private static final List data = Arrays.asList(TAGGED_DATA);

    private static boolean lazyDataQueried;

    private static final TaggedDataListSource lazyData = new TaggedDataListSource() {
        public List getTaggedData(String token) {
            lazyDataQueried = true;
            assertEquals("lazy", token);
            return Arrays.asList("label:lazy", "a", "f");
        }
    };

    public void testGlobSearch() {
        lazyDataQueried = false;
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
        assertFalse(lazyDataQueried);

        assertLazySearch("lazy", "a,d,f");
        assertLazySearch("foo | lazy", "a,b,c,d,f");
        assertLazySearch("foo -lazy", "b,c");
        assertLazySearch("-blah -laz*", "b,c,e");
        lazyDataQueried = true;
        assertLazySearch("-blah", "a,b,c,d,e");
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

    private void assertLazySearch(String expr, String results) {
        assertList(GlobEngine.search(expr, "label:", data, lazyData),
            results.split(","));
        assertTrue(lazyDataQueried);
        lazyDataQueried = false;
    }


    private void assertList(Set s, String[] values) {
        assertEquals(values.length, s.size());
        List l = new ArrayList(s);
        Collections.sort(l);
        for (int i = 0; i < values.length; i++)
            assertEquals(values[i], l.get(i));
    }
}
