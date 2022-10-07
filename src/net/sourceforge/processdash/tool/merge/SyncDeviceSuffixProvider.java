// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.merge;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.tool.bridge.bundle.DeviceID;

/**
 * When a dashboard stores its data in sync bundle mode, different devices must
 * make certain to use unique names for various files and identifiers. This
 * class chooses a unique suffix for each device.
 */
public class SyncDeviceSuffixProvider
        implements DashHierarchy.FilenameSuffixProvider {

    private String hierarchyFileSuffix;

    public String getHierarchyFileSuffix() {
        if (hierarchyFileSuffix == null)
            hierarchyFileSuffix = loadOrCreateHierFileSuffix();
        return hierarchyFileSuffix;
    }

    private String loadOrCreateHierFileSuffix() {
        String deviceID;
        try {
            deviceID = DeviceID.get();
        } catch (IOException e) {
            return null;
        }

        String settingName = SUFFIX_PROP_PREFIX + deviceID;
        String settingVal = Settings.getVal(settingName);
        if (settingVal != null)
            return settingVal;

        Set<String> inUse = getSuffixesInUse();
        String result = deviceID.substring(deviceID.length() - 3);
        while (inUse.contains(result)) {
            result = Integer.toString(100 + new Random().nextInt(900));
        }
        InternalSettings.set(settingName, result);

        return result;
    }

    private Set<String> getSuffixesInUse() {
        Set<String> result = new HashSet<String>();
        for (Object key : Settings.getSettings().keySet()) {
            String propName = (String) key;
            if (propName.startsWith(SUFFIX_PROP_PREFIX)) {
                result.add(Settings.getVal(propName));
            }
        }
        return result;
    }

    private static final String SUFFIX_PROP_PREFIX = "sync.hierFileSuffix.";

}
