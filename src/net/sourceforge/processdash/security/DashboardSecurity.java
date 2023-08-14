// Copyright (C) 2004-2023 Tuma Solutions, LLC
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.Policy;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.RuntimeUtils;


public class DashboardSecurity {

    private static final String DISABLE_SECURITY_MANAGER_PROPERTY =
        "net.sourceforge.processdash.disableSecurityManager";
    private static final String SECURITY_POLICY_PROPERTY =
            "net.sourceforge.processdash.securityPolicy";
    private static final String PREF_NODE =
        "/net/sourceforge/processdash/userPrefs";
    private static final String POLICY_PREF = "securityPolicy";
    private static final boolean SECURITY_MANAGER_SUPPORTED =
        isSecurityManagerSupportPresent();
    private static final String KEYSTORE_FILENAME = "third-party-certs.p12";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PASSWORD = "processdash";
    private static final String SECURE_CODEBASE_URL =
        "process.dashboard.codebase.url";
    private static final String JAVA_SECURITY_POLICY = "java.security.policy";

    private static final DashboardPermission SETUP_SECURITY_MANAGER_PERMISSION =
        new DashboardPermission("setupSecurityManager");
    private static final DashboardPermission RUNTIME_JVM_ARGS_PERMISSION =
        new DashboardPermission("runtimeUtils.setJvmArgs");

    private static final Logger logger = Logger.getLogger(DashboardSecurity.class.getName());


    private enum DashPolicy {
        manager, jar, none
    }

    private static DashPolicy POLICY;


    public static void setupSecurityManager() throws SecurityException {
        SETUP_SECURITY_MANAGER_PERMISSION.checkPermission();

        loadEnforcementPolicy();

        if (POLICY == DashPolicy.manager)
            installSecurityManager();
        else if (POLICY == DashPolicy.jar)
            installCertificateManager();
    }

    private static void loadEnforcementPolicy() {
        // disable security if requested by a specific system property
        if (Boolean.getBoolean(DISABLE_SECURITY_MANAGER_PROPERTY)) {
            logger.warning("Security Manager disabled / not installed.");
            POLICY = DashPolicy.none;
            return;
        }

        // jar file checking is the default policy
        POLICY = DashPolicy.jar;

        // choose alternate policy if requested in user settings
        try {
            Preferences prefs = Preferences.userRoot().node(PREF_NODE);
            String pref = prefs.get(POLICY_PREF, null);
            if (pref == null)
                pref = System.getProperty(SECURITY_POLICY_PROPERTY);
            if (pref != null)
                POLICY = DashPolicy.valueOf(pref.toLowerCase());

            // ignore user's request for manager if JVM doesn't support it
            if (POLICY == DashPolicy.manager && !SECURITY_MANAGER_SUPPORTED)
                POLICY = DashPolicy.jar;

        } catch (Exception e) {
        }

        // log the security policy setting
        logger.info("Process Dashboard add-on security policy = " + POLICY);
    }

    private static boolean isSecurityManagerSupportPresent() {
        try {
            String javaVersionString = System.getProperty("java.version");
            String firstDigitString = javaVersionString.split("\\.")[0];
            int firstDigit = Integer.parseInt(firstDigitString);
            return (firstDigit < 18);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSecurityManagerSupported() {
        return SECURITY_MANAGER_SUPPORTED;
    }

    private static void installSecurityManager() {
        URL policyURL = DashboardSecurity.class.getResource("security.policy");
        if (policyURL == null)
            throw new SecurityException("Security policy file not found");
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
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    private static void installCertificateManager() {
        File appDir = DirectoryPreferences.getApplicationDirectory();
        File appTemplateDir = new File(appDir, "Templates");
        File ksFile = new File(appTemplateDir, KEYSTORE_FILENAME);
        CertificateManager mgr = new CertificateManager(ksFile, KEYSTORE_TYPE,
                KEYSTORE_PASSWORD.toCharArray());
        JarVerifier.EXTERNAL_TRUST_SOURCE = mgr;
    }


    public static CertificateManager getCertificateManager() {
        return (CertificateManager) JarVerifier.EXTERNAL_TRUST_SOURCE;
    }


    public static boolean checkJar(JarFile jarFile) throws IOException {
        if (POLICY != DashPolicy.jar)
            return true;
        else
            return JarVerifier.verify(jarFile);
    }

}
