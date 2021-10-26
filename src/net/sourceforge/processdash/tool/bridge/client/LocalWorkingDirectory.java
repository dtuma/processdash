// Copyright (C) 2008-2021 Tuma Solutions, LLC
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
import java.net.URL;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.ConcurrencyLock;
import net.sourceforge.processdash.util.lock.ConcurrencyLockApprover;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.NotLockedException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

public class LocalWorkingDirectory extends AbstractWorkingDirectory implements
        ConcurrencyLockApprover {

    protected FileConcurrencyLock writeLock;

    protected LocalWorkingDirectory(File targetDirectory,
            FileResourceCollectionStrategy strategy,
            File workingDirectoryParent) {
        super(targetDirectory, null, strategy, workingDirectoryParent);
    }


    @Override
    public void acquireProcessLock(String msg, LockMessageHandler lockHandler)
            throws SentLockMessageException, LockFailureException {
        if (Boolean.getBoolean(NO_PROCESS_LOCK_PROPERTY) == false)
            super.acquireProcessLock(msg, lockHandler);
    }

    public void prepare() throws IOException {
        if (!targetDirectory.isDirectory())
            throw new FileNotFoundException(targetDirectory.getPath());
    }

    public void update() throws IOException {}

    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException {
        File lockFile = new File(targetDirectory, lockFilename);
        writeLock = new FileConcurrencyLock(lockFile);
        writeLock.setListenForLostLock(shouldMonitorWriteLock(lockFile));
        writeLock.setApprover(this);
        writeLock.acquireLock(null, lockHandler, ownerName);
    }

    private boolean shouldMonitorWriteLock(File lockFile) {
        String setting = System.getProperty(MONITOR_WRITE_LOCK_USER_SETTING);
        if ("false".equalsIgnoreCase(setting))
            return false;
        else
            return true;
    }

    public void approveLock(ConcurrencyLock lock, String extraInfo)
            throws LockFailureException {
        List<String> filenames = FileUtils.listRecursively(targetDirectory,
            strategy.getFilenameFilter());
        for (String name : filenames) {
            File f = new File(targetDirectory, name);
            if (!f.canWrite())
                throw new ReadOnlyLockFailureException(f);
        }
    }

    public void assertWriteLock() throws LockFailureException {
        if (writeLock == null)
            throw new NotLockedException();
        else
            writeLock.assertLock();
    }

    public void releaseWriteLock() {
        if (writeLock != null)
            writeLock.releaseLock();
    }

    public URL doBackup(String qualifier) throws IOException {
        return super.doBackupImpl(targetDirectory, qualifier);
    }

    public boolean flushData() throws LockFailureException, IOException {
        return true;
    }

    public File getDirectory() {
        return targetDirectory;
    }

    public void releaseLocks() {
        releaseWriteLock();
        if (processLock != null)
            processLock.releaseLock();

        // if the working directory was created solely for the purpose of
        // holding the lock and is now empty, delete the directory.
        String[] files = workingDirectory.list();
        if (files == null || files.length == 0)
            workingDirectory.delete();
    }

    private static final String MONITOR_WRITE_LOCK_USER_SETTING =
        LocalWorkingDirectory.class.getName() + ".monitorWriteLock";

}
