// Copyright (C) 2000-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;

import net.sourceforge.processdash.data.*;


/**
 * The Repository remote interface allows an object to access data values
 * in the repository and listen for changes in those values.
 */
public interface Repository {

    public void putValue(String name, SaveableData value)
        throws RemoteException;
    public void removeValue(String name) throws RemoteException;
    public void maybeCreateValue(String name, String value, String prefix)
        throws RemoteException;

    public void addDataListener(String name, DataListener dl)
        throws RemoteException;
    public void removeDataListener(String name, DataListener dl)
        throws RemoteException;
}
