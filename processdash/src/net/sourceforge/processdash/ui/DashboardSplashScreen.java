// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.io.InputStream;
import java.net.URL;

import javax.swing.ImageIcon;

import net.sourceforge.processdash.ui.lib.HTMLSplashScreen;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;

public class DashboardSplashScreen extends HTMLSplashScreen {

    public DashboardSplashScreen() {
        super(getSplashImage(), getSplashText());
    }

    private static ImageIcon getSplashImage() {
        URL url = DashboardSplashScreen.class.getResource("splash.png");
        return new ImageIcon(url);
    }

    private static String getSplashText() {
        try {
            InputStream in = DashboardSplashScreen.class
                    .getResourceAsStream("splash.html");
            byte[] rawContent = FileUtils.slurpContents(in, true);
            String html = new String(rawContent, "UTF-8");
            return StringUtils.findAndReplace(html, "####", getVersionNumber());
        } catch (Exception e) {
            throw new RuntimeException("unable to load splash screen html", e);
        }
    }

    private static String getVersionNumber() {
        String result = null;
        try {
            result = DashboardSplashScreen.class.getPackage()
                    .getImplementationVersion();
        } catch (Exception e) {
        }
        return (result == null ? "####" : result);
    }

    public static void main(String[] args) {
        new DashboardSplashScreen().displayFor(5000);
    }

}
