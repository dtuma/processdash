
package teamdash.wbs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import teamdash.RobustFileWriter;
import teamdash.XMLUtils;


public class WorkflowLibrary {

    private static final String WORKFLOW_LIBRARY_TAG = "workflowLibrary";
    private static final String PROCESS_NAME_ATTR = "processName";
    private static final String PROCESS_VERSION_ATTR = "processVersion";

    private File file;
    private String processName;
    private String processVersion;
    private WBSModel workflows;



    public WorkflowLibrary(File f) throws IOException {
        this.file = f;
        load();
    }

    public WorkflowLibrary(File f, TeamProcess process) throws IOException {
        if (f.exists())
            throw new IOException("File already exists.");

        this.file = f;
        this.processName = process.getProcessName();
        this.processVersion = process.getProcessVersion();
        this.workflows = new WorkflowWBSModel("Archived Workflows");
    }

    public String getProcessName() {
        return processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public WBSModel getWorkflows() {
        return workflows;
    }

    public boolean isValid() {
        return (processName != null && processVersion != null && workflows != null);
    }

    public boolean compatible(TeamProcess process) {
        return getProcessName().equals(process.getProcessName()) &&
            getProcessVersion().equals(process.getProcessVersion());
    }


    private void load() throws IOException {
        try {
            Document doc = XMLUtils.parse(new FileInputStream(file));
            Element xml = doc.getDocumentElement();
            if (WORKFLOW_LIBRARY_TAG.equals(xml.getTagName())) {
                processName = xml.getAttribute(PROCESS_NAME_ATTR);
                processVersion = xml.getAttribute(PROCESS_VERSION_ATTR);
                workflows = new WorkflowWBSModel((Element) xml.getFirstChild());
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to load workflow library.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void save() throws IOException {
        try {
            RobustFileWriter out = new RobustFileWriter(file, "UTF-8");
            out.write("<" + WORKFLOW_LIBRARY_TAG + " ");
            out.write(PROCESS_NAME_ATTR + "='" + XMLUtils.escapeAttribute(processName) + " ");
            out.write(PROCESS_VERSION_ATTR + "='" + XMLUtils.escapeAttribute(processVersion) + "'>\n");
            workflows.getAsXML(out);
            out.write("</" + WORKFLOW_LIBRARY_TAG + ">");
            out.close();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to save workflow library.");
            ioe.initCause(e);
            throw ioe;
        }
    }

}
