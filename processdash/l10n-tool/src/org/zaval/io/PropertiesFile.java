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

package org.zaval.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;


/** This class provides several small enhancements to the standard 
 * Properties class.
 */

public class PropertiesFile extends Properties {

    public synchronized Enumeration keys() {
        // return the list of keys in sorted order.  This will cause the
        // of the Properties.store method to store items in sorted order.
        // (This is something of a hack, since we have no guarantee that
        // Properties.store() will really call this method...but at least
        // the Sun Microsystems implementation currently does.)
        ArrayList l = Collections.list(super.keys());
        Collections.sort(l);
        return Collections.enumeration(l);
    }

}
