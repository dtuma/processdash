// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

/**
 * Enumeration constants indicating the offline status of a resource collection
 * lock.
 */
public enum OfflineLockStatus {

    /**
     * No lock is currently held on a particular resource collection, so it
     * cannot be enabled for offline use.
     */
    NotLocked,

    /**
     * Offline lock mode is not supported by a particular resource collection.
     */
    Unsupported,

    /**
     * Offline lock mode is supported by this resource collection, but the
     * collection is not currently locked for offline use.
     */
    Disabled,

    /**
     * A resource collection is locked and the lock is enabled for offline use.
     */
    Enabled

}
