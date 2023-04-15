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
        out.println("  tr.sign td { vertical-align: top }");
        out.println("  tr.sign table.firstCert td { vertical-align: middle }");
        out.println("  td.th { font-weight: bold; white-space: nowrap }");
        out.println("  a.changeLink { padding-left:0.5em; font-style:italic }");
        out.println("  div.advice form  { padding-left: 2cm }");
        out.println("  table.showAdvice .changeLink { display:none }");
        out.println("  table.hideAdvice .advice     { display:none }");
        out.println("</style>");
        out.println("<script type='text/javascript'>");
        out.println("  function showChangeAdvice() {");
        out.println("    document.getElementById('data').className = \"showAdvice\";");
        out.println("  }");
        out.println("</script>");
        out.println("</head><body>");

        // print a header
        printRes("<h1>${Title}</h1>");

        // display change advice right away for a rejected 3rd-party addon
        String adviceClass = "hideAdvice";
        if (isManagingCerts && sigType == SignatureType.ThirdParty //
                && !isTrusted && status == Status.Rejected)
            adviceClass = "showAdvice";

        // start a table of data
        out.println("<table cellspacing='10' id='data' class='" //
                + adviceClass + "'>");

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
        String statusResKey, action = null;
        if (isManagingCerts && sigType == SignatureType.ThirdParty) {
            action = (isTrusted ? REJECT_ACTION : APPROVE_ACTION);
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

        // if third-party certificate approval/rejection is an option, print a
        // list of relevant advice along with a hyperlink to display it
        if (action != null) {
            printRes(" <a onclick='showChangeAdvice(); return false;'"
                    + " href='#' class='changeLink'>${Actions.Change}</a>");
            out.println("<div class='advice' style='margin-top:12px'><ul>");
            printRes("<li>${Advice.Not_Dash_Team}</li>");
            printRes("<li>${Advice.Review_Step}</li>");
            if (pkg.signedBy.length == 1)
                printRes("<li>${Signature.Self_Signed}</li>");
            printRes("<li>${Advice.Contact_Step}</li>");
            printRes("<li>${Advice." + action + "_Step}</li>");
            out.println("</ul></div>");
        }

        out.println("</td></tr>");

        // print information about the entity that signed the add-on
        printRes("<tr class='sign'><td class='th'>"
                + "${ConfigScript.Add_On.SignedBy}</td><td>");
        prettyPrintCertChain(action, pkg.signedBy);

        // print resolution advice about rejected, unsigned add-ons
        if (status == Status.Rejected && sigType == SignatureType.Unsigned) {
            out.print("<ul>");
            for (String line : resources.getStrings("Advice.Unsigned"))
                out.println("<li>" + esc(line) + "</li>");
            out.print("</ul>");
        }

        out.println("</td></tr>");

        out.println("</table>");

        out.println("</body></html>");
    }

    private void prettyPrintCertChain(String action, Certificate... signedBy) {
        if (signedBy == null || signedBy.length == 0) {
            printRes("<p>${Signature.Unsigned}</p>");

        } else if (!(signedBy[0] instanceof X509Certificate)) {
            printRes("<p>${Signature.Unrecognized}</p>");

        } else {
            out.println("<table class='firstCert'><tr><td>");
            prettyPrintCert(signedBy[0]);
            if (action != null) {
                // if a third-party certificate action is possible, print a
                // form to the right of the certificate we'd be modifying
                out.println("</td><td>");
                printActionForm(pkgId, action);
            }
            out.println("</td></tr></table>");

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

    private void printActionForm(String pkgId, String action) {
        out.println("<div class='advice'>");
        out.println("<form action='showPackage' method='POST'>");
        writePostTokenFormElement(true);
        out.println("<input type='hidden' name='" + PKGID + "' value='"
                + esc(pkgId) + "'>");
        out.println("<input type='hidden' name='" + ACTION_PARAM + "' value='"
                + esc(action) + "'>");
        out.println("<input type='submit' name='submit' value='"
                + resources.getHTML("Actions." + action) + "'>");
        out.println("</form></div>");
    }

    @Override
    protected void doPost() throws IOException {
        // read input data and load the package in question
        parseFormData();
        init();

        // perform the requested certificate action
        performCertificateAction();

        // redirect to the plain page to display the results
        String uri = HTMLUtils.appendQuery("showPackage", PKGID, pkgId);
        out.write("Location: " + uri + "\r\n\r\n");
    }

    private void performCertificateAction() {
        // guard against CSRF attacks
        if (checkPostToken() == false)
            return;

        // check preconditions
        CertificateManager mgr = DashboardSecurity.getCertificateManager();
        if (mgr == null || sigType != SignatureType.ThirdParty)
            return;

        // identify the action the user wants to perform
        String action = getParameter(ACTION_PARAM);
        Boolean newTrustVal;
        if (APPROVE_ACTION.equals(action))
            newTrustVal = Boolean.TRUE;
        else if (REJECT_ACTION.equals(action))
            newTrustVal = Boolean.FALSE;
        else
            return;

        // perform the action
        mgr.addCert(pkg.signedBy[0], newTrustVal);
    }


    protected String getDefaultPostTokenDataNameSuffix() {
        return ShowDashPackageInfo.class.getSimpleName();
    }

    private void printRes(String text) {
        out.println(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

    private String esc(String text) {
        return HTMLUtils.escapeEntities(text);
    }

    private static final String PKGID = "pkgId";
    private static final String ACTION_PARAM = "action";
    private static final String APPROVE_ACTION = "Approve";
    private static final String REJECT_ACTION = "Reject";

}
