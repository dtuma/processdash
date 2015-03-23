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


import java.util.Vector;


/**
 * The DataListener remote interface allows an object to receive notification
 * when the value of a piece of named data changes.
 */
public interface DataListener {

    /**
     * Procedure called to notify the DataListener that a data value has
     * changed.
     * @param e information about the piece of data that changed.
     * @exception RemoteException if a communication failure occurs.
     */
    public void dataValueChanged(DataEvent e) throws RemoteException;

    /**
     * Procedure called to notify the DataListener that several data values
     * have changed.
     * @param v list of DataEvents with information about the pieces of data
     * that changed.
     * @exception RemoteException if a communication failure occurs.
     */
    public void dataValuesChanged(Vector v) throws RemoteException;
}
