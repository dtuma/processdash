// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.ui.lib.autocomplete.AssignedToDocument.SEPARATOR;
import static net.sourceforge.processdash.ui.lib.autocomplete.AssignedToDocument.SEPARATOR_SPACE;
import static teamdash.wbs.WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR;
import static teamdash.wbs.columns.WorkflowResourcesColumn.ROLE_BEG;
import static teamdash.wbs.columns.WorkflowResourcesColumn.ROLE_END;

import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToComboBox;
import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToDocument;
import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToEditList;
import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToEditList.Change;
import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.AttributeMerger;
import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.MergeWarning.Severity;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberFilter;
import teamdash.wbs.AnnotatedValue;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.HtmlRenderedValue;
import teamdash.wbs.IntList;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
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
    TeamMemberFilter teamFilter;
    boolean[] matchesFilter;
    boolean singlePersonFilter;
    RateColumn rateColumn;
    TimePerPersonColumn timePerPersonColumn;
    NumPeopleColumn numPeopleColumn;
    ResourcesColumn resourcesColumn;
    TeamTimeNoErrorColumn noErrorColumn;
    UnassignedTimeColumn unassignedTimeColumn;


    public TeamTimeColumn(DataTableModel m) {
        super(m, resources.getString("Team_Time.Name"), COLUMN_ID);
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

        checkTeamFilter();
    }

    public void setTeamFilter(TeamMemberFilter f) {
        this.teamFilter = f;
        checkTeamFilter();
        dataModel.columnChanged(this);
    }

    private void checkTeamFilter() {
        if (teamFilter == null) {
            matchesFilter = null;
            singlePersonFilter = false;
        } else {
            int count = 0;
            matchesFilter = new boolean[teamMemberInitials.length];
            for (int i = 0; i < matchesFilter.length; i++) {
                matchesFilter[i] = teamFilter.include(teamMemberInitials[i]);
                if (matchesFilter[i])
                    count++;
            }
            singlePersonFilter = (count == 1);
        }
    }


    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        // if the user directly edits the team time for a particular node,
        // clear the min time attrs for the node in question.
        if (aValue instanceof String)
            WorkflowMinTimeColumn.clearMinTimeAttrs(node);

        // now, follow through with remaining logic to handle this edit.
        super.setValueAt(aValue, node);
    }

    public Object getValueAt(WBSNode node) {
        return getValueAt(node, false);
    }

    public Object getValueAt(WBSNode node, boolean ignoreErrors) {
        // get the numeric value from superclass logic
        NumericDataValue result = (NumericDataValue) super.getValueAt(node);
        if (result == null)
            return null;

        // get the filtered time on this node
        double filteredTime = (matchesFilter == null ? 0
                : safe(node.getNumericAttribute(FILTERED_TIME_ATTR)));

        if (result.errorMessage != null) {
            // if this node has a top-down-bottom-up mismatch, adjust the text
            // of the tooltip so the bottom-up number accounts for the filtered
            // time. Do not remove filtered time from the top-down value,
            // because a mismatch prevents us from correlating the top-down
            // number to any child contribution, whether filtered or not.
            if (filteredTime > 0) {
                double bottomUpValue = result.expectedValue - filteredTime;
                result.errorMessage = getMismatchTooltip(bottomUpValue);
            }

        } else {
            // add informative warnings to the value if needed
            if (!ignoreErrors) {
                result.errorColor = Color.blue;
                result.errorMessage = getInfoTooltip(node, result,
                    filteredTime);
            }

            // remove filtered time from the value we display
            if (filteredTime > 0) {
                result.value -= filteredTime;
                result.expectedValue -= filteredTime;
            }
        }

        return result;
    }

    private String getInfoTooltip(WBSNode node, NumericDataValue result,
            double filteredTime) {
        if (result.value == 0)
            return NEED_ESTIMATE_TOOLTIP;

        LeafNodeData leafData = getLeafNodeData(node);
        if (leafData instanceof LeafTaskData) { // a leaf task
            LeafTaskData leafTaskData = (LeafTaskData) leafData;
            if (leafTaskData.isMinTimeViolated()) {
                result.errorColor = MIN_TIME_VIOLATION_COLOR;
                String resKey = (filteredTime > 0
                        ? "Team_Time.Min_Time_Violated_Team_FMT"
                        : "Team_Time.Min_Time_Violated_FMT");
                return resources.format(resKey,
                    NumericDataValue.format(leafTaskData.minTime),
                    NumericDataValue.format(result.value));
            }
            if (!leafData.isFullyAssigned())
                return NEED_ASSIGNMENT_TOOLTIP;

        } else if (leafData != null) { // a leaf component
            if (result.value > 0)
                return resources.format("Team_Time.Need_Tasks_Tooltip_FMT",
                    node.getType().toLowerCase());

        } else { // not a leaf node
            IndivTime[] indivTimes = getIndivTimes(node);
            double hiddenErrTime = 0;
            for (IndivTime i : indivTimes) {
                if (i.hasError) {
                    if (i.hidden) {
                        hiddenErrTime += i.time;
                    } else {
                        result.errorColor = Color.red;
                        return RESOURCES_TIME_MISMATCH;
                    }
                }
            }
            if (!equal(result.value, sumIndivTimes(indivTimes) - hiddenErrTime))
                return NEED_ASSIGNMENT_TOOLTIP;
        }

        return null;
    }

    private static final String NEED_ESTIMATE_TOOLTIP = resources
            .getString("Team_Time.Need_Estimate_Tooltip");
    private static final String NEED_ASSIGNMENT_TOOLTIP = resources
            .getString("Team_Time.Need_Assignment_Tooltip");
    private static final Color MIN_TIME_VIOLATION_COLOR = new Color(255, 110, 0);



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
            node.setNumericAttribute(FILTERED_TIME_ATTR, leafData.filteredTime);
        } else {
            unassignedTimeColumn.clearUnassignedTime(node);
        }

        // Then, recalculate as usual.
        return super.recalc(node);
    }


    @Override
    protected double sumUpChildValues(WBSNode parent, WBSNode[] children,
            int numToInclude) {
        // perform standard logic to recalc children and sum up total time
        double result = super.sumUpChildValues(parent, children, numToInclude);

        // sum up filtered time from child to parent
        double filteredTime = 0;
        if (matchesFilter != null) {
            for (int i = 0; i < numToInclude; i++) {
                WBSNode child = children[i];
                if (shouldFilterFromCalculations(child) == false)
                    filteredTime += safe(
                        child.getNumericAttribute(FILTERED_TIME_ATTR));
            }
        }
        parent.setNumericAttribute(FILTERED_TIME_ATTR, filteredTime);

        return result;
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
            if (matchesFilter == null || matchesFilter[i]) {
                int oneMemberColumn = teamMemberColumns.get(i);
                dataModel.setValueAt("", node, oneMemberColumn);
            }
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
        // scenario. Next, check for minimum times on workflow steps, and make
        // a note of the steps whose times need to be bumped up.
        Map<WBSNode, Double> minTimes = new HashMap();
        Map<WBSNode, Double> fixedTimePctOverrides = new HashMap();
        double timeToSpread = topDownValue;
        double pctToSpread = totalPercent;
        while (true) {
            boolean madeChangeToMinTimesDuringThisPass = false;
            for (WBSNode leaf : leaves) {
                if (minTimes.containsKey(leaf))
                    continue;
                double leafMinTime = WorkflowMinTimeColumn.getMinTimeAt(leaf);
                if (!(leafMinTime > 0))
                    continue;

                double leafPercent = WorkflowPercentageColumn
                        .getExplicitValueForNode(leaf);
                if (leafPercent == 0)
                    // track which leaves have a fixed time (e.g. zero %)
                    fixedTimePctOverrides.put(leaf, null);

                double leafTime = timeToSpread * leafPercent / pctToSpread;
                if (leafTime < leafMinTime) {
                    minTimes.put(leaf, leafMinTime);
                    timeToSpread -= leafMinTime;
                    pctToSpread -= leafPercent;
                    madeChangeToMinTimesDuringThisPass = true;
                }
            }
            if (madeChangeToMinTimesDuringThisPass == false)
                break;
        }

        // if the minimum times exceeded the top-down time we were spreading,
        // ignore them and fall back to straight percentages. Assign a nonzero
        // pseudo-percentage to tasks with a fixed time, so they don't suddenly
        // (and unexpectedly) flip down to zero.
        if (timeToSpread <= 0 || pctToSpread <= 0) {
            timeToSpread = topDownValue;
            pctToSpread = totalPercent;
            for (Entry<WBSNode, Double> e : fixedTimePctOverrides.entrySet()) {
                Double fixedTime = minTimes.remove(e.getKey());
                if (fixedTime == null)
                    fixedTime = 1.0;
                double fixedPct = fixedTime * totalPercent;
                pctToSpread += fixedPct;
                e.setValue(fixedPct);
            }
            minTimes.clear();
        }

        // Subdivide the time over the leaf tasks, based on what we've found.
        for (WBSNode leaf : leaves) {
            Double pctOverride = fixedTimePctOverrides.get(leaf);
            double leafPercent = pctOverride != null ? pctOverride
                    : WorkflowPercentageColumn.getExplicitValueForNode(leaf);
            double leafTime = timeToSpread * leafPercent / pctToSpread;
            Double leafMinTime = minTimes.get(leaf);
            if (leafMinTime != null) {
                WorkflowMinTimeColumn.storeReplacedTimeAt(leaf, leafTime);
                leafTime = leafMinTime;
            } else if (pctOverride != null) {
                WorkflowMinTimeColumn.storeReplacedTimeAt(leaf, 0);
            }
            userChangingValue(leaf, leafTime);
            leaf.setNumericAttribute(topDownAttrName, leafTime);
        }
        return true;
    }

    @Override
    protected void multiplyValuesUnder(WBSNode topNode, double newTopDownValue,
            double oldTopDownValue, double ratio) {
        // our goal with this method is to scale the team time across subtasks
        // while still observing minimum time constraints.

        // find a list of the tasks under this node that need multiplying.
        // see if any of those nodes have minimum times set.
        Map<WBSNode, Double> weights = new HashMap();
        Map<WBSNode, Double> minTimes = new HashMap();
        double totalWeight = 0;
        for (WBSNode child : wbsModel.getDescendants(topNode)) {
            // do not make any changes to nodes that are hidden.
            if (child.isHidden())
                continue;

            // get the top-down time estimate for this node.
            double val = child.getNumericAttribute(topDownAttrName);

            // look for a minimum time setting on this node.
            double minTime = WorkflowMinTimeColumn.getMinTimeAt(child);
            if (minTime > 0) {
                minTimes.put(child, minTime);

                // if the current node's time is the result of a previous "min
                // time" adjustment, allocate the node a weight based on the
                // time that was originally replaced by the minimum.
                double replacedTime = WorkflowMinTimeColumn
                        .getReplacedTimeAt(child);
                if (!Double.isNaN(replacedTime))
                    val = replacedTime;
            }

            // only scale nodes that have top-down values or minimum times
            if (!(val > 0 || minTime > 0))
                continue;

            // store the weight of this node in our map.
            weights.put(child, val);
            totalWeight += val;

            // if we previously saw a parent of this node, its top-down time
            // must have been a top-down-bottom-up mismatch. remove it from our
            // data structures and don't try to scale it.
            WBSNode parent = wbsModel.getParent(child);
            Double parentWeight = weights.remove(parent);
            if (parentWeight != null)
                totalWeight -= parentWeight;
        }

        // if we didn't find any min times, fall back to standard scaling logic.
        if (minTimes.isEmpty()) {
            super.multiplyValuesUnder(topNode, newTopDownValue, oldTopDownValue,
                ratio);
            return;
        }

        // we need to scale values, while respecting min times. Calculate the
        // weight-based time allocation for each node, and see if any of the
        // nodes will need a min time adjustment.
        Map<WBSNode, Double> minTimesToUse = new HashMap();
        Map<WBSNode, Double> fixedTimeWgtOverrides = new HashMap();
        double timeToSpread = newTopDownValue;
        double weightToSpread = totalWeight;
        while (true) {
            boolean madeChangeToMinTimesDuringThisPass = false;
            for (Entry<WBSNode, Double> e : weights.entrySet()) {
                WBSNode leaf = e.getKey();
                if (minTimesToUse.containsKey(leaf))
                    continue;
                Double leafMinTime = minTimes.get(leaf);
                if (leafMinTime == null)
                    continue;

                double leafWeight = e.getValue();
                if (leafWeight == 0)
                    // track which leaves have a fixed time (e.g. zero %)
                    fixedTimeWgtOverrides.put(leaf, null);

                double leafTime = timeToSpread * leafWeight / weightToSpread;
                if (leafTime < leafMinTime) {
                    minTimesToUse.put(leaf, leafMinTime);
                    timeToSpread -= leafMinTime;
                    weightToSpread -= leafWeight;
                    madeChangeToMinTimesDuringThisPass = true;
                }
            }
            if (madeChangeToMinTimesDuringThisPass == false)
                break;
        }

        // if the minimum times exceeded the top-down time we were spreading,
        // ignore them and fall back to straight weights. Assign a nonzero
        // pseudo-weight to tasks with a fixed time, so they don't suddenly
        // (and unexpectedly) flip down to zero.
        if (timeToSpread <= 0 || weightToSpread <= 0) {
            timeToSpread = newTopDownValue;
            weightToSpread = totalWeight;
            for (Entry<WBSNode, Double> e : fixedTimeWgtOverrides.entrySet()) {
                Double fixedTime = minTimes.remove(e.getKey());
                if (fixedTime == null)
                    fixedTime = 1.0;
                double fixedWeight = fixedTime * totalWeight;
                weightToSpread += fixedWeight;
                e.setValue(fixedWeight);
            }
            minTimesToUse.clear();
        }

        // Subdivide the time over the leaf tasks, based on what we've found.
        for (Entry<WBSNode, Double> e : weights.entrySet()) {
            WBSNode leaf = e.getKey();
            Double weightOverride = fixedTimeWgtOverrides.get(leaf);
            double leafWeight = weightOverride != null ? weightOverride 
                    : e.getValue();
            double leafTime = timeToSpread * leafWeight / weightToSpread;
            Double leafMinTime = minTimesToUse.get(leaf);
            if (leafMinTime != null) {
                WorkflowMinTimeColumn.storeReplacedTimeAt(leaf, leafTime);
                leafTime = leafMinTime;
            } else if (weightOverride != null) {
                WorkflowMinTimeColumn.storeReplacedTimeAt(leaf, 0);
            } else {
                WorkflowMinTimeColumn.storeReplacedTimeAt(leaf, Double.NaN);
            }
            userChangingValue(leaf, leafTime);
            leaf.setNumericAttribute(topDownAttrName, leafTime);
        }
    }


    public void replanInProgressTime() {
        for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot())) {
            LeafTaskData leafData = getLeafTaskData(node);
            if (leafData != null)
                leafData.replanInProgressTime();
        }
    }

    public String getAutoZeroUserString(WBSNode node) {
        return resourcesColumn.getAutoZeroUserString(node);
    }

    private void applyWorkflowRoleEdits(AssignedToEditList edits, WBSNode node) {
        // scan the list of edits, looking for role-related changes.
        for (Iterator i = edits.iterator(); i.hasNext();) {
            Change change = (Change) i.next();
            String origInitials = change.origInitials;
            boolean isRoleAssignment = false;
            if (origInitials != null && origInitials.startsWith(ROLE_BEG)) {
                if (change.isDelete()) {
                    deleteRole(node, origInitials);
                } else if (change.isInitialsChange()) {
                    isRoleAssignment = true;
                    deleteRole(node, origInitials);
                }
            }
            if (!isRoleAssignment)
                i.remove();
        }

        // the loop above will discard all changes except role assignments. If
        // no role assignments were found, we're done.
        if (edits.isEmpty())
            return;

        // find the parent node which bounds this workflow instantiation.
        while (true) {
            WBSNode parent = wbsModel.getParent(node);
            if (parent == null || parent.getAttribute(
                    WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR) == null)
                break;
            else
                node = parent;
        }

        // store these new role assignments on the parent node
        storeRoleAssignments(edits, node);

        // tell the decendants of that parent to apply these role assignments.
        for (WBSNode child : wbsModel.getDescendants(node)) {
            LeafTaskData leafTaskData = getLeafTaskData(child);
            if (leafTaskData != null)
                leafTaskData.applyWorkflowRoleAssignments(edits);
        }
    }

    private boolean deleteRole(WBSNode node, String roleToDelete) {
        String currentRoles = (String) node.getAttribute(PERFORMED_BY_ATTR);
        if (currentRoles == null)
            return false;

        String textToDelete = SEPARATOR_SPACE + roleToDelete;
        String newRoles = StringUtils.findAndReplace(currentRoles,
            textToDelete, "");
        if (currentRoles.equals(newRoles))
            return false;

        if (newRoles.trim().length() == 0)
            newRoles = null;
        node.setAttribute(PERFORMED_BY_ATTR, newRoles);
        return true;
    }

    private void storeRoleAssignments(AssignedToEditList newRoleAssignments,
            WBSNode node) {
        String knownRoles = (String) node.getAttribute(KNOWN_ROLES_ATTR);
        for (Change change : newRoleAssignments) {
            // add this role name to the list of roles that are known to be
            // stored on this node
            String roleName = change.origInitials;
            if (knownRoles == null)
                knownRoles = roleName;
            else if (!knownRoles.contains(roleName))
                knownRoles = knownRoles + "," + roleName;
            // store the actual role assignment in a separate attribute.
            node.setAttribute(ROLE_ASSIGNMENT_PREFIX + roleName,
                change.newInitials);
        }
        node.setAttribute(KNOWN_ROLES_ATTR, knownRoles);
    }

    private static void updateRoleAssignments(AssignedToEditList edits,
            WBSNode node) {
        String knownRoles = (String) node.getAttribute(KNOWN_ROLES_ATTR);
        if (knownRoles == null)
            return;

        // iterate over each role that is known to be stored on this node
        for (String roleName : knownRoles.split(",")) {
            // retrieve the person who used to be assigned to this role.
            String attrName = ROLE_ASSIGNMENT_PREFIX + roleName;
            String oldAssignment = (String) node.getAttribute(attrName);
            if (oldAssignment != null) {
                // if that person's work has been reassigned to someone else,
                // update the role assignment accordingly.
                for (Change change : edits) {
                    if (oldAssignment.equalsIgnoreCase(change.origInitials)) {
                        node.setAttribute(attrName, change.newInitials);
                        break;
                    }
                }
            }
        }
    }

    public static void changeInitials(WBSModel wbsModel,
            Map<String, String> initialsToChange) {
        AssignedToEditList initialsChanges = new AssignedToEditList();
        for (Entry<String, String> e : initialsToChange.entrySet()) {
            Change change = new Change();
            change.origInitials = e.getKey();
            change.newInitials = e.getValue();
            initialsChanges.add(change);
        }
        updateRoleAssignments(initialsChanges, wbsModel.getRoot());
        for (WBSNode node : wbsModel.getDescendants(wbsModel.getRoot()))
            updateRoleAssignments(initialsChanges, node);
    }

    private static class RolePlaceholderMerger implements
            AttributeMerger<Integer, String> {
        public String mergeAttribute(Integer nodeID, String attrName,
                String base, String main, String incoming,
                ErrorReporter<Integer> err) {
            // get the list of roles in each branch.
            Set baseRoles = getRoles(base);
            Set mainRoles = getRoles(main);
            Set incomingRoles = getRoles(incoming);

            // create a copy of the base which performs all deletions
            Set result = new TreeSet(baseRoles);
            result.retainAll(mainRoles);
            result.retainAll(incomingRoles);

            // determine which items have been added, and include them too
            Set additions = new HashSet(mainRoles);
            additions.addAll(incomingRoles);
            additions.removeAll(baseRoles);
            result.addAll(additions);

            // concatenate the items to produce a result
            return (result.isEmpty() ? null : StringUtils.join(result, ""));
        }

        private Set getRoles(String attr) {
            if (attr == null || attr.length() == 0)
                return Collections.EMPTY_SET;
            Set result = new HashSet();
            Matcher m = TIME_SETTING_PATTERN.matcher(attr);
            while (m.find())
                result.add(SEPARATOR_SPACE + ROLE_BEG + m.group(1) + ROLE_END);
            return result;
        }
    }
    public static final AttributeMerger ROLE_PLACEHOLDER_MERGER = new RolePlaceholderMerger();

    private static class KnownRolesMerger implements
            AttributeMerger<Integer, String> {
        public String mergeAttribute(Integer nodeID, String attrName,
                String base, String main, String incoming,
                ErrorReporter<Integer> err) {
            if (main == null)
                return incoming;
            else if (incoming == null)
                return main;

            Set<String> roles = new HashSet();
            roles.addAll(Arrays.asList(main.split(",")));
            roles.addAll(Arrays.asList(incoming.split(",")));
            roles.remove("");
            return StringUtils.join(roles, ",");
        }
    }
    public static final AttributeMerger KNOWN_ROLES_MERGER = new KnownRolesMerger();

    private static class AssignedRoleMerger implements
            AttributeMerger<Integer, String> {
        public String mergeAttribute(Integer nodeID, String attrName,
                String base, String main, String incoming,
                ErrorReporter<Integer> err) {
            if (main == null)
                return incoming;
            else if (incoming == null)
                return main;

            String roleName = attrName.substring(ROLE_ASSIGNMENT_PREFIX.length());
            err.addMergeWarning(new AttributeMergeWarning<Integer>(
                    Severity.CONFLICT, "Attribute.Assigned_To", nodeID,
                    attrName, roleName, main, incoming));
            return main;
        }
    }
    public static AttributeMerger ASSIGNED_ROLE_MERGER = new AssignedRoleMerger();



    /** Get an array of IndivTime objects representing the amount of task time
     * each individual has for the given wbs node. */
    protected IndivTime[] getIndivTimes(WBSNode node) {
        boolean isLeaf = wbsModel.isLeaf(node);
        IndivTime[] result = new IndivTime[teamMemberColumns.size()];
        for (int i = teamMemberColumns.size();   i-- > 0; )
            result[i] = new IndivTime(node, isLeaf, teamMemberColumns.get(i),
                    teamMemberInitials[i], matchesFilter != null && !matchesFilter[i]);
        return result;
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


    /** Find the largest task time in the given array of IndivTime objects. */
    private double getMax(IndivTime[] indivTimes) {
        double result = 0;
        for (int i = indivTimes.length;   i-- > 0; )
            result = Math.max(result, indivTimes[i].time);
        return result;
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



    /** Get the LeafTaskData object for a particular node.
     * 
     * If the node is a leaf task, this will create a LeafTaskData object if
     * necessary, or return the existing LeafTaskData object if one exists.
     * If the node is not a leaf task, this will return null. */
    private LeafTaskData getLeafTaskData(WBSNode node) {
        LeafNodeData result = getLeafNodeData(node);
        if (result instanceof LeafTaskData)
            return (LeafTaskData) result;
        else
            return null;
    }

    /** Get the LeafNodeData object for a particular node.
     * 
     * If the node is a leaf, this will create a LeafNodeData object if
     * necessary, or return the existing LeafNodeData object if one exists.
     * If the node is not a leaf, this will return null. */
    private LeafNodeData getLeafNodeData(WBSNode node) {
        if (!wbsModel.isLeaf(node)) {
            node.removeAttribute(DATA_ATTR_NAME);
            return null;
        }

        LeafNodeData result =
            (LeafNodeData) node.getAttribute(DATA_ATTR_NAME);
        if (result != null && result.isTypeMismatch())
            result = null;

        if (result == null) {
            if (isTask(node))
                result = new LeafTaskData(node);
            else
                result = new LeafComponentData(node);
            node.setAttribute(DATA_ATTR_NAME, result);
        }
        return result;
    }




    /** Manages the relationships between data values at a leaf node.
     * 
     * As data values change, determines how best to recalculate other data
     * values. */
    private abstract class LeafNodeData {
        WBSNode node;
        double timePerPerson, unassignedTime, filteredTime, teamTime;
        int actualNumPeople;
        IndivTime[] individualTimes;

        protected LeafNodeData(WBSNode node) {
            this.node = node;
            individualTimes = getIndivTimes(node);
        }

        protected void setTimes(AssignedToEditList edits) {
            Map<String, Double> newTimes = new HashMap();

            // Look for people who are assigned to this task and who are
            // mentioned in a change. Copy their times into the new time map.
            for (Change change : edits) {
                IndivTime origIndiv = getIndiv(change.origInitials);
                if (origIndiv != null && origIndiv.isAssigned()
                        && (change.isNoop() || change.isChange())) {
                    double time = origIndiv.time;
                    time *= change.getTimeRatio(timePerPerson);

                    String newInitials = change.newInitials.toLowerCase();
                    Double timeToSum = newTimes.get(newInitials);
                    if (timeToSum != null)
                        time += timeToSum;

                    newTimes.put(newInitials, time);
                }
            }

            // let subclasses decide how to handle added team member initials.
            maybeAddMembers(edits, newTimes);

            for (IndivTime indiv : individualTimes)
                indiv.setTime(newTimes);
        }

        protected IndivTime getIndiv(String initials) {
            if (initials != null)
                for (IndivTime indiv : individualTimes)
                    if (indiv.initials.equalsIgnoreCase(initials))
                        return indiv;
            return null;
        }

        void maybeAddMembers(AssignedToEditList edits, Map times) {}

        protected String getRoles() {
            return null;
        }

        abstract boolean isTypeMismatch();
        abstract boolean isFullyAssigned();
        abstract void userSetTeamTime(double value);
        abstract void userSetAssignedTo(Object newValue);
        abstract void recalc();

    }

    private class LeafTaskData extends LeafNodeData {

        double size, rate, minTime;
        int numPeople;
        String units;

        public LeafTaskData(WBSNode node) {
            super(node);
            size = parse(dataModel.getValueAt(node, sizeColumn));
            units = String.valueOf(dataModel.getValueAt(node, unitsColumn));
            maybeSetAutoZeroUser();
            figureTimePerPerson();
            figureNumPeople();
            figureTeamTime();
        }

        @Override
        boolean isTypeMismatch() {
            return !isTask(node);
        }

        private void maybeSetAutoZeroUser() {
            // check to see if an auto zero user has been set using either a
            // persistent or transient attribute or a workflow assignment
            String autoZeroInitials = (String) node.removeAttribute(
                    AUTO_ZERO_USER_ATTR_PERSISTENT);
            if (autoZeroInitials == null)
                autoZeroInitials = (String) node.removeAttribute(
                    AUTO_ZERO_USER_ATTR_TRANSIENT);
            autoZeroInitials = getWorkflowAssignedUsers(autoZeroInitials);
            if (autoZeroInitials == null)
                return;

            // if someone is already assigned to this task, don't make any
            // new task assignments.
            if (countPeople(individualTimes) > 0)
                return;

            // find out how many people should be assigned to this task, and
            // extract up to that many initials from the auto-zero setting
            Map times = new HashMap();
            int ordinal = 1;
            int numNeeded = Math.max(1,
                (int) safe(node.getNumericAttribute(NUM_PEOPLE_ATTR)));
            Matcher m = TIME_SETTING_PATTERN.matcher(autoZeroInitials);
            while (numNeeded-- > 0 && m.find()) {
                // record an "assigned with zero" instruction for this person
                String initials = m.group(1).toLowerCase();
                times.put(initials, 0);
                times.put(initials + ORDINAL_SUFFIX, ordinal++);
            }

            // if we found any matching individuals, set them as "assigned
            // but zero" users.
            if (!times.isEmpty())
                for (IndivTime oneIndiv : individualTimes)
                    oneIndiv.setTime(times);
        }

        /**
         * If a just-applied workflow contained assignments to real people
         * (rather than only abstract roles), return the initials of those
         * people. If the workflow contains roles and a parent node knows of
         * prior assignments for those roles, include those individuals in the
         * result too. If the workflow contains only roles which have not yet
         * been assigned, return the empty string. Otherwise, return the
         * fallback value.
         * 
         * As a side effect, clean up the value of the "performed by" attribute
         * so it can be used by other logic.
         */
        private String getWorkflowAssignedUsers(String fallbackValue) {
            if (node.getAttribute(PERFORMED_BY_PROCESSED_FLAG) != null)
                return fallbackValue;

            String performedBy = (String) node.getAttribute(PERFORMED_BY_ATTR);
            if (performedBy == null)
                return fallbackValue;

            StringBuilder people = new StringBuilder();
            StringBuilder roles = new StringBuilder();
            Matcher m = TIME_SETTING_PATTERN.matcher(performedBy);
            while (m.find()) {
                String word = m.group(1);
                if (getIndiv(word) != null) {
                    people.append(word).append(" ");
                } else {
                    String person = getPersonAssignedToRole(word);
                    if (person != null)
                        people.append(person).append(" ");
                    else
                        roles.append(SEPARATOR_SPACE).append(ROLE_BEG)
                                .append(word).append(ROLE_END);
                }
            }
            node.setAttribute(PERFORMED_BY_ATTR, roles.length() == 0 ? null
                    : roles.toString());
            node.setAttribute(PERFORMED_BY_PROCESSED_FLAG, "t");
            return people.toString();
        }

        private String getPersonAssignedToRole(String roleName) {
            String roleToken = ROLE_BEG + roleName + ROLE_END;
            WBSNode n = node;
            while (true) {
                // walk up the tree, looking at parents and grandparents. Abort
                // if we find a node that is non-workflow-related
                n = wbsModel.getParent(n);
                if (n == null || n.getAttribute(WORKFLOW_SOURCE_IDS_ATTR) == null)
                    return null;
                // retrieve the list of known roles for this node
                String knownRoles = (String) n.getAttribute(KNOWN_ROLES_ATTR);
                if (knownRoles == null)
                    continue;
                // if the list of known roles contains the given role name,
                // look up the corresponding assignment and return it.
                if (Arrays.asList(knownRoles.split(",")).contains(roleToken)) {
                    String attrName = ROLE_ASSIGNMENT_PREFIX + roleToken;
                    String assignment = (String) n.getAttribute(attrName);
                    return assignment;
                }
            }
        }

        @Override
        protected String getRoles() {
            return (String) node.getAttribute(PERFORMED_BY_ATTR);
        }

        void figureTimePerPerson() {
            // the default way to calculate time per person is by finding
            // the mode of the individual times
            timePerPerson = getMode(individualTimes);

            // if that didn't work, calculate time per person from rate and
            // size.
            if (timePerPerson == 0) {
                rate = node.getNumericAttribute(RATE_ATTR);
                timePerPerson = adjustRateDrivenTimeForMin(size, rate);
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

        private double adjustRateDrivenTimeForMin(double size, double rate) {
            double rateDrivenTimePerPerson = safe(size / rate);
            if (size > 0 && rate > 0) {
                double minTime = WorkflowMinTimeColumn.getMinTimeAt(node);
                if (minTime > 0) {
                    figureNumPeople();
                    double minTimePerPerson = safe(minTime / numPeople);
                    if (minTimePerPerson > rateDrivenTimePerPerson) {
                        WorkflowMinTimeColumn.storeReplacedTimeAt(node,
                            rateDrivenTimePerPerson * numPeople);
                        return minTimePerPerson;
                    }
                }
                WorkflowMinTimeColumn.storeReplacedTimeAt(node, Double.NaN);
            }
            return rateDrivenTimePerPerson;
        }

        void figureNumPeople() {
            numPeople = (int) safe(node.getNumericAttribute(NUM_PEOPLE_ATTR));
            actualNumPeople = countPeople(individualTimes);

            numPeople = Math.max(numPeople, 1);
            numPeople = Math.max(numPeople, actualNumPeople + countRoles());
        }

        private int countRoles() {
            int count = 0;
            String roles = (String) node.getAttribute(PERFORMED_BY_ATTR);
            if (roles != null) {
                for (int i = roles.length(); i-- > 0; )
                    if (roles.charAt(i) == AssignedToDocument.SEPARATOR_CHAR)
                        count++;
            }
            return count;
        }

        void figureTeamTime() {
            minTime = WorkflowMinTimeColumn.getMinTimeAt(node);
            unassignedTime = (numPeople - actualNumPeople) * timePerPerson;
            unassignedTimeColumn.setUnassignedTime(node, unassignedTime);
            teamTime = sumIndivTimes() + unassignedTime;
            filteredTime = figureFilteredTime();
        }

        @Override
        public boolean isFullyAssigned() {
            return (numPeople == actualNumPeople);
        }

        public boolean isUsingMinTime() {
            return (minTime > 0 && equal(minTime, timePerPerson * numPeople));
        }

        public boolean isMinTimeViolated() {
            return (minTime > teamTime + fuzzFactor);
        }

        public double sumIndivTimes() {
            double result = 0;
            for (int i = 0;   i < individualTimes.length;   i++)
                result += individualTimes[i].time;
            return result;
        }

        double figureFilteredTime() {
            // if no filter is in effect, filtered time is zero
            if (matchesFilter == null)
                return 0;

            boolean sawAssignment = false;
            double result = 0;
            for (int i = 0; i < individualTimes.length; i++) {
                if (individualTimes[i].hidden)
                    result += individualTimes[i].time;
                if (individualTimes[i].isAssigned())
                    sawAssignment = true;
            }

            if (singlePersonFilter) {
                // if we are in single person mode and this task has already
                // been assigned to someone, add unassigned time to filtered
                // time, so it will be excluded from the plan time column.
                if (sawAssignment)
                    return result + unassignedTime;

                // if we are in single person mode and this task is unassigned,
                // we'd like the unfiltered time to equal one "timePerPerson"
                // amount. calculate a filtered time value that will do that.
                double pseudoHidden = teamTime - timePerPerson;
                if (pseudoHidden >= 0)
                    return pseudoHidden;
            }

            // return the amount of time assigned to hidden individuals.
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
            if (savedRate > 0 && safe(size) != 0 && !node.isHidden())
                // if there is a saved value for the rate, and the size value
                // entered is meaningful, we should recalculate the time per
                // person based upon that rate.
                userSetTimePerPerson(adjustRateDrivenTimeForMin(size, savedRate));
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
                // equal (for any reason other than a min time adjustment), it
                // implies that someone has edited the time value to disagree
                // with the saved rate. In response, we'll erase the saved
                // value, to prevent it from being used to recalculate time
                // if the user edits size in the future.
                if (!equal(rate, oldRate)) {
                    if (!isUsingMinTime())
                        node.setAttribute(RATE_ATTR, null);
                    dataModel.columnChanged(rateColumn);
                }
            }
        }

        public boolean isRateCalculated() {
            return safe(rate) != 0 //
                    && (node.getAttribute(RATE_ATTR) == null || isUsingMinTime());
        }

        public void userSetRate(double value) {
            if (!(value > 0)) {
                // the user is zeroing or blanking out the rate field.
                node.setAttribute(RATE_ATTR, null);
                recalculateRate();
            } else {
                rate = value;
                node.setNumericAttribute(RATE_ATTR, rate);
                userSetTimePerPerson(adjustRateDrivenTimeForMin(size, rate));
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
            if (teamTime == value)
                return;

            node.setAttribute(RATE_ATTR, null);
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

        /** Messaged when the user edits the "Assigned To" column for a task */
        @Override
        void userSetAssignedTo(Object aValue) {
            boolean foundData = false;
            HashMap times = new HashMap();
            if (aValue instanceof String) {
                double defaultTime = timePerPerson;
                double multiplier = 1;
                double ordinal = 1;
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
                        initials = initials.toLowerCase();
                        Object timeToStore;
                        if (value == null)
                            timeToStore = defaultTime;
                        else if (multiplier == 1)
                            timeToStore = value;
                        else
                            timeToStore = timeVal * multiplier;
                        times.put(initials, timeToStore);
                        times.put(initials + ORDINAL_SUFFIX, ordinal++);
                    }
                }
            }
            if (aValue == null || "".equals(aValue))
                foundData = true;

            if (foundData)
                for (int i = individualTimes.length;   i-- > 0; )
                    individualTimes[i].setTime(times);
        }

        public void replanInProgressTime() {
            if (!TeamProcess.isPSPTask(node.getType())) {
                for (int i = individualTimes.length; i-- > 0;)
                    individualTimes[i].replanInProgressTime();
            }
        }

        void maybeAddMembers(AssignedToEditList edits, Map times) {
            int numMembersNeeded = numPeople - actualNumPeople;
            if (numMembersNeeded > 0) {
                for (Change c : edits) {
                    if (c.isAdd()) {
                        IndivTime indiv = getIndiv(c.newInitials);
                        if (indiv != null && !indiv.isAssigned()) {
                            times.put(c.newInitials.toLowerCase(),
                                timePerPerson);
                            if (--numMembersNeeded == 0)
                                break;
                        }
                    }
                }
            }
        }

        void applyWorkflowRoleAssignments(AssignedToEditList edits) {
            for (Change change : edits) {
                String roleName = change.origInitials;
                String initials = change.newInitials;
                if (deleteRole(node, roleName)) {
                    IndivTime indiv = getIndiv(initials);
                    if (indiv != null)
                        indiv.setTime(Collections.singletonMap(
                            initials.toLowerCase(), indiv.time + timePerPerson));
                }
            }
        }
    }

    private class LeafComponentData extends LeafNodeData {

        protected LeafComponentData(WBSNode node) {
            super(node);
            teamTime = safe(node.getNumericAttribute(topDownAttrName));
            recalc();
        }

        boolean isTypeMismatch() {
            return isTask(node);
        }

        @Override
        public boolean isFullyAssigned() {
            return actualNumPeople > 0;
        }

        @Override
        void userSetTeamTime(double value) {
            double oldTeamTime = teamTime;
            teamTime = value;
            if (oldTeamTime == 0) {
                // divide the new time among the assigned individuals, if any
                if (actualNumPeople > 0 && teamTime > 0) {
                    double timePerPerson = teamTime / actualNumPeople;
                    for (int i = individualTimes.length;   i-- > 0; )
                        if (individualTimes[i].isAssigned())
                            individualTimes[i].setTime(timePerPerson);
                }

            } else {
                // find individuals with nonzero time, and update them.
                double ratio = teamTime / oldTeamTime;
                for (int i = individualTimes.length;   i-- > 0; )
                    individualTimes[i].multiplyTime(ratio);
            }
        }

        @Override
        void userSetAssignedTo(Object newValue) {
            boolean foundData = false;
            HashMap times = new HashMap();
            if (newValue instanceof String) {
                double defaultTime = -2;
                Set<String> defaultIndivs = new HashSet();
                double totalExplicitTime = 0;
                int ordinal = 1;

                Matcher m = TIME_SETTING_PATTERN.matcher((String) newValue);
                while (m.find()) {
                    String initials = m.group(1);
                    String value = m.group(2);
                    double timeVal = NumericDataValue.parse(value);
                    if (DEFAULT_TIME_MARKER.equals(initials)) {
                        if (timeVal > 0)
                            defaultTime = timeVal;
                    } else {
                        foundData = true;
                        initials = initials.toLowerCase();
                        if (value == null || equal(defaultTime, timeVal)) {
                            defaultIndivs.add(initials);
                        } else {
                            times.put(initials, value);
                            totalExplicitTime += timeVal;
                        }
                        times.put(initials + ORDINAL_SUFFIX, ordinal++);
                    }
                }

                if (totalExplicitTime < teamTime) {
                    double remainingTime = teamTime - totalExplicitTime;
                    int numPeople = defaultIndivs.size();
                    defaultTime = remainingTime / numPeople;
                } else {
                    defaultTime = 0;
                }
                for (String initials : defaultIndivs)
                    times.put(initials, defaultTime);
            }
            if (newValue == null || "".equals(newValue))
                foundData = true;

            if (foundData)
                for (int i = individualTimes.length;   i-- > 0; )
                    individualTimes[i].setTime(times);
        }

        @Override
        public void recalc() {
            individualTimes = getIndivTimes(node);
            actualNumPeople = countPeople(individualTimes);
            double indivSum = sumIndivTimes(individualTimes);
            if (indivSum < teamTime) {
                timePerPerson = -1;
                unassignedTime = teamTime - indivSum;
                unassignedTimeColumn.setUnassignedTime(node, unassignedTime);
            } else {
                teamTime = indivSum;
                timePerPerson = getMax(individualTimes);
                unassignedTime = 0;
                unassignedTimeColumn.clearUnassignedTime(node);
            }
            filteredTime = sumFilteredTime();
        }

        private double sumFilteredTime() {
            // if no filter is in effect, filtered time is zero
            if (matchesFilter == null)
                return 0;

            // sum the time for individuals who were hidden by the filter
            double result = 0;
            for (int i = 0; i < individualTimes.length; i++) {
                if (individualTimes[i].hidden)
                    result += individualTimes[i].time;
            }
            return result;
        }

    }


    /** This class holds the task time for one individual */
    private class IndivTime {
        WBSNode node;
        double time;
        int column;
        String initials;
        boolean hidden;
        String zeroAttrName;
        boolean zeroButAssigned;
        String ordinalAttrName;
        int ordinal;
        boolean hasError;
        boolean completed;

        public IndivTime(WBSNode node, boolean isLeaf, int column,
                String initials, boolean hidden) {
            this.node = node;
            this.column = column;
            Object value = dataModel.getValueAt(node, column);
            this.time = safe(parse(value));
            this.initials = initials;
            this.hidden = hidden;
            this.zeroAttrName = getMemberAssignedZeroAttrName(this.initials);
            this.zeroButAssigned = (node.getAttribute(zeroAttrName) != null);
            this.ordinalAttrName = getMemberAssignedOrdinalAttrName(initials);
            this.ordinal = (int) safe(node.getNumericAttribute(ordinalAttrName));
            this.hasError = !isLeaf && value instanceof NumericDataValue
                    && ((NumericDataValue) value).errorMessage != null;
            if (time == 0 && ((NumericDataValue) value).isInvisible == false)
                this.zeroButAssigned = true;

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
            if (time > 0) {
                this.time *= ratio;
                this.zeroButAssigned = (time == 0);
                node.setAttribute(zeroAttrName, zeroButAssigned ? "t" : null);
                dataModel.setValueAt(new Double(time), node, column);
            }
        }
        public StringBuffer appendTimeString(StringBuffer buf,
                                             double defaultTime,
                                             boolean html) {
            if (html && completed)
                buf.append("<strike>");
            if (html && hasError)
                buf.append("<b style='color:red'>");
            buf.append(initials);
            if (!equal(time, defaultTime))
                buf.append("(").append(formatTime(time)).append(")");
            if (html && hasError)
                buf.append("</b>");
            if (html && completed)
                buf.append("</strike>");
            return buf;
        }
        public void setTime(Map times) {
            String ilc = initials.toLowerCase();
            Object timeVal = times.get(ilc);
            if (timeVal instanceof String && isAssigned()) {
                // if the user was editing the value in the "Assigned To"
                // column, but they did not alter the textual representation
                // of the time for this individual, make no changes.
                String oldTimeVal = formatTime(this.time);
                if (timeVal.equals(oldTimeVal))
                    return;
            }

            this.time = safe(parse(timeVal));
            this.zeroButAssigned = (time == 0 && times.containsKey(ilc));
            node.setAttribute(zeroAttrName, zeroButAssigned ? "t" : null);
            this.ordinal = times.size() < 3 ? 0 //
                    : (int) safe(parse(times.get(ilc + ORDINAL_SUFFIX)));
            node.setAttribute(ordinalAttrName, ordinal == 0 ? null : ordinal);
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
                    if (plannedTimeRemaining == 0)
                        node.removeAttribute(TPP_ATTR);
                    setTime(Collections.singletonMap(initials.toLowerCase(),
                        plannedTimeRemaining));
                }
            }
        }
        boolean sortBefore(IndivTime that) {
            if (that == null)
                return true;
            else if (this.hidden != that.hidden)
                return that.hidden;
            else if (this.time > that.time)
                return true;
            else if (this.time < that.time)
                return false;
            else
                return this.ordinal < that.ordinal;
        }
    }

    /**
     * This code is copied from NumericDataValue.format, but disables grouping
     * separators for locales that don't use commas. Otherwise, the nonstandard
     * grouping separators can confuse our time parsing logic.
     */
    private static String formatTime(double time) {
        if (Double.isNaN(time) || Double.isInfinite(time))
            return "";
        else if (time > 1)
            return TIME_FMT[0].format(time);
        else if (time > 0.1)
            return TIME_FMT[1].format(time);
        else
            return TIME_FMT[2].format(time);
    }
    private static final NumberFormat[] TIME_FMT = new NumberFormat[3];
    static {
        for (int i = 0; i < 3; i++) {
            TIME_FMT[i] = NumberFormat.getNumberInstance();
            TIME_FMT[i].setMaximumFractionDigits(i+1);
            if (TIME_FMT[i].format(9999).indexOf(',') == -1)
                TIME_FMT[i].setGroupingUsed(false);
        }
    }


    /** Base class for columns related to Team Time. */
    private abstract class DependentColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        public DependentColumn(String id, String resKey) {
            this.columnID = id;
            this.columnName = resources.getString(resKey);
        }

        public boolean isCellEditable(WBSNode node) {
            // default behavior: only leaf tasks are editable.
            return (getLeafTaskData(node) != null);
        }

        public Object getValueAt(WBSNode node) {
            LeafTaskData leafData = getLeafTaskData(node);
            if (leafData != null)
                return getValueAtLeaf(leafData);
            else
                return getValueAtNode(node);
        }
        protected abstract Object getValueAtLeaf(LeafTaskData nodeData);
        protected Object getValueAtNode(WBSNode node) {
            return BLANK;
        }

        protected void setValueForNode(double value, WBSNode node) {
            LeafTaskData leafData = getLeafTaskData(node);
            if (leafData != null)
                setValueAtLeaf(value, leafData);
            else
                setValueAtNode(value, node);
        }
        protected abstract void setValueAtLeaf(double value, LeafTaskData nd);
        protected void setValueAtNode(double value, WBSNode node) { }

        @Override
        public void setValueAt(Object aValue, WBSNode node) {
            if (isNoOpEdit(aValue, node)) {
                // if the user started an editing session but ultimately made
                // no changes, do nothing
                ;
            } else {
                // if the user directly edits the value for a particular node,
                // clear the min time attrs for the node in question.
                if (aValue instanceof String)
                    WorkflowMinTimeColumn.clearMinTimeAttrs(node);

                setValueForNode(NumericDataValue.parse(aValue), node);
            }
        }

        public boolean recalculate() { return true; }
        public void storeDependentColumn(String ID, int columnNumber) {}
    }



    /** A column representing the rate at which a task is performed */
    private class RateColumn extends DependentColumn
        implements CustomRenderedColumn
    {
        public RateColumn() {
            super(RATE_COL_ID, "Rate.Name");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafTaskData nodeData) {
            if (nodeData.isRateCalculated())
                return new NumericDataValue(nodeData.rate, true, false,
                                            EFFECTIVE_RATE_MESSAGE);
            else
                return new NumericDataValue(nodeData.rate);
        }
        protected void setValueAtLeaf(double value, LeafTaskData nodeData) {
            if (value != nodeData.rate)
                nodeData.userSetRate(value);
        }
        public TableCellRenderer getCellRenderer() {
            return RATE_RENDERER;
        }
    }
    public static final String RATE_COL_ID = "Rate";
    private static final String EFFECTIVE_RATE_MESSAGE =
        resources.getString("Rate.Calculated_Tooltip");
    private static final TableCellRenderer RATE_RENDERER =
        new ItalicNumericCellRenderer(EFFECTIVE_RATE_MESSAGE);


    /** A column representing the task time per individual */
    private class TimePerPersonColumn extends DependentColumn {
        public TimePerPersonColumn() {
            super(TIME_PER_PERSON_COL_ID, "Time_Per_Person.Name");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafTaskData nodeData) {
            return new NumericDataValue(nodeData.timePerPerson);
        }
        protected void setValueAtLeaf(double value, LeafTaskData nodeData) {
            nodeData.userSetTimePerPerson(value);
        }
    }
    public static final String TIME_PER_PERSON_COL_ID = "Time Per Person";



    /** A column representing the number of people assigned to a task. */
    private class NumPeopleColumn extends DependentColumn {
        public NumPeopleColumn() {
            super(NUM_PEOPLE_COL_ID, "Num_People.Name");
            this.preferredWidth = 60;
        }
        protected Object getValueAtLeaf(LeafTaskData nodeData) {
            return new NumericDataValue(nodeData.numPeople);
        }
        protected void setValueAtLeaf(double value, LeafTaskData nodeData) {
            nodeData.userSetNumPeople(value);
        }
    }
    public static final String NUM_PEOPLE_COL_ID = "Number of People";



    /** A column representing the initials of people assigned to a task. */
    private class ResourcesColumn extends AbstractDataColumn
            implements CalculatedDataColumn, CustomEditedColumn
    {
        public ResourcesColumn() {
            this.columnID = RESOURCES_COL_ID;
            this.columnName = resources.getString("Assigned_To.Name");
            this.preferredWidth = 200;
            this.conflictAttributeNamePattern = new PatternList()
                    .addLiteralStartsWith(ROLE_ASSIGNMENT_PREFIX)
                    .addLiteralEquals(RESOURCES_PSEUDO_ATTR);
        }

        public Object getConflictDisplayValue(String value, WBSNode node) {
            return value;
        }

        public boolean isCellEditable(WBSNode node) {
            return true;
        }

        public Object getValueAt(WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null) {
                Object result = getValueForTimes(leafData.individualTimes,
                    leafData.timePerPerson, leafData.getRoles(), true);
                if (leafData instanceof LeafComponentData
                        && result != UNASSIGNED)
                    result = new ErrorValue(result, resources.format(
                        "Assigned_To.Need_Tasks_Tooltip_FMT", //
                        node.getType().toLowerCase()), ErrorValue.INFO);
                return result;
            } else {
                return getValueForTimes(getIndivTimes(node), -1, null, false);
            }
        }

        public boolean recalculate() { return true; }
        public void storeDependentColumn(String ID, int columnNumber) {}

        private String getAutoZeroUserString(WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData == null || leafData.actualNumPeople == 0)
                return null;
            else
                return getValueForTimes(leafData.individualTimes,
                    leafData.timePerPerson, null, false).toString();
        }

        private Object getValueForTimes(IndivTime[] times, double defaultTime,
                String roles, boolean mentionHidden)
        {
            String tooltip = null;
            StringBuffer result = new StringBuffer();
            StringBuffer annotatedResult = new StringBuffer();
            StringBuffer htmlResult = new StringBuffer();
            StringBuffer hiddenPeople = new StringBuffer();
            annotatedResult.append(DEFAULT_TIME_MARKER + "(").append(
                NumericDataValue.format(defaultTime)).append(")");

            boolean[] needsDisplay = new boolean[times.length];
            for (int i = times.length; i-- > 0; )
                needsDisplay[i] = times[i].isAssigned();
            int visibleCount = 0;

            while (true) {
                int i = findPosOfSortFirstTime(times, needsDisplay);
                if (i == -1)
                    break;

                times[i].appendTimeString(
                    result.append(SEPARATOR_SPACE), defaultTime, false);

                if (times[i].hidden) {
                    hiddenPeople.append(SEPARATOR_SPACE).append(times[i].initials);

                } else {
                    if (times[i].hasError)
                        tooltip = RESOURCES_TIME_MISMATCH;
                    times[i].appendTimeString(
                        htmlResult.append(SEPARATOR + "&nbsp;"), defaultTime, true);
                    times[i].appendTimeString(
                        annotatedResult.append(SEPARATOR_SPACE), -1, false);
                    visibleCount++;
                }

                needsDisplay[i] = false;
            }

            if (roles != null && roles.length() > 0) {
                result.append(roles);

                if (!singlePersonFilter || result.length() == 0) {
                    String htmlRoles = StringUtils.findAndReplace(
                        roles.substring(2), " ", "&nbsp;");
                    htmlResult.append(SEPARATOR)
                            .append("&nbsp;<b style='color:blue'>")
                            .append(htmlRoles).append("</b>");

                    if (tooltip == null)
                        tooltip = UNASSIGNED_TOOLTIP;
                }
            }

            if (hiddenPeople.length() > 0 && mentionHidden) {
                if (tooltip == null)
                    tooltip = resources.format("Assigned_To.Also_Tooltip_FMT",
                        hiddenPeople.substring(SEPARATOR_SPACE.length()));

                htmlResult.append(visibleCount > 1 ? SEPARATOR : " ");
                htmlResult.append("&nbsp;<i style='color:silver'>")
                        .append(visibleCount == 0 ? ASSIGNED_TO_OTHERS
                                : AND_ASSIGNED_TO_OTHERS)
                        .append("</i>");
            }

            if (result.length() == 0)
                return UNASSIGNED;
            else if (ANNOTATE_ASSIGNMENT_VALUE)
                return new HtmlRenderedValue(
                    new AnnotatedValue(
                        result.toString().substring(2),
                        annotatedResult.toString()),
                    htmlResult.substring(Math.min(7, htmlResult.length())),
                    tooltip);
            else
                return result.toString().substring(2);
        }

        private int findPosOfSortFirstTime(IndivTime[] times,
                boolean[] needsDisplay) {
            int resultPos = -1;
            IndivTime bestSoFar = null;
            for (int i = 0; i < times.length; i++) {
                if (needsDisplay[i] && times[i].sortBefore(bestSoFar))
                    bestSoFar = times[resultPos = i];
            }
            return resultPos;
        }

        public void setValueAt(Object aValue, WBSNode node) {
            LeafNodeData leafData = getLeafNodeData(node);
            if (leafData != null) {
                leafData.userSetAssignedTo(aValue == null ? null //
                        : aValue.toString());
                if (aValue instanceof AssignedToEditList)
                    applyWorkflowRoleEdits((AssignedToEditList) aValue, node);

            } else if (aValue instanceof AssignedToEditList) {
                clearIndividualTopDownBottomUpMismatches(node);
                AssignedToEditList edits = (AssignedToEditList) aValue;
                if (!edits.isNoop()) {
                    updateRoleAssignments(edits, node);
                    for (WBSNode child : wbsModel.getDescendants(node)) {
                        leafData = getLeafNodeData(child);
                        if (leafData != null)
                            leafData.setTimes(edits);
                        else
                            clearIndividualTopDownBottomUpMismatches(child);
                        updateRoleAssignments(edits, child);
                    }
                }
            }
        }

        public TableCellEditor getCellEditor() {
            return new AssignedToCellEditor();
        }
    }
    public static final String RESOURCES_COL_ID = "Assigned To";
    public static final String RESOURCES_PSEUDO_ATTR = "Assigned_To_Attr";
    private static String UNASSIGNED_TEXT = "???";
    private static final String UNASSIGNED_TOOLTIP = resources
            .getString("Team_Time.Need_Assignment_Tooltip");
    private static Object UNASSIGNED = new ErrorValue(UNASSIGNED_TEXT,
            UNASSIGNED_TOOLTIP, ErrorValue.INFO);
    private static final String RESOURCES_TIME_MISMATCH = resources
            .getString("Assigned_To.TDBU_Mismatch");
    private static final String ASSIGNED_TO_OTHERS = resources
            .getHTML("Assigned_To.Others");
    private static final String AND_ASSIGNED_TO_OTHERS = resources
            .getHTML("Assigned_To.And_Others");
    private static Pattern TIME_SETTING_PATTERN = Pattern.compile( //
            "([a-zA-Z]+)[^a-zA-Z0-9.,;-]*(-?[0-9.][0-9.,]*)?");
    private static final boolean ANNOTATE_ASSIGNMENT_VALUE = true;
    private static final String DEFAULT_TIME_MARKER = "DefaultTimePerPerson";
    private static final String PERFORMED_BY_ATTR = WorkflowResourcesColumn.ATTR_NAME;
    public static final String KNOWN_ROLES_ATTR = "Workflow Roles List";
    public static final String ROLE_ASSIGNMENT_PREFIX = "Workflow Role Assignment ";
    private static final String PERFORMED_BY_PROCESSED_FLAG = "_Processed "
            + PERFORMED_BY_ATTR;

    private class AssignedToCellEditor extends AutocompletingDataTableCellEditor {
        private AssignedToComboBox comboBox;
        public AssignedToCellEditor() {
            super(new AssignedToComboBox(true));
            this.comboBox = (AssignedToComboBox) getComboBox();
        }
        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            // call super() so the editor setup timer will be restarted
            super.getTableCellEditorComponent(table, null, isSelected, row,
                column);

            // update the list of valid initials
            comboBox.setInitialsList(Arrays.asList(teamMemberInitials));

            // tweak the text we will be displaying
            String text = (String) value;
            if (text == null || text.equals(UNASSIGNED_TEXT))
                text = "";
            else if (!text.endsWith(")"))
                text = text + " ";

            // tweak editing behaviors based on the node in question
            WBSNode node = wbsModel.getNodeForRow(row);
            LeafNodeData leafData = getLeafNodeData(node);
            String defaultTime;
            if (leafData instanceof LeafTaskData) {
                defaultTime = NumericDataValue.format(leafData.timePerPerson);
            } else {
                defaultTime = "0";
            }

            // initialize the combo box contents and return it
            comboBox.setFullText(text);
            comboBox.setDefaultTime(defaultTime);
            comboBox.startTrackingChanges();
            return comboBox;
        }
        @Override
        public Object getCellEditorValue() {
            return comboBox.getTrackedChanges();
        }
    }



    /** A column which displays the same data as the main team time column,
     * but which does not attach errors for missing estimates or assignments. */
    private class TeamTimeNoErrorColumn extends AbstractNumericColumn
        implements CalculatedDataColumn {

        private int teamTimeColumn = -1;

        public TeamTimeNoErrorColumn() {
            this.columnName = resources.getString("Team_Time.Name");
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
        return (wbsModel.isLeaf(node) && isTask(node));
    }

    static boolean isTask(WBSNode node) {
        return node != null && node.getType().endsWith("Task");
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
            result = initials.replace('_', '-')
                    + TeamMemberTimeColumn.ASSIGNED_WITH_ZERO_SUFFIX;
            result = result.intern();
            MEMBER_ASSIGNED_ZERO_ATTR_NAMES.put(initials, result);
        }
        return result;
    }

    private static final Map<String, String> MEMBER_ASSIGNED_ZERO_ATTR_NAMES =
        new ConcurrentHashMap<String, String>();

    private static String getMemberAssignedOrdinalAttrName(String initials) {
        String result = MEMBER_ASSIGNED_ORDINAL_ATTR_NAMES.get(initials);
        if (result == null) {
            result = initials.replace('_', '-') + ORDINAL_SUFFIX;
            result = result.intern();
            MEMBER_ASSIGNED_ORDINAL_ATTR_NAMES.put(initials, result);
        }
        return result;
    }

    private static final Map<String, String> MEMBER_ASSIGNED_ORDINAL_ATTR_NAMES =
        new ConcurrentHashMap<String, String>();
    private static final String ORDINAL_SUFFIX = "-Assignment Ordinal";


    private static final String DATA_ATTR_NAME = "Time_Data";
    private static final String FILTERED_TIME_ATTR = "Filtered_Time";
    public static final String RATE_ATTR = "Rate";
    public static final String TPP_ATTR  = "Time Per Person";
    public static final String NUM_PEOPLE_ATTR = "# People";
    public static final String AUTO_ZERO_USER_ATTR_PERSISTENT = "Auto Zero User";
    public static final String AUTO_ZERO_USER_ATTR_TRANSIENT = "@Auto_Zero_User";
    public static final String TEAM_TIME_ATTR = getNodeDataAttrName();
    private static final NumericDataValue BLANK =
        new NumericDataValue(0, false, true, null);
}
