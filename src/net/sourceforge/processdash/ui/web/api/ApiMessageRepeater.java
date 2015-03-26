// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class ApiMessageRepeater implements MessageHandler {

    private static final String MESSAGE_TYPE = "restApiInvocation";

    private static final String[] MSG_TYPES = { MESSAGE_TYPE };

    private static final String HTTP_METHOD = "pdesRestHttpMethod";

    private static final String CLIENT_PATH = "pdesRestHttpUri";


    public String[] getMessageTypes() {
        return MSG_TYPES;
    }


    public void handle(MessageEvent message) {
        try {
            redeliver(message);
        } catch (Exception e) {
            System.out.println("Unable to deliver API message:");
            System.out.println(XMLUtils.getAsText(message.getMessageXml()));
            e.printStackTrace();
        }
    }

    private void redeliver(MessageEvent message) throws Exception {
        Element xml = message.getMessageXml();

        // open an HTTP connection to the requested API
        String uri = "/api" + xml.getAttribute(CLIENT_PATH);
        String url = Browser.mapURL(uri);
        URLConnection conn = new URL(url).openConnection();

        // Copy message attributes to connection parameters
        Map<String, String> attrs = XMLUtils.getAttributesAsMap(xml);
        for (Entry<String, String> e : attrs.entrySet()) {
            conn.setRequestProperty("X-PDASH-" + e.getKey(), e.getValue());
        }

        // if this was a POST request, write the POST-ed data.
        if ("POST".equalsIgnoreCase(xml.getAttribute(HTTP_METHOD))) {
            String body = xml.getTextContent();
            byte[] bodyBytes = body.getBytes(WebServer.getOutputCharset());

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length",
                Integer.toString(bodyBytes.length));

            OutputStream out = conn.getOutputStream();
            out.write(bodyBytes);
            out.close();
        }

        // read and discard the results returned by the api
        InputStream response = conn.getInputStream();
        System.out.print("Delivered message to " + uri + ", response is: ");
        FileUtils.copyFile(response, System.out);
        System.out.println();
        response.close();
    }

}
