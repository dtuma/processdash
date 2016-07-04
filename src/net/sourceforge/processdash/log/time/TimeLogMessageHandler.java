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

package net.sourceforge.processdash.log.time;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;

public class TimeLogMessageHandler implements MessageHandler {

    private DashboardContext ctx;

    private static final String[] MESSAGE_TYPES = { "pdash.alterTimeLog" };

    private static final Logger logger = Logger
            .getLogger(TimeLogMessageHandler.class.getName());

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public void handle(MessageEvent message) {
        // read the time log modifications from the message
        Iterator timeLogModifications = getTimeLogModifications(message);
        if (timeLogModifications == null)
            return;

        // apply the modifications to the time log
        while (timeLogModifications.hasNext()) {
            saveModification(message,
                (TimeLogEntry) timeLogModifications.next());
        }
    }

    private Iterator getTimeLogModifications(MessageEvent message) {
        try {
            String modifications = XMLUtils.getAsText(message.getMessageXml());
            ByteArrayInputStream in = new ByteArrayInputStream(
                    modifications.getBytes("UTF-8"));
            return new TimeLogReader(in);
        } catch (Exception e) {
            logger.warning("When handling message with ID '"
                    + message.getMessageId()
                    + "', could not parse time log entries. "
                    + "Discarding message.");
            return null;
        }
    }

    private void saveModification(MessageEvent message, TimeLogEntry mod) {
        // if the mod specifies a node ID, resolve it to an actual path
        String path = mod.getPath();
        if (path != null && !path.startsWith("/")) {
            PropertyKey key = ctx.getHierarchy().findKeyByNodeID(path);
            if (key != null) {
                TimeLogEntry setPath = new TimeLogEntryVO(mod.getID(),
                        key.path(), null, 0, 0, null);
                mod = TimeLogEntryVO.applyChanges(mod, setPath, true);
            } else {
                logger.warning("When handling message with ID '"
                        + message.getMessageId()
                        + "', could not find any node with ID '" + path
                        + "'. Discarding time log entry.");
                return;
            }
        }

        // add the modification to the dashboard time log
        ((ModifiableTimeLog) ctx.getTimeLog())
                .addModification((ChangeFlaggedTimeLogEntry) mod);
    }

}
