// Copyright (C) 2021 Tuma Solutions, LLC
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

package teamdash.license;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import net.sourceforge.processdash.util.Base64;
import net.sourceforge.processdash.util.FileUtils;


final class StandaloneLicenseValidator {

    static boolean isValid(StandaloneLicense license) {
        return (license != null && signatureIsValid(license));
    }

    private static boolean signatureIsValid(StandaloneLicense license) {
        String xml = license.getXmlText();
        xml = normalizeXml(xml);

        String signatureText = license.getSignature();
        if (signatureText == null || signatureText.trim().length() == 0)
            return false;
        byte[] signature = Base64.decode(signatureText);

        return verifySignature(xml, signature);
    }

    static String normalizeXml(String xml) {
        // remove XML comments and XML processing instructions
        xml = removeAll(xml, "<!--", "-->");
        xml = removeAll(xml, "<?", "?>");

        // strip the electronic signature element
        xml = removeAll(xml, "<" + StandaloneLicense.SIGNATURE_TAG + ">", //
            "</" + StandaloneLicense.SIGNATURE_TAG + ">");

        // canonicalize and compress whitespace
        xml = xml.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        xml = xml.replaceAll("  +", " ");

        // remove whitespace next to XML tags
        xml = xml.replaceAll("> ", ">");
        xml = xml.replaceAll(" >", ">");
        xml = xml.replaceAll(" />", "/>");
        xml = xml.replaceAll(" <", "<");
        xml = xml.replaceAll("< ", "<");

        return xml;
    }

    private static String removeAll(String xml, String beg, String end) {
        while (xml.length() > 0) {
            int endPos = xml.indexOf(end);
            if (endPos == -1)
                return xml;
            int begPos = xml.lastIndexOf(beg, endPos);
            if (begPos == -1)
                return xml;
            xml = xml.substring(0, begPos)
                    + xml.substring(endPos + end.length());
        }
        return xml;
    }


    private synchronized static boolean verifySignature(String xml,
            byte[] signature) {
        try {
            // create a signing engine for future use
            Signature verificationEngine = Signature.getInstance("SHA1withRSA");

            // Read the raw binary public key data
            InputStream in = StandaloneLicenseValidator.class
                    .getResourceAsStream("license-signing-public-key.der");
            byte[] rawKeyData = FileUtils.slurpContents(in, true);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                    rawKeyData);

            // construct the public key from the raw data and return it
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey licenseSigningKey = kf.generatePublic(publicKeySpec);

            // verify the signature and return the result
            verificationEngine.initVerify(licenseSigningKey);
            verificationEngine.update(xml.getBytes("UTF-8"));
            return verificationEngine.verify(signature);

        } catch (Exception e) {
            System.err.println("Cannot verify license signature");
            e.printStackTrace();
            return false;
        }
    }

}
