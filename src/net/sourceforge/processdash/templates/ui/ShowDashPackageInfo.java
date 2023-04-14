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

package net.sourceforge.processdash.templates.ui;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.security.CertificateManager;
import net.sourceforge.processdash.security.DashboardSecurity;
import net.sourceforge.processdash.security.JarVerifier;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.CertificateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class ShowDashPackageInfo extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.AddOn");

    private enum Status {
        Active, Rejected
    }

    private enum SignatureType {
        Internal, ThirdParty, Unrecognized, Unsigned
    }

    private String pkgId;

    private DashPackage pkg;

    private Status status;

    private SignatureType sigType;

    private boolean isManagingCerts, isTrusted;

    private void init() throws TinyCGIException {
        // get the ID of the package the user wants to display
        pkgId = getParameter(PKGID);
        if (!StringUtils.hasValue(pkgId))
            throw new TinyCGIException(400, "No pkgId parameter provided");

        // look up the package, and determine whether it is active or rejected
        pkg = TemplateLoader.getPackage(pkgId);
        status = Status.Active;
        if (pkg == null) {
            pkg = TemplateLoader.getRejectedPackage(pkgId);
            status = Status.Rejected;
        }
        if (pkg == null) {
            throw new TinyCGIException(404,
                    "No package found with ID " + pkgId);
        }

        // identify whether a certificate manager is in place
        CertificateManager mgr = DashboardSecurity.getCertificateManager();
        isManagingCerts = (mgr != null);

        // identify the type of signature on the package, and whether it has
        // been approved as a trusted source
        isTrusted = (status == Status.Active);
        if (pkg.signedBy == null || pkg.signedBy.length == 0) {
            sigType = SignatureType.Unsigned;
        } else if (JarVerifier.verify(pkg.signedBy, false)) {
            sigType = SignatureType.Internal;
        } else if (!(pkg.signedBy[0] instanceof X509Certificate)) {
            sigType = SignatureType.Unrecognized;
        } else {
            sigType = SignatureType.ThirdParty;
            if (isManagingCerts)
                isTrusted = Boolean.TRUE.equals( //
                    mgr.getCertificates().get(pkg.signedBy[0]));
        }
    }


    @Override
    protected void writeContents() throws IOException {
        // read initial state for this package
        init();

        // write the HTML header
        out.println("<html><head>");
        printRes("<title>${Title}</title>");
        out.println("<link rel='stylesheet' type='text/css' href='/style.css'>");
        out.println("<style>");
        out.println("  td { vertical-align: baseline }");
        out.println("  td.th { font-weight: bold; white-space: nowrap }");
        out.println("</style>");
        out.println("</head><body>");

        // print a header
        printRes("<h1>${Title}</h1>");

        // start a table of data
        out.println("<table cellspacing='10'>");

        // print a table row with the name of the package
        printRes("<tr><td class='th'>${ConfigScript.Add_On.Name}</td>");
        out.println("<td>" + esc(pkg.name) + "</td></tr>");

        // print a table row with the version number of the package
        printRes("<tr><td class='th'>${ConfigScript.Add_On.Version}</td>");
        out.println("<td>" + esc(pkg.version) + "</td></tr>");

        // if the package came from a file, print file details
        if (StringUtils.hasValue(pkg.filename)) {
            printRes("<tr><td class='th'>${ConfigScript.Add_On.Filename}</td>");
            out.println("<td><tt>" + esc(pkg.filename) + "</tt></td></tr>");

            try {
                String md5 = FileUtils.computeMD5(new File(pkg.filename));
                printRes("<tr><td class='th'>${Hash.Header}</td>");
                out.println("<td><tt>" + md5 + "</tt></td></tr>");
            } catch (IOException ioe) {
            }
        }

        // print the installation and approval status of the package
        String statusResKey;
        if (isManagingCerts && sigType == SignatureType.ThirdParty) {
            if (isTrusted && status == Status.Active)
                statusResKey = "Approved_Active";
            else if (isTrusted && status == Status.Rejected)
                statusResKey = "Approved_Pending";
            else if (!isTrusted && status == Status.Active)
                statusResKey = "Rejected_Pending";
            else
                statusResKey = "Rejected";
        } else {
            if (status == Status.Active)
                statusResKey = "Active";
            else
                statusResKey = "Rejected";
        }
        printRes("<tr><td class='th'>${Status.Header}</td><td>");
        out.print(resources.getHTML("Status." + statusResKey));
        out.println("</td></tr>");

        // print information about the entity that signed the add-on
        printRes("<tr><td class='th'>${ConfigScript.Add_On.SignedBy}</td><td>");
        prettyPrintCertChain(pkg.signedBy);
        out.println("</td></tr>");

        out.println("</table>");

        out.println("</body></html>");
    }

    private void prettyPrintCertChain(Certificate... signedBy) {
        if (signedBy == null || signedBy.length == 0) {
            printRes("<p>${Signature.Unsigned}</p>");

        } else if (!(signedBy[0] instanceof X509Certificate)) {
            printRes("<p>${Signature.Unrecognized}</p>");

        } else {
            prettyPrintCert(signedBy[0]);
            for (int i = 1; i < signedBy.length; i++) {
                printRes("<p><b>${Signature.Verified_By}</b></p>");
                prettyPrintCert(signedBy[i]);
            }
        }
    }

    private void prettyPrintCert(Certificate cert) {
        X509Certificate x509 = (X509Certificate) cert;
        String subject = x509.getSubjectDN().getName();
        for (String line : CertificateUtils.formatDN(subject)) {
            out.println("<div>" + esc(line) + "</div>");
        }
    }

    private void printRes(String text) {
        out.println(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

    private String esc(String text) {
        return HTMLUtils.escapeEntities(text);
    }

    private static final String PKGID = "pkgId";

}
