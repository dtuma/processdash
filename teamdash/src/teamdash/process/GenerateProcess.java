// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.process;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import net.sourceforge.processdash.net.http.ContentSource;

import org.w3c.dom.Document;

import teamdash.XMLUtils;

public class GenerateProcess {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("You must supply the name of a "
                        + "settings.xml file on the command line.");
                System.exit(1);
            }
            File settingsFile = new File(args[0]);
            if (!settingsFile.exists()) {
                System.err.println("Could not open the file '" + args[0] + "'");
                System.exit(1);
            }
            Document settings = null;
            try {
                settings = XMLUtils.parse(new FileInputStream(settingsFile));
            } catch (Exception e) {
                System.err.println("Invalid settings file '" + args[0] + "'");
                System.exit(1);
            }

            String destDirname = ".";
            if (args.length > 1)
                destDirname = args[1];
            File destDir = new File(destDirname);
            if (!destDir.isDirectory()) {
                System.err.println("No such directory '" + args[1] + "'");
                System.exit(1);
            }

            GenerateProcess instance = new GenerateProcess(destDir, settings,
                    settingsFile.toURL());
            instance.run();
            if (args.length < 3)
                System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File destDir;

    private Document settingsFile;

    private URL extBase;

    public GenerateProcess(File destDir, Document settingsFile, URL extBase) {
        this.destDir = destDir;
        this.settingsFile = settingsFile;
        this.extBase = extBase;
    }

    private void run() throws Exception {
        CustomProcess process = new CustomProcess(settingsFile);

        File outputFile = new File(destDir, process.getJarName());

        ContentSource content = new ClasspathContentProvider();
        CustomProcessPublisher.publish(process, outputFile, content, extBase);
    }

}
