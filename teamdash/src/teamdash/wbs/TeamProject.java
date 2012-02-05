// Copyright (C) 2002-2012 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMemberList;

public class TeamProject implements WBSFilenameConstants {

    private Element projectSettings;
    private String projectName;
    private String projectID;
    private File directory;
    private TeamMemberList teamList;
    private Element teamProcessXml;
    private TeamProcess teamProcess;
    private WBSModel wbs;
    private WBSModel workflows;
    private MilestonesWBSModel milestones;
    private long fileModTime;
    private String masterProjectID;
    private ImportDirectory masterProjectDirectory;
    private boolean readOnly;
    private boolean filesAreReadOnly;
    private Properties userSettings;
    private ImportDirectory importDirectory;


    /** Create or open a team project */
    public TeamProject(File directory, String projectName) {
        this.projectName = projectName;
        this.directory = directory;
        this.readOnly = false;
        reload();
    }

    /** Discard all data structures and reload them from the filesystem.
     * 
     * Upon return from this call, methods like {@link #getWBS()},
     * {@link #getTeamMemberList()}, and {@link #getWorkflows()} will
     * return <b>new objects</b> (not just updated objects).
     */
    public void reload() {
        fileModTime = 0;
        filesAreReadOnly = false;
        openProjectSettings();
        openTeamList();
        openTeamProcess();
        openWorkflows();
        openMilestones();
        openWBS();
    }

    /** Check to see if any files have changed since we opened them.  If so,
     * reload all files.
     * 
     * @return true if files were reloaded, false if no changes were made.
     */
    public boolean maybeReload() {
        refreshImportDirectory();
        for (int i = 0; i < ALL_FILENAMES.length; i++) {
            File f = new File(directory, ALL_FILENAMES[i]);
            if (f.isFile() && f.lastModified() > fileModTime) {
                reload();
                return true;
            }
        }

        return false;
    }

    /** Save the team project */
    public boolean save() {
        if (readOnly)
            return true;
        else
            return saveTo(directory);
    }

    /** Save a copy of all project files to an additional, external directory */
    public boolean saveCopy(File copyDirectory) {
        // save a copy of all regular, commonly edited project files.
        boolean result = saveTo(copyDirectory);
        // in addition to those project files, save a copy of the settings
        // and process data that provide context for the project.
        result = saveProjectSettings(copyDirectory) && result;
        result = saveUserSettings(copyDirectory) && result;
        result = saveTeamProcessXml(copyDirectory) && result;
        return result;
    }

    private boolean saveTo(File directory) {
        boolean result = true;
        result = saveTeamList(directory) && result;
        result = saveWBS(directory) && result;
        result = saveWorkflows(directory) && result;
        result = saveMilestones(directory) && result;
        return result;
    }

    /** Return the name of the project */
    public String getProjectName() {
        return projectName;
    }

    /** Return the ID of the project */
    public String getProjectID() {
        return projectID;
    }

    /** Get the list of team members on this project */
    public TeamMemberList getTeamMemberList() {
        return teamList;
    }

    /** Get the team process being used for this project */
    public TeamProcess getTeamProcess() {
        return teamProcess;
    }

    /** Get the work breakdown structure for this project */
    public WBSModel getWBS() {
        return wbs;
    }

    /** Get the common workflows for this project */
    public WBSModel getWorkflows() {
        return workflows;
    }

    public MilestonesWBSModel getMilestones() {
        return milestones;
    }

    /** If this project is part of a master project, get the directory where
     * the master project stores its files.
     * @return the master project's data directory, or null if this project is
     *     not part of a master project.
     */
    public ImportDirectory getMasterProjectDirectory() {
        return masterProjectDirectory;
    }

    /** Return true if this is a master project, false if it is a regular
     * team project.
     */
    public boolean isMasterProject() {
        if (projectSettings == null)
            return false;
        NodeList nl = projectSettings.getElementsByTagName("subproject");
        return (nl != null && nl.getLength() > 0);
    }

    /** Get the read-only status of this team project */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Set the read-only status of this team project */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (teamList != null)
            teamList.setReadOnly(readOnly);
    }

    /** Return true if some files for this team project are read-only */
    public boolean filesAreReadOnly() {
        return filesAreReadOnly;
    }

    /** Return the directory where files for this team project are stored */
    public File getStorageDirectory() {
        return directory;
    }

    /** Return the project settings */
    protected Element getProjectSettings() {
        return projectSettings;
    }

    /** Return the value of a user setting */
    public String getUserSetting(String name) {
        return userSettings.getProperty(name);
    }

    /** Return the value of a boolean user setting */
    public boolean getBoolUserSetting(String name) {
        return "true".equals(getUserSetting(name));
    }

    public void putUserSetting(String name, String value) {
        userSettings.put(name, value);
        if (!readOnly)
            saveUserSettings(directory);
    }

    public void putUserSetting(String name, boolean value) {
        putUserSetting(name, Boolean.toString(value));
    }

    public void setImportDirectory(ImportDirectory importDirectory) {
        this.importDirectory = importDirectory;
    }

    protected void refreshImportDirectory() {
        if (importDirectory != null) {
            try {
                importDirectory.update();
                directory = importDirectory.getDirectory();
            } catch (IOException ioe) {
                ;
            }
        }
    }

    /** Open and parse an XML file. @return null on error. */
    private Element openXML(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            Document doc = XMLUtils.parse(new BufferedInputStream(in));
            fileModTime = Math.max(fileModTime, file.lastModified());
            return doc.getDocumentElement();
        } catch (Exception e) {
            return null;
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    /** Save an XML file.  Return false on error. */
    private boolean saveXML(Element xml, File dir, String filename) {
        try {
            File file = new File(dir, filename);
            BufferedWriter out = new BufferedWriter(
                new RobustFileWriter(file, "utf-8"));
            out.write(XMLUtils.getAsText(xml));
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Make a note of whether a file is editable */
    private File checkEditable(File file) {
        if (file.exists() && !file.canWrite())
            filesAreReadOnly = true;
        return file;
    }

    /** Open the file containing the project settings (written by the team
     * project setup wizard) */
    protected void openProjectSettings() {
        Properties defaultUserSettings = new Properties();
        loadProperties(defaultUserSettings, TeamProject.class
                .getResourceAsStream("default-user-settings.txt"));
        userSettings = new Properties(defaultUserSettings);

        try {
            projectSettings = openXML(new File(directory, SETTINGS_FILENAME));
            String name = projectSettings.getAttribute("projectName");
            if (XMLUtils.hasValue(name))
                projectName = name;
            else if (XMLUtils.hasValue
                     (name = projectSettings.getAttribute("scheduleName")))
                projectName = name;

            String id = projectSettings.getAttribute("projectID");
            if (XMLUtils.hasValue(id))
                projectID = id;

            masterProjectID = null;
            NodeList nl = projectSettings.getElementsByTagName("masterProject");
            if (nl == null || nl.getLength() == 0)
                masterProjectDirectory = null;
            else {
                Element elem = (Element) nl.item(0);
                masterProjectDirectory = getProjectDataDirectory(elem, true);
                if (masterProjectDirectory != null)
                    masterProjectID = elem.getAttribute("projectID");
            }

            File userSettingsFile = new File(directory, USER_SETTINGS_FILENAME);
            if (userSettingsFile.canRead()) {
                loadProperties(userSettings, new FileInputStream(
                        userSettingsFile));
            }

        } catch (Exception e) {
            projectSettings = null;
        }
    }

    private void loadProperties(Properties p, InputStream in) {
        try {
            in = new BufferedInputStream(in);
            p.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtils.safelyClose(in);
        }
    }

    protected ImportDirectory getProjectDataDirectory(Element e, boolean checkExists) {
        // construct a list of possible data locations from the attributes
        // in this XML element
        String[] locations = new String[4];
        locations[0] = e.getAttribute("teamDataURL");
        locations[1] = getDataSubdir(e, "teamDirectoryUNC");
        locations[2] = getDataSubdir(e, "teamDirectory");
        locations[3] = "No Such Dir/data/" + e.getAttribute("projectID");

        // use this information to retrieve an object for accessing the
        // project's data directory.
        ImportDirectory result = ImportDirectoryFactory.getInstance().get(
            locations);

        if (checkExists && result != null
                && !result.getDirectory().isDirectory())
            result = null;

        return result;
    }

    private String getDataSubdir(Element e, String attrName) {
        String baseDir = e.getAttribute(attrName);
        if (!XMLUtils.hasValue(baseDir))
            return null;

        String projectID = e.getAttribute("projectID");
        if (!XMLUtils.hasValue(projectID))
            return null;

        File teamDir = new File(baseDir);
        File dataDir = new File(teamDir, "data");
        File projDir = new File(dataDir, projectID);
        return projDir.getPath();
    }

    private boolean saveProjectSettings(File externalDir) {
        if (projectSettings == null)
            return true;
        return saveXML(projectSettings, externalDir, SETTINGS_FILENAME);
    }

    private boolean saveUserSettings(File directory) {
        File userSettingsFile = new File(directory, USER_SETTINGS_FILENAME);
        try {
            RobustFileOutputStream out = new RobustFileOutputStream(
                    userSettingsFile);
            userSettings.store(out, null);
            out.close();
            return true;
        } catch (IOException ioe) {
            System.out.println("Encountered problem when saving "
                    + userSettingsFile);
            ioe.printStackTrace();
            return false;
        }
    }


    /** Open the file containing the list of team members */
    private void openTeamList() {
        try {
            File file1 = checkEditable(new File(directory, TEAM_LIST_FILENAME));
            File file2 = checkEditable(new File(directory, TEAM_LIST_FILENAME2));
            File file = (file2.exists() ? file2 : file1);
            Element xml = openXML(file);
            if (xml != null) teamList = new TeamMemberList(xml);
        } catch (Exception e) {
        }
        if (teamList == null) {
            System.out.println("No "+TEAM_LIST_FILENAME+
                               " found; creating empty team list");
            teamList = new TeamMemberList();
        }
        teamList.setReadOnly(readOnly);
    }

    /** Save the list of team members */
    private boolean saveTeamList(File directory) {
        try {
            // For now, we save the data to two different files:
            // "team.xml" and "team2.xml".  "team.xml" will be read - and
            // possibly overwritten incorrectly - by older versions of the
            // TeamTools code.  "team2.xml" will be preferred by newer
            // versions, and shouldn't get clobbered.
            File f = new File(directory, TEAM_LIST_FILENAME2);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            teamList.getAsXML(buf);
            buf.flush();
            out.close();

            f = new File(directory, TEAM_LIST_FILENAME);
            out = new RobustFileWriter(f, "UTF-8");
            buf = new BufferedWriter(out);
            teamList.getAsXML(buf);
            buf.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /** Open the team process */
    private void openTeamProcess() {
        Element xml = null;

        // first, try to open a file called "process.xml"
        try {
            xml = openXML(new File(directory, PROCESS_FILENAME));
        } catch (Exception e) { }

        // if that fails, see if a process spec has been provided in the
        // system properties.
        String specUrl = System.getProperty("teamdash.wbs.processSpecURL");
        if (specUrl != null) try {
            Document doc = XMLUtils.parse((new URL(specUrl)).openStream());
            xml = doc.getDocumentElement();
        } catch (Exception e) { }

        // if that fails, see if the project settings include a pointer to a
        // process description.
        if (xml == null && projectSettings != null) try {
            String relativeTemplateLocation =
                projectSettings.getAttribute("templatePath");
            if (XMLUtils.hasValue(relativeTemplateLocation)) {
                File templateJar =
                    new File(directory, relativeTemplateLocation);
                String jarURL = templateJar.toURL().toString();
                String xmlURL = "jar:" + jarURL + "!/settings.xml";
                Document doc = XMLUtils.parse((new URL(xmlURL)).openStream());
                xml = doc.getDocumentElement();
            }
        } catch (Exception e) { }

        // create a team process from the XML we found.  If we didn't find
        // anything, xml will equal null and a default team process will be
        // created instead.
        teamProcessXml = xml;
        teamProcess = new TeamProcess(xml);
    }

    private boolean saveTeamProcessXml(File externalDir) {
        if (teamProcessXml == null)
            return true;
        else
            return saveXML(teamProcessXml, externalDir, PROCESS_FILENAME);
    }

    /** Open the file containing the work breakdown structure */
    private void openWBS() {
        wbs = readWBS();

        if (masterProjectID != null
                && wbs.getRoot().getAttribute(MasterWBSUtil.MASTER_NODE_ID) == null)
            wbs.getRoot().setAttribute(MasterWBSUtil.MASTER_NODE_ID,
                    masterProjectID + ":000000");
    }

    protected WBSModel readWBS() {
        try {
            Element xml = openXML(checkEditable(new File(directory,
                    WBS_FILENAME)));
            if (xml != null) {
                WBSModel result = new WBSModel(xml);
                projectName = result.getRoot().getName();
                return result;
            }

        } catch (Exception e) {
        }

        System.out.println("No "+WBS_FILENAME+
                           " file found; creating default wbs");
        boolean createDefaultNode = !isMasterProject();
        WBSModel model = new WBSModel(projectName, createDefaultNode);
        setCreatedWithVersionAttribute(model);
        return model;
    }

    /** Save the work breakdown structure */
    private boolean saveWBS(File directory) {
        try {
            File f = new File(directory, WBS_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            wbs.getAsXML(buf);
            buf.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    /** Open the file containing the common workflows */
    private void openWorkflows() {
        try {
            Element xml = openXML(checkEditable(new File(directory,
                    FLOW_FILENAME)));
            if (xml != null) workflows = new WorkflowWBSModel(xml);
        } catch (Exception e) {
        }
        if (workflows == null) {
            System.out.println("No "+FLOW_FILENAME+
                               " file found; creating default workflows");
            workflows = new WorkflowWBSModel("Common Workflows");
            setCreatedWithVersionAttribute(workflows);
        }
    }

    /** Save the common workflows */
    private boolean saveWorkflows(File directory) {
        try {
            File f = new File(directory, FLOW_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            workflows.getAsXML(buf);
            buf.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    /** Open the file containing the project milestones */
    private void openMilestones() {
        try {
            Element xml = openXML(checkEditable(new File(directory,
                MILESTONES_FILENAME)));
            if (xml != null) milestones = new MilestonesWBSModel(xml);
        } catch (Exception e) {
        }
        if (milestones == null) {
            System.out.println("No "+MILESTONES_FILENAME+
                               " file found; creating default milestones");
            milestones = new MilestonesWBSModel("Project Milestones");
            setCreatedWithVersionAttribute(milestones);
        }
    }

    /** Save the project milestones */
    private boolean saveMilestones(File directory) {
        try {
            File f = new File(directory, MILESTONES_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            milestones.getAsXML(buf);
            buf.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void setCreatedWithVersionAttribute(WBSModel model) {
        try {
            String version = TeamProject.class.getPackage()
                    .getImplementationVersion();
            model.getRoot().setAttribute(WBSModel.CREATED_WITH_ATTR, version);
        } catch (Exception e) {}
    }

    private static class TeamProjectFileFilter implements FileFilter {

        private Set includedNames;

        TeamProjectFileFilter() {
            Set m = new HashSet();
            m.add(TEAM_LIST_FILENAME.toLowerCase());
            m.add(TEAM_LIST_FILENAME2.toLowerCase());
            m.add(WBS_FILENAME.toLowerCase());
            m.add(FLOW_FILENAME.toLowerCase());
            m.add(MILESTONES_FILENAME.toLowerCase());
            this.includedNames = Collections.unmodifiableSet(m);
        }

        public boolean accept(File f) {
            return includedNames.contains(f.getName().toLowerCase());
        }

    }

    /** A filter that accepts files used for storing team project data */
    public static final FileFilter FILE_FILTER = new TeamProjectFileFilter();


    /*

    // The following stub procedure was for running memory usage experiments.
    public static void main(String args[]) {
        Element el = openXML(new File("team.xml"));
        System.out.println("Press enter to start");
        try {
            byte[] buf = new byte[100];
            System.in.read(buf);
        } catch (Exception e) {}
        TeamProject p = new TeamProject(new File("."), "Team Project");
        DataTableModel m = new DataTableModel(p.getWBS(),p.getTeamMemberList(), p.getTeamProcess());
        System.out.println("Press a key to finish");
        try {
            System.in.read();
        } catch (Exception e) {}
    }
    */


    private static final String[] ALL_FILENAMES = {
        TEAM_LIST_FILENAME,
        WBS_FILENAME,
        FLOW_FILENAME,
        MILESTONES_FILENAME,
        PROCESS_FILENAME,
        SETTINGS_FILENAME
    };

}
