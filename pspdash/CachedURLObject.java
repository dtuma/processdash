// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Map;

import org.w3c.dom.*;

public class CachedURLObject extends CachedObject {

    public static final String PASSWORD_MISSING =
        "You must supply a username and password to retrieve this item.";
    public static final String PASSWORD_INCORRECT =
        "The username and password you provided are not correct.";
    public static final String NOT_FOUND =
        "There is no existing item by this name.";
    public static final String COULD_NOT_RETRIEVE =
        "The dashboard could not retrieve this item.";
    public static final String NO_SUCH_HOST =
        "Couldn't find the remote computer.";
    public static final String COULD_NOT_CONNECT =
        "Couldn't connect to the remote computer.";

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
            credential = TinyWebServer.calcCredential(username, password);
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
                byte [] httpData = TinyWebServer.slurpContents(in, true);
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



    public static String translateMessage(String errorMessage, Map m) {
        String result = (String) m.get(errorMessage);
        return result == null ? errorMessage : result;
    }

}
