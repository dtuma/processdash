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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import teamdash.XMLUtils;

public class CustomProcessServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");

        String prefix = getProcessTemplatePrefix(request);
        String resName = prefix + "/team/lib/script-v1.xml";
        try {
            URL u = CustomProcessServlet.class.getResource(resName);
            BufferedReader in = new BufferedReader(new InputStreamReader(u
                    .openConnection().getInputStream(), "UTF-8"));

            response.getWriter().println("Prefix is " + prefix);
            response.getWriter().println();

            String line = in.readLine();
            while (line != null && line.indexOf("generation-script") == -1)
                line = in.readLine();
            do {
                response.getWriter().println(line);
                line = in.readLine();
            } while (line != null && line.indexOf("startingJar") == -1);

        } catch (Exception e) {
            response.getWriter().print("Can't find " + resName);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ServletInputStream in = request.getInputStream();

        try {
            Document settings = XMLUtils.parse(in);
            CustomProcess process = new CustomProcess(settings);
            String prefix = getProcessTemplatePrefix(request);
            CustomProcessPublisher.publish(process, response.getOutputStream(),
                    new ClasspathContentProvider(prefix));
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private String getProcessTemplatePrefix(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/Templates"))
            return pathInfo;
        else
            return getInitParameter("prefix");
    }

}
