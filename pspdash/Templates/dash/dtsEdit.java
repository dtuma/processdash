// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.InterpolatingFilter;
import pspdash.data.ListData;
import pspdash.data.SaveableData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.*;

/** CGI script for editing defect type standards.
 */
public class dtsEdit extends TinyCGIBase {

    protected static final String ACTION = "action";
    protected static final String CREATE = "create";
    protected static final String NAME = "name";

    protected static final String[] OPTIONS = {
        "View", "Edit", "Delete", "Copy", "Default" };
    private static final int VIEW = 0;


    private static final ResourceBundle resources =
        Resources.getBundle("dash.dtsEdit");

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        String action = getParameter(ACTION);
        String name = getParameter(NAME);
        if (OPTIONS[VIEW].equals(action))
            showStandard(name);
        else
            showListOfDefinedStandards();
    }

    protected void showStandard(String name) throws IOException {
        out.print("<html><head><meta http-equiv='Refresh' "+
                  "CONTENT='0;URL=../reports/dts.class?name=");
        out.print(URLEncoder.encode(name));
        out.print("'></head><body>&nbsp;</body></html>");
    }


    protected void showListOfDefinedStandards() throws IOException {
        DataRepository data = getDataRepository();
        String[] standards = DefectTypeStandard.getDefinedStandards(data);
        Arrays.sort(standards, String.CASE_INSENSITIVE_ORDER);

        out.println(resources.getString("Header_HTML"));
        out.print("<p>");
        out.println(getHTML("Welcome_Prompt"));
        out.println("<ul>");
        out.print("<li><a href=\"dtsEdit.class?" + CREATE + "\">");
        out.print(getHTML("Create_Option"));
        out.println("</a></li>");

        if (standards.length > 0) {
            out.print("<li>");
            out.print(getHTML("Manage_Option"));
            out.println("<table>");

            for (int i = 0;   i < standards.length;   i++) {
                String htmlName = HTMLUtils.escapeEntities(standards[i]);
                String urlName = URLEncoder.encode(standards[i]);

                out.print("<tr><td><ul><li>&quot;<b>");
                out.print(htmlName);
                out.print("</b>&quot;</li></ul></td>");

                for (int o = 0;   o < OPTIONS.length;   o++) {
                    String opt = OPTIONS[o];
                    out.print("<td><a href='dtsEdit.class?"+ACTION+"="+opt+
                              "&"+NAME+"="+urlName+"'>");
                    out.print(getHTML(opt));
                    out.print("</a></td>");
                }
                out.println("</tr>");
            }
        }

        out.print("</table></li></ul></body></html>");
    }

    protected String getHTML(String key) {
        return HTMLUtils.escapeEntities(resources.getString(key));
    }
}
