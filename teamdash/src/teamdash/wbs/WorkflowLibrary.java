// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
