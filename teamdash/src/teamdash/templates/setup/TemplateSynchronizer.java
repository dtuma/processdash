package teamdash.templates.setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Reads the common team workflows for a team project, and creates dashboard
 * templates (for use by individuals in the Hierarchy Editor) corresponding to
 * each workflow.
 * 
 * @author Tuma
 */
public class TemplateSynchronizer {


    /** The ID of the process that this team project is using */
    private String processID;

    /** The unique ID assigned to this team project */
    private String projectID;

    /** The internal dashboard URI of the template.xml file for this process */
    private String templateUri;

    /** The file on the filesystem where workflow data was dumped */
    private File workflowFile;

    /** The file on the filesystem where we will write the new template info */
    private File destFile;

    /** A list of changes made by this object */
    private List changes;

    /** Does the caller just want to find out if anything needs changing? */
    private boolean whatIfMode = true;


    public TemplateSynchronizer(String processID, String projectID,
            String templateUri, File workflowFile, File destDir) {
        this.processID = processID;
        this.projectID = projectID;
        this.templateUri = templateUri;
        this.workflowFile = workflowFile;
        this.destFile = getDestFile(destDir);
        this.changes = Collections.EMPTY_LIST;
    }

    public void setWhatIfMode(boolean whatIf) {
        this.whatIfMode = whatIf;
    }

    public List getChanges() {
        return changes;
    }

    public void sync() {
        if (!syncIsNeeded())
            return;

        if (whatIfMode == false)
            try {
                doSync();
            } catch (Exception e) {
                e.printStackTrace();
            }

        changes = Collections
                .singletonList("Updated templates for common team workflows");
    }

    private File getDestFile(File destDir) {
        String filename = "Workflows-" + projectID + "-template.xml";
        return new File(destDir, filename);
    }

    private boolean syncIsNeeded() {
        if (!workflowFile.exists())
            // there is no workflow file, so there is nothing to sync.
            return false;

        if (!destFile.exists())
            // the output file has not been created, so it needs syncing.
            return true;

        long srcTime = workflowFile.lastModified();
        long destTime = destFile.lastModified();
        if (destTime < srcTime)
            // the input file is newer than the output file - syncing is needed
            return true;

        try {
            String processVersion = TemplateLoader.getPackageVersion(processID);
            int dotPos = processVersion.lastIndexOf('.');
            String processTimestamp = processVersion.substring(dotPos + 1);
            long processTime = Long.parseLong(processTimestamp);
            if (destTime < processTime)
                // the process itself is newer than the output file - sync
                // again to refresh the file based on latest template info
                return true;
        } catch (Exception e) {}

        return false;
    }

    private void doSync() throws Exception {
        Map templates = getTemplatesForProcess();
        List workflows = getWorkflowsForProject();
        writeTemplateXml(workflows, templates);
    }

    private Map getTemplatesForProcess() throws IOException, SAXException {
        Map result = new HashMap();

        URL u = new URL("processdash:" + templateUri);
        Document doc = XMLUtils.parse(u.openConnection().getInputStream());

        NodeList templateNodes = doc.getElementsByTagName(TEMPLATE_TAG);
        for (int i = 0; i < templateNodes.getLength(); i++) {
            Element template = (Element) templateNodes.item(i);
            String id = template.getAttribute(TEMPLATE_ID_ATTR);
            result.put(id, template);
        }

        return result;
    }

    private List getWorkflowsForProject() throws SAXException, IOException {
        Document doc = XMLUtils.parse(new FileInputStream(workflowFile));
        NodeList workflows = doc.getElementsByTagName(WORKFLOW_TAG);
        List result = new ArrayList(workflows.getLength());
        for (int i = 0; i < workflows.getLength(); i++)
            result.add(workflows.item(i));
        return result;
    }

    private Element genericSubtaskTemplate;

    private Map phaseIDs;

    private void writeTemplateXml(List workflows, Map templates)
            throws IOException {
        XmlSerializer ser = XMLUtils.getXmlSerializer(true);
        genericSubtaskTemplate = (Element) templates.get(processID
                + "/IndivEmptyNode");
        phaseIDs = HierarchySynchronizer.initPhaseIDs(processID);

        OutputStream out = new RobustFileOutputStream(destFile);
        ser.setOutput(out, ENCODING);
        ser.startDocument(ENCODING, null);
        ser.startTag(null, DOC_ROOT_ELEM);
        for (Iterator i = workflows.iterator(); i.hasNext();) {
            Element workflow = (Element) i.next();
            writeTemplate(ser, workflow);
        }

        ser.endTag(null, DOC_ROOT_ELEM);
        ser.flush();
        out.close();
    }

    private void writeTemplate(XmlSerializer ser, Element workflow)
            throws IOException {
        List children = getChildNodes(workflow);
        if (children == null || children.isEmpty())
            return;

        ser.startTag(null, TEMPLATE_TAG);

        String workflowName = workflow.getAttribute(WORKFLOW_NAME_ATTR);
        String templateName = processID + "-Common-Team-Workflow:!*!:"
                + workflowName;
        ser.attribute(null, TEMPLATE_NAME_ATTR, templateName);

        ser.attribute(null, "defineRollup", "no");
        ser.attribute(null, "href", "none");
        ser.attribute(null, "autoData", "none");
        copyAttributes(ser, genericSubtaskTemplate, COPY_TEMPLATE_ATTRS);

        for (Iterator i = children.iterator(); i.hasNext();)
            writeTemplateNode(ser, (Element) i.next());

        ser.endTag(null, TEMPLATE_TAG);
    }

    private List getChildNodes(Element workflow) {
        List result = XMLUtils.getChildElements(workflow);
        for (Iterator i = result.iterator(); i.hasNext();) {
            String tag = ((Element) i.next()).getTagName();
            if (!NODE_TYPES.contains(tag))
                i.remove();
        }
        return result;
    }

    private void writeTemplateNode(XmlSerializer ser, Element node)
            throws IOException {
        ser.startTag(null, NODE_TAG);

        String nodeName = node.getAttribute(WORKFLOW_NAME_ATTR);
        ser.attribute(null, TEMPLATE_NAME_ATTR, nodeName);

        if (PSP_TYPE.equals(node.getTagName())) {
            writePSPTask(ser);
        } else {
            copyAttributes(ser, genericSubtaskTemplate, COPY_NODE_ATTRS);

            List children = getChildNodes(node);
            if (children != null && !children.isEmpty()) {
                for (Iterator i = children.iterator(); i.hasNext();)
                    writeTemplateNode(ser, (Element) i.next());
            } else {
                String phaseName = node.getAttribute(PHASE_NAME_ATTR);
                String phaseID = (String) phaseIDs.get(phaseName);
                if (phaseID != null) {
                    ser.startTag(null, PHASE_TAG);
                    ser.attribute(null, TEMPLATE_NAME_ATTR, phaseName);
                    ser.attribute(null, TEMPLATE_ID_ATTR, phaseID);
                    ser.endTag(null, PHASE_TAG);
                }
            }
        }

        ser.endTag(null, NODE_TAG);
    }

    private void writePSPTask(XmlSerializer ser) throws IOException {
        ser.attribute(null, TEMPLATE_ID_ATTR, "PSP2.1");
        ser.attribute(null, "defectLog", "true");
        ser.attribute(null, "autoData", "none");
        ser.attribute(null, "dataFile", "psp2.1/dataFile.txt");
        ser.attribute(null, "constraints",
                "{Design Inspection(3){Code Inspection(-2)");

        for (int i = 0; i < PSP_PHASES.length; i++) {
            ser.startTag(null, PHASE_TAG);
            ser.attribute(null, TEMPLATE_NAME_ATTR, PSP_PHASES[i]);
            ser.endTag(null, PHASE_TAG);
        }
    }

    private void copyAttributes(XmlSerializer ser, Element srcTemplate,
            String[] attrName) throws IOException {
        for (int i = 0; i < attrName.length; i++)
            copyAttribute(ser, srcTemplate, attrName[i]);
    }


    private void copyAttribute(XmlSerializer ser, Element srcTemplate,
            String attrName) throws IOException {
        String value = srcTemplate.getAttribute(attrName);
        if (value != null && value.length() > 0)
            ser.attribute(null, attrName, value);
    }


    private static final String ENCODING = "UTF-8";

    private static final String WORKFLOW_TAG = "workflow";

    private static final String WORKFLOW_NAME_ATTR = "name";

    private static final List NODE_TYPES = HierarchySynchronizer.NODE_TYPES;

    private static final String PSP_TYPE = HierarchySynchronizer.PSP_TYPE;

    private static final String[] PSP_PHASES = HierarchySynchronizer.PSP_PHASES;


    private static final String PHASE_NAME_ATTR = "phaseName";

    private static final String DOC_ROOT_ELEM = "dashboard-process-template";

    private static final String TEMPLATE_TAG = "template";

    private static final String TEMPLATE_NAME_ATTR = "name";

    private static final String TEMPLATE_ID_ATTR = "ID";

    private static final String NODE_TAG = "node";

    private static final String PHASE_TAG = "phase";



    private static final String[] COPY_TEMPLATE_ATTRS = { TEMPLATE_ID_ATTR,
            "defectLog", "dataFile", "size", "constraints" };

    private static final String[] COPY_NODE_ATTRS = { TEMPLATE_ID_ATTR,
            "defectLog", "dataFile" };

}
