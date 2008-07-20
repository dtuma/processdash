// Copyright (C) 2003 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.FormatUtil;
import junit.framework.TestCase;


public class TestFormatUtil extends TestCase {

    public TestFormatUtil() {
        super();
    }

    public TestFormatUtil(String arg0) {
        super(arg0);
    }

    public void testPercentFormat() {
        NumberFormat f = NumberFormat.getPercentInstance();
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        assertEquals("12.7%", f.format(0.1265001));
    }

    public void testPercentages() throws Exception {
        assertEquals("1206%", FormatUtil.formatPercent(12.06));
        assertEquals("121%", FormatUtil.formatPercent(1.206));
        assertEquals("12.1%", FormatUtil.formatPercent(0.1206));
        assertEquals("1.2%", FormatUtil.formatPercent(0.0120));
        assertEquals("1.21%", FormatUtil.formatPercent(0.01206));
        assertEquals("99%", FormatUtil.formatPercent(0.99));
        assertEquals("99.9%", FormatUtil.formatPercent(0.9994));
//        assertEquals("100.0%", FormatUtil.formatPercent(0.9999));

        assertEquals("percentage", 0.99, FormatUtil.parsePercent(" 99   %"), 0.001);
        assertEquals("percentage", 3.3, FormatUtil.parsePercent("00330 "), 0.001);
    }

}
