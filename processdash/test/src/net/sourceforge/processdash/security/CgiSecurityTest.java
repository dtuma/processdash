package net.sourceforge.processdash.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import net.sourceforge.processdash.net.http.DashboardURLConnection;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class CgiSecurityTest extends TinyCGIBase {

    protected void writeContents() throws IOException {
        out.println("<html><head>");
        out.println("<title>CGI Security Test</title>");
        out.println("<style> .fail { color:red } </style>");
        out.println("</head><body><h1>CGI Security Test</h1>");

        testFileAccess();
        testDashProtocol();
        testHttpProtocol();

        out.println("</BODY></HTML>");
    }

    private void testFileAccess() {
        out.println("<h2>File Access</h2><ul>");
        testFileExists();
        testFileIsDirectory();
        testListFiles();
        testReadFile();
        testWriteFile();
        out.println("</ul>");
    }

    private void testFileExists() {
        try {
            File f = new File(".");
            boolean exists = f.exists();
            printFailure("File.exists()");
        } catch (SecurityException se) {
            printSuccess("File.exists()");
        }
    }

    private void testFileIsDirectory() {
        try {
            File f = new File(".");
            boolean exists = f.isDirectory();
            printFailure("File.isDirectory()");
        } catch (SecurityException se) {
            printSuccess("File.isDirectory()");
        }
    }

    private void testListFiles() {
        try {
            File f = new File(".");
            File[] contents = f.listFiles();
            printFailure("File.listFiles()");
        } catch (SecurityException se) {
            printSuccess("File.listFiles()");
        }
    }

    private void testReadFile() {
        try {
            File f = new File("global.dat");
            FileInputStream in = new FileInputStream(f);
            printFailure("Read file");
        } catch (FileNotFoundException e) {
            printFailure("Read file");
        } catch (SecurityException se) {
            printSuccess("Read file");
        }
    }

    private void testWriteFile() {
        try {
            File f = new File("foo");
            FileOutputStream out = new FileOutputStream(f);
            printFailure("Write file");
        } catch (FileNotFoundException e) {
            printFailure("Write file");
        } catch (SecurityException se) {
            printSuccess("Write file");
        }
    }

    private void testDashProtocol() {
        out.println("<h2>Dash Protocol</h2><ul>");
        testDashURLs("processdash:", true);
        out.println("</ul>");
    }

    private void testHttpProtocol() {
        out.println("<h2>Http Protocol</h2><ul>");
        String prefix = "http://" + getTinyWebServer().getHostName(false) + ":" + getTinyWebServer().getPort();
        testDashURLs(prefix, false);
        prefix = "http://" + getTinyWebServer().getHostName(true) + ":" + getTinyWebServer().getPort();
        testDashURLs(prefix, false);
        testURL("http://www.google.com/", false);
        out.println("</ul>");
    }

    private void testDashURLs(String prefix, boolean canAnySucceed) {
        testURL(prefix + "/Project//control/startTiming.class", false);
        testURL(prefix + "/help/PSPDash.hs", canAnySucceed);
        testURL(prefix + "/psp2/sumfull.shtm", canAnySucceed);
        testURL(prefix + "/To+Date/PSP/All//reports/analysis/toc.htm", canAnySucceed);
        testURL(prefix + "/To+Date/PSP/All//dash/summary.shtm", canAnySucceed);
        testURL(prefix + "/To+Date/PSP/All//dash/summary.shtm?rollup", canAnySucceed);
    }


    private void testURL(String url, boolean expectSuccess) {
        String msg = null;
        Throwable th = null;
        boolean success = false;
        boolean error = false;
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.connect();

            String status = null;
            if (conn instanceof DashboardURLConnection)
                status = ((DashboardURLConnection)conn).getHeaderField(-1);
            else if (conn instanceof HttpURLConnection)
                status = "" + ((HttpURLConnection) conn).getResponseCode();

            if (status == null) {
                msg = "didn't find status";
                error = true;
            } else if (status.startsWith("5"))
                success = false;
            else if (status.startsWith("200"))
                success = true;
            else {
                msg = "status "+status;
                error = true;
            }

        } catch (SecurityException se) {
            th = se;
            success = false;
        } catch (IOException e) {
            Throwable t = e;
            while (true) {
                if (t instanceof SecurityException) {
                    th = t;
                    success = false;
                    break;
                }
                t = t.getCause();
                if (t == null) {
                    th = e;
                    error = true;
                    break;
                }
            }
        }
        if (error || (success != expectSuccess))
            out.print("<li class=fail>");
        else
            out.print("<li>");

        out.print(url);
        if (msg != null) {
            out.print(" - ");
            out.print(msg);
        }

        if (error)
            out.print(" - unable to perform test; FAIL");
        else if (expectSuccess) {
            if (success)
                out.print(" - retrieved successfully as expected");
            else
                out.print(" - uable to retrieve as expected; FAIL");
        } else {
            if (!success)
                out.print(" - is secure");
            else
                out.print(" - FAIL/INSECURE");
        }
        if ((error || (success != expectSuccess)) && (th != null)) {
            out.print("<PRE>");
            th.printStackTrace(out);
            out.print("</PRE>");
        }
        out.println("</li>");
    }

    private void printFailure(String message) {
        out.print("<li class=fail>");
        out.print(message);
        out.println(" - FAIL/INSECURE</li>");
    }

    private void printSuccess(String message) {
        out.print("<li>");
        out.print(message);
        out.println(" - is secure</li>");
    }
}
