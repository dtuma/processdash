// Copyright (C) 2002-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;


public class WelcomePage extends WizardPage {

    protected void writePageTitle() {
        // write header for welcome screen (differs slightly from others)
        out.print("<table><tr><td><img src=\"probe.gif\"></td><td><h1>");
        out.print(resources.getHTML("Page_Title"));
        out.print("<br>");
        out.print(esc(prefix));
        out.println(" </h1> </td></tr></table>");
    }

    public void writeHTMLContents() {
        out.print(resources.getString("Intro_HTML"));
    }

    public boolean parseFormData() { return true; }

    public boolean writeReportSection() {
        String title = resources.getHTML("Report_Title");
        writeHTMLHeader(title, "/style.css", EXTRA_REPORT_HEADERS);
        out.println("<h1>" + title + "</h1>");
        out.println("<h2>" + esc(prefix) + "</h2>");
        return true;
    }
    private static final String EXTRA_REPORT_HEADERS =
        "<link rel='stylesheet' type='text/css' href='/dash/snippets/alert.css'>" +
        "<style>" +
            "A.plain:link    { color:black; text-decoration:none } "+
            "A.plain:visited { color:black; text-decoration:none } "+
            "A.plain:hover   { color:blue;  text-decoration:underline } "+
        "</style>";

}
