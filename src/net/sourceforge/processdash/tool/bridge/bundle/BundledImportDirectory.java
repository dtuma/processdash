// Copyright (C) 2021-2023 Tuma Solutions, LLC
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
import java.util.Arrays;
import java.util.Properties;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.DynamicImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.LocalImportDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.NotLockedException;

public class BundledImportDirectory implements ImportDirectory {

    protected BundledWorkingDirectory workingDir;

    protected long lastUpdateTime;

    public BundledImportDirectory(File dir) {
        this.workingDir = BundledWorkingDirectoryLocal.create(dir, STRATEGY,
            DirectoryPreferences.getMasterWorkingDirectory(), true);
        this.lastUpdateTime = -1;
    }

    protected BundledImportDirectory(File dir,
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
        return LocalImportDirectory.isBadDelegate(
            workingDir.getTargetDirectory(), workingDir.getBundleMode());
    }

    public FileBundleMode getBundleMode() {
        return workingDir.getBundleMode();
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

        BundledWorkingDirectoryLocal bwd = //
                (BundledWorkingDirectoryLocal) workingDir;
        boolean lockWasCreated = bwd.lockWorkingDir();
        try {
            FileUtils.copyFile(source, new File(getDirectory(), filename));
            flushSingleFile(filename);
            maybePurgeOldPdashFiles(filename);
        } finally {
            bwd.unlockWorkingDirIf(lockWasCreated);
        }
    }

    private void maybePurgeOldPdashFiles(String filename) {
        // only purge older PDASH files
        if (!filename.toLowerCase().endsWith(".pdash"))
            return;

        // see how many PDASH bundles we're configured to retain. 0 == no limit
        int retainBundleCount = getPdashRetentionBundleCount();
        if (retainBundleCount <= 0)
            return;

        // Find the bundle dir and list its contents
        File targetDir = workingDir.getTargetDirectory();
        File bundleDir = new File(targetDir, FileBundleConstants.BUNDLE_SUBDIR);
        String[] filenames = bundleDir.list();
        if (filenames == null)
            return;

        // Scan files starting with newest, looking for bundles of this file
        String suffix = "-" + FileBundleID.filenameToBundleName(filename)
                + ".xml";
        Arrays.sort(filenames);
        int count = 0;
        for (int i = filenames.length; i-- > 0;) {
            String fn = filenames[i];
            if (fn.endsWith(suffix)) {
                if (++count > retainBundleCount) {
                    // log a message about purging this bundle
                    String bundleID = fn.substring(0, fn.length() - 4);
                    BundledWorkingDirectoryLocal.logger.fine(
                        ((BundledWorkingDirectoryLocal) workingDir).logPrefix
                                + "Purging old bundle " + bundleID);

                    // delete the XML and ZIP file associated with the bundle
                    new File(bundleDir, fn).delete();
                    new File(bundleDir, bundleID + ".zip").delete();
                }
            }
        }
    }

    private int getPdashRetentionBundleCount() {
        if (pdashRetentionBundleCount == null)
            pdashRetentionBundleCount = readPdashRetentionBundleCount();
        return pdashRetentionBundleCount;
    }

    private Integer readPdashRetentionBundleCount() {
        try {
            File targetDir = workingDir.getTargetDirectory();
            Properties p = FileBundleUtils.getBundleProps(targetDir);
            String val = p.getProperty("pdashRetentionBundleCount");
            return Integer.valueOf(val);
        } catch (Throwable t) {
        }

        // default: retain 10 bundles
        return 10;
    }

    private Integer pdashRetentionBundleCount = null;


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


    public static boolean isBundled(ImportDirectory dir, FileBundleMode mode) {
        // retrieve underlying delegate for DynamicImportDirectory
        if (dir instanceof DynamicImportDirectory)
            dir = ((DynamicImportDirectory) dir).getDelegate();

        // if this is not a bundled import dir, return null
        if (!(dir instanceof BundledImportDirectory))
            return false;

        // if no specific mode was requested, return true
        if (mode == null)
            return true;

        // return true if this directory uses the given bundle mode
        BundledImportDirectory bid = (BundledImportDirectory) dir;
        return mode.equals(bid.getBundleMode());
    }


    public static ForkTracker getSyncBundleForkTracker(ImportDirectory dir) {
        // retrieve underlying delegate for DynamicImportDirectory
        if (dir instanceof DynamicImportDirectory)
            dir = ((DynamicImportDirectory) dir).getDelegate();

        // if this is not a bundled import dir, return null
        if (!(dir instanceof BundledImportDirectory))
            return null;

        // if the import isn't using a sync working dir, return null
        BundledImportDirectory bid = (BundledImportDirectory) dir;
        if (!(bid.workingDir instanceof BundledWorkingDirectorySync))
            return null;

        // retrieve the fork tracker from the working directory
        return ((BundledWorkingDirectorySync) bid.workingDir).getForkTracker();
    }

    private static final TeamDataDirStrategy STRATEGY = TeamDataDirStrategy.INSTANCE;

}
