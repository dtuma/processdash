// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.XMLUtils;

public class DisplayState extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {
        String script = (String) env.get("SCRIPT_NAME");
        if (script.contains("getTimingState"))
            writeTimingState();
        else if (script.contains("getHierarchy"))
            writeHierarchy();
        else
            throw new TinyCGIException(404, "Unrecognized request");
    }

    private void writeTimingState() {
        DashboardContext ctx = getDashboardContext();
        DashboardTimeLog tl = (DashboardTimeLog) ctx.getTimeLog();
        TimeLoggingModel tlm = tl.getTimeLoggingModel();
        String path = tlm.getActiveTaskModel().getPath();
        String isTiming = tlm.isPaused() ? "false" : "true";

        out.write("Content-Type: text/plain\r\n\r\n");
        out.write("activeTask=" + path + "\r\n");
        out.write("isTiming=" + isTiming + "\r\n");
        out.flush();
    }

    private void writeHierarchy() throws IOException {
        out.write("Content-Type: text/xml\r\n\r\n");
        out.flush();

        try {
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(outStream, "UTF-8");
            xml.startDocument("UTF-8", null);
            writeHierarchy(xml, getPSPProperties(), PropertyKey.ROOT);
            xml.endDocument();
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    private void writeHierarchy(XmlSerializer xml, DashHierarchy hier,
            PropertyKey node) throws Exception {
        xml.startTag(null, "node");
        xml.attribute(null, "name", node.name());

        int numChildren = hier.getNumChildren(node);
        for (int i = 0; i < numChildren; i++)
            writeHierarchy(xml, hier, hier.getChildKey(node, i));

        xml.endTag(null, "node");
    }

}
