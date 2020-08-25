// Copyright (C) 2014-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.util.DataPair;

public class WorkflowHistDataHelper {

    public class Enactment {
        public int rootKey;
        public String rootWbsID;
        public String projectName;
        public String rootName;
        public Date completed;

        public String getProjectName() { return projectName; }
        public String getRootName() { return rootName; }
        public String getRootID() { return rootWbsID; }
        public Date getCompleted() { return completed; }
        public double getHours() { return actualTime() / 60; }
        public Map<String, Double> getSizes() {
            Map<String, Double> result = new HashMap<String, Double>();
            for (String units : getSizeUnits())
                result.put(units, actualSize(units));
            return result;
        }
        public Set<String> getLabels() {
            return WorkflowHistDataHelper.this.getLabels(this);
        }
        public String getProjectID() {
            return rootWbsID.substring(0, rootWbsID.indexOf(':'));
        }

        public double actualTime() {
            return actualTime(null);
        }

        public double actualTime(Object phaseFilter) {
            if (_actualTime == null)
                _actualTime = new HashMap<Object, Double>();
            Double result = _actualTime.get(phaseFilter);
            if (result == null) {
                result = getTime(this, phaseFilter, true);
                if (!(phaseFilter instanceof Collection))
                    _actualTime.put(phaseFilter, result);
            }
            return result;
        }
        private Map<Object, Double> _actualTime;

        public double actualSize(String units) {
            if (_actualSize == null)
                _actualSize = new HashMap<String, Double>();
            Double result = _actualSize.get(units);
            if (result == null) {
                result = getSize(this, units, true);
                _actualSize.put(units, result);
            }
            return result;
        }
        private Map<String, Double> _actualSize;

        public int actualDefects(String phase, boolean removed) {
            if (_actualDefects == null)
                _actualDefects = getDefectsByPhase(this);

            int idx;
            if (UNKNOWN_PHASE_KEY.equals(phase)) {
                idx = UNK;
                phase = TOTAL_PHASE_KEY;
            } else {
                idx = removed ? REM : INJ;
            }
            return (int) _actualDefects[idx].get(phase).actual;
        }
        private Map<String, DataPair>[] _actualDefects;

        public double actualYield(String phase, boolean process) {
            if (_yields == null)
                _yields = getYields(this);

            if (phase == null) {
                List<String> failure = getPhasesOfType(PhaseType.Failure);
                phase = (failure.isEmpty() ? TOTAL_PHASE_KEY : failure.get(0));
            }
            return _yields[process ? 0 : 1].get(phase).actual;
        }
        private Map<String, DataPair>[] _yields;

        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Enactment //
                    && this.rootKey == ((Enactment) obj).rootKey);
        }
        public int hashCode() {
            return rootKey;
        }
        public String toString() {
            return projectName + "/" + rootName;
        }
    }

    public static final String TOTAL_PHASE_KEY = "Total ";

    public static final String UNKNOWN_PHASE_KEY = "Unknown ";

    public enum LegacyPhaseMapStrategy { Disabled, Enabled, Strict }


    private QueryRunner query;

    private String workflowID;

    private String contextProjectID;

    private String workflowName;

    private Set<String> includedEnactments;

    private Set<String> excludedEnactments;

    private Set<String> includedProjects;

    private Set<String> excludedProjects;

    private Set<String> includedNames;

    private Set<String> excludedNames;

    private Date onlyCompletedAfter;

    private Date onlyCompletedBefore;

    private boolean onlyCompleted = true;

    private Double minTime;

    private Double maxTime;

    private Map<String, Double> minSize = new HashMap();

    private Map<String, Double> maxSize = new HashMap();

    private Set<String> includedLabels;

    private Set<String> excludedLabels;

    private LegacyPhaseMapStrategy legacyPhaseMapStrategy;

    private Integer workflowKey;

    private Set<Integer> includedWorkflowKeys;

    private List<Object[]> enactments;

    private List<Enactment> enactmentInfo;

    public WorkflowHistDataHelper(QueryRunner query, String workflowID) {
        this.query = query;
        this.workflowID = workflowID;
        this.contextProjectID = workflowID.split(":")[1];
        this.legacyPhaseMapStrategy = LegacyPhaseMapStrategy.Strict;
    }

    public String getContextProjectID() {
        return contextProjectID;
    }

    public String getWorkflowName() {
        if (workflowName == null)
            initWorkflowData();
        return workflowName;
    }

    public String getWorkflowID() {
        return workflowID;
    }

    public boolean isOnlyCompleted() {
        return onlyCompleted;
    }

    public void setOnlyCompleted(boolean onlyCompleted) {
        this.onlyCompleted = onlyCompleted;
    }

    public boolean isFiltering() {
        return oneNotNull(includedEnactments, excludedEnactments, minTime,
            maxTime, includedProjects, excludedProjects, includedNames,
            excludedNames, onlyCompletedBefore, onlyCompletedAfter,
            includedLabels, excludedLabels)
                || !minSize.isEmpty() || !maxSize.isEmpty();
    }

    private boolean oneNotNull(Object... objects) {
        for (Object o : objects) {
            if (o != null)
                return true;
        }
        return false;
    }

    public Set<String> getIncludedEnactments() {
        return includedEnactments;
    }

    public void setIncludedEnactments(Set<String> includedEnactments) {
        this.includedEnactments = includedEnactments;
    }

    public Set<String> getExcludedEnactments() {
        return excludedEnactments;
    }

    public void setExcludedEnactments(Set<String> excludedEnactments) {
        this.excludedEnactments = excludedEnactments;
    }

    public Set<String> getIncludedProjects() {
        return includedProjects;
    }

    public void setIncludedProjects(Set<String> includedProjects) {
        this.includedProjects = includedProjects;
    }

    public Set<String> getExcludedProjects() {
        return excludedProjects;
    }

    public void setExcludedProjects(Set<String> excludedProjects) {
        this.excludedProjects = excludedProjects;
    }

    public Set<String> getIncludedNames() {
        return includedNames;
    }

    public void setIncludedNames(Set<String> includedNames) {
        this.includedNames = toLowerCaseSet(includedNames);
    }

    public Set<String> getExcludedNames() {
        return excludedNames;
    }

    public void setExcludedNames(Set<String> excludedNames) {
        this.excludedNames = toLowerCaseSet(excludedNames);
    }

    private Set<String> toLowerCaseSet(Set<String> set) {
        if (set == null || set.isEmpty())
            return set;
        Set<String> result = new HashSet();
        for (String s : set)
            result.add(s.toLowerCase());
        return result;
    }

    public Date getOnlyCompletedAfter() {
        return onlyCompletedAfter;
    }

    public void setOnlyCompletedAfter(Date completedAfter) {
        this.onlyCompletedAfter = completedAfter;
        if (completedAfter != null)
            this.onlyCompleted = true;
    }

    public Date getOnlyCompletedBefore() {
        return onlyCompletedBefore;
    }

    public void setOnlyCompletedBefore(Date completedBefore) {
        this.onlyCompletedBefore = completedBefore;
        if (completedBefore != null)
            this.onlyCompleted = true;
    }

    public Double getMinTime() {
        return minTime;
    }

    public void setMinTime(Double minTime) {
        this.minTime = minTime;
    }

    public Double getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Double maxTime) {
        this.maxTime = maxTime;
    }

    public void setMinSize(String units, Double size) {
        if (size == null)
            minSize.remove(units);
        else
            minSize.put(units, size);
    }

    public Double getMinSize(String units) {
        return minSize.get(units);
    }

    public void setMaxSize(String units, Double size) {
        if (size == null)
            maxSize.remove(units);
        else
            maxSize.put(units, size);
    }

    public Double getMaxSize(String units) {
        return maxSize.get(units);
    }

    public Set<String> getIncludedLabels() {
        return includedLabels;
    }

    public void setIncludedLabels(Set<String> includedLabels) {
        this.includedLabels = includedLabels;
    }

    public Set<String> getExcludedLabels() {
        return excludedLabels;
    }

    public void setExcludedLabels(Set<String> excludedLabels) {
        this.excludedLabels = excludedLabels;
    }

    public LegacyPhaseMapStrategy getLegacyPhaseMapStrategy() {
        return legacyPhaseMapStrategy;
    }

    public void setLegacyPhaseMapStrategy(
            LegacyPhaseMapStrategy legacyPhaseMapStrategy) {
        this.legacyPhaseMapStrategy = legacyPhaseMapStrategy;
    }

    private List query(String hql, Object... args) {
        for (Object oneArg : args) {
            if (oneArg instanceof Collection) {
                // when empty collections are bound to an argument in an HQL
                // query, Hibernate throws an exception. Since all of our
                // collection arguments are "in" clauses, an empty collection
                // arg means that the result will be empty too. Handle this
                // case specially to avoid receiving the exception.
                if (((Collection) oneArg).isEmpty())
                    return Collections.EMPTY_LIST;
            }
        }
        return query.queryHql(hql, args);
    }

    private List<Object[]> getEnactmentKeyList() {
        if (enactments == null) {
            Set<Integer> workflowKeys = getIncludedWorkflowKeys();
            if (Settings.isTeamMode())
                enactments = query(TEAM_ENACTMENT_QUERY, workflowKeys);
            else if (workflowHasProbePhase())
                enactments = query(PERSONAL_ENACTMENT_QUERY_PROBE, workflowKeys);
            else if (workflowHasConstructionPhase())
                enactments = query(PERSONAL_ENACTMENT_QUERY_NONPROBE,
                    workflowKey, workflowKeys);
            else
                enactments = query(TEAM_ENACTMENT_QUERY, workflowKeys);
            filterEnactments();
        }
        return enactments;
    }

    private List<Integer> getEnactmentRootKeys() {
        return QueryUtils.pluckColumn(getEnactmentKeyList(), EnactmentCol.RootKey);
    }

    /**
     * Query to find all the enactments of a given process for an entire team.
     * The rows returned will identify the key and identifier for the root nodes
     * of those enactments.
     */
    private static final String TEAM_ENACTMENT_QUERY = //
    "select distinct pe.rootItem.key, pe.rootItem.identifier, 0 "
            + "from ProcessEnactment pe "
            + "where pe.rootItem.key = pe.includesItem.key "
            + "and pe.process.key in (?)";

    /**
     * Query to find all the times when this individual user has performed a
     * PROBE task during the enactment of a given process. The rows returned
     * will identify the key and identifier for the root nodes of those
     * enactments.
     */
    private static final String PERSONAL_ENACTMENT_QUERY_PROBE = //
    "select distinct pe.rootItem.key, pe.rootItem.identifier, 0 "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join task.planItem.phase.mapsToPhase probePhase "
            + "where pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 "
            + "and probePhase.identifier = '*PROBE*/PROBE' "
            + "and pe.process.key in (?)";

    /**
     * Query to find all the times when this individual user has logged time to
     * a construction task during the enactment of a given process. The rows
     * returned will identify the key and identifier for the root nodes of those
     * enactments.
     */
    private static final String PERSONAL_ENACTMENT_QUERY_NONPROBE = //
    "select distinct pe.rootItem.key, pe.rootItem.identifier, 0 "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join task.planItem.phase.mapsToPhase constrPhase "
            + "where pe.includesItem.key = task.planItem.key "
            + "and task.actualTimeMin > 0 "
            + "and task.versionInfo.current = 1 "
            + "and constrPhase.process.key = ? "
            + "and constrPhase.typeName = 'Construction' "
            + "and pe.process.key in (?)";

    private enum EnactmentCol { RootKey, RootWbsID, Completed }; 


    private void filterEnactments() {
        if (includedEnactments != null || excludedEnactments != null)
            applyEnactmentWbsIDFilter();
        if (includedProjects != null || excludedProjects != null)
            applyProjectSpecificFilter();
        if (includedNames != null || excludedNames != null)
            applyNameSpecificFilter();
        if (onlyCompleted)
            discardIncompleteEnactments();
        if (minTime != null || maxTime != null)
            applyTimeSpecificFilter();
        if (!minSize.isEmpty() || !maxSize.isEmpty())
            applySizeSpecificFilter();
        if (includedLabels != null || excludedLabels != null)
            applyLabelSpecificFilter();
        discardNestedEnactments();
    }

    private void applyEnactmentWbsIDFilter() {
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] enactment = (Object[]) i.next();
            String rootItemID = get(enactment, EnactmentCol.RootWbsID);
            if (includedEnactments != null
                    && !includedEnactments.contains(rootItemID))
                i.remove();
            else if (excludedEnactments != null
                    && excludedEnactments.contains(rootItemID))
                i.remove();
        }
    }

    private void applyProjectSpecificFilter() {
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] enactment = (Object[]) i.next();
            String rootItemID = get(enactment, EnactmentCol.RootWbsID);
            if (shouldExcludeProject(rootItemID))
                i.remove();
        }
    }

    private boolean shouldExcludeProject(String planItemID) {
        String projectID = getProjectID(planItemID);
        if (projectID == null)
            return true;

        if (excludedProjects != null && excludedProjects.contains(projectID))
            return true;
        else if (includedProjects == null)
            return false;
        else
            return !includedProjects.contains(projectID);
    }

    private String getProjectID(String planItemID) {
        int colonPos = planItemID.indexOf(':');
        if (colonPos == -1)
            return null;
        else
            return planItemID.substring(0, colonPos);
    }


    private void applyNameSpecificFilter() {
        // get the names for the items in our enactment set
        Map<Integer, String> itemNames = new HashMap<Integer, String>();
        for (Object[] row : (List<Object[]>) query(ENACTMENT_INFO_QUERY,
            getEnactmentRootKeys())) {
            Integer rootKey = get(row, InfoCol.RootKey);
            String itemName = get(row, InfoCol.Element);
            String taskName = get(row, InfoCol.Task);
            if (taskName != null)
                itemName = itemName + "/" + taskName;
            itemNames.put(rootKey, itemName.toLowerCase());
        }

        // check whether the items match the inclusion/exclusion criteria
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] oneEnactment = (Object[]) i.next();
            Integer rootKey = get(oneEnactment, EnactmentCol.RootKey);
            String name = itemNames.get(rootKey);
            if (name == null)
                i.remove();
            else if (excludedNames != null && matchName(name, excludedNames))
                i.remove();
            else if (includedNames != null && !matchName(name, includedNames))
                i.remove();
        }
    }

    private boolean matchName(String name, Set<String> tokens) {
        for (String token : tokens) {
            if (name.contains(token))
                return true;
        }
        return false;
    }


    private void discardIncompleteEnactments() {
        loadEnactmentCompletionDates();

        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] oneEnactment = (Object[]) i.next();
            Date completed = get(oneEnactment, EnactmentCol.Completed);
            if (completed == null)
                i.remove();
            else if (cmp(onlyCompletedBefore, completed) < 0)
                i.remove();
            else if (cmp(onlyCompletedAfter, completed) > 0)
                i.remove();
        }
    }

    private int cmp(Date a, Date b) {
        return (a == null || b == null) ? 0 : a.compareTo(b);
    }

    private void loadEnactmentCompletionDates() {
        // no enactments? nothing to do
        if (getEnactmentKeyList().isEmpty())
            return;
        // if enactment dates have already been loaded, abort
        Object oneDate = get(enactments.get(0), EnactmentCol.Completed);
        if (oneDate == null || oneDate instanceof Date)
            return;

        // read enactment completion dates from the database
        Map<Object, Date> enactmentCompletionDates = QueryUtils.mapColumns( //
                query(ENACTMENT_COMPLETION_QUERY, getEnactmentRootKeys(),
                    getIncludedWorkflowKeys(), getWorkflowKey()));

        // save these dates into the list of enactments
        for (Object[] oneEnactment : enactments) {
            Object oneEnactmentRootKey = get(oneEnactment, EnactmentCol.RootKey);
            oneDate = enactmentCompletionDates.get(oneEnactmentRootKey);
            oneEnactment[EnactmentCol.Completed.ordinal()] = oneDate;
        }
    }

    /**
     * Query to determine finish dates of workflows that are 100% complete
     */
    private static final String ENACTMENT_COMPLETION_QUERY = //
    "select pe.rootItem.key, max(task.actualCompletionDate) "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) "
            + "and pe.process.key in (?) "
            + "and mapsTo.process.key = ? "
            + "and pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.rootItem.key "
            + "having max(task.actualCompletionDateDim.key) < 99990000";


    private void applyTimeSpecificFilter() {
        Map<Integer, Number> rawData = QueryUtils.mapColumns(query(
            TIME_FILTER_QUERY, getEnactmentRootKeys(),
            getIncludedWorkflowKeys(), getWorkflowKey()));
        applyNumericRangeFilter(rawData, minTime, maxTime);
    }

    private static final String TIME_FILTER_QUERY = //
    "select pe.rootItem.key, sum(task.actualTimeMin) "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) " //
            + "and mapsTo.process.key = ? "
            + "and pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.rootItem.key";


    private void applySizeSpecificFilter() {
        // get the list of units we are filtering by
        Set<String> units = new HashSet<String>();
        units.addAll(minSize.keySet());
        units.addAll(maxSize.keySet());

        // apply each size filter
        for (String oneUnit : units) {
            Map<Integer, Number> rawData = QueryUtils.mapColumns(query(
                SIZE_FILTER_QUERY, getEnactmentRootKeys(),
                getIncludedWorkflowKeys(), oneUnit));
            applyNumericRangeFilter(rawData, minSize.get(oneUnit),
                maxSize.get(oneUnit));
        }
    }

    private static final String SIZE_FILTER_QUERY = //
    "select pe.rootItem.key, sum(size.addedAndModifiedSize) "
            + "from ProcessEnactment pe, SizeFact size "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and pe.includesItem.key = size.planItem.key "
            + "and size.sizeMetric.shortName = ? "
            + "and size.measurementType.name = 'Actual' "
            + "and size.versionInfo.current = 1 "
            + "group by pe.rootItem.key";


    private void applyNumericRangeFilter(Map<Integer, Number> numericData,
            Double min, Double max) {
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] oneEnactment = (Object[]) i.next();
            Integer oneRootKey = get(oneEnactment, EnactmentCol.RootKey);
            Number oneValue = numericData.get(oneRootKey);
            double val = (oneValue == null ? 0 : oneValue.doubleValue());
            if (min != null && val < min)
                i.remove();
            else if (max != null && max <= val)
                i.remove();
        }
    }


    private void applyLabelSpecificFilter() {
        // scan label data, and make a note of enactments to include/exclude
        Set<Integer> toInclude = new HashSet();
        Set<Integer> toExclude = new HashSet();
        for (Object[] row : getLabelData()) {
            Integer oneRootKey = get(row, LabelCol.RootKey);
            String oneLabel = get(row, LabelCol.Label);
            if (excludedLabels != null && excludedLabels.contains(oneLabel))
                toExclude.add(oneRootKey);
            else if (includedLabels != null && includedLabels.contains(oneLabel))
                toInclude.add(oneRootKey);
        }

        // apply these decisions to the enactment list
        for (Iterator i = enactments.iterator(); i.hasNext();) {
            Object[] oneEnactment = (Object[]) i.next();
            Integer oneRootKey = get(oneEnactment, EnactmentCol.RootKey);
            if (toExclude.contains(oneRootKey))
                i.remove();
            else if (includedLabels != null && !toInclude.contains(oneRootKey))
                i.remove();
        }
    }


    private void discardNestedEnactments() {
        // within a project, enactments of different workflows can be nested
        // underneath each other. If two nested enactments both map to our
        // target workflow and both match our active filters, we must remove the
        // child workflow from our enactment set to avoid double-counting. The
        // data in the child will still be included in our analyses as a result
        // of its inclusion in the parent enactment.
        List<Integer> rootKeys = getEnactmentRootKeys();
        Set<Integer> nestedRoots = new HashSet(query(NESTED_ENACTMENT_QUERY,
            rootKeys, rootKeys));
        if (!nestedRoots.isEmpty()) {
            for (Iterator i = enactments.iterator(); i.hasNext();) {
                Object[] oneEnactment = (Object[]) i.next();
                Integer oneRootKey = get(oneEnactment, EnactmentCol.RootKey);
                if (nestedRoots.contains(oneRootKey))
                    i.remove();
            }
        }
    }

    /**
     * Query to find enactments in our target set that are nested underneath
     * another enactment in our target set
     */
    private static final String NESTED_ENACTMENT_QUERY = //
    "select distinct pe.includesItem.key " //
            + "from ProcessEnactment pe "
            + "where pe.rootItem.key in (?) "
            + "and pe.includesItem.key in (?) "
            + "and pe.rootItem.key <> pe.includesItem.key";


    public List<Enactment> getEnactments() {
        if (enactmentInfo == null)
            enactmentInfo = loadEnactmentInfo();
        return enactmentInfo;
    }

    private List<Enactment> loadEnactmentInfo() {
        // retrieve information about each of the enactments
        loadEnactmentCompletionDates();
        List<Object[]> rawInfo = query(ENACTMENT_INFO_QUERY,
            getEnactmentRootKeys());

        // use this data to create enactment info objects
        List<Enactment> result = new ArrayList(enactments.size());
        for (Object[] oneEnactment : enactments) {
            Enactment e = new Enactment();
            e.rootKey = (Integer) get(oneEnactment, EnactmentCol.RootKey);
            e.rootWbsID = get(oneEnactment, EnactmentCol.RootWbsID);
            e.completed = get(oneEnactment, EnactmentCol.Completed);
            for (Object[] row : rawInfo) {
                int rowKey = (Integer) get(row, InfoCol.RootKey);
                if (rowKey == e.rootKey) {
                    e.projectName = get(row, InfoCol.Project);
                    e.rootName = get(row, InfoCol.Element);
                    String task = get(row, InfoCol.Task);
                    if (task != null)
                        e.rootName = e.rootName + "/" + task;
                    result.add(e);
                    break;
                }
            }
        }

        // sort the enactments in chronological order of completion
        Collections.sort(result, new Comparator<Enactment>() {
            public int compare(Enactment e1, Enactment e2) {
                Date d1 = e1.completed;
                Date d2 = e2.completed;
                if (d1 == d2)
                    return 0;
                else if (d1 == null)
                    return 1;
                else if (d2 == null)
                    return -1;
                else
                    return d1.compareTo(d2);
            }
        });

        // return the results
        return result;
    }

    private static final String ENACTMENT_INFO_QUERY = //
    "select pi.key, pi.project.name, pi.wbsElement.name, task.name "
            + "from PlanItem as pi " //
            + "left join pi.task as task " //
            + "where pi.key in (?)";

    private enum InfoCol { RootKey, Project, Element, Task }


    /**
     * Print a list of the plan items whose data has contributed to this
     * historical data set.
     */
    public void debugPrintEnactments() {
        System.out.println("Process enactment steps for " + workflowName + ":");
        List<Object[]> enactmentSteps = query.queryHql(ENACTMENT_DEBUG_QUERY,
            getEnactmentRootKeys(), getIncludedWorkflowKeys(), getWorkflowKey());
        while (!enactmentSteps.isEmpty()) {
            Object[] oneRow = enactmentSteps.get(0);
            Integer rootKey = (Integer) oneRow[0];
            Object rootPlanItem = oneRow[1];
            String rootPath = rootPlanItem.toString();
            int rootPathLen = rootPath.length();

            List<String> includedPaths = new ArrayList<String>();
            for (Iterator i = enactmentSteps.iterator(); i.hasNext();) {
                Object[] includedRow = (Object[]) i.next();
                Integer includedRootKey = (Integer) includedRow[0];
                if (!includedRootKey.equals(rootKey))
                    continue;

                String includedPath = includedRow[2].toString();
                if (includedPath.startsWith(rootPath))
                    includedPath = includedPath.substring(rootPathLen + 1);
                includedPaths.add(includedPath);
                i.remove();
            }

            System.out.println(rootPath + " " + includedPaths);
        }
        System.out.println("-------------------------------------");
    }

    private static final String ENACTMENT_DEBUG_QUERY = //
    "select pe.rootItem.key, pe.rootItem, pe.includesItem "
            + "from ProcessEnactment pe "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and pe.includesItem.key <> pe.rootItem.key "
            + "and mapsTo.process.key = ? " //
            + "order by pe.rootItem.key";

    private Integer getWorkflowKey() {
        if (workflowKey == null)
            initWorkflowData();
        return workflowKey;
    }

    private Set<Integer> getIncludedWorkflowKeys() {
        if (includedWorkflowKeys == null)
            initWorkflowData();
        return includedWorkflowKeys;
    }

    private void initWorkflowData() {
        // find the key for the current workflow
        Object[] row = QueryUtils.singleValue(query(WORKFLOW_KEY_QUERY,
            workflowID));

        if (row == null || row[0] == null) {
            workflowKey = -1;
            includedWorkflowKeys = Collections.EMPTY_SET;
        } else {
            workflowKey = (Integer) row[0];
            workflowName = (String) row[1];
            // find all the workflows that map to the given workflow
            includedWorkflowKeys = new HashSet<Integer>(query.queryHql(
                WORKFLOW_MAPPING_QUERY, workflowKey));
        }
    }

    private static final String WORKFLOW_KEY_QUERY = //
    "select p.key, p.name from Process p where p.identifier = ?";

    private static final String WORKFLOW_MAPPING_QUERY = //
    "select distinct p.process.key from Phase p " //
            + "join p.mapsToPhase mapsTo " //
            + "where mapsTo.process.key = ?";


    private boolean workflowHasProbePhase() {
        Object test = QueryUtils.singleValue(query(WORKFLOW_PROBE_TEST,
            workflowKey));
        return test != null;
    }

    private static final String WORKFLOW_PROBE_TEST = //
    "select distinct 1 from Phase p " //
            + "join p.mapsToPhase probePhase " //
            + "where p.process.key = ? " //
            + "and probePhase.identifier = '*PROBE*/PROBE'";


    private boolean workflowHasConstructionPhase() {
        Object test = QueryUtils.singleValue(query(WORKFLOW_CONSTRUCTION_TEST,
            workflowKey));
        return test != null;
    }

    private static final String WORKFLOW_CONSTRUCTION_TEST = //
    "select distinct 1 from Phase p " //
            + "where p.process.key = ? " //
            + "and p.typeName = 'Construction'";



    public Map<String, String> getPhaseTypes() {
        return QueryUtils.mapColumns(getPhaseData(), PhaseCol.PhaseName,
            PhaseCol.PhaseType);
    }

    public List<String> getPhasesOfType(PhaseType... types) {
        Object type = (types.length == 0 ? null : Arrays.asList(types));
        List<String> result = new ArrayList<String>();
        for (Object[] row : getPhaseData()) {
            if (match(row, PhaseCol.PhaseType, type))
                result.add((String) get(row, PhaseCol.PhaseName));
        }
        return result;
    }

    public Map<String, String> getPhaseIdentifiers() {
        return QueryUtils.mapColumns(getPhaseData(), PhaseCol.PhaseName,
            PhaseCol.PhaseID);
    }

    private Map<Integer, String> getWorkflowStepsByKey() {
        return QueryUtils.mapColumns(getPhaseData(), PhaseCol.PhaseKey,
            PhaseCol.PhaseName);
    }

    private List<Object[]> _phaseData;

    private List<Object[]> getPhaseData() {
        if (_phaseData == null)
            _phaseData = query(WORKFLOW_PHASE_QUERY, getWorkflowKey());
        return _phaseData;
    }

    private static final String WORKFLOW_PHASE_QUERY = //
    "select phase.key, phase.shortName, phase.typeName, phase.identifier " //
            + "from Phase phase " //
            + "where phase.process.key = ? " //
            + "and phase.ordinal is not null " //
            + "order by phase.ordinal";

    private enum PhaseCol { PhaseKey, PhaseName, PhaseType, PhaseID };

    public enum PhaseType {
        Overhead, Construction, Appraisal, Failure
    }



    public Map<String, DataPair> getTotalTimeInPhase() {
        Map<String, DataPair> result = new LinkedHashMap();
        List<String> stepNames = QueryUtils.pluckColumn(getPhaseData(),
            PhaseCol.PhaseName);
        for (String step : stepNames)
            result.put(step, new DataPair());

        for (Object[] row : getTimeInPhaseData()) {
            String stepName = get(row, TimeCol.PhaseName);
            DataPair dataPair = result.get(stepName);
            if (dataPair == null) {
                stepName = "(" + stepName + ")";
                dataPair = new DataPair();
                result.put(stepName, dataPair);
            }
            dataPair.plan += ((Number) get(row, TimeCol.PlanTime)).doubleValue();
            dataPair.actual += ((Number) get(row, TimeCol.ActTime)).doubleValue();
        }

        DataPair total = new DataPair();
        for (DataPair phasePair : result.values())
            total.add(phasePair);
        result.put(TOTAL_PHASE_KEY, total);

        return result;
    }

    public double getTime(Enactment e, Object phaseFilter, boolean actual) {
        if (phaseFilter instanceof PhaseType)
            phaseFilter = getPhasesOfType((PhaseType) phaseFilter);
        TimeCol targetCol = (actual ? TimeCol.ActTime : TimeCol.PlanTime);
        return sum(getTimeInPhaseData(), targetCol, //
            TimeCol.RootKey, e, TimeCol.PhaseName, phaseFilter);
    }


    private List<Object[]> _timeInPhase;

    private List<Object[]> getTimeInPhaseData() {
        if (_timeInPhase == null)
            _timeInPhase = query(TIME_IN_PHASE_QUERY, getEnactmentRootKeys(),
                getIncludedWorkflowKeys(), getWorkflowKey());
        return _timeInPhase;
    }

    private static final String TIME_IN_PHASE_QUERY = //
    "select pe.rootItem.key, mapsTo.shortName, "
            + "sum(task.planTimeMin), sum(task.actualTimeMin) "
            + "from ProcessEnactment pe, TaskStatusFact task "
            + "join pe.includesItem.phase.mapsToPhase mapsTo "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and mapsTo.process.key = ? "
            + "and pe.includesItem.key = task.planItem.key "
            + "and task.versionInfo.current = 1 " //
            + "group by pe.rootItem.key, mapsTo.shortName";

    private enum TimeCol { RootKey, PhaseName, PlanTime, ActTime }



    public Map<String, DataPair> getAddedAndModifiedSizes() {
        Map<String, DataPair> result = new TreeMap();

        for (Object[] row : getSizeData()) {
            String sizeUnits = get(row, SizeCol.Units);
            String measurementType = get(row, SizeCol.MType);
            double sizeValue = ((Number) get(row, SizeCol.Size)).doubleValue();

            DataPair dataPair = result.get(sizeUnits);
            if (dataPair == null)
                result.put(sizeUnits, dataPair = new DataPair());

            if ("Plan".equals(measurementType))
                dataPair.plan += sizeValue;
            else if ("Actual".equals(measurementType))
                dataPair.actual += sizeValue;
        }

        return result;
    }

    public Set<String> getSizeUnits() {
        return new TreeSet(QueryUtils.pluckColumn(getSizeData(), SizeCol.Units));
    }

    public double getSize(Enactment e, String units, boolean actual) {
        String measurementType = (actual ? "Actual" : "Plan");
        return getSize(e, units, measurementType);
    }

    public double getSize(Enactment e, String units, String measurementType) {
        return sum(getSizeData(), SizeCol.Size, SizeCol.RootKey, e,
            SizeCol.Units, units, SizeCol.MType, measurementType);
    }

    private List<Object[]> _sizeData;

    private List<Object[]> getSizeData() {
        if (_sizeData == null)
            _sizeData = query(SIZE_QUERY, getEnactmentRootKeys(),
                getIncludedWorkflowKeys());
        return _sizeData;
    }

    /**
     * Query to find the size data for a set of process enactments.
     */
    private static final String SIZE_QUERY = //
    "select pe.rootItem.key, size.sizeMetric.shortName, " //
            + "size.measurementType.name, sum(size.addedAndModifiedSize) "
            + "from ProcessEnactment pe, SizeFact size "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) "
            + "and pe.includesItem.key = size.planItem.key "
            + "and size.versionInfo.current = 1 "
            + "group by pe.rootItem.key, size.sizeMetric.shortName, "
            + "  size.measurementType.name";

    private enum SizeCol { RootKey, Units, MType, Size }



    /**
     * Retrieve the actual defects injected and removed by workflow phase.
     * 
     * @return an array with three elements. The element at position 0 contains
     *         the injected defects by phase, and the element at position 1
     *         contains the removed defects. Although the Map values are
     *         {@link DataPair} objects, only their "actual" value will contain
     *         data; the "plan" value will be zero. The two maps will contain
     *         entries for "Before Development" and "After Development", in
     *         addition to a Total row. The Map in position 2 of the result will
     *         contain a single entry, with the Total key and a DataPair whose
     *         actual field gives the number of unrecognized defects (whose
     *         injection and removed phases are both not part of this workflow).
     */
    public Map<String, DataPair>[] getDefectsByPhase() {
        return getDefectsByPhase(null);
    }

    private Map<String, DataPair>[] getDefectsByPhase(Enactment e) {
        List<String> stepNames = QueryUtils.pluckColumn(getPhaseData(),
            PhaseCol.PhaseName);
        Map<String, DataPair>[] result = new Map[3];
        result[INJ] = new LinkedHashMap<String, DataPair>();
        result[REM] = new LinkedHashMap<String, DataPair>();
        result[INJ].put(Defect.BEFORE_DEVELOPMENT, new DataPair());
        for (String step : stepNames) {
            result[INJ].put(step, new DataPair());
            result[REM].put(step, new DataPair());
        }
        result[REM].put(Defect.AFTER_DEVELOPMENT, new DataPair());
        DataPair unrecognized = new DataPair();
        result[UNK] = Collections.singletonMap(TOTAL_PHASE_KEY, unrecognized);
        DataPair total = new DataPair();
        result[INJ].put(TOTAL_PHASE_KEY, total);
        result[REM].put(TOTAL_PHASE_KEY, total);

        addDefectData(result, unrecognized, total, e);

        return result;
    }

    private void addDefectData(Map<String, DataPair>[] result,
            DataPair unrecognized, DataPair total, Enactment e) {

        for (Object[] row : getDefectData()) {
            if (!match(row, DefectCol.RootKey, e))
                continue;

            String injPhaseName = get(row, DefectCol.Inj);
            String remPhaseName = get(row, DefectCol.Rem);
            int fixCount = ((Number) get(row, DefectCol.FixCount)).intValue();

            // if neither phase was recognized, this is most likely a legacy
            // defect that was recorded against MCF buckets. But even if it
            // wasn't, it is a defect that spanned this workflow process and
            // isn't relevant to our calculations. Disregard it.
            if (injPhaseName == null && remPhaseName == null) {
                unrecognized.actual += fixCount;
                continue;
            }

            if (injPhaseName == null)
                injPhaseName = Defect.BEFORE_DEVELOPMENT;
            result[INJ].get(injPhaseName).actual += fixCount;

            if (remPhaseName == null)
                remPhaseName = Defect.AFTER_DEVELOPMENT;
            result[REM].get(remPhaseName).actual += fixCount;

            total.actual += fixCount;
        }
    }

    public static final int INJ = 0, REM = 1, UNK = 2;

    private List<Object[]> _defectData;

    private List<Object[]> getDefectData() {
        if (_defectData == null) {
            _defectData = query(DEFECT_QUERY_1, getEnactmentRootKeys(),
                getIncludedWorkflowKeys());
            if (_defectData == Collections.EMPTY_LIST)
                _defectData = new ArrayList<Object[]>();
            _defectData.addAll(query(DEFECT_QUERY_2, getWorkflowKey(),
                getIncludedWorkflowKeys()));
            mapDefectPhases(_defectData);
        }
        return _defectData;
    }

    private void mapDefectPhases(List<Object[]> defects) {
        // get the direct list of phases that belong to the workflow.
        Map<Integer, String> stepMap = getWorkflowStepsByKey();

        // now, look for any inj/rem phases whose keys we don't recognize.
        Set<Integer> missingKeys = new HashSet<Integer>();
        for (Object[] oneDefect : defects) {
            missingKeys.add((Integer) get(oneDefect, DefectCol.Inj));
            missingKeys.add((Integer) get(oneDefect, DefectCol.Rem));
        }
        missingKeys.removeAll(stepMap.keySet());

        // if we find missing phases, see if those phases have an explicit
        // mapping to one of the phases in our workflow. Add all such mappings.
        if (!missingKeys.isEmpty()) {
            Map forwardMappings = QueryUtils.mapColumns(query(PHASE_MAP_QUERY,
                missingKeys, getWorkflowKey()));
            stepMap.putAll(forwardMappings);
            missingKeys.removeAll(forwardMappings.keySet());
        }

        // legacy defects were collected against MCF buckets, and those buckets
        // will not have mappings into our workflow. But the workflow is likely
        // to have mappings into the MCF. If only one workflow phase maps to a
        // given MCF phase, we can infer a correspondence between those phases.
        if (!missingKeys.isEmpty()) {
            stepMap.putAll(getBackwardPhaseMappings(missingKeys));
        }

        // use the mappings above to map inj/rem phases into this workflow.
        for (Object[] oneDefect : defects) {
            mapPhase(oneDefect, DefectCol.Inj, stepMap);
            mapPhase(oneDefect, DefectCol.Rem, stepMap);
        }
    }

    private Map getBackwardPhaseMappings(Set<Integer> missingKeys) {
        if (legacyPhaseMapStrategy == LegacyPhaseMapStrategy.Disabled)
            return Collections.EMPTY_MAP;

        // do a search to find backward/inferred phase mappings
        List<Object[]> rows = query(PHASE_MAP_QUERY_2, missingKeys,
            getWorkflowKey());

        // search the rows for items in position 0 that appear more than once.
        Set seen = new HashSet();
        Set dups = new HashSet();
        for (Object[] row : rows) {
            Object oneKey = row[0];
            if (seen.add(oneKey) == false)
                dups.add(oneKey);
        }

        // if we found duplicates, and the phase mapping strategy is "strict,"
        // don't use any of the backward mappings at all.
        if (legacyPhaseMapStrategy == LegacyPhaseMapStrategy.Strict
                && !dups.isEmpty())
            return Collections.EMPTY_MAP;

        // create a map of the phases we found, discarding any duplicates.
        Map result = QueryUtils.mapColumns(rows);
        result.keySet().removeAll(dups);
        return result;
    }

    private void mapPhase(Object[] defect, DefectCol col,
            Map<Integer, String> stepMap) {
        Integer phaseKey = get(defect, col);
        String phaseName = stepMap.get(phaseKey);
        defect[col.ordinal()] = phaseName;
    }

    /*
     * Query to find defects logged against one of our included enactments
     */
    private static final String DEFECT_QUERY_1 = "select " //
            + "pe.rootItem.key, " //
            + "defect.injectedPhase.key, " //
            + "defect.removedPhase.key, " //
            + "defect.fixCount " //
            + "from ProcessEnactment pe, DefectLogFact defect " //
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) " //
            + "and pe.includesItem.key = defect.planItem.key " //
            + "and defect.versionInfo.current = 1";

    /*
     * Query to find defects that were injected in one of our workflow phases,
     * but were not logged against an enactment of this workflow
     */
    private static final String DEFECT_QUERY_2 = "select 0, " //
            + "injected.key, " //
            + "defect.removedPhase.key, " //
            + "defect.fixCount " //
            + "from DefectLogFact as defect " //
            + "join defect.injectedPhase.mapsToPhase injected " //
            + "where injected.process.key = ? " //
            + "and not exists ( " //
            + "    from ProcessEnactment pi " //
            + "    where defect.planItem.key = pi.includesItem.key " //
            + "    and pi.process.key in (?)) "
            + "and defect.versionInfo.current = 1";

    private enum DefectCol { RootKey, Inj, Rem, FixCount }

    /*
     * Query to find the workflow phases that a set of other phases map to
     */
    private static final String PHASE_MAP_QUERY = "select " //
            + "missingPhase.key, " //
            + "targetPhase.shortName " //
            + "from Phase missingPhase " //
            + "join missingPhase.mapsToPhase targetPhase " //
            + "where missingPhase.key in (?) " //
            + "and targetPhase.process.key = ?";

    /*
     * Query to find inferred workflow phase mappings
     */
    private static final String PHASE_MAP_QUERY_2 = "select " //
            + "missingPhase.key, " //
            + "targetPhase.shortName " //
            + "from Phase targetPhase " //
            + "join targetPhase.mapsToPhase missingPhase " //
            + "where missingPhase.key in (?) " //
            + "and targetPhase.process.key = ?";


    /**
     * @return an array with two elements: the first entry gives the process
     *         yields for each workflow step, and the second gives the phase
     *         yields.
     */
    public Map<String, DataPair>[] getYields() {
        return getYields(null);
    }

    private Map<String, DataPair>[] getYields(Enactment e) {
        Map<String, DataPair>[] result = new Map[2];
        result[0] = new LinkedHashMap<String, DataPair>();
        result[1] = new LinkedHashMap<String, DataPair>();
        
        Map<String, DataPair>[] defectsByPhase = getDefectsByPhase(e);
        calculateYields(defectsByPhase[INJ], defectsByPhase[REM],
            result[0], result[1]);
        
        return result;
    }

    private void calculateYields(Map<String, DataPair> inj,
            Map<String, DataPair> rem, Map<String, DataPair> processYields,
            Map<String, DataPair> phaseYields) {

        // calculate cumulative defects injected and removed so far by phase
        Map<String, DataPair> cumInj = cum(inj);
        Map<String, DataPair> cumRem = cum(rem);
        cumRem.remove(Defect.AFTER_DEVELOPMENT);

        // special handling for the first phase
        Iterator<String> phaseNames = cumRem.keySet().iterator();
        String firstPhase = phaseNames.next();
        DataPair firstPhaseYield = new DataPair(rem.get(firstPhase))
                .divide(cumInj.get(firstPhase));
        phaseYields.put(firstPhase, firstPhaseYield);
        String prevPhase = firstPhase;

        // iterate over remaining phases and calculate yields
        while (phaseNames.hasNext()) {
            String phase = phaseNames.next();
            DataPair processYield = new DataPair(cumRem.get(prevPhase))
                    .divide(cumInj.get(prevPhase));
            processYields.put(phase, processYield);

            DataPair phaseYield = new DataPair(rem.get(phase))
                    .divide(new DataPair(cumInj.get(phase)).subtract(cumRem
                            .get(prevPhase)));
            phaseYields.put(phase, phaseYield);
            prevPhase = phase;
        }

        // write an entry for total process yield
        DataPair totalProcessYield = new DataPair(cumRem.get(prevPhase))
                .divide(cumInj.get(prevPhase));
        processYields.put(TOTAL_PHASE_KEY, totalProcessYield);
    }

    private Map<String, DataPair> cum(Map<String, DataPair> phaseData) {
        Map<String, DataPair> result = new LinkedHashMap<String, DataPair>();
        DataPair cum = new DataPair();
        for (Entry<String, DataPair> e : phaseData.entrySet()) {
            cum.add(e.getValue());
            result.put(e.getKey(), new DataPair(cum));
        }
        result.remove(TOTAL_PHASE_KEY);
        return result;
    }


    public Set<String> getLabels() {
        return getLabels(null);
    }

    private Set<String> getLabels(Enactment e) {
        Set<String> result = new TreeSet<String>();
        for (Object[] row : getLabelData()) {
            if (match(row, LabelCol.RootKey, e))
                result.add((String) get(row, LabelCol.Label));
        }
        return result;
    }

    private List<Object[]> labelData;

    private List<Object[]> getLabelData() {
        if (labelData == null)
            labelData = query(LABEL_QUERY, getEnactmentRootKeys(),
                getIncludedWorkflowKeys());
        return labelData;
    }

    private static final String LABEL_QUERY = //
    "select distinct pe.rootItem.key, attr.value.text "
            + "from ProcessEnactment pe, PlanItemAttrFact attr "
            + "where pe.rootItem.key in (?) " //
            + "and pe.process.key in (?) " //
            + "and pe.includesItem.key = attr.planItem.key "
            + "and attr.attribute.identifier = 'plan_item.label' "
            + "and attr.versionInfo.current = 1";

    private enum LabelCol { RootKey, Label }



    private <T> T get(Object[] row, Enum column) {
        return (T) row[column.ordinal()];
    }

    private boolean match(Object[] row, Enum column, Object criteria) {
        if (criteria == null) {
            return true;

        } else if (criteria instanceof Collection) {
            for (Object oneCriteria : (Collection) criteria)
                if (match(row, column, oneCriteria))
                    return true;
            return false;

        } else if (criteria instanceof Enactment) {
            int value = ((Number) get(row, column)).intValue();
            return ((Enactment) criteria).rootKey == value;

        } else if (criteria instanceof Enum || criteria instanceof String) {
            return criteria.toString().equals(get(row, column));

        } else
            throw new IllegalArgumentException("Unrecognized criteria "
                    + criteria);
    }

    private double sum(List<Object[]> rows, Enum targetCol, Object... filters) {
        double result = 0;
        ROW: for (Object[] row : rows) {
            for (int i = 0; i < filters.length; i += 2) {
                Enum filterColumn = (Enum) filters[i];
                Object filterCriteria = filters[i + 1];
                if (!match(row, filterColumn, filterCriteria))
                    continue ROW;
            }
            Object targetVal = get(row, targetCol);
            if (targetVal instanceof Number)
                result += ((Number) targetVal).doubleValue();
        }
        return result;
    }

}
