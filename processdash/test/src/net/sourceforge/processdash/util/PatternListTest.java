package net.sourceforge.processdash.util;

import java.util.regex.PatternSyntaxException;

import junit.framework.TestCase;

public class PatternListTest extends TestCase {

    public void testBreakDown() {
        PatternList p = new PatternList();
        p.addRegexp("^(foo )?(bar )?baz (qux|qwerty)$");
        assertTrue(p.matches("foo bar baz qux"));
        assertTrue(p.matches("foo bar baz qwerty"));
        assertTrue(p.matches("bar baz qux"));
        assertTrue(p.matches("bar baz qwerty"));
        assertTrue(p.matches("foo baz qux"));
        assertTrue(p.matches("foo baz qwerty"));
        assertTrue(p.matches("baz qux"));
        assertTrue(p.matches("baz qwerty"));
        assertFalse(p.matches("bar foo baz qwerty"));
        assertEquals(8, p.equalsItems.size());
        assertNull(p.regexpItems);
        assertNull(p.startsWithItems);
        assertNull(p.endsWithItems);
        assertNull(p.containsItems);

        p = new PatternList();
        p.addRegexp("foo|bar|baz");
        assertEquals(3, p.containsItems.size());
        assertTrue(p.matches("foo"));
        assertTrue(p.matches("bar"));
        assertTrue(p.matches("baz"));
    }

    public void testInvalidBreakdown() {
        PatternList p = new PatternList();
        try {
            p.addRegexp("foo(bar(baz)");
            fail("expected exception");
        } catch (PatternSyntaxException pse) {}
        try {
            p.addRegexp("foobar(baz))");
            fail("expected exception");
        } catch (PatternSyntaxException pse) {}
        p.addRegexp("foo?");
        assertEquals(1, p.regexpItems.size());
        assertTrue(p.matches("foo"));
        assertTrue(p.matches("fo"));
        assertFalse(p.matches("f"));
    }
}
