// Copyright (C) 2002-2012 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelListener;


import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer.ActualSubtaskData;

public class TeamActualTimeColumn extends AbstractNumericColumn implements
        CalculatedDataColumn {

    public static final String COLUMN_ID = "Actual-Time";

    public static String getRemainingTimeAttr(TeamMember m) {
        return getRemainingTimeAttr(m.getInitials());
    }
    public static String getMilestonePlanTimeAttr(TeamMember m) {
        return getMilestonePlanTimeAttr(m.getInitials());
    }
    public static String getMilestoneRemainingTimeAttr(TeamMember m) {
        return getMilestoneRemainingTimeAttr(m.getInitials());
    }
    public static boolean hasActualTime(WBSNode node) {
        double time = node.getNumericAttribute(ACT_TIME_ATTR_NAME);
        return time > 0;
    }

    private static final String ACT_TIME_ATTR_NAME = "Actual_Team_Time";

    private DataTableModel dataModel;

    private WBSModel wbsModel;

    private TeamMemberList teamMembers;

    private int teamPlanTimeColumnNum = -1;

    private int teamSize;
    private String[] initials;
    private String[] nodeTimeAttrs;
    private String[] actTimeAttrs;
    private String[] completionDateAttrs;
    private String[] subtaskDataAttrs;
    private String[] planTimeAttrs;

    public TeamActualTimeColumn(DataTableModel dataModel,
            TeamMemberList teamMembers) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.teamMembers = teamMembers;
        this.columnID = COLUMN_ID;
        this.columnName = "Actual Time";
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID,
                MilestoneColumn.COLUMN_ID };

        dataModel.addDataColumn(new PercentSpentColumn());
        dataModel.addDataColumn(new PercentCompleteColumn());
        dataModel.addDataColumn(new TeamCompletionDateColumn());

        refreshTeam();
        teamMembers.addTableModelListener(EventHandler.create(
            TableModelListener.class, this, "reloadTeam"));
    }

    public void reloadTeam() {
        refreshTeam();
        dataModel.columnChanged(this);
    }

    private void refreshTeam() {
        List people = teamMembers.getTeamMembers();
        teamSize = people.size();
        initials = new String[teamSize];
        nodeTimeAttrs = new String[teamSize];
        actTimeAttrs = new String[teamSize];
        completionDateAttrs = new String[teamSize];
        subtaskDataAttrs = new String[teamSize];
        planTimeAttrs = new String[teamSize];

        for (int i = 0; i < initials.length; i++) {
            TeamMember m = (TeamMember) people.get(i);
            initials[i] = m.getInitials();
            nodeTimeAttrs[i] = TeamMemberActualTimeColumn
                    .getNodeDataAttrName(m);
            actTimeAttrs[i] = TeamMemberActualTimeColumn
                    .getResultDataAttrName(m);
            completionDateAttrs[i] = TeamCompletionDateColumn
                    .getMemberNodeDataAttrName(m);
            subtaskDataAttrs[i] = TeamMemberActualTimeColumn
                    .getSubtaskDataAttrName(m);
            planTimeAttrs[i] = TopDownBottomUpColumn
                    .getTopDownAttrName(TeamMemberTimeColumn.getColumnID(m));
        }
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (TeamTimeColumn.COLUMN_ID.equals(ID))
            teamPlanTimeColumnNum = columnNumber;
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        double time = node.getNumericAttribute(ACT_TIME_ATTR_NAME);
        return new NumericDataValue(time, false, !(time > 0), null);
    }

    public boolean recalculate() {
        TimeCalculator[] timeCalculators = createTimeCalculators();

        recalculate(wbsModel.getRoot(), new double[teamSize], timeCalculators,
            new double[1], new long[1]);

        for (int i = 0; i < teamSize; i++)
            timeCalculators[i].saveCalculatedValues(initials[i]);

        return true;
    }


    /** Recalculate data for a single node in the WBS.
     * 
     * With a single pass over the WBS, this method calculates actual time
     * for each team member and for the entire team; actual earned value,
     * completion date, percent complete, and percent spent.
     * 
     * @param node the node to recalculate
     * @param actualTime a result array, having one entry for each member of the
     *     team project.  This method should calculate (for each team member)
     *     the total actual time for this node and all children, and store the
     *     resulting value in corresponding field of this array.
     * @param earnedValue a single-entry result array.  This method should
     *     calculate the team earned value (for this node and all children,
     *     in hours), and return the result in the single field of this array.
     * @param completionDate a single-entry result array.  This method should
     *     calculate the actual completion date of this node and all children,
     *     and return the result in the single field of this array.
     */
    private void recalculate(WBSNode node, double[] actualTime,
            TimeCalculator[] timeCalc, double[] earnedValue,
            long[] completionDate) {
        // get the list of children underneath this node
        WBSNode[] children = wbsModel.getChildren(node);
        boolean isLeaf = (children.length == 0);

        // load the actual node time for each individual into our working array
        for (int i = 0; i < teamSize; i++)
            actualTime[i] = nanToZero(node.getNumericAttribute(nodeTimeAttrs[i]));

        earnedValue[0] = 0;
        completionDate[0] = COMPL_DATE_NA;
        if (isLeaf) {
            int milestone = MilestoneColumn.getMilestoneID(node);
            // accumulate EV and completion date information for this leaf
            for (int i = 0; i < teamSize; i++) {
                // retrieve the planned time for one team member.
                double memberPlanTime = nanToZero(node
                        .getNumericAttribute(planTimeAttrs[i]));
                if (memberPlanTime > 0) {
                    // if this team member is assigned to this leaf task, get
                    // their actual completion date for the task.
                    Date memberCompletionDate = (Date) node
                            .getAttribute(completionDateAttrs[i]);
                    // keep track of the max completion date so far.
                    completionDate[0] = mergeCompletionDate(completionDate[0],
                        memberCompletionDate);
                    // if this individual has completed this task, then the
                    // team has earned the value associated with the task.
                    if (memberCompletionDate != null) {
                        earnedValue[0] += memberPlanTime;
                        timeCalc[i].addCompletedTask(memberPlanTime,
                            actualTime[i], milestone);
                    } else {
                        // See if subtask data is present for this task
                        List<ActualSubtaskData> subtaskData = (List) node
                                .getAttribute(subtaskDataAttrs[i]);
                        if (subtaskData != null && !subtaskData.isEmpty()) {
                            // if subtask data is present, record each subtask
                            // as an independent task
                            for (ActualSubtaskData subtask : subtaskData) {
                                if (subtask.getCompletionDate() != null) {
                                    // this subtask was completed
                                    earnedValue[0] += subtask.getPlanTime();
                                    timeCalc[i].addCompletedTask(
                                        subtask.getPlanTime(),
                                        subtask.getActualTime(), milestone);
                                } else {
                                    // this subtask is remaining
                                    timeCalc[i].addRemainingTask(
                                        subtask.getPlanTime(),
                                        subtask.getActualTime(), milestone);
                                }
                            }
                        } else {
                            // there is no subtask data for this node. Just
                            // record a plain remaining task.
                            timeCalc[i].addRemainingTask(memberPlanTime,
                                actualTime[i], milestone);
                        }
                    }
                }
            }

        } else {
            double[] childTime = new double[teamSize];
            double[] childEarnedValue = new double[1];
            long[] childCompletionDate = new long[1];
            for (int i = 0; i < children.length; i++) {
                // ask our child to compute its time data
                recalculate(children[i], childTime, timeCalc,
                    childEarnedValue, childCompletionDate);
                // now accumulate time from that child into our total
                for (int j = 0; j < teamSize; j++)
                    actualTime[j] += childTime[j];
                // accumulate EV related data from our children
                earnedValue[0] += childEarnedValue[0];
                completionDate[0] = Math.max(completionDate[0],
                    childCompletionDate[0]);
            }
        }

        double totalActualTime = 0;
        for (int i = 0; i < teamSize; i++) {
            // add up the actual time for the entire team
            totalActualTime += actualTime[i];
            // also store the total time per individual for this node
            node.setNumericAttribute(actTimeAttrs[i], actualTime[i]);
        }
        // store the actual time for the entire team for this node.
        node.setNumericAttribute(ACT_TIME_ATTR_NAME, totalActualTime);

        // retrieve the total plan time for this node from the TeamTimeColumn.
        double totalPlanTime = nanToZero(NumericDataValue.parse(dataModel
                .getValueAt(node, teamPlanTimeColumnNum)));

        // calculate and store the percent spent
        double percentSpent = totalActualTime / totalPlanTime;
        node.setNumericAttribute(PercentSpentColumn.RESULT_ATTR, percentSpent);

        // calculate and store the percent complete
        double percentComplete = earnedValue[0] / totalPlanTime;
        node.setNumericAttribute(PercentCompleteColumn.RESULT_ATTR,
            percentComplete);

        // store the calculated completion date
        Date cd = null;
        if (completionDate[0] != COMPL_DATE_NA
                && completionDate[0] != INCOMPLETE)
            cd = new Date(completionDate[0]);
        node.setAttribute(TeamCompletionDateColumn.ATTR_NAME, cd);
    }

    private static double nanToZero(double d) {
        return (Double.isNaN(d) ? 0 : d);
    }

    private static final long COMPL_DATE_NA = -1;
    private static final long INCOMPLETE = Long.MAX_VALUE;

    private static long mergeCompletionDate(long a, Date b) {
        if (b == null)
            return INCOMPLETE;
        else
            return Math.max(a, b.getTime());
    }

    private TimeCalculator[] createTimeCalculators() {
        TimeCalculator[] result = new TimeCalculator[teamSize];
        for (int i = 0; i < result.length; i++)
            result[i] = new TimeCalculator();
        return result;
    }

    private class TimeCalculator {

        private List<TaskData> completedTasks;
        private List<TaskData> remainingTasks;
        private double underspentTime;
        private double overspentTime;

        public TimeCalculator() {
            completedTasks = new ArrayList<TaskData>();
            remainingTasks = new ArrayList<TaskData>();
            underspentTime = overspentTime = 0;
        }

        public void addCompletedTask(double planTime, double actualTime, int milestone) {
            completedTasks.add(new TaskData(planTime, actualTime, milestone, true));
        }

        public void addRemainingTask(double planTime, double actualTime, int milestone) {
            remainingTasks.add(new TaskData(planTime, actualTime, milestone, false));
        }

        public void saveCalculatedValues(String initials) {
            double adjustmentRatio = - overspentTime / underspentTime;
            adjustmentRatio = Math.min(adjustmentRatio, MAX_ADJUSTMENT_RATIO);

            double cumRemainingTime = 0;
            Map<Integer, Double> milestonePlanTime = new HashMap<Integer, Double>();
            Map<Integer, Double> milestoneRemainingTime = new HashMap<Integer, Double>();

            for (TaskData task : completedTasks) {
                addMilestoneData(milestonePlanTime, task.milestone,
                    task.planTime);
            }

            for (TaskData task : remainingTasks) {
                task.setDeltaRatio(adjustmentRatio);
                addMilestoneData(milestonePlanTime, task.milestone,
                    task.planTime);
                addMilestoneData(milestoneRemainingTime, task.milestone,
                    task.timeRemaining);
                cumRemainingTime += task.timeRemaining;
            }

            wbsModel.getRoot().setNumericAttribute(
                getRemainingTimeAttr(initials), cumRemainingTime);
            wbsModel.getRoot().setAttribute(
                getMilestonePlanTimeAttr(initials), milestonePlanTime);
            wbsModel.getRoot().setAttribute(
                getMilestoneRemainingTimeAttr(initials), milestoneRemainingTime);
        }

        private void addMilestoneData(Map<Integer, Double> data, int milestone,
                double value) {
            Double current = data.get(milestone);
            if (current == null)
                data.put(milestone, value);
            else
                data.put(milestone, current + value);
        }

        private class TaskData {

            /** The user's planned time for this task */
            private double planTime;
            /** The actual time logged against this task to date */
            private double actualTime;
            /** The milestone this task is assigned to */
            private int milestone;

            /** The projected cost for this task, calculated by assuming that
             * the task is "almost done" */
            double almostDoneCost;
            double delta;
            double timeRemaining;

            public TaskData(double planTime, double actualTime, int milestone,
                    boolean isComplete) {

                this.planTime = planTime;
                this.actualTime = actualTime;
                this.milestone = milestone;
                almostDoneCost = actualTime / ALMOST_DONE_PERCENTAGE;
                delta = planTime - almostDoneCost;
                if (!isComplete) {
                    if (delta > 0)
                        underspentTime += delta;
                    else
                        overspentTime += delta;
                }
            }

            public void setDeltaRatio(double deltaRatio) {
                if (delta > 0)
                    timeRemaining = planTime - (delta * deltaRatio) - actualTime;
                else
                    timeRemaining = almostDoneCost - actualTime;
            }

        }

    }

    private static final double ALMOST_DONE_PERCENTAGE = 0.9;

    private static final double MAX_ADJUSTMENT_RATIO = 0.2;

    private static String getRemainingTimeAttr(String initials) {
        return ('_' + initials + "-Remaining_Time").intern();
    }
    private static String getMilestonePlanTimeAttr(String initials) {
        return ('_' + initials + "-Milestone_Plan_Time").intern();
    }
    private static String getMilestoneRemainingTimeAttr(String initials) {
        return ('_' + initials + "-Milestone_Remaining_Time").intern();
    }
}
