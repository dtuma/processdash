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

package teamdash.wbs;

import static teamdash.wbs.WBSFilenameConstants.DATA_DUMP_FILE;
import static teamdash.wbs.WBSFilenameConstants.CHANGE_HISTORY_FILE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMerger;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.util.FileUtils;

import teamdash.merge.ui.MergeConflictNotification;

public class TeamProjectBundleMerger implements BundleMerger {

    private WorkingDirectory workingDir;

    List<MergeConflictNotification> conflicts;

    public TeamProjectBundleMerger(WorkingDirectory dir) {
        this.workingDir = dir;
        this.conflicts = new ArrayList<MergeConflictNotification>();
    }

    public List<MergeConflictNotification> getAndClearConflicts() {
        try {
            return conflicts;
        } finally {
            conflicts = new ArrayList<MergeConflictNotification>();
        }
    }

    @Override
    public void mergeBundle(ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {
        // load team projects for each bundle. All the files we need are read
        // by the constructor, so this will throw an IOException if the user is
        // offline and bundles are not locally cached.
        TeamProject base = new BundledTeamProject(parent);
        TeamProject main = new BundledTeamProject(first);
        TeamProject incoming = new BundledTeamProject(second);

        // create an object to perform the merge, in-memory
        TeamProjectMerger merger = new TeamProjectMerger(base, main, incoming);

        try {
            merger.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        // save the merged project data to the destination
        TeamProject merged = merger.getMerged();
        merged.saveCopy(((FileResourceCollection) dest).getDirectory());

        // write a history file with an entry for the changes we just merged
        writeChangeHistoryForMerge(first, second, dest);

        // copy the projDump.xml file from the first branch
        copyFile(DATA_DUMP_FILE, first, dest);

        // the WBS bundle could include other files that aren't written by
        // TeamProject.saveCopy. Look for any such files and copy them to the
        // destination
        copyMissingFiles(second, dest);
        copyMissingFiles(first, dest);

        // keep track of any merge conflicts were encountered
        this.conflicts.addAll(merger.conflicts);
    }

    private void writeChangeHistoryForMerge(ReadableResourceCollection main,
            ReadableResourceCollection incoming, ResourceCollection dest)
            throws IOException {
        // read the change history from the main branch
        ChangeHistory hist = new ChangeHistory(
                main.getInputStream(CHANGE_HISTORY_FILE));

        // identify the person/people who added changes to the incoming branch
        ChangeHistory incomingHist = new ChangeHistory(
                incoming.getInputStream(CHANGE_HISTORY_FILE));
        String incomingUser = getIncomingUsername(hist, incomingHist);

        // add an entry for the merge operation with the current timestamp
        long now = System.currentTimeMillis();
        hist.addEntry(incomingUser);

        // write the updated history to the destination
        hist.write(dest.getOutputStream(CHANGE_HISTORY_FILE, now));
    }

    private String getIncomingUsername(ChangeHistory main,
            ChangeHistory incoming) {
        // gather the UIDs of changes in the main branch
        Set<String> mainUids = new HashSet<String>();
        for (ChangeHistory.Entry e : main.getEntries())
            mainUids.add(e.getUid());

        // scan the incoming branch for added changes and get the name of the
        // person who added them. Forks are often written by one person while
        // offline, so this is likely to be a single name
        String result = null;
        for (ChangeHistory.Entry e : incoming.getEntries()) {
            String oneUser = e.getUser();
            if (mainUids.contains(e.getUid())) {
                // this revision was in the main branch
            } else if (result == null) {
                // remember the name of the person who added this change
                result = oneUser;
            } else if (!result.equals(oneUser)) {
                // multiple people added changes to this fork
                return "Various individuals";
            }
        }

        return result == null ? "Various individuals" : result;
    }

    /** Find any files in src that don't appear in dest, and copy them over. */
    private void copyMissingFiles(ReadableResourceCollection src,
            ResourceCollection dest) throws IOException {
        for (String filename : src.listResourceNames()) {
            if (dest.getLastModified(filename) < 1)
                copyFile(filename, src, dest);
        }
    }

    private void copyFile(String filename, ReadableResourceCollection src,
            ResourceCollection dest) throws IOException {
        long modTime = src.getLastModified(filename);
        InputStream in = src.getInputStream(filename);
        OutputStream out = dest.getOutputStream(filename, modTime);
        FileUtils.copyFile(in, out);
        out.close();
        in.close();
    }



    private class BundledTeamProject extends TeamProject {

        private ReadableResourceCollection collection;

        public BundledTeamProject(ReadableResourceCollection collection) {
            super(NONEXISTENT_IGNORED_DIR, "");
            this.collection = collection;
            reload();
        }


        @Override
        public void reload() {
            // our TeamProject superclass calls reload() as part of its
            // constructor, before we have an opportunity to initialize the
            // collection field. Detect that to avoid NullPointerException.
            if (collection != null)
                super.reload();
        }


        /**
         * Read data from our bundle ZIP instead of the filesystem.
         */
        @Override
        protected InputStream openInputStream(File file) throws IOException {
            // read settings.xml from the real project, not the bundle
            String filename = file.getName();
            if ("settings.xml".equalsIgnoreCase(filename))
                return new FileInputStream(
                        new File(workingDir.getDirectory(), filename));

            // return other files from the bundle ZIP
            return collection.getInputStream(filename);
        }


        /**
         * In a master project/subproject environment, the TeamProject object
         * automatically creates ImportDirectory objects for all of the
         * interrelated projects. That is unnecessary for our purposes, so we
         * override this method to skip the ImportDirectory creation step.
         */
        @Override
        protected ImportDirectory getProjectDataDirectory(Element e,
                boolean checkExists) {
            return null;
        }

    }

    private static final File NONEXISTENT_IGNORED_DIR = new File(
            "BundledTeamProject nonexistent directory");

}
