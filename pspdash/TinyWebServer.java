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
import java.text.*;

public class TinyWebServer extends Thread {

    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();
    File rootDir = null;
    String packageName = null;

    public static final String PROTOCOL = "HTTP/1.0";
    public static final String DEFAULT_TEXT_MIME_TYPE =
        "text/plain; charset=iso-8859-1";
    public static final String DEFAULT_BINARY_MIME_TYPE =
        "application/octet-stream";
    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private static final Properties mimeTypes = new Properties();
    private static final String CRLF = "\r\n";
    private static InetAddress host = null;

    public static String getID(String URL) {
        try {
            StringTokenizer tok = new StringTokenizer(URL, "/");
            return tok.nextToken();
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }


    static {
        try {
            dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
            mimeTypes.load(TinyWebServer.class.getResourceAsStream("mime_types"));
            host = InetAddress.getLocalHost();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private class TinyWebThread extends Thread {

        Socket clientSocket = null;
        BufferedReader in = null;
        Writer out = null;
        boolean isRunning = false;

        private class TinyWebThreadException extends Exception {};

        public TinyWebThread(Socket clientSocket) {
            try {
                this.clientSocket = clientSocket;
                this.in = new BufferedReader
                    (new InputStreamReader(clientSocket.getInputStream()));
                this.out = new BufferedWriter
                    (new OutputStreamWriter(clientSocket.getOutputStream()));
            } catch (IOException ioe) {
                this.clientSocket = null;
            }
        }

        public synchronized void close() {
            if (isRunning)
                this.interrupt();
            serverThreads.remove(this);

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

        private void handleRequest() throws TinyWebThreadException {
            try {
                // read and process the header line
                String line = in.readLine();
                StringTokenizer tok = new StringTokenizer(line, " ");
                String method   = tok.nextToken();
                String path     = tok.nextToken();
                String protocol = tok.nextToken();

                // read the rest of the request header and discard it.
                while (null != (line = in.readLine()))
                    if (line.length() == 0)
                        break;

                // only accept localhost requests.
                if (! checkIP())
                    sendError(403, "Forbidden", "Not accepting " +
                               "requests from remote IP addresses ." );

                // only handle GET requests.
                if (! "GET".equalsIgnoreCase(method))
                    sendError(501, "Not Implemented",
                               "That method is not implemented.");

                // ensure path starts with a slash.
                if (! path.startsWith("/"))
                    sendError( 400, "Bad Request", "Bad filename." );

                // open the requested file
                String filename = getFilename(path);
                InputStream is = null;
                long contentLength = -1, lastModified = -1;
                if (rootDir != null) {
                    // If we are serving up a particular directory, find the
                    // given file under that directory.
                    File content = new File(rootDir, filename);

                    // reject illegal attempts to .. above the root directory.
                    if (! content.getAbsolutePath().startsWith
                        (rootDir.getAbsolutePath()))
                        sendError(400, "Bad Request", "Illegal filename." );

                    // reject requests for nonexistent files
                    if (! content.exists())
                        sendError(404, "Not Found", "File not found." );

                    // reject requests for protected files.
                    if (! content.canRead())
                        sendError( 403, "Forbidden", "File is protected." );

                    contentLength = content.length();
                    lastModified = content.lastModified();
                    is =  new FileInputStream(content);

                } else {
                    // If we are NOT serving up a particular directory,
                    // find the given file in the class path.
                    is = TinyWebServer.class.getResourceAsStream
                        (packageName + filename);

                    if (is == null)
                        sendError(404, "Not Found", "File not found." );
                }

                byte[] buffer = new byte[4096];
                int numBytes = is.read(buffer);
                if (numBytes == -1)
                    sendError( 500, "Internal Error", "Couldn't read file." );

                sendHeaders(200, "OK", getMimeType(filename, buffer, numBytes),
                            contentLength, lastModified);
                out.flush();
                OutputStream os = clientSocket.getOutputStream();

                do {
                    os.write(buffer, 0, numBytes);
                } while (-1 != (numBytes = is.read(buffer)));
                os.flush();

            } catch (NoSuchElementException nsee) {
                sendError( 400, "Bad Request", "No request found." );
            } catch (FileNotFoundException fnfe) {
                sendError( 404, "Not Found", "File not found." );
            } catch (IOException ioe) {
                sendError( 500, "Internal Error", "IO Exception.");
            }
        }

        private boolean checkIP() {
            return true; /*
            byte[] remoteIP = clientSocket.getInetAddress().getAddress();
            return (remoteIP[0] == 127 &&
                    remoteIP[1] == 0   &&
                    remoteIP[2] == 0   &&
                    remoteIP[3] == 1); */
        }
        private String getFilename(String path) throws TinyWebThreadException {
            try {
                // find and discard the initial Project ID, and
                // any trailing parameter/querystring/hash, from the URL
                StringTokenizer tok = new StringTokenizer(path, "/");
                // discard the first path component. it is a project ID.
                tok.nextToken();

                return URLDecoder.decode(tok.nextToken(";?#"));
            } catch (NoSuchElementException nsee) {
                sendError(400, "Bad Request", "Bad filename.");
            } catch (Exception e) {
                sendError(400, "Bad Request", "Malformed URL.");
            }
            return null;
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
            if (mod >= 0)
                out.write("Last-Modified: " +
                          dateFormat.format(new Date(mod)) + CRLF);
            if (length >= 0)
                out.write("Content-Length: " + length + CRLF);
            out.write("Connection: close" + CRLF + CRLF );
        }

        private String getMimeType(String name, byte[] buf, int numBytes) {
            String result = null;

            // first, try to determine mime type based on file extension
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                String suffix = name.substring(pos).toLowerCase();
                result = (String) mimeTypes.get(suffix);
            }

            // if that fails, check to see if the file is text or binary,
            // and choose an appropriate default mime type.
            if (result == null) {
                boolean isBinary = false;
                while (numBytes-- > 0)
                    if (Character.isISOControl((char) buf[numBytes])) {
                        isBinary = true;
                        break;
                    }

                result = (isBinary ? DEFAULT_BINARY_MIME_TYPE
                                   : DEFAULT_TEXT_MIME_TYPE);
            }

            return result;
        }
    }

    /**
     * Run a tiny web server on the given port, serving up resources
     * out of the given subdir of the class path.
     *
     * Serving up resources out of the classpath seems like a nice
     * idea, since it allows html pages, etc to be JAR-ed up and
     * invisible to the user.  However, it isn't as fast as serving up
     * resources that are stored in files.  It appears to be
     * perceptably slower to the user viewing pages in a browser.
     */
    public TinyWebServer(int port, String path) throws IOException
    {
        rootDir = null;
        if (path == null || path.length() == 0)
            this.packageName = "";
        //        else if (!path.endsWith("/"))
        //            this.packageName = path + "/";
        else
            this.packageName = path;
        serverSocket = new ServerSocket(port);
    }

    /**
     * Run a tiny web server on the given port, serving up files out
     * of the given directory.
     */
    public TinyWebServer(String directoryToServe, int port)
        throws IOException
    {
        rootDir = new File(directoryToServe);
        if (!rootDir.isDirectory())
            throw new IOException("Not a directory: " + directoryToServe);

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
            System.out.println("TinyWeb starting " +
                               dateFormat.format(new Date()) + " on " +
                               host);
            (new TinyWebServer(args[0], 8000)).run();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

}
