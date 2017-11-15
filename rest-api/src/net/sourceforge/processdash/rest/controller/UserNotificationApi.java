// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.POST;
import net.sourceforge.processdash.rest.rs.PUT;
import net.sourceforge.processdash.rest.rs.Path;
import net.sourceforge.processdash.rest.to.JsonMap;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.UserNotificationManager.Notification;

@Path("/notifications/")
public class UserNotificationApi {

    @GET
    @PUT
    @POST
    public Map getUserNotifications(HttpServletRequest req) {
        // delete a specific notification if requested
        UserNotificationManager mgr = UserNotificationManager.getInstance();
        String idToDelete = req.getParameter("delete");
        if (idToDelete != null)
            mgr.removeNotification(idToDelete);

        // retrieve list of user notifications
        List notifications = new ArrayList();
        try {
            for (Notification n : mgr.getNotifications()) {
                JsonMap json = new JsonMap("id", n.getId(), //
                        "message", n.getMessage(), "uri", n.getUri());
                if (TriggerURI.isTrigger(n.getUri()))
                    json.put("trigger", Boolean.TRUE);
                notifications.add(json);
            }
        } catch (Throwable t) {
            // Earlier versions of the Process Dashboard may not provide the
            // UserNotificationManager.getNotifications() method. In that case,
            // gracefully degrade and claim that no notifications are present.
        }

        // build result
        return new JsonMap("notifications", notifications, "stat", "ok");
    }

}
