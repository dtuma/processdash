// Copyright (C) 2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.clipboard;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.StringUtils;

public class HistoricalSelectionHelper {

    Map map;

    private String id;

    public HistoricalSelectionHelper(Map map, String id) {
        this.map = map;
        this.id = id;
    }

    /**
     * Consult the user settings, and load a list of column header names that
     * have been used for this selector during previous import operations.
     */
    public <T extends Collection> T loadHistoricalSelections(T dest) {
        String histCols = Settings.getVal(getHistSelectionSettingName());
        if (histCols != null && histCols.trim().length() > 0)
            dest.addAll(Arrays.asList(histCols.split("//")));
        return dest;
    }

    public <T extends Collection> void saveHistoricalSelections(T selections) {
        String settingVal = StringUtils.join(selections, "//");
        InternalSettings.set(getHistSelectionSettingName(), settingVal);
    }

    private String getHistSelectionSettingName() {
        return "defectImport.clipboard." + id + ".prevSelections";
    }

    public boolean hasHeaderRow() {
        return (map.get(ClipboardDataIDs.HAS_HEADER) == Boolean.TRUE);
    }

    public Set<String> getColumnNames(String prop) {
        List<TabularDataColumn> val = (List<TabularDataColumn>) map.get(prop);
        if (val == null)
            return Collections.EMPTY_SET;
        Set<String> result = new HashSet<String>();
        for (TabularDataColumn tdc : val)
            result.add(tdc.getName().toLowerCase());
        return result;
    }

}
