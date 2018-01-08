// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.util.Map;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.IntList;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamMemberActualTimeColumn;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class WBSUtil {

    private WBSModel wbs;

    private Map<String, String> timeChanges;

    private String[] initials;

    private String[] indivTimeAttrs;

    private String[] indivZeroAttrs;

    private String[] indivActualTimeAttrs;

    public WBSUtil(WBSModel wbs, Map<String, String> timeChanges,
            TeamMemberList team) {
        this.wbs = wbs;
        this.timeChanges = timeChanges;
        this.initials = new String[team.getRowCount()];
        this.indivTimeAttrs = new String[team.getRowCount()];
        this.indivZeroAttrs = new String[team.getRowCount()];
        this.indivActualTimeAttrs = new String[team.getRowCount()];
        for (int i = team.getRowCount(); i-- > 0;) {
            TeamMember tm = team.get(i);
            initials[i] = tm.getInitials();
            indivTimeAttrs[i] = TeamMemberTimeColumn
                    .getMemberNodeDataAttrName(tm);
            indivZeroAttrs[i] = TeamTimeColumn
                    .getMemberAssignedZeroAttrName(tm);
            indivActualTimeAttrs[i] = TeamMemberActualTimeColumn
                    .getNodeDataAttrName(tm);
        }
    }


    /**
     * @return the total estimated time for a node in the WBS
     */
    public double getEstimatedTime(WBSNode node) {
        return sumAttr(node, TEAM_TIME_ATTR);
    }


    /**
     * @return the total actual time for a node in the WBS
     */
    public double getActualTime(WBSNode node) {
        double result = 0;
        for (String attr : indivActualTimeAttrs)
            result += sumAttr(node, attr);
        return result;
    }


    /**
     * Sum up the values for a WBS node for a given top-down-bottom-up time
     * column, based on its storage attribute
     */
    public double sumAttr(WBSNode node, String topDownAttrName) {
        // recursively calculate the sum of the given attribute for the children
        // of this node. (If the node is a leaf, this will of course be zero.)
        double bottomUpValue = 0;
        for (WBSNode child : wbs.getChildren(node)) {
            bottomUpValue += sumAttr(child, topDownAttrName);
        }

        if (bottomUpValue > 0) {
            // if a bottom-up value was present, return it.
            return bottomUpValue;

        } else {
            // if the node had no children, or if the children had no values,
            // return the top-down value that has been entered on this node.
            double nodeValue = node.getNumericAttribute(topDownAttrName);
            return (nodeValue > 0 ? nodeValue : 0);
        }
    }


    /**
     * Alter the time estimate for a node in the WBS
     */
    public void changeTimeEstimate(WBSNode node, double oldTime,
            double newTime) {
        WBSNode[] descendants = wbs.getDescendants(node);
        if (descendants.length == 0) {
            // if this is a leaf node, change its estimate directly.
            changeNodeTimeEstimate(node, -1, newTime);

        } else if (oldTime > 0) {
            // if the node has a nonzero time estimate, scale it.
            double multRatio = newTime / oldTime;
            changeNodeTimeEstimate(node, multRatio, -1);
            for (WBSNode desc : descendants)
                changeNodeTimeEstimate(desc, multRatio, -1);

        } else if (newTime > 0) {
            // change the component time estimate from zero to some value
            assignComponentTimeEstimate(node, newTime);
        }
    }

    private void changeNodeTimeEstimate(WBSNode node, double multRatio,
            double newTime) {
        // read the current top-down time estimate on the node
        double oldTime = node.getNumericAttribute(TEAM_TIME_ATTR);
        if (Double.isNaN(oldTime)) {
            if (wbs.isLeaf(node))
                // missing estimates on leaf nodes are equivalent to zero
                oldTime = 0;
            else
                // if this node does not have a top-down time estimate, abort.
                // this will be typical for parent nodes in the WBS.
                return;
        }

        // calculate the new time and/or ratio
        if (multRatio >= 0)
            newTime = oldTime * multRatio;
        else
            multRatio = newTime / oldTime;

        // if the time has not changed, do nothing
        if (eq(oldTime, newTime))
            return;

        // store the new team time estimate on the node
        node.setNumericAttribute(TEAM_TIME_ATTR, newTime);
        node.removeAttribute(TeamTimeColumn.RATE_ATTR);

        if (oldTime == 0) {
            // if the time is going from zero to a real value, spread that new
            // time across the assigned individuals
            spreadTimeAcrossAssignedPeople(node, newTime);

        } else if (newTime == 0) {
            // if the time is going from a real value to zero, change the
            // affected individuals to "assigned with zero"
            zeroTimeForAssignedPeople(node);

        } else {
            // if individuals are assigned with nonzero time, scale their times
            scaleTeamMemberTimes(node, multRatio);
        }

        // record the new "assigned time" string for use in projDump.xml
        timeChanges.put(Integer.toString(node.getUniqueID()),
            getProjDumpTimeString(node));
    }

    private void spreadTimeAcrossAssignedPeople(WBSNode node, double newTime) {
        // find out how many people are assigned to this task
        int[] assigned = getAssignedPeople(node);
        int numPeople = Math.max(1, assigned.length);
        if (isTask(node) && wbs.isLeaf(node)) {
            Integer taskNumPeople = node
                    .getIntegerAttribute(TeamTimeColumn.NUM_PEOPLE_ATTR);
            if (taskNumPeople != null && taskNumPeople > numPeople)
                numPeople = taskNumPeople;
        }

        // calculate the time per person, and give each person that much time
        double tpp = newTime / numPeople;
        node.setNumericAttribute(TPP_ATTR, tpp);
        for (int i : assigned) {
            node.setNumericAttribute(indivTimeAttrs[i], tpp);
            node.removeAttribute(indivZeroAttrs[i]);
        }
    }

    private void zeroTimeForAssignedPeople(WBSNode node) {
        for (int i : getAssignedPeople(node)) {
            node.setNumericAttribute(indivTimeAttrs[i], 0.0);
            node.setAttribute(indivZeroAttrs[i], "t");
        }
        node.setNumericAttribute(TPP_ATTR, 0.0);
    }

    private void scaleTeamMemberTimes(WBSNode node, double multRatio) {
        for (int i : getAssignedPeople(node)) {
            double oldTime = node.getNumericAttribute(indivTimeAttrs[i]);
            node.setNumericAttribute(indivTimeAttrs[i], oldTime * multRatio);
        }
        double tpp = node.getNumericAttribute(TPP_ATTR);
        node.setNumericAttribute(TPP_ATTR, tpp * multRatio);
    }

    private void assignComponentTimeEstimate(WBSNode node, double newTime) {
        // this method is called when a node has children, but still has an
        // estimated time of zero. In practical terms, this could occur if:
        // -- the team had imported an external node with no time estimate
        // -- then they applied a workflow to that node in the WBS
        // -- BUT they did not enter any time estimate in the WBS
        // -- then, they assigned a time estimate in the external system.
        //
        // The "best" strategy in this scenario would be to spread the new
        // time estimate across the subtasks using workflow percentages. But
        // doing that would require this class to duplicate a large amount of
        // code from TeamTimeColumn.
        //
        // If we simply write a top-down estimate on the component, the time-
        // spreading logic in TeamTimeColumn will kick in the next time the WBS
        // Editor is opened. Since the scenario above seems extremely rare,
        // that approach seems acceptable.
        node.setNumericAttribute(TEAM_TIME_ATTR, newTime);
    }

    private String getProjDumpTimeString(WBSNode node) {
        StringBuffer result = new StringBuffer();
        for (int i : getAssignedPeople(node)) {
            double time = node.getNumericAttribute(indivTimeAttrs[i]);
            String timeStr = formatNumber(time);
            result.append(",").append(initials[i]).append("=").append(timeStr);
        }

        if (result.length() == 0)
            return null;

        result.append(",");
        return result.toString();
    }

    private String formatNumber(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d))
            return "0";
        else if (d == Math.floor(d))
            return Integer.toString((int) d);
        else
            return Double.toString(d);
    }


    private int[] getAssignedPeople(WBSNode node) {
        IntList result = new IntList();
        for (int i = indivTimeAttrs.length; i-- > 0;) {
            if (node.getAttribute(indivZeroAttrs[i]) != null
                    || node.getNumericAttribute(indivTimeAttrs[i]) > 0)
                result.add(i);
        }
        return result.getAsArray();
    }

    private static boolean isTask(WBSNode node) {
        return node != null && node.getType().endsWith("Task");
    }

    private static boolean eq(double a, double b) {
        return (Math.abs(a - b) < 0.0001);
    }

    private static final String TEAM_TIME_ATTR = "Time (Top Down)";

    private static final String TPP_ATTR = TeamTimeColumn.TPP_ATTR;

}
