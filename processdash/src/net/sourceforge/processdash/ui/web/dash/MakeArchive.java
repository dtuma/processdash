// Copyright (C) 2003-2010 Tuma Solutions, LLC
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



import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIHighVolume;
import net.sourceforge.processdash.tool.export.HTMLArchiver;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class MakeArchive extends TinyCGIBase implements TinyCGIHighVolume {

    protected static Resources resources =
        Resources.getDashBundle("ImportExport.HTMLArchive");
    private static final String SETTING_NAME = "archive.exportMethod";

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
        String url = "archive.class?run&uri=" + HTMLUtils.urlEncode(uri) +
            "&filename=" + HTMLUtils.urlEncode(filename);

        out.print("<html><head><title>\n");
        out.print(resources.getHTML("Title"));
        out.print("</title>\n<meta http-equiv='Refresh' "+
                  "content='1;URL=");
        out.print(url);
        if (parameters.containsKey("out")) {
            out.print("&out=");
            out.print(HTMLUtils.urlEncode(getParameter("out")));
        }
        out.print("'>\n</head><body><h1>");
        out.print(resources.getHTML("Title"));
        out.print("</h1>\n<p>");
        out.print(resources.getHTML("Wait_Message"));
        out.print("</p>\n<p>");
        String fileType = getOutputType();
        String fileTypeTitle = resources.getString
            ("HTMLArchive.Output."+fileType+".Title");
        out.print(HTMLUtils.escapeEntities
            (resources.format("HTMLArchive.Output_FMT", fileTypeTitle)));
        out.print("</p>\n<ul>\n");
        writeFileTypeOption("MIME", url);
        writeFileTypeOption("ZIP", url);
        writeFileTypeOption("JAR", url);
        out.print("</ul>\n</body></html>\n");
    }

    private void writeFileTypeOption(String type, String url) {
        out.print("<li><a href=\"");
        out.print(url);
        out.print("&out=");
        out.print(type);
        out.print("\">");
        out.print(resources.getHTML("HTMLArchive.Output."+type+".Title"));
        out.print("</a> ");
        out.print(resources.getHTML("HTMLArchive.Output."+type+".Description"));
        out.print("</li>\n");
    }

    private String getFilename(String uri) {
        String filename = getParameter("filename");
        if (filename == null)
            return "dash-archive";

        if (filename.indexOf("PREFIX") != -1) {
            String prefix = HTMLArchiver.getPrefixFromURI(uri);
            int pos = prefix.lastIndexOf('/');
            if (pos != -1) prefix = prefix.substring(pos+1);
            prefix = FileUtils.makeSafe(prefix);
            filename = StringUtils.findAndReplace(filename, "PREFIX", prefix);
        }

        return filename;
    }



    /** Determine the URI of the item to archive.
     */
    private String getURI() {
        String referer = (String) env.get("HTTP_REFERER");

        // if the query parameter "uri" is set, return the value.
        String uri = (String) parameters.get("uri");

        if (uri != null) {
            if (uri.startsWith("/") || referer == null)
                return uri;
            else
                return resolveRelativeURI(referer, uri);
        }

        uri = (String) env.get("REQUEST_URI");

        // If the "uri" parameter was not sent to this request, use the
        // uri of the referer.
        if (referer != null) {
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
        // first, check for an explicit output parameter in the URL.
        String outputModeName = getParameter("out");
        int result = decodeOutputMode(outputModeName);
        if (result != -1) {
            InternalSettings.set(SETTING_NAME, outputModeName.toLowerCase());
            return result;
        }

        // next, check for a saved user setting.
        outputModeName = Settings.getVal(SETTING_NAME);
        result = decodeOutputMode(outputModeName);
        if (result != -1)
            return result;

        // finally, choose an appropriate default based on the browser
        // the user is using.
        String userAgent = (String) env.get("HTTP_USER_AGENT");
        if (userAgent.indexOf("MSIE") != -1)
            return HTMLArchiver.OUTPUT_MIME;
        else
            return HTMLArchiver.OUTPUT_ZIP;
    }

    protected int decodeOutputMode(String mode) {
        if ("jar".equalsIgnoreCase(mode))
            return HTMLArchiver.OUTPUT_JAR;
        if ("zip".equalsIgnoreCase(mode))
            return HTMLArchiver.OUTPUT_ZIP;
        if ("mime".equalsIgnoreCase(mode))
            return HTMLArchiver.OUTPUT_MIME;
        if ("dir".equalsIgnoreCase(mode))
            return HTMLArchiver.OUTPUT_DIR;
        return -1;
    }

    protected String getOutputSuffix() {
        switch (getOutputMode()) {
            case HTMLArchiver.OUTPUT_JAR: return ".jar";
            case HTMLArchiver.OUTPUT_ZIP: return ".zip";
            case HTMLArchiver.OUTPUT_DIR: return ".txt";
            case HTMLArchiver.OUTPUT_MIME: default: return ".mhtml";
        }
    }
    protected String getOutputType() {
        switch (getOutputMode()) {
            case HTMLArchiver.OUTPUT_JAR: return "JAR";
            case HTMLArchiver.OUTPUT_ZIP: return "ZIP";
            case HTMLArchiver.OUTPUT_DIR: return "DIR";
            case HTMLArchiver.OUTPUT_MIME: default: return "MIME";
        }
    }

    protected static final String CRLF = "\r\n";
    protected static final SimpleDateFormat filenameDateFormat =
        // ------------------ 05-Dec-2000
        new SimpleDateFormat("dd-MMM-yyyy");

}
