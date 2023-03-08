// Copyright (C) 2023 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;

public class CloudStorageUtils {

    /**
     * Return true if a working directory is using cloud storage.
     */
    public static boolean isCloudStorage(WorkingDirectory dir) {
        // sync bundle mode is currently only enabled by logic in the cloud
        // migration wizard; so if a working directory is sync bundled, we can
        // assume it is being stored in the cloud. If this assumption changes
        // in the future, this logic will need revising.
        return dir instanceof BundledWorkingDirectorySync;
    }

    /**
     * Return true if an import directory is using cloud storage.
     */
    public static boolean isCloudStorage(ImportDirectory dir) {
        // sync bundle mode is currently only enabled by logic in the cloud
        // migration wizard; so if an import directory is sync bundled, we can
        // assume it is being stored in the cloud. If this assumption changes
        // in the future, this logic will need revising.
        return BundledImportDirectory.isBundled(dir, FileBundleMode.Sync);
    }

}
