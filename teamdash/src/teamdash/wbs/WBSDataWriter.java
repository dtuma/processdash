package teamdash.wbs;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import pspdash.RobustFileWriter;
import pspdash.XMLUtils;

/** This class writes out an XML data file describing the work breakdown
 * structure.
 * 
 * The XML file written by WBSModel.getAsXML() is designed to capture the
 * minimum necessary data needed to recreate the WBS in the WBSEditor. This
 * file, on the other hand, is designed to be read by logic which synchronizes
 * individual dashboard instances to the WBS.  It therefore contains only the
 * data elements needed by that logic. Even more important, the format is
 * independent of the WBS implementation - it does not rely on any aspect of
 * the internal naming schemes of WBS column attributes.
 */
public class WBSDataWriter {

    private WBSModel wbsModel;
    private DataTableModel dataModel;
    private TeamProcess process;
    private IntList teamMemberColumns;

    public WBSDataWriter(WBSModel wbsModel, DataTableModel dataModel,
                         TeamProcess process) {
        this.wbsModel = wbsModel;
        this.dataModel = dataModel;
        this.process = process;
    }

    public void write(File f) throws IOException {
        RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
        write(out);
        out.close();
    }

    public void write(Writer out) throws IOException {
        // initialize
        teamMemberColumns = dataModel.getTeamMemberColumnIDs();

        // write XML header
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        // write the node data
        write(out, wbsModel.getRoot(), 0);
    }

    private void write(Writer out, WBSNode node, int depth)
        throws IOException {

        String nodeType = translateType(node.getType());

        writeIndent(out, depth);

        out.write("<" + nodeType);
        writeAttr(out, NAME_ATTR, node.getName());
        writeAttr(out, ID_ATTR, node.getUniqueID());

        // write node specific attributes
        AttributeWriter w = (AttributeWriter) ATTRIBUTE_WRITERS.get(nodeType);
        if (w != null)
            w.writeAttributes(out, node);

        // todo: write node specific data.

        WBSNode[] children = wbsModel.getChildren(node);
        if (children == null || children.length == 0) {
            out.write("/>\n");
        } else {
            out.write(">\n");
            for (int i = 0;   i < children.length;   i++)
                write(out, children[i], depth+1);
            writeIndent(out, depth);
            out.write("</" + nodeType + ">\n");
        }
    }

    private String translateType(String type) {
        if ("Project".equals(type))
            return PROJECT_TYPE;
        if ("Software Component".equals(type))
            return SOFTWARE_TYPE;
        if (type.endsWith(" Document"))
            return DOCUMENT_TYPE;
        if ("PSP Task".equals(type))
            return PSP_TYPE;
        if (type.endsWith(" Task"))
            return TASK_TYPE;

        // default value (better than nothing)
        return TASK_TYPE;
    }



    private void writeIndent(Writer out, int depth) throws IOException {
        for (int i = 0; i < depth; i++)
            out.write("\t");
    }

    private void writeAttr(Writer out, String name, int value)
        throws IOException
    {
        writeAttr(out, name, Integer.toString(value));
    }
    private void writeAttr(Writer out, String name, String value)
        throws IOException
    {
        if (value == null) return;
        out.write(" ");
        out.write(name);
        out.write("='");
        out.write(XMLUtils.escapeAttribute(value));
        out.write("'");
    }


    private static final String NAME_ATTR = "name";
    private static final String ID_ATTR = "id";
    //private static final String TYPE_ATTR = "type";
    private static final String PHASE_NAME_ATTR = "phaseName";
    private static final String PHASE_TYPE_ATTR = "phaseType";
    private static final String TIME_ATTR = "time";


    private static final String PROJECT_TYPE = "project";
    private static final String SOFTWARE_TYPE = "component";
    private static final String DOCUMENT_TYPE = "document";
    private static final String PSP_TYPE = "psp";
    private static final String TASK_TYPE = "task";


    private final HashMap ATTRIBUTE_WRITERS = buildAttributeWriters();


    private interface AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node)
            throws IOException;
    }

    private class TaskAttributeWriter implements AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            String nodeType = node.getType();
            if (!nodeType.endsWith(" Task"))
                // should I throw some sort of error?
                return;

            String phaseName = nodeType.substring(0, nodeType.length()-5);
            String phaseType = process.getPhaseType(phaseName);
            writeAttr(out, PHASE_NAME_ATTR, phaseName);
            writeAttr(out, PHASE_TYPE_ATTR, phaseType);
            writeAttr(out, TIME_ATTR, getTeamMemberTimes(node));
        }
    }

    private class PSPTaskAttributeWriter implements AttributeWriter {
        public void writeAttributes(Writer out, WBSNode node) throws IOException {
            writeAttr(out, TIME_ATTR, getTeamMemberTimes(node));
        }
    }


    private HashMap buildAttributeWriters() {
        HashMap result = new HashMap();
        result.put(TASK_TYPE, new TaskAttributeWriter());
        result.put(PSP_TYPE, new PSPTaskAttributeWriter());
        return result;
    }

    private String getTeamMemberTimes(WBSNode node) {
        StringBuffer result = new StringBuffer();
        for (int i = 0;   i < teamMemberColumns.size();   i++) {
            int col = teamMemberColumns.get(i);
            String time = String.valueOf(dataModel.getValueAt(node, col));
            if ("null".equals(time) || "0".equals(time) || "0.0".equals(time))
                continue;

            result.append(",").append(dataModel.getColumnName(col))
                .append("=").append(time);
        }

        if (result.length() == 0)
            return null;

        result.append(",");
        return result.toString();
    }

}
