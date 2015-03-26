// Copyright (C) 2003-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.http;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;


/**
 * This class acts as an OutputStream for a TinyCGI script. As the script writes
 * data to this stream in CGI format, this object interprets the headers and
 * content and repeats the information to a standard HttpServletResponse object.
 */
public class CGIOutputStream extends OutputStream {

    public static final int NORMAL = 0;
    public static final int STREAMING = 1;
    public static final int LARGE = 2;

    private HttpServletResponse resp;
    private OutputStream out;
    private boolean isStreaming;
    private boolean isLarge;
    private boolean inHeader;

    private ByteArrayOutputStream headerBuffer;
    private byte[] last4HeaderBytes = new byte[4];

    private OutputStream contentBuffer;
    private File largeOutputFile;

    /**
     * Create a new CGI output stream.
     * 
     * @param resp
     *            an HttpServletResponse object for sending results back to the
     *            client
     * @param mode
     *            one of {@link #NORMAL}, {@link #STREAMING}, or {@link #LARGE}.
     */
    public CGIOutputStream(HttpServletResponse resp, int mode)
            throws IOException {
        this.resp = resp;
        this.out = resp.getOutputStream();
        this.isStreaming = (mode == STREAMING);
        this.isLarge = (mode == LARGE);
        this.inHeader = true;
        this.headerBuffer = new ByteArrayOutputStream();
    }

    public void write(int b) throws IOException {
        if (inHeader)
            writeHeaderByte(b);
        else
            writeContentByte(b);
    }

    public void cleanup() {
        FileUtils.safelyClose(contentBuffer);

        if (largeOutputFile != null)
            largeOutputFile.delete();
    }

    public void finish() throws IOException {
        FileUtils.safelyClose(contentBuffer);

        if (!isStreaming) {
            sendHeader();
            sendContent();
        }

        out.flush();
    }

    private void sendContent() throws IOException {
        if (contentBuffer instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream buf = (ByteArrayOutputStream) contentBuffer;
            if (buf.size() > 0)
                buf.writeTo(out);
            contentBuffer = null;

        } else if (largeOutputFile != null){
            if (largeOutputFile.length() > 0)
                FileUtils.copyFile(largeOutputFile, out);
            largeOutputFile.delete();
            largeOutputFile = null;
        }
    }

    private int getContentLength() {
        if (contentBuffer instanceof ByteArrayOutputStream)
            return ((ByteArrayOutputStream) contentBuffer).size();
        if (largeOutputFile != null)
            return (int) largeOutputFile.length();
        return -1;
    }

    public void flush() throws IOException {
        if (contentBuffer != null)
            contentBuffer.flush();
        if (isStreaming)
            out.flush();
    }

    private void writeHeaderByte(int b) throws IOException {
        headerBuffer.write(b);
        push(last4HeaderBytes, b);
        if (sawEndMarker()) {
            inHeader = false;
            prepForContent();
        }
    }

    private void prepForContent() throws IOException {
        if (isStreaming)
            sendHeader();
        else if (isLarge) {
            largeOutputFile = TempFileFactory.get().createTempFile("cgi", null);
            contentBuffer = new BufferedOutputStream(new FileOutputStream(
                    largeOutputFile));
        } else
            contentBuffer = new ByteArrayOutputStream();
    }

    private void push(byte[] buf, int b) {
        for (int i = 1;  i < buf.length;   i++)
            buf[i-1] = buf[i];
        buf[buf.length-1] = (byte) b;
    }

    private boolean sawEndMarker() {
        return (last4HeaderBytes[0] == '\r' &&
                last4HeaderBytes[1] == '\n' &&
                last4HeaderBytes[2] == '\r' &&
                last4HeaderBytes[3] == '\n');
    }

    private void writeContentByte(int b) throws IOException {
        if (isStreaming)
            out.write(b);
        else
            contentBuffer.write(b);
    }


    private void sendHeader() throws IOException {
        // Parse the headers generated by the cgi program.
        StringBuffer value = new StringBuffer();
        int status = -1;
        String statusString = null;
        String location = null;
        BufferedReader headerLines = new BufferedReader(new StringReader(
                headerBuffer.toString(WebServer.HEADER_CHARSET)));

        String line;
        while ((line = headerLines.readLine()) != null) {
            if (line.length() == 0)
                continue;

            String header = parseHeader(line, value);

            if (header.equalsIgnoreCase("Status")) {
                // header value is of the form nnn xxxxxxx (a 3-digit
                // status code, followed by an error string).
                statusString = value.toString();
                status = Integer.parseInt(statusString.substring(0, 3));
                statusString = statusString.substring(4);

            } else if (header.equalsIgnoreCase("Content-Type")) {
                resp.setContentType(value.toString());

            } else if (header.equalsIgnoreCase("Location")) {
                location = value.toString();

            } else {
                resp.addHeader(header, value.toString());
            }
        }

        // based on the headers read, send a response back
        if (location != null) {
            resp.sendRedirect(location);
        } else if (status > 0) {
            resp.sendError(status, statusString);
        } else {
            int len = getContentLength();
            if (len >= 0)
                resp.setContentLength(len);
        }
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

}
