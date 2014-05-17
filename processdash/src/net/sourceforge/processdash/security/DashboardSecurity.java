// Copyright (C) 2004-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.security;

import java.net.URL;
import java.security.Policy;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.RuntimeUtils;


public class DashboardSecurity {

    private static final String DISABLE_SECURITY_MANAGER_PROPERTY =
        "net.sourceforge.processdash.disableSecurityManager";
    private static final String SECURE_CODEBASE_URL =
        "process.dashboard.codebase.url";
    private static final String JAVA_SECURITY_POLICY = "java.security.policy";

    private static final DashboardPermission SETUP_SECURITY_MANAGER_PERMISSION =
        new DashboardPermission("setupSecurityManager");
    private static final DashboardPermission RUNTIME_JVM_ARGS_PERMISSION =
        new DashboardPermission("runtimeUtils.setJvmArgs");

    private static final Logger logger = Logger.getLogger(DashboardSecurity.class.getName());

    public static void setupSecurityManager() {
        SETUP_SECURITY_MANAGER_PERMISSION.checkPermission();

        if (Boolean.getBoolean(DISABLE_SECURITY_MANAGER_PROPERTY)) {
            logger.warning("Security Manager disabled / not installed.");
            return;
        }

        URL policyURL = DashboardSecurity.class.getResource("security.policy");
        if (policyURL == null) {
            logger.severe("No security policy found - security manager not installed.");
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
            System.setProperty(SECURE_CODEBASE_URL, baseURLStr);
            logger.fine(SECURE_CODEBASE_URL + "=" + baseURLStr);
            System.setProperty(JAVA_SECURITY_POLICY, policyURLStr);
            logger.fine(JAVA_SECURITY_POLICY + "=" + policyURLStr);
            Policy.getPolicy().refresh();

            System.setSecurityManager(new DashboardSecurityManager());
            DashboardPermission.enableChecking();

            RuntimeUtils.addPropagatedSystemProperty("java.security.manager",
                "default");
            RuntimeUtils.addPropagatedSystemProperty(SECURE_CODEBASE_URL,
                baseURLStr);
            RuntimeUtils.addPropagatedSystemProperty(JAVA_SECURITY_POLICY,
                policyURLStr);
            RuntimeUtils.setJvmArgsPermission(RUNTIME_JVM_ARGS_PERMISSION);
        } catch (Exception e) {
            logger.severe("Caught exception - security manager not installed.");
            e.printStackTrace();
        }
    }

}
