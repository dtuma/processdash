package teamdash.wbs.columns;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.IntList;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

/** This column calculates several interrelated columns dealing with team time.
 *
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
        this.dependentColumns = new String[] { "Size", "Size-Units" };
        this.teamMemberColumns = new IntList();

        // create and add our interrelated columns.
        m.addDataColumn(rateColumn = new RateColumn());
        m.addDataColumn(timePerPersonColumn = new TimePerPersonColumn());
        m.addDataColumn(numPeopleColumn = new NumPeopleColumn());
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if ("Size".equals(ID))
            sizeColumn = columnNumber;
        else if ("Size-Units".equals(ID))
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
        if (leafData == null) { // not a leaf
            if (!equal(result.value, sumIndivTimes(node)))
                needsAssigning = true;
        } else {
            if (!leafData.isConnected())
                needsAssigning = true;
        }

        if (needsAssigning) {
            result.errorMessage = "Task needs to be assigned to individual(s)";
            result.errorColor = Color.blue;
        }

        return result;
    }

    private double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v) ? 0 : v);
    }

    protected void setValueForNode(double value, WBSNode node) { }


    protected double recalc(WBSNode node) {
        // this could be called because:
        // 1) the user edited the team time, and we're recalculating
        // 2) the user edited the Size column, and we're recalculating
        // 3) the user edited an individual time column, and we're recalcing

        // do extra stuff here.
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null) {
            leafData.recalc();
            if (safe(leafData.teamTime) == 0)
                node.setAttribute(topDownAttrName, null);
            else
                node.setNumericAttribute(topDownAttrName, leafData.teamTime);
        }

        return super.recalc(node);
    }


    /** When the user edits and changes a value, this function is called
     * for each affected node before the change is made. */
    protected void userChangingValue(WBSNode node, double value) {
        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData != null)
            leafData.userSetTeamTime(value);
    }

    /** This class is used to record a change that needs to be made to
     * a dependent data column */
    private class DataChange {

        WBSNode node;   // the node where the change is to be made
        int column;     // the column to make the change to
        Object value;   // the new value to store

        public DataChange(WBSNode node, int column, double value) {
            this(node, column, new Double(value));
        }
        public DataChange(WBSNode node, int column, Object value) {
            this.node = node;
            this.column = column;
            this.value = value;
        }
        /** make the requested change */
        public void makeChange() {
            dataModel.setValueAt(value, node, column);
        }
    }


    /** Make a series of changes to dependent data columns. */
    private void makeDataChanges(List changesToMake) {
        if (changesToMake == null || changesToMake.size() == 0) return;
        Iterator i = changesToMake.iterator();
        while (i.hasNext())
            ((DataChange) i.next()).makeChange();
    }


    private class IndivTime {
        double time;
        int column;
        public IndivTime(WBSNode node, int column) {
            this.column = column;
            this.time = safe(parse(dataModel.getValueAt(node, column)));
        }
        public boolean equals(Object o) {
            return (o instanceof IndivTime &&
                    equal(((IndivTime) o).time, time) &&
                    ((IndivTime) o).column == column);
        }
    }


    protected IndivTime[] getIndivTimes(WBSNode node) {
        IndivTime[] result = new IndivTime[teamMemberColumns.size()];
        for (int i = teamMemberColumns.size();   i-- > 0; )
            result[i] = new IndivTime(node, teamMemberColumns.get(i));
        return result;
    }

    private double sumIndivTimes(WBSNode node) {
        double result = 0;
        for (int i = teamMemberColumns.size();   i-- > 0; )
            result += safe(parse
                (dataModel.getValueAt(node, teamMemberColumns.get(i))));
        return result;
    }

    private int countPeople(WBSNode node) {
        int count = 0;
        for (int i = teamMemberColumns.size();   i-- > 0; )
            if (parse
                (dataModel.getValueAt(node, teamMemberColumns.get(i))) > 0)
                count++;
        return count;
    }

    private int countPeople(IndivTime[] indivTimes) {
        int count = 0;
        for (int i = 0;   i < indivTimes.length;   i++)
            if (indivTimes[i].time > 0) count++;
        return count;
    }

    private static double parse(Object o) {
        return NumericDataValue.parse(o);
    }
    private boolean equal(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }
    private boolean equal(IndivTime a, IndivTime b) {
        if (a == null && b == null) return true;
        return (a != null && a.equals(b));
    }
    private boolean equal(IndivTime[] a, IndivTime[] b) {
        if (a == null) a = new IndivTime[0];
        if (b == null) b = new IndivTime[0];

        if (a.length != b.length) return false;
        for (int i = a.length;   i-- > 0; )
            if (!equal(a[i], b[i])) return false;
        return true;
    }

    private double getMode(IndivTime[] indivTimes) {
        double[] d = new double[indivTimes.length];
        for (int i = d.length;   i-- > 0; )
            d[i] = indivTimes[i].time;
        return getMode(d);
    }
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

    private static final String RATE_ATTR = "Rate";
    private static final String TPP_ATTR  = "Time_Per_Person";
    private static final String NUM_PEOPLE_ATTR = "# People";



    private interface LeafNodeManipulator {
        public void recalc(LeafNodeData leafData);
    }

    private class ConnectedManipulator implements LeafNodeManipulator {
        public void recalc(LeafNodeData leafData) {}
    }

    private class DisconnectedManipulator implements LeafNodeManipulator {
        public void recalc(LeafNodeData leafData) {}
    }


    private class LeafNodeData {
        WBSNode node;
        double size, rate, timePerPerson, numPeople, actualNumPeople, teamTime;
        String units;
        IndivTime[] individualTimes;
        LeafNodeManipulator manipulator;

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

            // if that worked, calculate the effective rate.
            if (timePerPerson != 0) {
                if (safe(size) != 0) {
                    rate = size / timePerPerson;
                    node.setNumericAttribute(RATE_ATTR, rate);
                } else {
                    rate = Double.NaN;
                    //node.setAttribute(RATE_ATTR, null);
                }

            } else {
                // next method: calculate time per person from rate and size.
                rate = node.getNumericAttribute(RATE_ATTR);
                timePerPerson = size / rate;

                // if that didn't work, revert to the last known good time
                // per person
                if (safe(timePerPerson) == 0) {
                    timePerPerson = safe(node.getNumericAttribute(TPP_ATTR));
                    rate = Double.NaN;
                }
            }

            node.setNumericAttribute(TPP_ATTR, timePerPerson);
        }

        void figureNumPeople() {
            numPeople = (int) safe(node.getNumericAttribute(NUM_PEOPLE_ATTR));
            actualNumPeople = countPeople(individualTimes);

            if (numPeople == 0)
                numPeople = (actualNumPeople > 0 ? actualNumPeople : 1);

            if (numPeople == actualNumPeople)
                manipulator = new ConnectedManipulator();
            else
                manipulator = new DisconnectedManipulator();
        }

        void figureTeamTime() {
            teamTime = sumIndivTimes() +
                (numPeople - actualNumPeople) * timePerPerson;
        }


        public boolean isConnected() {
            return (manipulator instanceof ConnectedManipulator);
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

            manipulator.recalc(this);
        }

        public void userSetSize(double value) {
            size = value;
            if (safe(rate) != 0) {
                double newTimePerPerson = size / rate;
                userSetTimePerPerson(newTimePerPerson);
                dataModel.columnChanged(timePerPersonColumn);
            }
        }

        protected void recalculateRate() {
            if (safe(timePerPerson) == 0) return;
            if (safe(size) == 0) return;
            rate = size / timePerPerson;
            node.setNumericAttribute(RATE_ATTR, rate);
            dataModel.columnChanged(rateColumn);
        }

        public void userSetRate(double value) {
            if (safe(value) == 0) {
                // the user is zeroing or blanking out the rate field.
                rate = Double.NaN;
                node.setAttribute(RATE_ATTR, null);
            } else {
                rate = value;
                double newTimePerPerson = size / rate;
                userSetTimePerPerson(newTimePerPerson);
                dataModel.columnChanged(timePerPersonColumn);
            }
        }

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
                Double newValue = new Double(timePerPerson);
                for (int i = individualTimes.length;   i-- > 0; )
                    if (equal(individualTimes[i].time, oldTimePerPerson))
                        changesToMake.add
                            (new DataChange(node, individualTimes[i].column,
                                            newValue));
                makeDataChanges(changesToMake);
                individualTimes = getIndivTimes(node);
            }

            // recalculate team time and register the change.
            figureTeamTime();
            dataModel.columnChanged(TeamTimeColumn.this);
        }

        public void userSetNumPeople(double value) {
            if (safe(value) > 0 && value >= actualNumPeople) {
                node.setNumericAttribute(NUM_PEOPLE_ATTR, value);
                figureNumPeople();
                figureTeamTime();
                dataModel.columnChanged(numPeopleColumn);
                dataModel.columnChanged(TeamTimeColumn.this);
            }
        }

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
                    if (individualTimes[i].time > 0)
                        changesToMake.add
                            (new DataChange(node, individualTimes[i].column,
                                            individualTimes[i].time * ratio));
                makeDataChanges(changesToMake);
            }
            figureTeamTime();
        }
    }




    protected LeafNodeData getLeafNodeData(WBSNode node) {
        if (!wbsModel.isLeaf(node)) {
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
            return wbsModel.isLeaf(node);
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



    private static final String DATA_ATTR_NAME = "Time_Data";
    private static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);
}
