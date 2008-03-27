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

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import teamdash.team.TeamMemberList;

public class TeamProject {

    private Element projectSettings;
    private String projectName;
    private String projectID;
    private File directory;
    private TeamMemberList teamList;
    private TeamProcess teamProcess;
    private WBSModel wbs;
    private WBSModel workflows;
    private MilestonesWBSModel milestones;
    private long fileModTime;
    private String masterProjectID;
    private File masterProjectDirectory;
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
        boolean result = true;
        if (!readOnly) {
            result = saveTeamList() && result;
            result = saveWBS() && result;
            result = saveWorkflows() && result;
            result = saveMilestones() && result;
        }
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
    public File getMasterProjectDirectory() {
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

    public void setImportDirectory(ImportDirectory importDirectory) {
        this.importDirectory = importDirectory;
    }

    protected void refreshImportDirectory() {
        if (importDirectory != null) {
            try {
                importDirectory.update();
            } catch (IOException ioe) {
                ;
            }
        }
    }

    /** Open and parse an XML file. @return null on error. */
    private Element openXML(File file) {
        try {
            Document doc = XMLUtils.parse(new BufferedInputStream(
                    new FileInputStream(file)));
            fileModTime = Math.max(fileModTime, file.lastModified());
            return doc.getDocumentElement();
        } catch (Exception e) {
            return null;
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
        userSettings = new Properties();
        loadProperties(userSettings, TeamProject.class.getResourceAsStream(
            "default-user-settings.txt"));

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
                masterProjectDirectory = getProjectDataDirectory(
                        (Element) nl.item(0), true);
                if (masterProjectDirectory != null)
                    masterProjectID = masterProjectDirectory.getName();
            }

            File userSettingsFile = new File(directory, USER_SETTINGS_FILENAME);
            if (userSettingsFile.canRead()) {
                userSettings = new Properties(userSettings);
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
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected File getProjectDataDirectory(Element e, boolean checkExists) {
        // first, lookup the master project team directory.
        String mastTeamDirName =  e.getAttribute("teamDirectoryUNC");
        if (!XMLUtils.hasValue(mastTeamDirName))
            mastTeamDirName = e.getAttribute("teamDirectory");
        if (!XMLUtils.hasValue(mastTeamDirName))
            return null;

        // next, look up the ID of the master project.
        String mastProjectID = e.getAttribute("projectID");
        if (!XMLUtils.hasValue(mastProjectID))
            return null;

        // use this information to calculate the location of the master
        // project's data directory.
        File mastTeamDir = new File(mastTeamDirName);
        File dataDir = new File(mastTeamDir, "data");
        File mastProjDir = new File(dataDir, mastProjectID);

        if (checkExists == false || mastProjDir.isDirectory())
            return mastProjDir;
        else
            return null;
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
    boolean saveTeamList() {
        if (!readOnly)
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
        teamProcess = new TeamProcess(xml);
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
        return new WBSModel(projectName, createDefaultNode);
    }

    /** Save the work breakdown structure */
    boolean saveWBS() {
        if (!readOnly)
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
        }
    }

    /** Save the common workflows */
    boolean saveWorkflows() {
        if (!readOnly)
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
        }
    }

    /** Save the project milestones */
    boolean saveMilestones() {
        if (!readOnly)
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

    private static final String TEAM_LIST_FILENAME = "team.xml";
    private static final String TEAM_LIST_FILENAME2 = "team2.xml";
    private static final String WBS_FILENAME = "wbs.xml";
    private static final String FLOW_FILENAME = "workflow.xml";
    private static final String MILESTONES_FILENAME = "milestones.xml";
    private static final String PROCESS_FILENAME = "process.xml";
    private static final String SETTINGS_FILENAME = "settings.xml";
    private static final String USER_SETTINGS_FILENAME = "user-settings.ini";

    private static final String[] ALL_FILENAMES = {
        TEAM_LIST_FILENAME,
        WBS_FILENAME,
        FLOW_FILENAME,
        MILESTONES_FILENAME,
        PROCESS_FILENAME,
        SETTINGS_FILENAME
    };

}
