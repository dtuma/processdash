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

package net.sourceforge.processdash.hier;

import java.util.logging.Logger;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.msg.MessageDispatcher;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class HierarchyAltererMessageHandler implements MessageHandler {

    private String messageType;

    private DashboardContext ctx;

    private static final Logger logger = Logger
            .getLogger(HierarchyAltererMessageHandler.class.getName());

    public void setConfigElement(Element xml, String attrName) {
        messageType = xml.getAttribute("messageType");
        if (!XMLUtils.hasValue(messageType))
            messageType = "pdash.alterHierarchy";
    }

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return new String[] { messageType };
    }

    public void handle(MessageEvent message) {
        try {
            logger.info("Handling hierarchy alteration message "
                    + message.getMessageId());
            HierarchyAlterer alterer = DashController.getHierarchyAlterer();
            Element xml = message.getMessageXml();
            for (Element instr : XMLUtils.getChildElements(xml))
                handleInstr(alterer, instr);
            logger.info("Done handling hierarchy alteration message");

        } catch (HierarchyAlterationException hae) {
            // HierarchyAlterationExceptions typically indicate transient
            // problems, so mark the message as unhandled and try again later.
            MessageDispatcher.getInstance().setMessageHandled(message, false);
            logError(hae, message.getMessageXml());

        } catch (IllegalArgumentException iae) {
            // Print a warning about the invalid XML message we received.
            logError(iae, message.getMessageXml());
        }
    }

    private void logError(Exception ex, Element xml) {
        StringBuilder errMsg = new StringBuilder();
        errMsg.append(ex.getMessage() + " while handling message:\n").append(
            XMLUtils.getAsText(xml));
        logger.warning(errMsg.toString());
    }

    private void handleInstr(HierarchyAlterer alterer, Element instr)
            throws HierarchyAlterationException {
        String type = instr.getTagName();
        if (ADD_TEMPLATE.equals(type))
            handleAddTemplate(alterer, instr);
        else if (ADD_NODE.equals(type))
            handleAddNode(alterer, instr);
        else
            throw new IllegalArgumentException("Unrecognized instruction "
                    + type);
    }

    private void handleAddTemplate(HierarchyAlterer alterer, Element instr)
            throws HierarchyAlterationException {
        String hierPath = getRequiredAttr(instr, PATH_ATTR);
        String templateID = getRequiredAttr(instr, TEMPLATE_ID_ATTR);
        if (checkForExistingNode(hierPath, templateID))
            return;

        alterer.addTemplate(hierPath, templateID);

        if ("true".equals(instr.getAttribute(SELECT_ATTR)))
            DashController.setPath(hierPath);
    }

    private void handleAddNode(HierarchyAlterer alterer, Element instr)
            throws HierarchyAlterationException {
        String hierPath = getRequiredAttr(instr, PATH_ATTR);
        if (checkForExistingNode(hierPath, PLAIN_NODE_ID))
            return;

        alterer.addNode(hierPath);

        if ("true".equals(instr.getAttribute(SELECT_ATTR)))
            DashController.setPath(hierPath);
    }

    private String getRequiredAttr(Element xml, String attrName) {
        String result = xml.getAttribute(attrName);
        if (XMLUtils.hasValue(result))
            return result;
        else
            throw new IllegalArgumentException(attrName + " missing from "
                    + xml.getTagName() + " tag");
    }

    private boolean checkForExistingNode(String hierPath,
            String templateIdNeeded) {
        String existingID = getExistingTemplateId(hierPath);
        if (templateIdNeeded.equals(existingID))
            return true;
        else if (existingID != null)
            throw new IllegalArgumentException("Cannot create '" + hierPath
                    + "' A node is already in the way");
        else
            return false;
    }

    private String getExistingTemplateId(String hierPath) {
        PropertyKey existing = ctx.getHierarchy().findExistingKey(hierPath);
        if (existing == null)
            return null;

        String result = ctx.getHierarchy().getID(existing);
        return (XMLUtils.hasValue(result) ? result : PLAIN_NODE_ID);
    }

    private static final String ADD_TEMPLATE = "addTemplate";

    private static final String ADD_NODE = "addNode";

    private static final String PATH_ATTR = "path";

    private static final String TEMPLATE_ID_ATTR = "templateID";

    private static final String SELECT_ATTR = "select";

    private static final String PLAIN_NODE_ID = "";

}
