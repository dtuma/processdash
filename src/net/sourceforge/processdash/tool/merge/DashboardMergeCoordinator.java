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

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMergeCoordinator;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMerger;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;

public class DashboardMergeCoordinator extends BundleMergeCoordinator {

    public DashboardMergeCoordinator(BundledWorkingDirectorySync dir) {
        super(dir);
    }


    @Override
    protected BundleMerger makeBundleMerger() {
        // currently, we only support sync bundle mode for teams
        return new DashboardBundleMergerTeam();
    }


    @Override
    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs)
            throws IOException {
        // sort the bundles to merge in chronological order
        Collections.sort(bundleIDs, FileBundleID.CHRONOLOGICAL_ORDER);

        // allow superclass logic to run the merge and publish the results
        return super.mergeBundleForks(bundleIDs);
    }


    public void showMergeConflictWarnings(Component parent) {
        List<String> warningKeys = getMergeConflictWarningKeys();
        if (warningKeys.isEmpty())
            return;

        Resources res = Resources.getDashBundle("Bundler.Merge.Conflict");
        String title = res.getString("Title");
        String[] bullets = new String[warningKeys.size()];
        for (int i = bullets.length; i-- > 0;)
            bullets[i] = BULLET + res.getString(warningKeys.get(i));
        Object[] message = new Object[] { res.getStrings("Header"), bullets,
                " ", res.getStrings("Footer") };
        JOptionPane.showMessageDialog(parent, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private List<String> getMergeConflictWarningKeys() {
        List<String> result = new ArrayList<String>();
        DashboardBundleMerger dbm = (DashboardBundleMerger) bundleMerger;
        for (String filename : dbm.getAndClearMergedFiles()) {
            String key = getMergeConflictWarningKey(filename);
            if (key == null) {
                log.warning("Overlapping edits made to file '" + filename
                        + "' - merge performed");
            } else {
                log.severe("Conflicting edits made to file '" + filename
                        + "' - data may have been lost");
                result.add(key);
            }
        }
        return result;
    }

    private String getMergeConflictWarningKey(String filename) {
        for (String[] item : MERGE_WARNING_KEYS) {
            if (filename.startsWith(item[0]))
                return item[1];
        }
        return null;
    }

    private static final String[][] MERGE_WARNING_KEYS = { //
            { "state", "Hierarchy" }, //
            { "groups.dat", "Groups" }, //
            { "roles.dat", "Roles" }, //
            { "users.dat", "Users" }, //
            { "cms/", "Reports" } //
    };

    private static final String BULLET = "    \u2022 ";

}
