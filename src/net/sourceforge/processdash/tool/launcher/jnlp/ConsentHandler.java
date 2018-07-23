// Copyright (C) 2010-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class ConsentHandler {

    private static final Preferences PREFS = Preferences.userNodeForPackage(
        ConsentHandler.class).node("LicenseConsent");

    private File distrDirectory;

    private String distrId;

    private File distrTagFile;

    public ConsentHandler(File distrDirectory) {
        this.distrDirectory = distrDirectory;
    }

    public boolean consentIsOK() {
        if (userHasConsentedToDistribution())
            return true;

        SortedMap<String, String> licenses = getLicensesNeedingConsent();
        if (licenses.remove(NEEDS_CONSENT_FLAG) == null)
            return true;

        if (askUserForConsent(licenses) == false)
            return false;

        storeConsentFlags(licenses);
        return true;
    }

    private boolean userHasConsentedToDistribution() {
        // look for a Java preference indicating that consent has been given.
        distrId = distrDirectory.getName();
        if (PREFS.getBoolean(distrId, false))
            return true;

        // look for a tagging file indicating that consent has been given.
        distrTagFile = new File(distrDirectory, TAG_FILE_NAME);
        if (distrTagFile.exists())
            return true;

        return false;
    }

    private SortedMap<String, String> getLicensesNeedingConsent() {
        SortedMap<String, String> licenses = new TreeMap<String, String>();

        for (File f : distrDirectory.listFiles())
            checkForLicenseNeedingConsent(f, licenses);

        return licenses;
    }


    private void checkForLicenseNeedingConsent(File f,
            SortedMap<String, String> licenses) {
        String name = f.getName().toLowerCase();
        if (!name.endsWith(".zip") && !name.endsWith(".jar"))
            return;

        try {
            String url = "jar:" + f.toURI().toURL() + "!/" + LICENSE_FILENAME;
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    new URL(url).openStream(), "utf-8"));
            scanForLicenseNeedingConsent(in, licenses);
            in.close();
        } catch (Exception e) {
        }
    }


    private void scanForLicenseNeedingConsent(BufferedReader in,
            SortedMap<String, String> licenses) throws IOException {
        String licenseID = null;
        boolean needsConsent = false;

        // scan the HTML header, looking for the meta tag
        String line;
        while (true) {
            line = in.readLine();
            if (line == null)
                return;
            String lower = line.toLowerCase();

            if (licenseID == null && lower.contains("<meta")
                    && lower.contains(META_NAME)) {
                Matcher m = META_CONTENT_PAT.matcher(line);
                if (m.find()) {
                    licenseID = m.group(1);
                    if (!PREFS.getBoolean(stripLicenseOrdinal(licenseID), false))
                        needsConsent = true;
                }
            }

            if (lower.contains("<body")) {
                if (licenseID == null)
                    return; // never saw the meta tag. Abort.
                else
                    break; // we found the end of the header.
            }
        }

        StringBuilder html = new StringBuilder();
        while (true) {
            line = in.readLine();
            if (line == null || line.toLowerCase().contains("</body"))
                break;
            else
                html.append(line).append("\n");
        }
        licenses.put(licenseID, html.toString());
        if (needsConsent)
            licenses.put(NEEDS_CONSENT_FLAG, "true");
    }



    private boolean askUserForConsent(SortedMap<String, String> licenses) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        for (String oneLicense : licenses.values())
            html.append(oneLicense).append("<hr>");
        html.setLength(html.length() - 4);
        html.append("</body></html>");


        JEditorPane editor = new JEditorPane();
        editor.setContentType("text/html");
        editor.setEditable(false);
        editor.setBackground(Color.white);
        editor.setText(html.toString());

        JScrollPane sp = new JScrollPane(editor);
        sp.setPreferredSize(new Dimension(500, 400));

        String title = JnlpDatasetLauncher.res.getString("Consent_Title");
        Object[] message = new Object[] {
                JnlpDatasetLauncher.res.getStrings("Consent_Header"), //
                " ", sp, " " };
        String[] options = new String[] {
                JnlpDatasetLauncher.res.getString("Consent_Cancel"),
                JnlpDatasetLauncher.res.getString("Consent_Agree") };

        int userChoice = JOptionPane.showOptionDialog(null, message, title,
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
            options, options[1]);
        return (userChoice == 1);
    }

    private void storeConsentFlags(SortedMap<String, String> consentedLicenses) {
        // store a prefs flag for each consented license
        for (String licenseID : consentedLicenses.keySet())
            PREFS.putBoolean(stripLicenseOrdinal(licenseID), true);

        // store a prefs flag for the overall distribution
        PREFS.putBoolean(distrId, true);

        // create a tag file for the distribution
        try {
            distrTagFile.createNewFile();
        } catch (Exception e) {
        }
    }

    private String stripLicenseOrdinal(String licenseID) {
        int semicolonPos = licenseID.indexOf(';');
        if (semicolonPos != -1)
            licenseID = licenseID.substring(semicolonPos + 1);
        return licenseID;
    }

    private static final String TAG_FILE_NAME = "licenseConsented.txt";

    private static final String LICENSE_FILENAME = "Templates/license-consent.html";

    private static final String META_NAME = "pdash-license-id";

    private static final Pattern META_CONTENT_PAT = Pattern.compile(
        "content=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    private static final String NEEDS_CONSENT_FLAG = "Consent is Needed";

}
