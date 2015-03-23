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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class FallbackObjectFactoryTest extends TestCase {

    public void testNPE() {
        try {
            new FallbackObjectFactory<Object>(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // correct behavior
        }
    }

    public void testCreation() {
        Object o = new FallbackObjectFactory<List>(List.class)//
                .add("no.such.class") //
                .add("java.lang.String") //
                .add("java.util.ArrayList") //
                .add("java.lang.Integer") //
                .add("java.util.LinkedList") //
                .get();
        assertTrue(o instanceof ArrayList);
    }

    public void testCreationDefaultingPackage() {
        Object o = new FallbackObjectFactory<List>(List.class)//
                .add("ArrayList") //
                .get();
        assertTrue(o instanceof ArrayList);
    }

    public void testNotInterface() {
        Object o = new FallbackObjectFactory<String>(String.class)
                .add("java.lang.String").get();
        assertNotNull(o);
        assertEquals("", o);

        o = new FallbackObjectFactory<String>(String.class).get();
        assertNull(o);
    }

    public void testNoOp() {
        Object o = new FallbackObjectFactory<TestInterface>(
                TestInterface.class).get();
        assertNotNull(o);
        assertTrue(o instanceof TestInterface);
        TestInterface ti = (TestInterface) o;

        ti.voidMethod();
        assertNull(ti.returnsObject());
        assertNull(ti.returnsObject2());
        assertNull(ti.returnsObject3());
        assertNull(ti.returnsObject4());
        assertEquals(false, ti.returnsBool());
        assertEquals((byte) 0, ti.returnsByte());
        assertEquals((char) 0, ti.returnsChar());
        assertEquals((double) 0, ti.returnsDouble());
        assertEquals((float) 0, ti.returnsFloat());
        assertEquals((int) 0, ti.returnsInt());
        assertEquals((long) 0, ti.returnsLong());
        assertEquals((short) 0, ti.returnsShort());
    }

    public void testNoAutoNoop() {
        Object o = new FallbackObjectFactory<TestInterface>(
                TestInterface.class).get(false);
        assertNull(o);
    }


    public interface TestInterface {

        public void voidMethod();

        public Object returnsObject();

        public String returnsObject2();

        public Double returnsObject3();

        public Boolean returnsObject4();

        public boolean returnsBool();

        public byte returnsByte();

        public char returnsChar();

        public double returnsDouble();

        public float returnsFloat();

        public int returnsInt();

        public long returnsLong();

        public short returnsShort();

    }
}
