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

import java.net.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.text.*;

public class TinyWebServer extends Thread {

    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();
    URL [] roots = null;

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
    private static final Properties DEFAULT_ENV = new Properties();
    private static final String CRLF = "\r\n";

    static {
        try {
            DEFAULT_ENV.put("SERVER_SOFTWARE", "PSPDASH");
            DEFAULT_ENV.put("SERVER_NAME", "localhost");
            DEFAULT_ENV.put("GATEWAY_INTERFACE", "CGI/1.1");
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

        String method, protocol, id, path, query;

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
                String line = in.readLine();
                StringTokenizer tok = new StringTokenizer(line, " ");
                method   = tok.nextToken();
                path     = tok.nextToken();
                protocol = tok.nextToken();

                // only accept localhost requests.
                if (! checkIP())
                    sendError(403, "Forbidden", "Not accepting " +
                               "requests from remote IP addresses ." );

                // ensure path starts with a slash.
                if (! path.startsWith("/"))
                    sendError( 400, "Bad Request", "Bad filename." );

                // extract the ID from the beginning of the path
                int pos = path.indexOf('/', 1);
                if (pos == -1)
                    sendError( 400, "Bad Request", "ID required." );
                id = path.substring(1, pos);
                path = path.substring(pos + 1);

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

        private void serveCGI(URLConnection conn)
            throws TinyWebThreadException { }

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
                        conn.getLastModified());
            out.flush();

            do {
                outputStream.write(buffer, 0, numBytes);
            } while (-1 != (numBytes = content.read(buffer)));
            outputStream.flush();
        }

        private void servePreprocessedFile(URLConnection conn)
            throws TinyWebThreadException, IOException
        {
            discardHeader();
            String content = preprocessTextFile(conn).toString();
            sendHeaders(200, "OK", "text/html", content.length(), -1);
            out.write(content);
        }

        private int nestingDepth = 0;
        private StringBuffer preprocessTextFile(URLConnection conn)
            throws TinyWebThreadException
        {
            if (nestingDepth > 25)
                sendError(500, "Recursion error", "Include file recursion");

            StringBuffer result = new StringBuffer();
            try {
                BufferedReader in = new BufferedReader
                    (new InputStreamReader(conn.getInputStream()));

                String line;
                while ((line = in.readLine()) != null)
                    result.append(line).append("\n");
            } catch (IOException ioe) {
                sendError( 500, "Internal Error", "Couldn't read file." );
            }

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

        private void discardHeader() throws IOException {
            // read the rest of the request header and discard it.
            String line;
            while (null != (line = in.readLine()))
                if (line.length() == 0)
                    break;
        }


        private void serveCGIRequest(String method, String path,
                                     String protocol, InputStream is)
            throws TinyWebThreadException
        {/*
            try {
                ObjectInputStream ois = new ObjectInputStream(is);
                CGIProgram cgi = (CGIProgram) in.readObject();

                Properties env = new Properties(DEFAULT_ENV);
                env.put("SERVER_PROTOCOL", protocol);
                env.put("REQUEST_METHOD", method);
                env.put("SCRIPT_NAME", getFilename(path));

                String line, lLine;
                while (null != (line = in.readLine())) {
                    if (line.length() == 0)
                        break;
                    lLine = line.toLowerCase();
                    //else if (lLine.startsWith(""));
                }

            } catch (Exception e) {
                sendError(500, "Internal Error", "Caught exception: " + e);
                } */
        }

        private boolean checkIP() {
            byte[] remoteIP = clientSocket.getInetAddress().getAddress();
            return (remoteIP[0] == 127 &&
                    remoteIP[1] == 0   &&
                    remoteIP[2] == 0   &&
                    remoteIP[3] == 1);
        }


        private void sendError(int status, String title, String text )
            throws TinyWebThreadException
        {
            try {
                sendHeaders( status, title, "text/html", -1, -1);
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
                                  long length, long mod )
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
            out.write("Connection: close" + CRLF + CRLF );
        }

        private String getMimeTypeFromName(String name) {
            // locate file extension and lookup associated mime type.
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                String suffix = name.substring(pos).toLowerCase();
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

        DEFAULT_ENV.put("SERVER_PORT", "" + port);
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

        DEFAULT_ENV.put("SERVER_PORT", "" + port);
        serverSocket = new ServerSocket(port);
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
