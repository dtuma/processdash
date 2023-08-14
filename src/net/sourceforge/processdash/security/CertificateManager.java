// Copyright (C) 2023 Tuma Solutions, LLC
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;

public class CertificateManager implements CertificateTrustSource {

    private File keystoreFile;

    private String keystoreType;

    private char[] keystorePassword;

    private Map<Certificate, Boolean> certificates;

    private Certificate LAST_TESTED_CERT;

    private Boolean LAST_TESTED_VAL;

    private static final Logger logger = Logger
            .getLogger(CertificateManager.class.getName());


    public CertificateManager(File keystoreFile, String keystoreType,
            char[] keystorePassword) {
        this.keystoreFile = keystoreFile;
        this.keystoreType = keystoreType;
        this.keystorePassword = keystorePassword.clone();
        this.certificates = null;
    }


    public boolean isTrusted(Certificate cert) {
        if (cert == null) {
            return false;

        } else if (cert == LAST_TESTED_CERT) {
            return Boolean.TRUE.equals(LAST_TESTED_VAL);

        } else {
            Boolean result = getCertificatesImpl().get(cert);
            if (result == null) {
                result = Boolean.FALSE;
                addCert(cert, false);
            }
            LAST_TESTED_CERT = cert;
            LAST_TESTED_VAL = result;
            return result;
        }
    }


    public void addCert(Certificate cert, boolean trusted) {
        Boolean oldVal = getCertificatesImpl().put(cert, trusted);
        if (oldVal == null || oldVal.booleanValue() != trusted)
            save();
    }


    public void deleteCert(Certificate cert) {
        Boolean oldVal = getCertificatesImpl().remove(cert);
        if (oldVal != null)
            save();
    }


    public Map<Certificate, Boolean> getCertificates() {
        return Collections.unmodifiableMap(getCertificatesImpl());
    }


    private Map<Certificate, Boolean> getCertificatesImpl() {
        if (certificates == null)
            load();

        return certificates;
    }


    public void load() {
        InputStream in = null;
        try {
            // initialize empty certificate map
            certificates = Collections.synchronizedMap(new LinkedHashMap());

            // if the keystore file does not exist, do nothing
            if (!keystoreFile.isFile())
                return;

            // load the contents of the keystore
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            in = new FileInputStream(keystoreFile);
            keystore.load(in, keystorePassword);

            // read the keystore certificates and add them to our Map
            List<String> aliases = Collections.list(keystore.aliases());
            Collections.sort(aliases);
            for (String alias : aliases) {
                if (keystore.isCertificateEntry(alias)) {
                    Certificate cert = keystore.getCertificate(alias);
                    certificates.put(cert, alias.endsWith(TRUSTED_SUFFIX));
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to load user certificate keystore "
                    + keystoreFile + " of type " + keystoreType,
                ex);
        } finally {
            FileUtils.safelyClose(in);
        }
    }


    public void save() {
        try {
            // make sure the parent directory exists
            File parentDir = keystoreFile.getParentFile();
            if (!parentDir.isDirectory() && !parentDir.mkdirs())
                throw new FileNotFoundException(parentDir.getPath());

            // create an empty keystore
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(null, keystorePassword);

            // add our certificates to the new keystore
            int entryNum = 1;
            for (Entry<Certificate, Boolean> e : certificates.entrySet()) {
                String numStr = "000" + entryNum++;
                numStr = numStr.substring(numStr.length() - 4);
                String suffix = (Boolean.TRUE.equals(e.getValue())
                        ? TRUSTED_SUFFIX
                        : UNTRUSTED_SUFFIX);
                String alias = numStr + suffix;
                keystore.setCertificateEntry(alias, e.getKey());
            }

            // save the keystore to our output file
            RobustFileOutputStream out = new RobustFileOutputStream(
                    keystoreFile);
            keystore.store(out, keystorePassword);
            out.close();

        } catch (Exception ex) {
            logger.log(Level.SEVERE,
                "Unable to save user certificate keystore " + keystoreFile, ex);
        }
    }


    private static final String TRUSTED_SUFFIX = "-trusted";

    private static final String UNTRUSTED_SUFFIX = "-untrusted";

}
