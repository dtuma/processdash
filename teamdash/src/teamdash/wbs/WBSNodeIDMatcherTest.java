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

package teamdash.wbs;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class WBSNodeIDMatcherTest extends TestCase {

    public void testAssignUniqueIDs() {
        // new nodes added to both branches; ensure that the incoming nodes
        // are assigned new, unique IDs
        assertMatch("R{a1,b2,c3}", "R{a1{aa4}b2{bb5}c3{cc9}dd7}",
            "R{a1{aaa4}b2{bbb5}c3{ccc10}ddd6}",
            "R{a1{aaa12}b2{bbb13}c3{ccc10}ddd11}");

        // test relaxed mode assignment, in which incoming nodes can retain
        // their IDs as long as they don't actively collide
        assertMatch("R{a1,b2,c3}", "R{a1{aa4}b2{bb5}c3{cc9}dd7}",
            "R{a1{aaa4}b2{bbb5}c3{ccc8}ddd6}",
            "R{a1{aaa10}b2{bbb11}c3{ccc8}ddd6}",
            WBSNodeIDMatcher.RELAX_INCOMING_ID_REASSIGNMENT);
    }

    public void testAliasedNodes() {
        // test to see that the aliasing logic works
        WBSModel base = buildWBS("R{a1}");

        WBSModel main = buildWBS("R{a1{aa4},b2,c3}");
        main.getNodeForRow(3).setAttribute("Foo", "FOO");
        main.getNodeForRow(4).setAttribute("Bar", 42);

        WBSModel incoming = buildWBS("R{a4{aa5},d2,e3}");
        incoming.getNodeForRow(3).setAttribute("Bar", 42);
        incoming.getNodeForRow(4).setAttribute("Foo", "FOO");

        // first, perform a match without aliases
        new WBSNodeIDMatcher(base, main, incoming);
        assertEquals("R{a1{aa4},d7,e8}", incoming);

        // now, perform a match with aliases
        new WBSNodeIDMatcher(base, main, incoming, "Foo", "Bar");
        assertEquals("R{a1{aa4},d3,e2}", incoming);
    }

    public void testIncomingDeleteAdd() {
        // a node was deleted and then readded in the incoming wbs.
        assertMatch("R{a1,b2}", "R{a1,b2}", "R{a1,b3}", "R{a1,b2}");
        assertMatch("R{a1{b2}}", "R{a1{b2}}", "R{a1{b3}}", "R{a1{b2}}");
    }

    public void testMainDeleteAdd() {
        // a node was deleted and then readded in the main wbs.
        assertMatch("R{a1,b2}", "R{a1,b3}", "R{a1,b2}", "R{a1,b3}");
        assertMatch("R{a1{b2}}", "R{a1{b3}}", "R{a1{b2}}", "R{a1{b3}}");
    }

    public void testIncomingNodeSwap() {
        // a node was inadvertently renamed in the incoming wbs.
        assertMatch("R{a1,b2}", "R{a1,b2}", "R{a1,b3,c2}", "R{a1,b2,c3}");
    }

    public void testIncomingNodeSwapWithMove() {
        // a node was inadvertently renamed in the incoming wbs.
        assertMatch("R{a1,b2}", "R{a1,b2}", "R{a1{c2}b3}", "R{a1{c3}b2}");
    }

    public void testIncomingDoubleSwap() {
        assertMatch("R{a1,b2,d3}", "R{a1,b2}", "R{a1,b4,c3,d2}",
            "R{a1,b2,c4,d3}");
    }

    public void testNoSwapOnDupName() {
        // a node with a duplicate name was inserted in the incoming wbs.
        assertMatch("R{a1,b2}", "R{a1,b2}", "R{a1,b3,b2}", "R{a1,b3,b2}");
        assertMatch("R{a1,b2}", "R{a1,b2}", "R{a1,b2,b3}", "R{a1,b2,b3}");
    }

    public void testDupNamesInBase() {
        assertMatch("R{a1,b2,b3}", "R{a1,b2,b3}", "R{a1,b2,c4}", "R{a1,b2,c4}");
        assertMatch("R{a1,b2,b3}", "R{a1,b2,b3}", "R{a1,b3,c4}", "R{a1,b3,c4}");
    }

    public void testDeepChanges() {
        // incoming branch has accidentally swapped the IDs of B and E.
        // those IDs need to get switched back.  Meanwhile, nodes C and D,
        // which are nested under B, have been added in both branches so
        // the incoming nodes need to match main.
        assertMatch("R{a1,b2}", "R{a1,b2{c3{d4}}}", "R{a1,b3{c5,{d6}},e2}",
            "R{a1,b2{c3,{d4}},e7}");
    }

    private static void assertMatch(String base, String main, String incoming,
            String expected, String... aliasAttrs) {
        WBSModel baseWBS = buildWBS(base);
        WBSModel mainWBS = buildWBS(main);
        WBSModel incomingWBS = buildWBS(incoming);
        WBSNodeIDMatcher matcher = new WBSNodeIDMatcher(baseWBS, mainWBS,
                incomingWBS, aliasAttrs);

        assertEquals(expected, matcher.getIncoming());
    }

    private static void assertEquals(String expected, WBSModel actual) {
        assertEquals(buildWBS(expected), actual);
    }

    private static void assertEquals(WBSModel expected, WBSModel actual) {
        WBSNode[] expNodes = expected.getDescendants(expected.getRoot());
        WBSNode[] actNodes = actual.getDescendants(actual.getRoot());
        assertEquals(expNodes.length, actNodes.length);
        for (int i = 0; i < expNodes.length; i++) {
            WBSNode exp = expNodes[i];
            WBSNode act = actNodes[i];
            assertEquals(exp.getName(), act.getName());
            assertEquals(exp.getUniqueID(), act.getUniqueID());
            assertEquals(exp.getIndentLevel(), act.getIndentLevel());
        }
    }

    private static WBSModel buildWBS(String spec) {
        StringTokenizer tok = new StringTokenizer(spec, "{,}", true);
        WBSModel model = new WBSModel(tok.nextToken(), false);
        int depth = 0;
        while (tok.hasMoreTokens()) {
            String item = tok.nextToken();
            if ("{".equals(item))
                depth++;
            else if ("}".equals(item))
                depth--;
            else if (!",".equals(item))
                buildWBSNode(model, depth, item);
        }
        model.getRoot().setUniqueID(0);
        return model;
    }

    private static void buildWBSNode(WBSModel model, int depth, String spec) {
        String name, id;
        Matcher m = NODE_PAT.matcher(spec);
        assertTrue(m.matches());
        name = m.group(1);
        id = m.group(2);
        WBSNode node = new WBSNode(model, name, "Task", depth, true);
        model.add(node);
        node.setUniqueID(Integer.parseInt(id));
    }

    private static final Pattern NODE_PAT = Pattern.compile("(\\D+)(\\d+)");

}
