// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

/**
 * Utility class for maintaining a list of most recently used objects, and
 * storing the data underneath a {@link java.util.prefs.Preferences} node.
 */
public class MRUPreferencesList {

    protected Preferences prefsBase;

    protected int maxEntries;

    protected List<String> recentList;


    public MRUPreferencesList(Preferences prefsBase) {
        this(prefsBase, 20);
    }

    public MRUPreferencesList(Preferences prefsBase, int maxEntries) {
        this.prefsBase = prefsBase;
        this.maxEntries = maxEntries;
        loadRecentList();
    }

    /**
     * @return a list of Preference nodes corresponding to entries that were
     *         stored in the past. The list will be ordered so the most recently
     *         used nodes are at the front, and the oldest are at the end.
     */
    public List<Preferences> getEntries() {
        return getEntries(Collections.EMPTY_MAP);
    }

    /**
     * @param matchAttrs
     *            a list of name/value attribute pairs.
     * @return a list of Preference nodes corresponding to entries that were
     *         stored in the past, which match the filters described by
     *         matchAttrs. The list will be ordered so the most recently used
     *         nodes are at the front, and the oldest are at the end. If no
     *         nodes match, the result will be empty.
     */
    public List<Preferences> getEntries(String... matchAttrs) {
        return getEntries(asMap(matchAttrs));
    }

    /**
     * @param matchAttrs
     *            a collection of name/value attributes.
     * @return a list of Preference nodes corresponding to entries that were
     *         stored in the past, which match the filters described by
     *         matchAttrs. The list will be ordered so the most recently used
     *         nodes are at the front, and the oldest are at the end. If no
     *         nodes match, the result will be empty.
     */
    public synchronized List<Preferences> getEntries(
            Map<String, String> matchAttrs) {
        List<Preferences> result = new ArrayList<Preferences>();
        boolean madeChange = false;
        for (Iterator<String> i = recentList.iterator(); i.hasNext();) {
            String oneKey = i.next();
            Preferences p = prefsBase.node(oneKey);
            if (!exists(p)) {
                i.remove();
                madeChange = true;
            } else if (matches(p, matchAttrs))
                result.add(p);
        }
        if (madeChange)
            saveRecentList();
        return result;
    }

    /**
     * Look up an entry in this list that matches the given attributes. If such
     * an entry exists, it will be moved to the front of the list. If no
     * matching entry exists, it will be created and inserted at the front of
     * the list.
     * 
     * @param matchAttrs
     *            a collection of name/value attributes.
     * @return a Preferences node representing this entry, where additional
     *         name/value attributes can be stored and retrieved.
     */
    public Preferences addEntry(String... matchAttrs) {
        return addEntry(asMap(matchAttrs));
    }

    /**
     * Look up an entry in this list that matches the given attributes. If such
     * an entry exists, it will be moved to the front of the list. If no
     * matching entry exists, it will be created and inserted at the front of
     * the list.
     * 
     * @param matchAttrs
     *            a collection of name/value attributes.
     * @return a Preferences node representing this entry, where additional
     *         name/value attributes can be stored and retrieved.
     */
    public synchronized Preferences addEntry(Map<String, String> matchAttrs) {
        Preferences result = findExistingNode(matchAttrs);
        if (result == null)
            result = createNewNode(matchAttrs);

        touch(result);

        return result;
    }


    private Preferences findExistingNode(Map<String, String> matchAttrs) {
        for (Iterator<String> i = recentList.iterator(); i.hasNext();) {
            String oneKey = i.next();
            Preferences oneNode = prefsBase.node(oneKey);
            if (!exists(oneNode))
                i.remove();
            else if (matches(oneNode, matchAttrs))
                return oneNode;
        }

        return null;
    }

    private Preferences createNewNode(Map<String, String> nodeAttrs) {
        String key = getUnusedKey();
        Preferences result = prefsBase.node(key);

        // if we are recycling a node from the past, delete and recreate it.
        if (exists(result)) {
            try {
                result.removeNode();
                result.flush();
                result = prefsBase.node(key);
            } catch (BackingStoreException bse) {
            }
        }

        for (Map.Entry<String, String> e : nodeAttrs.entrySet()) {
            String name = e.getKey();
            String val = e.getValue();
            if (val != null)
                result.put(name, val);
        }
        result.putLong(FIRST_USED, System.currentTimeMillis());

        flush(result);
        return result;
    }

    private String getUnusedKey() {
        int i = 1;
        while (true) {
            String result = Integer.toString(i);
            if (!recentList.contains(result))
                return result;
            i++;
        }
    }


    /** Retrieve the recent list from the backing store */
    protected void loadRecentList() {
        String listVal = prefsBase.get(LIST_KEY, "");
        String[] listItems = listVal.split(",");
        recentList = new ArrayList<String>(Arrays.asList(listItems));
    }

    /** Save the recent list to the backing store */
    protected void saveRecentList() {
        String listVal = StringUtils.join(recentList, ",");
        prefsBase.put(LIST_KEY, listVal);
        flush(prefsBase);
    }

    /**
     * @return true if this node appears to be a valid node created by this
     *         object
     */
    protected boolean exists(Preferences p) {
        return p.getLong(FIRST_USED, 0) > 0;
    }

    /**
     * @param p
     *            a preferences node created by this object
     * @param matchAttrs
     *            a set of key/value pairs. Null values are allowed.
     * @return true if the given preferences node has key value pairs that match
     *         the ones provided
     */
    protected boolean matches(Preferences p, Map<String, String> matchAttrs) {
        for (Map.Entry<String, String> e : matchAttrs.entrySet()) {
            String name = e.getKey();
            String expectedVal = e.getValue();
            String actualVal = p.get(name, null);
            if (NullSafeObjectUtils.EQ(expectedVal, actualVal) == false)
                return false;
        }
        return true;
    }

    /** Record the fact that this preferences object was just used */
    protected void touch(Preferences p) {
        // record the date when this node was used
        p.putLong(LAST_USED, System.currentTimeMillis());
        // record the number of times this node has been used
        int count = p.getInt(USE_COUNT, 0);
        p.putInt(USE_COUNT, count + 1);
        flush(p);

        // add this node's key to the front of our list
        String key = p.name();
        recentList.remove(key);
        recentList.add(0, key);

        // remove out-of-date nodes if necessary
        if (maxEntries > 0 && recentList.size() > maxEntries)
            recentList.subList(maxEntries, recentList.size()).clear();

        // save changes to our recent list
        saveRecentList();
    }

    private void flush(Preferences p) {
        try {
            p.flush();
        } catch (BackingStoreException bse) {
        }
    }

    private Map<String, String> asMap(String... values) {
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 1; i < values.length; i += 2)
            result.put(values[i - 1], values[i]);
        return result;
    }

    private static final String LIST_KEY = "list";

    private static final String FIRST_USED = "first-used";

    private static final String LAST_USED = "last-used";

    private static final String USE_COUNT = "use-count";

}
