package teamdash.wbs.columns;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.IntList;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

/** This column manages the calculation and interrelationship of several
 * tightly related columns dealing with team time.
 */
public class TeamTimeColumn extends TopDownBottomUpColumn {

    int sizeColumn = -1;
    int unitsColumn = -1;
    IntList teamMemberColumns;
    RateColumn rateColumn;
    TimePerPersonColumn timePerPersonColumn;
    NumPeopleColumn numPeopleColumn;


    public TeamTimeColumn(DataTableModel m) {
        super(m, "Time", "Time");
        this.dependentColumns = new String[] { "Task Size", "Task Size Units" };
        this.teamMemberColumns = new IntList();

        // create and add our interrelated columns.
        m.addDataColumn(rateColumn = new RateColumn());
        m.addDataColumn(timePerPersonColumn = new TimePerPersonColumn());
        m.addDataColumn(numPeopleColumn = new NumPeopleColumn());
    }


    public void storeDependentColumn(String ID, int columnNumber) {
        if ("Task Size".equals(ID))
            sizeColumn = columnNumber;
        else if ("Task Size Units".equals(ID))
            unitsColumn = columnNumber;
        else if (!teamMemberColumns.contains(columnNumber))
            teamMemberColumns.add(columnNumber);
    }


    public void resetDependentColumns() {
        sizeColumn = unitsColumn = -1;
        teamMemberColumns = new IntList();
    }


    public Object getValueAt(WBSNode node) {
        NumericDataValue result = (NumericDataValue) super.getValueAt(node);
        if (result == null || result.errorMessage != null) return result;

        boolean needsAssigning = false;
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null) { // a leaf task
            needsAssigning = !leafData.isConnected();
        } else if (wbsModel.isLeaf(node)) { // a leaf node which isn't a task
            result.errorMessage = "You need to create tasks underneath this "
                + node.getType().toLowerCase();
            result.errorColor = Color.red;

        } else { // not a leaf task
            needsAssigning = !equal(result.value, sumIndivTimes(node));
        }

        if (needsAssigning) {
            result.errorMessage =
                (equal(result.value, 0)
                 ? "Task time needs to be estimated"
                 : "Task needs to be assigned to individual(s)");
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
        }

        // Then, recalculate as usual.
        return super.recalc(node);
    }


    /** When the user edits and changes a value, this function is called
     * for each affected node before the change is made. */
    protected void userChangingValue(WBSNode node, double value) {
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null)
            leafData.userSetTeamTime(value);
    }




    /** Get an array of IndivTime objects representing the amount of task time
     * each individual has for the given wbs node. */
    protected IndivTime[] getIndivTimes(WBSNode node) {
        IndivTime[] result = new IndivTime[teamMemberColumns.size()];
        for (int i = teamMemberColumns.size();   i-- > 0; )
            result[i] = new IndivTime(node, teamMemberColumns.get(i));
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


    /** Count the number of individuals with nonzero times for the given node */
    private int countPeople(WBSNode node) {
        return countPeople(getIndivTimes(node));
    }

    /** Count the number of individuals with nonzero times in the given
     * list of IndivTime objects. */
    private int countPeople(IndivTime[] indivTimes) {
        int count = 0;
        for (int i = 0;   i < indivTimes.length;   i++)
            if (indivTimes[i].time > 0) count++;
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
        double size, rate, timePerPerson, numPeople, actualNumPeople, teamTime;
        String units;
        IndivTime[] individualTimes;

        public LeafNodeData(WBSNode node) {
            this.node = node;
            size = parse(dataModel.getValueAt(node, sizeColumn));
            units = String.valueOf(dataModel.getValueAt(node, unitsColumn));
            individualTimes = getIndivTimes(node);
            figureTimePerPerson();
            figureNumPeople();
            figureTeamTime();
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
            teamTime = sumIndivTimes() +
                (numPeople - actualNumPeople) * timePerPerson;
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

        public void userSetRate(double value) {
            if (safe(value) == 0) {
                // the user is zeroing or blanking out the rate field.
                rate = Double.NaN;
                node.setAttribute(RATE_ATTR, null);
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
            if (equal(value, timePerPerson)) return;

            double oldTimePerPerson = timePerPerson;
            timePerPerson = value;
            node.setNumericAttribute(TPP_ATTR, timePerPerson);
            recalculateRate();

            // if the time per person changed from or to zero, don't try
            // to proportionally propagate the change along.
            if (safe(oldTimePerPerson) != 0 && safe(timePerPerson) != 0) {

                // find individuals with that amount of time, and update them.
                List changesToMake = new ArrayList();
                for (int i = individualTimes.length;   i-- > 0; )
                    if (equal(individualTimes[i].time, oldTimePerPerson))
                        individualTimes[i].setTime(timePerPerson);
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
                List changesToMake = new ArrayList();
                for (int i = individualTimes.length;   i-- > 0; )
                    individualTimes[i].multiplyTime(ratio);
            }
            figureTeamTime();
        }
    }


    /** This class holds the task time for one individual */
    private class IndivTime {
        WBSNode node;
        double time;
        int column;

        public IndivTime(WBSNode node, int column) {
            this.node = node;
            this.column = column;
            this.time = safe(parse(dataModel.getValueAt(node, column)));
        }

        public void setTime(double newTime) {
            this.time = newTime;
            dataModel.setValueAt(new Double(time), node, column);
        }

        public void multiplyTime(double ratio) {
            if (time > 0)
                setTime(time * ratio);
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
    private class RateColumn extends DependentColumn {
        public RateColumn() {
            super("Rate");
        }
        protected Object getValueAtLeaf(LeafNodeData nodeData) {
            return new NumericDataValue(nodeData.rate);
        }
        protected void setValueAtLeaf(double value, LeafNodeData nodeData) {
            nodeData.userSetRate(value);
        }
    }



    /** A column representing the task time per individual */
    private class TimePerPersonColumn extends DependentColumn {
        public TimePerPersonColumn() {
            super("Hrs/Indiv", "Time Per Person");
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
        }
        protected Object getValueAtLeaf(LeafNodeData nodeData) {
            return new NumericDataValue(nodeData.numPeople);
        }
        protected Object getValueAtNode(WBSNode node) {
            return new NumericDataValue(countPeople(node), false);
        }
        protected void setValueAtLeaf(double value, LeafNodeData nodeData) {
            nodeData.userSetNumPeople(value);
        }
    }


    public static boolean isLeafTask(WBSModel wbsModel, WBSNode node) {
        return (wbsModel.isLeaf(node) && node.getType().endsWith("Task"));
    }


    private static final String DATA_ATTR_NAME = "Time_Data";
    static final String RATE_ATTR = "Rate";
    private static final String TPP_ATTR  = "Time Per Person";
    static final String NUM_PEOPLE_ATTR = "# People";
    private static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);
}
