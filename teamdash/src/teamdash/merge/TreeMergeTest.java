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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import teamdash.merge.MergeWarning.Severity;
import teamdash.merge.TreeNodeChange.Type;

public class TreeMergeTest extends TestCase {

    public void testNoOpRootDiff() {
        TreeDiff<String, String> diff = runDiff(TREE_ROOT_1, TREE_ROOT_1);
        assertEquals(0, diff.getChanges().size());
        for (Type t : Type.values())
            assertEquals(0, diff.getChangedNodeIDs(t).size());
    }

    public void testSimpleRootContentDiff() {
        TreeDiff<String, String> diff = runDiff(TREE_ROOT_1, TREE_ROOT_2);
        assertEquals(1, diff.getChanges().size());
        TreeNodeChange change = diff.getChanges().get(0);
        assertEquals(Type.Edit, change.getType());
        assertEquals("A", change.getNode().getID());
        assertEquals("'", change.getNode().getContent());
    }

    private static final String TREE_ROOT_1 = "A";

    private static final String TREE_ROOT_2 = "A'";

    public void testTreeDiff1() {
        TreeDiff<String, String> diff = runDiff(TREE_DIFF_0, TREE_DIFF_1);
        List<TreeNodeChange> changes = new ArrayList(diff.getChanges());

        TreeNodeChange a = extractChange(changes, "a", Type.Reorder);
        assertContext(a, "R", "b");

        TreeNodeChange i = extractChange(changes, "i", Type.Add);
        assertContext(i, "R", "b", "a");

        assertTrue(changes.isEmpty());
    }

    public void testTreeDiff2() {
        TreeDiff<String, String> diff = runDiff(TREE_DIFF_0, TREE_DIFF_2);
        List<TreeNodeChange> changes = new ArrayList(diff.getChanges());

        TreeNodeChange b = extractChange(changes, "b", Type.Edit);
        assertContext(b, "R", "a");

        TreeNodeChange d = extractChange(changes, "d", Type.Reorder);
        assertContext(d, "a", "e");

        TreeNodeChange g = extractChange(changes, "g", Type.Delete);
        assertEquals("b", g.getParentID());

        TreeNodeChange h = extractChange(changes, "h", Type.Move);
        assertContext(h, "f");

        assertTrue(changes.isEmpty());
    }

    public void testTreeDiff3() {
        TreeDiff<String, String> diff = runDiff(TREE_DIFF_0, TREE_DIFF_3);
        List<TreeNodeChange> changes = new ArrayList(diff.getChanges());

        extractChange(changes, "d", Type.Delete);

        TreeNodeChange g = extractChange(changes, "g", Type.Move);
        assertContext(g, "a");

        TreeNodeChange b = extractChange(changes, "b", Type.Move);
        assertContext(b, "a", "g");

        assertTrue(changes.isEmpty());
    }

    private static final String TREE_DIFF_0 = "R{a{d,e,f}b{g{h}}}";

    private static final String TREE_DIFF_1 = "R{b{g{h}}a{d,e,f}i}";

    private static final String TREE_DIFF_2 = "R{a{e,d,f{h}}b'}";

    private static final String TREE_DIFF_3 = "R{a{g{h}b,e,f}}";

    private static final String TREE_MERGE_012 = "R{b',a{e,d,f{h}}i";

    public void testEmptyMerge() {
        assertConflictFreeMerge(TREE_DIFF_0, TREE_DIFF_0, TREE_DIFF_0,
            TREE_DIFF_0);
    }

    public void testNoMainChangeMerge() {
        assertConflictFreeMerge(TREE_DIFF_0, TREE_DIFF_0, TREE_DIFF_2,
            TREE_DIFF_2);
    }

    public void testNoIncomingChangeMerge() {
        assertConflictFreeMerge(TREE_DIFF_0, TREE_DIFF_1, TREE_DIFF_0,
            TREE_DIFF_1);
    }

    public void testMerge1() {
        assertConflictFreeMerge(TREE_DIFF_0, TREE_DIFF_1, TREE_DIFF_2,
            TREE_MERGE_012);
    }

    public void testMergeI1() {
        assertConflictFreeMerge("R{a{c}b{d,e}}", "R{a{i,c}b{d,e}}",
            "R{a{c}b{d,j,e}}", "R{a{i,c}b{d,j,e}}");
    }

    public void testMergeI2() {
        assertConflictFreeMerge("R{a}", "R{a,i}", "R{a,j}", "R{a,i,j}",
            "R{a,j,i}");
    }

    public void testMergeI3() {
        assertConflictFreeMerge("R{a,b,c}", "R{a,i,b,c}", "R{a,b,c,j}",
            "R{a,i,b,c,j}");
    }

    public void testMergeI3b() {
        assertConflictFreeMerge("R{a,b,c}", "R{a,i,b,c}", "R{k,a,b,c,j}",
            "R{k,a,i,b,c,j}");
    }

    public void testMergeD1() {
        assertConflictFreeMerge("R{s1{p1,p2}s2{p3,p4}}", "R{s1{p1,p2}}",
            "R{s1{p1},s2{p3,p4}}", "R{s1{p1}}");
    }

    public void testMergeD2() {
        assertConflictFreeMerge("R{a,b}", "R{a}", "R{a}", "R{a}");
    }

    public void testMergeD3() {
        assertConflictFreeMerge("R{a,b,c,d}", "R{a,b,d}", "R{b,c,d}", "R{b,d}");
    }

    public void testMergeD4() {
        assertConflictFreeMerge("R{a,b,c,d}", "R{a,b,d}", "R{d,c,b,a}", "R{d,b,a}");
    }

    public void testMergeU1() {
        assertConflictFreeMerge("R{a,b}", "R{a',b}", "R{a,b'}", "R{a',b'}");
    }

    public void testMergeU2() {
        assertConflictFreeMerge("R{a,b}", "R{a,b'}", "R{a,b'}", "R{a,b'}");
    }

    public void testMergeU3() {
        assertConflictingMerge("R{a,b}", "R{a',b}", "R{a^,b'}", "R{a!!!,b'}",
            idList(), conflict("a", Type.Edit, Type.Edit));
    }

    public void testMergeM1() {
        assertConflictFreeMerge("R{a{c,d}b{e{f}}}", "R{a{d,c}b{e{f}}}",
            "R{a{c,d}b{f{e}}}", "R{a{d,c}b{f{e}}}");
    }

    public void testMergeM2() {
        assertConflictFreeMerge("R{p1,p2,p3,p4,p5}", "R{p2,p1,p3,p4,p5}",
            "R{p1,p2,p3,p5,p4}", "R{p2,p1,p3,p5,p4}");
    }

    public void testMergeM3() {
        assertConflictFreeMerge("R{a{a1,a2}b{b1,b2}}", "R{a{a2,a1}b{b1,b2}}",
            "R{b{b1,b2}a{a1,a2}}", "R{b{b1,b2}a{a2,a1}}");
    }

    public void testMergeM4() {
        assertConflictFreeMerge("R{a,b,c}", "R{b{a}c}", "R{b,c,a}", "R{b{a}c}");
    }

    public void testMergeM5() {
        assertConflictFreeMerge("R{a,b,c,d,e}", "R{b,c,d,e,a}", "R{a,b,d,c,e}",
            "R{b,d,c,e,a}");
    }

    public void testMergeA1() {
        assertConflictFreeMerge("R{a,b}", "R{a,b'}", "R{a,b,c}", "R{a,b',c}");
    }

    public void testMergeA2() {
        assertConflictFreeMerge("R{a,b,c}", "R{a,c}", "R{a',b,c}", "R{a',c}");
    }

    public void testMergeA3() {
        assertConflictFreeMerge("R{s1{p1}s2{p2}s3{p3}}",
            "R{s1{p1}s2{p2}s3{p3}s4{p4}}", "R{s2{p2}s1{p1}s3{p3}}",
            "R{s2{p2}s1{p1}s3{p3}s4{p4}}");
    }

    public void testMergeA4() {
        assertConflictFreeMerge("R{a{b,c}d{e,f}}", "R{a{c,b}d{e,f}}",
            "R{a{b,c}g{d{e,f}}}", "R{a{c,b}g{d{e,f}}}");
    }

    public void testMergeA11() {
        assertConflictFreeMerge("R{a,b,c}", "R{b,a,c}", "R{a,b,c,i}",
            "R{b,a,c,i}");
    }

    public void testMergeA13() {
        assertConflictFreeMerge("R{p1,p2}", "R{p1,p2'}", "R{p1,a{p2}}",
            "R{p1,a{p2'}}");
    }

    public void testMergeA15() {
        assertConflictFreeMerge("R{a{d,e}b{f}}", "R{b{f}a{d,e}i}",
            "R{a{e,d}b'}", "R{b',a{e,d}i}");
    }

    public void testMergeA16() {
        assertConflictFreeMerge("R{a{b,m}c{cc}}", "R{a{b,m{i}}c{cc}}",
            "R{c{cc}m}", "R{c{cc}m{i}}");
    }

    public void testDuplicatedAdd() {
        assertConflictFreeMerge("R{a,b}", "R{a,b,c}", "R{a,b,c}", "R{a,b,c}");
    }

    public void testDuplicatedAddWithConflictingParent() {
        assertConflictingMerge("R{a,b}", "R{a,b,c}", "R{a,b{c}}", "R{a,b,c}",
            idList(), conflict("c", Type.Add, Type.Add));
        assertConflictingMerge("R{a,b}", "R{a,b{c}}", "R{a,b,c}", "R{a,b{c}}",
            idList(), conflict("c", Type.Add, Type.Add));
    }

    public void testDuplicatedAddWithContentChanges() {
        assertConflictFreeMerge("R{a,b}", "R{a,b,c}", "R{a,b,c'}", "R{a,b,c'}");
        assertConflictingMerge("R{a,b}", "R{a,b,c^}", "R{a,b,c'}", "R{a,b,c!!!}",
            idList(), conflict("c", Type.Edit, Type.Edit));
    }

    public void testMergeOkToDeleteReorderedNode() {
        assertConflictFreeMerge("R{a,b,c}", "R{a,c,b}", "R{a,c}", "R{a,c}");
        assertConflictFreeMerge("R{a{b,c,d}e}", "R{e,a{d,c,b}}", "R{e}", "R{e}");
    }

    public void testIncomingReverse() {
        assertConflictFreeMerge("R{a,b{c,d,e,f,g}}", "R{b{c,d,e,f,g}a}",
            "R{a,b{g,f,e,d,c}}", "R{b{g,f,e,d,c}a}");
    }

    public void testTreeInversion() {
        assertConflictFreeMerge("R{a{b{c{d{e}}}}}",
            "R{a'{a1,b'{b1,c'{c1,d'{d1,e'{e1}}}}}}", "R{e{d{c{b{a}}}}}",
            "R{e'{e1,d'{d1,c'{c1,b'{b1,a'{a1}}}}}}",
            "R{e'{d'{c'{b'{a'{a1}b1}c1}d1}e1}}");
    }

    public void testCrossingReorders() {
        assertConflictFreeMerge("R{a,b,c,d,e}", "R{b,c,a,d,e}", "R{e,a,b,c,d}",
            "R{e,b,c,a,d}");
    }

    public void testCompetingReorders() {
        assertConflictFreeMerge("R{a,b,c}", "R{b,a,c}", "R{a,c,b}", "R{a,c,b}",
            "R{c,b,a}");
        assertConflictFreeMerge("R{a,b,c,d,e}", "R{b,c,a,d,e}", "R{b,c,d,e,a}",
            "R{b,c,d,e,a}", "R{b,c,a,d,e}");
    }

    public void testDeepChanges() {
        assertConflictFreeMerge("R{a{b{c{d{e{f{g}}}}}}}", "R{g'}",
            "R{a{b{c{d{e{f{g}}}}}}h{i{j{k{l{m}}}}}}", "R{g',h{i{j{k{l{m}}}}}}");
    }

    public void testMergeX5() {
        assertConflictingMerge("R{a,b,c{d{f,g}e}}", "R{a,b,c{d{g,f}e'}}",
            "R{d{f,g}a,b}", "R{d{g,f}a,b,c{e'}}", idList("c", "e"), //
            conflict("e", Type.Edit, Type.Delete));
    }

    public void testMergeX6() {
        assertConflictingMerge("R{a{b{c{d}}}}", "R{a{c{b{d}}}}",
            "R{d{b{c{a}}}}", "R{a{c{b{d}}}}", idList(), //
            conflict("c", TreeMerger.Conflict.TreeCycle, "a", Type.Move),
            conflict("b", Type.Move, Type.Move), //
            conflict("d", Type.Move, Type.Move));
    }

    public void testMainTreeDeletedEditedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b}", "R{a,b,c'}", "R{a,b,c'}",
            idList("c"), conflict("c", Type.Delete, Type.Edit));
    }

    public void testMainTreeDeletedEditedNode2() {
        assertConflictingMerge("R{a{b{c{d}}}}", "R{a}", "R{a{b{c{d'}}}}",
            "R{a{b{c{d'}}}}", idList("b", "c", "d"), //
            conflict("d", Type.Delete, Type.Edit));
    }

    public void testMainTreeDeletedMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b}", "R{a,b{c}}", "R{a,b{c}}",
            idList("c"), conflict("c", Type.Delete, Type.Move));
    }

    public void testMainTreeDeletedParentOfMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,c}", "R{a,b{c}}", "R{a,b{c}}",
            idList("b"), //
            conflict("b", TreeMerger.Conflict.DeleteParent, "c", Type.Move));
    }

    public void testMainTreeDeletedParentAndMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a}", "R{a,b{c}}", "R{a,b{c}}",
            idList("b", "c"), conflict("c", Type.Delete, Type.Move));
    }

    public void testMainTreeDeletedParentOfAddedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,c}", "R{a,b{i}c}", "R{a,b{i}c}",
            idList("b"), //
            conflict("b", TreeMerger.Conflict.DeleteParent, "i", Type.Add));
    }

    public void testIncomingTreeDeletedEditedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b,c'}", "R{a,b}", "R{a,b,c'}",
            idList("c"), conflict("c", Type.Edit, Type.Delete));
    }

    public void testIncomingTreeDeletedMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b{c}}", "R{a,b}", "R{a,b{c}}",
            idList("c"), conflict("c", Type.Move, Type.Delete));
    }

    public void testIncomingTreeDeletedParentOfMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b{c}}", "R{a,c}", "R{a,b{c}}",
            idList("b"), //
            conflict("c", Type.Move, "b", TreeMerger.Conflict.DeleteParent));
    }

    public void testIncomingTreeDeletedParentAndMovedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b{c}}", "R{a}", "R{a,b{c}}",
            idList("b", "c"), conflict("c", Type.Move, Type.Delete));
    }

    public void testIncomingTreeDeletedParentOfAddedNode() {
        assertConflictingMerge("R{a,b,c}", "R{a,b{i}c}", "R{a,c}", "R{a,b{i}c}",
            idList("b"), //
            conflict("i", Type.Add, "b", TreeMerger.Conflict.DeleteParent));
    }


    private static void assertConflictFreeMerge(String specB, String specM,
            String specI, String expected) {
        assertConflictFreeMergeImpl(specB, specM, specI, expected);
        assertConflictFreeMergeImpl(specB, specI, specM, expected);
    }

    private static void assertConflictFreeMerge(String specB, String specM,
            String specI, String expected1, String expected2) {
        assertConflictFreeMergeImpl(specB, specM, specI, expected1);
        assertConflictFreeMergeImpl(specB, specI, specM, expected2);
    }

    private static void assertConflictFreeMergeImpl(String specB, String specM,
            String specI, String expected) {
        TreeMerger<String, String> merge = runMerge(specB, specM, specI);
        assertEquals(expected, merge.getMergedTree());
        assertTrue(merge.getMergeWarnings().isEmpty());
        assertTrue(merge.getMergedUndeletedNodeIDs().isEmpty());
    }

    private static void assertConflictingMerge(String specB, String specM,
            String specI, String expected, List<String> undeletedNodes,
            MergeWarning<String>... warnings) {
        TreeMerger<String, String> merge = runMerge(specB, specM, specI);
        assertEquals(expected, merge.getMergedTree());
        assertSetEquals(Arrays.asList(warnings), merge.getMergeWarnings());
        assertSetEquals(undeletedNodes, merge.getMergedUndeletedNodeIDs());
    }

    private static MergeWarning<String> conflict(String id, Type main,
            Type incoming) {
        return conflict(id, main, id, incoming);
    }

    private static MergeWarning<String> conflict(String mainID,
            Object mainType, String incomingID, Object incomingType) {
        return new MergeWarning<String>(Severity.CONFLICT, mainID,
                mainType, incomingID, incomingType);
    }

    private static TreeMerger<String, String> runMerge(String specB,
            String specM, String specI) {
        TreeNode<String, String> treeB = buildTree(specB);
        TreeNode<String, String> treeM = buildTree(specM);
        TreeNode<String, String> treeI = buildTree(specI);
        TreeMerger<String, String> result = new TreeMerger<String, String>(
                treeB, treeM, treeI, CONTENT_HANDLER);
        result.run();
        return result;
    }

    private static TreeDiff<String, String> runDiff(String specA, String specB) {
        TreeNode<String, String> treeA = buildTree(specA);
        TreeNode<String, String> treeB = buildTree(specB);
        return new TreeDiff<String, String>(treeA, treeB, CONTENT_HANDLER);
    }

    private static TreeNodeChange extractChange(List<TreeNodeChange> changes,
            String id, Type... type) {
        for (Iterator i = changes.iterator(); i.hasNext();) {
            TreeNodeChange c = (TreeNodeChange) i.next();
            if (!id.equals(c.getNodeID()))
                continue;
            if (type.length > 0 && type[0] != c.getType())
                continue;

            i.remove();
            return c;
        }
        fail("No change found for node " + id);
        return null;
    }

    private void assertContext(TreeNodeChange c, String parent,
            String... predecessors) {
        assertContext(c.getNode(), parent, predecessors);
    }

    private void assertContext(TreeNode n, String parent,
            String... predecessors) {
        assertEquals(parent, n.getParent().getID());
        assertEquals(idList(predecessors), n.getPredecessorIDs());
    }

    private static void assertSetEquals(Collection expected, Collection actual) {
        assertEquals(expected.size(), actual.size());
        for (Object e : expected)
            assertTrue("Set contains " + e, actual.contains(e));
    }

    private List<String> idList(String... ids) {
        return Arrays.asList(ids);
    }

    private static void assertEquals(String expectedSpec,
            TreeNode<String, String> actual) {
        try {
            assertEquals(buildTree(expectedSpec), actual);
        } catch (Error e) {
            fail("expected " + expectedSpec + ", actual tree was "
                    + actual.toDebugString());
        }
    }

    private static void assertEquals(TreeNode<String, String> expected,
            TreeNode<String, String> actual) {
        assertEquals(expected.getID(), actual.getID());
        assertEquals(expected.getContent(), actual.getContent());
        assertEquals(expected.getChildren().size(), actual.getChildren().size());
        for (int i = 0; i < expected.getChildren().size(); i++) {
            TreeNode<String, String> expectedCh = expected.getChildren().get(i);
            TreeNode<String, String> actualCh = actual.getChildren().get(i);
            assertEquals(expectedCh, actualCh);
        }
    }

    private static TreeNode<String, String> buildTree(String spec) {
        StringTokenizer tok = new StringTokenizer(spec, "{,}", true);
        TreeNode<String, String> root = buildNode(tok.nextToken());
        buildTree(root, tok);
        return root;
    }

    private static void buildTree(TreeNode<String, String> parent,
            StringTokenizer tok) {
        TreeNode<String, String> child = parent;
        while (tok.hasMoreTokens()) {
            String item = tok.nextToken();
            if ("{".equals(item))
                buildTree(child, tok);
            else if ("}".equals(item))
                return;
            else if (!",".equals(item))
                parent.addChild(child = buildNode(item));
        }
    }

    private static TreeNode<String, String> buildNode(String nodeSpec) {
        String id, content;
        Matcher m = NODE_PAT.matcher(nodeSpec);
        assertTrue(m.matches());
        id = m.group(1);
        content = m.group(2);
        return new TreeNode<String, String>(id, content);
    }

    private static final Pattern NODE_PAT = Pattern.compile("(\\w+)((\\W.*)?)");

    private static class ContentHandler implements
            ContentMerger<String, String> {

        public boolean isEqual(String a, String b) {
            return (a != null && a.equals(b));
        }

        public String mergeContent(TreeNode<String, String> destNode,
                String base, String main, String incoming,
                ErrorReporter<String> err) {
            if (base == null)
                base = "";
            if (isEqual(base, main))
                return incoming;
            else if (isEqual(base, incoming))
                return main;
            else if (isEqual(main, incoming))
                return main;

            err.addMergeWarning(new MergeWarning<String>(
                    Severity.CONFLICT, destNode.getID(), Type.Edit, Type.Edit));
            return "!!!";
        }

    }

    private static final ContentHandler CONTENT_HANDLER = new ContentHandler();
}
