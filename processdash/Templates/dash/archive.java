// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


import pspdash.StringUtils;
import pspdash.TinyCGIBase;
import pspdash.TinyCGIHighVolume;
import pspdash.MimeHTMLArchiver;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;


public class archive extends TinyCGIBase implements TinyCGIHighVolume {

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

        out.print("<html><head>\n");
        out.print("<title>Archiving Dashboard Data</title>\n");
        out.print("<meta http-equiv='Refresh' "+
                  "content='1;URL=archive.class?run&uri=");
        out.print(URLEncoder.encode(uri));
        out.print("&filename=");
        out.print(URLEncoder.encode(filename));
        out.print("'>\n");
        out.print("</head><body><h1>Archiving Dashboard Data</h1>\n");
        out.print("Please wait while the dashboard creates a web archive ");
        out.print("of your data. When the archive is ready, your download ");
        out.print("should begin automatically.\n");
        out.print("</body></html>\n");
    }

    private String getFilename(String uri) {
        String filename = getParameter("filename");
        if (filename == null)
            return "dash-archive";

        if (filename.indexOf("PREFIX") != -1) {
            String prefix = MimeHTMLArchiver.getPrefixFromURI(uri);
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

        if (uri != null)
            return uri;

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
            "filename=\"" + filename + ".mhtml\"" + CRLF + CRLF);

        // flush in case writeContents wants to use outStream instead of out.
        out.flush();
    }


    protected void writeArchiveContents() throws IOException {
        String startingURI = getParameter("uri");
        if (startingURI == null)
            throw new NullPointerException();

        MimeHTMLArchiver.archive(getTinyWebServer(),
                                 getDataRepository(),
                                 outStream, startingURI);
    }

    protected static final String CRLF = "\r\n";
    protected static final SimpleDateFormat filenameDateFormat =
        // ------------------ 05-Dec-2000
        new SimpleDateFormat("dd-MMM-yyyy");

}
