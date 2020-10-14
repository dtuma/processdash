// Copyright (C) 2002-2020 Tuma Solutions, LLC
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.quicklauncher.TeamToolsVersionManager;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.SizeDataColumn;

public class TeamProject implements WBSFilenameConstants {

    private Element projectSettings;
    private String projectName;
    private String projectID;
    private File directory;
    private TeamMemberList teamList;
    private Element teamProcessXml;
    private TeamProcess teamProcess;
    private WBSModel wbs;
    private WorkflowWBSModel workflows;
    private ProxyWBSModel proxies;
    private SizeMetricsWBSModel sizeMetrics;
    private MilestonesWBSModel milestones;
    private CustomColumnSpecs columnSpecs;
    private long fileModTime;
    private String masterProjectID;
    private ImportDirectory masterProjectDirectory;
    private boolean readOnly;
    private boolean isPrimaryProject;
    private boolean filesAreReadOnly;
    private Properties userSettings;
    private ImportDirectory importDirectory;


    /** Create or open a team project */
    public TeamProject(File directory, String projectName) {
        this(directory, projectName, false);
    }

    public TeamProject(File directory, String projectName,
            boolean isPrimaryProject) {
        this.projectName = projectName;
        this.directory = directory;
        this.readOnly = false;
        this.isPrimaryProject = isPrimaryProject;
        reload();
    }

    TeamProject(File directory, String projectName, TeamMemberList teamList,
            WBSModel wbs, WorkflowWBSModel workflows,
            SizeMetricsWBSModel sizeMetrics, ProxyWBSModel proxies,
            MilestonesWBSModel milestones, CustomColumnSpecs columnSpecs,
            Map<String, String> userSettings) {
        this.projectName = projectName;
        this.directory = directory;
        this.readOnly = false;
        this.teamList = teamList;
        this.wbs = wbs;
        this.workflows = workflows;
        this.sizeMetrics = sizeMetrics;
        this.proxies = proxies;
        this.milestones = milestones;
        this.columnSpecs = columnSpecs;
        this.userSettings = new Properties();
        this.userSettings.putAll(userSettings);
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
        openProxies();
        openMilestones();
        openColumns();
        openWBS();
        openSizeMetrics();
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
        result = saveTeamProcessXml(copyDirectory) && result;
        return result;
    }

    private boolean saveTo(File directory) {
        boolean result = true;
        result = saveUserSettings(directory) && result;
        result = saveTeamList(directory) && result;
        result = saveWBS(directory) && result;
        result = saveWorkflows(directory) && result;
        result = saveProxies(directory) && result;
        result = saveSizeMetrics(directory) && result;
        result = saveMilestones(directory) && result;
        result = saveColumns(directory) && result;
        return result;
    }

    /** Return the name of the project */
    public String getProjectName() {
        return projectName;
    }

    protected void setProjectName(String projectName) {
        this.projectName = projectName;
        if (wbs != null)
            wbs.getRoot().setName(WBSClipSelection.scrubName(projectName));
    }

    /** Return the ID of the project */
    public String getProjectID() {
        return projectID;
    }

    /** Return the dataset ID of the team dashboard that owns this project */
    public String getDatasetID() {
        String result = null;
        if (projectSettings != null)
            result = projectSettings.getAttribute("datasetID");
        return (XMLUtils.hasValue(result) ? result : null);
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
    public WorkflowWBSModel getWorkflows() {
        return workflows;
    }

    /** Get the size estimating proxies for this project */
    public ProxyWBSModel getProxies() {
        return proxies;
    }

    /** Get the dynamic size metrics for this project */
    public SizeMetricsWBSModel getSizeMetrics() {
        return sizeMetrics;
    }

    /** Get the milestones for this project */
    public MilestonesWBSModel getMilestones() {
        return milestones;
    }

    /** Get the custom columns for this project */
    public CustomColumnSpecs getColumns() {
        return columnSpecs;
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

    /** Return true if this is a personal project, false otherwise.
     */
    public boolean isPersonalProject() {
        if (projectSettings == null)
            return false;
        else
            return "true".equals(projectSettings.getAttribute("personal"));
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

    /** Return the map of user settings */
    Properties getUserSettings() {
        return userSettings;
    }

    /** Return the value of a user setting */
    public String getUserSetting(String name) {
        return userSettings.getProperty(name);
    }

    /** Return the value of a boolean user setting */
    public boolean getBoolUserSetting(String name) {
        return getBoolUserSetting(name, false);
    }

    /** Return the value of a boolean user setting */
    public boolean getBoolUserSetting(String name, boolean defaultValue) {
        String setting = getUserSetting(name);
        if (setting == null || setting.length() == 0)
            return defaultValue;
        else
            return "true".equals(setting);
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

    /** Save a WBSModel. Return false on error. */
    private boolean saveXML(WBSModel model, File directory, String filename) {
        try {
            File f = new File(directory, filename);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            model.getAsXML(buf);
            buf.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
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

        } catch (Exception e) {
            projectSettings = null;
        }

        try {
            File userSettingsFile = new File(directory, USER_SETTINGS_FILENAME);
            if (userSettingsFile.canRead()) {
                loadProperties(userSettings, new FileInputStream(
                        userSettingsFile));
                if (projectSettings == null)
                    projectID = getUserSetting("projectID");
            }
        } catch (Exception e) {
        }
        requireVersion(TeamToolsVersionManager.DATA_VERSION, "3");
        requireVersion(TeamToolsVersionManager.WBS_EDITOR_VERSION_REQUIREMENT,
            "6.0a");

        // if we are loading the primary team project (the one which will be
        // displayed in all the WBS Editor windows), and it is a personal
        // project, optimize all labels/messages for personal WBS usage.
        if ((isPrimaryProject && isPersonalProject())
                || Boolean.getBoolean("teamdash.wbs.testPersonalResourceOverride"))
            Resources.registerDashBundleOverride("WBSEditor", "WBSEditorPersonal");
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

    private void requireVersion(String attr, String minVersion) {
        String currVersion = userSettings.getProperty(attr);
        if (currVersion == null
                || VersionUtils.compareVersions(currVersion, minVersion) < 0)
            userSettings.put(attr, minVersion);
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
        File srcFile = new File(directory, SETTINGS_FILENAME);
        if (!srcFile.isFile())
            return true;

        try {
            File destFile = new File(externalDir, SETTINGS_FILENAME);
            FileUtils.copyFile(srcFile, destFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        teamList.setSinglePersonTeam(isPersonalProject());
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
                String jarURL = templateJar.toURI().toURL().toString();
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
        InputStream in = null;
        try {
            File file = checkEditable(new File(directory, WBS_FILENAME));
            in = new BufferedInputStream(new FileInputStream(file));
            InputSource src = new InputSource(in);
            src.setEncoding("UTF-8");

            SAXParser p = SAXParserFactory.newInstance().newSAXParser();
            WBSModel result = new WBSModel(p, src);
            if (projectSettings == null)
                projectName = result.getRoot().getName();
            else
                result.getRoot().setName(projectName);
            fileModTime = Math.max(fileModTime, file.lastModified());
            return result;

        } catch (FileNotFoundException fnfe) {
        } catch (Exception e) {
            System.out.println("Unable to read " + WBS_FILENAME);
            e.printStackTrace();
        } finally {
            FileUtils.safelyClose(in);
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
        return saveXML(wbs, directory, WBS_FILENAME);
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
            String firstPhaseTaskType = teamProcess.getPhases().get(0)
                    + TeamProcess.TASK_SUFFIX;
            workflows.getNodeForPos(1).setType(firstPhaseTaskType);
            setCreatedWithVersionAttribute(workflows);
        }
    }

    /** Save the common workflows */
    private boolean saveWorkflows(File directory) {
        return saveXML(workflows, directory, FLOW_FILENAME);
    }



    /** Open the file containing the size estimation proxies */
    private void openProxies() {
        try {
            Element xml = openXML(checkEditable(new File(directory,
                    PROXY_FILENAME)));
            if (xml != null) proxies = new ProxyWBSModel(xml);
        } catch (Exception e) {
        }
        if (proxies == null) {
            System.out.println("No "+PROXY_FILENAME+
                               " file found; creating default proxies");
            proxies = new ProxyWBSModel();
            setCreatedWithVersionAttribute(proxies);
        }
    }

    /** Save the size estimation proxies */
    private boolean saveProxies(File directory) {
        return saveXML(proxies, directory, PROXY_FILENAME);
    }



    /** Open the file containing dynamic size metrics definitions */
    private void openSizeMetrics() {
        try {
            Element xml = openXML(checkEditable(new File(directory,
                SIZE_METRICS_FILENAME)));
            if (xml != null)
                sizeMetrics = new SizeMetricsWBSModel(xml);
        } catch (Exception e) {
        }
        if (sizeMetrics == null) {
            System.out.println("No " + SIZE_METRICS_FILENAME
                    + " file found; creating from process");
            sizeMetrics = new SizeMetricsWBSModel(teamProcess);
            setCreatedWithVersionAttribute(sizeMetrics);
            SizeDataColumn.renameLegacySizeDataAttrs(wbs,
                sizeMetrics.getIdToMetricMap().values());
        }
        sizeMetrics.registerProcessToUpdate(teamProcess);
    }

    /** Save the dynamic size metrics definitions */
    private boolean saveSizeMetrics(File directory) {
        return saveXML(sizeMetrics, directory, SIZE_METRICS_FILENAME);
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
        return saveXML(milestones, directory, MILESTONES_FILENAME);
    }



    /** Open the file containing the custom project columns */
    private void openColumns() {
        try {
            columnSpecs = new CustomColumnSpecs();
            Element xml = openXML(checkEditable(new File(directory,
                    COLUMNS_FILENAME)));
            if (xml != null)
                columnSpecs.load(xml, true);
        } catch (Exception e) {
        }
    }

    /** Save the project milestones */
    private boolean saveColumns(File directory) {
        try {
            File f = new File(directory, COLUMNS_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            BufferedWriter buf = new BufferedWriter(out);
            columnSpecs.getAsXML(buf);
            buf.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    public long getFileModificationTime() {
        return fileModTime;
    }

    private void setCreatedWithVersionAttribute(WBSModel model) {
        try {
            String version = TeamProject.class.getPackage()
                    .getImplementationVersion();
            model.getRoot().setAttribute(WBSModel.CREATED_WITH_ATTR, version);
        } catch (Exception e) {}
    }


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
        PROXY_FILENAME,
        SIZE_METRICS_FILENAME,
        MILESTONES_FILENAME,
        PROCESS_FILENAME,
        SETTINGS_FILENAME
    };

}
