// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.msg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.StringUtils;

public class MessageDispatcher {

    private static MessageDispatcher INSTANCE = null;

    private static DashboardPermission INIT_PERMISSION =
        new DashboardPermission("messageDispatcher.initialize");
    private static DashboardPermission DISPATCH_PERMISSION =
        new DashboardPermission("messageDispatcher.dispatch");

    private static Logger logger = Logger.getLogger(MessageDispatcher.class
            .getName());

    public synchronized static void init(DashboardContext context) {
        INIT_PERMISSION.checkPermission();
        INSTANCE = new MessageDispatcher(context);
    }

    public static MessageDispatcher getInstance() {
        if (INSTANCE == null)
            throw new IllegalStateException("Not yet initialized");
        return INSTANCE;
    }

    private DataRepository data;

    private Map<String, MessageHandler> handlers;

    private MessageDispatcher(DashboardContext context) {
        data = context.getData();
        handlers = new HashMap<String, MessageHandler>();

        List extensions = ExtensionManager.getExecutableExtensions(
            EXTENSION_NAME, context);
        for (Object ext : extensions) {
            if (ext instanceof MessageHandler) {
                MessageHandler handler = (MessageHandler) ext;
                String[] types = handler.getMessageTypes();
                if (types != null) {
                    for (String type : types)
                        if (StringUtils.hasValue(type))
                            handlers.put(type, handler);
                }
            } else if (ext != null) {
                logger.severe("Object of type '" + ext.getClass().getName()
                        + "' does not implement MessageHandler; discarding");
            }
        }
    }


    public boolean dispatch(MessageEvent message, boolean redo) {
        DISPATCH_PERMISSION.checkPermission();

        String serverID = message.getServerId();
        String messageID = message.getMessageId();
        if (redo == false && isMessageHandled(serverID, messageID))
            return true;

        String messageType = message.getMessageType();
        MessageHandler handler = handlers.get(messageType);
        if (handler == null)
            return false;

        try {
            // optimisically mark the message as "handled." This can prevent
            // infinite loops if a message somehow triggers itself again. It
            // also gives the handler an opportunity to explicitly clear the
            // "handled" flag as part of its work.
            setMessageHandled(serverID, messageID, true);
            // handle the message
            handler.handle(message);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING,
                "Encountered exception when handling message [msgId='"
                        + messageID + "', type='" + messageType + "']", e);
            // undo our optimistic assumption about handling the message
            setMessageHandled(serverID, messageID, false);
            return false;
        }
    }

    public void setMessageHandled(MessageEvent event, boolean handled) {
        setMessageHandled(event.getServerId(), event.getMessageId(), handled);
    }

    private void setMessageHandled(String serverID, String messageID,
            boolean handled) {
        if (serverID != null) {
            ListData l = ListData.asListData(data.getValue(SERVER_MESSAGES));
            if (l == null)
                l = new ListData();
            if (handled == true)
                l.setAdd(serverID);
            else
                l.remove(serverID);
            data.putValue(SERVER_MESSAGES, l);
            if (messageID.contains("/pdes/"))
                return;
        }

        SimpleData value = (handled ? ImmutableDoubleData.TRUE : null);
        data.putValue(getHandledDataName(messageID), value);
    }

    private boolean isMessageHandled(String serverID, String messageID) {
        if (serverID != null) {
            ListData l = ListData.asListData(data.getValue(SERVER_MESSAGES));
            if (l != null && l.contains(serverID))
                return true;
            else if (messageID.contains("/pdes/"))
                return false;
        }

        SimpleData d = data.getSimpleValue(getHandledDataName(messageID));
        if (d == null || !d.test()) {
            return false;
        } else {
            if (serverID != null)
                setMessageHandled(serverID, messageID, true);
            return true;
        }
    }

    private String getHandledDataName(String messageID) {
        messageID = StringUtils.findAndReplace(messageID, "//", ",,");
        messageID = StringUtils.findAndReplace(messageID, "=", "--");
        return "/MessageDispatch_Handled/" + messageID;
    }

    public void noServerMessagesPresent() {
        data.putValue(SERVER_MESSAGES, null);
    }

    private static final String EXTENSION_NAME = "messageHandler";

    private static final String SERVER_MESSAGES = "/MessageDispatch_Handled/Server_Message_IDs";

}
