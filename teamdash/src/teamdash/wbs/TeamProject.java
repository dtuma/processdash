package teamdash;

import java.io.File;
import java.io.FileInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pspdash.RobustFileWriter;
import pspdash.XMLUtils;
import teamdash.wbs.WBSModel;

public class TeamProject {

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
    private Element openXML(File file) {
        try {
            Document doc = XMLUtils.parse(new FileInputStream(file));
            return doc.getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Open the file containing the list of team members */
    private void openTeamList() {
        try {
            Element xml = openXML(new File(directory, TEAM_LIST_FILENAME));
            if (xml != null) teamList = new TeamMemberList(xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (teamList == null)
            teamList = new TeamMemberList();
    }

    /** Save the list of team members */
    private void saveTeamList() {
        try {
            File f = new File(directory, TEAM_LIST_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f);
            teamList.getAsXML(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Open the team process */
    private void openTeamProcess() {
        // TODO: locate and pass in appropriate process defn
        teamProcess = new TeamProcess();
    }

    /** Open the file containing the work breakdown structure */
    private void openWBS() {
        try {
            Element xml = openXML(new File(directory, WBS_FILENAME));
            if (xml != null) wbs = new WBSModel(xml);
            projectName = wbs.getRoot().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (wbs == null)
            wbs = new WBSModel(projectName);
    }

    /** Save the work breakdown structure */
    private void saveWBS() {
        try {
            File f = new File(directory, WBS_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f);
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
            if (xml != null) workflows = new WBSModel(xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (workflows == null)
            workflows = new WBSModel("Common Workflows");
    }

    /** Save the common workflows */
    private void saveWorkflows() {
        try {
            File f = new File(directory, FLOW_FILENAME);
            RobustFileWriter out = new RobustFileWriter(f);
            workflows.getAsXML(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static final String TEAM_LIST_FILENAME = "team.xml";
    private static final String WBS_FILENAME = "wbs.xml";
    private static final String FLOW_FILENAME = "workflow.xml";
}
