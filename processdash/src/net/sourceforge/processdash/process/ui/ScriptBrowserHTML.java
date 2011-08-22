// Copyright (C) 2003-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process.ui;


import java.io.IOException;
import java.util.List;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;



public class ScriptBrowserHTML extends TinyCGIBase {

    private static String LEAF_IMAGE =
        "<IMG WIDTH=13 HEIGHT=16 SRC='/Images/leaf.png'>";
    private static String NODE_IMAGE =
        "<IMG WIDTH=16 HEIGHT=13 SRC='/Images/node.png'>";
    private static String SCRIPT_IMAGE =
        "<IMG WIDTH=10 HEIGHT=13 SRC='/Images/script.png'>";

    protected void writeContents() throws IOException {
        String prefix = getPrefix();
        if (prefix == null) prefix = "";

        DashHierarchy props = getPSPProperties();
        PropertyKey key = props.findExistingKey(prefix);

        out.write("<HTML><HEAD>");
        out.write("<link rel=stylesheet type='text/css' href='/style.css'>");
        out.write("<TITLE>Hierarchy");
        if (prefix.length() > 0) {
            out.write(" - ");
            out.write(prefix);
        }
        out.write("</TITLE></HEAD><BODY>");
        if (prefix.length() > 0) {
            out.write("<B>");
            out.write(prefix);
            out.write("</B><BR>");
        }

        for (int i=0;   i < props.getNumChildren(key);   i++)
            writeNode(props, props.getChildKey(key, i));

        List<ScriptID> scripts = ScriptEnumerator.getScripts(
            getDashboardContext(), key);
        if (scripts != null && scripts.size() != 0) {
            out.write("<hr>");
            for (int i=1;  i < scripts.size();  i++)
                writeScript(scripts.get(i));
        }

        out.write("</BODY></HTML>");
    }

    protected void writeNode(DashHierarchy props, PropertyKey node) {
        boolean isLeaf = (props.getNumChildren(node) == 0);
        String prefix = WebServer.urlEncodePath(node.path());

        out.write(isLeaf ? LEAF_IMAGE : NODE_IMAGE);
        out.write("&nbsp;<A HREF='");
        out.write(prefix);
        out.write("//dash/hier.class'>");
        out.write(HTMLUtils.escapeEntities(node.name()));
        out.write("</A>");
        if (isLeaf) {
            out.write("&nbsp;<A HREF='");
            out.write(prefix);
            out.write("//control/setPath.class?start'>" +
                      "<img border='0' alt='Start timing' " +
                      "src='../control/startTiming.png'></A>");
        }
        out.write("<BR>\n");
    }

    protected void writeScript(ScriptID script) {
        String url = script.getHref();
        if (url == null) return;

        out.write(SCRIPT_IMAGE);
        out.write("&nbsp;<A HREF='");
        out.write(url);
        out.write("'>");
        out.write(HTMLUtils.escapeEntities(script.getDisplayName().trim()));
        out.write("</A><BR>\n");
    }
}
