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

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

public abstract class AbstractWorkingDirectory implements WorkingDirectory {

    protected File targetDirectory;

    protected String remoteURL;

    protected String lockFilename;

    protected File workingDirectory;

    protected FileConcurrencyLock processLock;

    protected AbstractWorkingDirectory(File targetDirectory, String remoteURL,
            String lockFilename, File workingDirectoryParent) {
        this.targetDirectory = targetDirectory;
        this.remoteURL = remoteURL;
        this.lockFilename = lockFilename;

        this.workingDirectory = new File(workingDirectoryParent, getWorkingId());

        File lockFile = new File(workingDirectory, lockFilename);
        this.processLock = new FileConcurrencyLock(lockFile);
        this.processLock.setListenForLostLock(false);
    }

    protected String getWorkingId() {
        if (targetDirectory != null) {
            String path = targetDirectory.getAbsolutePath();
            return FileUtils.makeSafeIdentifier(path);

        } else {
            String url = remoteURL;
            if (url.startsWith("https"))
                url = "http" + url.substring(5);
            return FileUtils.makeSafeIdentifier(url);
        }
    }

    public String getDescription() {
        if (remoteURL != null)
            return remoteURL;
        else
            return targetDirectory.getPath();
    }

    public void acquireProcessLock(LockMessageHandler lockHandler)
            throws SentLockMessageException, LockFailureException {
        workingDirectory.mkdirs();
        processLock.acquireLock(ACTIVATE_MESSAGE, lockHandler, null);
    }

    public static final String NO_PROCESS_LOCK_PROPERTY =
        WorkingDirectory.class.getName() + ".noProcessLock";
}
