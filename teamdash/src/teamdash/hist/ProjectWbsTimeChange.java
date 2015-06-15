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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class ProjectWbsTimeChange extends ProjectWbsChange {

    private WBSNodeContent oldData, newData;

    private List<ProjectWbsTimeChange> children;

    private Set<String> indivTimeAttrs;

    private Map<String, String> teamMemberNames;

    private Set<String> authors;

    private Map<String, IndivTime> added, deleted, changed, unchanged, zeros,
            all;

    private double oldTotalTime, newTotalTime;

    protected ProjectWbsTimeChange(WBSNode node,
            List<ProjectWbsTimeChange> children, Set<String> indivTimeAttrs,
            Map<String, String> teamMemberNames, String author, Date timestamp) {
        super(node, author, timestamp);

        this.children = children;
        this.indivTimeAttrs = indivTimeAttrs;
        this.teamMemberNames = teamMemberNames;

        // sum up information about time from each of our children
        sumUpChildTimeChanges();
    }

    protected ProjectWbsTimeChange(WBSNode node, WBSNodeContent oldData,
            WBSNodeContent newData, Set<String> indivTimeAttrs,
            Map<String, String> teamMemberNames, String author, Date timestamp) {
        super(node, author, timestamp);

        this.oldData = oldData;
        this.newData = newData;
        this.indivTimeAttrs = indivTimeAttrs;
        this.teamMemberNames = teamMemberNames;

        // load information about the time for each individual
        loadIndivTimes();

        // read the total old and new time for the node
        oldTotalTime = getTotalTime(oldTotalTime, oldData);
        newTotalTime = getTotalTime(newTotalTime, newData);
    }

    private double getTotalTime(double totalFromIndivs, WBSNodeContent data) {
        // use the explicit total if it is present and applicable
        double result = getDouble(data, TeamTimeColumn.TEAM_TIME_ATTR, 0);
        result = Math.max(totalFromIndivs, result);
        return result;
    }

    public List<ProjectWbsTimeChange> getChildren() {
        return children;
    }

    public double getOldTotalTime() {
        return oldTotalTime;
    }

    public double getNewTotalTime() {
        return newTotalTime;
    }

    @Override
    public String getDescription() {
        StringBuilder result = new StringBuilder();
        result.append("task " + getNode() + " (#" + getNode().getUniqueID()
                + (children == null ? "" : "*") + ") ");

        // append a message about task assignment changes
        if (deleted.isEmpty()) {
            if (added.isEmpty()) {
                // no task assignment changes to report
            } else {
                // new people were added to this task
                if (children != null)
                    result.append("subtasks ");
                result.append("assigned to " + added.values() + ". ");
            }
        } else {
            if (added.isEmpty()) {
                // people were unassigned from this task
                if (children != null)
                    result.append("subtasks ");
                result.append("unassigned from " + deleted.values() + ". ");
            } else {
                // task was reassigned
                if (children != null)
                    result.append("subtasks ");
                result.append("reassigned from " + deleted.values() + " to "
                        + added.values() + ". ");
            }
        }

        // append a message about changes to time estimates
        if (changed.isEmpty()) {
            // no changed time to report

        } else if (changed.size() == 1) {
            // exactly one individual changed time
            IndivTime change = changed.values().iterator().next();
            result.append("time estimate for " + change.name + " changed from "
                    + change.oldTime + " to " + change.newTime + ". ");

        } else if (unchanged.isEmpty()) {
            // time changed for everyone on this task (likely due to scaling)
            Set<IndivTime> active = new HashSet<ProjectWbsTimeChange.IndivTime>();
            active.addAll(added.values());
            active.addAll(changed.values());
            active.addAll(zeros.values());
            result.append("time estimate changed from " + oldTotalTime + " to "
                    + newTotalTime + ": " + active + ". ");

        } else {
            // a specific subset of individual time estimates changed
            result.append("time estimates changed " + changed.values() + ". ");
        }

        return result.toString();
    }


    private void sumUpChildTimeChanges() {
        initIndivTimeFields();

        for (String indivTimeAttr : indivTimeAttrs)
            new IndivTime(indivTimeAttr, children).storeData();

        authors = new TreeSet<String>();
        for (ProjectWbsTimeChange child : children) {
            authors.addAll(child.authors);
            oldTotalTime += child.oldTotalTime;
            newTotalTime += child.newTotalTime;
        }
        setAuthor(StringUtils.join(authors, ", "));
    }

    private void loadIndivTimes() {
        initIndivTimeFields();

        authors = new TreeSet<String>();
        for (String indivTimeAttr : indivTimeAttrs)
            new IndivTime(indivTimeAttr).storeData();

        setAuthor(StringUtils.join(authors, ", "));
    }

    protected void initIndivTimeFields() {
        added = new TreeMap<String, IndivTime>();
        deleted = new TreeMap<String, IndivTime>();
        changed = new TreeMap<String, IndivTime>();
        unchanged = new TreeMap<String, IndivTime>();
        zeros = new TreeMap<String, IndivTime>();
        all = new TreeMap<String, IndivTime>();
        oldTotalTime = newTotalTime = 0;
    }

    private class IndivTime {
        String timeAttr, name;

        double oldTime, newTime;

        protected IndivTime(String timeAttr) {
            this.timeAttr = timeAttr;
            this.name = teamMemberNames.get(timeAttr);
            this.oldTime = getDouble(oldData, timeAttr, Double.NaN);
            this.newTime = getDouble(newData, timeAttr, Double.NaN);
            oldTotalTime = sumTime(oldTotalTime, oldTime);
            newTotalTime = sumTime(newTotalTime, newTime);
        }

        protected IndivTime(String timeAttr, List<ProjectWbsTimeChange> children) {
            this.timeAttr = timeAttr;
            this.name = teamMemberNames.get(timeAttr);
            this.oldTime = this.newTime = Double.NaN;

            for (ProjectWbsTimeChange child : children) {
                IndivTime that = child.all.get(timeAttr);
                if (that != null) {
                    this.oldTime = sumTime(this.oldTime, that.oldTime);
                    this.newTime = sumTime(this.newTime, that.newTime);
                }
            }
        }

        protected void storeData() {
            if (Double.isNaN(oldTime)) {
                if (!Double.isNaN(newTime))
                    storeData(getAuthor(), added);

            } else if (Double.isNaN(newTime)) {
                storeData(getAuthor(), deleted);

            } else if (eq(oldTime, newTime)) {
                storeData(null, (oldTime == 0 ? zeros : unchanged));

            } else {
                storeData(timeEqualsSyncTime() ? name : getAuthor(), changed);
            }
        }

        private void storeData(String who, Map<String, IndivTime> where) {
            if (who != null && authors != null)
                authors.add(who);
            if (where != null)
                where.put(timeAttr, this);
            all.put(timeAttr, this);
        }

        private boolean timeEqualsSyncTime() {
            if (newData == null)
                return false;

            String timeVal = newData.get(timeAttr);
            if (timeVal == null)
                return false;

            String syncTimeAttr = StringUtils.findAndReplace(timeAttr,
                TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX,
                TeamMemberTimeColumn.TEAM_MEMBER_SYNC_TIME_SUFFIX);
            String syncVal = newData.get(syncTimeAttr);
            if (syncVal == null)
                return false;

            if (timeVal.equals(syncVal))
                return true;

            Double timeValNum = Double.parseDouble(timeVal);
            Double syncValNum = Double.parseDouble(syncVal);
            return eq(timeValNum, syncValNum);
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(name).append(" (");
            if (added.containsKey(timeAttr))
                result.append(newTime);
            else if (changed.containsKey(timeAttr))
                result.append(oldTime).append(" -> ").append(newTime);
            else
                result.append(oldTime);
            result.append(")");
            return result.toString();
        }
    }

    private static double getDouble(WBSNodeContent data, String attrName,
            double defaultVal) {
        String value = data.get(attrName);
        if (value == null)
            return defaultVal;
        else
            return Double.parseDouble(value);
    }

    private static double sumTime(double a, double b) {
        if (Double.isNaN(a))
            return b;
        else if (Double.isNaN(b))
            return a;
        else
            return a + b;
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

}
