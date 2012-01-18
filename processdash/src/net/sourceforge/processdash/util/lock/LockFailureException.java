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

/** General exception class indicating failure to obtain the lock */
public class LockFailureException extends Exception {

    public LockFailureException() {
        super("Unable to Acquire Lock");
    }

    public LockFailureException(String message) {
        super(message);
    }

    public LockFailureException(Throwable cause) {
        super("Unable to Acquire Lock", cause);
    }

    /**
     * Returns true if the occurrence of this exception indicates that no lock
     * is in place.
     */
    public boolean isFatal() {
        return true;
    }

}
