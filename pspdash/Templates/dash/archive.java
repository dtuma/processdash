import pspdash.StringUtils;
import pspdash.TinyCGIBase;
import pspdash.TinyWebServer;

import pspdash.data.FormToHTML;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;


public class archive extends TinyCGIBase {
    private String base;
    private String boundary;
    private HashSet seenURIs;

    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);

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
        out.print("Content-type: text/html" + CRLF + CRLF);
        out.flush();
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
            String prefix = getPrefixFromURI(uri);
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

        // We don't actually use the correct URL to the dashboard - if we did,
        // missing items would automatically get loaded from the running
        // dashboard.
        base = "http://localhost:9999";
        boundary = createBoundary();
        seenURIs = new HashSet();

        writeMimeHeader();
        writeItemAndRecurse(startingURI);
        writeMimeEnding();
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

            boundary.append(".").append(Integer.toString(i, Character.MAX_RADIX));
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
            byte[] contents = getRequest(getExportURI(uri), false);

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
    private String getExportURI(String uri) {
        if (uri.indexOf('?') == -1)
            return uri + "?EXPORT=archive";
        else
            return uri + "&EXPORT=archive";
    }


    private void writeMimePart(String uri, String contentType, byte[] contents,
        int headerLength) throws IOException
    {
        writePartHeader(uri, contentType);
        outStream.write(contents, headerLength, contents.length - headerLength);
    }


    private void writeMimePart(String uri, String contentType, StringBuffer content)
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
        out.print("Content-Location: " + base + uri + CRLF + CRLF);
        out.flush();
    }


    /** Determine the length (in bytes) of the header in an HTTP response.
     */
    private int getHeaderLength(byte[] result) {
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
    private String getContentType(String header) {
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


    private void handleHTML(String uri, byte[] contents, int headerLength,
        String contentType)
    {
        try {
            String htmlContent = newString
                (contents, headerLength, contents.length - headerLength,
                 getCharset(contentType));
            StringBuffer html = new StringBuffer(htmlContent);
            String normalizedHTML = normalizeHTML(htmlContent);

            ArrayList references = getReferencedItems(normalizedHTML);
            URL baseURL = new URL(base + uri);
            if (references != null) {
                Iterator i = references.iterator();
                String subURI;
                while (i.hasNext())
                    try {
                        subURI = (String) i.next();
                        URL u = new URL(baseURL, subURI);

                        /*if (subURI.startsWith("../") ||
                            subURI.startsWith("./")) */
                            StringUtils.findAndReplace
                                (html, subURI, u.toString());

                    } catch (Exception e) {}
            }

            if (isDashboardForm(normalizedHTML))

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
                        URL u = new URL(baseURL, subURI);
                        writeItemAndRecurse(u.getFile());

                    } catch (Exception e) {}
            }

        } catch (IOException ioe) {}
    }

    private int findFormScriptStart(String html) {
        int pos = html.indexOf("/data.js");
        if (pos == -1) return -1;
        int beg = html.lastIndexOf("<script", pos);
        if (beg == -1) beg = html.lastIndexOf("<SCRIPT", pos);
        return beg;
    }

    private int findFormScriptStart(StringBuffer html) {
        int pos = StringUtils.indexOf(html, "/data.js");
        if (pos == -1) return -1;
        int beg = StringUtils.lastIndexOf(html, "<script", pos);
        if (beg == -1) beg = StringUtils.lastIndexOf(html, "<SCRIPT", pos);
        return beg;
    }

    private boolean isDashboardForm(String normalizedHTML) {
        return (findFormScriptStart(normalizedHTML) != -1);
    }


    /**
     */
    private void handleDashboardForm(String uri, String contentType,
                                     StringBuffer result)
        throws IOException
    {
        // translate the HTML document using the FormToHTML class.
        String prefix = getPrefixFromURI(uri);
        FormToHTML.translate(result, getDataRepository(), prefix);

        // locate and delete the <script> tag from the HTML document.
        int pos = findFormScriptStart(result);
        if (pos != -1)
            deleteScriptElement(result, pos);
        else {
            pos = StringUtils.lastIndexOf(result, "</body>");
            if (pos == -1) pos = StringUtils.lastIndexOf(result, "</BODY>");
        }

        /*
        // write out the document with a special content-type to support
        // export-to-excel functionality.
        String excelURI = makeExcelURI(uri);
        writeMimePart(excelURI, "application/vnd.ms-excel", result);

        // insert an "export to excel" link in the HTML document, and write
        // it out
        String exportLink = "<hr><a href='"+ base + excelURI +"'>Export to Excel</a>";
        if (pos == -1)
            result.append(exportLink);
        else
            result.insert(pos, exportLink);
        */
        writeMimePart(uri, contentType, result);
    }


    private String getPrefixFromURI(String uri) {
        String prefix = "";
        int slashPos = uri.indexOf("//");

        if (slashPos != -1)
            prefix = URLDecoder.decode(uri.substring(0, slashPos));

        return prefix;
    }

    private String makeExcelURI(String uri) {
        if (uri.indexOf('?') == -1)
            return uri + "?exportToExcel";
        else
            return uri + "&exportToExcel";
    }


    private void deleteScriptElement(StringBuffer result, int scriptStart) {
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


    private String newString(byte[] bytes, int offset, int len, String enc) {
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


    private String getNormalizedHTML(String contentType, byte[] contents,
        int headerLength) {
        String charset = getCharset(contentType);
        String html = newString(contents, headerLength,
                contents.length - headerLength, charset);

        return normalizeHTML(html);
    }


    private ArrayList getReferencedItems(String normalizedHTML) {
        ArrayList result = new ArrayList();

        getForAttr(normalizedHTML, "HREF", result);
        getForAttr(normalizedHTML, "href", result);
        getForAttr(normalizedHTML, "SRC", result);
        getForAttr(normalizedHTML, "src", result);

        return result;
    }


    private String normalizeHTML(String html) {
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


    private static void getForAttr(String text, String attr, ArrayList v) {
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


    private String getCharset(String contentType) {
        String upType = contentType.toUpperCase();
        int pos = upType.indexOf("charset=");

        if (pos == -1)
            return null;

        int beg = pos + 8;

        return contentType.substring(beg);
    }

    private static final String CRLF = "\r\n";
    private static final SimpleDateFormat dateFormat =
        // ------------------ Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final SimpleDateFormat filenameDateFormat =
        // ------------------ 05-Dec-2000
        new SimpleDateFormat("dd-MMM-yyyy");

}
