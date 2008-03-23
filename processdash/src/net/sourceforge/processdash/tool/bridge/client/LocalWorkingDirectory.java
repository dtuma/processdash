// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
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

    protected FileResourceCollectionStrategy strategy;

    protected FileConcurrencyLock writeLock;

    protected LocalWorkingDirectory(File targetDirectory,
            FileResourceCollectionStrategy strategy,
            File workingDirectoryParent) {
        super(targetDirectory, null, strategy.getLockFilename(),
                workingDirectoryParent);
        this.strategy = strategy;
    }


    @Override
    public void acquireProcessLock(String msg, LockMessageHandler lockHandler)
            throws SentLockMessageException, LockFailureException {
        if (Boolean.getBoolean(NO_PROCESS_LOCK_PROPERTY) == false)
            super.acquireProcessLock(msg, lockHandler);
    }

    public void prepare() throws IOException {}

    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException {
        File lockFile = new File(targetDirectory, lockFilename);
        writeLock = new FileConcurrencyLock(lockFile);
        writeLock.setApprover(this);
        writeLock.acquireLock(null, lockHandler, ownerName);
    }

    public void approveLock(ConcurrencyLock lock, String extraInfo)
            throws LockFailureException {
        List<String> filenames = FileUtils.listRecursively(targetDirectory,
            strategy.getFilenameFilter());
        for (String name : filenames) {
            // AAAFIX: need to test and make certain this is still working properly
            File f = new File(targetDirectory, name);
            if (!f.canWrite())
                throw new ReadOnlyLockFailureException();
        }
    }

    public void assertWriteLock() throws LockFailureException {
        if (writeLock == null)
            throw new NotLockedException();
        else
            writeLock.assertLock();
    }

    public URL doBackup(String qualifier) throws IOException {
        File result = strategy.getBackupHandler(targetDirectory).backup(
            qualifier);
        return result.toURI().toURL();
    }

    public boolean flushData() throws LockFailureException, IOException {
        return true;
    }

    public File getDirectory() {
        return targetDirectory;
    }

    public void releaseLocks() {
        if (writeLock != null)
            writeLock.releaseLock();
        if (processLock != null)
            processLock.releaseLock();

        // if the working directory was created solely for the purpose of
        // holding the lock and is now empty, delete the directory.
        String[] files = workingDirectory.list();
        if (files == null || files.length == 0)
            workingDirectory.delete();
    }

}
