// Copyright (C) 2004-2014 Tuma Solutions, LLC
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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.FormToHTML;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.ThreadThrottler;


/** Class for creating archives of dashboard web content.
 */
public class HTMLArchiver {

    public static final int OUTPUT_MIME = 0;
    public static final int OUTPUT_JAR = 1;
    public static final int OUTPUT_ZIP = 2;
    public static final int OUTPUT_DIR = 3;

    protected HashSet seenURIs;
    protected WebServer webServer;
    protected DataRepository data;
    protected ArchiveWriter writer;
    protected Map requestContents;
    private Set itemsToWrite;
    private Set excelPartsWritten;
    private boolean postponeWritingAdditionalItems = false;
    private boolean supportAnchors;


    public static void archive(WebServer webServer, DataRepository data,
                               OutputStream outStream, String startingUri,
                               int outputMode)
        throws IOException
    {
        HTMLArchiver m = new HTMLArchiver(webServer, data, outputMode);
        try {
            ThreadThrottler.beginThrottling(getThrottlePercentage());
            m.run(outStream, startingUri);
        } finally {
            ThreadThrottler.endThrottling();
        }
    }

    protected HTMLArchiver(WebServer webServer, DataRepository data,
                           int outputMode) {

        this.seenURIs = new HashSet();
        this.webServer = webServer;
        this.data = data;
        this.requestContents = new HashMap();
        this.itemsToWrite = new TreeSet();
        this.excelPartsWritten = new HashSet();

        switch (outputMode) {
            case OUTPUT_JAR:
                this.writer = new JarArchiveWriter(); break;

            case OUTPUT_ZIP:
                this.writer = new ZipArchiveWriter(); break;

            case OUTPUT_DIR:
                this.writer = new DirArchiveWriter(); break;

            case OUTPUT_MIME: default:
                // the first entry in the MIME archive MUST be the HTML page
                // we want to appear in the browser, so it is important not
                // to write something else first.
                postponeWritingAdditionalItems = true;
                this.writer = new MimeArchiveWriter(); break;
        }

        supportAnchors = writer.supportsAnchors();
    }

    protected void run(OutputStream outStream, String startingURI)
        throws IOException
    {
        writer.startArchive(outStream);
        getMappedURI(startingURI);
        itemsToWrite.add(startingURI);
        String currentPrefix = "";
        while (!itemsToWrite.isEmpty()) {
            String uri = getNextUriToWrite(currentPrefix);
            currentPrefix = extractPrefix(uri);
            writeItem(uri);
            Thread.yield();
        }
        writer.finishArchive();
        outStream.flush();
        data.gc(null);
    }

    private String getNextUriToWrite(String currentPrefix) {
        // first, try to find an writeable item with the same prefix.  (This
        // can help to keep the memory usage of the data repository low.)
        for (Iterator u = itemsToWrite.iterator(); u.hasNext();) {
            String uriToWrite = (String) u.next();
            String prefix = extractPrefix(uriToWrite);
            if (prefix.equals(currentPrefix))
                return uriToWrite;
        }
        // If there were no more items with the same prefix, return any
        // writable item at random.  But first, encourage the repository to
        // clean up calculations made for the old prefix.
        data.gc(Collections.singleton(currentPrefix));
        String result = (String) itemsToWrite.iterator().next();
        logger.log(Level.FINE, "Switching to prefix {0}", extractPrefix(result));
        return result;
    }

    /** Add an item to the archive.
     *
     * @param uri the URI of the item to add.
     */
    protected void writeItem(String uri) {
        itemsToWrite.remove(uri);
        if (seenURIs.contains(uri))
            return;

        seenURIs.add(uri);
        logger.log(Level.FINER, "writing item ''{0}''", uri);
        ThreadThrottler.tick();

        try {
            RequestResult item = openURI(uri);

            if (item.getContents() == null)
                return;

            if (item.getContentType().startsWith("text/html")
                    || item.getContentType().startsWith("text/css"))
                handleHTML(uri, item.getContentType(),
                           item.getContents(),
                           item.getHeaderLength());

            else
                writer.addFile(uri, item.getContentType(),
                               item.getContents(),
                               item.getHeaderLength());

            item.clearContents();
        } catch (IOException ioe) {
            // couldn't open file. this could easily happen if the uri
            // did not name an internal dashboard uri; just move along.
        }
        postponeWritingAdditionalItems = false;
    }

    /** Add an item to the archive if it is not HTML/CSS content.
     * 
     * While an HTML page is being processed for archival, it is necessary to
     * load the items it refers to, to determine their mime type.  If the items
     * loaded in this manner are not HTML, they can immediately be added to the
     * archive verbatim.  This eliminates the need to hold their content in
     * memory any longer than necessary.
     */
    protected void writeIfNotHtml(String uri) {
        if (postponeWritingAdditionalItems)
            return;

        if (seenURIs.contains(uri))
            return;

        try {
            RequestResult item = openURI(uri);
            if (!item.getContentType().startsWith("text/html")
                    && !item.getContentType().startsWith("text/css")) {
                seenURIs.add(uri);
                itemsToWrite.remove(uri);
                logger.log(Level.FINER, "writing item ''{0}''", uri);

                writer.addFile(uri, item.getContentType(),
                               item.getContents(),
                               item.getHeaderLength());
                item.clearContents();
            }

        } catch (IOException ioe) {
            // couldn't open file. this could easily happen if the uri
            // did not name an internal dashboard uri; just move along.
            logger.log(Level.WARNING, "Error when archiving report", ioe);
        }
    }


    protected void handleHTML(String uri, String contentType, byte[] contents,
                              int headerLength)
    {
        try {
            String htmlContent = newString
                (contents, headerLength, contents.length - headerLength,
                 HTTPUtils.getCharset(contentType));
            StringBuffer html = new StringBuffer(htmlContent);
            stripHTMLComments(html);

            Set references = getReferencedItems(htmlContent);
            URL baseURL = new URL("http://ignored" + uri);
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
                            safeURL = writeExcelPart(uri, html, contentType);
                            extra = " target='_blank'";
                        } else if (subURI.equalsIgnoreCase("about:blank")
                                || subURI.endsWith("nohref")) {
                            // no need to alter about:blank references
                            continue;
                        } else {
                            URL u = new URL(baseURL, subURI);
                            String absoluteURI = u.getFile();
                            writeIfNotHtml(absoluteURI);
                            safeURL = getMappedURI(absoluteURI);
                            if (supportAnchors && u.getRef() != null)
                                safeURL = safeURL + "#" + u.getRef();
                            extra = "";
                        }

                        StringUtils.findAndReplace
                            (html, "'"+subURI+"'", "'"+safeURL+"'"+extra);
                        StringUtils.findAndReplace
                            (html, "\""+subURI+"\"", "\""+safeURL+"\""+extra);
                        subURI = HTMLUtils.escapeEntities(subURI);
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
                writeTextFile(uri, contentType, html);

            if (references != null) {
                Iterator i = references.iterator();
                String subURI;
                while (i.hasNext())
                    try {
                        subURI = (String) i.next();
                        if (subURI.indexOf("/data.js") != -1) continue;
                        if (subURI.indexOf(".iqy")     != -1) continue;
                        URL u = new URL(baseURL, subURI);
                        String refUri = u.getFile();
                        if (!seenURIs.contains(refUri))
                            itemsToWrite.add(refUri);

                    } catch (Exception e) {}
            }

        } catch (IOException ioe) {}
        ThreadThrottler.tick();
    }

    protected int findFormScriptStart(StringBuffer html) {
        int pos = StringUtils.indexOf(html, "/data.js");
        if (pos == -1) return -1;
        int beg = Math.max(StringUtils.lastIndexOf(html, "<script", pos),
                StringUtils.lastIndexOf(html, "<SCRIPT", pos));
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
        String excelURL = writeExcelPart(uri, result, contentType);

        // insert an "export to excel" link in the HTML document
        String exportLink = "<a target='_blank' href='"+ excelURL +
            "'>Export to Excel</a>";
        if (pos == -1)
            result.append(exportLink);
        else
            result.insert(pos, exportLink);

        // write out the resulting document
        writeTextFile(uri, contentType, result);
    }

    private void writeTextFile(String uri, String contentType,
                               StringBuffer result) throws IOException {
        byte[] htmlBytes = result.toString().getBytes
            (HTTPUtils.getCharset(contentType));
        writer.addFile(uri, contentType, htmlBytes, 0);
    }

    protected String writeExcelPart(String forUri, StringBuffer content, String htmlContentType)
        throws IOException
    {
        // write out the document with a special content-type to support
        // export-to-excel functionality.
        String safeURL = getMappedURI(forUri);
        String excelURL = makeExcelURL(safeURL);

        if (excelPartsWritten.contains(excelURL))
            return excelURL;

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
        String charset = HTTPUtils.getCharset(htmlContentType);
        String contentType = "application/vnd.ms-excel; charset=" + charset;
        writeTextFile(excelURL, contentType, content);
        excelPartsWritten.add(excelURL);

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


    protected Set getReferencedItems(String html) {
        String normalizedHTML = normalizeHTML(html);

        Set result = new TreeSet();

        getForAttr(normalizedHTML, "HREF=", result);
        getForAttr(normalizedHTML, "href=", result);
        getForAttr(normalizedHTML, "SRC=", result);
        getForAttr(normalizedHTML, "src=", result);
        getForAttr(normalizedHTML, "url(", result);

        // if any HREF atributes are just anchor jumps, remove them.
        for (Iterator i = result.iterator(); i.hasNext();) {
            String item = (String) i.next();
            if (item.startsWith("#"))
                i.remove();
        }

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


    protected static void getForAttr(String text, String attr, Collection v) {
        int pos = 0;
        int end;
        int nextSpace;
        int tagEnd;

        while (true) {
            pos = text.indexOf(attr, pos);

            if (pos == -1)
                return;

            pos += attr.length();

            char delim = text.charAt(pos);

            if ((delim == '\'') || (delim == '"')) {
                end = text.indexOf(delim, pos + 1);

                if (end != -1)
                    v.add(HTMLUtils.unescapeEntities(text.substring(pos + 1,
                            end)));

                pos = end + 1;
            } else {
                nextSpace = text.indexOf(' ', pos);
                tagEnd = text.indexOf('>', pos);
                end = Math.min(nextSpace, tagEnd);
                v.add(HTMLUtils.unescapeEntities(text.substring(pos, end)));
                pos = end + 1;
            }
        }
    }

    public static String getPrefixFromURI(String uri) {
        String prefix = "";
        int slashPos = uri.indexOf("//");

        if (slashPos != -1)
            prefix = HTMLUtils.urlDecode(uri.substring(0, slashPos));

        return prefix;
    }

    protected String makeExcelURL(String url) {
        int dotPos = url.lastIndexOf('.');
        if (dotPos != -1)
            url = url.substring(0, dotPos);

        return url + "_.xls";
    }

    protected String getMappedURI(String uri) throws IOException {
        try {
            return writer.mapURI(uri, getContentType(uri));
        } catch (IOException ioe) {
            logger.fine("Unable to map uri " + uri);
            throw ioe;
        }
    }

    private String extractPrefix(String uri) {
        int dblSlashPos = uri.indexOf("//");
        if (dblSlashPos == -1)
            dblSlashPos = uri.indexOf("/+/");
        if (dblSlashPos == -1)
            return "";
        else
            return uri.substring(0, dblSlashPos);
    }

    protected String getExportURI(String uri) {
        if (uri.indexOf('?') == -1)
            return uri + "?EXPORT=archive";
        else
            return uri + "&EXPORT=archive";
    }

    private String getContentType(String uri) throws IOException {
        return openURI(uri).getContentType();
    }

    private RequestResult openURI(String uri) throws IOException {
        RequestResult results = (RequestResult) requestContents.get(uri);
        if (results == null) {
            results = new RequestResult(uri);
            requestContents.put(uri, results);
        }

        return results;
    }


    private final class RequestResult {
        private String uri;
        private byte[] contents;
        private int headerLength = 0;
        private String header = null;
        private String contentType = null;

        public RequestResult(String uri) throws IOException {
            this.uri = uri;
            this.contents = webServer.getRequest(getExportURI(uri), false);
            ThreadThrottler.tick();

            if (this.contents != null) {
                headerLength = HTTPUtils.getHeaderLength(contents);
                header = new String(contents, 0, headerLength - 2,
                        "ISO-8859-1");
                contentType = HTTPUtils.getContentType(header);

                if (contentType == null)
                    contentType = "text/html";
            }
        }

        public boolean equals(Object obj) {
            return (obj instanceof RequestResult &&
                    ((RequestResult) obj).uri.equals(this.uri));
        }

        public int hashCode() { return uri.hashCode(); }

        public byte[] getContents() { return contents; }
        public String getContentType() { return contentType; }
        // public String getHeader() { return header; }
        public int getHeaderLength() { return headerLength; }
        // public String getUri() { return uri; }

        public void clearContents() {
            contents = null;
        }

    }

    private static final double getThrottlePercentage() {
        try {
            String setting = Settings.getVal(THROTTLE_PERCENT_SETTING);
            if (setting != null) {
                double val = Double.parseDouble(setting) / 100;
                val = Math.min(1.00, Math.max(0.05, val));
                return val;
            }
        } catch (Exception e) {
        }
        return 0.5;
    }

    private static final String THROTTLE_PERCENT_SETTING = HTMLArchiver.class
            .getName() + ".throttlePercent";

    private static final Logger logger = Logger.getLogger(HTMLArchiver.class
            .getName());
}
