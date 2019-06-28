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

package teamdash.wbs;

import static teamdash.wbs.WBSEditor.PROJECT_CLOSED_SETTING;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sourceforge.processdash.team.mcf.CustomProcess;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.XMLUtils;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.AbstractNumericColumn;
import teamdash.wbs.columns.DirectSizeTypeColumn;
import teamdash.wbs.columns.LabelSource;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.MilestoneDeferredColumn;
import teamdash.wbs.columns.MilestoneVisibilityColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.SizeAccountingColumnSet;
import teamdash.wbs.columns.SizeTypeColumn;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.UnassignedTimeColumn;
import teamdash.wbs.columns.WbsNodeAttributeSource;
import teamdash.wbs.columns.WorkflowDefectInjectionRateColumn;
import teamdash.wbs.columns.WorkflowScriptColumn;
import teamdash.wbs.columns.WorkflowYieldColumn;


/** This class writes out an XML data file describing the work breakdown
 * structure.
 * 
 * The XML file written by WBSModel.getAsXML() is designed to capture the
 * minimum necessary data needed to recreate the WBS in the WBSEditor. This
 * file, on the other hand, is designed to be read by logic which synchronizes
 * individual dashboard instances to the WBS.  It therefore contains only the
 * data elements needed by that logic.  Even more important, the format is
 * independent of the WBS implementation - it does not rely on any aspect of
 * the internal naming schemes of WBS column attributes.
 */
public class WBSDataWriter {

    /** The work breakdown structure */
    private WBSModel wbsModel;
    /** Calculated data associated with the work breakdown structure */
    private DataTableModel dataModel;
    /** The team process */
    private TeamProcess process;
    /** The ID of the team project */
    private String projectID;
    /** The team member list for the project */
    private TeamMemberList teamList;
    /** The list of project workflows */
    private WorkflowWBSModel workflows;
    /** The list of project milestones */
    private MilestonesWBSModel milestonesModel;
    /** The team project user settings */
    private Properties userSettings;
    /** The list of milestone IDs which are deferred */
    private Set deferredMilestoneIDs;
    /** The list of column numbers for each team member time column */
    private IntList teamMemberColumns;
    /** The initials for each team member */
    private String[] initials;
    /** Node attribute names for retrieving sync time information */
    private String[] syncAttrs;
    /** Node attribute names flagging whether a user is assigned with 0 hours */
    private String[] zeroAssignmentAttrs;
    /** The column number of the unassigned time column */
    private int unassignedTimeColumn;
    /** The list of column numbers for each top-level size accounting column */
    private int[] sizeAccountingColumns;
    /** This column number of the direct size units column */
    private int directSizeUnitsColumn;
    /** The column numbers of the task labels column */
    private Integer[] labelColumns;
    /** The column numbers of the task attribute columns */
    private Integer[] attrColumns;
    /** The column number of the task dependencies column */
    private int dependencyColumn;
    /** Columns for workflow quality parameters */
    private AbstractNumericColumn workflowYield, workflowDefectInjRate;
    /** Maps XML tag names to objects capable of writing their attributes.
     * 
     * Each key should be an XML tag name returned by {@link #getTagNameForNode
     * (WBSNode)}, and each value should be an object implementing
     * {@link WBSDataWriter.AttributeWriter} */
    private HashMap attributeWriters;



    /** Create a new WBSDataWriter.
     */
    public WBSDataWriter(WBSModel wbsModel, DataTableModel dataModel,
                         TeamProcess process, String projectID,
                         TeamMemberList teamList,
                         WorkflowWBSModel workflows,
                         MilestonesWBSModel milestonesModel,
                         Properties userSettings) {
        this.wbsModel = wbsModel;
        this.dataModel = dataModel;
        this.process = process;
        this.projectID = projectID;
        this.teamList = teamList;
        this.workflows = workflows;
        this.milestonesModel = milestonesModel;
        this.userSettings = userSettings;

        if (dataModel != null) {
            if (SizeTypeColumn.isUsingNewSizeDataColumns(wbsModel)) {
                sizeAccountingColumns = new int[0];
            } else {
                sizeAccountingColumns = new int[SIZE_COLUMN_IDS.length];
                for (int i = 0; i < SIZE_COLUMN_IDS.length; i++)
                    sizeAccountingColumns[i] = dataModel
                            .findColumn(SIZE_COLUMN_IDS[i]);
            }
            directSizeUnitsColumn =
                dataModel.findColumn(DirectSizeTypeColumn.COLUMN_ID);
            dependencyColumn =
                dataModel.findColumn(TaskDependencyColumn.COLUMN_ID);
        } else {
            workflowYield = new WorkflowYieldColumn((WorkflowWBSModel) wbsModel,
                    process);
            workflowDefectInjRate = new WorkflowDefectInjectionRateColumn(
                    (WorkflowWBSModel) wbsModel, process);
        }
        attributeWriters = buildAttributeWriters();
    }



    /** Write XML WBS data to the given file.
     */
    public void write(File f) throws IOException {
        RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
        write(out);
        out.close();
    }



    /** Write XML WBS data to the given writer.
     */
    public void write(Writer out) throws IOException {
        // initialize
        if (dataModel == null) {
            labelColumns = attrColumns = null;
            teamMemberColumns = null;
            initials = syncAttrs = zeroAssignmentAttrs = null;
            deferredMilestoneIDs = Collections.EMPTY_SET;
        } else {
            labelColumns = dataModel.getLabelSourceColumns();
            attrColumns = dataModel.getAttributeSourceColumns();
            teamMemberColumns = dataModel.getTeamMemberColumnIDs();
            int numTeamMembers = teamMemberColumns.size();

            initials = new String[numTeamMembers];
            syncAttrs = new String[numTeamMembers];
            zeroAssignmentAttrs = new String[numTeamMembers];
            for (int i = 0; i < numTeamMembers; i++) {
                int col = teamMemberColumns.get(i);
                initials[i] = dataModel.getColumnName(col);

                String colID = dataModel.getColumn(col).getColumnID();
                syncAttrs[i] = WBSSynchronizer.getSyncAttrName(colID);

                zeroAssignmentAttrs[i] = TeamTimeColumn
                        .getMemberAssignedZeroAttrName(initials[i]);
            }
            unassignedTimeColumn = dataModel
                    .findColumn(UnassignedTimeColumn.COLUMN_ID);
            deferredMilestoneIDs = getDeferredMilestoneIDs();
        }

        // write XML header
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        // write the node data
        write(out, wbsModel.getRoot(), 0);
    }



    /** Write XML WBS data for a given node and its children.
     */
    private void write(Writer out, WBSNode node, int depth)
        throws IOException {

        // determine which XML tag to use
        String tagName = getTagNameForNode(node);

        // format the XML minimally - indent some number of spaces based
        // on XML tag depth
        writeIndent(out, depth);

        // write the XML tag and the standard element attributes.
        out.write("<" + tagName);
        writeAttr(out, NAME_ATTR, getWbsNodeName(node));
        writeAttr(out, ID_ATTR, node.getUniqueID());
        writeAttr(out, TASK_ID_ATTR, MasterWBSUtil.getNodeIDs(node, projectID));
        writeAttr(out, CLIENT_ID_ATTR,
            (String) node.getAttribute(WBSSynchronizer.CLIENT_ID_ATTR));
        writeAttr(out, RELAUNCH_SOURCE_ID_ATTR, getRelaunchSourceID(node));
        writeAttr(out, LABELS_ATTR, getLabelSaveString(node));
        writeAttr(out, WORKFLOW_ID_ATTR, getWorkflowIdSaveString(node));
        writeAttr(out, MILESTONE_ID_ATTR, getMilestoneIdSaveString(node));

        // if we are in workflow mode, write the associated workflow URLs.
        if (dataModel == null)
            writeAttr(out, URL_ATTR, (String) node.getAttribute(
                WorkflowScriptColumn.VALUE_ATTR));
        else
            // in regular WBS mode, write the script URLs stored on this node
            writeAttr(out, URL_ATTR, getScriptURLs(node));

        // write attributes specific to this XML tag type
        AttributeWriter aw = (AttributeWriter) attributeWriters.get(tagName);
        if (aw != null)
            aw.writeAttributes(out, node);

        // write workflow quality attributes if applicable
        if (workflowYield != null)
            writeAttr(out, CustomProcess.EST_YIELD,
                (NumericDataValue) workflowYield.getValueAt(node));
        if (workflowDefectInjRate != null)
            writeAttr(out, CustomProcess.EST_INJ_RATE,
                (NumericDataValue) workflowDefectInjRate.getValueAt(node));

        WBSNode[] children = wbsModel.getChildren(node);
        TaskDependencyList dependencies = null;
        if (dataModel != null)
            dependencies = (TaskDependencyList) WrappedValue.unwrap(dataModel
                    .getValueAt(node, dependencyColumn));
        List<String> nodeAttributes = getWbsNodeAttributes(node);

        if ((children == null || children.length == 0)
                && (dependencies == null || dependencies.isEmpty())
                && (nodeAttributes == null || nodeAttributes.isEmpty())
                && (NotesColumn.getTextAt(node) == null)
                && (depth > 0)) {
            // if this node has no children and no dependencies, just close
            // the XML tag.
            out.write("/>\n");
        } else {
            // if this node has children, print them recursively.
            out.write(">\n");
            if (depth == 0) {
                writeWbsNodeAttributeSpecs(out);
                writeTeamMembers(out);
                writeMilestoneMetadata(out);
            }
            writeDependencies(out, dependencies, depth+1);
            writeNote(out, node, depth+1);
            writeWbsNodeAttributeValues(out, nodeAttributes, depth+1);
            if (children != null)
                for (int i = 0;   i < children.length;   i++)
                    write(out, children[i], depth+1);
            writeIndent(out, depth);
            out.write("</" + tagName + ">\n");
        }
    }



    /** Get the IDs of milestones that have been marked "deferred" */
    private Set getDeferredMilestoneIDs() {
        Set result = new HashSet();
        if (milestonesModel != null) {
            for (WBSNode milestone : milestonesModel.getMilestones()) {
                if (MilestoneDeferredColumn.isDeferred(milestone))
                    result.add(milestone.getUniqueID());
            }
        }
        return result;
    }

    /** Return true if this WBS node represents a task or other item that has
     * been assigned to a deferred milestone */
    private boolean isNodeMilestoneDeferred(WBSNode node) {
        int milestoneID = MilestoneColumn.getMilestoneID(node, null);
        return milestoneID != -1
                && deferredMilestoneIDs.contains(milestoneID);
    }



    private String getWbsNodeName(WBSNode node) {
        String name = node.getName();
        if (".".equals(name))
            return ",";
        else if ("..".equals(name))
            return ",,";
        else
            return name;
    }



    private String getRelaunchSourceID(WBSNode node) {
        return (String) node.getAttribute("relaunchSourceID");
    }



    private String getLabelSaveString(WBSNode node) {
        if (dataModel == null)
            return null;

        String result = null;

        for (int col : labelColumns) {
            LabelSource ls = (LabelSource) dataModel.getColumn(col);
            result = appendLabels(result, ls.getLabels(node));
        }

        if (milestonesModel != null) {
            int milestoneID = MilestoneColumn.getMilestoneID(node, null);
            String milestoneName = TaskLabelColumn.convertToLabel(
                    milestonesModel.getNameForMilestone(milestoneID));
            result = appendLabels(result, milestoneName);
        }

        if (result != null)
            result = result.replaceAll(", ", ",");
        else
            result = NO_LABELS_VAL;

        return result;
    }

    private String appendLabels(String result, String newLabels) {
        if (newLabels == null) return result;
        else if (result == null) return newLabels;
        else return result + "," + newLabels;
    }



    private String getWorkflowIdSaveString(WBSNode node) {
        Object result = node.getAttribute(WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR);
        return (result == null ? null : result.toString());
    }



    private String getMilestoneIdSaveString(WBSNode node) {
        int result = (milestonesModel == null ? -1 //
                : MilestoneColumn.getMilestoneID(node, milestonesModel));
        return (result < 0 ? null : Integer.toString(result));
    }



    private String getScriptURLs(WBSNode node) {
        StringBuilder result = new StringBuilder();
        for (String attr : node.listAttributeNames()) {
            if (attr.endsWith(" Script URL")) {
                String url = (String) node.getAttribute(attr);
                if (StringUtils.hasValue(url))
                    result.append("\n").append(url);
            }
        }
        return (result.length() == 0 ? null : result.substring(1));
    }



    private void writeWbsNodeAttributeSpecs(Writer out) throws IOException {
        if (attrColumns != null) {
            for (int col : attrColumns) {
                WbsNodeAttributeSource as = (WbsNodeAttributeSource) dataModel
                        .getColumn(col);
                out.write("  <" + ATTRIBUTE_TAG);
                writeAttr(out, ID_ATTR, as.getAttributeId());
                writeAttr(out, NAME_ATTR, as.getAttributeName());
                writeAttr(out, INHERITS_ATTR,
                    Boolean.toString(as.isAttributeAutoInherited()));
                out.write("/>\n");
            }
        }
    }



    private List<String> getWbsNodeAttributes(WBSNode node) {
        if (attrColumns == null || attrColumns.length == 0)
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList<String>();
        for (int col : attrColumns) {
            WbsNodeAttributeSource as = (WbsNodeAttributeSource) dataModel
                    .getColumn(col);
            List<String> values = as.getAttributeValues(node);
            if (values != null && !values.isEmpty()) {
                for (String oneValue : values) {
                    result.add(as.getAttributeId());
                    result.add(oneValue);
                }
            }
        }
        return result;
    }



    private void writeWbsNodeAttributeValues(Writer out,
            List<String> attributeValues, int depth) throws IOException {
        if (attributeValues != null) {
            for (int i = 0; i < attributeValues.size(); i += 2) {
                String id = attributeValues.get(i);
                String value = attributeValues.get(i + 1);
                writeIndent(out, depth);
                out.write("<" + ATTRIBUTE_VALUE_TAG);
                writeAttr(out, ATTR_ID_ATTR, id);
                out.write(">");
                out.write(XMLUtils.escapeAttribute(value));
                out.write("</" + ATTRIBUTE_VALUE_TAG + ">\n");
            }
        }
    }



    private void writeTeamMembers(Writer out) throws IOException {
        if (teamList == null)
            return;

        List members = teamList.getTeamMembers();
        for (Iterator i = members.iterator(); i.hasNext();) {
            TeamMember t = (TeamMember) i.next();
            if (t != null && !t.isEmpty())
                t.getAsXML(out, true, teamList.getSubteamModel());
        }

    }



    private void writeMilestoneMetadata(Writer out) throws IOException {
        if (milestonesModel == null)
            return;

        for (WBSNode m : milestonesModel.getMilestones()) {
            out.write("  <" + MILESTONE_TAG);
            writeAttr(out, NAME_ATTR, m.getName());
            writeAttr(out, MILESTONE_LABEL_ATTR,
                TaskLabelColumn.convertToLabel(m.getName()));
            writeAttr(out, MILESTONE_ID_ATTR, m.getUniqueID());
            writeAttr(out, MILESTONE_FULL_ID_ATTR,
                projectID + ":" + m.getUniqueID());
            writeAttr(out, COLOR_ATTR,
                (String) m.getAttribute(MilestoneColorColumn.VALUE_ATTR));
            Date commitDate = MilestoneCommitDateColumn.getCommitDate(m);
            if (commitDate != null)
                writeAttr(out, MILESTONE_DATE_ATTR,
                    CALENDAR_DATE_FMT.format(commitDate));
            if (MilestoneVisibilityColumn.isHidden(m))
                writeAttr(out, MILESTONE_HIDDEN_ATTR, "true");
            if (MilestoneDeferredColumn.isDeferred(m))
                writeAttr(out, MILESTONE_DEFERRED_ATTR, "true");
            out.write("/>\n");
        }
    }



    private void writeDependencies(Writer out, TaskDependencyList dependencies,
            int depth) throws IOException {
        if (dependencies != null) {
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                TaskDependency d = (TaskDependency) i.next();
                writeIndent(out, depth);
                out.write("<" + DEPENDENCY_TAG);
                writeAttr(out, NAME_ATTR, d.displayName);
                writeAttr(out, TASK_ID_ATTR, d.nodeID);
                writeAttr(out, DEP_SRC_ATTR, "wbs");
                out.write("/>\n");
            }
        }
    }



    private void writeNote(Writer out, WBSNode node, int depth)
            throws IOException {
        String text = NotesColumn.getTextAt(node);
        if (text == null)
            return;

        writeIndent(out, depth);
        out.write("<" + NOTE_TAG);
        writeAttr(out, AUTHOR_ATTR, NotesColumn.getAuthorAt(node));
        writeAttr(out, TIMESTAMP_ATTR, NotesColumn.getTimestampAt(node));
        writeAttr(out, FORMAT_ATTR, "text");
        out.write(">");
        out.write(XMLUtils.escapeAttribute(text));
        out.write("</" + NOTE_TAG + ">\n");
    }



    /** Determine which XML tag should be used to represent the given node.
     */
    private String getTagNameForNode(WBSNode node) {
        String type = WorkflowUtil.getTypeViaWorkflow(node, workflows, true);
        if ("Project".equals(type))
            return PROJECT_TAG;
        if (TeamProcess.WORKFLOW_TYPE.equals(type))
            return WORKFLOW_TAG;
        if (TeamProcess.SOFTWARE_COMPONENT_TYPE.equals(type)
                || TeamProcess.COMPONENT_TYPE.equals(type))
            return SOFTWARE_TAG;
        if (TeamProcess.isOtherSizeType(type))
            return DOCUMENT_TAG;
        if (TeamProcess.isPSPTask(type))
            return PSP_TAG;
        if (type.endsWith(" Task"))
            return TASK_TAG;

        // default value (better than nothing)
        return TASK_TAG;
    }



    /** Convenience method: indent some number of spaces based on XML tag depth
     */
    private void writeIndent(Writer out, int depth) throws IOException {
        for (int i = 0; i < depth; i++)
            out.write("\t");
    }



    /** Convenience method for writing an integer XML attribute
     */
    private void writeAttr(Writer out, String name, int value)
        throws IOException
    {
        writeAttr(out, name, Integer.toString(value));
    }



    /** Convenience method for writing a date XML attribute
     */
    private void writeAttr(Writer out, String name, Date value)
        throws IOException
    {
        if (value != null)
            writeAttr(out, name, XMLUtils.saveDate(value));
    }



    /** Convenience method for writing a string XML attribute
     */
    private void writeAttr(Writer out, String name, String value)
        throws IOException
    {
        if (value != null) {
            out.write(" ");
            out.write(name);
            out.write("='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'");
        }
    }



    /** Convenience method for writing a numeric XML attribute
     */
    private void writeAttr(Writer out, String name, NumericDataValue value)
        throws IOException
    {
        if (value != null && !Double.isNaN(value.value)) {
            out.write(" ");
            out.write(name);
            out.write("='");
            out.write(Double.toString(value.value));
            out.write("'");
        }
    }



    /** Create AttributeWriter objects capable of writing XML attributes for
     * specific output element types.
     */
    private HashMap buildAttributeWriters() {
        HashMap result = new HashMap();
        result.put(PROJECT_TAG, new ProjectAttributeWriter());
        SizeAttributeWriter sw = new SizeAttributeWriter();
        result.put(SOFTWARE_TAG, sw);
        result.put(DOCUMENT_TAG, sw);

        result.put(TASK_TAG, new TaskAttributeWriter());
        result.put(PSP_TAG, new PSPTaskAttributeWriter());
        return result;
    }



    /** Interface for a tag-specific attribute writer */
    private interface AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node)
            throws IOException;
    }



    /** AttributeWriter which writes top-level project attributes.
     */
    private class ProjectAttributeWriter implements AttributeWriter {

        public void writeAttributes(Writer out, WBSNode node)
                throws IOException {
            String version = null;
            try {
                version = WBSDataWriter.class.getPackage()
                        .getImplementationVersion();
            } catch (Exception e) {}
            if (version == null)
                version = "999";
            writeAttr(out, VERSION_ATTR, version);
            writeAttr(out, SAVE_DATE_ATTR, new Date());
            writeAttr(out, MAX_CLIENT_IDS_ATTR,
                WBSSynchronizer.getMaxClientIdStr(node));
            // writeAttr(out, "workflowPhaseMatch", "relaxed");
            if ("true".equals(getUserSetting(PROJECT_CLOSED_SETTING)))
                writeAttr(out, PROJECT_CLOSED_SETTING, "true");
        }

        private String getUserSetting(String name) {
            if (userSettings == null)
                return null;
            else
                return (String) userSettings.get(name);
        }
    }



    /** AttributeWriter which writes top-down size attributes.
     */
    private class SizeAttributeWriter implements AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            maybeWriteSizeAttrs(out, node);
        }

        /** Write out XML attributes for size if a top-down size has been entered
         * for the given node.
         */
        protected void maybeWriteSizeAttrs(Writer out, WBSNode node)
            throws IOException
        {
            if (dataModel == null) return;

            // check with the direct size units column to see if a top-down size
            // has been entered for the given node.
            Object units = dataModel.getValueAt(node, directSizeUnitsColumn);
            if (units == null) return;

            // write an XML attribute for the size units
            writeAttr(out, UNITS_ATTR, String.valueOf(units));
            // write out XML attributes for each size accounting number
            for (int i = 0;   i < sizeAccountingColumns.length;   i++){
                Object size =
                    dataModel.getValueAt(node, sizeAccountingColumns[i]);
                writeAttr(out, SIZE_ACCOUNTING_ATTRS[i], formatNumber(size));
            }
        }
    }



    /** AttributeWriter which writes information about a non-PSP leaf task.
     */
    private class TaskAttributeWriter extends SizeAttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            String nodeType = WorkflowUtil.getTypeViaWorkflow(node, workflows,
                true);
            if (!nodeType.endsWith(" Task"))
                // should I throw some sort of error?
                return;

            String phaseName = removeTaskSuffix(nodeType);
            if (wbsModel.getChildCount(node) != 0) {
                // only write an "effective phase" attribute for non-leaf tasks.
                writeAttr(out, EFFECTIVE_PHASE_ATTR, phaseName);
                return;
            }

            String phaseType = process.getPhaseType(phaseName);
            String syncPhaseName = removeTaskSuffix((String) node
                    .getAttribute(WBSSynchronizer.SYNC_NODE_TYPE_ATTR));
            writeAttr(out, PHASE_NAME_ATTR, phaseName);
            writeAttr(out, PHASE_TYPE_ATTR, phaseType);
            writeAttr(out, SYNC_PHASE_NAME_ATTR, syncPhaseName);
            writeAttr(out, TIME_ATTR, getTeamMemberTimes(node));
            writeAttr(out, DEFERRED_TIME_ATTR, getTeamMemberDeferredTimes(node));
            writeAttr(out, SYNC_TIME_ATTR, getTeamMemberSyncTimes(node));
            if (TeamProcess.isCodeTask(nodeType)
                    || TeamProcess.isProbeTask(nodeType))
                maybeWriteSizeAttrs(out, node);
            else
                maybeWriteQualitySize(out, phaseName, phaseType, node);
        }

        private String removeTaskSuffix(String phaseName) {
            if (phaseName != null && phaseName.endsWith(" Task"))
                return phaseName.substring(0, phaseName.length()-5);
            else
                return phaseName;
        }

        private void maybeWriteQualitySize(Writer out, String phase,
                                           String phaseType, WBSNode node)
            throws IOException
        {
            if (dataModel == null) return;
            if (phaseType == null) return;
            if (!QUALITY_PHASE_TYPES.contains(phaseType.toUpperCase())) return;

            String units = process.getPhaseSizeMetric(phase);
            String colID = SizeAccountingColumnSet.getNCID(units);
            int column = dataModel.findColumn(colID);
            if (column == -1) return;

            writeAttr(out, INSP_UNITS_ATTR, units);
            writeAttr(out, INSP_SIZE_ATTR,
                      formatNumber(dataModel.getValueAt(node, column)));
        }
    }



    /** AttributeWriter which writes information about a PSP task.
     */
    private class PSPTaskAttributeWriter extends SizeAttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            writeAttr(out, TIME_ATTR, getTeamMemberTimes(node));
            writeAttr(out, DEFERRED_TIME_ATTR, getTeamMemberDeferredTimes(node));
            writeAttr(out, SYNC_TIME_ATTR, getTeamMemberSyncTimes(node));
            super.writeAttributes(out, node);
        }
    }



    /** Build an XML attribute value describing the time each team member plans
     * to spend in the given node.
     */
    private String getTeamMemberTimes(WBSNode node) {
        if (teamMemberColumns == null || isNodeMilestoneDeferred(node))
            return null;
        else
            return buildTeamMemberTimeString(node);
    }

    /** Build an XML attribute value describing the time each team member plans
     * to spend in the given node.
     */
    private String getTeamMemberDeferredTimes(WBSNode node) {
        if (teamMemberColumns == null || isNodeMilestoneDeferred(node) == false)
            return null;
        else
            return buildTeamMemberTimeString(node);
    }

    private String buildTeamMemberTimeString(WBSNode node) {
        StringBuffer result = new StringBuffer();
        for (int i = 0;   i < teamMemberColumns.size();   i++) {
            int col = teamMemberColumns.get(i);
            String time = formatNumber(dataModel.getValueAt(node, col));
            if (isZeroString(time)) {
                if (node.getAttribute(zeroAssignmentAttrs[i]) == null)
                    continue;
            }
            result.append(",").append(initials[i]).append("=").append(time);
        }
        String time = formatNumber(dataModel.getValueAt(node,
            unassignedTimeColumn));
        if (!isZeroString(time))
            result.append(",unassigned=").append(time);

        if (result.length() == 0)
            return null;

        result.append(",");
        return result.toString();
    }

    private boolean isZeroString(String time) {
        return "null".equals(time) || "0".equals(time) || "0.0".equals(time);
    }



    /** Build an XML attribute value describing the time each team member plans
     * to spend in the given node.
     */
    private String getTeamMemberSyncTimes(WBSNode node) {
        if (teamMemberColumns == null)
            return null;


        StringBuffer result = new StringBuffer();
        for (int i = 0;   i < teamMemberColumns.size();   i++) {
            double lastSyncedVal = node.getNumericAttribute(syncAttrs[i]);
            if (!Double.isNaN(lastSyncedVal)) {
                result.append(",").append(initials[i])
                    .append("=").append(lastSyncedVal);
            }
        }

        if (result.length() == 0)
            return null;

        result.append(",");
        return result.toString();
    }



    /** Format a numeric value so we can write it in an XML attribute.
     */
    private String formatNumber(Object value) {
        if (value == null) return "0";
        double d = NumericDataValue.parse(value);
        if (Double.isNaN(d) || Double.isInfinite(d)) d = 0;
        if (d == Math.floor(d))
            return Integer.toString((int) d);
        else
            return Double.toString(d);
    }




    // strings naming each XML tag we will output
    private static final String PROJECT_TAG = "project";
    private static final String WORKFLOW_TAG = "workflow";
    private static final String SOFTWARE_TAG = "component";
    private static final String DOCUMENT_TAG = "document";
    private static final String PSP_TAG = "psp";
    private static final String TASK_TAG = "task";
    private static final String ATTRIBUTE_TAG = "attrType";
    private static final String ATTRIBUTE_VALUE_TAG = "attrValue";
    private static final String MILESTONE_TAG = "milestone";
    private static final String DEPENDENCY_TAG = "dependency";
    private static final String NOTE_TAG = "note";

    /** A list of column IDs for the top-level size accounting columns */
    private static final String[] SIZE_COLUMN_IDS = new String[] {
        "Base", "Deleted", "Modified", "Added", "Reused", "N&C" };
    /** XML attribute names we will use to store data extracted from the
     * size accounting columns listed on the previous line */
    private static final String[] SIZE_ACCOUNTING_ATTRS = new String[] {
        "sizeBase", "sizeDel", "sizeMod", "sizeAdd", "sizeReu", "sizeNC" };

    // strings naming each XML attribute we will output
    private static final String NAME_ATTR = "name";
    private static final String ID_ATTR = "id";
    private static final String TASK_ID_ATTR = "tid";
    private static final String CLIENT_ID_ATTR = "cid";
    private static final String MAX_CLIENT_IDS_ATTR = "maxCid";
    private static final String RELAUNCH_SOURCE_ID_ATTR = "rsid";
    private static final String LABELS_ATTR = "labels";
    private static final String WORKFLOW_ID_ATTR = "wid";
    private static final String MILESTONE_ID_ATTR = "mid";
    private static final String MILESTONE_FULL_ID_ATTR = "fullMid";
    private static final String MILESTONE_LABEL_ATTR = "labelName";
    private static final String MILESTONE_DATE_ATTR = "commitDate";
    private static final String MILESTONE_HIDDEN_ATTR = "hidden";
    private static final String MILESTONE_DEFERRED_ATTR = "deferred";
    private static final String COLOR_ATTR = "color";
    private static final String URL_ATTR = "url";
    private static final String NO_LABELS_VAL = "none";
    private static final String ATTR_ID_ATTR = "atid";
    private static final String INHERITS_ATTR = "inherits";
    private static final String DEP_SRC_ATTR = "source";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String SYNC_PHASE_NAME_ATTR = "syncPhaseName";
    private static final String PHASE_TYPE_ATTR = "phaseType";
    private static final String EFFECTIVE_PHASE_ATTR = "effectivePhase";
    private static final String TIME_ATTR = "time";
    private static final String SYNC_TIME_ATTR = "syncTime";
    private static final String DEFERRED_TIME_ATTR = "deferredTime";
    private static final String UNITS_ATTR = "sizeUnits";
    private static final String INSP_UNITS_ATTR = "inspUnits";
    private static final String INSP_SIZE_ATTR = "inspSize";
    private static final String VERSION_ATTR = "dumpFileVersion";
    private static final String SAVE_DATE_ATTR = "dumpTimestamp";
    private static final String AUTHOR_ATTR = "author";
    private static final String TIMESTAMP_ATTR = "timestamp";
    private static final String FORMAT_ATTR = "format";

    private static final DateFormat CALENDAR_DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");

    /** A list of phase types for quality phases */
    private static final List QUALITY_PHASE_TYPES = Arrays.asList(new String[] {
            "REQINSP", "HLDRINSP", "CR", "CODEINSP", "DLDINSP", "DLDR",
            "APPRAISAL", "REVIEW", "INSP" });

}
