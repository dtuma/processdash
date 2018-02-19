// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import net.sourceforge.processdash.util.StringUtils;

public class SyncMetadata extends Properties {

    public static final String DELETE_METADATA = " -- delete -- ";

    private boolean changed;

    public SyncMetadata() {
        this.changed = false;
    }

    public boolean isChanged() {
        return changed;
    }

    public void clearChanged() {
        changed = false;
    }

    public Double getNum(Double defaultValue, String... keyParts) {
        String value = getStr(keyParts);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    public void setNum(Double newValue, String... keyParts) {
        String value = newValue == null ? null : Double.toString(newValue);
        setStr(value, keyParts);
    }

    public String getStr(String... keyParts) {
        return getProperty(getAttrName(keyParts));
    }

    public void setStr(String newValue, String... keyParts) {
        String attrName = getAttrName(keyParts);
        setStrImpl(attrName, newValue);
    }

    private void setStrImpl(String attrName, String newValue) {
        String oldValue = getProperty(attrName);
        if (newValue == null) {
            if (oldValue != null) {
                remove(attrName);
                changed = true;
            }
        } else if (!newValue.equals(oldValue)) {
            setProperty(attrName, newValue);
            changed = true;
        }
    }

    public void discardAttrs(String... initialKeyParts) {
        String prefix = getAttrName(initialKeyParts) + ".";
        for (Iterator i = keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (key.startsWith(prefix)) {
                i.remove();
                changed = true;
            }
        }
    }

    private String getAttrName(String... keyParts) {
        return StringUtils.join(Arrays.asList(keyParts), ".");
    }

    public void applyChanges(SyncMetadata changes) {
        if (changes != null) {
            for (Entry<Object, Object> e : changes.entrySet()) {
                String attrName = (String) e.getKey();
                String newValue = (String) e.getValue();
                if (DELETE_METADATA.equals(newValue))
                    newValue = null;
                setStrImpl(attrName, newValue);
            }
        }
    }

    public synchronized Enumeration keys() {
        // return the list of keys in sorted order. This will cause the logic
        // in the Properties.store method to store items in sorted order.
        // (This is something of a hack, since we have no guarantee that
        // Properties.store() will really call this method...but at least
        // the Oracle implementation currently does.)
        ArrayList l = Collections.list(super.keys());
        Collections.sort(l);
        return Collections.enumeration(l);
    }

}
