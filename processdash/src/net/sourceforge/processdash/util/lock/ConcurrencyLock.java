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
