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

package net.sourceforge.processdash.ui.web.dash;



import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIHighVolume;
import net.sourceforge.processdash.tool.export.HTMLArchiver;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class MakeArchive extends TinyCGIBase implements TinyCGIHighVolume {

    protected static Resources resources =
        Resources.getDashBundle("ImportExport.HTMLArchive");

    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            parseFormData();

        if (parameters.containsKey("run")) {
            writeArchiveHeader();
            writeArchiveContents();
        } else {
            writeWaitHeader();
            writeWaitContents();
        }

        this.out.flush();
    }

    protected void writeHeader() { }
    protected void writeContents() { }

    protected void writeWaitHeader() {
        super.writeHeader();
    }


    protected void writeWaitContents() {
        String uri = getURI();
        String filename = getFilename(uri);

        out.print("<html><head><title>\n");
        out.print(resources.getHTML("Title"));
        out.print("</title>\n<meta http-equiv='Refresh' "+
                  "content='1;URL=archive.class?run&uri=");
        out.print(HTMLUtils.urlEncode(uri));
        out.print("&filename=");
        out.print(HTMLUtils.urlEncode(filename));
        if (parameters.containsKey("out")) {
            out.print("&out=");
            out.print(HTMLUtils.urlEncode(getParameter("out")));
        }
        out.print("'>\n</head><body><h1>");
        out.print(resources.getHTML("Title"));
        out.print("</h1>\n");
        out.print(resources.getHTML("Wait_Message"));
        out.print("</body></html>\n");
    }

    private String getFilename(String uri) {
        String filename = getParameter("filename");
        if (filename == null)
            return "dash-archive";

        if (filename.indexOf("PREFIX") != -1) {
            String prefix = HTMLArchiver.getPrefixFromURI(uri);
            int pos = prefix.lastIndexOf('/');
            if (pos != -1) prefix = prefix.substring(pos+1);
            prefix = makeSafe(prefix);
            filename = StringUtils.findAndReplace(filename, "PREFIX", prefix);
        }

        return filename;
    }

    private static String makeSafe(String s) {
        if (s == null) s = "";
        s = s.trim();
        // perform a round-trip through the default platform encoding.
        s = new String(s.getBytes());

        StringBuffer result = new StringBuffer(s);
        char c;
        for (int i = result.length();   i-- > 0; )
            if (-1 == ULTRA_SAFE_CHARS.indexOf(result.charAt(i)))
                result.setCharAt(i, '_');

        return result.toString();
    }
    private static final String ULTRA_SAFE_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "0123456789" + "_" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";



    /** Determine the URI of the item to archive.
     */
    private String getURI() {
        String referer = (String) env.get("HTTP_REFERER");

        // if the query parameter "URI" is set, return the value.
        String uri = (String) parameters.get("uri");

        if (uri != null) {
            if (uri.startsWith("/") || referer == null)
                return uri;
            else
                return resolveRelativeURI(referer, uri);
        }

        uri = (String) env.get("REQUEST_URI");

        // If no query parameters were sent to this request, use the
        // uri of the referer.
        if ((uri.indexOf('?') == -1) && (referer != null)) {
            try {
                if (referer.endsWith("?"))
                    referer = referer.substring(0, referer.length()-1);
                return (new URL(referer)).getFile();
            } catch (MalformedURLException mue) {}
        }

        return null;
    }



    protected void writeArchiveHeader() {
        String filename = getParameter("filename");
        if (filename == null) filename = "dash-archive";
        filename = filename + "-" + filenameDateFormat.format(new Date());

        out.print("Content-type: application/octet-stream" + CRLF);
        out.print("Content-Disposition: attachment; " +
            "filename=\"" + filename + getOutputSuffix() + "\"" + CRLF + CRLF);

        // flush in case writeContents wants to use outStream instead of out.
        out.flush();
    }


    protected void writeArchiveContents() throws IOException {
        String startingURI = getParameter("uri");
        if (startingURI == null)
            throw new NullPointerException();

        HTMLArchiver.archive(getTinyWebServer(), getDataRepository(),
                             outStream, startingURI, getOutputMode());
    }

    protected int getOutputMode() {
        if ("jar".equalsIgnoreCase(getParameter("out")))
            return HTMLArchiver.OUTPUT_JAR;
        if ("zip".equalsIgnoreCase(getParameter("out")))
            return HTMLArchiver.OUTPUT_ZIP;

        String userAgent = (String) env.get("HTTP_USER_AGENT");
        if (userAgent.indexOf("MSIE") != -1)
            return HTMLArchiver.OUTPUT_MIME;
        else
            return HTMLArchiver.OUTPUT_ZIP;
    }

    protected String getOutputSuffix() {
        switch (getOutputMode()) {
            case HTMLArchiver.OUTPUT_JAR: return ".jar";
            case HTMLArchiver.OUTPUT_ZIP: return ".zip";
            case HTMLArchiver.OUTPUT_MIME: default: return ".mhtml";
        }
    }

    protected static final String CRLF = "\r\n";
    protected static final SimpleDateFormat filenameDateFormat =
        // ------------------ 05-Dec-2000
        new SimpleDateFormat("dd-MMM-yyyy");

}
