// Copyright (C) 2010-2017 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.IntList;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer;

/**
 * This specialized column monitors changes in the total planned time for
 * each team member.
 * 
 * By default, this class is disabled and does nothing.  But it can be
 * enabled by registering the initials of a single team member whose time
 * is expected to be changing.  Once activated, if the plan time changes
 * for any <b>other</b> team member, an event will be generated to registered
 * listeners.
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

    private DataTableModel dataModel;
    private IntList teamMemberColumns;
    private String[] teamMemberAttrs;
    private String restrictTo;
    private List<String> discrepantIndividuals;
    private Set<PlanTimeDiscrepancyListener> listeners;

    public PlanTimeWatcher(DataTableModel m) {
        this.dataModel = m;
        this.columnName = this.columnID = COLUMN_ID;
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID };
        this.teamMemberColumns = new IntList();
        this.teamMemberAttrs = new String[0];
        this.restrictTo = null;
        this.discrepantIndividuals = null;
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
    }

    public boolean isCellEditable(WBSNode node) { return false; }
    public void setValueAt(Object aValue, WBSNode node) {}

    public Object getValueAt(WBSNode node) {
        return discrepantIndividuals;
    }

    public boolean recalculate() {
        discrepantIndividuals = calcDiscrepancies();
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
