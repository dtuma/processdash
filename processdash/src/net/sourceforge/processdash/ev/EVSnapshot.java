// Copyright (C) 2008-2014 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.Date;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EVSnapshot implements Comparable<EVSnapshot> {

    private String id;

    private String name;

    private Date date;

    private EVTaskList taskList;

    private boolean needsRecalc;


    public EVSnapshot(String id, String name, Date date, EVTaskList taskList) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.taskList = taskList;
        this.needsRecalc = false;
    }

    public EVSnapshot(String snapshotId, String xml) throws SAXException,
            IOException {
        this(snapshotId, XMLUtils.parse(xml).getDocumentElement());
    }

    public EVSnapshot(String snapshotId, Element xml) {
        this.id = snapshotId;
        this.name = xml.getAttribute(NAME_ATTR);
        this.date = XMLUtils.getXMLDate(xml, DATE_ATTR);

        NodeList taskListTags = xml.getElementsByTagName(
            EVTaskListXML.EV_TASK_LIST_ELEMENT_NAME);
        if (taskListTags.getLength() > 0) {
            Element root = (Element) taskListTags.item(0);
            this.taskList = new EVTaskListXML(name, root);
            ((EVCalculatorXML) taskList.calculator).setCalcForSnapshot();
            this.needsRecalc = true;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getDate() {
        return date;
    }

    public EVTaskList getTaskList() {
        if (needsRecalc) {
            taskList.recalc();
            needsRecalc = false;
        }
        return taskList;
    }

    public String getAsXML() {
        StringBuffer xml = new StringBuffer();
        xml.append("<" + SNAPSHOT_TAG + " " + NAME_ATTR + "='");
        xml.append(XMLUtils.escapeAttribute(name));
        xml.append("' " + DATE_ATTR + "='");
        xml.append(XMLUtils.saveDate(date));
        xml.append("'>");
        xml.append(taskList.getAsXML());
        xml.append("</" + SNAPSHOT_TAG + ">");
        return xml.toString();
    }

    public int compareTo(EVSnapshot that) {
        return that.date.compareTo(this.date);
    }

    public String toString() {
        return getName();
    }

    private static final String SNAPSHOT_TAG = "evSnapshot";
    private static final String NAME_ATTR = "name";
    private static final String DATE_ATTR = "when";


    /**
     * Simple class which can extract the name and date for an EV snapshot
     * without performing any other calculations
     */
    public static class Metadata extends EVSnapshot {

        public Metadata(String snapshotId, String xml) throws SAXException,
                IOException {
            super(snapshotId, extractMetadataTag(xml));
        }

        private static String extractMetadataTag(String xml) {
            try {
                int pos = xml.indexOf(SNAPSHOT_TAG);
                pos = xml.indexOf('>', pos);
                return xml.substring(0, pos) + "/>";
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid snapshot XML", e);
            }
        }

    }

}
