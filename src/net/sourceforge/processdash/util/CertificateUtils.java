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

package net.sourceforge.processdash.util;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

public class CertificateUtils {

    public static String getSignerSimpleName(Certificate... certChain) {
        if (certChain == null || certChain.length == 0)
            return null;
        Certificate cert = certChain[0];
        if (cert instanceof X509Certificate) {
            X509Certificate x509 = (X509Certificate) cert;
            return getCNName(x509.getSubjectDN().getName());
        } else {
            return null;
        }
    }

    public static String getCNName(String dn) {
        try {
            LdapName ldapName = new LdapName(dn);
            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

}
