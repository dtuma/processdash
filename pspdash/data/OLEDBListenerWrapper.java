// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// Foundation, Inc., 59 Temple Place -Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import com.ms.osp.*;
import com.ms.security.*;

public class OLEDBListenerWrapper implements OLEDBSimpleProviderListener
{
    OLEDBSimpleProviderListener listener;

    public OLEDBListenerWrapper(OLEDBSimpleProviderListener l) {
        listener = l;
    }

    public void aboutToChangeCell(int iRow, int iColumn) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.aboutToChangeCell(iRow, iColumn);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.aboutToChangeCell caught exception: "
                 + e);
        }
    }
    public void cellChanged(int iRow, int iColumn) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.cellChanged(iRow, iColumn);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.cellChanged caught exception: " + e);
        }
    }
    public void aboutToDeleteRows(int iRow, int cRows) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.aboutToDeleteRows(iRow, cRows);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.aboutToDeleteRows caught exception: "
                 + e);
        }
    }
    public void deletedRows(int iRow, int cRows) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.deletedRows(iRow, cRows);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.deletedRows caught exception: " + e);
        }
    }
    public void aboutToInsertRows(int iRow, int cRows) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.aboutToInsertRows(iRow, cRows);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.aboutToInsertRows caught exception: "
                 + e);
        }
    }
    public void insertedRows(int iRow, int cRows) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.insertedRows(iRow, cRows);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.insertedRows caught exception: " + e);
        }
    }
    public void rowsAvailable(int iRow, int cRows) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.rowsAvailable(iRow, cRows);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.rowsAvailable caught exception: " + e);
        }
    }
    public void transferComplete(int xfer) {
        try {
            PolicyEngine.assertPermission(PermissionID.SYSTEM);
            listener.transferComplete(xfer);
        } catch (Exception e) {
            System.err.println
                ("OLEDBListenerWrapper.transferComplete caught exception: "
                 + e);
        }
    }
}
