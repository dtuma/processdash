// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.pdes;

import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.HTMLUtils;

public class PDESUtil {


    /**
     * Compute the base URL of an enterprise server
     * 
     * @param url
     *            a URL to some resource on an Enterprise Server. It could be
     *            the base URL, a data URL, or some other webpage
     * @return the base URL of the server itself. This URL will always end in a
     *         slash
     */
    public static String getBaseUrl(String url) {
        for (String pat : BASE_URI_PATTERNS) {
            int pos = url.lastIndexOf(pat);
            if (pos != -1)
                return url.substring(0, pos + 1);
        }

        if (url.endsWith("/"))
            return url;
        else
            return url + "/";
    }

    private static final String[] BASE_URI_PATTERNS = { "/DataBridge", "/pub/",
            "/app/", "/api/" };



    /**
     * Compute the URL to the JNLP file for a given dataset
     * 
     * @param dataBridgeUrl
     *            the DataBridge URL for the dataset
     * @param thirdParty
     *            true if this is a personal dataset for some other individual
     * @return a URL to download the JNLP file for this dataset
     */
    public static String getJnlpUrl(String dataBridgeUrl, boolean thirdParty) {
        Matcher m = DATA_BRIDGE_URL_PAT.matcher(dataBridgeUrl);
        if (m.find())
            return getJnlpUrl(m.group(1), m.group(2), thirdParty);
        else
            return null;
    }

    /**
     * Compute the URL to the JNLP file for a given dataset
     * 
     * @param baseUrl
     *            the base URL of the server {@link #getBaseUrl(String)}
     * @param datasetID
     *            the ID of the dataset (e.g. INST-abc123)
     * @param thirdParty
     *            true if this is a personal dataset for some other individual
     * @return a URL to download the JNLP file for this dataset
     */
    public static String getJnlpUrl(String baseUrl, String datasetID,
            boolean thirdParty) {
        return baseUrl + "pub/LaunchDataset/" + HTMLUtils.urlEncode(datasetID)
                + (thirdParty ? "_" : "") + ".jnlp";
    }



    /**
     * Compute the URL to the "Details" page for a given dataset
     * 
     * @param dataBridgeUrl
     *            the DataBridge URL for the dataset
     * @return a URL to view the Details page for this dataset
     */
    public static String getViewUrl(String dataBridgeUrl) {
        Matcher m = DATA_BRIDGE_URL_PAT.matcher(dataBridgeUrl);
        if (m.find())
            return getViewUrl(m.group(1), m.group(2));
        else
            return null;
    }

    /**
     * Compute the URL to the "Details" page for a given dataset
     * 
     * @param baseUrl
     *            the base URL of the server {@link #getBaseUrl(String)}
     * @param datasetID
     *            the ID of the dataset (e.g. INST-abc123)
     * @return a URL to view the Details page for this dataset
     */
    public static String getViewUrl(String baseUrl, String datasetID) {
        return baseUrl + "app/Datasets/View.do?dataset="
                + HTMLUtils.urlEncode(datasetID);
    }

    private static final Pattern DATA_BRIDGE_URL_PAT = Pattern
            .compile("(http.+/)DataBridge/(INST-[a-z0-9]+)");



    /**
     * Get the dataset pseudo-ID used to refer to a given user's personal
     * dataset. (That dataset ID can then be used in calls to
     * {@link #getJnlpUrl(String, String, boolean)} or
     * {@link #getViewUrl(String, String)}.)
     * 
     * @param username
     *            the username of an individual
     * @return the pseudo-ID referring to that individual's personal dataset
     */
    public static String getAutoUserDatasetID(String username) {
        return "AUTOUSER-" + username;
    }



    /**
     * Retrieve an object for saving PDES-related user preferences
     */
    public static Preferences getPdesPrefs() {
        return Preferences.userRoot()
                .node("net/sourceforge/processdash/userPrefs/pdes");
    }

    public static final String DEFAULT_SERVER_PREF = "defaultServer";

}
