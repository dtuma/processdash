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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.util.XMLUtils;

public class TeamStartNotifier implements Runnable {

    private static final String JOIN_TAG = "join-team-project";

    private static final int MAX_OUTSTANDING_INVITES = 50;

    private static final String DATA_PREFIX = "/Team_Project_Invite/";

    private static final String DATA_SUFFIX = "/Join_XML";

    private static final String PROJECT_ID = "Project_ID";

    private static final String PROJECT_FULL_NAME = "Project_Full_Name";

    private static final String JOIN_URI = "/dash/teamStart.class?page=invite";

    private static final Resources resources = Resources
            .getDashBundle("TeamStart.Invite");


    private DashboardContext context;

    public void setDashboardContext(DashboardContext ctx) {
        this.context = ctx;
    }


    public void run() {
        notifyUserOfPendingInvitations(context);
    }


    public static void addJoinInvitation(DashboardContext ctx, Element joinXml) {
        // ensure this is a correct XML tag for joining a team project
        if (!JOIN_TAG.equals(joinXml.getTagName()))
            return;

        // choose a data name for storing the invitation
        String projectID = joinXml.getAttribute(PROJECT_ID);
        String dataName = getStorageDataName(ctx, projectID);

        // construct the XML string we should store
        String joinXmlStr = XMLUtils.getAsText(joinXml);
        joinXmlStr = joinXmlStr.replaceAll("\\s*[\r\n]\\s*", " ");

        // store the invitation in the data repository
        ctx.getData().putValue(dataName, StringData.create(joinXmlStr));

        // create a notification for the newly stored project
        notifyUserOfPendingInvitations(ctx);
    }

    private static String getStorageDataName(DashboardContext ctx,
            String projectID) {

        String result = null;

        // check to see if any pending invitations are currently stored.
        for (int i = MAX_OUTSTANDING_INVITES; i-- > 0;) {
            String dataName = DATA_PREFIX + i + DATA_SUFFIX;
            SimpleData preexistingInvitation = ctx.getData().getSimpleValue(
                dataName);

            if (preexistingInvitation == null) {
                // if there is no invitation with this data name, we can
                // store our new invitation here.
                result = dataName;

            } else {
                // if we found a preexisting invitation, check to see if it
                // is for the same project as the newly received invitation.
                // If so, write the new invitation to this data element,
                // overwriting the old invitation.
                try {
                    String preexistingProjectID = XMLUtils
                            .parse(preexistingInvitation.format())
                            .getDocumentElement().getAttribute(PROJECT_ID);
                    if (projectID.equals(preexistingProjectID))
                        return dataName;

                } catch (Exception e) {
                }
            }
        }

        return result;
    }


    public static void notifyUserOfPendingInvitations(DashboardContext ctx) {
        for (int i = MAX_OUTSTANDING_INVITES; i-- > 0;) {
            String dataPrefix = DATA_PREFIX + i;
            String dataName = dataPrefix + DATA_SUFFIX;
            SimpleData sd = ctx.getData().getSimpleValue(dataName);
            if (sd != null) {
                try {
                    handlePendingInvitation(ctx, dataPrefix, sd.format());
                } catch (Exception e) {
                    ctx.getData().putValue(dataName, null);
                    if (!(e instanceof AlreadyJoinedException))
                        e.printStackTrace();
                }
            }
        }
    }

    private static void handlePendingInvitation(DashboardContext ctx,
            String dataPrefix, String joinXmlStr) throws SAXException,
            IOException {
        Element joinXml = XMLUtils.parse(joinXmlStr).getDocumentElement();
        String projectID = joinXml.getAttribute(PROJECT_ID);
        checkAlreadyJoined(ctx, PropertyKey.ROOT, projectID);

        String notificationId = "joinTeamProject." + projectID;
        String projectName = joinXml.getAttribute(PROJECT_FULL_NAME);
        String message = resources.format("Message_FMT", projectName);
        UserNotificationManager.getInstance().addNotification(notificationId,
            message, new StartJoinAction(dataPrefix));
    }

    private static void checkAlreadyJoined(DashboardContext ctx,
            PropertyKey key, String projectID) {
        String dataName = DataRepository.createDataName(key.path(), PROJECT_ID);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        if (sd != null && projectID.equals(sd.format())) {
            throw new AlreadyJoinedException();

        } else {
            DashHierarchy hier = ctx.getHierarchy();
            int i = hier.getNumChildren(key);
            while (i-- > 0) {
                PropertyKey childKey = hier.getChildKey(key, i);
                checkAlreadyJoined(ctx, childKey, projectID);
            }
        }
    }

    private static class AlreadyJoinedException extends RuntimeException {
    }

    private static class StartJoinAction implements Runnable {

        private String prefix;

        public StartJoinAction(String dataPrefix) {
            this.prefix = dataPrefix;
        }

        public void run() {
            String uri = WebServer.urlEncodePath(prefix) + "/" + JOIN_URI;
            Browser.launch(uri);
        }

    }

}
