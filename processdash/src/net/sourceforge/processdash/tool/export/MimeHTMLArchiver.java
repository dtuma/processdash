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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;


import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.FormToHTML;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.*;

/** Class for creating MIME Encoded HTML archives of dashboard content.
 *
 * For more information on MIME Encoded HTML archives, see
 *     http://www.ietf.org/rfc/rfc2110.txt
 *
 * Current bug: if the item named by the starting URI is a dashboard form
 * or a page containing a link to "excel.iqy", the excel file will become
 * the root page instead.
 */
public class MimeHTMLArchiver {

    protected String base;
    protected String boundary;
    protected HashSet seenURIs;
    protected HashMap safeUriMap;
    protected int itemNumber;
    protected OutputStream outStream;
    protected PrintWriter out;
    protected WebServer webServer;
    protected DataRepository data;


    public static void archive(WebServer webServer, DataRepository data,
                               OutputStream outStream, String startingUri)
        throws IOException
    {
        MimeHTMLArchiver m = new MimeHTMLArchiver(webServer, data);
        m.run(outStream, startingUri);
    }

    protected MimeHTMLArchiver(WebServer webServer, DataRepository data) {
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
        base = "http://localhost:9999";
        boundary = createBoundary();
        seenURIs = new HashSet();
        safeUriMap = new HashMap();
        itemNumber = 0;
        this.webServer = webServer;
        this.data = data;
    }



    protected void run(OutputStream outStream, String startingURI)
        throws IOException
    {
        this.outStream = outStream;
        this.out = new PrintWriter(outStream);

        getSafeURL(startingURI);

        writeMimeHeader();
        writeItemAndRecurse(startingURI);
        writeMimeEnding();
        out.flush();
    }


    /** Create a randomly generated MIME-part boundary.
     */
    private String createBoundary() {
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


    /** Write the header of the MIME-HTML file.
     */
    protected void writeMimeHeader() {
        out.print("From: <Saved by the Process Dashboard>" + CRLF);
        out.print("Subject: <Archived Data>" + CRLF);
        out.print("Date: ");
        out.print(dateFormat.format(new Date()));
        out.print(CRLF);
        out.print("MIME-Version: 1.0" + CRLF);
        out.print("Content-Type: multipart/related;" + CRLF);
        out.print("\tboundary=\"" + boundary + "\";" + CRLF);
        out.print("\ttype=\"text/html\"" + CRLF + CRLF);
        out.print("This is a multi-part message in MIME format." + CRLF);
    }


    /** Write the closing boundary of the MIME-HTML file.
     */
    protected void writeMimeEnding() {
        out.print(CRLF + "--" + boundary + "--" + CRLF);
    }


    /** Add an item to the MIME archive, and recursively add any items it
     * refers to.
     *
     * @param uri the URI of the item to add.
     */
    protected void writeItemAndRecurse(String uri) {
        //System.out.println("writeItemAndRecurse("+uri+")");
        if (seenURIs.contains(uri))
            return;

        seenURIs.add(uri);

        try {
            byte[] contents = webServer.getRequest(getExportURI(uri), false);

            if (contents == null)
                return;

            int headerLength = getHeaderLength(contents);
            String header = new String(contents, 0, headerLength - 2,
                    "ISO-8859-1");
            String contentType = getContentType(header);

            if (contentType == null)
                contentType = "text/html";

            if (contentType.startsWith("text/html"))
                handleHTML(uri, contents, headerLength, contentType);
            else
                writeMimePart(uri, contentType, contents, headerLength);
        } catch (IOException ioe) {
            // couldn't open file. this could easily happen if the uri
            // did not name an internal dashboard uri; just move along.
        }
    }
    protected String getExportURI(String uri) {
        if (uri.indexOf('?') == -1)
            return uri + "?EXPORT=archive";
        else
            return uri + "&EXPORT=archive";
    }


    protected void writeMimePart(String uri, String contentType, byte[] contents,
        int headerLength) throws IOException
    {
        writePartHeader(uri, contentType);
        outStream.write(contents, headerLength, contents.length - headerLength);
    }


    protected void writeMimePart(String uri, String contentType, StringBuffer content)
        throws IOException
    {
        writePartHeader(uri, contentType);
        for (int i = 0; i < content.length(); i++)
            out.print(content.charAt(i));
        out.flush();
    }


    protected void writePartHeader(String uri, String contentType) {
        out.print(CRLF + "--" + boundary + CRLF);
        out.print("Content-Type: " + contentType + CRLF);
        out.print("Content-Transfer-Encoding: binary" + CRLF);
        out.print("Content-Location: " + getSafeURL(uri) + CRLF + CRLF);
        out.flush();
    }


    /** Determine the length (in bytes) of the header in an HTTP response.
     */
    protected int getHeaderLength(byte[] result) {
        int a = 0;
        int b = 1;
        int c = 2;
        int d = 3;

        do {
            if ((result[a] == '\r') && (result[b] == '\n') &&
                    (result[c] == '\r') && (result[d] == '\n'))
                return (d + 1);

            a++;
            b++;
            c++;
            d++;
        } while (d < result.length);

        return result.length;
    }


    /** Extract the content type from the given HTTP response headers.
     */
    protected String getContentType(String header) {
        String upHeader = CRLF + header.toUpperCase();
        int pos = upHeader.indexOf(CRLF + "CONTENT-TYPE:");

        if (pos == -1)
            return null;

        int beg = pos + 15; // add length of header name and CRLF

        // ASSUMPTION: not supporting wrapped headers
        int end = upHeader.indexOf(CRLF, beg);

        if (end == -1)
            end = upHeader.length();

        return header.substring(beg - 2, end - 2).trim();
    }


    protected void handleHTML(String uri, byte[] contents, int headerLength,
        String contentType)
    {
        try {
            String htmlContent = newString
                (contents, headerLength, contents.length - headerLength,
                 getCharset(contentType));
            StringBuffer html = new StringBuffer(htmlContent);
            stripHTMLComments(html);

            ArrayList references = getReferencedItems(htmlContent);
            URL baseURL = new URL(base + uri);
            if (references != null) {
                Iterator i = references.iterator();
                String subURI, safeURL, extra;
                while (i.hasNext())
                    try {
                        subURI = (String) i.next();


                        if (subURI.indexOf("/data.js") != -1) {
                            // don't alter the data.js reference - we'll
                            // remove it later.
                            continue;
                        } else if (subURI.indexOf(".iqy") != -1) {
                            // special handling for excel export links
                            safeURL = writeExcelPart(uri, html);
                            extra = " target='_blank'";
                        } else {
                            URL u = new URL(baseURL, subURI);
                            safeURL = getSafeURL(u.getFile());
                            extra = "";
                        }

                        StringUtils.findAndReplace
                            (html, "'"+subURI+"'", "'"+safeURL+"'"+extra);
                        StringUtils.findAndReplace
                            (html, "\""+subURI+"\"", "\""+safeURL+"\""+extra);

                    } catch (Exception e) {}
            }

            if (isDashboardForm(html))

                // if this is a form, translate it to HTML before
                // displaying it.
                handleDashboardForm(uri, contentType, html);
            else

                // if this is not a form, write it out verbatim
                writeMimePart(uri, contentType, html);


            if (references != null) {
                Iterator i = references.iterator();
                String subURI;
                while (i.hasNext())
                    try {
                        subURI = (String) i.next();
                        if (subURI.indexOf("/data.js") != -1) continue;
                        if (subURI.indexOf(".iqy")     != -1) continue;
                        URL u = new URL(baseURL, subURI);
                        writeItemAndRecurse(u.getFile());

                    } catch (Exception e) {}
            }

        } catch (IOException ioe) {}
    }

    protected String getSafeURL(String uri) {
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

    protected int findFormScriptStart(StringBuffer html) {
        int pos = StringUtils.indexOf(html, "/data.js");
        if (pos == -1) return -1;
        int beg = StringUtils.lastIndexOf(html, "<script", pos);
        if (beg == -1) beg = StringUtils.lastIndexOf(html, "<SCRIPT", pos);
        return beg;
    }

    protected boolean isDashboardForm(StringBuffer html) {
        return (findFormScriptStart(html) != -1);
    }


    /**
     */
    protected void handleDashboardForm(String uri, String contentType,
                                     StringBuffer result)
        throws IOException
    {
        // translate the HTML document using the FormToHTML class.
        String prefix = getPrefixFromURI(uri);
        FormToHTML.translate(result, data, prefix);

        // locate and delete the <script> tag from the HTML document.
        int pos = findFormScriptStart(result);
        if (pos != -1)
            deleteScriptElement(result, pos);
        else {
            pos = StringUtils.lastIndexOf(result, "</body>");
            if (pos == -1) pos = StringUtils.lastIndexOf(result, "</BODY>");
        }

        // write the target of the "export to excel" link.
        String excelURL = writeExcelPart(uri, result);

        // insert an "export to excel" link in the HTML document
        String exportLink = "<a target='_blank' href='"+ excelURL +
            "'>Export to Excel</a>";
        if (pos == -1)
            result.append(exportLink);
        else
            result.insert(pos, exportLink);

        // write out the resulting document
        writeMimePart(uri, contentType, result);
    }

    protected String writeExcelPart(String forUri, StringBuffer content)
        throws IOException
    {
        // write out the document with a special content-type to support
        // export-to-excel functionality.
        String safeURL = getSafeURL(forUri);
        String excelURL = makeExcelURL(safeURL);

        // hide portions that are marked with the "doNotPrint" style.
        hideNonPrintingElements(content);

        // disable links to external stylesheets - they'll cause Excel to hang.
        StringUtils.findAndReplace(content, "<link", "<LiNt");
        StringUtils.findAndReplace(content, "<LINK", "<LiNt");

        // disable hyperlinks - they won't work
        StringUtils.findAndReplace(content, "<a","<dis_a");
        StringUtils.findAndReplace(content, "<A","<dis_a");
        StringUtils.findAndReplace(content, "</a>", "</dis_a>");
        StringUtils.findAndReplace(content, "</A>", "</dis_a>");

        // disable images - although Excel doesn't display them, it crashes
        // when they aren't accessible.
        StringUtils.findAndReplace(content, "<img", "<dis_img");
        StringUtils.findAndReplace(content, "<IMG", "<dis_img");

        // write it out.
        writeMimePart(excelURL, "application/vnd.ms-excel", content);

        // undo the damage we did earlier
        StringUtils.findAndReplace(content, "<dis_img", "<img");
        StringUtils.findAndReplace(content, "</dis_a>", "</a>");
        StringUtils.findAndReplace(content, "<dis_a","<a");
        StringUtils.findAndReplace(content, "<LiNt", "<link");
        unhideElements(content);

        return excelURL;
    }

    protected void hideNonPrintingElements(StringBuffer html) {
        int pos = 0, beg, end;
        // find instances of "doNotPrint" in the document.
        while ((pos = StringUtils.indexOf(html, "doNotPrint", pos)) != -1) {
            // find the "<" which opens the nonprinting element.
            beg = StringUtils.lastIndexOf(html, "<", pos);
            if (beg == -1) { pos++; continue; }
            // get the tag name of the element (e.g. "span", "p", "a")
            StringBuffer tagName = new StringBuffer();
            int i = beg;
            while ((i < html.length()) &&
                   (" \t\n".indexOf(html.charAt(++i)) == -1))
                tagName.append(html.charAt(i));
            // locate the matching end tag. Note that this means that
            // people need to pedantically close tags like "<p>".
            end = findEndTagEnd(html, pos, tagName.toString());
            if (end == -1) return;
            html.insert(end, " HIDDEN--");
            html.insert(beg+1, "!--HIDDEN ");
            pos = end + 19;     // we just inserted 19 characters
        }
    }

    /** Locate the end of an html tag with the given name.
     *
     * This attempts to locate a closing tag "</a>"; if this fails, it simply
     * looks for ">".
     *
     * @return the position of the ">" character that ends this tag, or -1
     *   if none could be found.
     */
    protected int findEndTagEnd(StringBuffer html, int pos, String tag) {
        int result = StringUtils.indexOf(html, "</"+tag+">", pos, true);
        if (result != -1) {
            result += tag.length() + 2;
        } else {
            result = StringUtils.indexOf(html, ">", pos);
        }
        return result;
    }
    protected int minPos(int a, int b) {
        if (a == -1) return b;
        if (b == -1) return a;
        return Math.min(a, b);
    }

    protected void unhideElements(StringBuffer content) {
        StringUtils.findAndReplace(content, " HIDDEN--", "");
        StringUtils.findAndReplace(content, "!--HIDDEN ", "");
    }

    public static String getPrefixFromURI(String uri) {
        String prefix = "";
        int slashPos = uri.indexOf("//");

        if (slashPos != -1)
            prefix = HTMLUtils.urlDecode(uri.substring(0, slashPos));

        return prefix;
    }

    protected String makeExcelURL(String url) {
        return url + "_.xls";
    }


    protected void deleteScriptElement(StringBuffer result, int scriptStart) {
        if (scriptStart == -1) return;
        int scriptEnd = StringUtils.indexOf(result, "</script>", scriptStart);
        if (scriptEnd == -1)
            scriptEnd = StringUtils.indexOf(result, "</SCRIPT>", scriptStart);
        if (scriptEnd != -1)
            scriptEnd += 9;
        else {
            scriptEnd = StringUtils.indexOf(result, ">", scriptStart);
            if (scriptEnd != -1) scriptEnd++;
        }
        if (scriptEnd != -1)
            result.delete(scriptStart, scriptEnd);
    }


    protected String newString(byte[] bytes, int offset, int len, String enc) {
        if (enc != null) {
            try {
                return new String(bytes, offset, len, enc);
            } catch (UnsupportedEncodingException uee) {}
        }

        try {
            return new String(bytes, offset, len, "ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {}

        return new String(bytes, offset, len);
    }


    protected ArrayList getReferencedItems(String html) {
        String normalizedHTML = normalizeHTML(html);

        ArrayList result = new ArrayList();

        getForAttr(normalizedHTML, "HREF", result);
        getForAttr(normalizedHTML, "href", result);
        getForAttr(normalizedHTML, "SRC", result);
        getForAttr(normalizedHTML, "src", result);

        return result;
    }


    protected String normalizeHTML(String html) {
        StringBuffer buf = new StringBuffer(html);
        StringUtils.findAndReplace(buf, "\t", " ");
        StringUtils.findAndReplace(buf, "\n", " ");

        int len = -1;

        while (len != buf.length()) {
            len = buf.length();
            StringUtils.findAndReplace(buf, " =", "=");
            StringUtils.findAndReplace(buf, "= ", "=");
        }

        buf.append(" >");

        return buf.toString();
    }

    protected void stripHTMLComments(StringBuffer html) {
        int beg = 0, end;
        while ((beg = StringUtils.indexOf(html, "<!--", beg)) != -1) {
            end = StringUtils.indexOf(html, "-->", beg+4);
            if (end == -1) return;
            html.delete(beg, end+3);
        }
    }


    protected static void getForAttr(String text, String attr, ArrayList v) {
        int pos = 0;
        int end;
        int nextSpace;
        int tagEnd;
        attr = attr + "=";

        while (true) {
            pos = text.indexOf(attr, pos);

            if (pos == -1)
                return;

            pos += attr.length();

            char delim = text.charAt(pos);

            if ((delim == '\'') || (delim == '"')) {
                end = text.indexOf(delim, pos + 1);

                if (end != -1)
                    v.add(text.substring(pos + 1, end));

                pos = end + 1;
            } else {
                nextSpace = text.indexOf(' ', pos);
                tagEnd = text.indexOf('>', pos);
                end = Math.min(nextSpace, tagEnd);
                v.add(text.substring(pos, end));
                pos = end + 1;
            }
        }
    }


    protected String getCharset(String contentType) {
        String upType = contentType.toUpperCase();
        int pos = upType.indexOf("charset=");

        if (pos == -1)
            return null;

        int beg = pos + 8;

        return contentType.substring(beg);
    }

    protected static final String CRLF = "\r\n";
    protected static final SimpleDateFormat dateFormat =
        // ------------------ Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

}
