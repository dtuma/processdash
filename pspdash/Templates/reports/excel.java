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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import pspdash.StringUtils;
import pspdash.data.DataRepository;
import pspdash.data.ResultSet;

public class excel extends pspdash.TinyCGIBase {

    private static String exportMethod =
        pspdash.Settings.getVal("excel.exportMethod");

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
    }

    protected void writeHTML() throws IOException {
        // write header
        out.print("Content-type: application/vnd.ms-excel\r\n\r\n");

        // write contents
        byte [] contents = getTinyWebServer().getRequest(getURI(), true);
        String results = new String(contents);
        int beginPos = results.indexOf("<TABLE");
        int endPos   = results.lastIndexOf("/TABLE");
        if (endPos != -1) endPos = results.indexOf('>', endPos);

        if (beginPos == -1 || endPos == -1)
            out.println(results);
        else {
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
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            host = "localhost";
        }
        String port = (String) env.get("SERVER_PORT");
        return "http://" + host + ":" + port + getURI();
    }

}
