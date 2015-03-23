// Copyright (C) 2005-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;


public class LegacySupport {

    public static void fixupV13ScriptIDs(DashHierarchy hierarchy) {
        Hashtable brokenIDs = new Hashtable();
        brokenIDs.put("pspForEng/2A/script.htm", "PSP0.1-PFE-2A");
        brokenIDs.put("pspForEng/4A/script.htm", "PSP1-PFE-4A");
        brokenIDs.put("pspForEng/5A/script.htm", "PSP1.1-PFE-5A");
        brokenIDs.put("pspForEng/7A/script.htm", "PSP2-PFE-7A");
        brokenIDs.put("pspForEng/8A/script.htm", "PSP2.1-PFE-8A");
        brokenIDs.put("pspForMSE/2A/script.htm", "PSP0.1-MSE-2A");
        brokenIDs.put("pspForMSE/3B/script.htm", "PSP1-MSE-3B");
        brokenIDs.put("pspForMSE/4B/script.htm", "PSP1.0.1-MSE-4B");

        Prop        value;
        String      s;
        for (Iterator i = hierarchy.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            value = (Prop)e.getValue();
            if (! Prop.hasValue(value.getID())) continue;
            if (! Prop.hasValue(s = value.getScriptFile ())) continue;
            s = (String) brokenIDs.get(s);
            if (s != null) value.setID(s);
        }
    }

}
