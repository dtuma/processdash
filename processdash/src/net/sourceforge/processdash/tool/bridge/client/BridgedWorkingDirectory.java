// Copyright (C) 2008 Tuma Solutions, LLC
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
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;

public class BridgedWorkingDirectory extends AbstractWorkingDirectory {


    ResourceBridgeClient client;

    Worker worker;

    private static final Logger logger = Logger
            .getLogger(BridgedWorkingDirectory.class.getName());


    protected BridgedWorkingDirectory(File targetDirectory, String remoteURL,
            FileResourceCollectionStrategy strategy, File workingDirectoryParent) {
        super(targetDirectory, remoteURL, strategy.getLockFilename(),
                workingDirectoryParent);

        FileResourceCollection collection = new FileResourceCollection(
                workingDirectory);
        collection.setStrategy(strategy);

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

        for (int numTries = 5; numTries-- > 0;)
            if (client.syncDown() == false)
                return;

        throw new IOException("Unable to sync down");
    }

    public File getDirectory() {
        return workingDirectory;
    }

    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException {
        client.acquireLock(ownerName);
        worker = new Worker(lockHandler);
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
    }

    private String getSourceIdentifier() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return null;
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
                        client.syncUp();
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
                } catch (Exception e) {
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

}
