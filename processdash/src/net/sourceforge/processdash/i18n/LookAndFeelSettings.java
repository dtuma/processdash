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

package net.sourceforge.processdash.i18n;


import javax.swing.UIManager;


public class LookAndFeelSettings {

    public static void loadLocalizedSettings() {
        try {
            Resources r = Resources.getDashBundle("ProcessDashboard");
            String[] settings = r.getStrings("Look_And_Feel_Changes_");
            for (int i = 0;   i < settings.length;   i++) {
                int pos = settings[i].indexOf('=');
                if (pos == -1) continue;

                String dest = settings[i].substring(0, pos);
                String src = settings[i].substring(pos+1);
                UIManager.put(dest, UIManager.get(src));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
