// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum FileBundleMode {

    Local("local", //
            "pspdash", "2.6.6", //
            "teamToolsB", "6.2.0", //
            "tpidw-embedded", "1.6.7");


    private String modeName;

    private Map<String, String> minVersions;

    private FileBundleMode(String modeName, String... versionData) {
        this.modeName = modeName;

        Map<String, String> versions = new LinkedHashMap<String, String>();
        for (int i = 0; i < versionData.length; i += 2)
            versions.put(versionData[i], versionData[i + 1]);
        this.minVersions = Collections.unmodifiableMap(versions);
    }

    public String getName() {
        return modeName;
    }

    public Map<String, String> getMinVersions() {
        return minVersions;
    }

}
