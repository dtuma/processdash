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
import java.text.*;

public class TinyWebServer extends Thread {

    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();
    URL [] roots = null;
    DataRepository data = null;
    ClassLoader classLoader = null;
    Hashtable cgiCache = new Hashtable();
    boolean allowRemoteConnections = false;

    public static final String PROTOCOL = "HTTP/1.0";
    public static final String DEFAULT_TEXT_MIME_TYPE =
        "text/plain; charset=iso-8859-1";
    public static final String DEFAULT_BINARY_MIME_TYPE =
        "application/octet-stream";
    public static final String SERVER_PARSED_MIME_TYPE =
        "text/x-server-parsed-html";
    public static final String CGI_MIME_TYPE = "application/x-httpd-cgi";

    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

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
    }

    private class TinyWebThread extends Thread {

        Socket clientSocket = null;
        InputStream inputStream = null;
        BufferedReader in = null;
        OutputStream outputStream = null;
        Writer out = null;
        boolean isRunning = false;

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
                this.clientSocket = null;
            }
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

            if (clientSocket != null) {
                isRunning = true;
                try {
                    handleRequest();
                } catch (TinyWebThreadException twte) {}
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

                // ensure uri starts with a slash.
                if (! uri.startsWith("/"))
                    sendError( 400, "Bad Request", "Bad filename." );

                // extract the ID from the beginning of the uri
                int pos = uri.indexOf('/', 1);
                if (pos == -1)
                    sendError( 400, "Bad Request", "ID required." );
                id = uri.substring(1, pos);
                path = uri.substring(pos + 1);

                // extract the query string from the end.
                pos = path.indexOf('?');
                if (pos != -1) {
                    query = path.substring(pos + 1);
                    path = path.substring(0, pos);
                }

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
            TinyCGI script = (TinyCGI) cgiCache.get(path);
            if (script == null) {
                script = getScript(path);
                if (script != null)
                    cgiCache.put(path, script);
                else {
                    // if the attempt to get the uri as a cgi script
                    // failed, serve it up as a regular binary file.
                    servePlain(conn, DEFAULT_BINARY_MIME_TYPE);
                    return;
                }
            }

            // Create the environment for the cgi script.
            HashMap env = new HashMap(DEFAULT_ENV);
            env.put("SERVER_PROTOCOL", protocol);
            env.put("REQUEST_METHOD", method);
            env.put("PATH_INFO", id);
            env.put("PATH_TRANSLATED", data.getPath(id));
            env.put("SCRIPT_NAME", "/" + path);
            env.put("REQUEST_URI", uri);
            env.put("QUERY_STRING", query);
            env.put("REMOTE_PORT", Integer.toString(clientSocket.getPort()));

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

            // Run the cgi script, and capture the results.
            ByteArrayOutputStream cgiOut = new ByteArrayOutputStream();
            try {
                synchronized(script) {
                    script.service(inputStream, cgiOut, env);
                }
            } catch (Exception cgie) {
                StringWriter w = new StringWriter();
                cgie.printStackTrace(new PrintWriter(w));
                sendError(500, "CGI Error", "Error running script: " +
                          "<PRE>" + w.toString() + "</PRE>");
            }
            String results = cgiOut.toString();

            // Parse the headers generated by the cgi program.
            String contentType = null, statusString = "OK";
            StringBuffer otherHeaders = new StringBuffer();
            int status = 200, beg = 0, end;
            while (true) {
                end = results.indexOf("\r\n", beg);
                if (end == -1) break; // should we throw an error instead?
                line = results.substring(beg, end);
                beg = end+2;
                if (line.length() == 0) break; // empty line -> end of headers.

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
            results = results.substring(beg);

            // Success!! Send results back to the client.
            sendHeaders(status, statusString, contentType, results.length(),
                        -1, otherHeaders.toString());
            outputStream.write(results.getBytes());
            outputStream.flush();
        }

        /** Get a TinyCGI script for a given uri path.
         * @param path the name of the TinyCGI class, underneath the Templates
         *    package.  For example, a path of "foo/bar.class" would be
         *    looked up as the class, "Templates.foo.bar".
         * @return an instantiated TinyCGI script, or null on error.
         */
        private TinyCGI getScript(String path) {
            try {
                String className = "Templates." + path.replace('/', '.');
                className = className.substring(0, className.length() - 6);
                Class c = classLoader.loadClass(className);
                return (TinyCGI) c.newInstance();
            } catch (Throwable t) {}
            return null;
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
            value.append(line.substring(pos));
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

        private int nestingDepth = 0;
        private StringBuffer preprocessTextFile(URLConnection conn)
            throws TinyWebThreadException
        {
            if (nestingDepth > 25)
                sendError(500, "Recursion error", "Include file recursion");

            // Slurp the entire file into a StringBuffer.
            StringBuffer result = null;
            try {
                result = slurpFile(conn.getInputStream());
            } catch (IOException ioe) {
                sendError( 500, "Internal Error", "Couldn't read file." );
            }

            // Check to see if the file we just slurped is a server parsed
            // file.  If not, just return its contents. (This check is
            // necessary to keep from parsing included .html files).
            if (! SERVER_PARSED_MIME_TYPE.equals
                (getMimeTypeFromName(conn.getURL().getFile())))
                return result;

            // Look for and process include directives.
            String include, content = result.toString();
            result = new StringBuffer();
            StringBuffer includedContent;
            int beg, end;
            while ((beg = content.indexOf(INCLUDE_START)) != -1) {
                result.append(content.substring(0, beg));
                beg += INCLUDE_START.length();
                end = content.indexOf(INCLUDE_END, beg);
                include = content.substring(beg, end);
                content = content.substring(end + INCLUDE_END.length());

                include = parseIncludeDirective(include);
                nestingDepth++;
                includedContent = preprocessTextFile
                    (resolveURL(include, conn.getURL()));
                nestingDepth--;
                result.append(includedContent.toString());
            }
            result.append(content);

            return result;
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
            String line;
            while (null != (line = readLine(inputStream)))
                if (line.length() == 0)
                    break;
        }


        /** ensure that requests are originating from the local machine. */
        private boolean checkIP() {
            byte[] remoteIP = clientSocket.getInetAddress().getAddress();
            return (remoteIP[0] == 127 &&
                    remoteIP[1] == 0   &&
                    remoteIP[2] == 0   &&
                    remoteIP[3] == 1);
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
    public static StringBuffer slurpFile(InputStream in)
        throws IOException
    {
        StringBuffer result = new StringBuffer();
        BufferedReader file = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = file.readLine()) != null)
            result.append(line).append("\n");

        return result;
    }

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
        // this.port = port;
        if (path == null || path.length() == 0)
            throw new IOException("Path must be specified");

        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/"))  path = path + "/";
        Enumeration e = getClass().getClassLoader().getResources(path);
        Vector v = new Vector();
        while (e.hasMoreElements())
            v.addElement(e.nextElement());
        int i = v.size();
        roots = new URL[i];
        while (i-- > 0)
            roots[i] = (URL) v.elementAt(i);

        makeClassLoader();

        DEFAULT_ENV.put("SERVER_PORT", Integer.toString(port));
        serverSocket = new ServerSocket(port);
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

        roots = new URL[1];
        roots[0] = rootDir.toURL();

        makeClassLoader();

        DEFAULT_ENV.put("SERVER_PORT", Integer.toString(port));
        serverSocket = new ServerSocket(port);
    }


    /** Create a class loader capable of loading cgi script classes. */
    private void makeClassLoader() {
        int i = this.roots.length;
        URL [] roots = new URL[i];
        while (i-- > 0) try {
            String url = this.roots[i].toString();
            // url will end with "Templates/"
            url = url.substring(0, url.length() - 10);
            if (url.startsWith("jar:"))
                // strip initial "jar:" and final "!/"
                url = url.substring(4, url.length() - 2);
            roots[i] = new URL(url);
        } catch (java.net.MalformedURLException mue) {
            System.err.println("Caught MalformedURLException: " + mue);
        }

        classLoader = new URLClassLoader(roots);
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
    public void allowRemoteConnections(boolean flag) {
        this.allowRemoteConnections = flag;
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
