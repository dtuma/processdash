// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.jarsurf;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/** This class captures the basic logic behind the HTTP protocol, and
 *  implements a server which accepts HTTP connections on a given port.
 *
 *  This server responds to every incoming HTTP request by returning
 *  an empty HTML document.  This class can easily be extended to
 *  generate a useful HTTP server, by following these steps:<UL>
 *
 *  <LI> Create a new class which <code>extends</code> this class.
 *
 *  <LI> In your new class, create an inner class which
 *       <code>extends TinyWebThread</code>, and overrides the
 *       <code>serviceRequest</code> to generate a useful reply to the
 *       HTTP request.
 *
 *  <LI> Override the <code>createThread</code> method so it returns
 *       an instance of your custom inner class, rather than the
 *       default <code>TinyWebThread</code>.
 *
 *  </UL>
 */
class TinyWebServer extends Thread {

    /** The socket where we are listening for incoming HTTP connections */
    ServerSocket serverSocket = null;

    /** The port number where we are listening for incoming HTTP connections */
    int port = -1;

    /** The list of threads which are currently running */
    Vector serverThreads = new Vector();

    /** is the web server still running? */
    protected volatile boolean isRunning;

    /** How long do we wait (with no new connections) before shutting down?
     * 0 == forever */
    protected int shutdownWaitTime = 0;

    /** when did we receive our last HTTP request? */
    protected long lastRequestTime;


    public static final String PROTOCOL = "HTTP/1.0";
    public static final String DEFAULT_TEXT_MIME_TYPE =
        "text/plain; charset=iso-8859-1";
    public static final String DEFAULT_HTML_MIME_TYPE =
        "text/html; charset=iso-8859-1";
    public static final String DEFAULT_BINARY_MIME_TYPE =
        "application/octet-stream";
    protected static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    protected static final String CRLF = "\r\n";
    protected static final int MILLIS_PER_MINUTE = 60 * 1000;


    static {
        try {
            dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        } catch (Exception e) { e.printStackTrace(); }
    }


    /** Worker class to accept a single HTTP connection and respond to it.
     *
     * Subclasses of TinyWebServer will probably want to extend this
     * class and override the serviceRequest() method to do something
     * useful.
     */
    protected class TinyWebThread extends Thread {

        /** The socket in use by this HTTP connection */
        Socket clientSocket = null;

        /** A convienience object which can be used to read from the
         * input stream of the client socket */
        BufferedReader in = null;

        OutputStream outStream = null;

        /** A convienience object which can be used to write to the
         * output stream of the client socket */
        Writer out = null;

        /** true if this thread is still running. */
        boolean isRunning = false;

        /** the method (e.g. "GET" or "POST") used to initiate this HTTP
         * request */
        String method;

        /** the uri of the item requested by the client */
        String path;

        /** the protocol (e.g. "HTTP/1.0") used by the client to
         * initiate this HTTP request */
        String protocol;

        protected class TinyWebThreadException extends Exception {};

        public TinyWebThread(Socket clientSocket) {
            super("TinyWebThread");
            try {
                this.clientSocket = clientSocket;
                this.in = new BufferedReader
                    (new InputStreamReader(clientSocket.getInputStream()));
                this.outStream = clientSocket.getOutputStream();
                this.out = new BufferedWriter(new OutputStreamWriter(outStream));
            } catch (IOException ioe) {
                this.clientSocket = null;
            }
        }

        public synchronized void close() {
            if (isRunning)
                this.interrupt();
            serverThreads.removeElement(this);

            try {
                if (out != null) out.close();
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

        protected void handleRequest() throws TinyWebThreadException {
            try {
                // read and process the header line
                String line = in.readLine();
                StringTokenizer tok = new StringTokenizer(line, " ");
                method   = tok.nextToken();
                path     = tok.nextToken();
                protocol = tok.nextToken();

                // read the rest of the request header and discard it.
                while (null != (line = in.readLine()))
                    if (line.length() == 0)
                        break;

                // only handle GET requests.
                if (! "GET".equalsIgnoreCase(method))
                    sendError(501, "Not Implemented",
                               "That method is not implemented.");

                serviceRequest();

            } catch (TinyWebThreadException twte) {
            } catch (NoSuchElementException nsee) {
                sendError( 400, "Bad Request", "No request found." );
            } catch (Exception except) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter op = new PrintWriter(out);
                except.printStackTrace(op);
                op.flush();
                sendError( 500, "Internal Error", except.toString() +
                           "<!--\n" + out.toString() + "\n-->");
            }
        }

        void sendOK() throws IOException {
            sendHeaders(200, "OK", DEFAULT_HTML_MIME_TYPE, -1, -1);
        }


        public void serviceRequest() throws TinyWebThreadException, IOException
        {
            sendOK();
        }

        protected void sendError(int status, String title, String text )
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

        protected boolean headersSent = false;
        protected void sendHeaders(int status, String title, String mimeType,
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
            if (mod >= 0)
                out.write("Last-Modified: " +
                          dateFormat.format(new Date(mod)) + CRLF);
            if (length >= 0)
                out.write("Content-Length: " + length + CRLF);
            out.write("Connection: close" + CRLF + CRLF );
        }

        protected void copyStream(InputStream content) throws IOException {
            // we aren't going to use the PrintWriter for output within this
            // method.  Therefore, we flush it to ensure that no buffered
            // output ends up getting interleaved with the data sent directly
            // to the OutputStream.
            out.flush();

            OutputStream dest = clientSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int numBytes;
            while ((numBytes = content.read(buffer)) != -1)
                dest.write(buffer, 0, numBytes);
            dest.flush();
            content.close();
        }
    }


    /** Utility routine: encode HTML entities in <code>str</code>. */
    public static String encodeEntities(String str) {
        str = findAndReplace(str, "&",  "&amp;");
        str = findAndReplace(str, "<",  "&lt;");
        str = findAndReplace(str, ">",  "&gt;");
        str = findAndReplace(str, "\"", "&quot;");
        return str;
    }


    /** perform a case-sensitive find and replace operation. */
    public static String findAndReplace(String text, String find,
                                        String replace) {
        // handle degenerate case: if no replacements need to be made,
        // return the original text unchanged.
        int replaceStart = text.indexOf(find);
        if (replaceStart == -1) return text;

        int findLength = find.length();
        StringBuffer toReturn = new StringBuffer();

        while (replaceStart != -1) {
            toReturn.append(text.substring(0, replaceStart));
            toReturn.append(replace);
            text = text.substring(replaceStart+findLength);
            replaceStart = text.indexOf(find);
        }

        toReturn.append(text);
        return toReturn.toString();
    }

    /** Parse url-encoded query parameters */
    public static Map parseParams(String query) {
        StringTokenizer tok = new StringTokenizer(query, "?&");
        Map result = new HashMap();
        while (tok.hasMoreTokens()) {
            String param = tok.nextToken();
            int equalsPos = param.indexOf('=');
            try {
                result.put(URLDecoder.decode(param.substring(0,equalsPos), "UTF-8"),
                           URLDecoder.decode(param.substring(equalsPos+1), "UTF-8"));
            } catch (UnsupportedEncodingException e) { }
        }
        return result;
    }


    /**
     * Create a tiny web server.
     *
     * Note that after you create the web server, you are still
     * responsible for optionally setting its thread properties (like
     * setDaemon) and starting it.
     *
     * @param port the port to listen on for incoming connections.
     *      Use 0 to listen on any available port.
     * @param acceptRemote true if connections should be accepted from
     *      remote IP addresses; false to accept connections only from
     *      the local machine.  */
    public TinyWebServer(int port, boolean acceptRemote) throws IOException {
        super("TinyWebServer");
        if (acceptRemote)
            serverSocket = new ServerSocket(port);
        else
            serverSocket = new ServerSocket
                (port, 50, InetAddress.getByName("localhost"));
        serverSocket.setSoTimeout(MILLIS_PER_MINUTE);
        this.port = serverSocket.getLocalPort();
    }


    /** Configure this web server to shut down if a given time period elapses
     * with no new incoming requests.
     */
    public void setShutdownWaitTime(int minutes) {
        this.shutdownWaitTime = minutes;
    }

    public int getShutdownWaitTime() {
        return shutdownWaitTime;
    }



    /** Create a thread to respond to an incoming HTTP connection.
     *
     * This method exists so it can be overridden by subclasses.
     */
    protected TinyWebThread createThread(Socket clientSocket) {
        return new TinyWebThread(clientSocket);
    }


    /** handle http requests. */
    public void run() {
        Socket clientSocket = null;
        TinyWebThread serverThread = null;

        if (serverSocket == null) return;

        isRunning = true;

        while (isRunning) try {

            clientSocket = serverSocket.accept();

            lastRequestTime = System.currentTimeMillis();

            serverThread = createThread(clientSocket);
            serverThreads.addElement(serverThread);
            serverThread.start();

        } catch (SocketTimeoutException ste) {
            long waitTime = System.currentTimeMillis() - lastRequestTime;
            int waitMinutes = (int) (waitTime / MILLIS_PER_MINUTE);
            if (shutdownWaitTime != 0 && waitMinutes >= shutdownWaitTime) {
                isRunning = false;
                break;
            }
        } catch (IOException e) { }

        while (serverThreads.size() > 0) {
            serverThread = (TinyWebThread) serverThreads.elementAt(0);
            serverThreads.removeElementAt(0);
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


    /** Close the server socket */
    public synchronized void close() {
        if (serverSocket != null) try {
            serverSocket.close();
        } catch (IOException e2) {}
        serverSocket = null;
    }

}
