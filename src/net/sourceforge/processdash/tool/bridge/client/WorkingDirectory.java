// Copyright (C) 2008-2012 Tuma Solutions, LLC
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
import java.net.URL;

import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

public interface WorkingDirectory {

    /**
     * Return a human-readable, user-friendly description of the location
     * this data is coming from.
     */
    public String getDescription();

    /**
     * Generally, it would confuse the user to have two processes open on the
     * local computer, both displaying the same data. Calling this method
     * peforms a coordination process to ensure that this does not occur.
     * 
     * This method will return normally if a lock was sucessfully obtained;
     * otherwise, it will throw an appropriate exception.
     * 
     * @param message
     *                if another process has the lock, this message will be sent
     *                to that process.
     * @param localHandler
     *                an object in the local process which can respond to
     *                messages from other processes when they attempt to acquire
     *                the same process lock.
     * 
     * @throws SentLockMessageException
     *                 if we successfully contacted some other process that
     *                 currently holds the coordination lock on this data.
     * @throws LockFailureException
     *                 if we were unable to perform the coordination process for
     *                 any other reason.
     */
    public void acquireProcessLock(String message,
            LockMessageHandler localHandler) throws SentLockMessageException,
            LockFailureException;

    /**
     * Prepare the contents of this working directory for use.
     * 
     * This method would be called after acquiring a process lock.
     * 
     * @throws IllegalStateException
     *                 if a process lock has not been acquired
     * @throws IOException
     *                 if we were unable to prepare the directory for some
     *                 reason
     */
    public void prepare() throws IOException;

    /**
     * Returns the directory on the filesystem where working files are stored.
     * 
     * @return the directory on the filesystem where working files are stored.
     */
    public File getDirectory();

    /**
     * Get the filesystem location of permanent data storage.
     * 
     * @return
     */
    public File getTargetDirectory();

    /**
     * To prevent data corruption, only one process at a time is allowed to
     * modify the data in a particular collection. Calling this method attempts
     * to acquire the lock that grants that permission.
     * 
     * This method will return normally if a lock was sucessfully obtained;
     * otherwise, it will throw an appropriate exception.
     * 
     * @param lockHandler
     *                an object in the local process which can be alerted if the
     *                write lock is lost.
     * @param ownerName
     *                the name of the person requesting the lock. (When someone
     *                else requests the same lock, this string will be used to
     *                tell them who currently owns the lock.)
     * @throws AlreadyLockedException
     *                 if some other person owns the write lock
     * @throws LockFailureException
     *                 if we were unable to obtain the write lock for any other
     *                 reason.
     */
    public void acquireWriteLock(LockMessageHandler lockHandler,
            String ownerName) throws AlreadyLockedException,
            LockFailureException;

    /**
     * Make certain that we still own the write lock on this directory.
     * 
     * @throws LockFailureException if we have lost the lock
     */
    public void assertWriteLock() throws LockFailureException;

    /**
     * Release the write lock on this directory.
     * 
     * This method does not release the process lock; to accomplish that, use
     * {@link #releaseLocks()}.
     */
    public void releaseWriteLock();

    /**
     * Make certain all buffered data has been saved persistently.
     * 
     * @return true if all data has been successfully flushed.
     * @throws LockFailureException
     *                 if the current process does not own a lock on this data
     *                 collection
     * @throws IOException
     *                 if a communications failure prevents the operation from
     *                 completing
     */
    public boolean flushData() throws LockFailureException, IOException;

    /**
     * Update the contents of this working directory with changes made elsewhere
     * (by some other process which owns the write lock).
     * 
     * @throws IllegalStateException
     *             if an update operation is not appropriate based on the
     *             operational state of this collection
     * @throws IOException
     *             if a communications failure prevents the operation from
     *             completing
     */
    public void update() throws IllegalStateException, IOException;

    /**
     * Backup the data in this collection.
     * 
     * @param qualifier
     *                a short identifier describing the occasion for this
     *                backup.
     * @throws IOException
     *                 if a communications failure prevents the operation from
     *                 completing
     * @return a URL that could be used to read the backup, if desired
     */
    public URL doBackup(String qualifier) throws IOException;


    /**
     * In preparation for application shutdown, release the locks held by this
     * process.
     * 
     * @see #acquireProcessLock(LockMessageHandler)
     * @see #acquireWriteLock(String)
     */
    public void releaseLocks();

}
