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

package net.sourceforge.processdash.data.applet.js;



public class FormDataEvent {

    String id;
    String value;
    boolean readOnly;
    long timestamp;
    int coupon;

    public FormDataEvent(int coupon, String id, String value, boolean readOnly) {
        this.id = id;
        this.value = value;
        this.readOnly = readOnly;
        this.coupon = coupon;
        this.timestamp = System.currentTimeMillis();
    }

    public int getCoupon()      { return coupon;   }
    public String getId()       { return id;       }
    public boolean isReadOnly() { return readOnly; }
    public String getValue()    { return value;    }

    public String toString() {
        return "FormDataEvent[id=" + id +
            ",value=" + value +
            ",readOnly=" + readOnly +
            "]";
    }
}
