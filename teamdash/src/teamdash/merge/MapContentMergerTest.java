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

package teamdash.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.MergeWarning.Severity;

public class MapContentMergerTest extends TestCase {

    public void testEquality() {
        MapContentMerger merger = new MapContentMerger();
        Map a = asMap("a=aVal,b=bVal");
        Map b = asMap("a=aVal,b=bVal");
        Map c = asMap("a=aVal,b=bVal,c=cVal");
        assertTrue(merger.isEqual(a, b));
        assertTrue(merger.isEqual(b, a));
        assertFalse(merger.isEqual(a, c));
        assertFalse(merger.isEqual(c, a));
    }

    public void testAddedAttribute() {
        assertConflictFreeMerge("a=A", "a=A", "a=A,c=C", "a=A,c=C");
    }

    public void testDeletedAttribute() {
        assertConflictFreeMerge("a=A", "a=A", "c=C", map("a", null, "c", "C"));
    }

    public void testNonconflictingChange() {
        assertConflictFreeMerge("a=A", "a=A", "a=A'", "a=A'");
    }

    public void testSimultaneousChange() {
        assertConflictFreeMerge("a=A", "a=A'", "a=A'", "a=A'");
        assertConflictFreeMerge(map(), "a=A'", "a=A'", "a=A'");
        assertConflictFreeMerge(null, "a=A'", "a=A'", "a=A'");
    }

    public void testConflictingChange() {
        assertConflictingMerge("a=A", "a=A'", "a=A^", "a=A'", null,
            conflict("a"));
        assertConflictingMerge("a=A", "a=A^", "a=A'", "a=A^", null,
            conflict("a"));
    }

    public void testMultipleConflicts() {
        assertConflictingMerge("a=A", "a=A',b=B", "a=A^,b=B'", "a=A',b=B",
            null, conflict("a"), conflict("b"));
    }

    public void testUnrelatedChanges() {
        assertConflictFreeMerge("a=A", "a=A',b=B", "a=A,c=C", "a=A',b=B,c=C");
    }

    public void testBaselessMerge() {
        assertConflictFreeMerge(null, "a=A", "b=B", "a=A,b=B");
        assertConflictingMerge(null, "a=A'", "a=A", "a=A'", null, conflict("a"));
        assertConflictingMerge(null, "a=A", "a=A'", "a=A", null, conflict("a"));
    }

    public void testSpecialHandlers() {
        MapContentMerger merger = new MapContentMerger();
        merger.addHandler("a", DefaultAttributeMerger.SILENTLY_PREFER_MAIN);
        merger.addHandler("b", new DefaultAttributeMerger(Severity.INFO));
        merger.addHandler("c.*", new DefaultAttributeMerger(Severity.CONFLICT,
                "cKey"));
        merger.addHandler("d", DefaultAttributeMerger.SILENTLY_PREFER_INCOMING);
        merger.addHandler("e.*", new DefaultAttributeMerger(Severity.INFO,
                "eKey", true));

        assertConflictingMerge("a=A,b=B,cat=C,d=D,dog=D,eel=E",
            "a=A',b=B',cat=C',d=D',dog=D',eel=E'",
            "a=A^,b=B^,cat=C^,d=D^,dog=D^,eel=E^",
            "a=A',b=B',cat=C',d=D^,dog=D',eel=E^", merger,
            // no conflict for A, the handler said so.
            info("b"), conflict("cat", "cKey"), conflict("dog"),
            // no conflict for D, the handler said so.
            info("eel", "eKey"));
    }

    private void assertConflictFreeMerge(Object base, Object main,
            Object incoming, Object expected) {
        assertConflictFreeMerge(base, main, incoming, expected,
            new MapContentMerger());
        assertConflictFreeMerge(base, incoming, main, expected,
            new MapContentMerger());
    }

    private void assertConflictFreeMerge(Object base, Object main,
            Object incoming, Object expected, MapContentMerger merger) {
        Err err = new Err();
        Map merged = merger.mergeContent(new TreeNode("X", null), asMap(base),
            asMap(main), asMap(incoming), err);
        assertEquals(asMap(expected), merged);
        assertTrue(err.warnings.isEmpty());
    }

    private void assertConflictingMerge(Object base, Object main,
            Object incoming, Object expected, MapContentMerger merger,
            MergeWarning... warnings) {
        Err err = new Err();
        if (merger == null)
            merger = new MapContentMerger();
        Map merged = merger.mergeContent(new TreeNode("X", null), asMap(base),
            asMap(main), asMap(incoming), err);
        assertEquals(asMap(expected), merged);
        assertSetEquals(Arrays.asList(warnings), err.warnings);
    }

    private static void assertSetEquals(Collection expected, Collection actual) {
        assertEquals(expected.size(), actual.size());
        for (Object e : expected)
            assertTrue("Set contains " + e, actual.contains(e));
    }

    private static Map asMap(Object spec) {
        if (spec == null)
            return null;
        else if (spec instanceof Map)
            return (Map) spec;
        else
            return map((Object[]) spec.toString().split("[,=]"));
    }

    private static Map map(Object... data) {
        Map result = new HashMap();
        for (int i = 0; i < data.length; i += 2)
            result.put(data[i], data[i + 1]);
        return result;
    }

    private static MergeWarning<String> conflict(String attrName) {
        return conflict(attrName, "Attribute." + attrName);
    }

    private static MergeWarning<String> conflict(String attrName, String key) {
        return new AttributeMergeWarning<String>(Severity.CONFLICT, key, "X",
                attrName, null, null, null);
    }

    private static MergeWarning<String> info(String attrName) {
        return info(attrName, "Attribute." + attrName);
    }

    private static MergeWarning<String> info(String attrName, String key) {
        return new AttributeMergeWarning<String>(Severity.INFO, key, "X",
                attrName, null, null, null);
    }

    private class Err implements ErrorReporter {
        List<MergeWarning> warnings = new ArrayList<MergeWarning>();

        public void addMergeWarning(MergeWarning w) {
            warnings.add(w);
        }
    }

}
