// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleModeMismatch;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.NotLockedException;

/**
 * An {@link ImportDirectory} object that reads files directly from their
 * original source directory on the filesystem.
 */
public class LocalImportDirectory implements ImportDirectory {

    private File targetDirectory;

    protected LocalImportDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public File getDirectory() {
        return targetDirectory;
    }

    public String getRemoteLocation() {
        return null;
    }

    public String getDescription() {
        return targetDirectory.getAbsolutePath();
    }

    public Boolean isBadDelegate() {
        return isBadDelegate(targetDirectory, null);
    }

    public void validate() throws IOException {
        FileBundleUtils.ensureBundleMode(targetDirectory, null);
    }

    public void update() {}

    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException {
        ensureUnlocked(filename);
        FileUtils.copyFile(source, new File(targetDirectory, filename));
    }

    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException {
        ensureUnlocked(filename);
        File fileToDelete = new File(targetDirectory, filename);
        fileToDelete.delete();
        if (fileToDelete.exists())
            throw new IOException("Couldn't delete " + fileToDelete);
    }

    private void ensureUnlocked(String file)
            throws IOException, LockFailureException {
        // make sure the filename is acceptable to write without a lock
        if (!TeamDataDirStrategy.INSTANCE.getUnlockedFilter()
                .accept(targetDirectory, file))
            throw new NotLockedException();

        // make sure the target directory is reachable
        if (!targetDirectory.isDirectory())
            throw new FileNotFoundException(targetDirectory.getPath());
    }

    public static Boolean isBadDelegate(File targetDir,
            FileBundleMode expectedBundleMode) {
        // if we can't reach the directory, status is indeterminate
        if (!targetDir.isDirectory())
            return null;

        // if the directory contains a team server file, it is obsolete
        if (new File(targetDir, TeamServerPointerFile.FILE_NAME).isFile())
            return Boolean.TRUE;

        // if the bundled type doesn't match, it is bad
        try {
            FileBundleUtils.ensureBundleMode(targetDir, expectedBundleMode);
        } catch (FileBundleModeMismatch fbmm) {
            return Boolean.TRUE;
        } catch (IOException ioe) {
            // if we couldn't read the bundle mode, status is indeterminate
            return null;
        }

        // the directory exists and hasn't been migrated. Not a bad delegate
        return Boolean.FALSE;
    }

}
