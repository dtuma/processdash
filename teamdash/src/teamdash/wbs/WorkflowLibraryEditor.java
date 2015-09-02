// Copyright (C) 2002-2015 Tuma Solutions, LLC
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

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.wbs.columns.WorkflowOptionalColumn;
import teamdash.wbs.columns.WorkflowResourcesColumn;

public class WorkflowLibraryEditor extends AbstractLibraryEditor {

    public static final String ORG_WORKFLOWS_SYS_PROP = //
            "teamdash.wbs.orgWorkflowURLs";

    private static final Resources RESOURCES = Resources
            .getDashBundle("WBSEditor.Workflow_Library");

    private static final String FILENAME_EXTENSION = ".wfxml";

    public WorkflowLibraryEditor(TeamProject teamProject, JFrame parent,
            Mode mode) throws UserCancelledException {
        super(teamProject, parent, mode, RESOURCES, FILENAME_EXTENSION,
                ORG_WORKFLOWS_SYS_PROP);
    }

    @Override
    protected void openModels() {
        TeamProcess process = teamProject.getTeamProcess();
        libraryModel =  new WorkflowModel(library, process, null);

        projectWbs = new WorkflowWBSModel();
        projectWbs.copyFrom(teamProject.getWorkflows());
        projectModel = new WorkflowModel(this.projectWbs, process, null);
    }

    @Override
    protected WBSJTable buildJTable(DataTableModel model) {
        WBSJTable table = WorkflowEditor.createWorkflowJTable(
            (WorkflowModel) model, teamProject.getTeamProcess());
        for (int i = table.getColumnCount(); i-- > 0; ) {
            DataColumn column = model.getColumn(i);
            if (column instanceof WorkflowOptionalColumn
                    || column instanceof WorkflowResourcesColumn)
                table.removeColumn(table.getColumnModel().getColumn(i));
        }
        return table;
    }

    @Override
    protected WBSLibrary openLibraryFile(File file) throws IOException {
        return new WBSLibrary.Workflows(file);
    }

    @Override
    protected WBSLibrary openOrgLibrary(String[] urls) {
        return new WBSLibrary.Workflows(urls, teamProject.getTeamProcess());
    }

    @Override
    protected WBSLibrary openNewLibrary(File file) throws IOException {
        return new WBSLibrary.Workflows(file, teamProject.getTeamProcess());
    }

    @Override
    public boolean doImport() {
        teamProject.getWorkflows().copyFrom(projectWbs);
        return true;
    }

    public static boolean orgAssetsAreAvailable(TeamProcess process) {
        try {
            String sysprop = System.getProperty(ORG_WORKFLOWS_SYS_PROP);
            if (sysprop == null)
                return false;
            String[] urls = sysprop.trim().split("\\s+");
            return new WBSLibrary.Workflows(urls, process).isNotEmpty();
        } catch (Exception e) {
            return false;
        }
    }

}
