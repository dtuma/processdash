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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;

public class BundledWorkingDirectorySync extends BundledWorkingDirectoryLocal {

    private boolean enableBackgroundFastForward;

    protected boolean enforceLocks;

    private ForkTracker forkTracker;

    private BundleMergeCoordinator bundleMergeCoordinator;

    private Worker worker;

    private static final Logger logger = Logger
            .getLogger(BundledWorkingDirectorySync.class.getName());

    public BundledWorkingDirectorySync(File targetDirectory,
            FileResourceCollectionStrategy strategy,
            File workingDirectoryParent) {
        super(targetDirectory, strategy, workingDirectoryParent);
        super.setEnableBackgroundFlush(false);

        // add fast-forward voting for WBS directories. (Not needed for team
        // dashboard directories at this time.)
        enableBackgroundFastForward = isWbsOrDisseminateDirectory();

        // disable locking by default. (It isn't possible to acquire exclusive
        // locks of directories managed by an external sync client.)
        enforceLocks = false;
    }

    public boolean isEnableBackgroundFastForward() {
        return enableBackgroundFastForward;
    }

    public void setEnableBackgroundFastForward(
            boolean enableBackgroundFastForward) {
        this.enableBackgroundFastForward = enableBackgroundFastForward;
    }

    @Override
    public void setEnableBackgroundFlush(boolean enableBackgroundFlush) {
        // do not allow clients to enable background flush
    }

    public ForkTracker getForkTracker() {
        return forkTracker;
    }

    public FileBundleDirectory getBundleDirectory() {
        return client.getBundleDir();
    }

    public BundleMergeCoordinator getBundleMergeCoordinator() {
        return bundleMergeCoordinator;
    }

    public void setBundleMergeCoordinator(BundleMergeCoordinator bmc) {
        this.bundleMergeCoordinator = bmc;
    }

    @Override
    public FileBundleMode getBundleMode() {
        return FileBundleMode.Sync;
    }


    @Override
    protected HeadRefs makeBundleHeads(File bundleHeadsDir) throws IOException {
        // the fork tracker creates the HEAD management object for a sync dir
        this.forkTracker = new ForkTracker(bundleHeadsDir, HEADS_FILE_PREFIX,
                DeviceID.get(), getOverwriteBundleNames());
        return this.forkTracker.getSelfHeadRefs();
    }

    private Set<String> getOverwriteBundleNames() {
        Set<String> result = new HashSet<String>();
        for (Object[] partitionSpec : strategy.getBundlePartitions()) {
            String bundleName = (String) partitionSpec[0];
            if (FileBundlePartitioner.isOverwriteBundle(partitionSpec))
                result.add(bundleName);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    protected void makeBundleClient() throws IOException {
        super.makeBundleClient();

        // give the fork tracker a source for reading bundle manifests
        forkTracker.setManifestSource(client.getManifests());

        // start a background worker to perform fast-forwards if desired
        if (enableBackgroundFastForward)
            worker = new Worker();
    }

    @Override
    protected void repairCorruptFiles() throws IOException {
        super.repairCorruptFiles();

        // if unflushed files are present in a dashboard directory at startup,
        // eagerly save them. This will create a new bundle for the dirty files
        // to make sure they are not lost before we update() the directory. That
        // bundle could potentially be a fork that will trigger merge logic.
        if (isDashboardDatasetDirectory() && client.isDirty())
            flushLeftoverDirtyFiles();
    }


    @Override
    public void update() throws IllegalStateException, IOException {
        clientPrecheck();
        fastForward();
        doMerge();
        doSyncDown(2);
    }

    private boolean fastForward() {
        // see if other people have published new forks on top of our last save.
        // if so, adopt them as our own.
        try {
            return forkTracker.fastForward();
        } catch (IOException ioe) {
            // if the computer is offline or the external file sync client is
            // not running, attempts to read uncached cloud files will fail.
            // Proceed anyway, working against our previous HEAD
            return false;
        }
    }


    private boolean doMerge() {
        try {
            if (bundleMergeCoordinator != null)
                return bundleMergeCoordinator.doMerge();

        } catch (IOException ioe) {
            // if the computer is offline or the external file sync client is
            // not running, attempts to read uncached cloud files will fail.
            // Proceed anyway, saving the merge for another time
            ioe.printStackTrace();
        }
        return false;
    }


    @Override
    public boolean flushData() throws LockFailureException, IOException {
        clientPrecheck();
        return doSyncUp(2);
    }


    @Override
    protected void shutDown() {
        if (worker != null) {
            worker.shutDown();
            worker = null;
        }
    }


    //
    // Conditional implementations of locking methods:
    //
    //   * It isn't possible to acquire exclusive locks of directories
    //     managed by an external sync client, so our bundle logic is written
    //     to work in the complete absence of locks.
    //
    //   * locks are reenabled during migration operations, to cover the
    //     boundary case where Sync mode is being used on shared network
    //     directories.
    //

    @Override
    public void acquireWriteLock(LockMessageHandler lmh, String ownerName)
            throws LockFailureException {
        if (enforceLocks)
            super.acquireWriteLock(lmh, ownerName);
    }

    @Override
    public void assertWriteLock() throws LockFailureException {
        if (enforceLocks)
            super.assertWriteLock();
    }

    @Override
    public void releaseWriteLock() {
        if (enforceLocks)
            super.releaseWriteLock();
    }



    private class Worker extends Thread {

        private volatile boolean isRunning;

        public Worker() {
            super("BundledDirectorySync.Worker(" + getDescription() + ")");
            setDaemon(true);
            this.isRunning = true;
            start();
        }

        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(ONE_MINUTE);
                } catch (InterruptedException ie) {
                }

                if (!isRunning)
                    break;

                try {
                    fastForward();
                } catch (Throwable e) {
                    logger.log(Level.WARNING,
                        "Unexpected exception encountered when "
                                + "attempting bundle fast forward",
                        e);
                }
            }
        }

        public void shutDown() {
            this.isRunning = false;
            this.interrupt();
        }

    }


    private static final String HEADS_FILE_PREFIX = "heads-";

    private static final long ONE_MINUTE = 60 * 1000;

}
