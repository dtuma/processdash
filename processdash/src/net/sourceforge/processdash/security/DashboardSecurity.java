// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.security;

import java.net.URL;
import java.security.AccessController;

import net.sourceforge.processdash.ProcessDashboard;


public class DashboardSecurity {

    private static final String DISABLE_SECURITY_MANAGER_PROPERTY =
        "net.sourceforge.processdash.disableSecurityManager";


    public static void setupSecurityManager() {
        if  (System.getSecurityManager() != null)
            AccessController.checkPermission
                (new DashboardPermission("setupSecurityManager"));

        if (Boolean.getBoolean(DISABLE_SECURITY_MANAGER_PROPERTY)) {
            System.out.println("Security Manager disabled / not installed.");
            return;
        }

        URL policyURL = DashboardSecurity.class.getResource("security.policy");
        if (policyURL == null) {
            System.err.println("No security policy found - security manager not installed.");
            return;
        }
        String policyURLStr = policyURL.toString();

        String baseURLStr = policyURLStr;
        String packageName = "/" +
            DashboardSecurity.class.getPackage().getName().replace('.', '/');
        int baseURLPos = policyURLStr.indexOf(packageName);
        if (baseURLPos != -1)
            baseURLStr = policyURLStr.substring(0, baseURLPos+1);
        if (baseURLStr.startsWith("jar:") && baseURLStr.indexOf("!/") != -1)
                baseURLStr = baseURLStr.substring(4, baseURLStr.indexOf("!/"));

        try {
            System.setProperty("process.dashboard.codebase.url", baseURLStr);
            // System.out.println("process.dashboard.codebase.url="+baseURLStr);
            System.setProperty("java.security.policy", policyURLStr);
            // System.out.println("java.security.policy="+policyURLStr);
            System.setSecurityManager(new SecurityManager());
        } catch (Exception e) {
            System.out.println("Caught exception - security manager not installed.");
            e.printStackTrace();
        }
    }

}
