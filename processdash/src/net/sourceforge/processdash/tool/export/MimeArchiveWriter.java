// Copyright (C) 2004-2006 Tuma Solutions, LLC
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
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTTPUtils;


/** Class for creating MIME Encoded HTML archives of dashboard content.
 *
 * For more information on MIME Encoded HTML archives, see
 *     http://www.ietf.org/rfc/rfc2110.txt
 *
 * Current bug: if the item named by the starting URI is a dashboard form
 * or a page containing a link to "excel.iqy", the excel file will become
 * the root page instead.
 */
public class MimeArchiveWriter implements ArchiveWriter {

    private String base;
    private String boundary;
    private HashMap safeUriMap;
    private int itemNumber;
    private OutputStream outStream;
    private PrintWriter out;
    private String charset;

    public MimeArchiveWriter() {
        charset = TinyCGIBase.getDefaultCharset();
    }

    //////////////////////////////////////////////////////////////
    // implementation of ArchiveWriter
    //////////////////////////////////////////////////////////////

    public boolean supportsAnchors() {
        return false;
    }

    public void startArchive(OutputStream out) {
        init(out);
        writeMimeHeader();
    }

    public String mapURI(String uri, String contentType) {
        return getSafeURL(uri);
    }

    public void addFile(String uri, String contentType, byte[] content,
                        int offset) throws IOException {
        writeMimePart(uri, contentType, content, offset);
    }

    public void finishArchive() {
        writeMimeEnding();
    }



    //////////////////////////////////////////////////////////////
    // internal implementation methods
    //////////////////////////////////////////////////////////////


    /** Initialize data */
    private void init(OutputStream outStream) {
        base = "http://localhost:9999";
        boundary = createBoundary();
        safeUriMap = new HashMap();
        itemNumber = 0;


        this.outStream = outStream;
        this.out = new PrintWriter(outStream);
    }

    /** Create a randomly generated MIME-part boundary.
     */
    private String createBoundary() {
        if (testing())
            return "Boundary=_.12345.67890.ABCDE.FGHIJ.KLMNO";

        StringBuffer boundary = new StringBuffer("Boundary=_");
        Random r = new Random();

        while (boundary.length() < 45) {
            int i = r.nextInt();

            if (i < 0)
                i = 0 - i;

            boundary.append(".")
                .append(Integer.toString(i, Character.MAX_RADIX));
        }

        String result = boundary.toString();

        if (result.length() > 45)
            result = result.substring(0, 45);

        return result;
    }



    private String getSafeURL(String uri) {
        // the URL we use needs to be generally recognizable and
        // parseable as a URL.  It does not have to correspond to any
        // real URL, however.  I intentionally don't use the real
        // URLs, for two reasons:
        // 1) Using real URLs means that items missing from the
        //    archive might get loaded on the fly from the running
        //    dashboard, which misrepresents archiving functionality
        // 2) Some valid URLs seem to confuse internet explorer. For
        //    example, a url containing the string "%23" (the URL encoded
        //    version of "#") seems to confuse IE enough that it fails to
        //    locate the identically named mime part within the archive.

        if (uri.startsWith(base + "/item"))
            // this is already a safe URL, not a URI!  Just return it.
            return uri;

        String result = (String) safeUriMap.get(uri);
        if (result == null) {
            result = base + "/item" + itemNumber++;
            safeUriMap.put(uri, result);
        }
        return result;
    }



    /** Write the header of the MIME-HTML file.
     */
    private void writeMimeHeader() {
        out.print("From: <Saved by the Process Dashboard>" + CRLF);
        out.print("Subject: <Archived Data>" + CRLF);
        out.print("Date: ");
        Date now = (testing() ? new Date(0) : new Date());
        out.print(dateFormat.format(now));
        out.print(CRLF);
        out.print("MIME-Version: 1.0" + CRLF);
        out.print("Content-Type: multipart/related;" + CRLF);
        out.print("\tboundary=\"" + boundary + "\";" + CRLF);
        out.print("\ttype=\"text/html\";" + CRLF);
        out.print("\tcharset=\"" + charset + "\"" + CRLF + CRLF);
        out.print("This is a multi-part message in MIME format." + CRLF);
        out.flush();
    }


    /** Write the closing boundary of the MIME-HTML file.
     */
    private void writeMimeEnding() {
        out.print(CRLF + "--" + boundary + "--" + CRLF);
        out.flush();
    }


    private void writePartHeader(String uri, String contentType) {
        out.print(CRLF + "--" + boundary + CRLF);
        out.print("Content-Type: " + contentType + CRLF);
        out.print("Content-Transfer-Encoding: binary" + CRLF);
        out.print("Content-Location: " + mapURI(uri, contentType) +CRLF+CRLF);
        out.flush();
    }


    private void writeMimePart(String uri, String contentType,
                               byte[] contents, int headerLength)
        throws IOException
    {
        if (contentType.startsWith("text/")) {
            String partCharset = HTTPUtils.getCharset(contentType);
            if (!this.charset.equalsIgnoreCase(partCharset)) {
                contents = translateCharset(contents, headerLength,
                                            partCharset, this.charset);
                headerLength = 0;
                contentType = HTTPUtils.setCharset(contentType, this.charset);
            }
        }

        writePartHeader(uri, contentType);
        outStream.write(contents, headerLength,
                        contents.length - headerLength);
    }



    private byte[] translateCharset(byte[] contents, int headerLength,
                                    String srcCharset, String destCharset)
        throws IOException
    {
        String text = new String
            (contents, headerLength, contents.length - headerLength,
             srcCharset);
        return text.getBytes(destCharset);
    }


    private boolean testing() {
        return Boolean.getBoolean("processdash.testArchiver")
            || Settings.getBool("export.testArchiver", false);
    }

    private static final String CRLF = "\r\n";
    private static final SimpleDateFormat dateFormat =
            // ------------------ Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

}
