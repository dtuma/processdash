// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.templates;

import junit.framework.TestCase;

public class DashPackageTest extends TestCase {

    private static final String[][] VERSION_DATA = {
        { "beta 5" },
        { "1.2.rc-4", "1- 2-rc(4)" },
        { "1.7a", "1-7 alpha" },
        { "1.7b", "1.7 beta" },
        { "1.7b.20060231" },
        { "1.7b.20060231.1" },
        { "1.7-rc.1", "1.7 RC(1)", "1.7-rc1", "1.7 RC1" },
        { "1.7-rc2" },
        { "1.7", " 1-07.0.0", "1.7+" },
        { " 1-07.0.0.1" },
        { "1-999" },
        { "2.0", "2.0" }
    };

    public void testCompareVersions() {
        for (int a = 0; a < VERSION_DATA.length; a++) {
            for (int i = 0; i < VERSION_DATA[a].length; i++) {
                String versionA = VERSION_DATA[a][i];
                testCompareVersions(versionA, a);
            }
        }
    }
    private void testCompareVersions(String versionA, int ordinalA) {
        for (int b = 0; b < VERSION_DATA.length; b++) {
            for (int i = 0; i < VERSION_DATA[b].length; i++) {
                String versionB = VERSION_DATA[b][i];
                int cmp = ordinalA - b;
                int result = DashPackage.compareVersions(versionA, versionB);
                if (cmp == 0)
                    assertEquals("expected '" + versionA + "' == '" + versionB
                            + "'", 0, result);
                else if (cmp < 0)
                    assertEquals("expected '" + versionA + "' < '" + versionB
                            + "'", -1, result);
                else
                    assertEquals("expected '" + versionA + "' > '" + versionB
                            + "'", 1, result);
            }
        }
    }

}
