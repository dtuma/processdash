package teamdash.wbs;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.util.RobustFileWriter;
import teamdash.XMLUtils;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.DirectSizeTypeColumn;
import teamdash.wbs.columns.SizeAccountingColumnSet;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TeamTimeColumn;


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
    /** The list of column numbers for each team member time column */
    private IntList teamMemberColumns;
    /** The initials for each team member */
    private String[] initials;
    /** Node attribute names for retrieving sync time information */
    private String[] syncAttrs;
    /** Node attribute names flagging whether a user is assigned with 0 hours */
    private String[] zeroAssignmentAttrs;
    /** The list of column numbers for each top-level size accounting column */
    private int[] sizeAccountingColumns = new int[SIZE_COLUMN_IDS.length];
    /** This column number of the direct size units column */
    private int directSizeUnitsColumn;
    /** The column number of the task labels column */
    private int labelsColumn;
    /** The column number of the task dependencies column */
    private int dependencyColumn;
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
                         TeamMemberList teamList) {
        this.wbsModel = wbsModel;
        this.dataModel = dataModel;
        this.process = process;
        this.projectID = projectID;
        this.teamList = teamList;

        if (dataModel != null) {
            for (int i = 0;   i < SIZE_COLUMN_IDS.length;   i++)
                sizeAccountingColumns[i] =
                    dataModel.findColumn(SIZE_COLUMN_IDS[i]);
            directSizeUnitsColumn =
                dataModel.findColumn(DirectSizeTypeColumn.COLUMN_ID);
            labelsColumn =
                dataModel.findColumn(TaskLabelColumn.COLUMN_ID);
            dependencyColumn =
                dataModel.findColumn(TaskDependencyColumn.COLUMN_ID);
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
            teamMemberColumns = null;
            initials = syncAttrs = zeroAssignmentAttrs = null;
        } else {
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
        writeAttr(out, NAME_ATTR, node.getName());
        writeAttr(out, ID_ATTR, node.getUniqueID());
        writeAttr(out, TASK_ID_ATTR, MasterWBSUtil.getNodeIDs(node, projectID));
        writeAttr(out, LABELS_ATTR, getLabelSaveString(node));

        // write attributes specific to this XML tag type
        AttributeWriter aw = (AttributeWriter) attributeWriters.get(tagName);
        if (aw != null)
            aw.writeAttributes(out, node);

        WBSNode[] children = wbsModel.getChildren(node);
        TaskDependencyList dependencies = null;
        if (dataModel != null)
            dependencies = (TaskDependencyList) WrappedValue.unwrap(dataModel
                    .getValueAt(node, dependencyColumn));

        if ((children == null || children.length == 0)
                && (dependencies == null || dependencies.isEmpty())
                && (depth > 0)) {
            // if this node has no children and no dependencies, just close
            // the XML tag.
            out.write("/>\n");
        } else {
            // if this node has children, print them recursively.
            out.write(">\n");
            if (depth == 0)
                writeTeamMembers(out);
            writeDependencies(out, dependencies, depth+1);
            if (children != null)
                for (int i = 0;   i < children.length;   i++)
                    write(out, children[i], depth+1);
            writeIndent(out, depth);
            out.write("</" + tagName + ">\n");
        }
    }



    private String getLabelSaveString(WBSNode node) {
        if (dataModel == null)
            return null;

        String result = (String) WrappedValue.unwrap(dataModel.getValueAt(node,
                labelsColumn));

        if (result != null)
            result = result.replaceAll(", ", ",");
        else
            result = NO_LABELS_VAL;

        return result;
    }



    private void writeTeamMembers(Writer out) throws IOException {
        if (teamList == null)
            return;

        List members = teamList.getTeamMembers();
        for (Iterator i = members.iterator(); i.hasNext();) {
            TeamMember t = (TeamMember) i.next();
            if (t != null && !t.isEmpty())
                t.getAsXML(out, true);
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



    /** Determine which XML tag should be used to represent the given node.
     */
    private String getTagNameForNode(WBSNode node) {
        String type = wbsModel.filterNodeType(node);
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
        private void maybeWriteSizeAttrs(Writer out, WBSNode node)
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
            for (int i = 0;   i < SIZE_ACCOUNTING_ATTRS.length;   i++){
                Object size =
                    dataModel.getValueAt(node, sizeAccountingColumns[i]);
                writeAttr(out, SIZE_ACCOUNTING_ATTRS[i], formatNumber(size));
            }
        }
    }



    /** AttributeWriter which writes information about a non-PSP leaf task.
     */
    private class TaskAttributeWriter implements AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            String nodeType = node.getType();
            if (!nodeType.endsWith(" Task"))
                // should I throw some sort of error?
                return;

            String phaseName = nodeType.substring(0, nodeType.length()-5);
            if (wbsModel.getChildCount(node) != 0) {
                // only write an "effective phase" attribute for non-leaf tasks.
                writeAttr(out, EFFECTIVE_PHASE_ATTR, phaseName);
                return;
            }

            String phaseType = process.getPhaseType(phaseName);
            writeAttr(out, PHASE_NAME_ATTR, phaseName);
            writeAttr(out, PHASE_TYPE_ATTR, phaseType);
            writeAttr(out, TIME_ATTR, getTeamMemberTimes(node));
            writeAttr(out, SYNC_TIME_ATTR, getTeamMemberSyncTimes(node));
            maybeWriteQualitySize(out, phaseName, phaseType, node);
        }

        private void maybeWriteQualitySize(Writer out, String phase,
                                           String phaseType, WBSNode node)
            throws IOException
        {
            if (dataModel == null) return;
            if (!QUALITY_PHASE_TYPES.contains(phaseType)) return;

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
            writeAttr(out, SYNC_TIME_ATTR, getTeamMemberSyncTimes(node));
            super.writeAttributes(out, node);
        }
    }



    /** Build an XML attribute value describing the time each team member plans
     * to spend in the given node.
     */
    private String getTeamMemberTimes(WBSNode node) {
        if (teamMemberColumns == null)
            return null;

        StringBuffer result = new StringBuffer();
        for (int i = 0;   i < teamMemberColumns.size();   i++) {
            int col = teamMemberColumns.get(i);
            String time = formatNumber(dataModel.getValueAt(node, col));
            if ("null".equals(time) || "0".equals(time) || "0.0".equals(time)) {
                if (node.getAttribute(zeroAssignmentAttrs[i]) == null)
                    continue;
            }
            result.append(",").append(initials[i]).append("=").append(time);
        }

        if (result.length() == 0)
            return null;

        result.append(",");
        return result.toString();
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
    private static final String DEPENDENCY_TAG = "dependency";

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
    private static final String LABELS_ATTR = "labels";
    private static final String NO_LABELS_VAL = "none";
    private static final String DEP_SRC_ATTR = "source";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String PHASE_TYPE_ATTR = "phaseType";
    private static final String EFFECTIVE_PHASE_ATTR = "effectivePhase";
    private static final String TIME_ATTR = "time";
    private static final String SYNC_TIME_ATTR = "syncTime";
    private static final String UNITS_ATTR = "sizeUnits";
    private static final String INSP_UNITS_ATTR = "inspUnits";
    private static final String INSP_SIZE_ATTR = "inspSize";
    private static final String VERSION_ATTR = "dumpFileVersion";

    /** A list of phase types for quality phases */
    private static final List QUALITY_PHASE_TYPES = Arrays.asList(new String[] {
            "REQINSP", "HLDRINSP", "CR", "CODEINSP", "DLDINSP", "DLDR" } );

}
