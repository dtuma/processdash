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


package teamdash.templates.tools;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import net.sourceforge.processdash.ui.web.TinyCGIBase;
import teamdash.process.CustomProcess;
import teamdash.process.CustomProcessPublisher;

public class GenerateTestProcess extends TinyCGIBase {

    protected void writeContents() throws IOException {
        CustomProcess process = new CustomProcess();
        process.setName("Test");
        process.setVersion("2");
        String dir = getParameter("dir");
        File dest = new File(dir);
        dest = new File(dest, process.getJarName());
        CustomProcessPublisher.publish(process, dest, getTinyWebServer());
    }

    private static final String THIS_URL =
        "http://localhost:2468/team/tools/GenerateTestProcess.class";
    public static void main(String[] args) {
        try {
            URL u = new URL(THIS_URL + "?dir="
                        + URLEncoder.encode(args[0], "UTF-8"));
            URLConnection conn = u.openConnection();
            conn.connect();
            conn.getInputStream();
        } catch (Exception e) {}
    }

}
