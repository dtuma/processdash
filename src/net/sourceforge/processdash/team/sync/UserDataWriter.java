// Copyright (C) 2002-2019 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.sync;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.sync.SyncDiscrepancy.EVSchedule;
import net.sourceforge.processdash.team.sync.SyncDiscrepancy.ItemNote;
import net.sourceforge.processdash.team.sync.SyncDiscrepancy.NodeType;
import net.sourceforge.processdash.team.sync.SyncDiscrepancy.PlanTime;
import net.sourceforge.processdash.team.sync.SyncDiscrepancy.SizeData;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class UserDataWriter extends TinyCGIBase {


    private static final String FORCE_REFRESH_PARAM = "forceRefresh";


    private String taskListName;

    @Override
    protected void writeHeader() {
        out.print("Content-type: text/xml; charset=UTF-8\r\n\r\n");
        out.flush();
    }

    @Override
    protected void writeContents() throws IOException {
        String processID = getStringValue("Team_Process_PID");
        String processVersion = TemplateLoader.getPackageVersion(processID);
        String initials = getStringValue(TeamDataConstants.INDIV_INITIALS);
        taskListName = getStringValue(TeamDataConstants.PROJECT_SCHEDULE_NAME);
        Date timestamp = new Date();

        XmlSerializer ser = XMLUtils.getXmlSerializer(true);
        ser.setOutput(outStream, ENCODING);
        ser.startDocument(ENCODING, Boolean.TRUE);

        ser.startTag(null, DOCUMENT_TAG);
        ser.attribute(null, PROCESS_ID_ATTR, processID);
        ser.attribute(null, VERSION_ATTR, processVersion);
        ser.attribute(null, INITIALS_ATTR, initials);
        String userName = System
                .getProperty(TeamDataConstants.DATASET_OWNER_USERNAME_SYSPROP);
        if (hasValue(userName))
            ser.attribute(null, OWNER_USERNAME_ATTR, userName);
        String fullName = ProcessDashboard.getOwnerName(getDataContext());
        if (hasValue(fullName))
            ser.attribute(null, OWNER_FULLNAME_ATTR, fullName);
        String datasetID = DashController.getDatasetID(false);
        if (hasValue(datasetID))
            ser.attribute(null, DATASET_ID_ATTR, datasetID);
        ser.attribute(null, TIMESTAMP_ATTR, XMLUtils.saveDate(timestamp));

        if (hasValue(getData("Enable_Reverse_Sync"))) {
            try {
                writeDiscrepancies(ser);
                writeNewTasks(ser, processID);
                writeActualData(ser);
                writeSizeData(ser);
            } catch (WrappedIOException wie) {
                throw wie.getIOException();
            }
        }

        ser.endTag(null, DOCUMENT_TAG);
        ser.endDocument();
    }

    private void writeNewTasks(XmlSerializer ser, String processID)
            throws IOException {
        DashHierarchy hier = getPSPProperties();
        PropertyKey projectRoot = hier.findExistingKey(getPrefix());
        String phaseDataName = HierarchySynchronizer
                .getEffectivePhaseDataName(processID);
        writeNewTasks(ser, hier, projectRoot, "root", phaseDataName);
    }

    private void writeNewTasks(XmlSerializer ser, DashHierarchy hier,
            PropertyKey parent, String parentID, String phaseDataName)
            throws IOException {

        String prevSiblingID = null;
        for (int i = 0;  i < hier.getNumChildren(parent);  i++) {
            PropertyKey child = hier.getChildKey(parent, i);
            String path = child.path();
            String childID = getWbsIdForPath(path);

            if (!hasValue(childID)) {
                // this task does not have a WBS ID, so it is new.  Write a
                // "new task" entry for it.
                childID = getClientIdForPath(path);
                if (hasValue(childID))
                    writeNewTaskTag(ser, hier, parentID, prevSiblingID, child,
                        childID, phaseDataName);
                else
                    continue;
            }

            // Recurse and look for new tasks underneath this task.
            if (!isPSPTask(path))
                writeNewTasks(ser, hier, child, childID, phaseDataName);

            prevSiblingID = childID;
        }
    }

    private void writeNewTaskTag(XmlSerializer ser, DashHierarchy hier,
            String parentID, String previousSiblingID, PropertyKey node,
            String nodeClientID, String phaseDataName) throws IOException {

        // start a tag for the new task.
        ser.startTag(null, NEW_TASK_TAG);

        // write the name and ID of the task
        ser.attribute(null, WBS_ID_ATTR, nodeClientID);
        ser.attribute(null, TASK_NAME_ATTR, node.name());

        // write information about where the task is located in the WBS
        writeAttr(ser, PARENT_ID_ATTR, parentID);
        writeAttr(ser, PREV_SIBLING_ID_ATTR, previousSiblingID);

        // write the estimated time for the task
        String path = node.path();
        SimpleData estimatedTime = getData(path, "Estimated Time");
        writeTimeDataAttr(ser, TIME_ATTR, estimatedTime);

        if (isPSPTask(path)) {
            // if this is a PSP project, write the node type "PSP"
            writeAttr(ser, NODE_TYPE_ATTR, "PSP");

        } else {
            // otherwise, write the node type if there is one
            String phaseType = getStringData(getData(path, phaseDataName));
            if (phaseType != null && !phaseType.startsWith("?"))
                writeAttr(ser, NODE_TYPE_ATTR, phaseType);
        }

        // if this node has a workflow ID, write it as well
        String workflowID = getStringData(getData(path,
            TeamDataConstants.WORKFLOW_ID_DATA_NAME));
        writeAttr(ser, WORKFLOW_ID_ATTR, workflowID);

        // end the new task tag
        ser.endTag(null, NEW_TASK_TAG);
    }

    private void writeAttr(XmlSerializer ser, String name, String value)
            throws IOException {
        if (hasValue(value))
            ser.attribute(null, name, value);
    }

    private void writeActualData(XmlSerializer ser) throws IOException {
        DashHierarchy hier = getPSPProperties();
        PropertyKey projectRoot = hier.findExistingKey(getPrefix());
        writeActualData(ser, hier, projectRoot, true);
    }

    private void writeActualData(XmlSerializer ser, DashHierarchy hier,
            PropertyKey node, boolean isRoot) throws IOException {

        if (!isRoot)
            writeActualDataForNode(ser, hier, node);

        int numChildren = hier.getNumChildren(node);
        for (int i = 0;  i < numChildren;  i++) {
            PropertyKey child = hier.getChildKey(node, i);
            writeActualData(ser, hier, child, false);
        }
    }

    private void writeActualDataForNode(XmlSerializer ser, DashHierarchy hier,
            PropertyKey node) throws IOException {
        String path = node.path();
        String wbsID = getWbsOrClientIdForPath(path);
        if (!hasValue(wbsID))
            return;

        SimpleData actualTime = getData(path, "Time");
        SimpleData startDate = getData(path, "Started");
        SimpleData completionDate = getData(path, "Completed");
        boolean pruned = isPrunedLeaf(path);
        boolean reportSubtasks = isPSPTaskWithReportableSubtaskData(hier, node,
            actualTime, completionDate);

        if (hasValue(actualTime) || hasValue(startDate)
                || hasValue(completionDate) || pruned || reportSubtasks) {
            ser.startTag(null, ACTUAL_DATA_TAG);
            ser.attribute(null, WBS_ID_ATTR, wbsID);
            writeTimeDataAttr(ser, TIME_ATTR, actualTime);
            writeActualDataAttr(ser, START_DATE_ATTR, startDate);
            writeActualDataAttr(ser, COMPLETION_DATE_ATTR, completionDate);
            if (pruned)
                ser.attribute(null, PRUNED_ATTR, "t");
            if (reportSubtasks)
                writeActualDataForPSPPhases(ser, hier, node);
            ser.endTag(null, ACTUAL_DATA_TAG);
        }
    }

    private boolean isPSPTaskWithReportableSubtaskData(DashHierarchy hier,
            PropertyKey node, SimpleData actualTime, SimpleData completionDate) {
        // if this isn't a PSP project, return false.
        if (getData(node.path(), "PSP Project") == null)
            return false;

        // if any of the PSP phases are pruned, report subtask data
        for (int i = hier.getNumChildren(node); i-- > 0; ) {
            PropertyKey phase = hier.getChildKey(node, i);
            if (isPrunedLeaf(phase.path()))
                return true;
        }

        // if the entire PSP task has been marked complete, we don't need to
        // report subtask data.
        if (hasValue(completionDate))
            return false;

        // if time has been logged to any of the phases, we need to report how
        // the actual time was spread across the phases. return true.
        if (hasValue(actualTime))
            return true;

        // check to see if any of the phases have been marked complete (with
        // no actual time logged). if so, return true.
        for (int i = hier.getNumChildren(node);  i-- > 0; ) {
            PropertyKey phase = hier.getChildKey(node, i);
            if (hasValue(getData(phase.path(), "Completed")))
                return true;
        }

        // This PSP task has no interesting subtask data.  Save CPU cycles and
        // don't write subtask data for this task.
        return false;
    }

    private void writeActualDataForPSPPhases(XmlSerializer ser,
            DashHierarchy hier, PropertyKey node) throws IOException {
        // iterate over the phases in this PSP process
        for (int i = hier.getNumChildren(node);  i-- > 0; ) {
            PropertyKey phase = hier.getChildKey(node, i);
            String path = phase.path();

            // for each phase, write an XML element containing subtask data.
            ser.startTag(null, SUBTASK_DATA_TAG);
            writeTimeDataAttr(ser, EST_TIME_ATTR, getData(path, "Estimated Time"));
            writeTimeDataAttr(ser, TIME_ATTR, getData(path, "Time"));
            writeActualDataAttr(ser, START_DATE_ATTR, getData(path, "Started"));
            writeActualDataAttr(ser, COMPLETION_DATE_ATTR, getData(path, "Completed"));
            if (isPrunedLeaf(path))
                ser.attribute(null, PRUNED_ATTR, "t");
            ser.endTag(null, SUBTASK_DATA_TAG);
        }
    }

    private void writeSizeData(XmlSerializer ser) throws IOException {
        DashHierarchy hier = getPSPProperties();
        PropertyKey projectRoot = hier.findExistingKey(getPrefix());
        if (projectRoot != null)
            writePspSizeData(ser, hier, projectRoot);

        ListData sizedObjects = ListData
                .asListData(getData("Sized_Object_List"));
        if (sizedObjects != null) {
            for (int i = sizedObjects.size(); i-- > 0;)
                writeLocalObjectSizeData(ser, (String) sizedObjects.get(i));
        }
    }

    private void writePspSizeData(XmlSerializer ser, DashHierarchy hier,
            PropertyKey node) throws IOException {
        String path = node.path();
        if (getData(path, "PSP Project") != null) {
            double planSize = getNumberData(getData(path, EST_NC_LOC));
            double actualSize = getNumberData(getData(path, NC_LOC));
            String wbsId = getInheritedWbsIdForPath(path);
            writeSizeDataTag(ser, wbsId, "LOC", planSize, actualSize, null);

        } else {
            for (int i = hier.getNumChildren(node);  i-- > 0; )
                writePspSizeData(ser, hier, hier.getChildKey(node, i));
        }
    }

    private void writeLocalObjectSizeData(XmlSerializer ser, String path)
            throws IOException {
        String description = getStringValue(path + "/Description");
        String units = getStringValue(path + "/Sized_Object_Units");
        double planSize = getNumberData(path + "/Estimated Size");
        double actualSize = getNumberData(path + "/Size");
        String wbsId = getInheritedWbsIdForPath(path);

        writeSizeDataTag(ser, wbsId, units, planSize, actualSize, description);
    }

    private void writeSizeDataTag(XmlSerializer ser, String wbsId,
            String units, double planSize, double actualSize, String description)
            throws IOException {
        boolean hasSize = (planSize > 0 || actualSize > 0);
        boolean hasUnits = (hasValue(units) && !units.startsWith("Inspected "));
        boolean hasWbsId = hasValue(wbsId);
        if (hasSize && hasUnits && hasWbsId) {
            if (NC_LOC.equals(units))
                units = "LOC";

            ser.startTag(null, SIZE_DATA_TAG);
            ser.attribute(null, WBS_ID_ATTR, wbsId);
            ser.attribute(null, UNITS_ATTR, units);
            if (planSize > 0)
                ser.attribute(null, EST_SIZE_ATTR, Double.toString(planSize));
            if (actualSize > 0)
                ser.attribute(null, ACTUAL_SIZE_ATTR, Double.toString(actualSize));
            if (hasValue(description))
                ser.attribute(null, DESCRIPTION_ATTR, description);
            ser.endTag(null, SIZE_DATA_TAG);
        }
    }

    private void writeTimeDataAttr(XmlSerializer ser, String attrName,
            SimpleData time) throws IOException {
        if (hasValue(time) && time instanceof DoubleData) {
            double hours = ((DoubleData) time).getDouble() / 60.0;
            ser.attribute(null, attrName, Double.toString(hours));
        }
    }

    private void writeActualDataAttr(XmlSerializer ser, String attrName,
            SimpleData value) throws IOException {
        if (hasValue(value)) {
            if (value instanceof StringData)
                ser.attribute(null, attrName, value.format());
            else
                ser.attribute(null, attrName, value.saveString());
        }
    }

    private boolean hasValue(SimpleData value) {
        return value != null && value.test();
    }

    private void writeDiscrepancies(XmlSerializer ser) throws IOException {
        ListData discrepancies = getDiscrepancyList();
        if (discrepancies != null && discrepancies.size() > 1) {
            DiscrepancyWriter dw = new DiscrepancyWriter(ser);
            for (int i = 1; i < discrepancies.size(); i++) {
                SyncDiscrepancy d = (SyncDiscrepancy) discrepancies.get(i);
                d.visit(dw);
            }
        }
    }

    private ListData getDiscrepancyList() throws IOException {
        ListData discrepancies = ListData.asListData(getData(
            SyncDiscrepancy.DISCREPANCIES_DATANAME));

        if (needsRefresh(discrepancies)) {
            refreshDiscrepancyList();
            discrepancies = ListData.asListData(getData(
                SyncDiscrepancy.DISCREPANCIES_DATANAME));
        }

        return discrepancies;
    }

    private boolean needsRefresh(ListData discrepancies) {
        if (discrepancies == null || discrepancies.size() < 1
                || parameters.containsKey(FORCE_REFRESH_PARAM))
            return true;
        Date timestamp = (Date) discrepancies.get(0);
        long age = System.currentTimeMillis() - timestamp.getTime();
        return (age > REFRESH_AGE_CUTOFF);
    }

    private void refreshDiscrepancyList() throws IOException {
        String syncUri = resolveRelativeURI("sync.class");
        getRequest(syncUri, false);
    }

    private SimpleData getData(String name) {
        return getDataContext().getSimpleValue(name);
    }

    private SimpleData getData(String prefix, String name) {
        String dataName = DataRepository.createDataName(prefix, name);
        return getDataContext().getSimpleValue(dataName);
    }

    private double getNumberData(String name) {
        return getNumberData(getData(name));
    }

    private double getNumberData(SimpleData sd) {
        if (sd instanceof NumberData)
            return ((NumberData) sd).getDouble();
        else
            return 0;
    }

    private String getStringData(SimpleData sd) {
        return (sd == null ? "" : sd.format());
    }

    private String getStringValue(String name) {
        return getStringData(getData(name));
    }

    private String getWbsIdForPath(String path) {
        return getStringData(getData(path, TeamDataConstants.WBS_ID_DATA_NAME));
    }

    private String getClientIdForPath(String path) {
        return getStringData(getData(path, TeamDataConstants.CLIENT_ID_DATA_NAME));
    }

    private String getWbsOrClientIdForPath(String path) {
        String result = getWbsIdForPath(path);
        if (!hasValue(result))
            result = getClientIdForPath(path);
        return result;
    }

    private String getInheritedWbsIdForPath(String path) {
        String result = getWbsOrClientIdForPath(path);
        if (hasValue(result))
            return result;

        SaveableData sd = getDataRepository().getInheritableValue(path,
            TeamDataConstants.WBS_ID_DATA_NAME);
        return (sd == null ? null : getStringData(sd.getSimpleValue()));
    }

    private boolean isPSPTask(String path) {
        return hasValue(getData(path, "PSP Project"));
    }

    private boolean isPrunedLeaf(String path) {
        return hasValue(getData(path, "leaf"))
                && EVTask.taskIsPruned(getDataRepository(), taskListName, path);
    }

    private static boolean hasValue(String s) {
        return StringUtils.hasValue(s);
    }

    private class WrappedIOException extends RuntimeException {

        public WrappedIOException(IOException ioe) {
            super(ioe);
        }

        public IOException getIOException() {
            return (IOException) getCause();
        }

    }

    private class DiscrepancyWriter implements SyncDiscrepancy.Visitor {

        XmlSerializer ser;

        public DiscrepancyWriter(XmlSerializer ser) {
            this.ser = ser;
        }

        public void visit(PlanTime p) {
            try {
                ser.startTag(null, PLAN_TIME_CHANGE_TAG);
                ser.attribute(null, PATH_ATTR, p.getPath());
                ser.attribute(null, WBS_ID_ATTR, p.getWbsId());
                ser.attribute(null, TIME_ATTR, Double.toString(p
                        .getPlannedHours()));
                ser.endTag(null, PLAN_TIME_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        public void visit(SizeData s) {
            try {
                ser.startTag(null, SIZE_CHANGE_TAG);
                ser.attribute(null, PATH_ATTR, s.getPath());
                ser.attribute(null, WBS_ID_ATTR, s.getWbsId());
                ser.attribute(null, UNITS_ATTR, s.getMetric());
                ser.attribute(null, PLAN_ATTR, Boolean.toString(s.isPlan()));
                ser.attribute(null, SIZE_VALUE_ATTR, Double.toString(s.getValue()));
                ser.attribute(null, WHEN_ATTR, "@" + s.getTimestamp());
                ser.endTag(null, SIZE_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        public void visit(EVSchedule s) {
            Map<Date, Double> changes = s.getExceptions();
            if (changes == null || changes.isEmpty())
                return;

            try {
                ser.startTag(null, SCHEDULE_CHANGE_TAG);
                for (Map.Entry<Date, Double> e : changes.entrySet()) {
                    Date when = e.getKey();
                    Double hours = e.getValue();
                    ser.startTag(null, SCHEDULE_EXCEPTION_TAG);
                    ser.attribute(null, WHEN_ATTR, XMLUtils.saveDate(when));
                    if (hours != null)
                        ser.attribute(null, HOURS_ATTR, hours.toString());
                    else
                        ser.attribute(null, HOURS_ATTR, DEFAULT_HOURS_VAL);
                    ser.endTag(null, SCHEDULE_EXCEPTION_TAG);
                }
                ser.endTag(null, SCHEDULE_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        public void visit(NodeType n) {
            try {
                ser.startTag(null, NODE_TYPE_CHANGE_TAG);
                ser.attribute(null, WBS_ID_ATTR, n.getWbsId());
                ser.attribute(null, NODE_TYPE_ATTR, n.getType());
                ser.endTag(null, NODE_TYPE_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        public void visit(ItemNote n) {
            try {
                ser.startTag(null, NOTE_CHANGE_TAG);
                ser.attribute(null, WBS_ID_ATTR, n.getWbsId());
                maybeWriteAttr(BASE_TIMESTAMP_ATTR, n.getBaseTimestamp());
                maybeWriteAttr(TIMESTAMP_ATTR, n.getTimestamp());
                maybeWriteAttr(AUTHOR_ATTR, n.getAuthor());
                maybeWriteAttr(FORMAT_ATTR, n.getFormat());
                if (StringUtils.hasValue(n.getText()))
                    ser.text(n.getText());
                ser.endTag(null, NOTE_CHANGE_TAG);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        }

        private void maybeWriteAttr(String attrName, Object value)
                throws IOException {
            if (value instanceof Date) {
                ser.attribute(null, attrName, XMLUtils.saveDate((Date) value));
            } else if (value != null) {
                ser.attribute(null, attrName, String.valueOf(value));
            }
        }

    }

    private static final long REFRESH_AGE_CUTOFF = 60 * 1000;

    private static final String ENCODING = "UTF-8";

    private static final String DOCUMENT_TAG = "userData";

    private static final String PROCESS_ID_ATTR = "teamProcessID";

    private static final String VERSION_ATTR = "dumpFileVersion";

    private static final String INITIALS_ATTR = "initials";

    private static final String OWNER_USERNAME_ATTR = "userName";

    private static final String OWNER_FULLNAME_ATTR = "fullName";

    private static final String DATASET_ID_ATTR = "datasetID";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String NEW_TASK_TAG = "newTask";

    private static final String TASK_NAME_ATTR = "name";

    private static final String PARENT_ID_ATTR = "parentWbsId";

    private static final String PREV_SIBLING_ID_ATTR = "prevSiblingId";

    private static final String EST_TIME_ATTR = "estTime";

    private static final String NODE_TYPE_ATTR = "nodeType";

    private static final String WORKFLOW_ID_ATTR = "workflowID";

    private static final String ACTUAL_DATA_TAG = "actualData";

    private static final String SUBTASK_DATA_TAG = "subtaskData";

    private static final String START_DATE_ATTR = "started";

    private static final String COMPLETION_DATE_ATTR = "completed";

    private static final String PRUNED_ATTR = "pruned";

    private static final String NC_LOC = "New & Changed LOC";

    private static final String EST_NC_LOC = "Estimated " + NC_LOC;

    private static final String SIZE_DATA_TAG = "sizeData";

    private static final String DESCRIPTION_ATTR = "description";

    private static final String UNITS_ATTR = "units";

    private static final String EST_SIZE_ATTR = "estSize";

    private static final String ACTUAL_SIZE_ATTR = "actSize";

    private static final String PLAN_TIME_CHANGE_TAG = "planTimeChange";

    private static final String PATH_ATTR = "path";

    private static final String WBS_ID_ATTR = "wbsId";

    private static final String TIME_ATTR = "time";

    private static final String SIZE_CHANGE_TAG = "sizeChange";

    private static final String PLAN_ATTR = "plan";

    private static final String SIZE_VALUE_ATTR = "value";

    private static final String SCHEDULE_CHANGE_TAG = "scheduleChange";

    private static final String SCHEDULE_EXCEPTION_TAG = "scheduleException";

    private static final String WHEN_ATTR = "when";

    private static final String HOURS_ATTR = "hours";

    private static final String DEFAULT_HOURS_VAL = "DEFAULT";

    private static final String NODE_TYPE_CHANGE_TAG = "nodeTypeChange";

    private static final String NOTE_CHANGE_TAG = "noteChange";

    private static final String BASE_TIMESTAMP_ATTR = "baseTimestamp";

    private static final String AUTHOR_ATTR = "author";

    private static final String FORMAT_ATTR = "format";

}
