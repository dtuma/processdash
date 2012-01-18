// Copyright (C) 2008-2012 Tuma Solutions, LLC
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
 * Exception thrown if we cannot determine whether a lock is valid. This
 * typically occurs if the lock file is in a network directory that is not
 * currently reachable.
 */
public class LockUncertainException extends LockFailureException {

    public LockUncertainException() {
        super("Unable to determine validity of lock");
    }

    public LockUncertainException(Throwable cause) {
        super("Unable to determine validity of lock");
        initCause(cause);
    }

    @Override
    public boolean isFatal() {
        return false;
    }

}
