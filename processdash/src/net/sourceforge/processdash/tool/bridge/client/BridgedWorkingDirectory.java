// Copyright (C) 2008-2011 Tuma Solutions, LLC
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
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.DirectoryBackup;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;

public class BridgedWorkingDirectory extends AbstractWorkingDirectory {


    ResourceBridgeClient client;

    DirectoryBackup startupBackupTask;

    Worker worker;

    Thread shutdownHook;

    private static final Logger logger = Logger
            .getLogger(BridgedWorkingDirectory.class.getName());


    protected BridgedWorkingDirectory(File targetDirectory, String remoteURL,
            FileResourceCollectionStrategy strategy, File workingDirectoryParent) {
        super(targetDirectory, remoteURL, strategy.getLockFilename(),
                workingDirectoryParent);

        FileResourceCollection collection = new FileResourceCollection(
                workingDirectory, false);
        collection.setStrategy(strategy);

        startupBackupTask = strategy.getBackupHandler(workingDirectory);

        client = new ResourceBridgeClient(collection, remoteURL, strategy
                .getUnlockedFilter());
        client.setSourceIdentifier(getSourceIdentifier());
        client.setExtraLockData(processLock.getLockHash());
    }

    public void prepare() throws IOException {
        if (!processLock.isLocked())
            // don't attempt to sync down if we don't own the rights to the
            // working directory!
            throw new IllegalStateException(
                    "Process lock has not been obtained");

        // Make a local backup of the initial data in the working directory.
        // This way, its former contents will be saved before we overwrite the
        // files with data from the server.
        if (startupBackupTask != null) {
            startupBackupTask.backup("startup");
            startupBackupTask = null;
        }

        SyncFilter filter = getSyncDownFilter();
        for (int numTries = 5; numTries-- > 0;)
            if (client.syncDown(filter) == false)
                return;

        throw new IOException("Unable to sync down");
    }

    /**
     * @see WorkingDirectory#update()
     * 
     * @throws IllegalStateException
     *             if the current process owns a write lock on this collection,
     *             or if it does not own a process lock
     */
    public void update() throws IOException {
        if (worker == null)
            prepare();
        else
            throw new IllegalStateException("update should not be called in "
                    + "read-write mode.");
    }

    public File getDirectory() {
        return workingDirectory;
    }

    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException {
        client.acquireLock(ownerName);
        worker = new Worker(lockHandler);
        registerShutdownHook();
    }

    public void assertWriteLock() throws LockFailureException {
        client.assertLock();
    }

    public boolean flushData() throws LockFailureException, IOException {
        for (int numTries = 5; numTries-- > 0;) {
            if (client.syncUp() == false) {
                if (worker != null)
                    worker.resetFlushFrequency();
                try {
                    client.saveDefaultExcludedFiles();
                } catch (Exception e) {
                    logger.log(Level.FINE,
                        "Unable to save default excluded files", e);
                }
                saveSyncTimestamp();
                return true;
            }
        }
        return false;
    }

    public URL doBackup(String qualifier) throws IOException {
        return client.doBackup(qualifier);
    }

    public void releaseLocks() {
        if (worker != null)
            worker.shutDown();
        client.releaseLock();
        if (processLock != null)
            processLock.releaseLock();
        unregisterShutdownHook();
    }

    private void registerShutdownHook() {
        // release the lock when the JVM is closing
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {
            public void run() {
                shutdownHook = null;
                releaseLocks();
            }
        });
    }

    private void unregisterShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Exception e) {}
            shutdownHook = null;
        }
    }

    private String getSourceIdentifier() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    private SyncFilter getSyncDownFilter() {
        long timestamp;
        try {
            timestamp = Long.parseLong(getMetadata(SYNC_TIMESTAMP));
        } catch (Exception e) {
            // if the sync timestamp is not present or cannot be read, don't
            // use a sync down filter.
            return null;
        }
        return new SyncDownFilter(timestamp);
    }

    private void saveSyncTimestamp() {
        try {
            setMetadata(SYNC_TIMESTAMP,
                Long.toString(System.currentTimeMillis()));
        } catch (IOException ioe) {
        }
    }


    private class SyncDownFilter implements SyncFilter {

        private long lastSyncTimestamp;

        public SyncDownFilter(long lastSyncTimestamp) {
            this.lastSyncTimestamp = lastSyncTimestamp;
        }

        /**
         * When running in bridged mode, the dashboard flushes all data to the
         * server periodically, and when it shuts down. Unfortunately, there
         * could still be scenarios in which the dashboard shuts down without
         * getting a chance to connect to the server and save changes.
         * 
         * Fortunately, many datasets are personal use datasets, used by a
         * single individual. If an individual's dashboard closes without saving
         * data, and no one else has opened the dataset in the meantime, there
         * isn't any reason why we can't recover from the earlier connectivity
         * problem and upload our locally changed files.
         * 
         * If a file was locally created/modified after our the most recent
         * connection to the server, and if the file has not been modified on
         * the server in the meantime, this class will detect that pattern and
         * ask the syncDown logic NOT to retrieve the obsolete file from the
         * server. Then, we will automatically save the locally modified file
         * to the server when we perform our next syncUp operation.
         */
        public boolean shouldSync(String name, long localTimestamp,
                long remoteTimestamp) {

            if (localTimestamp <= 0) {
                // file does not exist locally; it's only on the server.  We
                // want to copy it down to get the complete set of resources.
                return true;
            }

            if (remoteTimestamp <= 0) {
                // the file exists locally, but not on the server.

                if (localTimestamp > lastSyncTimestamp)
                    // The local file was created/modified sometime after our
                    // last server sync.  We want to keep it around.
                    return false;

                else
                    // the last modification of our local file occurred before
                    // the last syncUp operation.  That means that the file
                    // must have been deleted on the server at some later
                    // point in time.  We should abide by the server's
                    // instructions and delete the file too.
                    return true;
            }

            // the file exists in both places, but differs.
            if (remoteTimestamp <= lastSyncTimestamp)
                // the file on the server is older than our last syncUp
                // operation.  That means it must be the file that we wrote. If
                // our local file is newer, we want to keep our local changes.
                return false;

            else
                // the file on the server is newer than our last syncUp
                // operation.  This indicates an unfortunate situation where
                // the file was modified concurrently by two different people.
                // In this case, the server "wins" and we must retrieve its
                // version to stay in sync.
                return true;
        }

    }


    private class Worker extends Thread {

        private volatile boolean isRunning;

        private LockMessageHandler lockHandler;

        private volatile int flushCountdown;

        public Worker(LockMessageHandler lockHandler) {
            super("WorkingDirBridge.Worker(" + remoteURL + ")");
            setDaemon(true);
            this.isRunning = true;
            this.lockHandler = lockHandler;
            start();
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(ONE_MINUTE);
                } catch (InterruptedException ie) {
                }

                if (!isRunning)
                    break;

                try {
                    client.pingLock();

                    if (flushCountdown > 1)
                        flushCountdown--;
                    else {
                        resetFlushFrequency();
                        if (client.syncUp())
                            saveSyncTimestamp();
                    }
                } catch (LockUncertainException lue) {
                } catch (LockFailureException lfe) {
                    sendMessage(LockMessage.LOCK_LOST_MESSAGE);
                } catch (IOException ioe) {
                    // some problem has prevented us from contacting the
                    // server and syncing up our data.  The problem could
                    // be transient (temporary network unavailability), in
                    // which case we'll get a chance to catch up later.
                    // So we don't bug the user about it, because it may be
                    // harmless.  In the event that we never get a chance to
                    // recover, we will be able to display a warning to the
                    // user as they shut down.
                } catch (Throwable e) {
                    logger.log(Level.WARNING,
                        "Unexpected exception encountered when "
                                + "uploading working files to server", e);
                }
            }
        }

        private void sendMessage(String message) {
            try {
                LockMessage lockMessage = new LockMessage(
                        BridgedWorkingDirectory.this, message);
                lockHandler.handleMessage(lockMessage);
            } catch (Exception e) {
            }
        }

        public void shutDown() {
            this.isRunning = false;
        }

        public void resetFlushFrequency() {
            flushCountdown = FLUSH_FREQUENCY;
        }

    }

    private static final long ONE_MINUTE = 60 * 1000;

    private static final int FLUSH_FREQUENCY = 5;

    private static final String SYNC_TIMESTAMP = "syncUpTimestamp";

}
