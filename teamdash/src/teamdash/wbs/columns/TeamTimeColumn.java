// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.wbs.AnnotatedValue;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.HtmlRenderedValue;
import teamdash.wbs.IntList;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowModel;

/** This column manages the calculation and interrelationship of several
 * tightly related columns dealing with team time.
 */
public class TeamTimeColumn extends TopDownBottomUpColumn implements ChangeListener {

    public static final String COLUMN_ID = "Time";

    int sizeColumn = -1;
    int unitsColumn = -1;
    IntList teamMemberColumns;
    String[] teamMemberInitials;
    RateColumn rateColumn;
    TimePerPersonColumn timePerPersonColumn;
    NumPeopleColumn numPeopleColumn;
    ResourcesColumn resourcesColumn;
    TeamTimeNoErrorColumn noErrorColumn;
    UnassignedTimeColumn unassignedTimeColumn;


    public TeamTimeColumn(DataTableModel m) {
        super(m, "Time", COLUMN_ID);
        this.dependentColumns = new String[] { "Task Size", "Task Size Units" };
        this.setTeamMemberColumns(new IntList());
        this.preferredWidth = 55;
        setConflictAttributeName(topDownAttrName);

        // create and add our interrelated columns.
        m.addDataColumn(rateColumn = new RateColumn());
        m.addDataColumn(timePerPersonColumn = new TimePerPersonColumn());
        m.addDataColumn(numPeopleColumn = new NumPeopleColumn());
        m.addDataColumn(resourcesColumn = new ResourcesColumn());
        m.addDataColumn(noErrorColumn = new TeamTimeNoErrorColumn());
        m.addDataColumn(unassignedTimeColumn = new UnassignedTimeColumn(m));

        m.addTeamMemberColumnListener(this);
    }


    public void storeDependentColumn(String ID, int columnNumber) {
        if ("Task Size".equals(ID))
            sizeColumn = columnNumber;
        else if ("Task Size Units".equals(ID)) {
            unitsColumn = columnNumber;
            setTeamMemberColumns(dataModel.getTeamMemberColumnIDs());
        }
    }

    // messaged when the list of team member columns changes
    public void stateChanged(ChangeEvent e) {
        IntList newCols = dataModel.getTeamMemberColumnIDs();
        if (!newCols.equals(teamMemberColumns)) {
            setTeamMemberColumns(newCols);
            dataModel.columnChanged(this);
        }
    }


    public void resetDependentColumns() {
        sizeColumn = unitsColumn = -1;
        setTeamMemberColumns(new IntList());
    }


    private void setTeamMemberColumns(IntList newCols) {
        teamMemberColumns = newCols;

        teamMemberInitials = new String[newCols.size()];
        for (int i = 0; i < teamMemberInitials.length; i++)
            teamMemberInitials[i] = dataModel.getColumnName(newCols.get(i));
    }


    public Object getValueAt(WBSNode node) {
        return getValueAt(node, false);
    }

    public Object getValueAt(WBSNode node, boolean ignoreErrors) {
        NumericDataValue result = (NumericDataValue) super.getValueAt(node);
        if (result == null || result.errorMessage != null || ignoreErrors)
            return result;

        boolean needsAssigning = false;
        boolean needsEstimating = false;
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null) { // a leaf task
            needsAssigning = !leafData.isConnected();
            needsEstimating = (result.value == 0);
        } else if (wbsModel.isLeaf(node)) { // a leaf node which isn't a task
            if (safe(result.value) != 0) {
                result.errorMessage =
                    "You need to create tasks underneath this "
                    + node.getType().toLowerCase();
                result.errorColor = Color.blue;
            }

        } else { // not a leaf task
            needsAssigning = !equal(result.value, sumIndivTimes(node));
            needsEstimating = (result.value == 0);
        }

        if (needsEstimating) {
            result.errorMessage = "Task time needs to be estimated";
            result.errorColor = Color.blue;
        } else if (needsAssigning) {
            result.errorMessage = "Task needs to be assigned to individual(s)";
            result.errorColor = Color.blue;
        }

        return result;
    }




    protected double recalc(WBSNode node) {
        // this could be called because:
        // 1) the user edited the team time, and we're recalculating
        // 2) the user edited the Size column, and we're recalculating
        // 3) the user edited an individual time column, and we're recalcing

        // if this is a leaf, automatically reset its top-down estimate to
        // match the "teamTime" value calculated by the associated LeafNodeData
        // object.
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null) {
            leafData.recalc();
            if (safe(leafData.teamTime) == 0)
                node.setAttribute(topDownAttrName, null);
            else
                node.setNumericAttribute(topDownAttrName, leafData.teamTime);
        } else {
            double teamTime = 0;
            // If this is a leaf component (not a task), any time attached to
            // it will be unassigned time.
            if (wbsModel.isLeaf(node))
                teamTime = safe(node.getNumericAttribute(topDownAttrName));
            if (teamTime == 0)
                unassignedTimeColumn.clearUnassignedTime(node);
            else
                unassignedTimeColumn.setUnassignedTime(node, teamTime);
        }

        // Then, recalculate as usual.
        return super.recalc(node);
    }


    /** When the user edits and changes a value, this function is called
     * for each affected node before the change is made. */
    protected void userChangingValue(WBSNode node, double value) {
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null)
            // if this is a leaf node, pass this notification along to the
            // LeafNodeData object so it can take the appropriate action
            // (e.g., scaling the personal times appropriately)
            leafData.userSetTeamTime(value);

        else if (Double.isNaN(value))
            // if this is NOT a leaf node, and the user deleted the number
            // from the "Team Time" column, they are probably trying to clear
            // a top-down-bottom-up mismatch.  Oblige by deleting the top-down
            // estimates for each individual team member.
            clearIndividualTopDownBottomUpMismatches(node);
    }

    private void clearIndividualTopDownBottomUpMismatches(WBSNode node) {
        for (int i = teamMemberColumns.size();  i-- > 0; ) {
            int oneMemberColumn = teamMemberColumns.get(i);
            dataModel.setValueAt("", node, oneMemberColumn);
        }
    }


    @Override
    protected boolean attemptToRepairTopDownBottomUpMismatch(WBSNode node,
            double topDownValue, double bottomUpValue, WBSNode[] children,
            int numToInclude) {
        // our goal with this method is to subdivide the time across the tasks
        // in a workflow when the user is using plain percentages without
        // associated historical productivity rates.

        // To detect this scenario, we first ensure that we have a top-down
        // estimate and multiple children with no time assigned.
        if (!(topDownValue > 0) || bottomUpValue > 0 || numToInclude < 2)
            return false;

        // Check to see if a workflow has been applied to this (parent) node.
        if (node.getAttribute(WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR) == null)
            return false;

        // Next, we get a list of the leaf tasks underneath this node, and add
        // up the "Workflow Percentage" values for each one.  (Basically, we
        // want to do the "right thing," even if the user made a mistake and
        // their numbers don't add to 100%.)
        ArrayList<WBSNode> leaves = new ArrayList();
        getLeavesForNode(node, false, leaves);
        double totalPercent = 0;
        for (WBSNode leaf : leaves) {
            totalPercent += WorkflowPercentageColumn
                    .getExplicitValueForNode(leaf);

            // Check for historical productivity rates on the nodes; those
            // would indicate that this is not a percentage-driven workflow.
            if (leaf.getNumericAttribute(RATE_ATTR) > 0)
                return false;
        }
        // If the percentages add up to zero, this isn't a percentage-driven
        // workflow insertion scenario.
        if (!(totalPercent > 0))
            return false;

        // At this point, we appear to have a valid workflow insertion
        // scenario.  Subdivide the time over the leaf tasks.
        for (WBSNode leaf : leaves) {
            double leafPercent = WorkflowPercentageColumn
                    .getExplicitValueForNode(leaf);
            double leafTime = topDownValue * leafPercent / totalPercent;
            userChangingValue(leaf, leafTime);
            leaf.setNumericAttribute(topDownAttrName, leafTime);
        }
        return true;
    }

    public void replanInProgressTime() {
        for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot())) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null)
                leafData.replanInProgressTime();
        }
    }




    /** Get an array of IndivTime objects representing the amount of task time
     * each individual has for the given wbs node. */
    protected IndivTime[] getIndivTimes(WBSNode node) {
        IndivTime[] result = new IndivTime[teamMemberColumns.size()];
        for (int i = teamMemberColumns.size();   i-- > 0; )
            result[i] = new IndivTime(node, teamMemberColumns.get(i),
                    teamMemberInitials[i]);
        return result;
    }



    /** Add up the task times for each individual at the given node */
    private double sumIndivTimes(WBSNode node) {
        return sumIndivTimes(getIndivTimes(node));
    }

    /** Add up the task times for each IndivTime object in the given array. */
    private double sumIndivTimes(IndivTime[] individualTimes) {
        double result = 0;
        for (int i = 0;   i < individualTimes.length;   i++)
            result += individualTimes[i].time;
        return result;
    }


    /** Count the number of assigned individuals in the given array. */
    private int countPeople(IndivTime[] indivTimes) {
        int count = 0;
        for (int i = 0;   i < indivTimes.length;   i++)
            if (indivTimes[i].isAssigned()) count++;
        return count;
    }



    /** Convenience method */
    private static double parse(Object o) {
        return NumericDataValue.parse(o);
    }


    /** Compare two (possibly null) strings for equality */
    private boolean equal(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }


    /** filter out infinite/NaN values, replacing them with 0. */
    private double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v) ? 0 : v);
    }


    /** Find the most frequently occurring nonzero task time in the given
     * array of IndivTime objects.
     * 
     * @return the most frequently occurring task time, or zero if there is a
     * tie for the most frequently occurring time. */
    private double getMode(IndivTime[] indivTimes) {
        double[] d = new double[indivTimes.length];
        for (int i = d.length;   i-- > 0; )
            d[i] = indivTimes[i].time;
        return getMode(d);
    }

    /** Find the most frequently occurring nonzero number in the array.
     * 
     * Note: this is a destructive operation; it modifies the array passed in.
     * 
     * @return the most frequently occurring nonzero number in the array.  if
     * all numbers are zero, or if there is a "tie" for the most frequently
     * occurring number, returns zero. */
    private double getMode(double[] d) {
        double result = 0;
        int count = 0;
        boolean tie = false;

        for (int i = 0;   i < d.length;   i++) {
            if (safe(d[i]) == 0) continue;

            int thisCount = 1;
            for (int j = i+1;   j < d.length;   j++)
                if (equal(d[i], d[j])) {
                    thisCount++;
                    d[j] = 0;
                }

            if (thisCount > count) {
                result = d[i];
                count = thisCount;
                tie = false;
            } else if (thisCount == count) {
                tie = true;
            }
        }

        return tie ? 0 : result;
    }



    /** Get the LeafNodeData object for a particular node.
     * 
     * If the node is a leaf, this will create a LeafNodeData object if
     * necessary, or return the existing LeafNodeData object if one exists.
     * If the node is not a leaf, this will return null. */
    protected LeafNodeData getLeafNodeData(WBSNode node) {
        if (!wbsModel.isLeaf(node) || !node.getType().endsWith("Task")) {
            node.setAttribute(DATA_ATTR_NAME, null);
            return null;
        }

        LeafNodeData result =
            (LeafNodeData) node.getAttribute(DATA_ATTR_NAME);
        if (result == null) {
            result = new LeafNodeData(node);
            node.setAttribute(DATA_ATTR_NAME, result);
        }
        return result;
    }




    /** Manages the relationships between data values at a leaf node.
     * 
     * As data values change, determines how best to recalculate other data
     * values. */
    private class LeafNodeData {
        WBSNode node;
        double size, rate, timePerPerson, numPeople, actualNumPeople,
                unassignedTime, teamTime;
        String units;
        IndivTime[] individualTimes;

        public LeafNodeData(WBSNode node) {
            this.node = node;
            size = parse(dataModel.getValueAt(node, sizeColumn));
            units = String.valueOf(dataModel.getValueAt(node, unitsColumn));
            individualTimes = getIndivTimes(node);
            maybeSetAutoZeroUser();
            figureTimePerPerson();
            figureNumPeople();
            figureTeamTime();
        }

        private void maybeSetAutoZeroUser() {
            // check to see if an auto zero user has been set using either
            // a persistent or transient attribute
            String autoZeroInitials = (String) node.removeAttribute(
                    AUTO_ZERO_USER_ATTR_PERSISTENT);
            if (autoZeroInitials == null)
                autoZeroInitials = (String) node.removeAttribute(
                    AUTO_ZERO_USER_ATTR_TRANSIENT);
            if (autoZeroInitials == null)
                return;

            // if an auto zero user has been registered, look for someone
            // with those initials in our list of individuals
            IndivTime targetIndiv = null;
            for (IndivTime oneIndiv : individualTimes) {
                if (oneIndiv.isAssigned())
                    return;
                else if (autoZeroInitials.equalsIgnoreCase(oneIndiv.initials))
                    targetIndiv = oneIndiv;
            }

            // if we found a matching individual, set them as an "assigned
            // but zero" user.
            if (targetIndiv != null)
                targetIndiv.setTime(Collections.singletonMap(autoZeroInitials
                        .toLowerCase(), 0));
        }

        void figureTimePerPerson() {
            // the default way to calculate time per person is by finding
            // the mode of the individual times
            timePerPerson = getMode(individualTimes);

            // if that didn't work, calculate time per person from rate and
            // size.
            if (timePerPerson == 0) {
                rate = node.getNumericAttribute(RATE_ATTR);
                timePerPerson = safe(size / rate);
            }

            // if that didn't work, revert to the last known good time
            // per person
            if (timePerPerson == 0)
                timePerPerson = safe(node.getNumericAttribute(TPP_ATTR));

            // save the value we came up with for time per person
            node.setNumericAttribute(TPP_ATTR, timePerPerson);

            // possibly recalculate the effective rate.
            recalculateRate();
        }

        void figureNumPeople() {
            numPeople = (int) safe(node.getNumericAttribute(NUM_PEOPLE_ATTR));
            actualNumPeople = countPeople(individualTimes);

            numPeople = Math.max(numPeople, 1);
            numPeople = Math.max(numPeople, actualNumPeople);
        }

        void figureTeamTime() {
            unassignedTime = (numPeople - actualNumPeople) * timePerPerson;
            unassignedTimeColumn.setUnassignedTime(node, unassignedTime);
            teamTime = sumIndivTimes() + unassignedTime;
        }


        public boolean isConnected() {
            return (numPeople == actualNumPeople);
        }

        public double sumIndivTimes() {
            double result = 0;
            for (int i = 0;   i < individualTimes.length;   i++)
                result += individualTimes[i].time;
            return result;
        }

        public void recalc() {
            double newSize = parse(dataModel.getValueAt(node, sizeColumn));
            String newUnits =
                String.valueOf(dataModel.getValueAt(node, unitsColumn));

            if (!equal(units, newUnits)) {
                size = newSize;
                units = newUnits;
                recalculateRate();
            } else if (!equal(size, newSize)) {
                userSetSize(newSize);
            } else {
                individualTimes = getIndivTimes(node);
                figureTimePerPerson();
                figureNumPeople();
                figureTeamTime();
                dataModel.columnChanged(timePerPersonColumn);
                dataModel.columnChanged(rateColumn);
                dataModel.columnChanged(numPeopleColumn);
                dataModel.columnChanged(resourcesColumn);
                dataModel.columnChanged(TeamTimeColumn.this);
            }
        }

        public void userSetSize(double value) {
            size = value;

            double savedRate = node.getNumericAttribute(RATE_ATTR);
            if (safe(savedRate) != 0 && safe(size) != 0)
                // if there is a saved value for the rate, and the size value
                // entered is meaningful, we should recalculate the time per
                // person based upon that rate.
                userSetTimePerPerson(size / savedRate);
            else
                // if there is no saved value for the rate, recalculate the
                // effective rate based upon the new size.
                recalculateRate();
        }

        protected void recalculateRate() {
            // is there a saved value for rate?
            double oldRate = node.getNumericAttribute(RATE_ATTR);

            if (safe(size) == 0 && safe(timePerPerson) == 0) {
                // if both the size and the time per person are missing,
                // (which is often the case when workflows are first inserted
                // into the hierarchy, if the size has not yet been entered),
                // just keep and display the saved rate
                rate = oldRate;

            } else {
                // calculate the current effective rate.  If the effective
                // rate cannot be calculated, use NaN.
                rate = size / timePerPerson;
                if (safe(rate) == 0) rate = Double.NaN;

                // if the current effective rate and the saved rate are not
                // equal, it implies that someone has edited the time value to
                // disagree with the saved rate.  In response, we'll erase the
                // saved value, to prevent it from being used to recalculate
                // time if the user edits size in the future.
                if (!equal(rate, oldRate)) {
                    node.setAttribute(RATE_ATTR, null);
                    dataModel.columnChanged(rateColumn);
                }
            }
        }

        public boolean isRateCalculated() {
            return (safe(rate) != 0 && node.getAttribute(RATE_ATTR) == null);
        }

        public void userSetRate(double value) {
            if (safe(value) == 0) {
                // the user is zeroing or blanking out the rate field.
                node.setAttribute(RATE_ATTR, null);
                recalculateRate();
            } else {
                rate = value;
                node.setNumericAttribute(RATE_ATTR, rate);
                userSetTimePerPerson(size / rate);
            }
        }

        /** Messaged when the user (directly or indirectly) alters the time
         * per person.  */
        public void userSetTimePerPerson(double value) {
            // if nothing has changed, avoid the pain of recalculating.
            if (value == timePerPerson) return;

            double oldTimePerPerson = timePerPerson;
            timePerPerson = value;
            node.setNumericAttribute(TPP_ATTR, timePerPerson);
            recalculateRate();

            // if the time per person changed to zero, don't try
            // to proportionally propagate the change along.
            if (safe(timePerPerson) > 0) {

                // find individuals with that amount of time, and update them.
                for (int i = individualTimes.length;   i-- > 0; ) {
                    if (individualTimes[i].isAssigned() &&
                            equal(individualTimes[i].time, oldTimePerPerson))
                        individualTimes[i].setTime(timePerPerson);
                }
                individualTimes = getIndivTimes(node);
            }

            // recalculate team time and register the change.
            figureTeamTime();
            dataModel.columnChanged(TeamTimeColumn.this);
            dataModel.columnChanged(timePerPersonColumn);
        }

        /** Messaged when the user edits the # of people for a leaf node. */
        public void userSetNumPeople(double value) {
            if (safe(value) > 0 && value >= actualNumPeople) {
                node.setNumericAttribute(NUM_PEOPLE_ATTR, value);
                figureNumPeople();
                figureTeamTime();
                dataModel.columnChanged(numPeopleColumn);
                dataModel.columnChanged(TeamTimeColumn.this);
            }
        }

        /** Messaged when the user edits the team time for a leaf node. */
        public void userSetTeamTime(double value) {
            double oldTeamTime = teamTime;
            teamTime = value;
            if (oldTeamTime == 0)
                userSetTimePerPerson(teamTime / numPeople);
            else {
                double ratio = teamTime / oldTeamTime;
                timePerPerson *= ratio;
                node.setNumericAttribute(TPP_ATTR, timePerPerson);
                dataModel.columnChanged(timePerPersonColumn);

                recalculateRate();

                // find individuals with nonzero time, and update them.
                for (int i = individualTimes.length;   i-- > 0; )
                    individualTimes[i].multiplyTime(ratio);
            }
            figureTeamTime();
        }

        public void replanInProgressTime() {
            for (int i = individualTimes.length;   i-- > 0; )
                individualTimes[i].replanInProgressTime();
        }
    }


    /** This class holds the task time for one individual */
    private class IndivTime {
        WBSNode node;
        double time;
        int column;
        String initials;
        String zeroAttrName;
        boolean zeroButAssigned;
        boolean completed;

        public IndivTime(WBSNode node, int column, String initials) {
            this.node = node;
            this.column = column;
            this.time = safe(parse(dataModel.getValueAt(node, column)));
            this.initials = initials;
            this.zeroAttrName = getMemberAssignedZeroAttrName(this.initials);
            this.zeroButAssigned = (node.getAttribute(zeroAttrName) != null);

            String completedAttrName = TeamCompletionDateColumn
                    .getMemberNodeDataAttrName(this.initials);
            this.completed = (node.getAttribute(completedAttrName) != null);
        }

        public boolean isAssigned() {
            return time > 0 || zeroButAssigned;
        }

        public void setTime(double newTime) {
            this.time = newTime;
            this.zeroButAssigned = false;
            node.setAttribute(zeroAttrName, null);
            dataModel.setValueAt(new Double(time), node, column);
        }

        public void multiplyTime(double ratio) {
            if (time > 0)
                setTime(time * ratio);
        }
        public StringBuffer appendTimeString(StringBuffer buf,
                                             double defaultTime,
                                             boolean html) {
            if (html && completed)
                buf.append("<strike>");
            buf.append(initials);
            if (!equal(time, defaultTime))
                buf.append("(").append(NumericDataValue.format(time)).append(")");
            if (html && completed)
                buf.append("</strike>");
            return buf;
        }
        public void setTime(Map times) {
            String ilc = initials.toLowerCase();
            Object timeVal = times.get(ilc);
            if (timeVal instanceof String) {
                // if the user was editing the value in the "Assigned To"
                // column, but they did not alter the textual representation
                // of the time for this individual, make no changes.
                String oldTimeVal = NumericDataValue.format(this.time);
                if (timeVal.equals(oldTimeVal))
                    return;
            }

            this.time = safe(parse(timeVal));
            this.zeroButAssigned = (time == 0 && times.containsKey(ilc));
            node.setAttribute(zeroAttrName, zeroButAssigned ? "t" : null);
            dataModel.setValueAt(new Double(time), node, column);
        }
        public void replanInProgressTime() {
            if (isAssigned() == false) {
                // this person is not assigned to this task
            } else if (completed) {
                // if this person has completed this task, unassign it.
                setTime(0);
            } else {
                // subtract the actual time from the plan to calculate planned
                // time remaining. If the remaining time is positive, set it.
                // Otherwise (if they are overspent), set the plan as "assigned
                // with zero."
                String actualTimeAttr = TeamMemberActualTimeColumn
                        .getResultDataAttrName(initials);
                double actual = safe(node.getNumericAttribute(actualTimeAttr));
                if (actual > 0) {
                    double plannedTimeRemaining = Math.max(0, time - actual);
                    setTime(Collections.singletonMap(initials.toLowerCase(),
                        plannedTimeRemaining));
                }
            }
        }
    }



    /** Base class for columns related to Team Time. */
    private abstract class DependentColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        public DependentColumn(String id) {
            this(id, id);
        }
        public DependentColumn(String name, String id) {
            this.columnName = name;
            this.columnID = id;
        }

        public boolean isCellEditable(WBSNode node) {
            // default behavior: only leaf nodes are editable.
            return (getLeafNodeData(node) != null);
        }

        public Object getValueAt(WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null)
                return getValueAtLeaf(leafData);
            else
                return getValueAtNode(node);
        }
        protected abstract Object getValueAtLeaf(LeafNodeData nodeData);
        protected Object getValueAtNode(WBSNode node) {
            return BLANK;
        }

        protected void setValueForNode(double value, WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null)
                setValueAtLeaf(value, leafData);
            else
                setValueAtNode(value, node);
        }
        protected abstract void setValueAtLeaf(double value, LeafNodeData nd);
        protected void setValueAtNode(double value, WBSNode node) { }

        public boolean recalculate() { return true; }
        public void storeDependentColumn(String ID, int columnNumber) {}
    }



    /** A column representing the rate at which a task is performed */
    private class RateColumn extends DependentColumn
        implements CustomRenderedColumn
    {
        public RateColumn() {
            super("Rate");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafNodeData nodeData) {
            if (nodeData.isRateCalculated())
                return new NumericDataValue(nodeData.rate, true, false,
                                            EFFECTIVE_RATE_MESSAGE);
            else
                return new NumericDataValue(nodeData.rate);
        }
        protected void setValueAtLeaf(double value, LeafNodeData nodeData) {
            if (value != nodeData.rate)
                nodeData.userSetRate(value);
        }
        public TableCellRenderer getCellRenderer() {
            return RATE_RENDERER;
        }
    }

    private static final String EFFECTIVE_RATE_MESSAGE =
        "Effective (calculated) rate";
    private static final TableCellRenderer RATE_RENDERER =
        new ItalicNumericCellRenderer(EFFECTIVE_RATE_MESSAGE);


    /** A column representing the task time per individual */
    private class TimePerPersonColumn extends DependentColumn {
        public TimePerPersonColumn() {
            super("Hrs/Indiv", "Time Per Person");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafNodeData nodeData) {
            return new NumericDataValue(nodeData.timePerPerson);
        }
        protected void setValueAtLeaf(double value, LeafNodeData nodeData) {
            nodeData.userSetTimePerPerson(value);
        }
    }



    /** A column representing the number of people assigned to a task. */
    private class NumPeopleColumn extends DependentColumn {
        public NumPeopleColumn() {
            super("# People", "Number of People");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafNodeData nodeData) {
            return new NumericDataValue(nodeData.numPeople);
        }
        //protected Object getValueAtNode(WBSNode node) {
        //    return new NumericDataValue(countPeople(node), false);
        //}
        protected void setValueAtLeaf(double value, LeafNodeData nodeData) {
            nodeData.userSetNumPeople(value);
        }
    }



    /** A column representing the initials of people assigned to a task. */
    private class ResourcesColumn extends AbstractDataColumn
            implements CalculatedDataColumn
    {
        public ResourcesColumn() {
            this.columnID = this.columnName = "Assigned To";
            this.preferredWidth = 200;
        }

        public boolean isCellEditable(WBSNode node) {
            // default behavior: only leaf nodes are editable.
            return (getLeafNodeData(node) != null);
        }

        public Object getValueAt(WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null)
                return getValueForTimes(leafData.individualTimes,
                                        leafData.timePerPerson);
            else
                return new ReadOnlyValue
                    (getValueForTimes(getIndivTimes(node), 0));
        }

        public boolean recalculate() { return true; }
        public void storeDependentColumn(String ID, int columnNumber) {}

        private Object getValueForTimes(IndivTime[] times,
                                        double defaultTime)
        {
            StringBuffer result = new StringBuffer();
            StringBuffer annotatedResult = new StringBuffer();
            StringBuffer htmlResult = new StringBuffer();
            annotatedResult.append(DEFAULT_TIME_MARKER + "(").append(
                NumericDataValue.format(defaultTime)).append(")");
            for (int i = 0;   i < times.length;   i++)
                if (times[i].isAssigned()) {
                    times[i].appendTimeString
                        (result.append(", "), defaultTime, false);
                    times[i].appendTimeString
                        (htmlResult.append(",&nbsp;"), defaultTime, true);
                    times[i].appendTimeString
                        (annotatedResult.append(", "), -1, false);
                }

            if (result.length() == 0)
                return UNASSIGNED;
            else if (ANNOTATE_ASSIGNMENT_VALUE)
                return new HtmlRenderedValue(
                    new AnnotatedValue(
                        result.toString().substring(2),
                        annotatedResult.toString()),
                    htmlResult.substring(7));
            else
                return result.toString().substring(2);
        }

        public void setValueAt(Object aValue, WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData == null) return;
            double defaultTime = leafData.timePerPerson;
            double multiplier = 1;

            boolean foundData = false;
            HashMap times = new HashMap();
            if (aValue instanceof String) {
                Matcher m = TIME_SETTING_PATTERN.matcher((String) aValue);
                while (m.find()) {
                    String initials = m.group(1);
                    String value = m.group(2);
                    double timeVal = NumericDataValue.parse(value);
                    if (DEFAULT_TIME_MARKER.equals(initials)) {
                        if (timeVal > 0 && defaultTime > 0)
                            multiplier = defaultTime / timeVal;
                    } else {
                        foundData = true;
                        Object timeToStore;
                        if (value == null)
                            timeToStore = defaultTime;
                        else if (multiplier == 1)
                            timeToStore = value;
                        else
                            timeToStore = timeVal * multiplier;
                        times.put(initials.toLowerCase(), timeToStore);
                    }
                }
            }
            if (aValue == null || "".equals(aValue))
                foundData = true;

            if (foundData)
                for (int i = leafData.individualTimes.length;   i-- > 0; )
                    leafData.individualTimes[i].setTime(times);
        }
    }
    private static Object UNASSIGNED = new ErrorValue
        ("???", "Task needs to be assigned to individual(s)");
    private static Pattern TIME_SETTING_PATTERN =
        Pattern.compile("([a-zA-Z]+)[^a-zA-Z0-9\\.]*([0-9\\.]+)?");
    private static final boolean ANNOTATE_ASSIGNMENT_VALUE = true;
    private static final String DEFAULT_TIME_MARKER = "DefaultTimePerPerson";



    /** A column which displays the same data as the main team time column,
     * but which does not attach errors for missing estimates or assignments. */
    private class TeamTimeNoErrorColumn extends AbstractNumericColumn
        implements CalculatedDataColumn {

        private int teamTimeColumn = -1;

        public TeamTimeNoErrorColumn() {
            this.columnName = "Time";
            this.columnID = "TimeNoErr";
            this.preferredWidth = 55;
            this.dependentColumns = new String[] { "Time" };
        }

        public Object getValueAt(WBSNode node) {
            return TeamTimeColumn.this.getValueAt(node, true);
        }

        public boolean isCellEditable(WBSNode node) {
            return TeamTimeColumn.this.isCellEditable(node);
        }

        public void setValueAt(Object aValue, WBSNode node) {
            if (teamTimeColumn != -1)
                dataModel.setValueAt(aValue, node, teamTimeColumn);
        }

        public boolean recalculate() { return true; }

        public void storeDependentColumn(String ID, int columnNumber) {
            if ("Time".equals(ID))
                teamTimeColumn = columnNumber;
        }

        public void resetDependentColumns() {
            teamTimeColumn = -1;
        }
    }



    public static boolean isLeafTask(WBSModel wbsModel, WBSNode node) {
        return (wbsModel.isLeaf(node) && node.getType().endsWith("Task"));
    }

    public static String getNodeDataAttrName() {
        return TopDownBottomUpColumn.getTopDownAttrName(COLUMN_ID);
    }

    public static String getMemberAssignedZeroAttrName(TeamMember m) {
        return getMemberAssignedZeroAttrName(m.getInitials());
    }

    public static String getMemberAssignedZeroAttrName(String initials) {
        String result = MEMBER_ASSIGNED_ZERO_ATTR_NAMES.get(initials);
        if (result == null) {
            result = initials.replace('_', '-') + " (Assigned With Zero)";
            result = result.intern();
            MEMBER_ASSIGNED_ZERO_ATTR_NAMES.put(initials, result);
        }
        return result;
    }

    private static final Map<String, String> MEMBER_ASSIGNED_ZERO_ATTR_NAMES =
        new ConcurrentHashMap<String, String>();


    private static final String DATA_ATTR_NAME = "Time_Data";
    public static final String RATE_ATTR = "Rate";
    public static final String TPP_ATTR  = "Time Per Person";
    public static final String NUM_PEOPLE_ATTR = "# People";
    public static final String AUTO_ZERO_USER_ATTR_PERSISTENT = "Auto Zero User";
    public static final String AUTO_ZERO_USER_ATTR_TRANSIENT = "@Auto_Zero_User";
    public static final String TEAM_TIME_ATTR = getNodeDataAttrName();
    private static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);
}
