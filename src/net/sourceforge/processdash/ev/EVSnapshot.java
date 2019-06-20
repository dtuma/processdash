// Copyright (C) 2008-2019 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ExternalDataFile;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.util.XMLUtils;

public class EVSnapshot implements Comparable<EVSnapshot> {

    public static final String ID_DELIM = "/*";

    private String id;

    String name;

    String comment;

    private Date date;

    private EVTaskList taskList;

    private boolean needsRecalc;


    public EVSnapshot(String id, String name, String comment, Date date,
            EVTaskList taskList) {
        this.id = id;
        this.name = name;
        this.comment = comment;
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
        this.comment = xml.getAttribute(COMMENT_ATTR);
        this.date = XMLUtils.getXMLDate(xml, DATE_ATTR);

        NodeList taskListTags = xml.getElementsByTagName(
            EVTaskListXML.EV_TASK_LIST_ELEMENT_NAME);
        if (taskListTags.getLength() > 0) {
            Element root = (Element) taskListTags.item(0);

            NodeList scheduleTags = root
                    .getElementsByTagName(EVSchedule.SCHEDULE_TAG);
            if (scheduleTags.getLength() == 1) {
                // old-style snapshots only contain a single rolled up schedule
                this.taskList = new EVTaskListXML(name, root);
                ((EVCalculatorXML) taskList.calculator).setCalcForSnapshot();

            } else {
                // newer snapshots have schedules for each embedded task list
                List taskLists = new ArrayList();
                for (int i = scheduleTags.getLength() - 1; i-- > 0;) {
                    Element oneScheduleTag = (Element) scheduleTags.item(i);
                    Element taskTag = (Element) oneScheduleTag.getParentNode();
                    EVTaskList tl = new EVTaskListXML(null, taskTag);
                    ((EVCalculatorXML) tl.calculator).setCalcForSnapshot();
                    taskLists.add(tl);
                }
                EVTaskListRollup r = new EVTaskListRollup(name, taskLists);
                r.taskListID = root.getAttribute("tlid");
                r.setPseudoTaskIdForRoot();
                this.taskList = r;
            }

            this.needsRecalc = true;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
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

    /**
     * Apply a given task list filter to the data in this snapshot.
     * 
     * @param f
     *            the filter to apply
     * @return true if we were able to apply the filter; false if this is an
     *         older snapshot that does not support filtering
     */
    public boolean applyTaskListFilter(EVTaskListFilter f) {
        if (taskList instanceof EVTaskListRollup) {
            EVTaskListRollup r = (EVTaskListRollup) taskList;
            if (r.applyTaskListFilter(f))
                needsRecalc = true;
            return true;
        } else {
            return f == null || f.include("nonexistent task list");
        }
    }

    public String getAsXML() {
        return getAsXML(taskList.getAsXML());
    }

    protected String getAsXML(String taskListXml) {
        StringBuffer xml = new StringBuffer();
        xml.append("<" + SNAPSHOT_TAG + " " + NAME_ATTR + "='");
        xml.append(XMLUtils.escapeAttribute(name));
        if (XMLUtils.hasValue(comment))
            xml.append("' " + COMMENT_ATTR + "='")
                .append(XMLUtils.escapeAttribute(comment));
        xml.append("' " + DATE_ATTR + "='");
        xml.append(XMLUtils.saveDate(date));
        xml.append("'>");
        xml.append(taskListXml);
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
    private static final String COMMENT_ATTR = "comment";
    private static final String DATE_ATTR = "when";


    /**
     * Simple class which can extract the name and date for an EV snapshot
     * without performing any other calculations
     */
    public static class Metadata extends EVSnapshot {

        private String dataName;

        private String taskListID;

        public Metadata(String dataName, String taskListID, String snapshotId,
                String xml) throws SAXException, IOException {
            this(dataName, taskListID, snapshotId, xml,
                    getStartOfTaskListXml(xml));
        }

        private Metadata(String dataName, String taskListID, String snapshotId,
                String xml, int beg) throws SAXException, IOException {
            super(snapshotId, xml.substring(0, beg - 1) + "/>");
            this.dataName = dataName;
            this.taskListID = taskListID;
        }

        private static int getStartOfTaskListXml(String xml) {
            int pos = xml.indexOf(SNAPSHOT_TAG) + SNAPSHOT_TAG.length();
            pos = xml.indexOf('>', pos);
            if (pos == -1)
                throw new IllegalArgumentException("Invalid snapshot XML");
            else
                return pos + 1;
        }

        private static int getEndOfTaskListXml(String xml) {
            return xml.lastIndexOf("</" + SNAPSHOT_TAG);
        }

        @Override
        public String getAsXML() {
            return getAsXML("");
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public void save(DataContext data) {
            // to save memory, we did not retain the full XML document for
            // this snapshot. So to recreate the XML, we lazily retrieve the
            // data element that created this snapshot
            SimpleData val = data.getSimpleValue(dataName);
            if (val == null) {
                try {
                    val = getExtData().getDataValue(getExtDataName());
                } catch (IOException ioe) {}
            }

            // extract the task list XML from that data value
            String taskListXml;
            try {
                taskListXml = val.format();
                int beg = getStartOfTaskListXml(taskListXml);
                int end = getEndOfTaskListXml(taskListXml);
                taskListXml = taskListXml.substring(beg, end);
            } catch (Exception e) {
                return;
            }

            val = StringData.create(getAsXML(taskListXml));
            try {
                // save the modified snapshot to external data
                getExtData().putDataValue(getExtDataName(), val);
                data.putValue(dataName, null);
            } catch (IOException ioe) {
                // if an IO error occurs, save to the repository
                data.putValue(dataName, val);
            }
        }

        public void delete(DataContext data) {
            // delete the snapshot from the repository
            data.putValue(dataName, null);
            try {
                // delete the snapshot from external data
                getExtData().putDataValue(getExtDataName(), null);
            } catch (IOException ioe) {
                // if an I/O error prevents us from writing to ext data,
                // save a "false" value to the repository to override
                data.putValue(dataName, ImmutableDoubleData.FALSE);
            }
        }

        private String getExtDataName() {
            return EVTaskList.SNAPSHOT_DATA_PREFIX + "/" + getId();
        }

        private ExternalDataFile getExtData() {
            return EVTaskList.getExtData(taskListID);
        }

    }

}
