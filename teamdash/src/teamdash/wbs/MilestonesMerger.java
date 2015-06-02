// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

import teamdash.merge.ui.MergeConflictNotification.ModelType;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;

public class MilestonesMerger extends AbstractWBSModelMerger<MilestonesWBSModel> {

    public MilestonesMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this(base.getMilestones(), main.getMilestones(), incoming
                .getMilestones());
    }

    public MilestonesMerger(MilestonesWBSModel base, MilestonesWBSModel main,
            MilestonesWBSModel incoming) {
        super(base, main, incoming);

        // register handlers for attributes as needed.
        ignoreAttributeConflicts(
            MilestoneCommitDateColumn.MASTER_VALUE_ATTR,
            MilestoneColorColumn.VALUE_ATTR);
    }

    @Override
    protected MilestonesWBSModel createWbsModel() {
        return new MilestonesWBSModel("x");
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.Milestones;
    }

}
