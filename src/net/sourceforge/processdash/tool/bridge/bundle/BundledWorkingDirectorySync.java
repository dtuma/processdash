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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;

public class BundledWorkingDirectorySync extends BundledWorkingDirectoryLocal {

    private boolean enableBackgroundFastForward;

    private ForkTracker forkTracker;

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

    @Override
    public FileBundleMode getBundleMode() {
        return FileBundleMode.Sync;
    }


    @Override
    protected HeadRefs makeBundleHeads(File bundleHeadsDir) throws IOException {
        // the fork tracker creates the HEAD management object for a sync dir
        this.forkTracker = new ForkTracker(bundleHeadsDir, HEADS_FILE_PREFIX,
                DeviceID.get());
        return this.forkTracker.getSelfHeadRefs();
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
    // No-op implementations of locking methods. (It isn't possible to acquire
    // exclusive locks of directories managed by an external sync client.)
    //

    @Override
    public void acquireWriteLock(LockMessageHandler lmh, String ownerName) {}

    @Override
    public void assertWriteLock() {}

    @Override
    public void releaseWriteLock() {}



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
