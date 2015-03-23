package net.sourceforge.processdash.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

public class HashTreeTest extends TestCase {

    private static final boolean SKIP_SLOW = false;

    private static final Object VALUE_2 = new Double(2);
    private static final String VALUE_1 = "special value";
    private static final String DATA_NAME_1 = "/Foo/Bar//Baz";
    private static final String DATA_NAME_2 = "/Foo/Bar//Qux";
    private static final String INIT_CTX = "/Foo/";
    private HashTree tree;

    public HashTreeTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        tree = new HashTree();
    }

    private void testCanon(String input, String output) {
        assertEquals(output, HashTree.canonicalizeKey(input));
    }
    public void testCanonicalize() {
        testCanon("..", "../");
        testCanon("../", "../");
        testCanon("a/..", "");
        testCanon("a/../", "");
        testCanon("a/b/../c/../..", "");
        testCanon("a/b/../c/../../", "");
        testCanon("../../../foo/bar/baz", "../../../foo/bar/baz");
        testCanon("../../../foo/bar/baz/", "../../../foo/bar/baz/");
        Random r = new Random();
        for (int iter = 300; iter-- > 0; ) {
            String a = "foo/";
            int count = r.nextInt(20);
            while (count-- > 0) {
                int len = r.nextInt(10);
                String b = "";
                while (len-- > 0)
                    b = NAMES[r.nextInt(5)] + "/" + b + "../";

                if (r.nextBoolean())
                    a =  b + a;
                else
                    a = a + b;
            }
            testCanon(a, "foo/");
            testCanon(a + "qwerty", "foo/qwerty");
            testCanon("../" + a, "../foo/");
            testCanon("../../" + a, "../../foo/");
            testCanon(a + "..", "");
            testCanon(a + "../", "");
        }

    }

    public void testSingleValue() {
        assertEquals(null, tree.get(DATA_NAME_1));

        tree.put(DATA_NAME_1, VALUE_1);
        assertEquals(VALUE_1, tree.get(DATA_NAME_1));

        tree.put(DATA_NAME_1, VALUE_2);
        assertEquals(VALUE_2, tree.get(DATA_NAME_1));
        assertEquals(VALUE_2, tree.get(DATA_NAME_1.substring(1)));

        tree.remove(DATA_NAME_1);
        assertEquals(null, tree.get(DATA_NAME_1));
    }

    public void testSubcontexts() {
        assertEquals(null, tree.get(DATA_NAME_1));

        tree.put(DATA_NAME_1, VALUE_1);

        HashTree t = (HashTree) tree.get(INIT_CTX);
        assertNotNull(t);
        assertNotSame(tree, t);
        assertSame(tree, t.get("../"));
        assertEquals(VALUE_1, t.get(DATA_NAME_1.substring(INIT_CTX.length())));
        assertEquals(VALUE_1, t.get(DATA_NAME_1));

        assertEquals(null, tree.get(DATA_NAME_2));
        String subName2 = DATA_NAME_2.substring(INIT_CTX.length());
        t.put(subName2, VALUE_2);
        assertEquals(VALUE_2, t.get(subName2));
        assertEquals(VALUE_2, t.get(DATA_NAME_2));
        assertNull(tree.get(subName2));
        assertEquals(VALUE_2, tree.get(DATA_NAME_2));
        assertEquals(VALUE_2, t.get(".." + DATA_NAME_2));
    }

    public void testLotsOfData() {
        if (SKIP_SLOW) return;

        HashSet allNames = new HashSet();
        fillHash(allNames, "", 1);
        assertFalse(allNames.isEmpty());
        /*
        System.out.println(
            "allNames contains " + allNames.size() + " elements.");
         */

        for (Iterator i = allNames.iterator(); i.hasNext();) {
            String name = (String) i.next();
            Object value = tree.get(name);
            assertNotNull(value);
            assertTrue(value instanceof Integer);
            assertEquals(name.hashCode(), ((Integer) value).intValue());
        }

        for (Iterator i = allNames.iterator(); i.hasNext();) {
            String name = (String) i.next();
            Object value = tree.remove(name);
            assertNull(tree.get(name));
            assertNotNull(value);
            assertTrue(value instanceof Integer);
            assertEquals(name.hashCode(), ((Integer) value).intValue());
        }
    }

    public void testGetAllKeys() {
        if (SKIP_SLOW) return;

        HashSet allNames = new HashSet();
        fillHash(allNames, "", 1);
        assertFalse(allNames.isEmpty());

        Iterator i = tree.getAllKeys();
        while (i.hasNext()) {
            String name = (String) i.next();
            assertTrue(allNames.contains(name));
            allNames.remove(name);
        }
        assertTrue(allNames.isEmpty());
    }

    public void testGetKeysEndingWith() {
        if (SKIP_SLOW) return;

        HashSet allNames = new HashSet();
        fillHash(allNames, "", 1);
        assertFalse(allNames.isEmpty());

        for (int i = 0; i < NAMES.length; i++) {
            String terminal = NAMES[i];
            String slashTerminal = "/" + terminal;
            Iterator it = tree.getKeysEndingWith(terminal);
            while (it.hasNext()) {
                String name = (String) it.next();
                assertTrue(allNames.contains(name));
                assertTrue(name.endsWith(slashTerminal));
                allNames.remove(name);
            }
        }
        assertTrue(allNames.isEmpty());

//        i = allNames.iterator();
//        while (i.hasNext()) {
//            String name = (String) i.next();
//            assertFalse(name.endsWith(slashTerminal));
//        }

        Iterator it = tree.getKeysEndingWith("booga booga");
        assertFalse(it.hasNext());
    }

    public void testGetDeepestExistingSubtree() {
        if (SKIP_SLOW) return;

        HashSet allNames = new HashSet();
        fillHash(allNames, "", 1);

        assertEquals(tree, tree.getDeepestExistingSubtree("/asdfas/asdfa/sadfa"));
        assertEquals(tree, tree.getDeepestExistingSubtree("asdfas/asdfa/sadfa"));
        assertEquals(tree, tree.getDeepestExistingSubtree("/qierqoiwerqoiweu"));
        assertEquals(tree, tree.getDeepestExistingSubtree("qierqoiwerqoiweu"));
        assertEquals(tree, tree.getDeepestExistingSubtree(NAMES[0]));

        for (Iterator i = allNames.iterator(); i.hasNext();) {
            String name = (String) i.next();
            int slashPos = name.lastIndexOf('/');
            String parentName = name.substring(0, slashPos + 1);
            HashTree parent = (HashTree) tree.get(parentName);
            assertNotNull(parent);
            assertEquals(parent, tree.getDeepestExistingSubtree(name));
            assertEquals(parent, tree.getDeepestExistingSubtree(name + "/SUPASDOIF/ASDFASP/ASDFASASFDAS"));
            assertEquals(parent, tree.getDeepestExistingSubtree(parentName + "/SUPASDOIF/ASDFASP/ASDFASASFDAS"));
        }
    }

    /** This fills the HashTree with an unbalanced tree containing over 50,000
     * elements.
     */
    private void fillHash(Set names, String prefix, int depth) {
        for (int i = 0; i < NAMES.length; i++) {
            String name = NAMES[i];
            String fullName = prefix + "/" + name;
            if (i < depth) {
                tree.put(fullName, new Integer(fullName.hashCode()));
                names.add(fullName);
            } else if (depth < 6) {
                fillHash(names, fullName, depth + 1);
            }
        }
    }
    private static final String[] NAMES =
        {
            "EXPORT_FILENAME",
            "f",
            "bar ",
            "baz",
            "Qux",
            "anon///ymous",
            "@!%",
            "Design/Time",
            "% Yield",
            };

}
