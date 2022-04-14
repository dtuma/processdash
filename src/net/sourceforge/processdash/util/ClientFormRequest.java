// Copyright (C) 2022 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Class that automates the submission of a POST request with an
 * <tt>x-www-form-urlencoded</tt> body.
 */
public class ClientFormRequest implements ClientPostRequest {

    private URLConnection conn;

    private ByteArrayOutputStream body;

    public ClientFormRequest(URL url) throws IOException {
        this(url.openConnection());
    }

    public ClientFormRequest(URLConnection connection) throws IOException {
        this.conn = connection;
        this.body = new ByteArrayOutputStream();
        connection.setDoOutput(true);
        connection.setDoInput(true);
    }

    public URLConnection getConnection() {
        return conn;
    }

    public void setParameter(String name, Object value) throws IOException {
        if (body.size() > 0)
            body.write('&');
        body.write(urlEncode(name));
        body.write('=');
        body.write(urlEncode(value.toString()));
    }

    private byte[] urlEncode(String text) throws IOException {
        // Use the UTF-8 charset to URL encode the text as %dd escapes
        String encoded = URLEncoder.encode(text, ENCODING);
        // return bytes for the URL-encoded text, which will be plain ASCII
        return encoded.getBytes("US-ASCII");
    }

    public void setParameters(Object[] parameters) throws IOException {
        if (parameters != null) {
            for (int i = 0; i < parameters.length - 1; i += 2)
                setParameter(parameters[i].toString(), parameters[i + 1]);
        }
    }

    public InputStream post() throws IOException {
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setRequestProperty("Content-Length",
            Integer.toString(body.size()));

        OutputStream out = conn.getOutputStream();
        body.writeTo(out);
        out.close();

        return conn.getInputStream();
    }

    private static final String ENCODING = "UTF-8";

    private static final String CONTENT_TYPE = //
            "application/x-www-form-urlencoded; charset=" + ENCODING;

}
