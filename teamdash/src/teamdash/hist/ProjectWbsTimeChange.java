// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamMemberTimeColumn;

public class ProjectWbsTimeChange extends ProjectWbsChange {

    private Set<String> authors;

    private WBSNodeContent oldData;

    private WBSNodeContent newData;

    private Set<String> unchangedIndivAttrs;

    private Set<String> changedIndivAttrs;

    protected ProjectWbsTimeChange(WBSNode node, WBSNodeContent oldData,
            WBSNodeContent newData, Set<String> unchangedIndivAttrs,
            Set<String> changedIndivAttrs, Map<String, String> teamMemberNames,
            String author, Date timestamp) {
        this(node, oldData, newData, unchangedIndivAttrs, changedIndivAttrs,
                findAuthorsFromSyncData(changedIndivAttrs, newData,
                    teamMemberNames, author), timestamp);
    }

    protected ProjectWbsTimeChange(WBSNode node, WBSNodeContent oldData,
            WBSNodeContent newData, Set<String> unchangedIndivAttrs,
            Set<String> changedIndivAttrs, Set<String> authors, Date timestamp) {
        super(node, StringUtils.join(authors, ", "), timestamp);
        this.authors = authors;
        this.oldData = oldData;
        this.newData = newData;
        this.unchangedIndivAttrs = unchangedIndivAttrs;
        this.changedIndivAttrs = changedIndivAttrs;
    }

    public boolean allTimesWereChanged() {
        return unchangedIndivAttrs == null || unchangedIndivAttrs.isEmpty();
    }

    @Override
    public String getDescription() {
        return "times changed for " + getNode();
    }

    private static Set<String> findAuthorsFromSyncData(
            Set<String> changedIndivAttrs, WBSNodeContent newData,
            Map<String, String> teamMemberNames, String globalAuthor) {
        Set<String> result = new TreeSet<String>();
        for (String indivTimeAttr : changedIndivAttrs) {
            if (timeEqualsSyncTime(newData, indivTimeAttr)) {
                String thisAuthor = teamMemberNames.get(indivTimeAttr);
                if (thisAuthor != null)
                    result.add(thisAuthor);
            } else {
                result.add(globalAuthor);
            }
        }
        return result;
    }

    private static boolean timeEqualsSyncTime(WBSNodeContent data,
            String indivTimeAttr) {
        String timeVal = data.get(indivTimeAttr);
        if (timeVal == null)
            return false;

        String syncTimeAttr = StringUtils.findAndReplace(indivTimeAttr,
            TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX,
            TeamMemberTimeColumn.TEAM_MEMBER_SYNC_TIME_SUFFIX);
        String syncVal = data.get(syncTimeAttr);
        if (syncVal == null)
            return false;

        if (timeVal.equals(syncVal))
            return true;

        Double timeValNum = Double.parseDouble(timeVal);
        Double syncValNum = Double.parseDouble(syncVal);
        return Math.abs(timeValNum - syncValNum) < 0.00001;
    }

}
