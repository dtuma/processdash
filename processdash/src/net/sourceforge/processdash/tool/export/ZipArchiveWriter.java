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

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class ZipArchiveWriter implements ArchiveWriter {

    protected ZipOutputStream zipOut;
    protected String defaultFile;
    protected String defaultPath;
    private int itemNumber;
    private Map uriMap;
    private Map<String, String> contentTypeMap;


    //////////////////////////////////////////////////////////////
    // implementation of ArchiveWriter
    //////////////////////////////////////////////////////////////

    public boolean supportsAnchors() {
        return true;
    }

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
        writeManifest();
        writeIndexFile();
        zipOut.finish();
    }



    //////////////////////////////////////////////////////////////
    // internal implementation methods
    //////////////////////////////////////////////////////////////

    protected void init(OutputStream out) throws IOException {
        zipOut = createArchiveOutputStream(out);
        if (zipOut != null)
            zipOut.setLevel(9);
        itemNumber = 0;
        uriMap = new HashMap();
        contentTypeMap = new HashMap<String, String>();
    }

    protected ZipOutputStream createArchiveOutputStream(OutputStream out) throws IOException {
        return new ZipOutputStream(out);
    }

    protected void writeEntry(String uri, String contentType, byte[] content, int offset) throws IOException {
        String safeURI = getSafeURI(uri, contentType);
        String path = getZipEntryFilename(safeURI);

        String dnType = contentType.toLowerCase();
        if (dnType.startsWith("text/html") &&
            dnType.indexOf("charset=") != -1) {
            content = addContentTypeHTMLHeader(contentType, content, offset);
            offset = 0;
        }

        addingZipEntry(path, contentType);
        zipOut.putNextEntry(new ZipEntry(path));
        zipOut.write(content, offset, content.length - offset);
        zipOut.closeEntry();
    }

    protected void addingZipEntry(String path, String contentType) {

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

    private void writeManifest() throws IOException {
        zipOut.putNextEntry(new ZipEntry(getZipEntryFilename(
            MANIFEST_FILENAME)));
        writeManifest(zipOut);
        zipOut.closeEntry();
    }

    protected void writeManifest(OutputStream out) throws IOException {
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, "UTF-8");
        xml.startDocument("UTF-8", null);

        xml.startTag(null, "pdashReportArchive");
        xml.attribute(null, "version", "1.0");

        for (Map.Entry<String, String> e : contentTypeMap.entrySet()) {
            xml.startTag(null, "file");
            xml.attribute(null, "name", e.getKey());
            xml.attribute(null, "contentType", e.getValue());
            if (defaultFile.equals(e.getKey()))
                xml.attribute(null, "isDefault", "true");
            xml.endTag(null, "file");
        }

        xml.endTag(null, "pdashReportArchive");
        xml.endDocument();
    }

    private void writeIndexFile() throws IOException {
        zipOut.putNextEntry(new ZipEntry(INDEX_FILENAME));
        String contents = StringUtils.findAndReplace
            (INDEX_CONTENTS, "DEFAULT_FILE", defaultPath);
        zipOut.write(contents.getBytes(HTTPUtils.DEFAULT_CHARSET));
        zipOut.closeEntry();
    }

    protected String getZipEntryFilename(String fileURI) {
        return FILES_SUBDIR + "/" + fileURI;
    }

    protected String getSafeURI(String uri, String contentType) {
        if (uri.startsWith(ITEM_PREFIX))
            // this is already a safe URL, not a URI!  Just return it.
            return uri;

        String result = (String) uriMap.get(uri);
        if (result == null) {
            result = ITEM_PREFIX + itemNumber++ + getSuffix(contentType);
            if (defaultPath == null)
                defaultPath = getZipEntryFilename(defaultFile = result);
            uriMap.put(uri, result);
            contentTypeMap.put(result, contentType);
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
    protected static final String FILES_SUBDIR = "files";
    private static final String INDEX_FILENAME = "index.htm";
    private static final String MANIFEST_FILENAME = "manifest.xml";
    private static final Map CONTENT_TYPE_SUFFIX_MAP = loadSuffixes();


    private static final String INDEX_CONTENTS =
        "<html><head>" +
        "<meta http-equiv=\"Refresh\" CONTENT=\"0;URL=DEFAULT_FILE\">" +
        "</head><body>&nbsp;</body></html>";


}
