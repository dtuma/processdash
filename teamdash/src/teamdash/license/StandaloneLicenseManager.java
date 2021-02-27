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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RuntimeUtils;

public class StandaloneLicenseManager {

    public static StandaloneLicense getLicense() {
        // try loading a license file from the global application directory
        File licenseFile = getGlobalLicenseFile();
        StandaloneLicense globalLicense = null;
        try {
            globalLicense = loadLicense(new FileInputStream(licenseFile));
        } catch (Exception e) {
        }

        // if a valid, non-expired global license was found, return it
        if (globalLicense != null && !globalLicense.isExpired())
            return globalLicense;

        // try loading a bundled license file
        StandaloneLicense bundledLicense = null;
        try {
            // look for a license inside this JAR file
            InputStream licenseData = StandaloneLicenseManager.class
                    .getResourceAsStream(LICENSE_PATH);

            // if the JAR didn't contain a license, look in the directory
            // containing the JAR
            if (licenseData == null)
                licenseData = new FileInputStream(getAdjacentLicenseFile());

            bundledLicense = loadLicense(licenseData);

        } catch (Exception e) {
        }

        if (bundledLicense != null)
            // if a valid bundled license was found, return it
            return bundledLicense;
        else if (globalLicense != null)
            // otherwise, return the (expired) global license
            return globalLicense;

        // no valid licenses were found.
        return null;
    }

    private static File getGlobalLicenseFile() {
        File appBaseDirectory = DirectoryPreferences.getApplicationDirectory();
        File licenseDirectory = new File(appBaseDirectory, APP_LICENSE_DIR);
        return new File(licenseDirectory, LICENSE_FILE);
    }

    private static File getAdjacentLicenseFile() {
        File selfClasspath = RuntimeUtils
                .getClasspathFile(StandaloneLicenseManager.class);
        File installationDir = selfClasspath.getParentFile();
        return new File(installationDir, LICENSE_FILE);
    }

    private static StandaloneLicense loadLicense(InputStream licenseData)
            throws IOException, StandaloneLicenseException {
        String xml = new String(FileUtils.slurpContents(licenseData, true),
                "UTF-8");
        StandaloneLicense license = new StandaloneLicense(xml);
        if (license.isValid())
            return license;
        else
            return null;
    }

    public static boolean isStandaloneJar() {
        URL url = StandaloneLicenseManager.class.getResource(LICENSE_PATH);
        return url != null && "jar".equals(url.getProtocol());
    }

    private static final String LICENSE_FILE = "planning-accelerator-license.xml";

    private static final String LICENSE_PATH = "/" + LICENSE_FILE;

    private static final String APP_LICENSE_DIR = "licenseData";

}
