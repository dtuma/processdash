// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.awt.event.ActionEvent;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.ui.TeamProjectAlterer.ProjectType;

/**
 * This class provides an adaptive "Alter Project" submenu that can be added to
 * the configuration menu in a personal dashboard.
 */
public class IndivAlterProjectMenu extends JMenu {

    private AbstractAction relaunchAction, closeAction;

    private ProcessDashboard dash;

    private TeamProjectAlterer alterer;

    private PropertyKey activeProject;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Menu.File");


    public IndivAlterProjectMenu(ProcessDashboard dash) {
        super(resources.getString("Alter_Team_Project"));

        add(new RenameProjectAction());
        add(relaunchAction = new RelaunchProjectAction());
        add(closeAction = new CloseProjectAction());
        add(new DeleteProjectAction());

        this.dash = dash;
        this.alterer = new TeamProjectAlterer(dash, dash);
        dash.getActiveTaskModel().addPropertyChangeListener(EventHandler.create(
            PropertyChangeListener.class, this, "updateActiveTask"));
        updateActiveTask();
    }

    public void updateActiveTask() {
        // find the project containing the active task
        PropertyKey activeTask = dash.getActiveTaskModel().getNode();
        activeProject = findProjectContaining(activeTask);

        // if there is no containing project, disable the menu and return
        setEnabled(activeProject != null);
        if (activeProject == null)
            return;

        // enable/disable actions that are only allowed for personal projects
        boolean isPersonalProject = (alterer
                .getProjectType(activeProject) == ProjectType.Personal);
        relaunchAction.setEnabled(isPersonalProject);
        closeAction.setEnabled(isPersonalProject);
    }

    private PropertyKey findProjectContaining(PropertyKey node) {
        while (node != null) {
            String templateID = dash.getHierarchy().getID(node);
            if (templateID != null && templateID.endsWith("/Indiv2Root"))
                return node;
            else
                node = node.getParent();
        }
        return null;
    }



    private class RenameProjectAction extends AbstractAction {

        public RenameProjectAction() {
            super(resources.getString("Rename_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            PropertyKey renamedProject = alterer
                    .maybeRenameProject(activeProject);
            if (renamedProject != null)
                dash.getActiveTaskModel().setNode(renamedProject);
        }

    }

    private class CloseProjectAction extends AbstractAction {

        public CloseProjectAction() {
            super(resources.getString("Close_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            alterer.maybeCloseProject(activeProject);
        }

    }

    private class DeleteProjectAction extends AbstractAction {

        public DeleteProjectAction() {
            super(resources.getString("Delete_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            alterer.maybeDeleteProject(activeProject);
        }

    }

    private class RelaunchProjectAction extends AbstractAction {

        public RelaunchProjectAction() {
            super(resources.getString("Relaunch_Team_Project"));
        }

        public void actionPerformed(ActionEvent e) {
            alterer.maybeRelaunchProject(activeProject);
        }

    }

}
