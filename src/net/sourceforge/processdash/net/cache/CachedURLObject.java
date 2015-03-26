// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.Ping;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class CachedURLObject extends CachedObject {

    public static final String PASSWORD_MISSING = "Password_Missing";
    public static final String PASSWORD_INCORRECT = "Password_Incorrect";
    public static final String NOT_FOUND = "Not_Found";
    public static final String COULD_NOT_RETRIEVE = "Could_Not_Retrieve";
    public static final String NO_SUCH_HOST = "No_Such_Host";
    public static final String COULD_NOT_CONNECT = "Could_Not_Connect";

    public static final String OWNER_HEADER_FIELD = "Dash-Owner-Name";
    public static final String OWNER_ATTR = "Owner";


    protected URL url;
    protected String credential;

    public CachedURLObject(ObjectCache c, String type, URL u) {
        this(c, type, u, null, null);
    }
    public CachedURLObject(ObjectCache c, String type, URL u,
                           String username, String password) {
        super(c, type);

        this.url = u;
        if (username != null && password != null)
            credential = HTTPUtils.calcCredential(username, password);
        else
            credential = null;


        // try to fetch the data.
        refresh();
    }

    /** Deserialize a cached URL object from an XML stream. */
    public CachedURLObject(ObjectCache c, int id, Element xml,
                           CachedDataProvider dataProvider) {
        super(c, id, xml, dataProvider);

        Element e = (Element) xml.getElementsByTagName("url").item(0);
        try {
            url = new URL(e.getAttribute("href"));
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Malformed or missing URL");
        }
        credential = e.getAttribute("credential");
        if (!XMLUtils.hasValue(credential)) credential = null;
    }

    /** Serialize information to XML */
    public void getXMLContent(StringBuffer buf) {
        buf.append("  <url href='")
            .append(XMLUtils.escapeAttribute(url.toString()));
        if (credential != null)
            buf.append("' credential='")
                .append(XMLUtils.escapeAttribute(credential));
        buf.append("'/>\n");
    }

    public boolean refresh() {
        try {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            if (credential != null)
                conn.setRequestProperty("Authorization", credential);

            /*
             * future enhancement: add a request header identifying a
             * "postback URL". This would be a URL on the local
             * dashboard. The TinyWebServer on the remote dashboard
             * would watch for this request header and update a list
             * of (local object, postback URL) pairs.  On shutdown, it
             * could automatically fetch copies of the objects and
             * POST them to the postback URLs.  This would basically
             * add a "push" mechanism to the existing "pull" mechanism
             * for object caching.  This would help keep cached objects
             * as up-to-date as possible.
             */

            conn.connect();

            // check for errors.
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status == 401 || status == 403)    // unauthorized?
                errorMessage = (credential == null
                                ? PASSWORD_MISSING : PASSWORD_INCORRECT);
            else if (status == 404)     // no such schedule?
                errorMessage = NOT_FOUND;
            else if (status != 200)     // some other problem?
                errorMessage = COULD_NOT_RETRIEVE;
            else {
                InputStream in = conn.getInputStream();
                byte [] httpData = FileUtils.slurpContents(in, true);
                synchronized (this) {
                    data = httpData;
                    dataProvider = null;
                }
                errorMessage = null;
                refreshDate = new Date();
                String owner = conn.getHeaderField(OWNER_HEADER_FIELD);
                if (owner != null)
                    setLocalAttr(OWNER_ATTR, owner);
                store();
                return true;
            }
        } catch (UnknownHostException uhe) {
            errorMessage = NO_SUCH_HOST;
        } catch (ConnectException ce) {
            errorMessage = COULD_NOT_CONNECT;
        } catch (IOException ioe) {
            errorMessage = COULD_NOT_RETRIEVE;
        }
        return false;
    }

    public boolean refresh(double maxAge, long maxWait) {
        if (!olderThanAge(maxAge))
            return true;

        switch (Ping.ping(url.getHost(), url.getPort(), maxWait)) {

        case Ping.HOST_NOT_FOUND:
            errorMessage = NO_SUCH_HOST;
            return false;

        case Ping.CANNOT_CONNECT:
            errorMessage = COULD_NOT_CONNECT;
            return false;

        case Ping.SUCCESS: default:
            return refresh();
        }
    }


    private static Resources RESOURCES = null;

    public static String translateMessage(ResourceBundle resources,
                                          String prefix,
                                          String errorKey) {
        try {
            String resourceKey = prefix + errorKey;
            String result = resources.getString(resourceKey);
            if (result != null) return result;
        } catch (MissingResourceException mre) {}

        if (RESOURCES == null)
            RESOURCES = Resources.getDashBundle("CachedURLObject");

        try {
            String result = RESOURCES.getString(errorKey);
            if (result != null) return result;
        } catch (MissingResourceException mre) {}

        return errorKey;
    }
}
