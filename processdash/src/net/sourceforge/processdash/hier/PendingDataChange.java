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

package net.sourceforge.processdash.hier;

public class PendingDataChange {
    public static final int CREATE = 0;   // also copy file
    public static final int DELETE = 1;
    public static final int CHANGE = 2;

    public static final int NOT_MERGED = 0;
    public static final int MERGED = 1;
    public static final int CANCELLED = 2;

    public String srcFile;
    public String destFile;
    public String newPrefix;
    public String oldPrefix;
    public int    changeType;

    private PendingDataChange (String src,
                               String dest,
                               String newPre,
                               String oldPre,
                               int typeOfChange) {
        srcFile    = src;
        destFile   = dest;
        newPrefix  = newPre;
        oldPrefix  = oldPre;
        changeType = typeOfChange;
    }

    /** Create a pending data change for the creation of a new datafile.
     */
    public PendingDataChange (String src,
                              String dest,
                              String newPre) {
        this (src, dest, newPre, null, CREATE);
    }

    /** Create a pending data change for the renaming of a datafile.
     */
    public PendingDataChange (String newPre,
                              String oldPre) {
        this (null, null, newPre, oldPre, CHANGE);
    }

    /** Create a pending data change for the deletion of a datafile.
     */
    public PendingDataChange (String pre) {
        this (null, null, null, pre, DELETE);
    }

    public String toString() {
        return ("PendingDataChange["+
                "srcFile='"+srcFile+"', destFile='"+destFile+
                "', oldPrefix='"+oldPrefix+", newPrefix='"+newPrefix+
                ", changeType='"+(changeType == CREATE ? "CREATE" :
                                  (changeType == DELETE ? "DELETE" : "CHANGE"))
                +"']");
    }


    /** Try to merge this change with a new change.
     * @return NOT_MERGED if the merge could not be made; MERGED if the
     * merge was successful; or CANCELLED if the two changes cancel each
     * other out */
    public int mergeChange(PendingDataChange newChange) {
        if (newChange == null)
            return MERGED;    // nothing to do!

        if (newChange.changeType == CREATE || this.changeType == DELETE)
            return NOT_MERGED; // we can't merge with a subsequently created item.

        // we can only merge if our "newPrefix" equals the "oldPrefix" of
        // the new change.
        if (!newChange.oldPrefix.equals(this.newPrefix))
            return NOT_MERGED;

        if (newChange.changeType == CHANGE) {
            this.newPrefix = newChange.newPrefix;
            if (this.changeType == CHANGE && this.oldPrefix.equals(this.newPrefix))
                return CANCELLED;
            else
                return MERGED;
        }

        if (newChange.changeType == DELETE) {

            if (this.changeType == CREATE)
                return CANCELLED;

            else if (this.changeType == CHANGE) {
                this.newPrefix = null;
                this.changeType = DELETE;
                return MERGED;
            }
        }

        return NOT_MERGED;
    }
}
