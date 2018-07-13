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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sourceforge.processdash.tool.launcher.LaunchableDataset;
import net.sourceforge.processdash.tool.launcher.LaunchableDataset.Type;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.HttpException;

public class DatasetsApi {

    /**
     * Get the list of datasets that appear on a user's My Datasets page.
     * 
     * Calling this method will trigger a user login action, if one has not
     * already been performed. The credentials entered will determine whose "My
     * Datasets" list gets returned.
     * 
     * @param pdesUrl
     *            any URL on the PDES server
     * @return a list of datasets that appear on the My Datasets page for the
     *         currently authenticated user
     * @throws IOException
     *             if the list of datasets could not be retrieved
     */
    public static List<LaunchableDataset> myDatasets(String pdesUrl)
            throws IOException {
        // compute the based URL for the server
        String baseUrl = PDESUtil.getBaseUrl(pdesUrl);

        // get the dataset list for the current user
        JSONObject data = getDatasetList(baseUrl);

        // if the response was empty due to a legacy authentication problem,
        // authenticate and then retry
        if (isLegacyPdesAuthProblem(data)) {
            triggerClientAuthentication(baseUrl);
            data = getDatasetList(baseUrl);
        }

        // build the result list
        List<LaunchableDataset> result = buildResultList(baseUrl, data);
        return result;
    }

    private static JSONObject getDatasetList(String baseUrl)
            throws IOException {
        // connect to the server and retrieve the data. This requires PDES
        // version 3.1.0 or higher
        String apiUrl = baseUrl + "api/datasets";
        URLConnection conn = new URL(apiUrl).openConnection();
        conn.setUseCaches(false);
        HttpException.checkValid(conn);
        String jsonText = HTTPUtils.getResponseAsString(conn);

        // parse the JSON we received
        try {
            return (JSONObject) new JSONParser().parse(jsonText);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    private static boolean isLegacyPdesAuthProblem(JSONObject data) {
        // Starting with PDES 3.6.2, the datasets API sends HTTP response codes
        // that trigger an HTTP Basic auth in the client. Prior versions of the
        // PDES would just respond with HTTP 200 OK, and a document describing
        // the error. This method returns true if it sees that error document.
        Object err = data.get("err");
        return (err instanceof Map
                && "auth-required".equals(((Map) err).get("code")));
    }

    private static void triggerClientAuthentication(String baseUrl)
            throws IOException {
        try {
            String url = baseUrl + "DataBridge/INST-000";
            URLConnection conn = new URL(url).openConnection();
            HttpException.checkValid(conn);

        } catch (HttpException.Unauthorized u) {
            // if the user fails to provide a valid username and password, abort
            throw u;

        } catch (IOException ioe) {
            // HTTP "404 Not Found" is expected, since INST-000 does not exist.
            // But the PDES should have requested authentication before telling
            // us that, and that authentication is our real goal.
        }
    }

    private static List<LaunchableDataset> buildResultList(String baseUrl,
            JSONObject data) {
        // get the identity of the user who this list is for
        String currentUser = (String) data.get("forUser");
        if (currentUser == null)
            return Collections.EMPTY_LIST;

        // get the list of datasets returned
        List<LaunchableDataset> result = new ArrayList<LaunchableDataset>();
        List<JSONObject> datasets = (List<JSONObject>) data.get("datasets");
        for (JSONObject dataset : datasets)
            result.add(makeLaunchableDataset(baseUrl, currentUser, dataset));

        // if the personal dataset is not present, add it
        maybeAddPersonalDataset(baseUrl, currentUser, result);

        return result;
    }

    private static LaunchableDataset makeLaunchableDataset(String baseUrl,
            String currentUser, JSONObject dataset) {
        // retrieve information about the dataset from the JSON
        String datasetID = (String) dataset.get("id");
        String name = (String) dataset.get("name");
        String owner = (String) dataset.get("owner");
        Type type = "team".equals(dataset.get("type")) //
                ? Type.Team : Type.Personal;

        // compute URLs for this dataset
        boolean thirdParty = !currentUser.equalsIgnoreCase(owner)
                && type == Type.Personal;
        String jnlpUrl = PDESUtil.getJnlpUrl(baseUrl, datasetID, thirdParty);
        String detailsUrl = PDESUtil.getViewUrl(baseUrl, datasetID);

        // create a dataset object and return it
        LaunchableDataset d = new LaunchableDataset(name, type, jnlpUrl,
                detailsUrl, owner, thirdParty);
        return d;
    }

    private static void maybeAddPersonalDataset(String baseUrl,
            String currentUser, List<LaunchableDataset> datasets) {
        // look for a personal dataset belonging to the current user. If one is
        // already present, do nothing.
        for (LaunchableDataset dataset : datasets) {
            if (dataset.isPersonal() && !dataset.isThirdParty())
                return;
        }

        // create a default personal dataset and add it to the list
        String datasetID = PDESUtil.getAutoUserDatasetID(currentUser);
        LaunchableDataset d = new LaunchableDataset("Process Dashboard",
                Type.Personal, PDESUtil.getJnlpUrl(baseUrl, datasetID, false),
                PDESUtil.getViewUrl(baseUrl, datasetID), currentUser, false);
        datasets.add(0, d);
    }

}
