// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import junit.framework.TestCase;

public class TimeNumberFormatTest extends TestCase {

    private TimeNumberFormat fmt;

    protected void setUp() throws Exception {
        super.setUp();
        fmt = new TimeNumberFormat();
    }

    private static String[][] EXPECTED_FORMATTING = {
        { "0:01", "1" },
        { "0:59", "59" },
        { "1:39", "99" },
        { "4:59", "299" },
        { "5:00", "300" },
        { "5:01", "301" },
    };

    public void testFormatting() {
        testFormatting(fmt);
    }
    private void testFormatting(NumberFormat format) {
        for (int i = 0; i < EXPECTED_FORMATTING.length; i++) {
            String[] oneTest = EXPECTED_FORMATTING[i];
            long time = Long.parseLong(oneTest[1]);
            assertEquals(oneTest[0], format.format(time));
            assertEquals("-" + oneTest[0], format.format(-time));
        }
    }

    private static String[][] UNUSUAL_PARSING = {
        { " \t2: ", "120" },
        { "  2: a", "120", "5" },
        { "5:", "300" },
        { ":5", "5" },
        { "5 ", "5", "1" },
        { "-5", "-5" },
        { " 120q", "120", "4" },
        { " :642", "642" },
        { ":" },
        { "- : " },
        { "-0: 99", "-99" },
        { "  -:0042 ", "-42", "8" },
    };

    public void testParsing() throws Exception {
        for (int i = 0; i < EXPECTED_FORMATTING.length; i++) {
            String[] oneTest = EXPECTED_FORMATTING[i];
            long time = Long.parseLong(oneTest[1]);
            assertParse(time, oneTest[0]);
            assertParse(time, oneTest[0] + "foo", oneTest[0].length());
            assertParse(-time, "-" + oneTest[0]);
            assertParseFailure("foo" + oneTest[0]);
        }
        for (int i = 0; i < UNUSUAL_PARSING.length; i++) {
            String[] oneTest = UNUSUAL_PARSING[i];
            if (oneTest.length == 1)
                assertParseFailure(oneTest[0]);
            else if (oneTest.length == 2)
                assertParse(Long.parseLong(oneTest[1]), oneTest[0]);
            else
                assertParse(Long.parseLong(oneTest[1]), oneTest[0], Integer.parseInt(oneTest[2]));
        }
    }
    private void assertParse(long value, String s) throws Exception {
        assertParse(value, s, s.length());
    }
    private void assertParse(long value, String s, int finalPos) throws Exception {
        ParsePosition pos = new ParsePosition(0);
        assertEquals((double) value, fmt.parse(s, pos).doubleValue(), 0.0);
        assertEquals(finalPos, pos.getIndex());
    }
    private void assertParseFailure(String s) {
        try {
            fmt.parse(s);
            fail("Expected parse exception");
        } catch (ParseException pe) {}
    }

    public void testBoundaries() throws Exception {
        AdaptiveNumberFormat format = new AdaptiveNumberFormat(fmt, 1);
        testFormatting(format);
        assertEquals(AdaptiveNumberFormat.INF_STRING,
                format.format(Double.POSITIVE_INFINITY));
        assertEquals(AdaptiveNumberFormat.INF_STRING,
                format.format(Double.NEGATIVE_INFINITY));
        assertEquals(AdaptiveNumberFormat.NAN_STRING,
                format.format(Double.NaN));
        assertTrue(Double.isNaN(
                format.parse(AdaptiveNumberFormat.NAN_STRING).doubleValue()));
        assertTrue(Double.isInfinite(
                format.parse(AdaptiveNumberFormat.INF_STRING).doubleValue()));
    }
}
