// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.processdash.util.RobustFileOutputStream;

public class FileBundleUtils {

    /**
     * Open a stream for writing to a file in a bundle directory.
     * 
     * (This method provides a single place for modifying our policy in the
     * future, if the RobustFileOutputStream is found to cause problems with a
     * sync client.)
     */
    public static OutputStream outputStream(File destFile) throws IOException {
        return new BufferedOutputStream(
                new RobustFileOutputStream(destFile, false));
    }

    /**
     * @return true if the given directory is a bundled directory
     */
    public static boolean isBundledDir(File dir) {
        File bundles = new File(dir, FileBundleConstants.BUNDLE_SUBDIR);
        File heads = new File(dir, FileBundleConstants.HEADS_SUBDIR);
        return dir != null && bundles.isDirectory() && heads.isDirectory();
    }

}
