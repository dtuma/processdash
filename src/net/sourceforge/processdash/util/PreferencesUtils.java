// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesUtils {

    /**
     * Store a character large object (CLOB) under the given key in the given
     * preference node.
     * 
     * @param prefs the preference node to store data under
     * @param key the key to associate data with
     * @param clob the CLOB value to store
     */
    public static void putCLOB(Preferences prefs, String key, String clob) {
        if (clob == null)
            throw new NullPointerException("CLOB value cannot be null");

        // Degenerate case: if the value to store is short enough to fit under
        // a single preference key, just store it normally.
        if (clob.length() < Preferences.MAX_VALUE_LENGTH && !isCheckVal(clob)) {
            removeCLOB(prefs, key);
            prefs.put(key, clob);
            return;
        }

        // First, write the checksum value.  Then, if anything below fails, the
        // worst case scenario is that we will be able to detect the corrupt
        // data and revert to a future default.
        prefs.put(key, getCheckVal(clob));

        // Create a child node specifically for "clob storage" for this key
        Preferences p = getClobStorage(prefs, key);
        try {
            p.clear();
        } catch (BackingStoreException bse) {
            // it would be nice to clear all data underneath the clob storage
            // node since it might reduce our resource utilization.  But even
            // if this fails, the code below ought to cover our bases.
        }

        // Split the CLOB into smaller pieces, and put them under our clob
        // storage node with the numerically ascending keys.
        int num = 0;
        while (true) {
            int len = clob.length();
            int pieceLen = Math.min(len, Preferences.MAX_VALUE_LENGTH - 1);
            p.put(Integer.toString(num++), clob.substring(0, pieceLen));
            if (len == pieceLen)
                break;
            clob = clob.substring(pieceLen);
        }

        // Delete the key that immediately follows our numerically ascending
        // keys.  This way, if the clear() call above failed, we will still
        // be able to detect the end of the stored data.
        p.remove(Integer.toString(num));
    }

    /**
     * Removes the character large object (CLOB) associated with the specified
     * key in the given preference node, if any.
     * 
     * @param prefs the preferences node where data is stored
     * @param key the key the CLOB was associated with
     */
    public static void removeCLOB(Preferences prefs, String key) {
        prefs.remove(key);
        try {
            getClobStorage(prefs, key).removeNode();
        } catch (BackingStoreException bse) {}
    }


    /** Returns the character large object (CLOB) associated with the specified
     * key in the given preference node.
     * 
     * This method can be safely called even if a non-CLOB value was stored for
     * the given key;  in that case, the non-CLOB value will be returned.
     * 
     * Returns the specified default if there is no value associated with the
     * key, if the backing store is inaccessible, or if a previously stored
     * CLOB has been corrupted by external writes to this preference node.
     * 
     * @param prefs the preference node to store data under
     * @param key the key whose value is to be returned
     * @param def the value to be returned in the event that this preference
     *    node has no value associated with <code>key</code>.
     */
    public static String getCLOB(Preferences prefs, String key, String def) {
        String checkVal = prefs.get(key, null);
        if (checkVal == null)
            return def;
        else if (!isCheckVal(checkVal))
            return checkVal;

        Preferences p = getClobStorage(prefs, key);
        StringBuffer buf = new StringBuffer();
        int num = 0;
        while (true) {
            String val = p.get(Integer.toString(num++), null);
            if (val == null)
                break;
            else
                buf.append(val);
        }

        String result = buf.toString();
        if (!checkVal.equals(getCheckVal(result)))
            return def;

        return result;
    }


    static final String CHECKSUM_PREFIX = "PrefsUtil$CLOBValue@";

    private static String getCheckVal(String val) {
        return CHECKSUM_PREFIX + val.hashCode();
    }

    private static boolean isCheckVal(String checkVal) {
        return checkVal.startsWith(CHECKSUM_PREFIX);
    }

    private static Preferences getClobStorage(Preferences prefs, String key) {
        return prefs.node(key + "_clob");
    }
}
