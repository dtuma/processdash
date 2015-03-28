// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class TeamSettingsFile {

    public static class RelatedProject {
        public String shortName;
        public String projectID;
        public String teamDirectory;
        public String teamDirectoryUNC;
        public String teamDataURL;
    }


    private File projDataDir;

    private String projDataUrl;

    private String projectName;

    private String projectID;

    private String processID;

    private String templatePath;

    private String scheduleName;

    private List masterProjects;

    private List subprojects;

    private boolean isReadOnly;


    public TeamSettingsFile(String dir, String url) {
        if (StringUtils.hasValue(dir))
            this.projDataDir = new File(dir);
        if (StringUtils.hasValue(url))
            this.projDataUrl = url;
        if (this.projDataDir == null && this.projDataUrl == null)
            throw new IllegalArgumentException("No location specified");

        this.masterProjects = new LinkedList();
        this.subprojects = new LinkedList();
        this.isReadOnly = false;
    }

    public String getSettingsFileDescription() {
        if (projDataDir != null)
            return new File(projDataDir, SETTINGS_FILENAME).getPath();
        else
            return projDataUrl + "/" + SETTINGS_FILENAME;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public void setProjectHierarchyPath(String path) {
        if (path == null)
            projectName = null;
        else {
            int pos = path.lastIndexOf('/');
            if (pos != -1)
                path = path.substring(pos + 1);
            projectName = path;
        }
    }

    public List getMasterProjects() {
        return masterProjects;
    }

    public List getSubprojects() {
        return subprojects;
    }

    public void setReadOnly() {
        this.isReadOnly = true;
    }

    public void read() throws IOException {
        try {
            Element e = XMLUtils.parse(new BufferedInputStream(
                getInputStream())).getDocumentElement();

            this.projectName = getAttribute(e, PROJECT_NAME_ATTR);
            this.projectID = getAttribute(e, PROJECT_ID_ATTR);
            this.processID = getAttribute(e, PROCESS_ID_ATTR);
            this.templatePath = getAttribute(e, TEMPLATE_PATH_ATTR);
            this.scheduleName = getAttribute(e, SCHEDULE_NAME_ATTR);

            readRelatedProjects(e, MASTER_PROJECT_TAG, masterProjects);
            readRelatedProjects(e, SUBPROJECT_TAG, subprojects);

        } catch (SAXException e) {
            IOException ioe = new IOException("Could not read "
                    + getSettingsFileDescription());
            ioe.initCause(e);
            throw ioe;
        }
    }

    private InputStream getInputStream() throws IOException {
        IOException ioe = null;
        if (projDataUrl != null) {
            try {
                URL u = new URL(projDataUrl + "/" + SETTINGS_FILENAME);
                return u.openStream();
            } catch (IOException e) {
                ioe = e;
            }
        }

        if (projDataDir != null) {
            try {
                File f = new File(projDataDir, SETTINGS_FILENAME);
                return new FileInputStream(f);
            } catch (IOException e) {
                ioe = e;
            }
        }

        throw ioe;
    }

    private void readRelatedProjects(Element e, String tagName, List projects) {
        projects.clear();
        NodeList nodes = e.getElementsByTagName(tagName);
        for(int i = 0;  i < nodes.getLength();  i++)
            projects.add(readRelatedProject((Element) nodes.item(i)));
    }

    private RelatedProject readRelatedProject(Element e) {
        RelatedProject result = new RelatedProject();
        result.shortName = getAttribute(e, SHORT_NAME_ATTR);
        result.projectID = getAttribute(e, PROJECT_ID_ATTR);
        result.teamDirectory = getAttribute(e, TEAM_DIR_ATTR);
        result.teamDirectoryUNC = getAttribute(e, TEAM_DIR_UNC_ATTR);
        result.teamDataURL = getAttribute(e, TEAM_DATA_URL_ATTR);
        return result;
    }

    private String getAttribute(Element e, String attrName) {
        String result = e.getAttribute(attrName);
        if (XMLUtils.hasValue(result))
            return result;
        else
            return null;
    }


    public void write() throws IOException {
        if (isReadOnly)
            throw new IOException("Cannot save read-only file");

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Writer out = new OutputStreamWriter(buf, "UTF-8");

        // write XML header
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        // open XML tag
        out.write("<project-settings");

        // write the project attributes
        writeAttr(out, "\n    ", PROJECT_NAME_ATTR, projectName);
        writeAttr(out, "\n    ", PROJECT_ID_ATTR, projectID);
        writeAttr(out, "\n    ", PROCESS_ID_ATTR, processID);
        writeAttr(out, "\n    ", TEMPLATE_PATH_ATTR, templatePath);
        writeAttr(out, "\n    ", SCHEDULE_NAME_ATTR, scheduleName);
        out.write(">\n");

        writeRelatedProjects(out, MASTER_PROJECT_TAG, masterProjects);
        writeRelatedProjects(out, SUBPROJECT_TAG, subprojects);

        out.write("</project-settings>\n");

        out.close();

        copyToDestination(buf.toByteArray());
    }

    private void writeRelatedProjects(Writer out, String tagName, List projects) throws IOException {
        for (Iterator i = projects.iterator(); i.hasNext();) {
            RelatedProject proj = (RelatedProject) i.next();
            out.write("    <");
            out.write(tagName);
            writeAttr(out, "\n        ", SHORT_NAME_ATTR, proj.shortName);
            writeAttr(out, "\n        ", PROJECT_ID_ATTR, proj.projectID);
            writeAttr(out, "\n        ", TEAM_DIR_ATTR, proj.teamDirectory);
            writeAttr(out, "\n        ", TEAM_DIR_UNC_ATTR, proj.teamDirectoryUNC);
            writeAttr(out, "\n        ", TEAM_DATA_URL_ATTR, proj.teamDataURL);
            out.write("/>\n");
        }
    }

    private void writeAttr(Writer out, String indent, String name, String value)
            throws IOException {
        if (value != null) {
            out.write(indent);
            out.write(name);
            out.write("='");
            out.write(XMLUtils.escapeAttribute(value));
            out.write("'");
        }
    }

    private void copyToDestination(byte[] data) throws IOException {
        IOException ioe = null;

        if (projDataUrl != null) {
            try {
                ResourceBridgeClient.uploadSingleFile(new URL(projDataUrl),
                    SETTINGS_FILENAME, new ByteArrayInputStream(data));
                return;
            } catch (IOException e) {
                ioe = e;
            } catch (Exception e) {
                ioe = new IOException();
                ioe.initCause(e);
            }
        }

        if (projDataDir != null) {
            File dest = new File(projDataDir, SETTINGS_FILENAME);
            RobustFileOutputStream out = new RobustFileOutputStream(dest);
            FileUtils.copyFile(new ByteArrayInputStream(data), out);
            out.close();
            return;
        }

        throw ioe;
    }


    private static final String SETTINGS_FILENAME = "settings.xml";

    private static final String PROJECT_NAME_ATTR = "projectName";

    private static final String PROJECT_ID_ATTR = "projectID";

    private static final String PROCESS_ID_ATTR = "processID";

    private static final String TEMPLATE_PATH_ATTR = "templatePath";

    private static final String SCHEDULE_NAME_ATTR = "scheduleName";

    private static final String SHORT_NAME_ATTR = "shortName";

    private static final String TEAM_DIR_ATTR = "teamDirectory";

    private static final String TEAM_DIR_UNC_ATTR = "teamDirectoryUNC";

    private static final String TEAM_DATA_URL_ATTR = "teamDataURL";

    private static final String SUBPROJECT_TAG = "subproject";

    private static final String MASTER_PROJECT_TAG = "masterProject";
}
