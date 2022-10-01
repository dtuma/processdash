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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.FileBackupManager;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.FileProperties;
import net.sourceforge.processdash.util.StringUtils;

public class PspdashIniMergeHandler extends MapMerger<String, String>
        implements DashboardFileMergeHandler {

    public static final PspdashIniMergeHandler INSTANCE = new PspdashIniMergeHandler();

    private PspdashIniMergeHandler() {}

    @Override
    public void mergeFile(String filename, ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {

        Map parentSettings = loadSettings(filename, parent);
        Map firstSettings = loadSettings(filename, first);
        Map secondSettings = loadSettings(filename, second);

        Map mergedSettings = new TreeMap();
        mergeMaps(parentSettings, firstSettings, secondSettings,
            mergedSettings);

        saveSettingsFile(filename, dest, mergedSettings);
    }

    private Map loadSettings(String filename, ReadableResourceCollection src)
            throws IOException {
        FileProperties result = InternalSettings.getSaveableProps(null);
        if (src.getLastModified(filename) > 0) {
            InputStream in = src.getInputStream(filename);
            result.load(in);
            in.close();
        }

        return result;
    }

    private void saveSettingsFile(String filename, ResourceCollection dest,
            Map<String, String> settings) throws IOException {
        FileProperties props = InternalSettings.getSaveableProps(null);
        props.putAll(settings);

        OutputStream out = dest.getOutputStream(filename, 0);
        props.store(out);
        out.close();
    }

    @Override
    protected String mergeConflictingChange(String key, String parent,
            String first, String second) {
        if (DataVersionChecker.SETTING_NAME.equals(key)) {
            // version requirements must never be lost. Merge all
            return DataVersionChecker.mergeRequirements(parent, first, second);

        } else if (FileBackupManager.EXTRA_DIRS_SETTING.equals(key)
                || DashController.TEMPLATE_PATH.equals(key)) {
            return mergeLists(parent, first, second, ";");

        } else {
            // simple resolution strategy for now: latest edit wins
            return second;
        }
    }

    private String mergeLists(String parent, String first, String second,
            String delim) {
        List<String> parentList = asList(parent, delim);
        List<String> firstList = asList(first, delim);
        List<String> secondList = asList(second, delim);
        Set<String> mergedList = SetMerger.INSTANCE.mergeSets(parentList,
            firstList, secondList);
        return StringUtils.join(mergedList, delim);
    }

    private List<String> asList(String value, String delim) {
        if (StringUtils.hasValue(value))
            return Arrays.asList(value.split(delim));
        else
            return Collections.EMPTY_LIST;
    }

}
