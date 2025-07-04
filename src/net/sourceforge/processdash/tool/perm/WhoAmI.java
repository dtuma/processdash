// Copyright (C) 2017-2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;

public class WhoAmI {

    private String username;

    private String displayName;

    private boolean legacyPdesMode;

    private List<String> externalPermissionGrants;

    private List<UserAccountFlag> userAccountFlags;

    private static final Logger logger = Logger
            .getLogger(WhoAmI.class.getName());


    public WhoAmI(String pdesUrl) throws HttpException.Unauthorized {
        username = null;
        displayName = null;
        legacyPdesMode = false;
        externalPermissionGrants = Collections.EMPTY_LIST;
        userAccountFlags = Collections.EMPTY_LIST;
        identifyUser(pdesUrl);
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isLegacyPdesMode() {
        return legacyPdesMode;
    }

    public List<String> getExternalPermissionGrants() {
        return externalPermissionGrants;
    }

    public List<UserAccountFlag> getUserAccountFlags() {
        return userAccountFlags;
    }


    private void identifyUser(String pdesUrl)
            throws HttpException.Unauthorized {
        // try a number of ways to figure the current user, until one succeeds
        if (pdesUrl != null)
            identifyUserFromPDES(pdesUrl);
        if (!StringUtils.hasValue(username))
            identifyUserFromFileIO();
        if (!StringUtils.hasValue(username))
            identifyUserFromWhoamiCall();
        if (!StringUtils.hasValue(username))
            identifyUserFromSystemProperties();
    }

    private void identifyUserFromPDES(String datasetUrl)
            throws HttpException.Unauthorized {
        Matcher m = DATA_BRIDGE_URL_PAT.matcher(datasetUrl);
        if (!m.find())
            return;

        try {
            // make a REST API call to the server to identify the current user
            String baseUrl = m.group(1);
            String whoamiUrl = baseUrl + "/api/v1/users/whoami/";
            JSONObject json = makeRestApiCall(whoamiUrl);
            Map user = (Map) json.get("user");
            this.username = (String) user.get("username");
            this.displayName = (String) user.get("name");
            logger.info("From PDES, current user is " + username);

            // see if this user should have extra permissions
            List<String> permIDs = (List<String>) user.get("clientPermissions");
            if (permIDs != null)
                externalPermissionGrants = permIDs;

            // see if this user has flags on their account
            List<Map> flags = (List<Map>) user.get("flags");
            if (flags != null)
                userAccountFlags = UserAccountFlag.parseList(flags, baseUrl);

        } catch (HttpException.Unauthorized he) {
            // if the attempt to contact the "whoami" API triggered a password
            // challenge that the user failed, propagate that failure along.
            throw he;

        } catch (Exception e) {
            // older versions of the PDES will not have the whoami REST API.
            // fall back and try retrieving the HTTP Auth username that was
            // used to authenticate to the DataBridge
            this.username = HttpAuthenticator.getLastUsername();
            if (StringUtils.hasValue(username)) {
                logger.info("From HTTP, current user is " + username);
                legacyPdesMode = true;
            }
        }
    }

    private void identifyUserFromFileIO() {
        try {
            File f = File.createTempFile("whoami", ".tmp");
            Path path = Paths.get(f.getAbsolutePath());
            FileOwnerAttributeView ownerAttributeView = Files
                    .getFileAttributeView(path, FileOwnerAttributeView.class);
            UserPrincipal owner = ownerAttributeView.getOwner();
            this.username = discardDomain(owner.getName());
            f.delete();
            logger.info("From NIO, current user is " + username);

        } catch (Throwable t) {
            // this will fail on Java 1.6. Try the next option
        }
    }

    private void identifyUserFromWhoamiCall() {
        try {
            // execute the "whoami" command
            Process p = Runtime.getRuntime().exec("whoami");
            byte[] out = RuntimeUtils.collectOutput(p, true, false);
            String result = new String(out).trim();
            this.username = discardDomain(result);
            logger.info("From whoami, current user is " + username);

        } catch (Throwable t) {
        }
    }

    private void identifyUserFromSystemProperties() {
        this.username = discardDomain(System.getProperty("user.name"));
        logger.info("From system, current user is " + username);
    }

    private String discardDomain(String username) {
        if (!StringUtils.hasValue(username))
            return null;
        int pos = username.lastIndexOf('\\');
        return (pos == -1 ? username : username.substring(pos + 1));
    }



    /**
     * Return a predictable hash of a username.
     */
    public static String hashUsername(String username) {
        if (username == null || username.isEmpty() || "*".equals(username)
                || (username.startsWith("[") && username.endsWith("]")))
            return username;

        int hashCode = Math.abs(username.toLowerCase().hashCode());
        return "[" + Long.toString(hashCode, Character.MAX_RADIX) + "]";
    }



    /**
     * Make a REST API call, and return the result as JSON.
     */
    static JSONObject makeRestApiCall(String urlStr) throws IOException {
        InputStream in = null;
        try {
            URLConnection conn = new URL(urlStr).openConnection();
            conn.setRequestProperty("Accept-Language",
                Locale.getDefault().getLanguage());
            HttpException.checkValid(conn);
            in = conn.getInputStream();
            return (JSONObject) new JSONParser().parse(
                new InputStreamReader(new BufferedInputStream(in), "UTF-8"));
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception pe) {
            throw new IOException(pe);
        } finally {
            FileUtils.safelyClose(in);
        }
    }


    static final Pattern DATA_BRIDGE_URL_PAT = Pattern
            .compile("^(http.*)/DataBridge/([\\w-]+)");

}
