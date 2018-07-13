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
import java.util.List;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.launcher.LaunchableDataset;
import net.sourceforge.processdash.tool.launcher.jnlp.JnlpDatasetLauncher;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.util.HttpException;

public class PDESPersonalDatasetLauncher {

    public static void launch() {
        // initialize the authentication logic
        Resources r = Resources.getDashBundle("Launcher");
        String title = r.getString("Process_Dashboard");
        HttpAuthenticator.maybeInitialize(title);

        // get the URL of the server
        String pdesUrl = getServerUrl();
        if (pdesUrl == null)
            return;

        // get the user's personal dataset on the server
        LaunchableDataset dataset = getPersonalDataset(pdesUrl);
        if (dataset == null)
            return;

        // launch the user's personal dataset
        JnlpDatasetLauncher.launch(dataset.getLocation());
    }

    private static String getServerUrl() {
        // if a default server is configured, return it
        Preferences prefs = PDESUtil.getPdesPrefs();
        String defaultUrl = prefs.get(PDESUtil.DEFAULT_SERVER_PREF, null);
        if (defaultUrl != null)
            return PDESUtil.getBaseUrl(defaultUrl);

        // prompt the user for the server URL
        String url = PDESDatasetChooser.promptForServerUrl(null);
        if (url != null)
            prefs.put(PDESUtil.DEFAULT_SERVER_PREF, url);
        return url;
    }

    private static LaunchableDataset getPersonalDataset(String pdesUrl) {
        try {
            // retrieve the user's datasets, and return their personal one
            List<LaunchableDataset> datasets = DatasetsApi.myDatasets(pdesUrl);
            for (LaunchableDataset d : datasets) {
                if (d.isPersonal() && !d.isThirdParty())
                    return d;
            }

        } catch (HttpException.Unauthorized u) {
            // if the user didn't enter valid credentials, admonish them
            PDESDatasetChooser.showErrorMessage(null, "Error.Unauthorized");

        } catch (IOException e) {
            // if we couldn't reach the server, display an error message
            Resources r = Resources.getDashBundle("ProcessDashboard.Errors");
            ExceptionDialog.showWithSubdialog(null, //
                r.getString("Read_File_Error.Title"), //
                r.formatStrings("Read_File_Error.Message_FMT", //
                    r.getString("Read_File_Error.Data_Server"), pdesUrl),
                " ", "<a>" + r.getString("More_Information") + "</a>", e);
        }

        return null;
    }

}
