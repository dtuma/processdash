// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import net.sourceforge.processdash.ui.lib.HTMLSplashScreen;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;

public class DashboardSplashScreen extends HTMLSplashScreen {

    // All HTML blocks of content we show on the splash screen are
    //  located in a "splash.html" files.
    private static final String SPLASH_CONTENT_FILE = "splash.html";

    public DashboardSplashScreen() {
        super(getSplashImage(), getSplashHtmlBlocks());
    }

    private static ImageIcon getSplashImage() {
        URL url = DashboardSplashScreen.class.getResource("splash.png");
        return new ImageIcon(url);
    }

    private static List<String> getSplashHtmlBlocks() {
        List<String> blocks = new ArrayList<String>();

        File classpathFile = RuntimeUtils.getClasspathFile(DashboardSplashScreen.class);

        if (classpathFile.isFile()) {
            File parent = classpathFile.getParentFile();

            if (parent.isDirectory()) {
                // "parent" points to the directory containing pspdash.jar. We
                //  scan all other jars in that directory to find splash content
                //  files. when we find one, we add it's content to the list.

                File[] jars = parent.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".jar")
                            || name.toLowerCase().endsWith(".zip");
                    }
                });

                // We want the files to be in a consistent order
                Arrays.sort(jars);

                for (File file : jars) {
                    try {
                        URL splashURL = new URL("jar:" +
                                                file.toURI().toURL().toString() +
                                                "!/Templates/" + SPLASH_CONTENT_FILE);

                         try {
                             URLConnection conn = splashURL.openConnection();
                             InputStream inputStream = conn.getInputStream();
                             String html = getHtmlFromInputStrean(inputStream);

                             blocks.add(html);
                         } catch (IOException e) {
                             // The jar simply does not contain any splash content file.
                         }
                    } catch (MalformedURLException e) {
                        // Since the URL has been created from an existing file,
                        //  it cannot be malformed. That's why there's nothing
                        //  to do here.
                    }
                }
            }
        }

        // We add the default spash text last to be sure that it's shown last.
        blocks.add(getSplashText());

        return blocks;
    }

    private static String getSplashText() {
        try {
            InputStream in = DashboardSplashScreen.class
                    .getResourceAsStream(SPLASH_CONTENT_FILE);
            String html = getHtmlFromInputStrean(in);
            return StringUtils.findAndReplace(html, "####", getVersionNumber());
        } catch (Exception e) {
            throw new RuntimeException("unable to load splash screen html", e);
        }
    }

    private static String getHtmlFromInputStrean(InputStream in)
            throws IOException, UnsupportedEncodingException {
        byte[] rawContent = FileUtils.slurpContents(in, true);
        return new String(rawContent, "UTF-8");
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
