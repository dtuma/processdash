// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.mobile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTTPUtils;

public class MobileSetupWizard extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {
        printRedirect("mobileWelcome.htm");
    }


    @Override
    protected void doPost() throws IOException {
        // make certain the request is coming from the local computer
        DashController.checkIP(env.get("REMOTE_ADDR"));

        // get the URL of our bridged working directory
        ProcessDashboard dash = (ProcessDashboard) getDashboardContext();
        WorkingDirectory workingDirectory = dash.getWorkingDirectory();
        String location = workingDirectory.getDescription();
        if (!location.startsWith("http")) {
            printRedirect("mobileError.shtm?notBridged");
            return;
        }

        // ask the PDES for a data token for our directory
        String dataToken;
        try {
            dataToken = getDataToken(location);
        } catch (IOException ioe) {
            printRedirect("mobileError.shtm?upgradePdes");
            return;
        }

        // retrieve the username we are using for our data
        String username = HttpAuthenticator.getLastUsername();

        // prepare the dashboard for use with the mobile REST APIs
        DashController.assignHierarchyNodeIDs();
        dash.flushWorkingData();

        // write information to the repository, and print a success page
        DataContext ctx = getDataRepository();
        ctx.putValue("/mobile//Data_Token", StringData.create(dataToken));
        if (username != null)
            ctx.putValue("/mobile//Username", StringData.create(username));
        printRedirect("mobileSuccess.shtm");
    }

    private String getDataToken(String datasetUrl) throws IOException {
        URL url = new URL(datasetUrl);
        String query = ResourceBridgeConstants.ACTION_PARAM + "="
                + ResourceBridgeConstants.LOCATION_TOKEN_ACTION;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length",
            Integer.toString(query.length()));
        OutputStream out = conn.getOutputStream();
        out.write(query.getBytes("UTF-8"));
        out.close();

        return HTTPUtils.getResponseAsString(conn);
    }

    private void printRedirect(String page) {
        out.write("Location: /dash/" + page + "\r\n\r\n");
    }

}
