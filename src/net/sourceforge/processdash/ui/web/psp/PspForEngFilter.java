// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class PspForEngFilter extends TinyCGIBase {


    protected void writeContents() throws IOException {
        String targetURI = getTargetURI();
        byte [] contents = getRequest(targetURI, true);
        writeFilteredContents(contents);
    }


    private String getTargetURI() {
        String currentURI = (String) env.get("REQUEST_URI");
        String srcDir = getParameter("srcDir");
        String destDir = getParameter("destDir");

        int pos = currentURI.indexOf(destDir);
        return currentURI.substring(0, pos) +
            srcDir +
            currentURI.substring(pos + destDir.length());
    }


    private void writeFilteredContents(byte[] contents) throws IOException {
        // retrieve applicable parameters
        String filterToken = getParameter("filterToken");
        boolean tweakManualCalcs = !parameters.containsKey("ignoreManualFor");

        // filter the file
        InputStream inStr = new ByteArrayInputStream(contents);
        BufferedReader in =
            new BufferedReader(new InputStreamReader(inStr, "UTF-8"));

        String line;
        while ((line = in.readLine()) != null) {
            line = filterLine(line, tweakManualCalcs, filterToken);
            if (line != null) {
                out.write(line);
                out.write("\r\n");
            }
        }
    }

    private String filterLine(String line, boolean tweakManualCalcs,
            String filterToken) {
        if (filterToken != null && line.indexOf(filterToken) != -1)
            return null;

        if (tweakManualCalcs && line.indexOf("MANUALFOR") != -1) {
            Matcher m = MANUAL_FOR_PATTERN.matcher(line);
            line = m.replaceAll("NAME=\"$2$1 ");
        }

        if (line.indexOf("../") != -1) {
            Matcher m = PATH_PATTERN.matcher(line);
            line = m.replaceAll("../../");
        }

        return line;
    }

    private static final Pattern MANUAL_FOR_PATTERN = Pattern.compile
        ("MANUALFOR=\"([0-9AB]+)\" NAME=\"/?([a-zA-Z ]+/)?");
    private static final Pattern PATH_PATTERN = Pattern.compile("\\.\\./");

}
