// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import pspdash.data.DataRepository;
import java.net.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.*;

public class TinyWebServer extends Thread {

    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();
    URL [] roots = null;
    DataRepository data = null;
    CGILoader cgiLoader = new CGILoader();
    Hashtable cgiCache = new Hashtable();
    boolean allowRemoteConnections = false;
    private int port;
    private String startupTimestamp, startupTimestampHeader;

    public static final String PROTOCOL = "HTTP/1.0";
    public static final String DEFAULT_TEXT_MIME_TYPE =
        "text/plain; charset=iso-8859-1";
    public static final String DEFAULT_BINARY_MIME_TYPE =
        "application/octet-stream";
    public static final String SERVER_PARSED_MIME_TYPE =
        "text/x-server-parsed-html";
    public static final String CGI_MIME_TYPE = "application/x-httpd-cgi";
    public static final String TIMESTAMP_HEADER = "Dash-Startup-Timestamp";

    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private static InetAddress LOCAL_HOST_ADDR, LOOPBACK_ADDR;
    private static final Properties mimeTypes = new Properties();
    private static final Hashtable DEFAULT_ENV = new Hashtable();
    private static final String CRLF = "\r\n";

    static {
        try {
            DEFAULT_ENV.put("SERVER_SOFTWARE", "PSPDASH");
            DEFAULT_ENV.put("SERVER_NAME", "localhost");
            DEFAULT_ENV.put("GATEWAY_INTERFACE", "CGI/1.1");
            DEFAULT_ENV.put("SERVER_ADDR", "127.0.0.1");
            DEFAULT_ENV.put("PATH_INFO", "");
            DEFAULT_ENV.put("PATH_TRANSLATED", "");
            DEFAULT_ENV.put("REMOTE_HOST", "localhost");
            DEFAULT_ENV.put("REMOTE_ADDR", "127.0.0.1");

            dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
            mimeTypes.load(TinyWebServer.class
                           .getResourceAsStream("mime_types"));
        } catch (Exception e) { e.printStackTrace(); }
        try {
            LOCAL_HOST_ADDR = InetAddress.getLocalHost();
            LOOPBACK_ADDR   = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException uhe) {}
    }

    private class CGILoader extends ClassLoader {
        public Class loadFromConnection(URLConnection conn)
            throws IOException, ClassFormatError
        {
            // Get the class name for the CGI script.  This is equal
            // to the name of the file, sans the ".class" extension.
            String className = conn.getURL().getFile();
            int beg = className.lastIndexOf('/');
            int end = className.indexOf('.', beg);
            className = className.substring(beg + 1, end);

            // If we have already loaded this class, fetch and return it
            Class result = findLoadedClass(className);
            if (result != null)
                return result;

            // Read the class definition from the connection
            byte [] defn = slurpContents(conn.getInputStream(), true);

            synchronized (this) {
                // check to see if someone else defined the class since
                // we last checked.
                result = findLoadedClass(className);
                if (result != null) return result;

                // Create a class from the definition read.
                result = defineClass(className, defn, 0, defn.length);
                resolveClass(result);
            }
            return result;
        }
    }

    private class TinyWebThread extends Thread {

        Socket clientSocket = null;
        InputStream inputStream = null;
        BufferedReader in = null;
        OutputStream outputStream = null;
        Writer out = null;
        boolean isRunning = false;
        IOException ioexception = null;
        boolean errorEncountered = false, headerRead = false;

        String uri, method, protocol, id, path, query;

        private class TinyWebThreadException extends Exception {};

        public TinyWebThread(Socket clientSocket) {
            try {
                this.clientSocket = clientSocket;
                this.inputStream = clientSocket.getInputStream();
                this.in = new BufferedReader
                    (new InputStreamReader(inputStream));
                this.outputStream = clientSocket.getOutputStream();
                this.out = new BufferedWriter
                    (new OutputStreamWriter(outputStream));
            } catch (IOException ioe) {
                this.inputStream = null;
            }
        }

        public TinyWebThread(String uri) {
            this.clientSocket = null;
            String request = "GET " + uri + " HTTP/1.0\r\n\r\n";
            this.inputStream = new ByteArrayInputStream(request.getBytes());
            this.in = new BufferedReader(new InputStreamReader(inputStream));
            this.outputStream = new ByteArrayOutputStream(1024);
            this.out = new BufferedWriter
                (new OutputStreamWriter(outputStream));
        }

        public byte[] getOutput() throws IOException {
            if (outputStream instanceof ByteArrayOutputStream) {
                run();
                if (ioexception != null) throw ioexception;
                if (errorEncountered) throw new IOException();
                return ((ByteArrayOutputStream) outputStream).toByteArray();

            } else
                return null;
        }

        public synchronized void close() {
            if (isRunning)
                this.interrupt();
            serverThreads.remove(this);

            try {
                if (out != null) { out.flush(); out.close(); }
                if (in  != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ioe) {}

            out = null;
            in = null;
            clientSocket = null;
        }

        public void run() {

            if (inputStream != null) {
                isRunning = true;
                try {
                    handleRequest();
                } catch (TinyWebThreadException twte) {
                    errorEncountered = true;
                }
                isRunning = false;
            }

            close();
        }

        private void handleRequest() throws TinyWebThreadException {
            try {
                // read and process the header line
                String line = readLine(inputStream);
                StringTokenizer tok = new StringTokenizer(line, " ");
                method   = tok.nextToken();
                uri      = tok.nextToken();
                protocol = tok.nextToken();

                // only accept localhost requests.
                if (! allowRemoteConnections &&
                    ! checkIP())
                    sendError(403, "Forbidden", "Not accepting " +
                               "requests from remote IP addresses ." );

                // break the uri into hierarchy path, file path, and
                // query string; place the results in the appropriate
                // object-global variables.
                parseURI(uri);

                // open the requested file
                URLConnection conn = resolveURL(path);

                // decide what to do with the file based on its mime-type.
                String initial_mime_type =
                    getMimeTypeFromName(conn.getURL().getFile());
                if (SERVER_PARSED_MIME_TYPE.equals(initial_mime_type))
                    servePreprocessedFile(conn);
                else if (CGI_MIME_TYPE.equals(initial_mime_type))
                    serveCGI(conn);
                else
                    servePlain(conn, initial_mime_type);

            } catch (NoSuchElementException nsee) {
                sendError( 400, "Bad Request", "No request found." );
            } catch (IOException ioe) {
                sendError( 500, "Internal Error", "IO Exception." );
            }
        }

        /** Break the URI into hierarchy path, file path, and query string.
         *
         * The results are placed into the object-global variables
         * "id", "path", and "query".
         *
         * URIs of the following forms are recognized (all may have
         * query strings appended): <PRE>
         *     /#####/regular/path
         *     /regular/path
         *     //regular/path
         *     /hierarchy/path//regular/path
         * </PRE> */
        private void parseURI(String uri) throws TinyWebThreadException {

            // ensure uri starts with a slash.
            if (! uri.startsWith("/"))
                sendError( 400, "Bad Request", "Bad filename." );

            int pos = uri.indexOf("//");
            if (pos >= 0) {
                id = uri.substring(0, pos);
                path = uri.substring(pos+2);
            } else try {
                pos = uri.indexOf('/', 1);
                id = uri.substring(1, pos);
                Integer.parseInt(id);
                path = uri.substring(pos + 1);
            } catch (Exception e) {
                /* This block will be reached if the uri did not contain a
                 * second '/' character, or if the text between the initial
                 * and the second slash was not a number.  In these cases,
                 * we treat the uri as a simple file path, with no hierarchy
                 * path information.
                 */
                id = "";
                path = uri.substring(1);
            }

            // extract the query string from the end.
            pos = path.indexOf('?');
            if (pos != -1) {
                query = path.substring(pos + 1);
                path = path.substring(0, pos);
            }
        }

        /** Resolve an absolute URL */
        private URLConnection resolveURL(String url)
            throws TinyWebThreadException
        {
            URL u;
            URLConnection result;
            for (int i = 0;  i < roots.length;  i++) try {
                u = new URL(roots[i], url);
                // System.out.println("trying url: " + u);
                result = u.openConnection();
                // System.out.println("connection opened.");
                result.connect();
                // System.out.println("connection connected.");
                // System.out.println("Using URL: " + u);
                return result;
            } catch (IOException ioe) { }

            sendError(404, "Not Found", "File '" + url + "' not found.");
            return null;        // this line will never be reached.
        }

        /** Resolve a relative URL */
        private URLConnection resolveURL(String url, URL base)
            throws TinyWebThreadException
        {
            if (url.charAt(0) == '/')
                return resolveURL(url.substring(1));

            try {
                URLConnection result = (new URL(base, url)).openConnection();
                result.connect();
                return result;
            } catch (IOException a) { }

            sendError(404, "Not Found", "File '" + url + "' not found.");
            return null;        // this line will never be reached.
        }

        /** Handle a cgi-like http request. */
        private void serveCGI(URLConnection conn)
            throws IOException, TinyWebThreadException
        {
            // get an instantiation of the cgi script object
            TinyCGI script = getScript(conn);
            if (script == null)
                sendError (500, "Internal Error", "Couldn't load script." );

            // Create the environment for the cgi script.
            HashMap env = new HashMap(DEFAULT_ENV);
            env.put("SERVER_PROTOCOL", protocol);
            env.put("REQUEST_METHOD", method);
            env.put("PATH_INFO", id);
            if (id != null && id.startsWith("/")) {
                env.put("PATH_TRANSLATED", URLDecoder.decode(id));
                env.put("SCRIPT_PATH", id + "//" + path);
            } else {
                env.put("PATH_TRANSLATED", data.getPath(id));
                env.put("SCRIPT_PATH", "/" + id + "/" + path);
            }
            env.put("SCRIPT_NAME", "/" + path);
            env.put("REQUEST_URI", uri);
            env.put("QUERY_STRING", query);
            if (clientSocket != null) {
                env.put("REMOTE_PORT",
                        Integer.toString(clientSocket.getPort()));
                InetAddress addr = clientSocket.getInetAddress();
                env.put("REMOTE_HOST", addr.getHostName());
                env.put("REMOTE_ADDR", addr.getHostAddress());
                addr = clientSocket.getLocalAddress();
                env.put("SERVER_NAME", addr.getHostName());
                env.put("SERVER_ADDR", addr.getHostAddress());
            }
            env.put(TinyCGI.TINY_WEB_SERVER, TinyWebServer.this);

            // Parse the headers on the original http request and add to
            // the cgi script environment.
            String line, header;
            StringBuffer text = new StringBuffer();
            int pos;
            while (null != (line = readLine(inputStream))) {
                if (line.length() == 0) break;
                header = parseHeader(line,text).toUpperCase().replace('-','_');

                if (header.equals("CONTENT_TYPE") ||
                    header.equals("CONTENT_LENGTH"))
                    env.put(header, text.toString());
                else
                    env.put("HTTP_" + header, text.toString());
            }
            headerRead = true;

            // Run the cgi script, and capture the results.
            ByteArrayOutputStream cgiOut = new ByteArrayOutputStream();
            try {
                script.service(inputStream, cgiOut, env);
            } catch (Exception cgie) {
                if (cgie instanceof IOException)
                    this.ioexception = (IOException) cgie;
                if (clientSocket == null) {
                    errorEncountered = true;
                    return;
                } else {
                    StringWriter w = new StringWriter();
                    cgie.printStackTrace(new PrintWriter(w));
                    sendError(500, "CGI Error", "Error running script: " +
                              "<PRE>" + w.toString() + "</PRE>");
                }
            } finally {
                doneWithScript(script);
            }
            byte [] results = cgiOut.toByteArray();

            // Parse the headers generated by the cgi program.
            String contentType = null, statusString = "OK";
            StringBuffer otherHeaders = new StringBuffer();
            int status = 200;
            pos = 0;
            while (true) {
                line = readLine(results, pos);
                pos += line.length();

                // if the header line begins with a line termination char,
                if (line.charAt(0) == '\r' || line.charAt(0) == '\n')
                    break; // then we've encountered the end of the headers.

                header = parseHeader(line, text);

                if (header.toUpperCase().equals("STATUS")) {
                    // header value is of the form nnn xxxxxxx (a 3-digit
                    // status code, followed by an error string.
                    statusString = text.toString();
                    status = Integer.parseInt(statusString.substring(0, 3));
                    statusString = statusString.substring(4);
                }
                else if (header.toUpperCase().equals("CONTENT-TYPE"))
                    contentType = text.toString();
                else {
                    if (header.toUpperCase().equals("LOCATION"))
                        status = 302;
                    otherHeaders.append(header).append(": ")
                        .append(text.toString()).append(CRLF);
                }
            }

            // Success!! Send results back to the client.
            sendHeaders(status, statusString, contentType, results.length-pos,
                        -1, otherHeaders.toString());
            outputStream.write(results, pos, results.length-pos);
            outputStream.flush();
        }

        /** Get a TinyCGI script for a given uri path.
         * @param conn the URLConnection to the ".class" file for the script.
         *   TinyCGI scripts must be java classes in the root package (like
         *   the servlets API).
         * @return an instantiated TinyCGI script, or null on error.
         */
        private TinyCGI getScript(URLConnection conn) {
            TinyCGI result = (TinyCGI) cgiCache.remove(path);
            if (result != null)
                return result;

            try {
                Class c = cgiLoader.loadFromConnection(conn);
                result = (TinyCGI) c.newInstance();
                return result;
            } catch (Throwable t) {}
            return null;
        }
        private void doneWithScript(Object script) {
            cgiCache.put(path, script);
        }


        /** Parse an HTTP header (of the form "Header: value").
         *
         *  @param line The HTTP header line.
         *  @param value The value of the header found will be placed in
         *               this StringBuffer.
         *  @return The name of the header found.
         */
        private String parseHeader(String line, StringBuffer value) {
            int len = line.length();
            int pos = 0;
            while (pos < len  &&  ": \t".indexOf(line.charAt(pos)) == -1)
                pos++;
            String result = line.substring(0, pos);
            while (pos < len  &&  ": \t".indexOf(line.charAt(pos)) != -1)
                pos++;
            value.setLength(0);
            int end = line.indexOf('\r', pos);
            if (end == -1) end = line.indexOf('\n', pos);
            if (end == -1) end = line.length();
            value.append(line.substring(pos, end));
            return result;
        }


        /** Serve a plain HTTP request */
        private void servePlain(URLConnection conn, String mime_type)
            throws TinyWebThreadException, IOException
        {
            discardHeader();

            byte[] buffer = new byte[4096];
            InputStream content = conn.getInputStream();
            int numBytes = content.read(buffer);
            if (numBytes == -1)
                sendError( 500, "Internal Error", "Couldn't read file." );

            if (mime_type == null)
                mime_type = getDefaultMimeType(buffer, numBytes);

            sendHeaders(200, "OK", mime_type, conn.getContentLength(),
                        conn.getLastModified(), null);
            out.flush();

            do {
                outputStream.write(buffer, 0, numBytes);
            } while (-1 != (numBytes = content.read(buffer)));
            outputStream.flush();
            content.close();
        }


        /** Serve up a server-parsed html file. */
        private void servePreprocessedFile(URLConnection conn)
            throws TinyWebThreadException, IOException
        {
            discardHeader();
            String content = preprocessTextFile(conn).toString();
            sendHeaders(200, "OK", "text/html", content.length(), -1, null);
            out.write(content);
        }

        private String preprocessTextFile(URLConnection conn)
            throws TinyWebThreadException, IOException
        {
            // Slurp the entire file into a StringBuffer.
            String content = new String(slurpContents(conn.getInputStream(),
                                                      true));

            // Look for and process include directives.
            StringBuffer result = new StringBuffer();
            String include, includedContent;
            int beg, end;
            while ((beg = content.indexOf(INCLUDE_START)) != -1) {
                result.append(content.substring(0, beg));
                beg += INCLUDE_START.length();
                end = content.indexOf(INCLUDE_END, beg);
                include = content.substring(beg, end);
                content = content.substring(end + INCLUDE_END.length());

                include = parseIncludeDirective(include);
                includedContent = new String(getRequest(uri, include, true));
                result.append(includedContent);
            }
            result.append(content);

            return result.toString();
        }

        private static final String INCLUDE_START = "<!--#include";
        private static final String INCLUDE_END   = "-->";

        /** Currently not very robust. */
        private String parseIncludeDirective(String include) {
            if (!include.startsWith(" file=\"") || !include.endsWith("\" "))
                return null;
            else
                return include.substring(7, include.length() - 2);
        }

        /** read and discard the rest of the request header from inputStream */
        private void discardHeader() throws IOException {
            if (headerRead) return;

            String line;
            while (null != (line = readLine(inputStream)))
                if (line.length() == 0)
                    break;

            headerRead = true;
        }


        /** ensure that requests are originating from the local machine. */
        private boolean checkIP() {
            if (clientSocket == null)
                return true;
            InetAddress remoteIP = clientSocket.getInetAddress();
            return (remoteIP.equals(LOOPBACK_ADDR) ||
                    remoteIP.equals(LOCAL_HOST_ADDR));
        }


        /** Send an HTTP error page.
         *
         * @throws TinyWebThreadException automatically and unequivocally
         *    after printing the error page.  (This greatly simplifies
         *    TinyWebThread control logic.  Anytime an exception or
         *    other error is found, just call this method;  an error page
         *    will be generated, and then an exception will be thrown.
         *    TinyWebThreadExceptions are caught in only one place, at
         *    the top level run() method.)
         */
        private void sendError(int status, String title, String text )
            throws TinyWebThreadException
        {
            try {
                errorEncountered = true;
                discardHeader();
                sendHeaders( status, title, "text/html", -1, -1, null);
                out.write("<HTML><HEAD><TITLE>" + status + " " + title +
                          "</TITLE></HEAD>\n<BODY BGCOLOR=\"#cc9999\"><H4>" +
                          status + " " + title + "</H4>\n" +
                          text + "\n" + "</BODY></HTML>\n");
            } catch (IOException ioe) {
            }
            throw new TinyWebThreadException();
        }

        private boolean headersSent = false;
        private void sendHeaders(int status, String title, String mimeType,
                                  long length, long mod, String otherHeaders )
            throws IOException
        {
            if (headersSent) return;

            headersSent = true;

            Date now = new Date();

            out.write(PROTOCOL + " " + status + " " + title + CRLF);
            out.write("Server: localhost" + CRLF);
            out.write("Date: " + dateFormat.format(now) + CRLF);
            if (mimeType != null)
                out.write("Content-Type: " + mimeType + CRLF);
            // out.write("Accept-Ranges: bytes" + CRLF);
            if (mod > 0)
                out.write("Last-Modified: " +
                          dateFormat.format(new Date(mod)) + CRLF);
            if (length >= 0)
                out.write("Content-Length: " + length + CRLF);
            out.write(startupTimestampHeader + CRLF);
            if (otherHeaders != null)
                out.write(otherHeaders);
            out.write("Connection: close" + CRLF + CRLF);
            out.flush();
        }

        private String getMimeTypeFromName(String name) {
            // locate file extension and lookup associated mime type.
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                String suffix = name.substring(pos).toLowerCase();
                if (suffix.equals(".class") &&
                    name.indexOf("/IE/") == -1 &&
                    name.indexOf("/NS/") == -1)
                    // Eventually, we may want a better method of deciding
                    // between cgi scripts and other class files.  Perhaps
                    // a special ID (like 0) can flag an uninterpreted .class
                    // file?
                    return CGI_MIME_TYPE;
                else
                    return (String) mimeTypes.get(suffix);
            } else
                return null;
        }

        /** Check to see if the data is text or binary, and return the
         *  appropriate default mime type. */
        private String getDefaultMimeType(byte [] buffer, int numBytes)
        {
            while (numBytes-- > 0)
                if (Character.isISOControl((char) buffer[numBytes]))
                    return DEFAULT_BINARY_MIME_TYPE;

            return DEFAULT_TEXT_MIME_TYPE;
        }
    }

    /** Utility routine: slurp an entire file from an InputStream. */
    public static byte[] slurpContents(InputStream in, boolean close)
        throws IOException
    {
        byte [] result = null;
        synchronized (slurpBuffer) {
            slurpBuffer.reset();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
                slurpBuffer.write(buffer, 0, bytesRead);
            result = slurpBuffer.toByteArray();
        }
        if (close) try { in.close(); } catch (IOException ioe) {}
        return result;
    }
    private static ByteArrayOutputStream slurpBuffer =
        new ByteArrayOutputStream();


    /** Utility routine: readLine from an InputStream.
     *
     * This is needed because the only readLine method in the Java library
     * is in the BufferedReader class.  A BufferedReader will likely grab
     * more bytes than we necessarily want it to.
     *
     * Although this method is not performing any character encoding,
     * Hopefully we're okay because we're just parsing plaintext HTTP headers.
     */
    static String readLine(InputStream in) throws IOException {
        StringBuffer result = new StringBuffer();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n')
                break;
            else if (c == '\r')
                ; //do nothing
            else
                result.append((char) c);
        }

        return result.toString();
    }

    /** Utility routine: readLine from a byte array. The carraige return
     * and linefeed that terminate the line are returned as part of the
     * final result.
     *
     * Although this method is not performing any character encoding,
     * Hopefully we're okay because we're just parsing plaintext HTTP headers.
     */
    static String readLine(byte[] buf, int beg) throws IOException {
        int p = beg;
        // find an initial sequence of non-line-terminating charaters
        while (p < buf.length && buf[p] != '\r' && buf[p] != '\n') p++;
        // skip over up to two line termination characters.
        if (p < buf.length && buf[p] == '\r') p++;
        if (p < buf.length && buf[p] == '\n') p++;
        return new String(buf, beg, p-beg);
    }

    /** Perform an internal http request for the caller.
     *
     * @param uri the absolute uri of a resource on this server (e.g.
     *     <code>/0980/help/about.htm?foo=bar</code>)
     * @param skipHeaders if true, the generated response headers are discarded
     * @return the response generated by performing the http request.
     */
    public byte[] getRequest(String uri, boolean skipHeaders)
        throws IOException
    {
        if (internalRequestNesting > 50)
            throw new IOException("Infinite recursion - aborting.");

        synchronized(this) { internalRequestNesting++; }
        TinyWebThread t = new TinyWebThread(uri);
        byte [] result = null;
        try {
            result = t.getOutput();
        } finally {
            synchronized(this) { internalRequestNesting--; }
        }

        if (!skipHeaders)
            return result;
        else {
            int a=0, b=1, c=2, d=3;
            do {
                if (result[a] == '\r' && result[b] == '\n' &&
                    result[c] == '\r' && result[d] == '\n')
                    break;
                a++; b++; c++; d++;
            } while (d < result.length);
            byte [] contents = new byte[result.length - d - 1];
            System.arraycopy(result, d+1, contents, 0, contents.length);
            return contents;
        }
    }
    private volatile int internalRequestNesting = 0;

    /** Perform an internal http request for the caller.
     * @param context the uri of an original request within this web server
     * @param uri a uri to fetch the contents of.  If it does not begin with
     *     a slash, it will be interpreted relative to <code>context</code>.
     * @param skipHeaders if true, the generated response headers are discarded
     * @return the response generated by performing the http request.
     */
    public byte[] getRequest(String context, String uri, boolean skipHeaders)
        throws IOException
    {
        if (!uri.startsWith("/")) {
            URL contextURL = new URL("http://unimportant" + context);
            URL uriURL = new URL(contextURL, uri);
            uri = uriURL.getFile();
        }
        return getRequest(uri, skipHeaders);
    }

    /** Return the number of the port this server is listening on. */
    public int getPort()         { return port; }
    /** Return the startup timestamp for this server. */
    public String getTimestamp() { return startupTimestamp; }

    private void init(int port, URL [] roots) throws IOException
    {
        this.roots = roots;
        startupTimestamp = Long.toString((new Date()).getTime());
        startupTimestampHeader = TIMESTAMP_HEADER + ": " + startupTimestamp;

        while (serverSocket == null) try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            port += 2;
        }
        this.port = port;
        DEFAULT_ENV.put("SERVER_PORT", Integer.toString(port));
    }

    /**
     * Run a tiny web server on the given port, serving up resources
     * out of the given package within the class path.
     *
     * Serving up resources out of the classpath seems like a nice
     * idea, since it allows html pages, etc to be JAR-ed up and
     * invisible to the user.
     */
    public TinyWebServer(int port, String path) throws IOException
    {
        if (path == null || path.length() == 0)
            throw new IOException("Path must be specified");

        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/"))  path = path + "/";
        Enumeration e = getClass().getClassLoader().getResources(path);
        Vector v = new Vector();
        while (e.hasMoreElements())
            v.addElement(e.nextElement());
        int i = v.size();
        URL [] roots = new URL[i];
        while (i-- > 0)
            roots[i] = (URL) v.elementAt(i);

        init(port, roots);
    }

    /**
     * Run a tiny web server on the given port, serving up files out
     * of the given directory.
     */
    public TinyWebServer(String directoryToServe, int port)
        throws IOException
    {
        File rootDir = new File(directoryToServe);
        if (!rootDir.isDirectory())
            throw new IOException("Not a directory: " + directoryToServe);

        URL [] roots = new URL[1];
        roots[0] = rootDir.toURL();

        init(port, roots);
    }

    /**
     * Run a tiny web server on the given port, serving up resources
     * out of the given list of template search URLs.
     */
    public TinyWebServer(int port, URL [] roots) throws IOException {
        init(port, roots);
    }


    public void setProps(PSPProperties props) {
        if (props == null)
            DEFAULT_ENV.remove(TinyCGI.PSP_PROPERTIES);
        else
            DEFAULT_ENV.put(TinyCGI.PSP_PROPERTIES, props);
    }
    public void setData(DataRepository data) {
        this.data = data;
        if (data == null)
            DEFAULT_ENV.remove(TinyCGI.DATA_REPOSITORY);
        else
            DEFAULT_ENV.put(TinyCGI.DATA_REPOSITORY, data);
    }
    public void allowRemoteConnections(String setting) {
        this.allowRemoteConnections = "true".equalsIgnoreCase(setting);

        /* in the future, if better remote host filtering is desired,
         * this would be the place to put it.  For example instead of
         * "true", meaning "allow connections from any host", a user
         * might be able to supply a comma-separated list of hostnames.
         * Just add logic here to parse the setting, and logic in the
         * checkIP() method to act in accordance with the settings. */
    }


    private volatile boolean isRunning;


    /** handle http requests. */
    public void run() {
        Socket clientSocket = null;
        TinyWebThread serverThread = null;

        if (serverSocket == null) return;

        isRunning = true;

        while (isRunning) try {
            clientSocket = serverSocket.accept();

            serverThread = new TinyWebThread(clientSocket);
            serverThreads.addElement(serverThread);
            serverThread.start();

        } catch (IOException e) { }

        while (serverThreads.size() > 0) {
            serverThread = (TinyWebThread) serverThreads.remove(0);
            serverThread.close();
        }

        close();
    }


    /** Stop the web server. */
    public void quit() {
        isRunning = false;
        this.interrupt();
        close();
    }

    public synchronized void close() {
        if (serverSocket != null) try {
            serverSocket.close();
        } catch (IOException e2) {}
        serverSocket = null;
    }

    /** Run a web server on port 8000.  the first arg must name the
     *  directory to serve */
    public static void main(String [] args) {
        try {
            InetAddress host = InetAddress.getLocalHost();
            System.out.println("TinyWeb starting " +
                               dateFormat.format(new Date()) + " on " +
                               host);
            (new TinyWebServer(args[0], 8000)).run();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

}
