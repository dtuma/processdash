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


import pspdash.HTMLUtils;
import pspdash.PSPProperties;
import pspdash.PropertyKey;
import pspdash.ScriptID;
import pspdash.TinyCGIBase;
import pspdash.TinyWebServer;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Vector;

public class hier extends TinyCGIBase {

    private static String LEAF_IMAGE =
        "<IMG WIDTH=13 HEIGHT=16 SRC='leaf.png'>";
    private static String NODE_IMAGE =
        "<IMG WIDTH=16 HEIGHT=13 SRC='node.png'>";
    private static String SCRIPT_IMAGE =
        "<IMG WIDTH=10 HEIGHT=13 SRC='script.png'>";

    protected void writeContents() throws IOException {
        String prefix = getPrefix();
        if (prefix == null) prefix = "";

        PSPProperties props = getPSPProperties();
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
            out.write("</B>");
        }

        PropertyKey child;
        for (int i=0;   i < props.getNumChildren(key);   i++)
            writeNode(props, props.getChildKey(key, i));

        Vector scripts = props.getScriptIDs(key);
        ScriptID script;
        if (scripts != null && scripts.size() != 0) {
            out.write("<hr>");
            for (int i=1;  i < scripts.size();  i++)
                writeScript((ScriptID) scripts.elementAt(i));
        }

        out.write("</BODY></HTML>");
    }

    protected void writeNode(PSPProperties props, PropertyKey node) {
        boolean isLeaf = (props.getNumChildren(node) == 0);
        String prefix = TinyWebServer.urlEncodePath(node.path());

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
        out.write(HTMLUtils.escapeEntities(script.getDisplayName()));
        out.write("</A><BR>\n");
    }
}
