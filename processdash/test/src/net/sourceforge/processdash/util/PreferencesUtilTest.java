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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.prefs.Preferences;

import junit.framework.TestCase;

public class PreferencesUtilTest extends TestCase {

    Preferences prefs;

    protected void setUp() throws Exception {
        super.setUp();
        prefs = Preferences.userNodeForPackage(PreferencesUtilTest.class);
    }

    protected void tearDown() throws Exception {
        prefs.removeNode();
        super.tearDown();
    }

    private String get(String key, String def) {
        return PreferencesUtils.getCLOB(prefs, key, def);
    }

    private void put(String key, String val) {
        PreferencesUtils.putCLOB(prefs, key, val);
    }

    private void remove(String key) {
        PreferencesUtils.removeCLOB(prefs, key);
    }

    public void testEmptyValues() {
        // with no values stored yet, we should be returned the default.
        assertEquals(null, get("empty", null));
        assertEquals("def", get("empty", "def"));

        // store an empty string, to assert that we can detect it as different
        // from the default value.
        put("empty", "");
        assertEquals("", get("empty", null));
        assertEquals("", get("empty", "def"));

        // remove the value and assert that we can tell it was removed.
        prefs.remove("empty");
        assertEquals(null, get("empty", null));
        assertEquals("def", get("empty", "def"));

        // store a real value now
        put("empty", "EMPTY");
        assertEquals("EMPTY", get("empty", null));
        assertEquals("EMPTY", get("empty", "def"));

        // officially remove the CLOB
        remove("empty");
        assertEquals(null, get("empty", null));
        assertEquals("def", get("empty", "def"));
    }

    public void testChecksumValue() {
        // Anal-retentively doublecheck that we can write values that look
        // like a PrefsUtil checksum.
        String val = PreferencesUtils.CHECKSUM_PREFIX + "foo";
        put("value", val);
        assertEquals(val, get("value", null));
    }

    public void testLongValues() {
        String value = "foo";
        for (int i = 0;  i < 15; i++) {
            value = value + value;
            if (i > 10) {
                put("value", value);
                assertEquals(value, get("value", null));
            }
        }
        remove("value");
        assertEquals(null, get("value", null));
    }

    public void testChecksum() {
        // build a long value to store.
        String value = "foo";
        for (int i = 0;  i < 13; i++)
            value = value + value;
        assertTrue(value.length() > Preferences.MAX_VALUE_LENGTH);

        // store the long value, make certain it made it OK.
        put("value", value);
        assertEquals(value, get("value", "default"));

        // write some other value into the same place, make certain we can
        // retrieve it.
        prefs.put("value", "foo");
        assertEquals("foo", get("value", "default"));

        // write the long value back again, make certain it made it OK.
        put("value", value);
        assertEquals(value, get("value", "default"));

        // corrupt part of the long value, make certain we don't get garbage
        // back
        Preferences node = prefs.node("value_clob");
        node.put("1", "foo");
        assertEquals("default", get("value", "default"));

        // write the long value back again, make certain it made it OK.
        put("value", value);
        assertEquals(value, get("value", "default"));

        // delete the value
        remove("value");
        assertEquals(null, get("value", null));
    }
}
