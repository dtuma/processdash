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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;

public class DatafileMergeHandler extends MapMerger<String, String>
        implements DashboardFileMergeHandler {

    public static final DatafileMergeHandler INSTANCE = new DatafileMergeHandler();

    private DatafileMergeHandler() {}

    @Override
    public void mergeFile(String filename, ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {

        Map parentData = loadDatafile(filename, parent);
        Map firstData = loadDatafile(filename, first);
        Map secondData = loadDatafile(filename, second);

        Map mergedData = new TreeMap();
        mergeMaps(parentData, firstData, secondData, mergedData);

        saveDatafile(filename, dest, mergedData);
    }

    private Map loadDatafile(String filename, ReadableResourceCollection src)
            throws IOException {
        if (src.getLastModified(filename) <= 0)
            return Collections.EMPTY_MAP;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(src.getInputStream(filename), ENCODING));
        StringBuilder nonMapLines = new StringBuilder();
        Map<String, String> dataElements = new HashMap<String, String>();
        String line;

        while ((line = in.readLine()) != null) {
            int eqPos = line.indexOf('=');
            if (line.startsWith("#") || eqPos < 1) {
                nonMapLines.append(line).append(LINE_SEPARATOR);
            } else {
                String key = line.substring(0, eqPos);
                String value = line.substring(eqPos + 1);
                dataElements.put(key, value);
            }
        }

        in.close();

        dataElements.put(NON_MAP_LINES_KEY, nonMapLines.toString());

        return dataElements;
    }

    private void saveDatafile(String filename, ResourceCollection dest,
            Map<String, String> dataElements) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                dest.getOutputStream(filename, 0), ENCODING));

        String nonMapLines = dataElements.remove(NON_MAP_LINES_KEY);
        out.write(nonMapLines);

        for (Entry<String, String> e : dataElements.entrySet()) {
            out.write(e.getKey());
            out.write("=");
            out.write(e.getValue());
            out.newLine();
        }

        out.close();
    }

    @Override
    protected String mergeConflictingChange(String key, String parent,
            String first, String second) {
        // simple resolution strategy for now: latest edit wins
        return second;
    }

    private static final String ENCODING = "UTF-8";

    private static final String NON_MAP_LINES_KEY = " == ";

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

}
