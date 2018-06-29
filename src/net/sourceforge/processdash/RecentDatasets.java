// Copyright (C) 2012-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.util.prefs.Preferences;

import net.sourceforge.processdash.ui.lib.MRUPreferencesList;


public class RecentDatasets {

    public static final String LOCATION = "location";

    public static final String LOC_TYPE = "location-type";

    public static final String LOC_TYPE_FILE = "file";

    public static final String LOC_TYPE_HTTP = "http";

    public static final String DATASET_TYPE = "dataset-type";

    public static final String DATASET_TYPE_TEAM = "team";

    public static final String DATASET_TYPE_PERSONAL = "personal";

    /** @since 2.4.3 */
    public static final String DATASET_NAME = "dataset-name";

    /** @since 2.4.3 */
    public static final String DATASET_OWNER = "dataset-owner";

    public static final String TIME_USED = "minutes-used";


    static Preferences register(String location, boolean isTeam) {
        Preferences p = list().addEntry(LOCATION, location);

        String locationType = (location.toLowerCase().startsWith("http")
                ? LOC_TYPE_HTTP : LOC_TYPE_FILE);
        p.put(LOC_TYPE, locationType);

        String datasetType = isTeam ? DATASET_TYPE_TEAM : DATASET_TYPE_PERSONAL;
        p.put(DATASET_TYPE, datasetType);

        return p;
    }

    static void elapsed(Preferences p, int minutes) {
        if (p != null) {
            int pastTime = p.getInt(TIME_USED, 0);
            p.putInt(TIME_USED, pastTime + minutes);
        }
    }


    public static MRUPreferencesList list() {
        Preferences p = Preferences.userNodeForPackage(RecentDatasets.class)
                .node("recentDatasets");
        return new MRUPreferencesList(p, 100);
    }

}
