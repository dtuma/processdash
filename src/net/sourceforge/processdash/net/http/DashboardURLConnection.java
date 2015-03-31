// Copyright (C) 2003-2015 Tuma Solutions, LLC
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.HTTPUtils;


public class DashboardURLConnection extends URLConnection {

    private WebServer webServer;
    private String[] headerLines;
    private InputStream inputStream;

    public DashboardURLConnection(WebServer webServer, URL url) {
        super(url);
        this.webServer = webServer;
    }


    public void connect() throws IOException {
        if (!connected) {
            byte[] results = webServer.getRequest(url.getFile(), false);

            int headerLen = HTTPUtils.getHeaderLength(results);
            parseHeader(results, headerLen);

            connected = true;
            inputStream = createResponseStream(results, headerLen);
        }
    }

    protected InputStream createResponseStream(byte[] rawData, int headerLen) {
        int contentLen = rawData.length - headerLen;
        return new ByteArrayInputStream(rawData, headerLen, contentLen);
    }

    private void parseHeader(byte[] results, int headerLen) throws IOException {
        String headerText = new String(results, 0, headerLen, WebServer.HEADER_CHARSET);
        headerLines = headerText.split("[\r\n]+");
    }

    protected void setConnectionData(String[] headers, InputStream response) {
        this.connected = true;
        this.headerLines = headers;
        this.inputStream = response;
    }

    private static Pattern HEADER_LINE_PATTERN =
        Pattern.compile("([^: \t]+)[: \t]*([^\r\n]*)");

    private String getKey(String line) {
        Matcher m = HEADER_LINE_PATTERN.matcher(line);
        if (m.matches() && m.group(2) != null)
            return m.group(1);
        else
            return null;
    }

    private String getValue(String line) {
        Matcher m = HEADER_LINE_PATTERN.matcher(line);
        if (m.matches())
            return m.group(2);
        else
            return null;
    }



    public String getHeaderFieldKey(int n) {
        if (!connected) try { connect(); } catch (Exception e) { return null; }

        if (++n < headerLines.length)
            return getKey(headerLines[n]);
        else
            return null;
    }

    public String getHeaderField(int n) {
        if (!connected) try { connect(); } catch (Exception e) { return null; }

        if (++n < headerLines.length)
            return getValue(headerLines[n]);
        else
            return null;
    }

    public String getHeaderField(String name) {
        if (!connected) try { connect(); } catch (Exception e) { return null; }

        for (int i = headerLines.length;  i-- > 1;  ) {
            Matcher m = HEADER_LINE_PATTERN.matcher(headerLines[i]);
            if (m.matches() && name.equalsIgnoreCase(m.group(1)))
                return m.group(2);
        }

        return null;
    }


    public Map getHeaderFields() {
        if (!connected) try { connect(); } catch (Exception e) { return null; }

        Map fields = new HashMap();

        for (int i = 1; i < headerLines.length; i++) {
            Matcher m = HEADER_LINE_PATTERN.matcher(headerLines[i]);
            if (m.matches()) {
                String key = m.group(1);
                List value = (List) fields.get(key);
                if (value == null)
                    value = new LinkedList();
                value.add(m.group(2));
                fields.put(key, value);
            }
        }

        Map result = new HashMap();
        for (Iterator i = fields.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            result.put(e.getKey(), Collections.unmodifiableList((List) e.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    public InputStream getInputStream() throws IOException {
        if (!connected)
            connect();

        return inputStream;
    }

    public Permission getPermission() {
        return null;
    }

}
