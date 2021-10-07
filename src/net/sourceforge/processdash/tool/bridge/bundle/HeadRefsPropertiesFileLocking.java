// Copyright (C) 2021 Tuma Solutions, LLC
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
import java.util.Collection;

import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class HeadRefsPropertiesFileLocking extends HeadRefsPropertiesFile {

    private FileConcurrencyLock lock;

    public HeadRefsPropertiesFileLocking(File file) {
        this(file, new File(file.getParentFile(),
                file.getName().replace('.', ',') + "-lock.tmp"));
    }

    public HeadRefsPropertiesFileLocking(File file, File lockFile) {
        super(file);
        this.lock = new FileConcurrencyLock(lockFile);
    }


    @Override
    public synchronized void storeHeadRefs(Collection<FileBundleID> headRefs)
            throws IOException {
        // acquire a lock to ensure that only one process modifies the file
        // at a time
        acquireLock();
        try {
            // now that we've got the lock, call our superclass to do the work
            super.storeHeadRefs(headRefs);
        } finally {
            // release the lock when we're done
            releaseLock();
        }
    }

    private void acquireLock() throws IOException {
        int errCount = 0;
        while (true) {
            try {
                lock.acquireLock(null);
                return;
            } catch (LockFailureException lfe) {
                retryOrAbort(errCount++, lfe, //
                    50, 100, 100, 250, 500, 500, 1000, 2000, 2000, 3000);
            }
        }
    }

    private void releaseLock() {
        lock.releaseLock(true);
    }

}
