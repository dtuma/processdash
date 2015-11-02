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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teamdash.merge.DefaultAttributeMerger;
import teamdash.merge.MapContentMerger;
import teamdash.merge.ui.DataModelSource;
import teamdash.merge.ui.MergeConflictNotification;
import teamdash.team.TeamMemberList;
import teamdash.team.TeamMemberListMerger;
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.CustomColumnsMerger;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WorkflowResourcesColumn;

public class TeamProjectMerger {

    private TeamProject base;

    private TeamProject main;

    private TeamProject incoming;

    private TeamProject merged;

    private List<MergeConflictNotification> conflicts;

    public TeamProjectMerger(TeamProject base, TeamProject main,
            TeamProject incoming) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;
        this.conflicts = new ArrayList<MergeConflictNotification>();
    }

    public TeamProject getMerged() {
        return merged;
    }

    public List<MergeConflictNotification> getConflicts(
            DataModelSource dataModelSource) {
        WBSModelMergeConflictNotificationFactory.refineAll(conflicts,
            dataModelSource);
        return conflicts;
    }

    public void run() {
        // alter the node IDs in the incoming branch to ensure the best match.
        TeamProjectNodeIDMatcher.performMatch(base, main, incoming);

        // merge the various data structures in the team project.
        TeamMemberList team = mergeTeams();
        WorkflowWBSModel workflows = mergeWorkflows();
        ProxyWBSModel proxies = mergeProxies();
        MilestonesWBSModel milestones = mergeMilestones();
        CustomColumnSpecs columns = mergeColumns();
        WBSModel wbs = mergeWBS();
        Map userSettings = mergeUserSettings();

        // create a TeamProject object to hold the merged data.
        File dir = new File("no such directory " + System.currentTimeMillis());
        merged = new TeamProject(dir, "Unused", team, wbs, workflows, proxies,
                milestones, columns, userSettings);
    }

    private TeamMemberList mergeTeams() {
        // calculate the merged team member list.
        TeamMemberListMerger teamMerger = new TeamMemberListMerger(base, main,
                incoming);

        // record any conflicts that occurred during the merge
        conflicts.addAll(teamMerger.getConflictNotifications());

        // the team member merge may have caused initials to change in the
        // main and incoming projects. Apply those changes to the WBS and
        // to the workflows.
        changeInitials(main.getWBS(), main.getWorkflows(),
            teamMerger.getChangesNeededToMainInitials());
        changeInitials(incoming.getWBS(), incoming.getWorkflows(),
            teamMerger.getChangesNeededToIncomingInitials());

        return teamMerger.getMerged();
    }

    private void changeInitials(WBSModel wbsModel,
            WorkflowWBSModel workflowModel,
            Map<String, String> changesToInitials) {
        if (changesToInitials != null && !changesToInitials.isEmpty()) {
            TeamTimeColumn.changeInitials(wbsModel, changesToInitials);
            TeamMemberTimeColumn.changeInitials(wbsModel, changesToInitials);
            WorkflowResourcesColumn.changeInitials(workflowModel,
                changesToInitials);
        }
    }

    private WorkflowWBSModel mergeWorkflows() {
        // calculate the merged workflows
        WorkflowMerger workflowMerger = new WorkflowMerger(base, main,
                incoming);
        workflowMerger.run();

        // record any conflicts that occurred during the merge
        conflicts.addAll(workflowMerger.getConflictNotifications());

        return workflowMerger.getMerged();
    }

    private ProxyWBSModel mergeProxies() {
        // calculate the merged proxies
        ProxyMerger proxyMerger = new ProxyMerger(base, main, incoming);
        proxyMerger.run();

        // record any conflicts that occurred during the merge
        conflicts.addAll(proxyMerger.getConflictNotifications());

        return proxyMerger.getMerged();
    }

    private MilestonesWBSModel mergeMilestones() {
        // calculate the merged milestones
        MilestonesMerger milestonesMerger = new MilestonesMerger(base, main,
                incoming);
        milestonesMerger.run();

        // record any conflicts that occurred during the merge
        conflicts.addAll(milestonesMerger.getConflictNotifications());

        return milestonesMerger.getMerged();
    }

    private CustomColumnSpecs mergeColumns() {
        // calculate the merged custom column specifications
        CustomColumnsMerger columnsMerger = new CustomColumnsMerger(base, main,
                incoming);

        // record any conflicts that occurred during the merge
        conflicts.addAll(columnsMerger.getConflictNotifications());

        return columnsMerger.getMerged();
    }

    private WBSModel mergeWBS() {
        // calculate the merged WBS
        WBSMerger wbsMerger = new WBSMerger(base, main, incoming);
        wbsMerger.run();

        // record any conflicts that occurred during the merge
        conflicts.addAll(wbsMerger.getConflictNotifications());

        return wbsMerger.getMerged();
    }

    private Map mergeUserSettings() {
        // merge settings silently, preferring the value from main if
        // the two branches include conflicting edits
        MapContentMerger<String> settingsMerger = new MapContentMerger();
        settingsMerger.setDefaultHandler(
                DefaultAttributeMerger.SILENTLY_PREFER_MAIN);
        Map result = settingsMerger.mergeContent("unused",
                base.getUserSettings(), main.getUserSettings(),
                incoming.getUserSettings(), null);
        return result;
    }

}
