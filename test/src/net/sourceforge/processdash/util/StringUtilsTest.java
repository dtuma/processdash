package net.sourceforge.processdash.util;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public void testStartsWith() {
        testStartsWith(true, "foobar", "foo", 0);
        testStartsWith(true, "foobar", "foobar", 0);
        testStartsWith(false, "foobar", "foobaz", 0);
        testStartsWith(false, "foobar", "foobarbaz", 0);
        testStartsWith(true, "foobar", "o", 2);
        testStartsWith(true, "foobar", "obar", 2);
        testStartsWith(false, "foobar", "obaz", 2);
        testStartsWith(false, "foobar", "obarbaz", 2);
    }
    private void testStartsWith(boolean expected, String a, String b, int offset) {
        StringBuffer aa = new StringBuffer(a);
        StringBuffer bb = new StringBuffer(b);
        assertEquals(expected, StringUtils.startsWith(a, b, offset));
        assertEquals(expected, StringUtils.startsWith(aa, b, offset));
        assertEquals(expected, StringUtils.startsWith(a, bb, offset));
        assertEquals(expected, StringUtils.startsWith(aa, bb, offset));
    }
}
