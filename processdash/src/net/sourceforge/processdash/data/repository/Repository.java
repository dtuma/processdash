// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
