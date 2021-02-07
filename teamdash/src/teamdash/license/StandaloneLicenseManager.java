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
import java.net.URL;

import net.sourceforge.processdash.util.FileUtils;

public class StandaloneLicenseManager {

    public static StandaloneLicense getLicense() {
        try {
            InputStream licenseData = StandaloneLicenseManager.class
                    .getResourceAsStream(LICENSE_PATH);
            String xml = new String(FileUtils.slurpContents(licenseData, true),
                    "UTF-8");
            StandaloneLicense license = new StandaloneLicense(xml);
            if (license.isValid())
                return license;

        } catch (Exception e) {
        }
        return null;
    }

    public static boolean isStandaloneJar() {
        URL url = StandaloneLicenseManager.class.getResource(LICENSE_PATH);
        return url != null && "jar".equals(url.getProtocol());
    }

    private static final String LICENSE_FILE = "planning-accelerator-license.xml";

    private static final String LICENSE_PATH = "/" + LICENSE_FILE;

}
