// Copyright (C) 2007-2008 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import junit.framework.TestCase;

public class ExternalLocationMapperTest extends TestCase {

    private static final String DATADIR1 = "/data/eqayjbq7";

    private static final String DRIVE1 = "r:/long/path" + DATADIR1;

    private static final String UNC1 = "//server/share" + DRIVE1.substring(2);

    private static final String[][] ABSOLUTE_REMAPPING_TESTS = {
    // various tests using drive letter mappings
            { DRIVE1, DRIVE1, "./foo1", "./foo1" },
            { DRIVE1, DRIVE1.toUpperCase(), "./foo2", "./foo2" },
            { DRIVE1.toUpperCase(), DRIVE1, "./foo3", "./foo3" },
            { DRIVE1 + "/bar", DRIVE1, "./foo4", "./foo4/bar" },
            { DRIVE1 + "/bar", DRIVE1.toUpperCase(), "./foo5", "./foo5/bar" },
            { DRIVE1.toUpperCase() + "/bar", DRIVE1, "./foo6", "./foo6/bar" },
            { DRIVE1 + "bar", DRIVE1, "./foo7", null },
            { DRIVE1 + "bar", DRIVE1.toUpperCase(), "./foo8", null },
            // the same tests with UNC paths
            { UNC1, UNC1, "./foo9", "./foo9" },
            { UNC1, UNC1.toUpperCase(), "./foo10", "./foo10" },
            { UNC1.toUpperCase(), UNC1, "./foo11", "./foo11" },
            { UNC1 + "/bar", UNC1, "./foo12", "./foo12/bar" },
            { UNC1 + "/bar", UNC1.toUpperCase(), "./foo13", "./foo13/bar" },
            { UNC1.toUpperCase() + "/bar", UNC1, "./foo14", "./foo14/bar" },
            { UNC1 + "bar", UNC1, "./foo15", null },
            { UNC1 + "bar", UNC1.toUpperCase(), "./foo16", null },
            // test that mismatched drive/UNC letters don't work for
            // remapFilename1
            { DRIVE1, UNC1, "./foo17", null },
            { DRIVE1, UNC1.toUpperCase(), "./foo18", null },
            { DRIVE1 + "/bar", UNC1, "./foo19", null },
            { DRIVE1 + "/bar", UNC1.toUpperCase(), "./foo20", null },
            { UNC1, DRIVE1, "./foo21", null },
            { UNC1, DRIVE1.toUpperCase(), "./foo22", null },
            { UNC1 + "/bar", DRIVE1, "./foo23", null },
            { UNC1 + "/bar", DRIVE1.toUpperCase(), "./foo24", null }, };

    public void testAbsoluteRemapping() {
        for (int i = 0; i < ABSOLUTE_REMAPPING_TESTS.length; i++) {
            String[] test = ABSOLUTE_REMAPPING_TESTS[i];
            assertEquals(test[3], ExternalLocationMapper
                    .performAbsoluteRemapping(test[0], test[1], test[2]));
        }
    }


    private static final String[][] GENERALIZED_REMAPPING_TESTS = {
            // various tests using exactly matching directories
            { DRIVE1, DATADIR1, "./foo1", "./foo1" },
            { DRIVE1, DATADIR1.toUpperCase(), "./foo2", "./foo2" },
            { DRIVE1.toUpperCase(), DATADIR1, "./foo3", "./foo3" },
            { UNC1, DATADIR1, "./foo4", "./foo4" },
            { UNC1, DATADIR1.toUpperCase(), "./foo5", "./foo5" },
            { UNC1.toUpperCase(), DATADIR1, "./foo6", "./foo6" },
            // the same tests with additional path info on the end
            { DRIVE1 + "/bar", DATADIR1, "./foo7", "./foo7/bar" },
            { DRIVE1 + "/bar", DATADIR1.toUpperCase(), "./foo8", "./foo8/bar" },
            { DRIVE1.toUpperCase() + "/bar", DATADIR1, "./foo9", "./foo9/bar" },
            { UNC1 + "/bar", DATADIR1, "./foo10", "./foo10/bar" },
            { UNC1 + "/bar", DATADIR1.toUpperCase(), "./foo11", "./foo11/bar" },
            { UNC1.toUpperCase() + "/bar", DATADIR1, "./foo12", "./foo12/bar" },
            // check for non-subdir matches
            { DRIVE1 + "bar", DATADIR1, "./foo13", null },
            { DRIVE1 + "bar", DATADIR1.toUpperCase(), "./foo14", null },
            { DRIVE1.toUpperCase() + "bar", DATADIR1, "./foo15", null },
            { UNC1 + "bar", DATADIR1, "./foo16", null },
            { UNC1 + "bar", DATADIR1.toUpperCase(), "./foo17", null },
            { UNC1.toUpperCase() + "bar", DATADIR1, "./foo18", null }, };

    public void testGeneralizedRemapping() {
        for (int i = 0; i < GENERALIZED_REMAPPING_TESTS.length; i++) {
            String[] test = GENERALIZED_REMAPPING_TESTS[i];
            assertEquals(test[3], ExternalLocationMapper
                    .performGeneralizedRemapping(test[0], test[1], test[2]));
        }
    }
}
