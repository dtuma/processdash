// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.reports;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;


public class ExcelReport extends TinyCGIBase {

    private static String exportMethod =
        Settings.getVal("excel.exportMethod");

    protected void writeHeader() { }

    protected void writeContents() throws IOException {
        if (useIQY())
            writeIQY();
        else
            writeHTML();
    }

    protected void writeIQY() throws IOException {
        // write header
        out.print("Content-type: application/octet-stream\r\n\r\n");

        // write contents
        out.println("WEB");
        out.println("1");
        out.println(getURL());

        // write optional settings?
        if (parameters.get("fullPage") != null) {
            out.println();
            out.println("Selection=EntirePage");
        }
    }

    protected void writeHTML() throws IOException {
        // write header
        out.print("Content-type: application/vnd.ms-excel\r\n\r\n");

        // write contents
        String results = getTinyWebServer().getRequestAsString(getURI());
        int beginPos = results.indexOf("<TABLE");
        int endPos   = results.lastIndexOf("/TABLE");
        if (endPos != -1) endPos = results.indexOf('>', endPos);

        if (parameters.get("fullPage") != null ||
            beginPos == -1 || endPos == -1) {
            // Break the stylesheet links - otherwise Excel will complain
            results = StringUtils.findAndReplace(results, "<link", "<notlink");
            results = StringUtils.findAndReplace(results, "<LINK", "<NOTLINK");
            out.println(results);
        } else {
            out.println("<HTML>");
            out.println(results.substring(beginPos, endPos + 1));
            out.println("</HTML>");
        }
    }

    private boolean useIQY() {
        if ("mime".equalsIgnoreCase(exportMethod))
            return false;
        else if ("iqy".equalsIgnoreCase(exportMethod))
            return true;
        else {
            String userAgent = (String) env.get("HTTP_USER_AGENT");
            return (userAgent.indexOf("MSIE") != -1);
        }
    }

    private String getURI() {
        String referer = (String) env.get("HTTP_REFERER");

        // if the query parameter "URI" is set, resolve it relative to
        // the referer URI and return the value.
        String uri = (String) parameters.get("uri");
        if (uri != null)
            return resolveRelativeURI(referer, uri);

        uri = (String) env.get("REQUEST_URI");

        // If no query parameters were sent to this request, use the
        // uri of the referer.
        if (uri.indexOf('?') == -1 && referer != null) try {
            return (new URL(referer)).getFile();
        } catch (MalformedURLException mue) {}

        // fallback method: use our own uri, but replace "excel" with "table"
        uri = StringUtils.findAndReplace(uri, "/excel.iqy",   "/table.class");
        uri = StringUtils.findAndReplace(uri, "/excel.class", "/table.class");
        if (uri.indexOf('?') == -1)
            uri = uri + "?qf=export.rpt";

        return uri;
    }
    private String getURL() {
        String host = WebServer.getHostName();
        String port = (String) env.get("SERVER_PORT");
        return "http://" + host + ":" + port + getURI();
    }

}
