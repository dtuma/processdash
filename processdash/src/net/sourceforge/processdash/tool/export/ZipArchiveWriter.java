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

package net.sourceforge.processdash.tool.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.StringUtils;


public class ZipArchiveWriter implements ArchiveWriter {

    private ZipOutputStream zipOut;
    private int itemNumber;
    private Map uriMap;
    private String defaultURI;


    //////////////////////////////////////////////////////////////
    // implementation of ArchiveWriter
    //////////////////////////////////////////////////////////////

    public void startArchive(OutputStream out) throws IOException {
        init(out);
    }

    public String mapURI(String uri, String contentType) {
        return getSafeURI(uri, contentType);
    }

    public void addFile(String uri, String contentType, byte[] content,
                        int offset)
        throws IOException
    {
        writeEntry(uri, contentType, content, offset);
    }

    public void finishArchive() throws IOException {
        writeIndexFile();
        zipOut.finish();
    }



    //////////////////////////////////////////////////////////////
    // internal implementation methods
    //////////////////////////////////////////////////////////////

    protected void init(OutputStream out) {
        zipOut = createArchiveOutputStream(out);
        zipOut.setLevel(9);
        itemNumber = 0;
        uriMap = new HashMap();
    }

    protected ZipOutputStream createArchiveOutputStream(OutputStream out) {
        return new ZipOutputStream(out);
    }

    protected void writeEntry(String uri, String contentType, byte[] content, int offset) throws IOException {
        String safeURI = getSafeURI(uri, contentType);
        String path = FILES_SUBDIR + "/" + safeURI;

        String dnType = contentType.toLowerCase();
        if (dnType.startsWith("text/html") &&
            dnType.indexOf("charset=") != -1) {
            content = addContentTypeHTMLHeader(contentType, content, offset);
            offset = 0;
        }

        zipOut.putNextEntry(new ZipEntry(path));
        zipOut.write(content, offset, content.length - offset);
        zipOut.closeEntry();
    }

    private byte[] addContentTypeHTMLHeader(String contentType, byte[] content,
                                            int offset) throws IOException
    {
        String charset = HTTPUtils.getCharset(contentType);
        String html = new String
            (content, offset, content.length - offset, charset);
        StringBuffer buf = new StringBuffer(html);
        html = html.toLowerCase();

        String newHeader = "<meta http-equiv=\"Content-Type\" content=\"" +
            contentType + "\">";

        int insPos = html.indexOf("</head>");
        if (insPos == -1) {
            newHeader = "<head>" + newHeader + "</head>";
            insPos = html.indexOf("<body>");
            if (insPos == -1) {
                newHeader = "<html>" + newHeader + "<body>";
                buf.append("</body></html>");
                insPos = 0;
            }
        }
        buf.insert(insPos, newHeader);

        return buf.toString().getBytes(charset);
    }

    private void writeIndexFile() throws IOException {
        zipOut.putNextEntry(new ZipEntry(INDEX_FILENAME));
        String contents = StringUtils.findAndReplace
            (INDEX_CONTENTS, "DEFAULT_FILE", FILES_SUBDIR + "/" + defaultURI);
        zipOut.write(contents.getBytes(HTTPUtils.DEFAULT_CHARSET));
        zipOut.closeEntry();
    }


    protected String getSafeURI(String uri, String contentType) {
        if (uri.startsWith(ITEM_PREFIX))
            // this is already a safe URL, not a URI!  Just return it.
            return uri;

        String result = (String) uriMap.get(uri);
        if (result == null) {
            result = ITEM_PREFIX + itemNumber++ + getSuffix(contentType);
            if (defaultURI == null) defaultURI = result;
            uriMap.put(uri, result);
        }
        return result;
    }

    private String getSuffix(String contentType) {
        // strip off trailing qualifiers, like charset parameters
        int semicolonPos = contentType.indexOf(';');
        if (semicolonPos != -1)
            contentType = contentType.substring(0, semicolonPos);

        String result = (String) CONTENT_TYPE_SUFFIX_MAP.get
            (contentType.toLowerCase());
        return (result == null ? "" : result);
    }

    private static Map loadSuffixes() {
        Map result = new HashMap();

        Map mimeTypes = new TreeMap(WebServer.getMimeTypeMap());
        for (Iterator i = mimeTypes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String type = e.getValue().toString().toLowerCase();
            if (!result.containsKey(type))
                result.put(type, e.getKey());
        }

        // special overrides
        result.remove("application/octet-stream");
        result.put("text/plain", ".txt");

        return Collections.unmodifiableMap(result);
    }


    private static final String ITEM_PREFIX = "arch_item_";
    private static final String FILES_SUBDIR = "files";
    private static final String INDEX_FILENAME = "index.htm";
    private static final Map CONTENT_TYPE_SUFFIX_MAP = loadSuffixes();


    private static final String INDEX_CONTENTS =
        "<html><head>" +
        "<meta http-equiv=\"Refresh\" CONTENT=\"0;URL=DEFAULT_FILE\">" +
        "</head><body>&nbsp;</body></html>";


}
