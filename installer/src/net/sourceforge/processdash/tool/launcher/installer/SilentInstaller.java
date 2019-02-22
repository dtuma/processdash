// Copyright (C) 2018-2019 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.VersionUtils;

public class SilentInstaller implements Runnable {

    @Override
    public void run() {
        if (needsInstall())
            install();
    }

    private static boolean needsInstall() {
        if (LauncherInstallerPaths.getInstalledPath() == null)
            return true;

        String installedVersion = LauncherInstallerPaths.getInstalledVersion();
        if (installedVersion == null)
            return true;

        try {
            // if this version is newer than the installed version, upgrade
            String thisVersion = TemplateLoader
                    .getPackageVersion("pdes-installer-launcher");
            return VersionUtils.compareVersions(installedVersion,
                thisVersion) < 0;
        } catch (Throwable t) {
            return false;
        }
    }


    public static void main(String[] args) {
        install();
    }

    private static void install() {
        try {
            doInstall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doInstall() throws Exception {
        // build a command line for executing the automated installer
        String java = RuntimeUtils.getJreExecutable();
        File self = RuntimeUtils.getClasspathFile(SilentInstaller.class);
        File autoInstallData = extractAutoInstallData();
        String[] cmd = new String[] { java, "-jar", self.getAbsolutePath(),
                autoInstallData.getAbsolutePath() };

        // run the automated installer, and wait for it to finish
        System.out.println(DELIM);
        System.out.println("Silent-installing Process Dashboard Launcher");
        Process p = Runtime.getRuntime().exec(cmd);
        RuntimeUtils.consumeOutput(p, System.out, System.err);
        autoInstallData.delete();
        System.out.println(DELIM);
    }

    private static File extractAutoInstallData() throws IOException {
        // open the file containing the auto-install XML data
        boolean isWindows = System.getProperty("os.name").contains("Windows");
        String filename = (isWindows ? "auto-windows.xml" : "auto-unix.xml");
        InputStream in = SilentInstaller.class.getResourceAsStream(filename);

        // create a temporary file to hold the document
        File out = File.createTempFile("auto-install", ".xml");
        out.deleteOnExit();

        // on unix, the file can be used verbatim
        if (!isWindows) {
            FileUtils.copyFile(in, out);
            return out;
        }

        // on Windows, we must filter the file to insert the installation dir
        BufferedReader r = new BufferedReader(
                new InputStreamReader(in, "UTF-8"));
        Writer w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
        String instDir = escapeAttribute(
            LauncherInstallerPaths.getDefaultInstallationPath());
        String line;
        while ((line = r.readLine()) != null) {
            line = line.replace("$INSTALL_DIR", instDir);
            w.write(line + "\r\n");
        }
        r.close();
        w.close();
        return out;
    }
    
    private static String escapeAttribute(String value) {
        if (value == null)
            return "";

        StringBuffer result = new StringBuffer(value.length());
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case '<': result.append("&lt;"); break;
                case '>': result.append("&gt;"); break;
                case '&': result.append("&amp;"); break;
                case '"': result.append("&quot;"); break;
                case '\'': result.append("&apos;"); break;
                default:
                    if (chars[i] < 32)
                        result.append("&#").append((int) chars[i]).append(";");
                    else
                        result.append(chars[i]);
            }
        }
        return result.toString();
    }


    private static final String DELIM = "==============================="
            + "=======================================";


}
