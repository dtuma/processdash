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
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.util.*;


/**
 ** class VectorQSort
 **
 ** This class performs a quicksort of the supplied vector, using a
 ** comparator implementing the ILessThan interface.
 **
 ** Although the current implementation sorts the Vector that was passed in,
 ** the user should use the Vector returned by getVector to ensure future
 ** compatibility.
 */
public class VectorQSort {
    private transient Vector    v;
    private transient ILessThan lt;

    public VectorQSort (Vector v, ILessThan lt) {
        this.v = v;
        this.lt = lt;
    }

    public void setVector (Vector v) {
        this.v = v;
    }

    public void setComparator (ILessThan lt) {
        this.lt = lt;
    }

    public Vector getVector () {
        return v;
    }

    public void sort () {
        sort (v, 0, v.size() - 1);
    }

    public void sort (int fromIndex, int toIndex) {
        sort (v, fromIndex, toIndex);
    }

    // the next 2 routines implement a quicksort of the vector.  The first two
    // routines are generic, using the passed in ILessThan interface object for
    // comparisons.
    private void sort(Vector rgo, int nLow0, int nHigh0) {
        int nLow = nLow0;
        int nHigh = nHigh0;

        Object oMid;

        if (nHigh0 > nLow0) {
            oMid = rgo.elementAt ( (nLow0 + nHigh0) / 2 );

            while(nLow <= nHigh) {
                while((nLow < nHigh0) && lt.lessThan(rgo.elementAt(nLow), oMid))
                    ++nLow;

                while((nLow0 < nHigh) && lt.lessThan(oMid, rgo.elementAt(nHigh)))
                    --nHigh;

                if(nLow <= nHigh) {
                    swap(rgo, nLow++, nHigh--);
                }
            }

            if(nLow0 < nHigh) sort(rgo, nLow0, nHigh);

            if(nLow < nHigh0) sort(rgo, nLow, nHigh0);
        }
    }

    private void swap(Vector rgo, int i, int j) {
        Object o;

        o = rgo.elementAt(i);
        rgo.setElementAt (rgo.elementAt(j), i);
        rgo.setElementAt (o, j);
    }

}
