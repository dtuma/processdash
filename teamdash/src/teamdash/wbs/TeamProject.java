package teamdash.wbs;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pspdash.RobustFileWriter;
import pspdash.XMLUtils;
import teamdash.TeamMemberList;

public class TeamProject {

    private Element projectSettings;
    private String projectName;
    private File directory;
    private TeamMemberList teamList;
    private TeamProcess teamProcess;
    private WBSModel wbs;
    private WBSModel workflows;


    /** Create or open a team project */
    public TeamProject(File directory, String projectName) {
        this.projectName = projectName;
        this.directory = directory;
        openProjectSettings();
        openTeamList();
        openTeamProcess();
        openWorkflows();
        openWBS();
    }

    /** Save the team project */
    public void save() {
        saveTeamList();
        saveWBS();
        saveWorkflows();
    }

    /** Return the name of the project */
    public String getProjectName() {
        return projectName;
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



    /** Open and parse an XML file. @return null on error. */
    private static Element openXML(File file) {
        try {
            Document doc = XMLUtils.parse(new FileInputStream(file));
            return doc.getDocumentElement();
        } catch (Exception e) {
            return null;
        }
    }

    /** Open the file containing the project settings (written by the team
     * project setup wizard) */
    private void openProjectSettings() {
        try {
            projectSettings = openXML(new File(directory, SETTINGS_FILENAME));
            String name = projectSettings.getAttribute("projectName");
            if (XMLUtils.hasValue(name))
                projectName = name;
            else if (XMLUtils.hasValue
                     (name = projectSettings.getAttribute("scheduleName")))
                projectName = name;
        } catch (Exception e) {
            projectSettings = null;
        }
    }

    /** Open the file containing the list of team members */
    private void openTeamList() {
        try {
            Element xml = openXML(new File(directory, TEAM_LIST_FILENAME));
            if (xml != null) teamList = new TeamMemberList(xml);
        } catch (Exception e) {
        }
        if (teamList == null) {
            System.out.println("No "+TEAM_LIST_FILENAME+
                               " found; creating empty team list");
            teamList = new TeamMemberList();
        }
    }

    /** Save the list of team members */
    void saveTeamList() {
        try {
            File f = new File(directory, TEAM_LIST_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            teamList.getAsXML(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Open the team process */
    private void openTeamProcess() {
        Element xml = null;

        // first, try to open a file called "process.xml"
        try {
            xml = openXML(new File(directory, PROCESS_FILENAME));
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
        try {
            Element xml = openXML(new File(directory, WBS_FILENAME));
            if (xml != null) {
                wbs = new WBSModel(xml);
                projectName = wbs.getRoot().getName();
            }
        } catch (Exception e) {
        }
        if (wbs == null) {
            System.out.println("No "+WBS_FILENAME+
                               " file found; creating default wbs");
            wbs = new WBSModel(projectName);
        }
    }

    /** Save the work breakdown structure */
    void saveWBS() {
        try {
            File f = new File(directory, WBS_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            wbs.getAsXML(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /** Open the file containing the common workflows */
    private void openWorkflows() {
        try {
            Element xml = openXML(new File(directory, FLOW_FILENAME));
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
    void saveWorkflows() {
        try {
            File f = new File(directory, FLOW_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f, "UTF-8");
            workflows.getAsXML(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static final String TEAM_LIST_FILENAME = "team.xml";
    private static final String WBS_FILENAME = "wbs.xml";
    private static final String FLOW_FILENAME = "workflow.xml";
    private static final String PROCESS_FILENAME = "process.xml";
    private static final String SETTINGS_FILENAME = "settings.xml";

}
