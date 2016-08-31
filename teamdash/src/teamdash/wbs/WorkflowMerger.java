// Copyright (C) 2012-2016 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.DependentAttributeMerger;
import teamdash.merge.ModelType;
import teamdash.merge.MergeWarning.Severity;
import teamdash.wbs.columns.AbstractNotesColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WorkflowNotesColumn;

public class WorkflowMerger extends AbstractWBSModelMerger<WorkflowWBSModel> {

    public WorkflowMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this(base.getWorkflows(), main.getWorkflows(), incoming.getWorkflows());
    }

    public WorkflowMerger(WorkflowWBSModel base, WorkflowWBSModel main,
            WorkflowWBSModel incoming) {
        super(base, main, incoming);

        // register handlers for attributes as needed.
        ignoreAttributeConflicts("^Phase Mapping ");
        ignoreAttributeConflicts(TeamTimeColumn.RATE_ATTR);
        addNoteAttrHandler(WorkflowNotesColumn.VALUE_ATTR);
    }

    private void addNoteAttrHandler(String attrName) {
        contentMerger.addHandler(new PatternList().addLiteralEquals(attrName),
            new DependentAttributeMerger(Severity.CONFLICT).setDependentAttrs(
                attrName, AbstractNotesColumn.getMetadataAttrs(attrName)));
    }

    @Override
    protected WorkflowWBSModel createWbsModel() {
        return new WorkflowWBSModel();
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.Workflows;
    }

}
