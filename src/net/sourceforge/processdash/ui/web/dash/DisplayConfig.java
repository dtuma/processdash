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

package net.sourceforge.processdash.ui.web.dash;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.CertificateUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;





/** CGI script to print out the DNS name of the web server.
 */
public class DisplayConfig extends TinyCGIBase {

    private static final String DASHBOARD_PACKAGE_ID = "pspdash";
    private static final Resources resources =
        Resources.getDashBundle("ProcessDashboard.ConfigScript");

    protected void writeContents() throws IOException {
        if (parameters.containsKey("serverName"))
            printServerName();
        else if (parameters.containsKey("config"))
            printConfigFile();
        else
            printUserConfig();

        out.flush();
    }

    private void printServerName() {
        out.print(getTinyWebServer().getHostName(true));
    }

    private void printConfigFile() {
        loadConfigurationInformation();
        if (configFile != null)
            out.print(configFile.getPath());
    }

    private void printUserConfig() {
        boolean brief = parameters.containsKey("brief");

        printRes("<HTML><HEAD><TITLE>${Title}</TITLE>");

        double indentLeftMargin = brief ? 0.3 : 1;
        out.print("<STYLE> .indent { margin-left: " + indentLeftMargin + "cm }");

        if (brief) {
            out.print("body { font-size: small }");
            out.print("sup { font-size: small }");
        } else {
            out.println("a.plain:link { color:black; text-decoration:none }");
            out.println("a.plain:visited { color:black; text-decoration:none }");
            out.println("a.plain:hover { color:blue; text-decoration:underline }");
        }
        out.print("</STYLE>");

        out.print("<HEAD>");
        out.print("<BODY>");

        if (!brief) {
            printRes("<H1>${Header}</H1>");
        }

        loadConfigurationInformation();

        if (dataDirectory != null) {
            out.print("<DIV>");
            out.print(resources.getHTML(Settings.isPersonalMode()
                    ? "Data_Dir_Header" : "Team_Config_Dir_Header"));
            out.print("<PRE class='indent'>");
            out.println(HTMLUtils.escapeEntities(dataDirectory.getPath()));
            out.println("   </PRE></DIV>");
        }

        if (bundleMode != null) {
            out.print("<DIV>");
            out.print(resources.getString("Bundle_Mode_" + bundleMode.getName()));
            out.print("<BR>&nbsp;</DIV>");
        }

        if (dataURL != null) {
            out.print("<DIV>");
            out.print(resources.getHTML(Settings.isPersonalMode()
                    ? "Data_Url_Header" : "Team_Config_Url_Header"));
            out.print("<PRE class='indent'>");
            out.println(HTMLUtils.escapeEntities(dataURL));
            out.println("   </PRE></DIV>");
        }

        if (installationDirectory != null) {
            out.print("<DIV>");
            out.print(resources.getHTML(Settings.isPersonalMode()
                    ? "Install_Dir_Header" : "Team_Install_Dir_Header"));
            out.print("<PRE class='indent'>");
            out.println(HTMLUtils.escapeEntities(installationDirectory.getPath()));
            out.println("   </PRE></DIV>");
        }

        if (appTemplateDirectory != null) {
            printRes("<DIV>${App_Template_Dir_Header}");
            out.print("<PRE class='indent'>");
            out.println(HTMLUtils.escapeEntities(appTemplateDirectory.getPath()));
            out.println("   </PRE></DIV>");
        }

        printRes("<DIV>${JVM_Header}");
        out.print("<PRE class='indent'>");
        out.println(HTMLUtils.escapeEntities(jvmInfo));
        out.println("   </PRE></DIV>");

        printRes("<DIV>${Add_On.Header}");

        List<DashPackage> packages = TemplateLoader.getPackages();

        if (packages == null || packages.size() < 2)
            printRes("<PRE class='indent'><i>${Add_On.None}</i></PRE>");
        else {
                packages = new ArrayList(packages);
                Collections.sort(packages, PACKAGE_SORTER);

                printRes("<br>&nbsp;"
                         + "<table border class='indent' cellpadding='5'><tr>"
                         + "<th>${Add_On.Name}</th>"
                         + "<th>${Add_On.Version}</th>");

                // We want the brief layout to be as compact as possible so we
                //  don't display the "location" and "signed by" columns
                if (!brief) {
                    printRes("<th>${Add_On.Filename}</th>");
                    printRes("<th>${Add_On.SignedBy}</th>");
                }
                out.println("</tr>");

            for (Iterator<DashPackage> i = packages.iterator(); i.hasNext();) {
                DashPackage pkg = i.next();

                if (!DASHBOARD_PACKAGE_ID.equals(pkg.id))
                    printDashPackageTableRow(pkg, !brief, true, !brief, !brief);
            }
            maybePrintCustomTranslationPackages(brief);
            out.print("</TABLE>");
        }

        out.println("</DIV>");

        List<DashPackage> badPackages = TemplateLoader.getRejectedPackages();
        if (badPackages != null && !badPackages.isEmpty()) {
            printRes("&nbsp;<div>${Rejected.Header}");

            String color = (brief ? " color='#770000'" : "");
            printRes("<br>&nbsp;"
                     + "<table border class='indent' cellpadding='5'" + color + "><tr>"
                     + "<th>${Add_On.Name}</th>"
                     + "<th>${Add_On.Filename}</th>");
            if (!brief)
                printRes("<th>${Add_On.SignedBy}</th>");
            out.print("</tr>");

            for (DashPackage pkg : badPackages) {
                printDashPackageTableRow(pkg, !brief, false, true, !brief);
            }

            out.print("</table>");
            out.println("</div>");
        }

        // Showing a link to "more details" if we are in brief mode
        if (brief) {
            printRes("<p><a href=\"/control/showenv.class\">${More_Details}</a>"
                    + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                    + "<a href=\"/control/showConsole.class?trigger\">${Show_Log}</a></p>");
        }

        out.println("</BODY></HTML>");
    }

    private void printDashPackageTableRow(DashPackage pkg, boolean printLink,
            boolean printVersion, boolean printFilename, boolean printSigner) {
        out.print("<tr>");

        out.print("<td>");
        if (printLink)
            out.print("<a class='plain' href='showPackage?pkgId="
                    + HTMLUtils.urlEncode(pkg.id) + "' target='_blank'>");
        out.print(HTMLUtils.escapeEntities(pkg.name));
        if (printLink)
            out.print("</a>");
        out.print("</td>");

        if (printVersion)
            out.print("<td>" + HTMLUtils.escapeEntities(pkg.version) + "</td>");

        if (printFilename)
            out.print("<td>" + HTMLUtils.escapeEntities(cleanupFilename( //
                pkg.filename)) + "</td>");

        if (printSigner) {
            String signer = CertificateUtils.getSignerSimpleName(pkg.signedBy);
            if (signer == null)
                out.print("<td align='center'>&mdash;</td>");
            else
                out.print("<td>" + HTMLUtils.escapeEntities(signer) + "</td>");
        }

        out.print("</tr>");
    }

    private void maybePrintCustomTranslationPackages(boolean brief) {
        maybePrintCustomTranslationPackages(installationDirectory, brief);
        maybePrintCustomTranslationPackages(TemplateLoader
                .getApplicationTemplateDir(), brief);
    }
    private void maybePrintCustomTranslationPackages(File dir, boolean brief) {
        File[] dirFiles = (dir == null ? null : dir.listFiles());
        if (dirFiles == null) return;

        for (File f : dirFiles) {
            Matcher m = TRANSLATION_FILENAME_PAT.matcher(f.getName());
            if (m.matches()) {
                String langCode = m.group(1);
                Locale locale = new Locale(langCode);
                String packageName = resources.format(
                    "Add_On.Translations_FMT", locale.getDisplayLanguage());

                out.print("<tr><td>");
                out.print(HTMLUtils.escapeEntities(packageName));
                out.print("</td><td> - </td>");

                if (!brief) {
                    String fn = cleanupFilename(f.getPath());
                    out.print("<td>" + HTMLUtils.escapeEntities(fn) + "</td>");
                    out.print("<td align='center'>&mdash;</td>");
                }

                out.println("</tr>");
            }
        }
    }
    private static final Pattern TRANSLATION_FILENAME_PAT = Pattern.compile(
        "pspdash_(..).*jar", Pattern.CASE_INSENSITIVE);


    File dataDirectory;
    FileBundleMode bundleMode;
    File installationDirectory;
    File appTemplateDirectory;
    File configFile;
    String dataURL;
    String jvmInfo;

    private void loadConfigurationInformation() {
        dataDirectory = null;
        bundleMode = null;
        dataURL = null;
        configFile = new File(DashController.getSettingsFileName());

        WorkingDirectory workingDir = ((ProcessDashboard) getDashboardContext())
                .getWorkingDirectory();
        if (workingDir instanceof BridgedWorkingDirectory) {
            BridgedWorkingDirectory bwd = (BridgedWorkingDirectory) workingDir;
            dataURL = bwd.getDescription();
            dataDirectory = bwd.getTargetDirectory();
            if (dataDirectory == null)
                configFile = null;
            else
                configFile = new File(dataDirectory, configFile.getName());

        } else if (workingDir instanceof LocalWorkingDirectory) {
            LocalWorkingDirectory lwd = (LocalWorkingDirectory) workingDir;
            dataDirectory = lwd.getTargetDirectory();
        }
        if (workingDir instanceof BundledWorkingDirectory) {
            bundleMode = ((BundledWorkingDirectory) workingDir).getBundleMode();
        }

        DashPackage dash = TemplateLoader.getPackage(DASHBOARD_PACKAGE_ID);
        if (dash != null && StringUtils.hasValue(dash.filename)) {
            File dashJar = new File(dash.filename);
            installationDirectory = dashJar.getParentFile();
        }

        appTemplateDirectory = TemplateLoader.getApplicationTemplateDir();

        jvmInfo = System.getProperty("java.vendor") + " JRE "
                + System.getProperty("java.version") + "; "
                + System.getProperty("os.name");
    }

    private String cleanupFilename(String filename) {
        if (filename == null)
            return "";
        else
            return filename;
    }

    private void printRes(String text) {
        out.println(resources.interpolate(text, HTMLUtils.ESC_ENTITIES));
    }

    private static class PackageNameSorter implements Comparator<DashPackage> {
        public int compare(DashPackage o1, DashPackage o2) {
            return getName(o1).compareToIgnoreCase(getName(o2));
        }
        private String getName(DashPackage pkg) {
            String result = pkg.name;
            return (result == null ? "ZZZ" : result);
        }
    }
    private static final PackageNameSorter PACKAGE_SORTER = new PackageNameSorter();
}
