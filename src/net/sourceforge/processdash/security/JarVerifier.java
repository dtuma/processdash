// Copyright (C) 2003-2023 Tuma Solutions, LLC
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
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.FileUtils;

public class JarVerifier {

    private static byte[] buffer = new byte[8192];

    private static Certificate LAST_VALID_CERT = null;

    static CertificateTrustSource EXTERNAL_TRUST_SOURCE = null;

    private static final Logger logger = Logger
            .getLogger(JarVerifier.class.getName());


    public static boolean verify(File jarFile) throws IOException {
        JarFile jar = null;
        try {
            // open the jar in verification mode
            jar = new JarFile(jarFile, true);

            // verify all the entries within
            return verify(jar);

        } catch (IOException ioe) {
            throw ioe;

        } catch (Exception ex) {
            logger.log(Level.WARNING,
                "Error verifying jar file " + jarFile.getPath(), ex);
            return false;

        } finally {
            FileUtils.safelyClose(jar);
        }
    }


    public static boolean verify(JarFile jar) throws IOException {
        // scan the entries and verify each one
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!verify(jar, entry))
                return false;
        }

        // all entries verified successfully
        return true;
    }


    public static boolean verify(JarFile jar, JarEntry entry)
            throws IOException {
        if (isIgnoredEntry(entry))
            return true;

        InputStream in = null;
        try {
            // the entry must be read completely to validate signatures
            in = jar.getInputStream(entry);
            while (in.read(buffer) != -1)
                ;

            // verify the signatures on the entry
            return verify(entry);

        } catch (IOException ioe) {
            throw ioe;

        } catch (SecurityException se) {
            logger.warning("Invalid signature on entry " + entry.getName()
                    + " in jar file " + jar.getName());
            return false;

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error verifying entry " + entry.getName()
                    + " in jar file " + jar.getName(),
                ex);
            return false;

        } finally {
            FileUtils.safelyClose(in);
        }
    }


    public static boolean verify(JarEntry entry) {
        if (isIgnoredEntry(entry))
            return true;
        else
            return verify(entry.getCertificates());
    }


    public static boolean verify(Certificate[] certs) {
        return verify(certs, true);
    }

    public static boolean verify(Certificate[] certs, boolean includeExternal) {
        // if no certs were provided, return false
        if (certs == null || certs.length == 0)
            return false;

        // scan the list of certs to see if one is trusted
        for (Certificate c : certs) {
            // optimization: if this is the exact same Java object as the
            // previously verified cert, it's still valid
            if (c != null && c == LAST_VALID_CERT) {
                return true;
            }

            // see if this cert is in our list of trusted certs
            for (Certificate s : TRUSTED_CERTS) {
                if (s.equals(c)) {
                    LAST_VALID_CERT = c;
                    return true;
                }
            }

            // if a secondary trust provider is in effect, ask it to test the
            // unrecognized cert
            if (includeExternal //
                    && EXTERNAL_TRUST_SOURCE != null //
                    && EXTERNAL_TRUST_SOURCE.isTrusted(c)) {
                LAST_VALID_CERT = c;
                return true;
            }
        }

        // we did not find a trusted cert
        return false;
    }


    private static boolean isIgnoredEntry(JarEntry e) {
        if (e.isDirectory())
            return true;

        if (e.getName().toUpperCase().startsWith(META_INF_PREFIX))
            return true;

        return false;
    }


    /**
     * A list of the certs that are trusted by the Process Dashboard
     */
    private static final List<Certificate> TRUSTED_CERTS = loadTrustedCerts();

    private static List<Certificate> loadTrustedCerts() {
        List<Certificate> result = new ArrayList();

        // read certs from the keystore that is bundled within pspdash.jar
        readCerts(JarVerifier.class.getResourceAsStream(DASH_KEYSTORE),
            DASH_KEYSTORE_TYPE, DASH_KEYSTORE_DESCR, result);

        return Collections.unmodifiableList(result);
    }

    private static void readCerts(InputStream in, String keystoreType,
            String description, List<Certificate> result) {
        try {
            KeyStore ks = KeyStore.getInstance(keystoreType);
            ks.load(in, null);

            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isCertificateEntry(alias))
                    result.add(ks.getCertificate(alias));
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to read keystore " + description,
                ex);

        } finally {
            FileUtils.safelyClose(in);
        }
    }


    private static final String DASH_KEYSTORE = "keystore";

    private static final String DASH_KEYSTORE_TYPE = "JKS";

    private static final String DASH_KEYSTORE_DESCR = "in pspdash.jar";

    private static final String META_INF_PREFIX = "META-INF/";

}
