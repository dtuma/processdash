// Copyright (C) 2014-2021 Tuma Solutions, LLC
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class HttpException extends IOException {

    private int responseCode;

    private String responseMessage;

    private URL url;

    private Map<String, List<String>> headers;

    private HttpException(HttpURLConnection conn) throws IOException {
        super("HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage()
                + " <" + conn.getURL() + ">");
        this.responseCode = conn.getResponseCode();
        this.responseMessage = conn.getResponseMessage();
        this.headers = conn.getHeaderFields();
        this.url = conn.getURL();
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    /** @since 2.5.6 */
    public URL getUrl() {
        return url;
    }

    /** @since 2.6.6 */
    public Map<String, List<String>> getResponseHeaders() {
        return headers;
    }

    /** @since 2.6.6 */
    public String getResponseHeader(String name) {
        List<String> values = headers.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public static class Unauthorized extends HttpException {
        private Unauthorized(HttpURLConnection conn) throws IOException {
            super(conn);
        }
    }

    public static class Forbidden extends HttpException {
        private Forbidden(HttpURLConnection conn) throws IOException {
            super(conn);
        }
    }

    /** @since 2.5.6 */
    public static class NotFound extends HttpException {
        private NotFound(HttpURLConnection conn) throws IOException {
            super(conn);
        }
    }

    public static void checkValid(URLConnection conn) throws IOException {
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            int status = httpConn.getResponseCode();
            if (status / 100 == 2)
                ;
            else if (status == HttpURLConnection.HTTP_UNAUTHORIZED)
                throw new Unauthorized(httpConn);
            else if (status == HttpURLConnection.HTTP_FORBIDDEN)
                throw new Forbidden(httpConn);
            else if (status == HttpURLConnection.HTTP_NOT_FOUND)
                throw new NotFound(httpConn);
            else
                throw new HttpException(httpConn);
        }
    }

    /** @since 2.5.6 */
    public static IOException maybeWrap(URLConnection conn, IOException ioe) {
        try {
            checkValid(conn);
        } catch (HttpException he) {
            he.initCause(ioe);
            return he;
        } catch (Exception e) {
        }
        return ioe;
    }

}
