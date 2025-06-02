// Copyright (C) 2021-2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import net.sourceforge.processdash.util.SortedProperties;

/**
 * An implementation of HeadRefs that stores bundle references in a
 * java.util.Properties object.
 */
public class HeadRefsProperties implements HeadRefs {

    protected SortedProperties props;

    public HeadRefsProperties() {
        this.props = new SortedProperties();
    }

    /**
     * Make sure in-memory data is up-to-date with any external storage
     * 
     * @throws IOException
     */
    protected void update() throws IOException {}

    /**
     * Update external storage with any changes to in-memory data
     * 
     * @throws IOException
     */
    protected void flush() throws IOException {}


    /**
     * Get the ID of the HEAD ref for the bundle with a given name.
     */
    public synchronized FileBundleID getHeadRef(String bundleName)
            throws IOException {
        update();
        String prop = bundleName + REF_PROP;
        String token = props.getProperty(prop);
        return (token == null ? null : new FileBundleID(token));
    }


    /**
     * Get a Map whose keys are the names of bundles tracked by this file, and
     * whose values are the HEAD refs for each.
     */
    public synchronized Map<String, FileBundleID> getHeadRefs()
            throws IOException {
        update();
        Map<String, FileBundleID> result = new HashMap<String, FileBundleID>();
        for (Entry<Object, Object> e : props.entrySet()) {
            String prop = (String) e.getKey();
            if (prop.endsWith(REF_PROP)) {
                try {
                    String bundleName = prop.substring(0,
                        prop.length() - REF_PROP.length());
                    String token = (String) e.getValue();
                    result.put(bundleName, new FileBundleID(token));
                } catch (IllegalArgumentException iae) {
                }
            }
        }
        return result;
    }


    /**
     * Store a new HEAD ref for a given bundle.
     */
    public void storeHeadRef(FileBundleID bundleID) throws IOException {
        storeHeadRefs(Collections.singleton(bundleID));
    }


    /**
     * Store a set of new HEAD ref for a list of bundles.
     */
    public synchronized void storeHeadRefs(Collection<FileBundleID> headRefs)
            throws IOException {
        update();
        String now = Long.toString(System.currentTimeMillis());
        for (FileBundleID bundleID : headRefs) {
            String bundleName = bundleID.getBundleName();
            props.setProperty(bundleName + REF_PROP, bundleID.getToken());
            props.setProperty(bundleName + MOD_PROP, now);
        }
        flush();
    }


    /**
     * Delete the HEAD ref for a given bundle
     */
    public synchronized void deleteHeadRef(String bundleName)
            throws IOException {
        update();
        String now = Long.toString(System.currentTimeMillis());
        props.remove(bundleName + REF_PROP);
        props.setProperty(bundleName + MOD_PROP, now);
        flush();
    }


    /**
     * Return the newest head ref modification time within a set of properties.
     */
    protected static long getRefModTimestamp(Properties p) {
        long result = -1;
        for (String key : p.stringPropertyNames()) {
            if (key.endsWith(MOD_PROP)) {
                try {
                    String value = p.getProperty(key);
                    result = Math.max(result, Long.valueOf(value));
                } catch (NumberFormatException nfe) {
                }
            }
        }
        return result;
    }


    private static final String REF_PROP = ".ref";

    private static final String MOD_PROP = ".mod";

}
