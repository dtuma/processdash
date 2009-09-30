// Copyright (C) 2003-2009 Tuma Solutions, LLC
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

import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @since 1.11.0.1
 */
public class VersionUtils {

    /**
     * Compare two version strings.
     * 
     * @return +1 if version1 is greater than version 2; -1 if version1 is less
     *         than version2; and 0 if the two version numbers are the same.
     *
     */
    public static int compareVersions(String version1, String version2) {
        if (version1.equals(version2)) return 0;

        List v1 = getVersionComponents(version1);
        List v2 = getVersionComponents(version2);

        while (true) {
            if (v1.isEmpty() && v2.isEmpty()) return 0;

            double result = vNum(v1) - vNum(v2);
            if (result > 0) return 1;
            if (result < 0) return -1;
        }
    }
    private static List getVersionComponents(String version) {
        Stack result = new Stack();
        Matcher m = VERSION_COMPONENT.matcher(version);
        while (m.find()) {
            if (m.group(1) != null)
                result.push(new Double(m.group(1)));
            else {
                double lastDigit = 0;
                if (!result.isEmpty())
                    lastDigit = ((Double) result.pop()).doubleValue();
                if (m.group(2) != null)
                    lastDigit = lastDigit - 0.3;
                else if (m.group(4) != null)
                    lastDigit = lastDigit - 0.2;
                else if (m.group(6) != null)
                    lastDigit = lastDigit - 0.1;
                result.push(new Double(lastDigit));
            }
        }
        return result;
    }
    private static final Pattern VERSION_COMPONENT = Pattern.compile(
            "(\\d+)|(a(lpha)?)|(b(eta)?)|(rc)",
            Pattern.CASE_INSENSITIVE);

    private static double vNum(List components) {
        if (components.isEmpty()) return 0;
        Double d = (Double) components.remove(0);
        return d.doubleValue();
    }

}
