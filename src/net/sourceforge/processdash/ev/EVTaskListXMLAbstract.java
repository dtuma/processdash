// Copyright (C) 2002-2016 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.processdash.util.XMLUtils;

public class EVTaskListXMLAbstract extends EVTaskList {

    private String xmlSource = null;
    private String displayName = null;
    private String errorMessage = null;

    protected EVTaskListXMLAbstract(String taskListName,
                                    String displayName,
                                    boolean willNeedChangeNotification) {
        super(taskListName, displayName, willNeedChangeNotification);
    }

    protected boolean openXML(String xmlDoc, String displayName) {
        return openXML(xmlDoc, displayName, null);
    }

    protected boolean openXML(String xmlDoc, String displayName,
                              String errorMessage) {
        if (xmlDoc == null) {
            if (errorMessage == null)
                errorMessage = resources.getString
                    ("TaskList.Invalid_Schedule_Error_Message");
            createErrorRootNode(displayName, errorMessage);
            return false;
        } else if (xmlDoc.equals(xmlSource) &&
                   stringEquals(this.errorMessage, errorMessage) &&
                   stringEquals(this.displayName,  displayName))
            return true;

        try {
            // parse the XML document.
            Document doc = XMLUtils.parse(xmlDoc);
            Element docRoot = doc.getDocumentElement();
            openXML(docRoot, displayName, errorMessage);

            // keep a record of the xml doc we parsed for future efficiency.
            xmlSource = xmlDoc;
            return true;
        } catch (Exception e) {
            System.err.println("Got exception: " +e);
            e.printStackTrace();
            if (errorMessage == null)
                errorMessage = resources.getString
                    ("TaskList.Invalid_Schedule_Error_Message");
            createErrorRootNode(displayName, errorMessage);
            return false;
        }
    }

    protected void openXML(Element docRoot, String displayName,
            String errorMessage) throws Exception {
        List children = XMLUtils.getChildElements(docRoot);
        if (EV_TASK_LIST_ELEMENT_NAME.equals(docRoot.getTagName())) {
            // standard case: our document is rooted at an <EVModel> tag.
            // extract the task list and the schedule from the two child tags.
            if (children.size() != 2)
                throw new Exception("Expected two children of EVModel, but " +
                        "found " + children.size());
            root = new EVTask((Element) children.get(0));
            schedule = new EVSchedule((Element) children.get(1));
            taskListID = docRoot.getAttribute("tlid");

        } else if ("task".equals(docRoot.getTagName())
                && XMLUtils.hasValue(docRoot.getAttribute("flag"))) {
            // special case: we have been asked to build a task list from a
            // fragment within a larger XML document
            Element scheduleTag = (Element) children.get(children.size() - 1);
            if (!EVSchedule.SCHEDULE_TAG.equals(scheduleTag.getTagName()))
                throw new Exception("Didn't find schedule in XML fragment");
            root = new EVTask(docRoot);
            schedule = new EVSchedule(scheduleTag);
            taskListID = docRoot.getAttribute("tid").substring(3);
            // find the EVModel parent tag, so logic below can use it
            while (docRoot.getParentNode() instanceof Element) {
                docRoot = (Element) docRoot.getParentNode();
                if (EV_TASK_LIST_ELEMENT_NAME.equals(docRoot.getTagName()))
                    break;
            }

        } else {
            // the XML tag we were passed isn't one we can work with.
            throw new Exception("Unexpected source XML tag for task list");
        }

        setupTimeZone(docRoot);
        maybeRetargetTimezone();

        // optionally set the display name.
        if (displayName != null)
            ((EVTask) root).name = displayName;

        // optionally set an error message.
        if (errorMessage != null) {
            ((EVTask) root).setTaskError(errorMessage);
            if (!errorMessage.startsWith(" "))
                schedule.getMetrics().addError
                    (errorMessage, (EVTask) root);
        }

        // If this EV schedule was exported before task flags were
        // introduced, set an arbitrary flag on the EVTask root to pass
        // along our knowledge that it isn't a direct task node.
        if (!XMLUtils.hasValue(((EVTask) root).getFlag()))
            ((EVTask) root).flag = "xml/unknown";

        // create a calculator to minimally recalculate the schedule.
        boolean reorder = !"false".equals(docRoot.getAttribute("rct"));
        calculator = new EVCalculatorXML(this, reorder);
        setPseudoTaskIdForRoot();

        this.errorMessage = errorMessage;
        this.displayName  = displayName;
    }

    protected void setupTimeZone(Element docRoot) {
        // read the time zone data from the XML document if it is present
        String timezoneID = docRoot.getAttribute("tz");
        if (XMLUtils.hasValue(timezoneID))
            setTimezoneID(timezoneID);
    }

    private boolean stringEquals(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

}
