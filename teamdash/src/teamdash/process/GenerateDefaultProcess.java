// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package teamdash.process;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.processdash.net.http.WebServer;


public class GenerateDefaultProcess {

    public static void main(String[] args) {
        try {
            String destDir = ".";
            if (args.length > 0) destDir = args[0];

            String processName = "TSP";
            if (args.length > 1) processName = args[1];

            String processVersion = "1";
            if (args.length > 2) processVersion = args[2];

            GenerateDefaultProcess instance = new GenerateDefaultProcess
                (destDir, processName, processVersion);
            instance.run();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String destDir;
    private String processName;
    private String processVersion;
    private File outputFile;

    public GenerateDefaultProcess(String destDir, String processName,
                    String processVersion) {
        this.destDir = destDir;
        this.processName = processName;
        this.processVersion = processVersion;
    }

    private void run() throws Exception {
        CustomProcess process = new CustomProcess();
        process.setName(processName);
        process.setVersion(processVersion);

        File dir = new File(destDir);
        File dest = new File(dir, process.getJarName());

        WebServer webServer = getTinyWebServer();
        CustomProcessPublisher.publish(process, dest, webServer);
    }

    private WebServer getTinyWebServer() throws IOException {
        URL[] roots = getRoots();
        WebServer result = new WebServer(0, roots);
        return result;
    }

    private URL[] getRoots() throws IOException {
        URL[] result = new URL[2];
        result[0] = fixURL(getUrlForClass(GenerateDefaultProcess.class));
        result[1] = fixURL(getUrlForClass(WebServer.class));

        return result;
    }


    private URL getUrlForClass(Class class1) {
        String resourceName = "/" + class1.getName().replace('.', '/') + ".class";
        return getClass().getResource(resourceName);
    }

    private URL fixURL(URL u) throws MalformedURLException {
        String url = u.toString();
        if (url.startsWith("jar:")) {
            int exclPos = url.indexOf("!/");
            url = url.substring(0, exclPos) + "!/Templates/";
        } else if (url.indexOf("/bin") != -1) {
            int binPos = url.indexOf("/bin");
            url = url.substring(0, binPos) + "/Templates/";
        }
//        System.out.println("Using url: " + url);
        return new URL(url);
    }

}
