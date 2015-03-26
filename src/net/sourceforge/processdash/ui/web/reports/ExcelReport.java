// Copyright (C) 2001-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class ExcelReport extends TinyCGIBase {

    public static final String EXPORT_TAG = "excel";

    protected void writeHeader() { }

    protected void writeContents() throws IOException {
        if (useIQY())
            writeIQY();
        else
            writeHTML();
    }

    protected void writeIQY() throws IOException {
        // write header
        out.print("Content-Disposition: attachment; filename=\"excel.iqy\"\r\n");
        out.print("Content-type: text/x-ms-iqy\r\n\r\n");

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
        // check the user setting for a requested behavior
        String exportMethod = Settings.getVal("excel.exportMethod");
        if ("mime".equalsIgnoreCase(exportMethod))
            return false;
        else if ("iqy".equalsIgnoreCase(exportMethod))
            return true;
        else
            // default behavior
            return true;
    }

    private String getURI() {
        String uri = getURIImpl();
        return HTMLUtils.appendQuery(uri, "EXPORT", EXPORT_TAG);
    }

    private String getURIImpl() {
        String referer = (String) env.get("HTTP_REFERER");

        // if the query parameter "URI" is set, resolve it relative to
        // the referer URI and return the value.
        String uri = (String) parameters.get("uri");
        if (uri != null)
            return resolveRelativeURI(referer, uri);

        uri = (String) env.get("REQUEST_URI");

        // If no query parameters were sent to this request, use the
        // uri of the referer.
        if ((uri.indexOf('?') == -1 || uri.endsWith("?fullPage"))
            && referer != null) try {
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
        String uri = getURI();
        int pos = uri.indexOf("//");
        if (pos != -1)
            // work around bug in Excel 2003. (Excel can't handle the
            // double slash that appears in dashboard URLs.)
            uri = uri.substring(0, pos) + "/+/" + uri.substring(pos+2);
        return getRequestURLBase() + uri;
    }

}
