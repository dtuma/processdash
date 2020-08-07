// Copyright (C) 2010-2019 Tuma Solutions, LLC
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

import static teamdash.wbs.columns.SizeDataColumn.PROBE_MULTI_FLAG_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.IntList;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSDataModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer;

/**
 * This specialized column monitors changes in the total planned time for each
 * team member. It also watches PSP and PROBE tasks to see whether they are
 * assigned to more than one person.
 * 
 * When these values change, appropriate targets are notified:
 * <ul>
 * 
 * <li>A single set of initials can be registered to indicate a single team
 * member whose time is expected to be changing. If the plan time changes for
 * any <b>other</b> team member, an event will be generated to registered
 * listeners.</li>
 * 
 * <li>If a PSP or PROBE task changes from one assignee to multiple (or vice
 * versa), the appropriate {@link SizeDataColumn}s will be notified so they can
 * dynamically change behavior.</li>
 * 
 * </ul>
 */
public class PlanTimeWatcher extends AbstractDataColumn implements
        CalculatedDataColumn {

    public static final String COLUMN_ID = "Plan Time Watcher";

    public static class PlanTimeDiscrepancyEvent extends EventObject {
        private List<String> initials;
        private PlanTimeDiscrepancyEvent(Object source, List<String> initials) {
            super(source);
            this.initials = initials;
        }
        public List<String> getDiscrepantInitials() {
            return initials;
        }
    }

    public interface PlanTimeDiscrepancyListener extends EventListener {
        public void discrepancyNoted(PlanTimeDiscrepancyEvent e);
    }

    private WBSDataModel dataModel;
    private TeamProcess process;
    private IntList teamMemberColumns;
    private String[] teamMemberAttrs;
    private int numPeopleCol;
    private int rateCol;
    private String restrictTo;
    private List<String> discrepantIndividuals;
    private boolean watchPspAndProbeTasks;
    private Set<PlanTimeDiscrepancyListener> listeners;

    public PlanTimeWatcher(WBSDataModel m, TeamProcess p) {
        this.dataModel = m;
        this.process = p;
        this.columnName = this.columnID = COLUMN_ID;
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID };
        this.teamMemberColumns = new IntList();
        this.teamMemberAttrs = new String[0];
        this.numPeopleCol = this.rateCol = -1;
        this.restrictTo = null;
        this.discrepantIndividuals = null;
        this.watchPspAndProbeTasks = SizeTypeColumn
                .isUsingNewSizeDataColumns(m.getWBSModel());
        this.listeners = new HashSet<PlanTimeDiscrepancyListener>();
    }

    public void addPlanTimeDiscrepancyListener(PlanTimeDiscrepancyListener l) {
        listeners.add(l);
    }

    public void removePlanTimeDiscrepancyListener(PlanTimeDiscrepancyListener l) {
        listeners.remove(l);
    }

    public String getRestrictTo() {
        return restrictTo;
    }

    public void setRestrictTo(String restrictTo) {
        this.restrictTo = restrictTo;

        // after changing the "restrict to" setting, we recalculate the
        // current list of discrepancies.  If monitoring has just been
        // turned off, this will do nothing.  Otherwise, it will have the
        // side effect of resetting the "last known plan time" of each
        // team member to the current values, so the next data edit will
        // be able to properly detect whether a change occurs.
        calcDiscrepancies();
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        teamMemberColumns = dataModel.getTeamMemberColumnIDs();
        teamMemberAttrs = new String[teamMemberColumns.size()];
        for (int i = teamMemberColumns.size(); i-- > 0;) {
            int col = teamMemberColumns.get(i);
            String initials = getInitialsFor(col);
            teamMemberAttrs[i] = "_Plan_Time_Watcher_Last_Val-" + initials;
        }
        numPeopleCol = dataModel.findColumn(TeamTimeColumn.NUM_PEOPLE_COL_ID);
        rateCol = dataModel.findColumn(TeamTimeColumn.RATE_COL_ID);
    }

    public boolean isCellEditable(WBSNode node) { return false; }
    public void setValueAt(Object aValue, WBSNode node) {}

    public Object getValueAt(WBSNode node) {
        return discrepantIndividuals;
    }

    public boolean recalculate() {
        discrepantIndividuals = calcDiscrepancies();
        maybeScanPspAndProbeTasks();
        maybeFireChangeEvent();
        return false;
    }

    public void reset() {
        WBSNode rootNode = dataModel.getWBSModel().getRoot();
        for (int i = teamMemberColumns.size(); i-- > 0;)
            rootNode.removeAttribute(teamMemberAttrs[i]);
    }

    private List<String> calcDiscrepancies() {
        if (restrictTo == null)
            return null;

        List<String> result = null;
        WBSNode rootNode = dataModel.getWBSModel().getRoot();
        if (rootNode.getAttribute(WBSSynchronizer.SYNC_IN_PROGRESS_ATTR) != null)
            return null;

        for (int i = teamMemberColumns.size(); i-- > 0;) {
            int oneColumn = teamMemberColumns.get(i);
            String oneAttr = teamMemberAttrs[i];
            String oneDiscrepancy = calcDiscrepancyFor(rootNode, oneColumn,
                oneAttr);
            if (oneDiscrepancy != null) {
                if (result == null)
                    result = new ArrayList<String>();
                result.add(oneDiscrepancy);
            }
        }

        return result;
    }

    private String calcDiscrepancyFor(WBSNode rootNode, int col, String attrName) {
        String initials = getInitialsFor(col);
        if (restrictTo.equalsIgnoreCase(initials))
            return null;

        double currentVal = NumericDataValue
                .parse(dataModel.getValueAt(0, col));
        double lastVal = rootNode.getNumericAttribute(attrName);

        String result = null;
        if (Math.abs(currentVal - lastVal) > 0.05) {
            // note that if currentVal or lastVal were missing, they would
            // come back as NaN, this test would evaluate to false, and this
            // branch of code would not execute.
            result = initials;
        }

        rootNode.setNumericAttribute(attrName, currentVal);
        return result;
    }

    private String getInitialsFor(int col) {
        return dataModel.getColumnName(col);
    }



    private void maybeScanPspAndProbeTasks() {
        if (watchPspAndProbeTasks && numPeopleCol != -1) {
            for (WBSNode n : dataModel.getWBSModel().getWbsNodes())
                scanPspOrProbeTask(n);
        }
    }

    private void scanPspOrProbeTask(WBSNode node) {
        // we only need to look at PSP/PROBE tasks
        String type = node.getType();
        if (!TeamProcess.isPSPTask(type) && !TeamProcess.isProbeTask(type))
            return;

        // count the number of people assigned to this task
        double numPeople = NumericDataValue
                .parse(dataModel.getValueAt(node, numPeopleCol));
        boolean isMulti = (numPeople > 1);

        // check the "PROBE Multi" flag on the node. If it's correct, nothing
        // more needs to be done
        Object multiFlag = node.getAttribute(PROBE_MULTI_FLAG_ATTR);
        if (isMulti == false && multiFlag == null)
            return;
        String units = TaskSizeUnitsColumn.getSizeUnitsForTask(node, process);
        if (isMulti == true && units.equals(multiFlag))
            return;

        // set the flag on the node. If the task is assigned to multiple people,
        // the flag hold the units of the PSP/PROBE task. Otherwise null
        node.setAttribute(PROBE_MULTI_FLAG_ATTR, isMulti ? units : null);

        // alert the affected columns
        Set<String> affectedMetrics = new HashSet(Collections.singleton(units));
        if (multiFlag instanceof String)
            affectedMetrics.add((String) multiFlag);
        for (String metric : affectedMetrics) {
            for (boolean plan : new boolean[] { true, false }) {
                String colID = SizeDataColumn.getColumnID(metric, plan);
                int colPos = dataModel.findColumn(colID);
                if (colPos != -1)
                    dataModel.setValueAt(PROBE_MULTI_FLAG_ATTR, node, colPos);
            }
        }
        if (isMulti && rateCol != -1)
            dataModel.columnChanged(dataModel.getColumn(rateCol));
    }



    private void maybeFireChangeEvent() {
        if (empty(discrepantIndividuals) || empty(listeners))
            return;

        PlanTimeDiscrepancyEvent e = new PlanTimeDiscrepancyEvent(this,
                discrepantIndividuals);
        for (PlanTimeDiscrepancyListener l : listeners)
            l.discrepancyNoted(e);
    }

    private boolean empty(Collection c) {
        return c == null || c.isEmpty();
    }

}
