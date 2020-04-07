// Copyright (C) 2013-2020 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamStartBootstrap;
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
        if (getProjectType(projectPath, templateID) == null)
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

                    // if this caused the old parent of the project to become
                    // childless, delete that parent (and grandparent, etc)
                    PropertyKey oldParent = projectNode.getParent();
                    while (ctx.getHierarchy().getNumChildren(oldParent) == 0) {
                        hierarchyAlterer.deleteNode(oldParent.path());
                        oldParent = oldParent.getParent();
                    }

                    // point the "active task" at the renamed project.
                    PropertyKey newNode = ctx.getHierarchy().findExistingKey(
                        newPath);
                    return newNode;

                } catch (HierarchyAlterationException e) {
                    e.printStackTrace();
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

        // do not close plain nodes (which act as folders full of projects)
        if (type == null)
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
                DashController.getHierarchyAlterer().deleteNode(projectPath);
            } catch (HierarchyAlterationException e) {
                e.printStackTrace();
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
        if (hasWbsData(projectPath)) {
            message.add(resources.getStrings("Delete.WBS_Data_Warning"));
            message.add(" ");
        }

        // if team members have joined the project, add a warning.
        if (membersHaveJoined(projectPath, type)) {
            message.add(resources.getStrings("Delete.Members_Joined"));
            message.add(" ");
        }

        // add a footer to the message.
        message.add(resources.getString("Delete.Message_Footer"));
        return message.toArray();
    }

    private boolean hasWbsData(String projectPath) {
        return listLongerThan(projectPath, "Synchronized_Task_ID_WBS_Order", 2);
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

}
