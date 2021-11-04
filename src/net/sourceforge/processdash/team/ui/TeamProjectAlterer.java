// Copyright (C) 2013-2021 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.team.ui;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamProjectSetupWizard;
import net.sourceforge.processdash.team.setup.TeamSettingsFile;
import net.sourceforge.processdash.team.setup.TeamStartBootstrap;
import net.sourceforge.processdash.tool.export.impl.ExportFileStream;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class TeamProjectAlterer {

    public enum ProjectType {
        Stub, Team, Master, Indiv, Personal
    }


    private DashboardContext ctx;

    private Component parent;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.TeamProject");


    public TeamProjectAlterer(DashboardContext ctx, Component parent) {
        this.ctx = ctx;
        this.parent = parent;
    }


    /** 
     * Interact with the user to rename a team/master/personal project.
     *  
     * @param projectNode
     *            the node representing the project root
     * @return the new node representing the renamed project. If the project
     *         could not be renamed, or if the user cancelled the operation,
     *         returns null.
     */ 
    public PropertyKey maybeRenameProject(PropertyKey projectNode) {
        String projectPath = projectNode.path();
        String templateID = ctx.getHierarchy().getID(projectNode);
        ProjectType projectType = getProjectType(projectPath, templateID);
        if (projectType == null)
            return null;

        // Create objects we will use in a dialog
        String title = resources.getString("Rename.Title");
        JTextField newPathField = new JTextField(projectPath);
        Object message = resources.formatStrings("Rename.Message_Header_FMT",
            projectPath);
        message = new Object[] { message, " ", newPathField,
                new JOptionPaneTweaker.GrabFocus(newPathField) };

        while (true) {
            // Show the user a dialog asking for the new path and name.
            int userChoice = JOptionPane.showConfirmDialog(parent, message,
                title, JOptionPane.OK_CANCEL_OPTION);

            // if the user didn't press the OK button, abort.
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            // get the new path in canonical form. If it was not absolute,
            // interpret it relative to the current parent of the project.
            String newPath = newPathField.getText().trim();
            if (newPath.indexOf('/') == -1)
                newPath = projectNode.getParent().path() + "/" + newPath;
            newPath = DashHierarchy.scrubPath(newPath);
            newPathField.setText(newPath);

            // if the user didn't change the name, abort.
            if (newPath.length() < 2 || newPath.equals(projectPath))
                return null;

            // check for various error conditions.
            if (projectAlreadyExists(newPath)) {
                showInvalidRenameMessage("Rename.Duplicate_Name");

            } else if (projectParentIsInvalid(newPath)) {
                showInvalidRenameMessage("Rename.Invalid_Parent");

            } else {
                try {
                    // try performing the renaming operation.
                    HierarchyAlterer hierarchyAlterer = DashController
                            .getHierarchyAlterer();
                    hierarchyAlterer.renameNode(projectPath, newPath);
                    deleteChildlessParentsOfNode(hierarchyAlterer, projectNode);

                    // possibly update the project settings.xml file
                    maybeRenameProjectInSettingsFile(projectType, newPath);

                    // possibly rename the project EV schedule
                    maybeRenameSchedule(newPath);

                    // point the "active task" at the renamed project.
                    PropertyKey newNode = ctx.getHierarchy().findExistingKey(
                        newPath);
                    return newNode;

                } catch (HierarchyAlterationException e) {
                    showHierarchyError(e);
                }
                return null;
            }
        }
    }

    private boolean projectAlreadyExists(String path) {
        PropertyKey key = ctx.getHierarchy().findExistingKey(path);
        return (key != null);
    }

    private boolean projectParentIsInvalid(String path) {
        PropertyKey key = ctx.getHierarchy().findClosestKey(path);
        while (key != null) {
            String templateID = ctx.getHierarchy().getID(key);
            if (StringUtils.hasValue(templateID))
                return true;
            key = key.getParent();
        }
        return false;
    }

    private void showInvalidRenameMessage(String resKey) {
        JOptionPane.showMessageDialog(parent, resources.getStrings(resKey),
            resources.getString("Rename.Error_Title"),
            JOptionPane.ERROR_MESSAGE);
    }

    private void maybeRenameProjectInSettingsFile(ProjectType projectType,
            String newPath) {
        // don't modify the team project settings file when an individual
        // changes the name of their joined project
        if (projectType == ProjectType.Indiv)
            return;

        try {
            // read the current settings file
            TeamSettingsFile tsf = new TeamSettingsFile(
                    getValue(newPath, TeamDataConstants.TEAM_DATA_DIRECTORY),
                    getValue(newPath, TeamDataConstants.TEAM_DATA_DIRECTORY_URL));
            tsf.read();

            // if this dataset "owns" the given file, update the project name
            if (!tsf.isReadOnly() && tsf.isDatasetMatch()) {
                tsf.setProjectHierarchyPath(newPath);
                tsf.write();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void maybeRenameSchedule(String newPath) {
        // load the EV task list for this project
        EVTaskList taskList = loadProjectTaskList(newPath);
        if (taskList == null)
            return;

        // get the new project name, and see if that schedule name is taken
        String newName = newPath.substring(newPath.lastIndexOf('/') + 1);
        if (EVTaskListData.exists(ctx.getData(), newName))
            return;
        if (EVTaskListRollup.exists(ctx.getData(), newName))
            return;

        // save the schedule with the new name, and update internal pointers
        taskList.save(newName);
        EVTaskList.getRegisteredTaskListForPath(ctx.getData(), newPath);
    }



    /**
     * Display the "close project" wizard for a team/personal project
     * 
     * @param selectedNode
     *            the node representing the project to close
     */ 
    public void maybeCloseProject(PropertyKey selectedNode) {
        String projectPath = selectedNode.path();
        String templateID = ctx.getHierarchy().getID(selectedNode);
        ProjectType type = getProjectType(projectPath, templateID);

        // do not close plain nodes (which act as folders full of projects).
        // also refuse to close team projects from an individual dashboard
        if (type == null || type == ProjectType.Indiv)
            return;

        // if this is a team project stub, delete rather than closing
        if (type == ProjectType.Stub) {
            maybeDeleteProject(selectedNode);
            return;
        }

        // extract the process ID
        int slashPos = templateID.indexOf('/');
        if (slashPos == -1)
            return;
        String processID = templateID.substring(0, slashPos);

        // open the Close Team Project page.
        StringBuilder uri = new StringBuilder();
        uri.append(HTMLUtils.urlEncodePath(projectPath)) //
                .append("//").append(processID)
                .append("/setup/wizard.class?page=close");
        Browser.launch(uri.toString());
    }



    /**
     * Interact with the user to delete a team/master/personal project.
     * 
     * @param selectedNode
     *            the project to delete
     */
    public void maybeDeleteProject(PropertyKey selectedNode) {
        String projectPath = selectedNode.path();
        String templateID = ctx.getHierarchy().getID(selectedNode);
        if (okToDeleteProject(projectPath, templateID)) {
            try {
                // if this is an indiv project, clean up the PDASH file
                maybeCleanupPdashFile(projectPath, templateID);

                // delete the import instruction for this project
                DashController.deleteImportSetting(getProjectID(projectPath));

                // retrieve the project task list
                EVTaskList taskList = loadProjectTaskList(projectPath);

                // delete the project from the hierarchy
                HierarchyAlterer alt = DashController.getHierarchyAlterer();
                alt.deleteNode(projectPath);
                deleteChildlessParentsOfNode(alt, selectedNode);

                // delete the project task list
                if (taskList != null)
                    taskList.save(null);

            } catch (HierarchyAlterationException e) {
                showHierarchyError(e);
            }
        }
    }

    private boolean okToDeleteProject(String projectPath, String templateID) {
        // do not delete plain nodes (which act as folders full of projects)
        ProjectType type = getProjectType(projectPath, templateID);
        if (type == null)
            return false;

        // it is harmless to delete a team project stub.
        if (type == ProjectType.Stub)
            return true;

        // the user is deleting a real project. Display a dialog to confirm.
        String title = resources.getString("Delete.Title");
        Object message = getDeleteProjectWarningMessage(projectPath, type);
        int userChoice = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return userChoice == JOptionPane.YES_OPTION;
    }

    private Object getDeleteProjectWarningMessage(String projectPath,
            ProjectType type) {
        List message = new ArrayList();

        // add a header to the message.
        message.add(resources.formatStrings("Delete.Message_Header_FMT",
            projectPath));
        message.add(" ");

        // if WBS planning has been done, add a warning.
        if (hasWbsData(projectPath, type)) {
            message.add(resources.getStrings("Delete.WBS_Data_Warning"));
            message.add(" ");
        }

        // if team members have joined the project, add a warning.
        if (membersHaveJoined(projectPath, type)) {
            message.add(resources.getStrings("Delete.Members_Joined"));
            message.add(" ");
        }

        // warn an individual that actual data will be lost.
        if (type == ProjectType.Indiv || type == ProjectType.Personal) {
            message.add(resources.getStrings("Delete.Personal_Data_Warning"));
            message.add(" ");
        }

        // if an individual is deleting a joined project, add a warning.
        if (type == ProjectType.Indiv) {
            message.add(resources.getStrings("Delete.Unjoin_Warning"));
            message.add(" ");
        }

        // add a footer to the message.
        message.add(resources.getString("Delete.Message_Footer"));
        return message.toArray();
    }

    private boolean hasWbsData(String projectPath, ProjectType type) {
        return (type != ProjectType.Indiv && listLongerThan(projectPath,
            "Synchronized_Task_ID_WBS_Order", 2));
    }

    private boolean membersHaveJoined(String projectPath, ProjectType type) {
        return (type == ProjectType.Team && listLongerThan(projectPath,
            "Corresponding_Project_Nodes", 0));
    }

    private boolean listLongerThan(String projectPath, String listName,
            int length) {
        String name = projectPath + "/" + listName;
        ListData l = ListData.asListData(ctx.getData().getSimpleValue(name));
        return l != null && l.size() > length;
    }

    private void maybeCleanupPdashFile(String projectPath, String templateID) {
        // we only need to clean up the PDASH file for indiv joined projects
        ProjectType type = getProjectType(projectPath, templateID);
        if (type != ProjectType.Indiv)
            return;

        // export one last copy of the file, so it contains all of the most
        // up-to-date metrics this person has collected.
        DashController.exportData(projectPath);

        // make a backup copy of our PDASH for recovery purposes
        DataContext data = ctx.getData().getSubcontext(projectPath);
        TeamProjectSetupWizard.maybeBackupExistingPdashFile(data, false);

        // Now delete our PDASH file so it doesn't contribute to team rollups.
        ExportFileStream.deleteExportTarget(getExportFilePath(data));
    }

    private String getExportFilePath(DataContext data) {
        // retrieve the name of the PDASH export file
        SimpleData exportFilename = data.getSimpleValue("EXPORT_FILE");
        if (exportFilename == null || !exportFilename.test())
            return null;
        File file = new File(exportFilename.format());

        // if this is a server-based project, retrieve its URL
        SimpleData teamDataDirUrl = data
                .getSimpleValue(TeamDataConstants.TEAM_DATA_DIRECTORY_URL);
        String url = (teamDataDirUrl == null ? null : teamDataDirUrl.format());

        // ask ExportFileStream to produce its canonical export target string
        return ExportFileStream.getExportTargetPath(file, url);
    }

    private void deleteChildlessParentsOfNode(HierarchyAlterer hierarchyAlterer,
            PropertyKey node) throws HierarchyAlterationException {
        // if the deletion of a node caused its old parent to become
        // childless, delete that parent (and grandparent, etc)
        PropertyKey oldParent = node.getParent();
        while (isDeletableChildlessParent(oldParent)) {
            hierarchyAlterer.deleteNode(oldParent.path());
            oldParent = oldParent.getParent();
        }
    }

    private boolean isDeletableChildlessParent(PropertyKey node) {
        if (node == null || PropertyKey.ROOT.equals(node))
            return false;
        else if (PropertyKey.ROOT.equals(node.getParent())
                && STANDARD_NODE_NAMES.contains(node.name()))
            return false;
        else
            return !StringUtils.hasValue(ctx.getHierarchy().getID(node))
                    && ctx.getHierarchy().getNumChildren(node) == 0;
    }

    private static final Set<String> STANDARD_NODE_NAMES = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList( //
                "Project", resources.getString("Project"), //
                "Non Project", resources.getString("Non_Project"))));

    private void showHierarchyError(HierarchyAlterationException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent,
            resources.getStrings("Hierarchy_Err.Message"),
            resources.getString("Hierarchy_Err.Title"),
            JOptionPane.ERROR_MESSAGE);
    }


 
    /** 
     * Interact with the user to relaunch a team/personal project.
     *  
     * @param projectKey
     *            the project to relaunch
     */ 
    public void maybeRelaunchProject(PropertyKey projectKey) {
            String templateID = ctx.getHierarchy().getID(projectKey);
            ProjectType type = getProjectType(projectKey);
            if (type != ProjectType.Team && type != ProjectType.Personal)
                return;

            StringBuilder uri = new StringBuilder();
            uri.append(HTMLUtils.urlEncodePath(projectKey.path()));

            int slashPos = templateID.indexOf('/');
            String processID = templateID.substring(0, slashPos);
            uri.append("//").append(processID)
                    .append("/setup/wizard.class?page=relaunch");

            Browser.launch(uri.toString());
    }
 
 
 
    public ProjectType getProjectType(PropertyKey projectKey) {
        return getProjectType(projectKey.path(),
            ctx.getHierarchy().getID(projectKey));
    } 
 
    private ProjectType getProjectType(String projectPath, String templateID) {
        if (templateID == null)
            return null;
 
        else if (templateID.equals(TeamStartBootstrap.TEAM_STUB_ID))
            return ProjectType.Stub;
 
        else if (templateID.endsWith("/TeamRoot"))
            return ProjectType.Team;
 
        else if (templateID.endsWith("/MasterRoot"))
            return ProjectType.Master;
 
        else if (templateID.endsWith("/Indiv2Root")) {
            String dataName = DataRepository.createDataName(projectPath,
                TeamDataConstants.PERSONAL_PROJECT_FLAG);
            SimpleData sd = ctx.getData().getSimpleValue(dataName); 
            return (sd != null && sd.test() ? ProjectType.Personal
                    : ProjectType.Indiv);
 
        } else
            return null;
    } 

    private String getProjectID(String projectPath) {
        return getValue(projectPath, TeamDataConstants.PROJECT_ID);
    }

    private String getValue(String projectPath, String name) {
        String dataName = DataRepository.createDataName(projectPath, name);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    private EVTaskList loadProjectTaskList(String projectPath) {
        String taskListName = EVTaskList
                .getRegisteredTaskListForPath(ctx.getData(), projectPath);
        if (taskListName == null)
            return null;
        else
            return EVTaskList.openExisting(taskListName, ctx.getData(),
                ctx.getHierarchy(), ctx.getCache(), false);
    }

}
