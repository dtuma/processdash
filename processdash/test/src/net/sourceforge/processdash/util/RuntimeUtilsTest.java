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

import junit.framework.TestCase;

public class RuntimeUtilsTest extends TestCase {

    public void testAssertMethod() {
        // test regular method
        RuntimeUtils.assertMethod(String.class, "concat");
        // test method of superclass
        RuntimeUtils.assertMethod(String.class, "toString");
        // test static method
        RuntimeUtils.assertMethod(String.class, "copyValueOf");

        try {
            // test missing method behavior
            RuntimeUtils.assertMethod(String.class, "foobar");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {
            // expected behavior
            assertEquals(
                "Class java.lang.String does not support method foobar",
                uoe.getMessage());
        }
    }

}
