// Copyright (C) 2022-2025 Tuma Solutions, LLC
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

import static teamdash.wbs.WBSFilenameConstants.MERGE_METADATA_FILE;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.bundle.BundleMergeCoordinator;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMerger;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleSpec;

import teamdash.merge.ui.MergeConflictNotification;

public class TeamProjectBundleMergeCoordinator extends BundleMergeCoordinator {

    public static final String CONFLICT_FLAG = "mergeConflict";

    public static final String ERROR_FLAG = "mergeError";

    private String errorFlag;


    public TeamProjectBundleMergeCoordinator(BundledWorkingDirectorySync dir) {
        super(dir);
    }

    public List<MergeConflictNotification> getConflicts() {
        // return the list of conflict notifications from our bundle merger
        return getBundleMerger().getAndClearConflicts();
    }

    @Override
    public boolean doMerge() throws IOException {
        try {
            // perform the bundle merge operation
            boolean madeChange = super.doMerge();

            // set an error flag if merge conflicts were detected
            if (getBundleMerger().conflicts.isEmpty() == false)
                errorFlag = CONFLICT_FLAG;

            return madeChange;

        } catch (IOException ioe) {
            // set an error flag if an IOException occurred
            errorFlag = ERROR_FLAG;
            throw ioe;
        }
    }

    public String getAndClearErrorFlag() {
        try {
            return errorFlag;
        } finally {
            errorFlag = null;
        }
    }

    @Override
    protected BundleMerger makeBundleMerger() {
        return new TeamProjectBundleMerger(workingDir);
    }

    private TeamProjectBundleMerger getBundleMerger() {
        return (TeamProjectBundleMerger) bundleMerger;
    }

    @Override
    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs)
            throws IOException {
        // the fork tracker sorts bundles by the number of votes they receive;
        // so the first item is the one in popular use by the team.
        FileBundleID teamRef = bundleIDs.get(0);

        // check the name of the bundle. We only merge the WBS bundle
        if (!WBS_BUNDLE_NAME.equals(teamRef.getBundleName())) {
            log.severe(logPrefix + "Unexpected forks in bundle '"
                    + teamRef.getBundleName() + "' during team project merge");
            return null;
        }

        // find out which version of the bundle this device is using, and where
        // that ref falls in the team voting order.
        FileBundleID selfRef = forkTracker.getSelfHeadRefs()
                .getHeadRef(WBS_BUNDLE_NAME);
        int selfPos = bundleIDs.indexOf(selfRef);

        if (selfPos < 0) {
            // this device hasn't adopted any of the recognized forks. It may
            // need to perform a fast-forward before trying again.
            return null;

        } else if (selfPos == 0) {
            // this device is already using the team bundle. No changes needed
            return null;

        } else {
            // this device has forked away from the team branch. merge this
            // device back into the team's branch
            return super.mergeBundleForks(Arrays.asList(teamRef, selfRef));
        }
    }

    @Override
    protected void tweakSpec(FileBundleSpec spec) {
        if (spec.filenames.remove(MERGE_METADATA_FILE))
            spec.metadata = Collections.singletonList(MERGE_METADATA_FILE);
    }

    private static final String WBS_BUNDLE_NAME = "wbs";

}
