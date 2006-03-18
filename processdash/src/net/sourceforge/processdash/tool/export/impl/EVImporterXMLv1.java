// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.impl;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListMerger;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EVImporterXMLv1 implements ArchiveMetricsFileImporter.Handler,
        ArchiveMetricsXmlConstants, EVXmlConstantsv1 {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_EARNED_VALUE.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {
        Document doc = XMLUtils.parse(in);
        NodeList schedules = doc.getElementsByTagName(SCHEDULE_ELEM);
        for (int i = 0; i < schedules.getLength(); i++)
            importSchedule(caller, (Element) schedules.item(i));
    }

    private void importSchedule(ArchiveMetricsFileImporter caller,
            Element element) {

        String owner = "";
        if (isPlainSchedule(element))
            owner = caller.getOwner();

        String scheduleName = element.getAttribute(SCHEDULE_NAME_ATTR);
        String dataName = ExportManager.exportedScheduleDataName(
                owner, scheduleName).substring(1);

        Element xmlElement = (Element) element.getElementsByTagName(
                EVTaskList.EV_TASK_LIST_ELEMENT_NAME).item(0);
        String xmlStr = XMLUtils.getAsText(xmlElement);
        StringData xmlVal = StringData.create(xmlStr);
        xmlVal.setEditable(false);

        caller.getDefns().put(dataName, xmlVal);
    }

    private boolean isPlainSchedule(Element element) {
        Element rootTask = (Element) element.getElementsByTagName("task")
                .item(0);
        String flag = rootTask.getAttribute("flag");
        return EVTaskListData.TASK_LIST_FLAG.equals(flag);
    }

}
