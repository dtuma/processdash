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

package net.sourceforge.processdash.net.http;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;


public class CGIOutputStream extends OutputStream {

    public static final int NORMAL = 0;
    public static final int STREAMING = 1;
    public static final int LARGE = 2;

    private HTTPHeaderWriter headerWriter;
    private OutputStream out;
    private String charset;
    private boolean isStreaming;
    private boolean isLarge;
    private boolean inHeader;

    private ByteArrayOutputStream headerBuffer;
    private byte[] last4HeaderBytes = new byte[4];

    private OutputStream contentBuffer;
    private File largeOutputFile;

    /** Create a new CGI output stream.
     * 
     * @param headerWriter an object which is capable of sending headers
     *    to the client
     * @param out the output stream to which the content body should be written
     * @param charset the character set that was used to write the HTTP header
     * @param mode one of {@link #NORMAL}, {@link #STREAMING}, or {@link #LARGE}.
     */

    public CGIOutputStream(HTTPHeaderWriter headerWriter, OutputStream out,
                           String charset, int mode) {
        this.headerWriter = headerWriter;
        this.out = out;
        this.charset = charset;
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
        if (contentBuffer != null) try {
            contentBuffer.close();
        } catch (Exception e) {}

        if (largeOutputFile != null)
            largeOutputFile.delete();
    }

    public void finish() throws IOException {
        if (contentBuffer != null) try {
            contentBuffer.close();
        } catch (Exception e) {}

        if (!isStreaming) {
            sendHeader();
            sendContent();
        }

        out.flush();
    }

    private void sendContent() throws IOException {
        if (contentBuffer instanceof ByteArrayOutputStream) {
            ((ByteArrayOutputStream) contentBuffer).writeTo(out);
            contentBuffer = null;

        } else if (largeOutputFile != null){
            FileInputStream content = new FileInputStream(largeOutputFile);
            byte[] buf = new byte[2048];
            int bytesRead;
            while ((bytesRead = content.read(buf)) != -1)
                out.write(buf, 0, bytesRead);
            content.close();
            largeOutputFile.delete();
            largeOutputFile = null;
        }
    }

    private long getContentLength() {
        if (contentBuffer instanceof ByteArrayOutputStream)
            return ((ByteArrayOutputStream) contentBuffer).size();
        if (largeOutputFile != null)
            return largeOutputFile.length();
        return -1;
    }

    public void flush() throws IOException {
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
            largeOutputFile = File.createTempFile("cgi", null);
            contentBuffer = new FileOutputStream(largeOutputFile);
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
        String contentType = null, statusString = "OK", line, header;
        StringBuffer otherHeaders = new StringBuffer();
        StringBuffer text = new StringBuffer();
        int status = 200;
        BufferedReader headerLines = new BufferedReader
            (new StringReader(headerBuffer.toString(charset)));

        while ((line = headerLines.readLine()) != null) {
            if (line.length() == 0) continue;

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
                    .append(text.toString()).append("\r\n");
            }
        }

        // Success!! Send results back to the client.
        headerWriter.sendHeaders(status, statusString, contentType,
                                 getContentLength(),  -1,
                                 otherHeaders.toString());
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
