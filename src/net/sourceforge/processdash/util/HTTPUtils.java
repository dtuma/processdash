// Copyright (C) 2004-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;


public class HTTPUtils {


    public static final String DEFAULT_CHARSET = "iso-8859-1";


    /** Extract the charset from a mime content type
     */
    public static String getCharset(String contentType) {
        return getCharset(contentType, DEFAULT_CHARSET);
    }

    /** Extract the charset from a mime content type
     */
    public static String getCharset(String contentType, String defaultCharset) {
        String upType = contentType.toUpperCase();
        int pos = upType.indexOf("CHARSET=");

        if (pos == -1)
            return defaultCharset;

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


    /** Read the response from a connection and interpret it as a String,
     * using the content type charset from the response headers.
     */
    public static String getResponseAsString(URLConnection conn) throws IOException {
        String contentType = conn.getContentType();
        if (contentType == null)
            throw new IOException("No reponse, or no Content-Type");
        String charset = getCharset(contentType);
        byte[] rawData = FileUtils.slurpContents(conn.getInputStream(), true);
        return new String(rawData, charset);
    }


    /** Calculate the user credential that would work for an http
     * Authorization field.
     */
    public static String calcCredential(String user, String password) {
        String credential = user + ":" + password;
        credential = Base64.encodeBytes(credential.getBytes(),
                Base64.DONT_BREAK_LINES);
        return "Basic " + credential;
    }


    /**
     * Return true if the given Accept header requests a JSON result
     * @since 2.4.0.1
     */
    public static boolean isJsonRequest(String acceptHeader) {
        // if no Accept header was offered, JSON was not requested
        if (acceptHeader == null)
            return false;

        // if the Accept header does not mention JSON, return false.
        acceptHeader = acceptHeader.toLowerCase();
        int jsonPos = acceptHeader.indexOf("json");
        if (jsonPos < 0)
            return false;

        // if the Accept header doesn't mention HTML, or if the JSON entry
        // precedes the HTML entry, return true
        int htmlPos = acceptHeader.indexOf("html");
        return (htmlPos < 0 || jsonPos < htmlPos);
    }


    protected static final String CRLF = "\r\n";

    /**
     * Examine the request headers on a given request, and return true if the
     * current request appears to be on a different server (host) than its
     * referrer. (This method does not distinguish between different ports on
     * the same server.)
     * 
     * @param req
     *            a request to examine
     * @param additionalAcceptableHosts
     *            other hosts that are deemed to be "local." If the referrer URL
     *            matches one of those hosts, this method will return false.
     *            Entries in this list can be plain host names, host:port
     *            values, or URLs.
     * @return false if the referrer is present, and is from the same host as
     *         the current request, or from a host in the
     *         additionalAcceptableHosts list. true otherwise
     */
    public static boolean isCrossSiteRequest(HttpServletRequest req,
            String... additionalAcceptableHosts) {
        // reject null requests
        if (req == null)
            return true;

        // identify the host the request came from
        String source = getHostFromUri(req.getHeader("Origin"));
        if (source == null)
            source = getHostFromUri(req.getHeader("Referer"));

        // identify the host the request was sent to
        String target = getHostFromAuthority(req.getHeader("X-Forwarded-Host"));
        if (target == null)
            target = getHostFromAuthority(req.getHeader("Host"));

        // compare the source and the target
        if (source == null)
            return true;
        if (source.equalsIgnoreCase(target))
            return false;

        // compare the source to the acceptable aliases
        for (String alias : additionalAcceptableHosts) {
            // normalize the alias into a hostname
            if (alias == null)
                continue;
            else if (alias.contains("/"))
                alias = getHostFromUri(alias);
            else if (alias.contains(":"))
                alias = getHostFromAuthority(alias);

            // compare the source to the alias
            if (source.equalsIgnoreCase(alias))
                return false;
        }

        // the source doesn't match the target or the acceptable aliases
        return true;
    }

    private static String getHostFromUri(String uri) {
        try {
            return new URI(uri).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getHostFromAuthority(String auth) {
        if (!StringUtils.hasValue(auth))
            return null;

        int colonPos = auth.indexOf(':');
        return (colonPos == -1 ? auth : auth.substring(0, colonPos));
    }

}
