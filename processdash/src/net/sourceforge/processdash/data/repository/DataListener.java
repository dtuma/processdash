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
