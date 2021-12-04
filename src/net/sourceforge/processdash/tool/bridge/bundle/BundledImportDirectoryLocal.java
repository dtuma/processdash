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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.LocalImportDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.NotLockedException;

public class BundledImportDirectoryLocal implements ImportDirectory {

    protected BundledWorkingDirectory workingDir;

    protected long lastUpdateTime;

    public BundledImportDirectoryLocal(File dir) {
        this.workingDir = BundledWorkingDirectoryLocal.create(dir, STRATEGY,
            DirectoryPreferences.getMasterWorkingDirectory(), true);
        this.lastUpdateTime = -1;
    }

    protected BundledImportDirectoryLocal(File dir,
            FileResourceCollectionStrategy strategy) {
        this.workingDir = BundledWorkingDirectoryLocal.create(dir, strategy,
            DirectoryPreferences.getMasterImportDirectory(), false);
        this.lastUpdateTime = -1;
    }

    public String getDescription() {
        return workingDir.getDescription();
    }

    public File getDirectory() {
        return workingDir.getDirectory();
    }

    public String getRemoteLocation() {
        return null;
    }

    public Boolean isBadDelegate() {
        return LocalImportDirectory
                .isBadDelegate(workingDir.getTargetDirectory(), true);
    }

    public void validate() throws IOException {
        doUpdate(true);
    }

    public void update() throws IOException {
        doUpdate(false);
    }

    private synchronized void doUpdate(boolean force) throws IOException {
        // for the very first update, ensure the working directory has been
        // created and prepared
        if (lastUpdateTime < 0) {
            workingDir.getDirectory().mkdir();
            workingDir.prepare();
            lastUpdateTime = 0;
        }

        // this method may get called overzealously by code in different layers
        // of the application. If it is called more than once within a few
        // milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateAge = now - lastUpdateTime;
        if (force || lastUpdateAge > 1000 || lastUpdateAge < 0) {
            workingDir.update();
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException {
        ensureUnlocked(filename);
        FileUtils.copyFile(source, new File(getDirectory(), filename));
        flushSingleFile(filename);
    }

    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException {
        // check preconditions
        ensureUnlocked(filename);
        File dir = getDirectory();
        if (!dir.isDirectory())
            throw new FileNotFoundException(dir.getPath());

        // delete the file, and make sure the deletion was successful
        File fileToDelete = new File(dir, filename);
        fileToDelete.delete();
        if (fileToDelete.exists())
            throw new IOException("Couldn't delete " + fileToDelete);

        // flush changes to bundle storage
        flushSingleFile(filename);
    }

    private void ensureUnlocked(String filename) throws LockFailureException {
        if (!STRATEGY.getUnlockedFilter().accept(getDirectory(), filename))
            throw new NotLockedException();
    }

    private void flushSingleFile(String filename)
            throws LockFailureException, IOException {
        if (workingDir.flushFile(filename) == false)
            throw new IOException("Couldn't flush changes to " + filename
                    + " in " + getDescription());
    }

    private static final TeamDataDirStrategy STRATEGY = TeamDataDirStrategy.INSTANCE;

}
