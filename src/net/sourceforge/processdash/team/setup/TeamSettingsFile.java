// Copyright (C) 2002-2021 Tuma Solutions, LLC
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.security.TamperDeterrent.FileType;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;


public class TeamSettingsFile {

    public static class RelatedProject {
        public String shortName;
        public String projectID;
        public String teamDirectory;
        public String teamDirectoryUNC;
        public String teamDataURL;
    }


    private String location;

    private ImportDirectory projDataDir;

    private String version;

    private Date timestamp;

    private String projectName;

    private String projectID;

    private String processID;

    private String templatePath;

    private String scheduleName;

    private String datasetID;

    private String datasetUrl;

    private boolean isPersonal;

    /** @since 2.5.6 */
    private Map<String, String> extraAttributes;

    private List masterProjects;

    private List subprojects;

    private boolean isReadOnly;


    public TeamSettingsFile(String dir, String url) {
        this.location = (StringUtils.hasValue(url) ? url : dir);
        if (!StringUtils.hasValue(location))
            throw new IllegalArgumentException("No location specified");
        this.projDataDir = ImportDirectoryFactory.getInstance().get(location);

        this.datasetID = null;
        this.datasetUrl = DATASET_URL;
        this.isPersonal = false;
        this.masterProjects = new LinkedList();
        this.subprojects = new LinkedList();
        this.isReadOnly = false;

        maybeForceReadOnly(location);
    }

    private void maybeForceReadOnly(String location) {
        // if we are running from a data backup, but this particular
        // settings.xml file is located on the network, mark it as read-only.
        if (CompressedInstanceLauncher.isRunningFromCompressedData()) {
            // ask the external mapper about a redirection for this path
            String remapped = ExternalResourceManager.getInstance()
                    .remapFilename(location);

            // if the path wasn't remapped to an embedded location within the
            // ZIP, it must be a file on the network. Mark this object read-only
            // so there is no risk of modifying real project settings.
            if (location.equals(remapped))
                setReadOnly();
        }
    }

    public String getSettingsFileDescription() {
        return location + (location.indexOf('\\') > 0 ? '\\' : '/')
                + SETTINGS_FILENAME;
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

    /**
     * Update the project name if necessary to agree with the provided path.
     * 
     * @param path
     *            the path to the project root
     * @return true if the project name was changed
     */
    public boolean maybeUpdateProjectNameFromPath(String path) {
        if (path == null)
            return false;
        String oldName = projectName;
        setProjectHierarchyPath(path);
        return !projectName.equals(oldName);
    }

    public String getDatasetID() {
        return datasetID;
    }

    public boolean isDatasetMatch() {
        if (datasetID == null || FORCED_DATASET_ID != null || isPersonal)
            return true;
        else
            return datasetID.equals(DashController.getDatasetID());
    }

    public String getDatasetUrl() {
        return datasetUrl;
    }

    public boolean isPersonal() {
        return isPersonal;
    }

    public void setPersonal(boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    public List getMasterProjects() {
        return masterProjects;
    }

    public List getSubprojects() {
        return subprojects;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly() {
        this.isReadOnly = true;
    }

    public void read() throws IOException {
        try {
            Element e = XMLUtils.parse(new BufferedInputStream(
                getInputStream())).getDocumentElement();

            Map<String, String> attrs = XMLUtils.getAttributesAsMap(e);
            this.version = attrs.remove(VERSION_ATTR);
            this.timestamp = XMLUtils.getXMLDate(e, TIMESTAMP_ATTR);
            this.projectName = attrs.remove(PROJECT_NAME_ATTR);
            this.projectID = attrs.remove(PROJECT_ID_ATTR);
            this.processID = attrs.remove(PROCESS_ID_ATTR);
            this.templatePath = attrs.remove(TEMPLATE_PATH_ATTR);
            this.scheduleName = attrs.remove(SCHEDULE_NAME_ATTR);
            this.isPersonal = "true".equals(attrs.remove(PERSONAL_ATTR));
            this.datasetID = attrs.remove(DATASET_ID_ATTR);
            this.datasetUrl = attrs.remove(DATASET_URL_ATTR);

            attrs.remove(TIMESTAMP_ATTR);
            this.extraAttributes = attrs;

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
        if (projDataDir == null)
            throw new FileNotFoundException(getSettingsFileDescription());

        projDataDir.validate();
        File f = new File(projDataDir.getDirectory(), SETTINGS_FILENAME);
        return new FileInputStream(f);
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
            Date oneTime = dataWriter.getDataTimestamp(this.projectID);
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
        if (isPersonal)
            writeAttr(out, indent, PERSONAL_ATTR, "true");

        // write any extra attributes that were present in the original file,
        // which we didn't recognize during our parsing (since 2.5.6)
        if (extraAttributes != null && !extraAttributes.isEmpty()) {
            for (Entry<String, String> e : extraAttributes.entrySet())
                writeAttr(out, indent, e.getKey(), e.getValue());
        }

        // write global, dashboard-specific attributes
        writeAttr(out, indent, VERSION_ATTR,
            TemplateLoader.getPackageVersion("pspdash"));
        writeAttr(out, indent, TIMESTAMP_ATTR,
            "@" + System.currentTimeMillis());
        if (FORCED_DATASET_ID != null)
            datasetID = FORCED_DATASET_ID;
        else if (isPersonal || datasetID == null)
            datasetID = DashController.getDatasetID();
        writeAttr(out, indent, DATASET_ID_ATTR, datasetID);
        if (DATASET_URL != null)
            datasetUrl = DATASET_URL;
        writeAttr(out, indent, DATASET_URL_ATTR, datasetUrl);
        out.write(">" + NL);

        writeRelatedProjects(out, MASTER_PROJECT_TAG, masterProjects);
        writeRelatedProjects(out, SUBPROJECT_TAG, subprojects);

        if (!DATA_WRITERS.isEmpty()) {
            XmlSerializer xml = XMLUtils.getXmlSerializer(true);
            xml.setOutput(out);
            for (TeamSettingsDataWriter dataWriter : DATA_WRITERS) {
                if (dataWriter.getDataTimestamp(projectID) != null) {
                    dataWriter.writeTeamSettings(projectID, xml);
                    xml.flush();
                    out.write(NL);
                }
            }
            out.write(NL);
        }

        out.write("</project-settings>" + NL);

        out.close();

        // sign the file for tamper deterrence
        TamperDeterrent.getInstance().addThumbprint(tmp, tmp, FileType.WBS);

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
        if (projDataDir == null)
            throw new FileNotFoundException(getSettingsFileDescription());
        projDataDir.validate();

        // save the new file to the import directory
        InputStream in = new BufferedInputStream(new FileInputStream(tmp));
        try {
            projDataDir.writeUnlockedFile(SETTINGS_FILENAME, in);
        } catch (LockFailureException lfe) {
            // can't happen - settings.xml file is unlocked
            throw new IOException(lfe);
        } finally {
            in.close();
        }
    }

    public static void addDataWriter(TeamSettingsDataWriter w) {
        PERMISSION.checkPermission();
        DATA_WRITERS.add(w);
    }

    private static List<TeamSettingsDataWriter> DATA_WRITERS = Collections
            .synchronizedList(new ArrayList<TeamSettingsDataWriter>());

    /** @since 2.5.6 */
    public static void setDatasetURL(String datasetUrl) {
        PERMISSION.checkPermission();
        DATASET_URL = datasetUrl;
    }

    private static String DATASET_URL = null;

    /** @since 2.5.6 */
    public static void setForcedDatasetID(String datasetID) {
        PERMISSION.checkPermission();
        FORCED_DATASET_ID = datasetID;
    }

    private static String FORCED_DATASET_ID = null;


    private static final DashboardPermission PERMISSION = new DashboardPermission(
            "teamSettingsFile.config");

    private static final String SETTINGS_FILENAME = "settings.xml";

    private static final String NL = System.getProperty("line.separator");

    private static final String PROJECT_NAME_ATTR = "projectName";

    private static final String PROJECT_ID_ATTR = "projectID";

    private static final String PROCESS_ID_ATTR = "processID";

    private static final String TEMPLATE_PATH_ATTR = "templatePath";

    private static final String SCHEDULE_NAME_ATTR = "scheduleName";

    private static final String PERSONAL_ATTR = "personal";

    private static final String VERSION_ATTR = "version";

    private static final String TIMESTAMP_ATTR = "timestamp";

    private static final String DATASET_ID_ATTR = "datasetID";

    private static final String DATASET_URL_ATTR = "datasetURL";

    private static final String SHORT_NAME_ATTR = "shortName";

    private static final String TEAM_DIR_ATTR = "teamDirectory";

    private static final String TEAM_DIR_UNC_ATTR = "teamDirectoryUNC";

    private static final String TEAM_DATA_URL_ATTR = "teamDataURL";

    private static final String SUBPROJECT_TAG = "subproject";

    private static final String MASTER_PROJECT_TAG = "masterProject";
}
