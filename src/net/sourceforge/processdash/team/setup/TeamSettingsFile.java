// Copyright (C) 2002-2017 Tuma Solutions, LLC
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.security.TamperDeterrent.FileType;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;


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

    private String version;

    private Date timestamp;

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

    public String getVersion() {
        return version;
    }

    public Date getTimestamp() {
        return timestamp;
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

            this.version = getAttribute(e, VERSION_ATTR);
            this.timestamp = XMLUtils.getXMLDate(e, TIMESTAMP_ATTR);
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


    public boolean needsRefresh() {
        // if this team settings file didn't contain version or timestamp
        // attributes, it is old and needs refreshing.
        if (this.version == null || this.timestamp == null)
            return true;

        // check with each of the data writers to see if they have new data.
        for (TeamSettingsDataWriter dataWriter : DATA_WRITERS) {

            // if the format of data written by this writer has changed, give
            // it a chance to write its data in the new format.
            String oneVersion = dataWriter.getFormatVersion();
            if (VersionUtils.compareVersions(oneVersion, this.version) > 0)
                return true;

            // if the data written by this writer has changed since the file
            // was written, give it a chance to write its new data.
            Date oneTime = dataWriter.getDataTimestamp();
            if (oneTime != null && oneTime.compareTo(this.timestamp) > 0)
                return true;
        }

        return false;
    }


    public void write() throws IOException {
        if (isReadOnly)
            throw new IOException("Cannot save read-only file");

        File tmp = TempFileFactory.get().createTempFile("settings", ".tmp");
        Writer out = new OutputStreamWriter(new BufferedOutputStream( //
                new FileOutputStream(tmp)), "UTF-8");

        // write XML header
        out.write("<?xml version='1.0' encoding='UTF-8'?>" + NL);
        // open XML tag
        out.write("<project-settings");

        // write the project attributes
        String indent = NL + "    ";
        writeAttr(out, indent, PROJECT_NAME_ATTR, projectName);
        writeAttr(out, indent, PROJECT_ID_ATTR, projectID);
        writeAttr(out, indent, PROCESS_ID_ATTR, processID);
        writeAttr(out, indent, TEMPLATE_PATH_ATTR, templatePath);
        writeAttr(out, indent, SCHEDULE_NAME_ATTR, scheduleName);

        // write global, dashboard-specific attributes
        writeAttr(out, indent, VERSION_ATTR,
            TemplateLoader.getPackageVersion("pspdash"));
        writeAttr(out, indent, TIMESTAMP_ATTR,
            "@" + System.currentTimeMillis());
        writeAttr(out, indent, DATASET_ID_ATTR, DashController.getDatasetID());
        out.write(">" + NL);

        writeRelatedProjects(out, MASTER_PROJECT_TAG, masterProjects);
        writeRelatedProjects(out, SUBPROJECT_TAG, subprojects);

        if (!DATA_WRITERS.isEmpty()) {
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(out);
            for (TeamSettingsDataWriter dataWriter : DATA_WRITERS) {
                if (dataWriter.getDataTimestamp() != null) {
                    dataWriter.writeTeamSettings(projectID, xml);
                    xml.flush();
                    out.write(NL);
                }
            }
            out.write(NL);
        }

        out.write("</project-settings>" + NL);

        out.close();

        try {
            copyToDestination(tmp);
        } finally {
            tmp.delete();
        }
    }

    private void writeRelatedProjects(Writer out, String tagName, List projects) throws IOException {
        for (Iterator i = projects.iterator(); i.hasNext();) {
            RelatedProject proj = (RelatedProject) i.next();
            out.write("    <");
            out.write(tagName);
            String indent = NL + "        ";
            writeAttr(out, indent, SHORT_NAME_ATTR, proj.shortName);
            writeAttr(out, indent, PROJECT_ID_ATTR, proj.projectID);
            writeAttr(out, indent, TEAM_DIR_ATTR, proj.teamDirectory);
            writeAttr(out, indent, TEAM_DIR_UNC_ATTR, proj.teamDirectoryUNC);
            writeAttr(out, indent, TEAM_DATA_URL_ATTR, proj.teamDataURL);
            out.write("/>" + NL);
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

    private void copyToDestination(File tmp) throws IOException {
        IOException ioe = null;

        if (projDataUrl != null) {
            try {
                TamperDeterrent.getInstance().addThumbprint(tmp, tmp,
                    FileType.WBS);
                InputStream in = new BufferedInputStream(
                        new FileInputStream(tmp));
                ResourceBridgeClient.uploadSingleFile(new URL(projDataUrl),
                    SETTINGS_FILENAME, in);
                in.close();
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
            TamperDeterrent.getInstance().addThumbprint(tmp, dest,
                FileType.WBS);
            return;
        }

        throw ioe;
    }

    public static void addDataWriter(TeamSettingsDataWriter w) {
        DATA_WRITERS.add(w);
    }

    private static List<TeamSettingsDataWriter> DATA_WRITERS = Collections
            .synchronizedList(new ArrayList<TeamSettingsDataWriter>());


    private static final String SETTINGS_FILENAME = "settings.xml";

    private static final String NL = System.lineSeparator();

    private static final String PROJECT_NAME_ATTR = "projectName";

    private static final String PROJECT_ID_ATTR = "projectID";

    private static final String PROCESS_ID_ATTR = "processID";

    private static final String TEMPLATE_PATH_ATTR = "templatePath";

    private static final String SCHEDULE_NAME_ATTR = "scheduleName";

    private static final String VERSION_ATTR = "version";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String DATASET_ID_ATTR = "datasetID";

    private static final String SHORT_NAME_ATTR = "shortName";

    private static final String TEAM_DIR_ATTR = "teamDirectory";

    private static final String TEAM_DIR_UNC_ATTR = "teamDirectoryUNC";

    private static final String TEAM_DATA_URL_ATTR = "teamDataURL";

    private static final String SUBPROJECT_TAG = "subproject";

    private static final String MASTER_PROJECT_TAG = "masterProject";
}
