// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;

import junit.framework.TestCase;

public class MainTest extends TestCase {

    public void testVersionInstantiate1() {
        Main.Version version = new Main.Version("");
        assertTrue(version.getMajor() == 0);
        assertTrue(version.getMinor() == 0);
    }

    public void testVersionInstantiate2() {
        Main.Version version = new Main.Version("1");
        assertTrue(version.getMajor() == 1);
        assertTrue(version.getMinor() == 0);
    }

    public void testVersionInstantiate3() {
        Main.Version version = new Main.Version("1111.2222.3333_35");
        assertTrue(version.getMajor() == 1111);
        assertTrue(version.getMinor() == 2222);
        assertTrue("1111.2222.3333_35".equals(version.toString()));
    }

    public void testVersionInstantiate4() {
        Main.Version version = new Main.Version(".2");
        assertTrue(version.getMajor() == 0);
        assertTrue(version.getMinor() == 2);
    }

    public void testVersionInstantiate5() {
        Main.Version version = new Main.Version(".");
        assertTrue(version.getMajor() == 0);
        assertTrue(version.getMinor() == 0);
    }

    public void testVersionInstantiate6() {
        Main.Version version = new Main.Version("2.");
        assertTrue(version.getMajor() == 2);
        assertTrue(version.getMinor() == 0);
    }

    public void testVersionCompareTo1() {
        Main.Version version = new Main.Version("1.2.3");
        Main.Version otherVersion = new Main.Version("1.3.0");
        assertTrue(version.compareTo(otherVersion) < 0);
    }

    public void testVersionCompareTo2() {
        Main.Version version = new Main.Version("1.2.3");
        Main.Version otherVersion = new Main.Version("1.2.3");
        assertTrue(version.compareTo(otherVersion) == 0);
    }

    public void testVersionCompareTo3() {
        Main.Version version = new Main.Version("1.6");
        Main.Version otherVersion = new Main.Version("1.4");
        assertTrue(version.compareTo(otherVersion) > 0);
    }

    public void testVersionCompareTo4() {
        Main.Version version = new Main.Version("1.2.3");
        Main.Version otherVersion = new Main.Version("2.0.0");
        assertTrue(version.compareTo(otherVersion) < 0);
    }

    public void testVersionCompareTo5() {
        Main.Version version = new Main.Version("2.1");
        Main.Version otherVersion = new Main.Version("2.1.3");
        assertTrue(version.compareTo(otherVersion) == 0);
    }

    public void testVersionCompareTo6() {
        Main.Version version = new Main.Version("2.6");
        Main.Version otherVersion = new Main.Version("1.4");
        assertTrue(version.compareTo(otherVersion) > 0);
    }
}
