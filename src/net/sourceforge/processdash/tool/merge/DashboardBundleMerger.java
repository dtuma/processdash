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

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMerger;
import net.sourceforge.processdash.util.FileUtils;

public class DashboardBundleMerger
        extends MapMerger<String, DashboardBundleMerger.FileState>
        implements BundleMerger {

    private static final Logger logger = DashboardFileMergeHandler.logger;

    private List<String> mergedFiles;

    protected DashboardBundleMerger() {
        mergedFiles = new ArrayList<String>();
    }

    public List<String> getAndClearMergedFiles() {
        try {
            return mergedFiles;
        } finally {
            mergedFiles = new ArrayList<String>();
        }
    }

    public void mergeBundle(ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {

        Map<String, FileState> parentFiles = getFiles(parent);
        Map<String, FileState> firstFiles = getFiles(first);
        Map<String, FileState> secondFiles = getFiles(second);

        Map<String, FileState> mergedFiles = new TreeMap();
        mergeMaps(parentFiles, firstFiles, secondFiles, mergedFiles);

        saveFiles(parent, first, second, dest, mergedFiles);
    }

    private Map<String, FileState> getFiles(ReadableResourceCollection dir) {
        Map<String, FileState> result = new HashMap();
        for (String filename : dir.listResourceNames())
            result.put(filename, new FileState(dir, filename));
        return result;
    }

    @Override
    protected FileState mergeValue(String key, FileState parent,
            FileState first, FileState second) {
        // files don't normally get deleted from a dashboard directory. If one
        // of our branches has lost a file, use the file from the other one.
        if (first == null)
            return second;
        else if (second == null)
            return first;
        else
            return super.mergeValue(key, parent, first, second);
    }

    @Override
    protected FileState mergeConflictingChange(String filename,
            FileState parent, FileState first, FileState second) {
        // record this file as one that required a merge
        mergedFiles.add(filename);

        // look for a merge handler for this file. If found, return it
        String filenameLC = filename.toLowerCase();
        DashboardFileMergeHandler handler = getMergeHandler(filenameLC);
        if (handler != null)
            return new FileState(handler);

        // if no merge handler was found, this file uses an overwrite strategy.
        // just keep the newest version of the file.
        logger.warning(
            "Overlapping edits to file " + filename + ", keeping newest");
        return second;
    }

    protected DashboardFileMergeHandler getMergeHandler(String filenameLC) {
        if ("pspdash.ini".equals(filenameLC))
            return PspdashIniMergeHandler.INSTANCE;

        if (filenameLC.endsWith(".dat")) {
            if (filenameLC.startsWith("ev-"))
                return EVSnapshotMergeHandler.INSTANCE;

            if ("global.dat".equals(filenameLC)
                    || Character.isDigit(filenameLC.charAt(0)))
                return DatafileMergeHandler.INSTANCE;
        }

        return null;
    }

    private void saveFiles(ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest, Map<String, FileState> mergedFiles)
            throws IOException {

        for (Entry<String, FileState> e : mergedFiles.entrySet()) {
            String filename = e.getKey();
            FileState state = e.getValue();

            if (state.source != null) {
                // copy the file from state.source to dest
                long modTime = state.source.getLastModified(filename);
                InputStream in = state.source.getInputStream(filename);
                OutputStream out = dest.getOutputStream(filename, modTime);
                FileUtils.copyFile(in, out);
                in.close();
                out.close();

            } else if (state.conflictHandler != null) {
                logger.warning("Overlapping edits to file " + filename
                        + ", attempting merge");
                state.conflictHandler.mergeFile(filename, parent, first, second,
                    dest);
            }
        }
    }



    public static class FileState {

        private ReadableResourceCollection source;

        private Long checksum;

        private DashboardFileMergeHandler conflictHandler;

        private FileState(ReadableResourceCollection source, String filename) {
            this.source = source;
            this.checksum = source.getChecksum(filename);
        }

        private FileState(DashboardFileMergeHandler conflictHandler) {
            this.conflictHandler = conflictHandler;
        }

        public boolean equals(Object obj) {
            return (obj instanceof FileState
                    && EQ(((FileState) obj).checksum, this.checksum));
        }

        public int hashCode() {
            return checksum == null ? 0 : checksum.hashCode();
        }

    }

}

