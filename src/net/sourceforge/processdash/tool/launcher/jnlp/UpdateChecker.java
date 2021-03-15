// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import java.io.IOException;
import java.util.UUID;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class UpdateChecker extends TinyCGIBase implements Runnable {

    private static Runnable relaunchTask;

    private static String token;


    /**
     * This logic runs periodically on the background task thread to check for
     * software updates. If an update is found, display a notification
     */
    @Override
    public void run() {
        // ask the JNLP logic to check for updates. If any are found, this
        // will return a task that can relaunch the app.
        relaunchTask = JnlpDatasetLauncher.checkUpdate();

        // if a restart is applicable, display a notification to the user
        if (relaunchTask != null) {
            if (token == null)
                token = UUID.randomUUID().toString();
            UserNotificationManager.getInstance().addNotification(
                UpdateChecker.class.getName(),
                JnlpDatasetLauncher.res.getString("Update.Notification"),
                HTMLUtils.appendQuery(TRIGGER_URI, "token", token));
        }
    }

    /**
     * This logic runs when the user clicks the notification
     */
    @Override
    protected void writeContents() throws IOException {
        // guard against unrequested invocations of this script
        DashController.checkIP(env.get("REMOTE_ADDR"));

        String tokenParam = getParameter("token");
        if (tokenParam == null) {
            // with no token parameter, manually request an update check
            run();

        } else if (!tokenParam.equals(token)) {
            // if the token parameter is present but wrong, abort
            throw new TinyCGIException(403, "Forbidden");

        } else {
            // with correct trigger token, close and relaunch the dashboard
            ProcessDashboard dash = (ProcessDashboard) getDashboardContext();
            dash.exitProgram(relaunchTask);
        }

        // print a null document
        DashController.printNullDocument(out);
    }

    private static final String TRIGGER_URI = "/control/checkPdesUpdate?trigger";

}
