package teamdash.wbs.columns;

import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.event.TableModelListener;


import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class TeamActualTimeColumn extends AbstractNumericColumn implements
        CalculatedDataColumn {

    public static final String COLUMN_ID = "Actual-Time";

    public static String getRemainingTimeAttr(TeamMember m) {
        return getRemainingTimeAttr(m.getInitials());
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
    private String[] planTimeAttrs;

    public TeamActualTimeColumn(DataTableModel dataModel,
            TeamMemberList teamMembers) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.teamMembers = teamMembers;
        this.columnID = COLUMN_ID;
        this.columnName = "Actual Time";
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID };

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
        RemainingTimeCalculator[] remainingTime =
            createRemainingTimeCalculators();

        recalculate(wbsModel.getRoot(), new double[teamSize], remainingTime,
            new double[1], new long[1]);

        for (int i = 0; i < teamSize; i++)
            wbsModel.getRoot().setNumericAttribute(
                getRemainingTimeAttr(initials[i]),
                remainingTime[i].getTotalRemainingTime());

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
            RemainingTimeCalculator[] remainingTime, double[] earnedValue,
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
                    if (memberCompletionDate != null)
                        earnedValue[0] += memberPlanTime;
                    else
                        remainingTime[i].addTask(memberPlanTime, actualTime[i]);
                }
            }

        } else {
            double[] childTime = new double[teamSize];
            double[] childEarnedValue = new double[1];
            long[] childCompletionDate = new long[1];
            for (int i = 0; i < children.length; i++) {
                // ask our child to compute its time data
                recalculate(children[i], childTime, remainingTime,
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

    private RemainingTimeCalculator[] createRemainingTimeCalculators() {
        RemainingTimeCalculator[] result = new RemainingTimeCalculator[teamSize];
        for (int i = 0; i < result.length; i++)
            result[i] = new RemainingTimeCalculator();
        return result;
    }

    private class RemainingTimeCalculator {

        private List<TaskData> remainingTasks;
        private double underspentTime;
        private double overspentTime;

        public RemainingTimeCalculator() {
            remainingTasks = new ArrayList<TaskData>();
            underspentTime = overspentTime = 0;
        }

        public void addTask(double planTime, double actualTime) {
            remainingTasks.add(new TaskData(planTime, actualTime));
        }

        public double getTotalRemainingTime() {
            double adjustmentRatio = - overspentTime / underspentTime;
            adjustmentRatio = Math.min(adjustmentRatio, MAX_ADJUSTMENT_RATIO);

            double cumRemainingTime = 0;
            for (TaskData task : remainingTasks)
                cumRemainingTime += task.getTimeRemaining(adjustmentRatio);
            return cumRemainingTime;
        }

        private class TaskData {

            /** The user's planned time for this task */
            private double planTime;
            /** The actual time logged against this task to date */
            private double actualTime;

            /** The projected cost for this task, calculated by assuming that
             * the task is "almost done" */
            double almostDoneCost;
            double delta;

            public TaskData(double planTime, double actualTime) {

                this.planTime = planTime;
                this.actualTime = actualTime;
                almostDoneCost = actualTime / ALMOST_DONE_PERCENTAGE;
                delta = planTime - almostDoneCost;
                if (delta > 0)
                    underspentTime += delta;
                else
                    overspentTime += delta;
            }

            public double getTimeRemaining(double deltaRatio) {
                if (delta > 0)
                    return planTime - (delta * deltaRatio) - actualTime;
                else
                    return almostDoneCost - actualTime;
            }

        }

    }

    private static final double ALMOST_DONE_PERCENTAGE = 0.9;

    private static final double MAX_ADJUSTMENT_RATIO = 0.2;

    private static String getRemainingTimeAttr(String initials) {
        return initials + "-Remaining_Time";
    }
}
