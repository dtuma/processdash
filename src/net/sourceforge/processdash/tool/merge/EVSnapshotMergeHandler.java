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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.CRC32;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;

public class EVSnapshotMergeHandler
        extends MapMerger<String, EVSnapshotMergeHandler.Value>
        implements DashboardFileMergeHandler {

    public static final EVSnapshotMergeHandler INSTANCE = new EVSnapshotMergeHandler();

    private EVSnapshotMergeHandler() {}

    @Override
    public void mergeFile(String filename, ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {

        Map parentData = loadDatafile(filename, parent);
        Map firstData = loadDatafile(filename, first);
        Map secondData = loadDatafile(filename, second);

        Map mergedData = new TreeMap();
        mergeMaps(parentData, firstData, secondData, mergedData);

        saveDatafile(filename, parent, first, second, dest, mergedData);
    }

    private Map<String, Value> loadDatafile(String filename,
            ReadableResourceCollection src) throws IOException {
        if (src.getLastModified(filename) <= 0)
            return Collections.EMPTY_MAP;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(src.getInputStream(filename), ENCODING));
        StringBuilder nonMapLines = new StringBuilder();
        Map<String, Value> dataElements = new HashMap<String, Value>();
        String line;

        while ((line = in.readLine()) != null) {
            int eqPos = line.indexOf('=');
            if (line.startsWith("#") || eqPos < 1) {
                nonMapLines.append(line).append(System.lineSeparator());
            } else {
                String key = line.substring(0, eqPos);
                String value = line.substring(eqPos + 1);
                dataElements.put(key, new Value(src, value, false));
            }
        }

        in.close();

        dataElements.put(NON_MAP_LINES_KEY,
            new Value(src, nonMapLines.toString(), true));

        return dataElements;
    }

    private void saveDatafile(String filename,
            ReadableResourceCollection parent, ReadableResourceCollection first,
            ReadableResourceCollection second, ResourceCollection dest,
            Map<String, Value> dataElements) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                dest.getOutputStream(filename, 0), ENCODING));

        Value nonMapLines = dataElements.remove(NON_MAP_LINES_KEY);
        if (nonMapLines != null)
            out.write(nonMapLines.savedVal);

        copyDataValues(out, dataElements, filename, second);
        copyDataValues(out, dataElements, filename, first);
        copyDataValues(out, dataElements, filename, parent);

        out.close();
    }

    private void copyDataValues(BufferedWriter out,
            Map<String, Value> dataElements, String filename,
            ReadableResourceCollection src) throws IOException {
        Set<String> keysToCopy = new HashSet<String>();
        for (Entry<String, Value> e : dataElements.entrySet()) {
            if (e.getValue().source == src)
                keysToCopy.add(e.getKey());
        }

        if (keysToCopy.isEmpty())
            return;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(src.getInputStream(filename), ENCODING));
        String line;

        while ((line = in.readLine()) != null) {
            int eqPos = line.indexOf('=');
            if (eqPos > 0) {
                String key = line.substring(0, eqPos);
                if (keysToCopy.contains(key)) {
                    out.write(line);
                    out.newLine();
                }
            }
        }

        in.close();
    }

    @Override
    protected Value mergeConflictingChange(String key, Value parent,
            Value first, Value second) {
        // simple resolution strategy for now: latest edit wins
        return second;
    }


    private static final String ENCODING = "UTF-8";

    private static final String NON_MAP_LINES_KEY = " == ";


    public static class Value {

        private ReadableResourceCollection source;

        private long checksum;

        private String savedVal;

        private Value(ReadableResourceCollection source, String value,
                boolean saveValue) throws IOException {
            this.source = source;
            CRC32 ck = new CRC32();
            ck.update(value.getBytes(ENCODING));
            this.checksum = ck.getValue();
            if (saveValue)
                this.savedVal = value;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Value && ((Value) obj).checksum == checksum);
        }

        public int hashCode() {
            return Long.hashCode(checksum);
        }

    }

}
