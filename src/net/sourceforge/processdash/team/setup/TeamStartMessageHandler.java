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

import java.io.InputStream;
import java.net.URL;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.msg.MessageDispatcher;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;

public class TeamStartMessageHandler implements MessageHandler {

    private static final String MESSAGE_TYPE = "pdash.joinTeamProject";

    private static final String[] MESSAGE_TYPES = { MESSAGE_TYPE };

    private static final String JOIN_XML_URL = "Join_XML_URL";

    private static final String TEAM_URL = "Team_URL";

    private DashboardContext ctx;

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public void handle(MessageEvent message) {
        Element messageXml = message.getMessageXml();
        for (Element joinXml : XMLUtils.getChildElements(messageXml)) {

            // see if this message refers to a joining document that is
            // located elsewhere. If so, try downloading it.
            String externalUrl = joinXml.getAttribute(JOIN_XML_URL);
            if (XMLUtils.hasValue(externalUrl)) {
                try {
                    InputStream in = new URL(externalUrl).openStream();
                    joinXml = XMLUtils.parse(in).getDocumentElement();
                    if (!XMLUtils.hasValue(joinXml.getAttribute(TEAM_URL)))
                        joinXml.setAttribute(TEAM_URL, externalUrl);
                } catch (Exception e) {
                    // If we encounter any problems downloading the document,
                    // mark this message as "not handled" so we can potentially
                    // retry the operation in the future.
                    MessageDispatcher.getInstance().setMessageHandled(message,
                        false);
                    continue;
                }
            }

            // register the joining invitation with the notifier.
            TeamStartNotifier.addJoinInvitation(ctx, joinXml);
        }
    }

}
