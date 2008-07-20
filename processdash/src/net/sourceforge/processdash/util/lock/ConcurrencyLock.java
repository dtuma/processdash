// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util.lock;

public interface ConcurrencyLock {

    public void setApprover(ConcurrencyLockApprover approver);

    public String getLockToken();

    /**
     * Attempt to acquire a new lock.
     * 
     * @param extraInfo
     * @throws LockFailureException
     */
    public void acquireLock(String extraInfo) throws LockFailureException;

    public boolean isLocked();

    public String getExtraInfo();

    public void assertLock() throws LockFailureException;

    public void releaseLock(boolean deleteMetadata);

}
