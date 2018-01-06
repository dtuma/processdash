// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class TeamProjectDataTargetFactory {


    /**
     * Get a data target corresponding to a working directory that has already
     * been created and initialized within the WBS Editor process.
     */
    public static TeamProjectDataTarget getWbsEditorInProcessTarget(
            WorkingDirectory dir) throws IOException {
        return new InProcessTarget(dir);
    }


    private static class InProcessTarget implements TeamProjectDataTarget {

        WorkingDirectory workingDir;

        InProcessTarget(WorkingDirectory dir) {
            this.workingDir = dir;
        }

        @Override
        public File getDirectory() {
            return workingDir.getDirectory();
        }

        @Override
        public void lock(String lockOwner) throws LockFailureException {
            int timeoutSeconds = Integer
                    .getInteger("teamdash.wbs.acquireLockTimeout", 60);
            long timeoutTimestamp = System.currentTimeMillis()
                    + (timeoutSeconds * 1000);
            Random r = null;
            AlreadyLockedException ale = null;

            while (System.currentTimeMillis() < timeoutTimestamp) {
                try {
                    workingDir.acquireWriteLock(null, lockOwner);
                    return;

                } catch (AlreadyLockedException e) {
                    // if someone else is holding the lock, wait for a moment
                    // to see if they release it. Then try again.
                    ale = e;
                    try {
                        // wait a randomly selected amount of time between 0.5
                        // and 1.5 seconds. Randomness is included in case
                        // several processes are attempting to get the lock at
                        // the same time
                        if (r == null)
                            r = new Random();
                        Thread.sleep(500 + r.nextInt(1000));
                    } catch (InterruptedException e1) {
                    }

                } catch (LockFailureException e) {
                    throw e;
                }
            }

            throw (ale != null ? ale : new LockFailureException());
        }

        @Override
        public void update() throws IOException {
            workingDir.update();
        }

        @Override
        public void saveChanges() throws IOException {
            try {
                workingDir.flushData();
            } catch (LockFailureException lfe) {
                throw new IOException(lfe);
            }
        }

        @Override
        public void saveSyncData(String syncFile) throws IOException {
            if (workingDir instanceof BridgedWorkingDirectory) {
                URL url = new URL(workingDir.getDescription());
                File src = new File(getDirectory(), syncFile);
                InputStream data = new FileInputStream(src);
                try {
                    ResourceBridgeClient.uploadSingleFile(url, syncFile, data);
                } catch (LockFailureException e) {
                    throw new IOException(e);
                }
            }
        }

        @Override
        public void unlock() {
            workingDir.releaseWriteLock();
        }

        @Override
        public void dispose() {}

    }



    /**
     * Get a data target corresponding to a given location, which could be a
     * filesystem path or an HTTP URL
     */
    public static TeamProjectDataTarget getBatchProcessTarget(String location)
            throws IOException {
        try {
            WorkingDirectory dir = WorkingDirectoryFactory.getInstance()
                    .get(TEMP_WBS_PURPOSE, location);
            if (dir instanceof BridgedWorkingDirectory)
                ((BridgedWorkingDirectory) dir).setAllowUpdateWhenLocked(true);
            dir.acquireProcessLock(IGNORED_MESSAGE, null);
            dir.prepare();
            return new BatchTarget(dir);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    private static class BatchTarget extends InProcessTarget {

        private boolean dirWasJustPrepared;

        BatchTarget(WorkingDirectory dir) {
            super(dir);
            dirWasJustPrepared = true;
        }

        @Override
        public void update() throws IOException {
            if (dirWasJustPrepared)
                dirWasJustPrepared = false;
            else
                super.update();
        }

        @Override
        public void dispose() {
            workingDir.releaseLocks();
            try {
                if (workingDir instanceof BridgedWorkingDirectory)
                    FileUtils.deleteDirectory(getDirectory(), true);
            } catch (IOException e) {
            }
        }

    }


    private static final int TEMP_WBS_PURPOSE = //
            WorkingDirectoryFactory.PURPOSE_WBS
                    + WorkingDirectoryFactory.PURPOSE_TEMP;

    private static final String IGNORED_MESSAGE = "";

}
