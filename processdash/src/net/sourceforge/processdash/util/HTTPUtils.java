// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.util;


public class HTTPUtils {


    public static final String DEFAULT_CHARSET = "iso-8859-1";


    /** Extract the charset from a mime content type
     */
    public static String getCharset(String contentType) {
        String upType = contentType.toUpperCase();
        int pos = upType.indexOf("CHARSET=");

        if (pos == -1)
            return DEFAULT_CHARSET;

        int beg = pos + 8;

        return contentType.substring(beg);
    }


    /** Change the charset in a mime content type string; return the new
     * content type string
     */
    public static String setCharset(String contentType, String newCharset) {
        int pos = contentType.toLowerCase().indexOf("charset=");
        if (pos == -1)
            return contentType + "; charset=" + newCharset;
        else
            return contentType.substring(0, pos+8) + newCharset;
    }


    /** Extract the content type from the given HTTP response headers.
     */
    public static String getContentType(String header) {
        String upHeader = CRLF + header.toUpperCase();
        int pos = upHeader.indexOf(CRLF + "CONTENT-TYPE:");

        if (pos == -1)
            return null;

        int beg = pos + 15; // add length of header name and CRLF

        // ASSUMPTION: not supporting headers wrapped over multiple lines
        int end = upHeader.indexOf(CRLF, beg);

        if (end == -1)
            end = upHeader.length();

        return header.substring(beg - 2, end - 2).trim();
    }


    /** Determine the length (in bytes) of the header in an HTTP response.
     */
    public static int getHeaderLength(byte[] result) {
        int i = 0;
        int max = result.length-3;

        do {
            if ((result[i  ] == '\r') && (result[i+1] == '\n') &&
                (result[i+2] == '\r') && (result[i+3] == '\n'))
                return (i+4);

            i++;
        } while (i < max);

        return result.length;
    }


    protected static final String CRLF = "\r\n";
}
