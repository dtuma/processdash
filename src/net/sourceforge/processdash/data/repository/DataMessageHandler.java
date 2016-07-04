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

package net.sourceforge.processdash.data.repository;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.MalformedData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;


/**
 * @since 2.2.4
 */
public class DataMessageHandler implements MessageHandler {

    private DashboardContext ctx;

    private static final String[] MESSAGE_TYPES = { "pdash.alterData" };

    private static final Logger logger = Logger
            .getLogger(DataMessageHandler.class.getName());


    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return MESSAGE_TYPES;
    }

    public void handle(MessageEvent message) {
        String prefix = getPrefix(message);
        if (prefix == null)
            return;

        Map<String, SimpleData> dataValues = parseData(message);
        if (dataValues == null || dataValues.isEmpty())
            return;

        Date editTimestamp = XMLUtils.getXMLDate(message.getMessageXml(),
            "editTimestamp");
        for (Entry<String, SimpleData> e : dataValues.entrySet()) {
            String dataName = DataRepository.createDataName(prefix, e.getKey());
            maybeSaveValue(message, dataName, e.getValue(), editTimestamp);
        }
    }

    private String getPrefix(MessageEvent message) {
        String nodeID = message.getMessageXml().getAttribute("nodeID");
        if (XMLUtils.hasValue(nodeID)) {
            PropertyKey key = ctx.getHierarchy().findKeyByNodeID(nodeID);
            if (key != null)
                return key.path();

            logger.warning("When handling message with ID '"
                    + message.getMessageId()
                    + "', could not find any node with ID '" + nodeID
                    + "'. Discarding message.");
            return null;
        }

        String prefix = message.getMessageXml().getAttribute("prefix");
        if (XMLUtils.hasValue(prefix))
            return prefix;

        logger.warning("When handling message with ID '"
                + message.getMessageId()
                + "', no nodeID or prefix was provided. Discarding message.");
        return null;
    }

    private Map<String, SimpleData> parseData(MessageEvent message) {
        try {
            String data = XMLUtils.getTextContents(message.getMessageXml());
            return ctx.getData().parseSimpleData(data);
        } catch (Exception e) {
            logger.warning("When handling message with ID '"
                    + message.getMessageId()
                    + "', couldn't parse data definitions. Discarding message.");
            return null;
        }
    }

    private void maybeSaveValue(MessageEvent message, String dataName,
            SimpleData value, Date editTimestamp) {
        // discard malformed data values
        if (value instanceof MalformedData) {
            logger.warning("When handling message with ID '"
                    + message.getMessageId()
                    + "', found malformed definition for '" //
                    + dataName + "'. Discarding value.");
            return;
        }

        // check to see if this data element has been edited locally *after*
        // the time in the message. If so, prefer the local edit and discard
        // this data change.
        DataRepository data = ctx.getData();
        String localTimestampName = dataName + "/Edit_Timestamp";
        if (editTimestamp != null) {
            SimpleData localTimestamp = data.getSimpleValue(localTimestampName);
            if (localTimestamp instanceof DateData) {
                if (((DateData) localTimestamp).getValue().after(editTimestamp))
                    return;
            }
        }

        // store the value in the data repository
        data.userPutValue(dataName, value);
        if (editTimestamp != null)
            data.putValue(localTimestampName, new DateData(editTimestamp, true));
    }

}
