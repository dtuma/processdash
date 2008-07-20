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

/**
 * Exception indicating that the lock could not be obtained because some other
 * process owns it, and we were unable to contact them.
 */
public class AlreadyLockedException extends LockFailureException {

    private String extraInfo;

    public AlreadyLockedException(String extraInfo) {
        super("Already Locked" + (extraInfo != null ? " by " + extraInfo : ""));
        this.extraInfo = extraInfo;
    }

    /**
     * Get the extra info that was written into the lock file by the owner of
     * this lock. If no extra information was provided by the owner of the lock,
     * returns null.
     */
    public String getExtraInfo() {
        return extraInfo;
    }


}
