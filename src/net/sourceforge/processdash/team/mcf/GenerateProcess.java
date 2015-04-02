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

package net.sourceforge.processdash.team.mcf;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.w3c.dom.Document;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.util.XMLUtils;


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

            String destName = "dist";
            if (args.length > 1)
                destName = args[1];
            File dest = new File(destName);
            if (!dest.isDirectory() && !dest.getParentFile().isDirectory()) {
                System.err.println("No such directory '" + args[1] + "'");
                System.exit(1);
            }

            boolean light = !Boolean.getBoolean("full");

            GenerateProcess instance = new GenerateProcess(dest, settings,
                    settingsFile.toURI().toURL(), light);
            instance.run();
            if (args.length < 3)
                System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private File dest;

    private Document settingsFile;

    private URL extBase;

    private boolean light;


    public GenerateProcess(File dest, Document settingsFile, URL extBase,
            boolean light) {
        this.dest = dest;
        this.settingsFile = settingsFile;
        this.extBase = extBase;
        this.light = light;
    }

    private void run() throws Exception {
        CustomProcess process = new CustomProcess(settingsFile);

        File outputFile;
        if (dest.isDirectory())
            outputFile = new File(dest, process.getJarName().replace(',', '-'));
        else
            outputFile = dest;

        ContentSource content;
        String templateDir = System.getProperty("templatesDir");
        if (templateDir == null)
            content = new ClasspathContentProvider();
        else
            content = new FileContentProvider(new File(templateDir));

        CustomProcessPublisher.publish(process, outputFile, content, extBase,
            light);
    }

}
