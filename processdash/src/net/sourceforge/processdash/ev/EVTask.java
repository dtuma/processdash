// Copyright (C) 2001-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.DateAdjuster;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class EVTask implements Cloneable, DataListener {

    public static final String PLAN_TIME_DATA_NAME      = "Estimated Time";
    public static final String ACT_TIME_DATA_NAME       = "Time";
    public static final String NODE_TYPE_DATA_NAME      = "Node_Type";
    public static final String NODE_TYPE_SPEC_DATA_NAME = "Node_Type_Spec";
    public static final String MISSING_NODE_TYPE        = "?????";
    public static final String DATE_COMPLETED_DATA_NAME = "Completed";
    public static final String NODE_IS_ROLLUP_DATA_NAME = "Rollup Tag";
    private static final String LEVEL_OF_EFFORT_PREFIX  = "TST-LOE_";
    private static final String TASK_ORDINAL_PREFIX  = "TST-TSK#_";
    private static final String TASK_PRUNING_PREFIX  = "TST-PRUNED_";

    public interface Listener {
        public void evNodeChanged(EVTask node, boolean needsRecalc);
    }

    EVTask parent = null;
    ArrayList<EVTask> children = new ArrayList<EVTask>();
    String flag;
    List<String> taskIDs = null;
    List<EVTaskDependency> dependencies = null;
    List<String> assignedTo = null;

    String name, fullName, taskListName;
    Listener listener;
    DataRepository data;


    /* The percentage of time a user plans to spend on this task, as a level
     * of effort.
     */
    double planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
    double rollupLevelOfEffort = 0;

    public static final int NOT_LEVEL_OF_EFFORT = -1;
    public static final int ANCESTOR_LEVEL_OF_EFFORT = 0;


    /** Value indicating user-requested reordering of the task list.
     * Values of interest:<ul>
     *
     * <li>0 - indicates that the order is unknown and needs to be inferred
     *     from the context of this node.
     * <li>&gt;= 1 - the order of this node in the list
     */
    int taskOrdinal = INFER_FROM_CONTEXT;
    int savedTaskOrdinal = INFER_FROM_CONTEXT;

    /** Value indicating user-requested pruning of the task list.
     * Values of interest:<ul>
     *
     * <li>0 - indicates that the pruning state is unknown and needs to be
     *     inferred from the context of this node.
     * <li>-1 - indicates that the user has explicitly pruned this node.
     * <li>-2 - indicates that this node has inherited its pruned status from
     *     an ancestor.
     *  <li>1 - indicates that the user has explicitly un-pruned this node.
     *</ul>
     */
    int pruningFlag = INFER_FROM_CONTEXT;
    int savedPruningFlag = INFER_FROM_CONTEXT;

    public static final int INFER_FROM_CONTEXT = 0;
    public static final int USER_PRUNED = -1;
    public static final int ANCESTOR_PRUNED = -2;
    public static final int USER_UNPRUNED = 1;


    /** The type of node represented by this task (Generally used by a process
     * definition to capture process-specific information.) */
    String nodeType;
    /** True if the node type is an editable value */
    boolean nodeTypeEditable;
    /** Information about acceptable node types */
    ListData nodeTypeSpec;


    /** The time (minutes) the user plans to spend in this node, taken
     * directly from the data repository. */
    double topDownPlanTime;
    /** The time (minutes) the user plans to spend in this node, calculated
     * by adding up plan times of children */
    double bottomUpPlanTime;
    // various flags which determine how we should interpret the top down
    // plan time for this node.
    boolean planTimeEditable, planTimeNull, planTimeUndefined,
        ignorePlanTimeValue = false;



    /** The plan time (minutes) for this node, determined intelligently from
     * the top down and bottom up times for this node. */
    double planTime;
    /** The baselined plan time for this node. */
    double baselineTime;
    /** The portion of the plan time that "counts" toward this schedule
     * (minutes) */
    double planValue;
    /** The total plan value spent in this node and all prior nodes. */
    double cumPlanValue;

    /** Actual time (minutes) spent in this node before the start of the
     * schedule */
    double actualPreTime;
    /** Actual time (minutes) spent in this node during the schedule */
    double actualNodeTime;
    /** The total time (minutes) actually spent in this node and its children,
     * both before and during this schedule  */
    double actualTime;
    /** The total time (minutes) actually spent during this schedule
     * in this node and its children */
    double actualCurrentTime;
    /** The total time (minutes) actually spent during this schedule
     * in this node and its children on tasks that count toward earned value */
    double actualDirectTime;
    /** Actual value earned (minutes) in this node and its children. */
    double valueEarned;

    /** The date we plan to start this task */
    Date planStartDate;
    /** The date this task might start, based on current schedule slip */
    Date replanStartDate;
    /** The date this task might start, based on current forecasts */
    Date forecastStartDate;
    /** The date we planned to start this task in the baseline schedule */
    Date baselineStartDate;
    /** The date we actually started this task */
    Date actualStartDate;
    /** The date we plan to complete this task */
    Date planDate;
    /** The date we planned to complete this task in the baseline schedule */
    Date baselineDate;
    /** The date the task could complete, based on current schedule slip */
    Date replanDate;
    /** The date this task is forecast to be completed */
    Date forecastDate;
    /** The date this task was actually completed */
    Date dateCompleted;
    /** True if the user can edit the completion date for this task */
    boolean dateCompletedEditable;
    /** If this is a task that will never finish because its schedule is
     * overspent, this field can be used to store nominal task plan dates */
    DateRange overspentPlanDates;
    /** If this is a task that will never finish because its schedule is
     * overspent, this field can be used to store nominal task replan dates */
    DateRange overspentReplanDates;
    /** If this is a task that will never finish because its schedule is
     * overspent, this field can be used to store nominal task forecast dates */
    DateRange overspentForecastDates;
    /** Note associated with this task, if applicable */
    Map<String, HierarchyNote> noteData;

    private static final Date COMPLETION_DATE_NA = EVSchedule.A_LONG_TIME_AGO;

    static Resources resources = Resources.getDashBundle("EV");

    /** Creates an EVTask suitable for the root of an EVTaskList.  */
    public EVTask(String rootName) {
        this.name = rootName;
        this.fullName = "";
        planTime = cumPlanValue = actualTime = valueEarned =
            topDownPlanTime = bottomUpPlanTime = actualNodeTime = 0;
        planDate = dateCompleted = null;
        listener = null;
        planTimeEditable = dateCompletedEditable = planTimeUndefined = false;
        planTimeNull = true;
        data = null;
    }

    /** Add a child task to this EVTask. */
    public boolean add(EVTask child) {
        if (containsNode(children, child))
            return false;

        forceAdd(child);
        return true;
    }

    /** Add a child task to this EVTask, even if if some other child with
     * the same name is present. */
    protected void forceAdd(EVTask child) {
        child.parent = this;
        children.add(child);
    }

    /** Add a child task to this EVTask. */
    public boolean add(int pos, EVTask child) {
        if (containsNode(children, child))
            return false;

        child.parent = this;
        children.add(pos, child);
        return true;
    }

    /** Remove a child task from this EVTask */
    public int remove(EVTask child) {
        int pos = indexOfNode(children, child);
        if (pos != -1)
            children.remove(pos);
        return pos;
    }

    /** Replace a child task of this EVTask
     * WARNING: no checks are performed on the parameters. This method
     * is <b>only</b> meant to be called when recalculations on a child
     * caused a replacement object to be created (rather than just
     * mutations within the existing object).  This method should <b>NOT</b>
     * be used to replace one task with an entirely different task - use
     * remove() and add() for that.
     */
    void replace(int pos, EVTask newChild) {
        children.set(pos, newChild);
        newChild.parent = this;
    }

    public void moveUp(int childPos) {
        if (childPos > 0 && childPos < children.size()) {
            EVTask a = children.get(childPos-1);
            EVTask b = children.get(childPos);

            children.set(childPos-1, b);
            children.set(childPos,   a);
        }
    }

    public boolean sameNode(EVTask that) {
        if ("".equals(this.fullName))
            // compare root nodes by examining their node name.
            return ("".equals(that.fullName)
                    && cmpStrings(this.name, that.name));
        else
            // compare regular nodes by examining their full name.
            return cmpStrings(this.fullName, that.fullName);
    }
    private boolean cmpStrings(String a, String b) {
        if (a == b) return true;
        if (a != null) return a.equals(b);
        return false;
    }

    /** Creates an EVTask for the tasks in the hierarchy at the given path */
    public EVTask(String taskListName, String hierarchyPath, DataRepository data,
                  DashHierarchy hierarchy, Listener listener) {
        this(null, taskListName, hierarchyPath.substring(1), hierarchyPath,
             null, data, hierarchy, listener);
    }


    protected EVTask(EVTask parent, String taskListName, String name,
                     String fullName, PropertyKey key, DataRepository data,
                     DashHierarchy hierarchy, Listener listener)
    {
        this.parent = parent;
        this.name = name;
        this.fullName = fullName;
        this.taskListName = taskListName;
        this.data = data;
        this.listener = listener;

        if (getValue(NODE_IS_ROLLUP_DATA_NAME) != null) {
            ignorePlanTime();
        } else {
            setPlanTime(getValue(PLAN_TIME_DATA_NAME));
            setActualTime(getValue(ACT_TIME_DATA_NAME));
        }

        if (parent == null || !StringUtils.hasValue(parent.fullName))
            nodeTypeSpec = ListData.asListData(data.getInheritableValue(
                    fullName, NODE_TYPE_SPEC_DATA_NAME));
        else
            nodeTypeSpec = ListData.asListData(getValue(
                    NODE_TYPE_SPEC_DATA_NAME, false));
        setNodeType(getValue(NODE_TYPE_DATA_NAME));

        setLevelOfEffort(getValue(getLevelOfEffortDataname()));
        loadStructuralData();
        loadDependencyInformation();
        loadTaskNote();

        addChildrenFromHierarchy(fullName, key, data, hierarchy, listener);

        if (isLeaf()) {
            setActualDate(getValue(DATE_COMPLETED_DATA_NAME));
        }
    }

    /** Attempt to find children in the hierarchy, and add them to our
     *  list of children.
     * @return true if any children were found and added.
     */
    protected boolean addChildrenFromHierarchy(String fullName,
                     PropertyKey key, DataRepository data,
                     DashHierarchy hierarchy, Listener listener)
    {
        boolean addedChild = false;

        if (key == null)
            // make an attempt to lookup the name in the hierarchy.
            key = hierarchy.findExistingKey(fullName);

        if (key != null) {
            int numKids = hierarchy.getNumChildren(key);
            for (int i = 0;   i < numKids;  i++) {
                PropertyKey child = hierarchy.getChildKey(key, i);
                children.add(new EVTask(this, taskListName,
                                        child.name(), child.path(), child,
                                        data, hierarchy, listener));
                addedChild = true;
            }
        }
        return addedChild;
    }

    public EVTask(Element e) { this(e, null); }
    private EVTask(Element e, String parentName) {
        name = e.getAttribute("name");
        fullName = (parentName == null ? "" : parentName + "/" + name);

        planValue = EVSchedule.getXMLNum(e, "pt");
        planTime = EVSchedule.getXMLNum(e, "ptt", planValue);
        topDownPlanTime = bottomUpPlanTime = planTime;
        actualTime = EVSchedule.getXMLNum(e, "at");
        actualDirectTime = EVSchedule.getXMLNum(e, "adt", actualTime);
        actualCurrentTime = EVSchedule.getXMLNum(e, "act", actualDirectTime);
        planStartDate = EVSchedule.getXMLDate(e, "psd");
        actualStartDate = EVSchedule.getXMLDate(e, "asd");
        planDate = EVSchedule.getXMLDate(e, "pd");
        replanDate = EVSchedule.getXMLDate(e, "rpd");
        forecastDate = EVSchedule.getXMLDate(e, "fd");
        dateCompleted = EVSchedule.getXMLDate(e, "cd");
        if (e.hasAttribute("loe"))
            planLevelOfEffort = EVSchedule.getXMLNum(e, "loe");
        if (e.hasAttribute("ord"))
            taskOrdinal = (int) EVSchedule.getXMLNum(e, "ord");
        if (e.hasAttribute("prune"))
            pruningFlag = (int) EVSchedule.getXMLNum(e, "prune");
        taskIDs = parseListAttr(e, "tid");
        assignedTo = parseListAttr(e, "who");
        flag = e.getAttribute("flag");
        if (!XMLUtils.hasValue(flag))
            flag = null;
        if (e.hasAttribute("nt"))
            nodeType = e.getAttribute("nt");
        if (e.hasAttribute("nts"))
            nodeTypeSpec = new ListData(e.getAttribute("nts"));

        planTimeEditable = planTimeNull = planTimeUndefined = false;
        actualPreTime = 0;

        List childElements = XMLUtils.getChildElements(e);
        for (Iterator i = childElements.iterator(); i.hasNext();) {
            Element c = (Element) i.next();
            if ("task".equals(c.getTagName()))
                add(new EVTask(c, fullName));
            else if (EVTaskDependency.DEPENDENCY_TAG.equals(c.getTagName()))
                getDependencies(true).add(new EVTaskDependency(c));
            else if (HierarchyNote.NOTE_TAG.equals(c.getTagName()))
                try { setTaskNote(new HierarchyNote(c)); } catch (Exception ex) {}
        }
    }

    private List<String> parseListAttr(Element e, String attrName) {
        String value = e.getAttribute(attrName);
        if (!XMLUtils.hasValue(value))
            return null;
        String[] items = value.split(",");
        return new ArrayList<String>(Arrays.asList(items));
    }
    protected boolean hasValue(Collection c) {
        return (c != null && c.isEmpty() == false);
    }

    protected SimpleData getValue(String name) { return getValue(name, true); }
    protected SimpleData getValue(String name, boolean notify) {
        String dataName = DataRepository.createDataName(fullName, name);
        if (notify && listener != null) {
            if (listeningToData == null)
                listeningToData = new HashSet();
            if (!listeningToData.contains(dataName))
                data.addDataListener(dataName, this, false);
        }
        return data.getSimpleValue(dataName);
    }
    private Set listeningToData = null;


    /** Returns true if the planned time for this task is a user-editable value.
     */
    public boolean isPlannedTimeEditable() {
        return (planTimeEditable &&
                (planLevelOfEffort != ANCESTOR_LEVEL_OF_EFFORT));
    }

    /** Returns true if the actual completion date for this task is a
     * user-editable value.
     */
    public boolean isCompletionDateEditable() {
        return dateCompletedEditable &&
            !isLevelOfEffortTask() && !isUserPruned();
    }

    protected void setPlanTime(SimpleData time) {
        if (time instanceof NumberData) {
            if (!ignorePlanTimeValue) {
                topDownPlanTime = ((NumberData) time).getDouble();
                if (Double.isNaN(topDownPlanTime) ||
                    Double.isInfinite(topDownPlanTime))
                    topDownPlanTime = 0.0;
                planTimeEditable = time.isEditable();
                planTimeUndefined = !time.isDefined();
                planTimeNull = false;
            }
        } else {
            planTimeNull = (time == null);
            topDownPlanTime = 0;
            planTimeEditable = true;
            planTimeUndefined = false;
        }
    }

    protected void ignorePlanTime() {
        ignorePlanTimeValue = true;
        topDownPlanTime = 0;
        planTimeNull = true;
        planTimeEditable = planTimeUndefined = false;
    }

    private String getLevelOfEffortDataname() {
        return LEVEL_OF_EFFORT_PREFIX + taskListName;
    }

    /** Returns true if this task represents "level of effort" work instead of
     * earned value work
     */
    public boolean isLevelOfEffortTask() {
        return (planLevelOfEffort >= 0);
    }

    private void setLevelOfEffort(SimpleData levelOfEffort) {
        if (levelOfEffort instanceof NumberData) {
            planLevelOfEffort = ((NumberData) levelOfEffort).getDouble();
            if (!(planLevelOfEffort > 0 && planLevelOfEffort < 1))
                planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
        } else {
            planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
        }
    }

    private void userSetLevelOfEffort(String value) {
        double p = NOT_LEVEL_OF_EFFORT;

        if (value == null || value.trim().length() == 0) {
            p = 0;
        } else try {
            Number percentage = percentFormatter.parse(value);
            p = percentage.doubleValue();
        } catch (ParseException e) {}

        if (p == 0) {
            planLevelOfEffort = NOT_LEVEL_OF_EFFORT;
            // erase the level of effort in the data repository
            data.userPutValue
                (DataRepository.createDataName(fullName, getLevelOfEffortDataname()),
                 null);
        } else if (p > 0 && p < 1) {
            planLevelOfEffort = p;
            // save this level of effort to the data repository
            data.userPutValue
                (DataRepository.createDataName(fullName, getLevelOfEffortDataname()),
                 new DoubleData(planLevelOfEffort, true));
        }
    }


    private void loadStructuralData() {
        SimpleData d = getValue(TASK_ORDINAL_PREFIX + taskListName);
        if (d instanceof NumberData)
            taskOrdinal = savedTaskOrdinal = ((NumberData) d).getInteger();

        d = getValue(TASK_PRUNING_PREFIX + taskListName);
        if (d instanceof NumberData)
            pruningFlag = savedPruningFlag = ((NumberData) d).getInteger();
    }

    /** Save any structural data about this node to the repository.
     */
    void saveStructuralData(String newTaskListName) {

        saveDataElement(taskListName, newTaskListName, TASK_ORDINAL_PREFIX,
            savedTaskOrdinal, taskOrdinal, INFER_FROM_CONTEXT);

        int effectivePruningFlag = pruningFlag;
        if (effectivePruningFlag == ANCESTOR_PRUNED)
            effectivePruningFlag = INFER_FROM_CONTEXT;
        saveDataElement(taskListName, newTaskListName, TASK_PRUNING_PREFIX,
            savedPruningFlag, effectivePruningFlag, INFER_FROM_CONTEXT);

        taskListName = newTaskListName;
        savedTaskOrdinal = taskOrdinal;
        savedPruningFlag = effectivePruningFlag;

        for (int i = 0;   i < getNumChildren();   i++)
            getChild(i).saveStructuralData(newTaskListName);
    }



    private void saveDataElement(String oldTaskListName,
        String newTaskListName, String dataNamePrefix, int savedValue,
        int newValue, int defaultValue)
    {
        if (fullName == null || fullName.length() == 0) return;

        String oldDataName = null;
        if (savedValue != defaultValue)
            oldDataName = dataNamePrefix + oldTaskListName;

        if (newTaskListName != null && newValue != defaultValue) {
            String newDataName = dataNamePrefix + newTaskListName;
            if (newDataName.equals(oldDataName)) oldDataName = null;
            if (newValue != savedValue || oldDataName != null) {
                SimpleData d = new DoubleData(newValue, false);
                String dataName =
                    DataRepository.createDataName(fullName, newDataName);
                data.putValue(dataName, d);
            }
        }

        if (oldDataName != null) {
            String dataName =
                DataRepository.createDataName(fullName, oldDataName);
            data.putValue(dataName, null);
        }
    }


    protected void loadDependencyInformation() {
        this.taskIDs = EVTaskDependency.getTaskIDs(data, fullName);
        this.dependencies = EVTaskDependency.getDependencies(data, fullName);
    }

    protected void saveDependencyInformation() {
        if (fullName != null && fullName.length() > 0)
            EVTaskDependency.saveDependencies(data, fullName, dependencies);

        for (int i = 0;   i < getNumChildren();   i++)
            getChild(i).saveDependencyInformation();
    }

    protected void loadTaskNote() {
        noteData = HierarchyNoteManager.getNotesForPath(data, fullName);
    }

    /** This method is NOT a public API, because it does not interface with
     * the real persistence mechanism for task notes.  This method simply
     * provides a way to attach a transient note to this EV task. The note
     * will be lost whenever the object is discarded.
     */
    protected void setTaskNote(HierarchyNote note) {
        if (note == null) {
            noteData = null;
        } else {
            noteData = Collections.singletonMap(HierarchyNoteManager.NOTE_KEY,
                note);
        }
    }

    protected void resetActualDate() {
        if (data != null && fullName != null)
            setActualDate(getValue(DATE_COMPLETED_DATA_NAME));
    }

    protected void setActualDate(SimpleData date) {
        if (date instanceof DateData) {
            dateCompleted = ((DateData) date).getValue();
            dateCompletedEditable = date.isEditable();
        } else {
            dateCompleted = null;
            dateCompletedEditable = true;
        }
    }

    protected void setActualTime(SimpleData time) {
        if (time instanceof NumberData) {
            // look in the repository to see if this value is a simple
            // number, or a calculation.  We aren't interested in
            // calculations - just simple numbers.
            String dataName =
                DataRepository.createDataName(fullName, ACT_TIME_DATA_NAME);
            Object val = data.getValue(dataName);
            if (val != null &&
                (!(val instanceof DoubleData) ||
                 val instanceof NumberFunction)) return;

            actualNodeTime = ((NumberData) time).getDouble();
            if (Double.isNaN(actualNodeTime) ||
                Double.isInfinite(actualNodeTime))
                actualNodeTime = 0.0;
        } else {
            actualNodeTime = 0;
        }
    }

    /** Messaged to indicate that the user has entered a new value for the
     * planned time of this task.
     * 
     * @param aValue the value entered by the user (a String is expected)
     */
    public void userSetPlanTime(Object aValue) {
        if ((aValue instanceof String && ((String) aValue).trim().endsWith("%")) ||
            (isLevelOfEffortTask() && (aValue == null || "".equals(aValue)))) {
            userSetLevelOfEffort((String) aValue);
        } else if (isPlannedTimeEditable() && aValue instanceof String) {
            long planTime = -1;

            // parse the value to obtain a number of minutes
            if (((String) aValue).length() > 0)
                planTime = FormatUtil.parseTime((String) aValue);

            // if the user is obviously correcting a top-down/bottom-up
            // mismatch, then just treat the input the same as if the
            // user had deleted the top-down estimate.
            if (hasTopDownBottomUpError() &&
                Math.abs(planTime - bottomUpPlanTime) < 0.9)
                planTime = -1;

            if (planTime != -1) {
                this.planTime = topDownPlanTime = bottomUpPlanTime = planTime;
                planTimeNull = planTimeUndefined = false;
                // save those minutes to the data repository
                data.userPutValue(DataRepository.createDataName
                                  (fullName, PLAN_TIME_DATA_NAME),
                                  new DoubleData(planTime, true));
                userSetLevelOfEffort(null);
            } else {
                this.planTime = topDownPlanTime = bottomUpPlanTime;
                data.userPutValue(DataRepository.createDataName
                                  (fullName, PLAN_TIME_DATA_NAME),
                                  null);
                planTimeNull = true;
                planTimeUndefined = false;
                userSetLevelOfEffort(null);
            }
        }
    }

    /** Messaged to indicate that the user has entered a new value for the
     * actual completion date of this task.
     * 
     * @param aValue the value entered by the user (a Date is expected)
     */
    public void userSetActualDate(Object aValue) {
        if (!isCompletionDateEditable())
            return;

        if (isLeaf()) {
            String dataName = DataRepository.createDataName
                (fullName, DATE_COMPLETED_DATA_NAME);

            // save the Date object to the data repository
            if (aValue instanceof Date) {
                dateCompleted = (Date) aValue;
                data.userPutValue(dataName, new DateData(dateCompleted, true));
            } else {
                dateCompleted = null;
                data.userPutValue(dataName, null);
            }
        } else if (getNumChildren() == 1) {
            getChild(0).userSetActualDate(aValue);
            recalcDateCompleted();
            notifyListener(true);
        }
    }

    /** Messaged to indicate that the user has entered a new value for the
     * node type of this task.
     * 
     * @param aValue the value entered by the user (a String is expected)
     */
    public void userSetNodeType(Object aValue) {
        if (!isNodeTypeEditable() || aValue == null)
            return;
        String newType = aValue.toString();

        ListData allowedTypes = getAcceptableNodeTypes();
        if (allowedTypes == null || !allowedTypes.contains(newType))
            return;

        String dataName = DataRepository.createDataName(fullName,
                NODE_TYPE_DATA_NAME);
        this.nodeType = newType;
        data.userPutValue(dataName, StringData.create(newType));
    }

    protected static NumberFormat percentFormatter =
        NumberFormat.getPercentInstance();
    protected static NumberFormat intPercentFormatter =
        NumberFormat.getPercentInstance();
    static {
        percentFormatter.setMaximumFractionDigits(1);
        intPercentFormatter.setMaximumFractionDigits(0);
    }
    static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent))
            percent = 0;
        if (percent > 0.99 || percent < -0.99)
            return intPercentFormatter.format(percent);
        else
            return percentFormatter.format(percent);
    }
    static String formatIntPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent) || percent == 0)
            return "";
        else
            return intPercentFormatter.format(percent);
    }


    /** Returns a list of the subtasks underneath this task.
     * 
     * @return the children of this task. If this task has no children, returns
     *     an empty list.
     */
    public List<EVTask> getChildren() {
        return new ArrayList<EVTask>(children);
    }

    /** Returns the number of subtasks underneath this task. */
    public int getNumChildren() {
        return children.size();
    }

    /** Finds a particular subtask and returns its position.
     * 
     * @param child the subtask to find (an EVTask object is expected)
     * @return the index of the given subtask. If the given object is not an
     *     EVTask, or if it is not an immediate child of this task, returns -1.
     *     If this method return a number >= 0, an immediate call to
     *     {@link #getChild(int)} with that number would return the subtask
     *     correponding to the parameter value.
     */
    public int getChildIndex(Object child) {
        if (child instanceof EVTask)
            return indexOfNode(children, (EVTask) child);
        else
            return -1;
    }

    /** Returns true if this task has no subtasks. */
    public boolean isLeaf() { return children.isEmpty(); }

    /** Returns a single subtask of this node.
     * 
     * @param pos the index of the subtask, must be less than
     *          {@link #getNumChildren()}
     * @return the child at that position
     */
    public EVTask getChild(int pos) {
        return children.get(pos);
    }

    /** Returns the parent of this task, if one exists.
     * 
     * @return the parent of this task.  The root node of a task list has no
     *     parent.
     */
    public EVTask getParent() { return parent; }

    /** Returns the name of this task. */
    public String getName() { return name; }
    public String toString() { return name; }
    protected void setName(String name) { this.name = name; }

    /** Returns the task IDs associated with this task. */
    public List<String> getTaskIDs() { return taskIDs; }
    protected void setTaskIDs(List<String> taskIDs) { this.taskIDs = taskIDs; }

    public List<String> getInheritedTaskIDs() {
        if (taskIDs != null)
            return taskIDs;
        else if (parent != null)
            return parent.getInheritedTaskIDs();
        else
            return Collections.EMPTY_LIST;
    }

    public String getFullTaskID() {
        if (taskIDs != null && !taskIDs.isEmpty())
            return taskIDs.get(0);
        else if (parent != null) {
            String parentId = parent.getFullTaskID();
            return (parentId == null ? null : parentId + "/" + name);
        } else
            return null;
    }

    /** Returns the flag associated with this task node.
     * 
     * A task list is generally composed of one or more structural root nodes
     * (representing the task list itself) with children underneath
     * (representing the actual work to be performed).  Structural nodes are
     * given a flag to indicate their structural purpose, while work nodes will
     * have no flag.
     * 
     * @return the flag associated with this task node, or null if this node
     *     represents actual work to be performed.
     */
    public String getFlag() { return flag; }

    /** Returns the type of this node.
     * 
     * Some dashboard processes allow a "type" to be associated with work
     * tasks.  (For example, the type might capture a process phase for
     * high-maturity metrics collection purposes.)
     * 
     * @return the type of this node, or null if it has no type.
     */
    public String getNodeType() {
        if (isNodeTypeImplicit())
            return nodeType.substring(1, nodeType.length()-1);
        else
            return nodeType;
    }

    /** Set the type of this node.
     *
     * @param type the new type for this task node.
     */
    public void setNodeType(SimpleData type) {
        if (type == null) {
            this.nodeType = null;
            this.nodeTypeEditable = false;
        } else {
            this.nodeType = type.format();
            this.nodeTypeEditable = type.isEditable();
        }
    }

    /** Returns true if the user should be allowed to edit the type of this
     * task node.
     */
    public boolean isNodeTypeEditable() {
        return nodeType != null && nodeTypeEditable;
    }

    /** Returns true if this node is <i>supposed</i> to have a type, but none
     * is present.
     */
    public boolean isNodeTypeMissing() {
        return MISSING_NODE_TYPE.equals(nodeType);
    }

    /** Returns true if this node has a type that is not a legal value
     * (according to the rules established by the controlling process
     * definition).
     */
    public boolean isNodeTypeInvalid() {
        if (nodeType == null || nodeType.length() == 0 || isNodeTypeImplicit())
            return false;

        ListData l = getAcceptableNodeTypes();
        return (l != null && !l.contains(nodeType));
    }

    /** Returns true if this node has an implicit node type.
     * 
     * The controlling process definition may implicitly assign types to
     * certain task nodes.  This method will detect those implicitly assigned
     * nodes.
     * 
     * @return true if this node has an implicitly assigned type.
     */
    public boolean isNodeTypeImplicit() {
        return (nodeType != null
                && nodeType.startsWith("(")
                && nodeType.endsWith(")"));
    }

    /** Return any applicable error message associated with the type of this
     * node.
     * 
     * @return a user-friendly string describing the error associated with
     *      the type of this node, or null if no error is present.
     */
    public String getNodeTypeError() {
        if (isNodeTypeMissing())
            return resources.getString("Task.Node_Type_Missing.Error");
        if (isNodeTypeInvalid())
            return resources.getString("Task.Node_Type_Invalid.Error");
        return null;
    }

    public String getNodeTypeQualifier() {
        return getNodeTypeSpecValue("processName", null);
    }

    /** Returns the list of node types that would be acceptable for this
     * node, according to the controlling process definition.
     */
    public ListData getAcceptableNodeTypes() {
        if (nodeTypeSpec != null)
            return nodeTypeSpec;
        else if (parent == null)
            return null;
        else
            return parent.getAcceptableNodeTypes();
    }
    protected String getNodeTypeSpecValue(String key, String defaulVal) {
        ListData spec = getAcceptableNodeTypes();
        if (spec == null)
            return defaulVal;

        String prefix = "(" + key + ":";
        for (int i = 0;  i < spec.size();  i++) {
            String specItem = (String) spec.get(i);
            if (specItem.startsWith(prefix) && specItem.endsWith(")"))
                return specItem.substring(prefix.length(), specItem.length()-1);
        }

        return defaulVal;
    }

    /** Returns true if this task or any of its children has a non-null
     * node type.
     */
    public boolean isUsingNodeTypes() {
        if (XMLUtils.hasValue(nodeType) && !isNodeTypeImplicit())
            return true;
        for (int i = getNumChildren();  i-- > 0; ) {
            if (getChild(i).isUsingNodeTypes())
                return true;
        }
        return false;
    }

    /** Returns the list of individuals assigned to this task, formatted for
     * display.
     */
    public String getAssignedToText() {
        return StringUtils.join(getAssignedTo(), ", ");
    }

    /** Returns a list of the names of individuals assigned to this task. */
    public List<String> getAssignedTo() {
        if (assignedTo != null)
            return assignedTo;
        else if (parent != null)
            return parent.getAssignedTo();
        else
            return null;
    }

    protected void setAssignedTo(List<String> assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Map<String, HierarchyNote> getNoteData() {
        return noteData;
    }

    /** Returns the list of forward and reverse dependencies for this task. */
    public List<EVTaskDependency> getDependencies() {
        return getDependencies(false);
    }

    /** Returns the dependencies for this task and all its ancestors */
    public Set<EVTaskDependency> getAllDependencies() {
        EVTask node = this;
        Set<EVTaskDependency> result = null;
        while (node != null) {
            List deps = node.getDependencies(false);
            if (deps != null) {
                if (result == null)
                    result = new HashSet();
                for (Iterator i = deps.iterator(); i.hasNext();) {
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    result.add(d.getWithNewParent(this));
                }
            }
            node = node.getParent();
        }
        return result;
    }

    /** Returns the task IDs for all forward dependences of this task. */
    public List<String> getDependentTaskIDs() {
        if (dependencies == null || dependencies.isEmpty())
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList<String>();
        for (EVTaskDependency d : dependencies) {
            if (!d.isReverse()) {
                result.add(d.getTaskID());
            }
        }
        return result;
    }

    protected List<EVTaskDependency> getDependencies(boolean create) {
        if (dependencies == null && create)
            dependencies = new ArrayList();
        return dependencies;
    }
    public void setDependencies(Collection dependencies) {
        this.dependencies = new ArrayList(dependencies);
    }

    /** Returns the full name of this task.
     * 
     * Each task has a name; the full name of a task contains the names of
     * all parents, plus this task, concatentated with the "/" character.
     */
    public String getFullName() { return fullName; }

    /** Returns a formatted string, appropriate for display in the planned time
     * column of a task list.
     */
    public String getPlanTimeText() {
        if (planLevelOfEffort == ANCESTOR_LEVEL_OF_EFFORT) return "";
        else if (rollupLevelOfEffort > 0)
            return formatPercent(rollupLevelOfEffort);
        else if (planLevelOfEffort > 0)
            return formatPercent(planLevelOfEffort);
        else return FormatUtil.formatTime(planTime);
    }

    /** Returns the planned time (in minutes) for this task and all subtasks */
    public double getPlanTime() {
        return planTime;
    }

    public String getPlanDirectTimeText() {
        if (isValuePruned() && planValue == 0) return "";
        else return FormatUtil.formatTime(planValue);
    }

    /** Returns the portion of the plan time that "counts" toward this schedule
     * (minutes) */
    public double getPlanDirectTime() {
        return planValue;
    }

    public boolean hasPlanTimeError() {
        return (hasTopDownBottomUpError() || planTimeIsMissing());
    }
    private boolean hasTopDownBottomUpError() {
        return (!isValuePruned() &&
            (bottomUpPlanTime > 0) &&
            (Math.abs(planTime - bottomUpPlanTime) > 0.5));
    }
    private boolean planTimeIsMissing() {
        return (!isValuePruned() &&
                planTimeEditable && (planTimeNull || planTimeUndefined));
    }
    public String getPlanTimeError() {
        if (hasTopDownBottomUpError())
            return resources.format("Task.Mismatch.Error_FMT",
                                    FormatUtil.formatTime(bottomUpPlanTime));
        if (planTimeIsMissing())
            return resources.getString("Task.Plan_Time_Missing.Error");
        return null;
    }

    public String getActualTimeText(double totalActualTime) {
        if (isLevelOfEffortTask())
            return formatPercent(actualTime / totalActualTime);
        else return FormatUtil.formatTime(actualTime);
    }

    /** Returns the total time (minutes) actually spent in this node and its
     * children, both before and during this schedule
     */
    public double getActualTime() {
        return actualTime;
    }

    public String getActualDirectTimeText() {
        if (//isLevelOfEffortTask() || isTotallyPruned() ||
            (isValuePruned() && actualDirectTime == 0)) return "";
        else return FormatUtil.formatTime(actualDirectTime);
    }

    /** Returns the total time (minutes) actually spent during this schedule
     * in this node and its children on tasks that count toward earned value
     */
    public double getActualDirectTime() {
        return actualDirectTime;
    }

    public String getPlanValueText(double totalPlanValue) {
        if (isValuePruned() && planValue == 0) return "";
        return formatPercent(planValue/totalPlanValue);
    }

    /** Returns the portion of the plan time (minutes) that "counts" toward
     * this schedule */
     public double getPlanValue() {
        return planValue;
     }

     /** Returns the plan value as a percentage of the total value */
     public double getPlanValuePercent(double totalPlanValue) {
         return planValue / totalPlanValue;
     }

     public String getBaselineTimeText() {
        if (!(baselineTime > 0) && baselineDate == null)
            return "";
        else
            return FormatUtil.formatTime(baselineTime);
    }

    /** Returns the amount of time planned for this task in the baseline
     * schedule. */
    public double getBaselineTime() {
        return baselineTime;
    }

    public String getCumPlanTimeText() {
        if (isValuePruned() && cumPlanValue == 0) return "";
        return FormatUtil.formatTime(cumPlanValue);
    }

    public String getCumPlanValueText(double totalPlanValue) {
        if (isValuePruned() && cumPlanValue == 0) return "";
        return formatPercent(cumPlanValue/totalPlanValue);
    }

    /** Returns the approximate date this task is planned to start */
    public Date getPlanStartDate() {
        return planStartDate;
    }

    /** Returns the approximate date this task is planned to start, based
     * on current schedule slip. */
    public Date getReplanStartDate() {
        return replanStartDate;
    }

    /** Returns the approximate date this task might start, based
     * on current forecast projections. */
    public Date getForecastStartDate() {
        return forecastStartDate;
    }

    /** Returns the approximate date this task was planned to start in the
     * baseline saved for this schedule */
    public Date getBaselineStartDate() {
        return baselineStartDate;
    }

    /** Returns the date work was actually started on this task. */
    public Date getActualStartDate() {
        return actualStartDate;
    }

    protected void setActualStartDate(Date actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    /** Returns the date this task is planned to be completed. */
    public Date getPlanDate() {
        if (isValuePruned()) return null;
        return planDate;
    }

    protected void setPlanDate(Date planDate) {
        this.planDate = planDate;
    }

    /** Returns the date we planned to complete this task in the baseline
     * schedule */
    public Date getBaselineDate() {
        return baselineDate;
    }

    /** Returns the date the task could complete, based on current
     * schedule slip */
    public Date getReplanDate() {
        if (isValuePruned()) return null;
        if (dateCompleted != null) return dateCompleted;
        return replanDate;
    }

    /** Returns the date the task is forecast to complete. */
    public Date getForecastDate() {
        if (isValuePruned()) return null;
        if (dateCompleted != null) return dateCompleted;
        return forecastDate;
    }
    public Date getActualDate() {
        if (isLevelOfEffortTask() || isTotallyPruned() ||
            dateCompleted == COMPLETION_DATE_NA) return null;
        return dateCompleted;
    }

    /** Returns the date this task was actually completed, or null if the
     * task has not yet been completed.
     */
    public Date getDateCompleted() {
        return dateCompleted;
    }

    protected void setDateCompleted(Date d) {
        dateCompleted = d;
    }

    public String getDateCompleteError() {
        if (isLeafWithFutureCompletionDate())
            return resources.getString("Task.Future_Completion_Date.Error");
        else
            return null;
    }
    private boolean isLeafWithFutureCompletionDate() {
        if (dateCompleted == null) return false;
        if (isLeaf() == false) return false;
        long delta = dateCompleted.getTime() - System.currentTimeMillis();
        // check if the completion date is in the future.  Allow a full day
        // of slack time to allow for time zone differences.
        return delta > EVCalculator.DAY_MILLIS;
    }

    public String getPercentCompleteText() {
        double pct = getPercentComplete();
        if (pct > 0.994 && pct < 0.99999)
            pct = 0.99;  // round "almost complete" down to 99%
        return formatIntPercent(pct);
    }

    /** Returns the percent complete for this task and subtasks, as a number
     * between 0.0 and 1.0. */
    public double getPercentComplete() {
        if (isLevelOfEffortTask() || isTotallyPruned() ||
                dateCompleted == COMPLETION_DATE_NA)
            return 0;
        else if (planValue == 0)
            return (dateCompleted == null ? 0 : 1);
        else
            return valueEarned / planValue;
    }

    public String getPercentSpentText() {
        if (actualDirectTime == 0 || planValue == 0 || isValuePruned()) return "";
        return formatIntPercent(getPercentSpent());
    }

    /** Returns the percent spent for this task and subtasks.
     * 
     * Percent Spent is defined as actual direct time divided by planned direct
     * time.  This calculation only includes time spent during the current
     * EV schedule.
     */
    public double getPercentSpent() {
        return actualDirectTime / planValue;
    }

    /** Returns the actual value earned (minutes) in this node and its
     * children. */
    public double getValueEarned() {
        return valueEarned;
    }
    public String getValueEarnedText(double totalPlanTime) {
        if (isValuePruned() && valueEarned == 0) return "";
        else if (dateCompleted != null || valueEarned != 0.0)
            return formatPercent(valueEarned/totalPlanTime);
        else
            return "";
    }

    private String taskError = null;
    public boolean hasTaskError() { return taskError != null; }
    public String getTaskError() { return taskError; }
    public void setTaskError(String err) { taskError = err; }

    public void adjustDates(DateAdjuster adj) {
        planStartDate = adj.adjust(planStartDate);
        replanStartDate = adj.adjust(replanStartDate);
        forecastStartDate = adj.adjust(forecastStartDate);
        baselineStartDate = adj.adjust(baselineStartDate);
        actualStartDate = adj.adjust(actualStartDate);
        planDate = adj.adjust(planDate);
        baselineDate = adj.adjust(baselineDate);
        replanDate = adj.adjust(replanDate);
        forecastDate = adj.adjust(forecastDate);
        dateCompleted = adj.adjust(dateCompleted);

        for (int i = children.size();   i-- > 0;  )
            getChild(i).adjustDates(adj);
    }


    /** Gets the path from the root to the receiver. */
    public EVTask[] getPath() { return getPathToRoot(this, 0); }

    protected EVTask[] getPathToRoot(EVTask aNode, int depth) {
        EVTask[] retNodes;

        if(aNode == null) {
            if(depth == 0)
                return null;
            else
                retNodes = new EVTask[depth];
        }
        else {
            depth++;
            retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }


    /** Get a list of the leaf tasks under this task.
     *
     * elements in the list will be EVTask objects.
     */
    public List getLeafTasks() {
        ArrayList result = new ArrayList();
        getLeafTasks(result);
        return result;
    }
    protected void getLeafTasks(List list) {
        if (isEVLeaf()) {
            if (!isLevelOfEffortTask() && !isUserPruned())
                list.add(this);
        } else
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).getLeafTasks(list);
    }
    public boolean isEVLeaf() {
        return (isLeaf() || (planTime > 0 && bottomUpPlanTime == 0));
    }


    /** Finds one or more tasks with the given full name
     * 
     * @return a list of tasks (either equal to or descending from this task)
     *    that have the given full name.
     */
    public List findByFullName(String fullName) {
        if (!StringUtils.hasValue(fullName))
            return null;

        ArrayList result = new ArrayList(1);
        findByFullName(result, fullName);
        return result;
    }
    protected void findByFullName(List result, String fullName) {
        if (fullName.equals(this.fullName))
            result.add(this);
        else {
            for (int i = getNumChildren(); i-- > 0;)
                getChild(i).findByFullName(result, fullName);
        }
    }


    /** Finds a task in this task tree with one of the given task IDs
     * 
     * @param ids a collection of task IDs
     * @return an EVTask (either this task or one of its children) which
     *    has a task ID matching one in the given collection
     */
    public EVTask findByTaskIDs(Collection ids) {
        if (ids == null || ids.isEmpty())
            return null;

        if (this.taskIDs != null) {
            for (Iterator i = this.taskIDs.iterator(); i.hasNext();) {
                String oneID = (String) i.next();
                if (ids.contains(oneID))
                    return this;
            }
        }

        for (int i = getNumChildren();  i-- > 0;) {
            EVTask result = getChild(i).findByTaskIDs(ids);
            if (result != null)
                return result;
        }

        return null;
    }


    /** Return a collection of all the descendants of this task */
    public Set getDescendants() {
        HashSet result = new HashSet();
        addSelfAndDescendants(result);
        result.remove(this);
        return result;
    }
    protected void addSelfAndDescendants(Set dest) {
        dest.add(this);
        for (int i = getNumChildren(); i-- > 0;)
            getChild(i).addSelfAndDescendants(dest);
    }


    public double recalcPlanTimes() {
        if (isLeaf())
            planTime = bottomUpPlanTime = topDownPlanTime;
        else {
            bottomUpPlanTime = 0;
            for (int i = 0;   i < getNumChildren();   i++)
                bottomUpPlanTime += getChild(i).recalcPlanTimes();

            if (bottomUpPlanTime == 0)
                return (planTime = topDownPlanTime);
            else if (!planTimeNull && topDownPlanTime > 0)
                planTime = topDownPlanTime;
            else {
                planTime = bottomUpPlanTime;
                planTimeEditable = false;
            }
        }
        return bottomUpPlanTime;
    }

    public void recalcDateCompleted() {
        if (isLeaf()) return;

        for (int i = 0;   i < getNumChildren();   i++) {
            if (getChild(i).isTotallyPruned()) continue;
            getChild(i).recalcDateCompleted();
        }

        recalcParentDateCompleted();
    }

    void recalcParentDateCompleted() {
        Date d, result = COMPLETION_DATE_NA;
        boolean childEditable = true;
        for (int i = 0;   i < getNumChildren();   i++) {
            EVTask child = getChild(i);
            if (child.isTotallyPruned()) {
                childEditable = false;
                continue;
            }
            if (!child.dateCompletedEditable)
                childEditable = false;
            d = child.dateCompleted;
            if (d == null)
                result = null;
            else if (result != null && result.compareTo(d) < 0)
                result = d;
        }
        dateCompletedEditable = (getNumChildren() == 1 && childEditable);
        dateCompleted = result;
    }


    protected void maybeFallbackToOverspentDates() {
        boolean usedFallback = false;
        if (shouldFallback(planDate, overspentPlanDates)) {
            planStartDate = overspentPlanDates.getStart();
            planDate = overspentPlanDates.getEnd();
            usedFallback = true;
        }
        if (shouldFallback(replanDate, overspentReplanDates)) {
            replanStartDate = resolveStartDate(overspentReplanDates);
            replanDate = overspentReplanDates.getEnd();
            usedFallback = true;
        }
        if (shouldFallback(forecastDate, overspentForecastDates)) {
            forecastStartDate = resolveStartDate(overspentForecastDates);
            forecastDate = overspentForecastDates.getEnd();
            usedFallback = true;
        }
        if (usedFallback) {
            if (assignedTo != null && assignedTo.size() == 1) {
                String who = assignedTo.get(0);
                who = resources.format("Schedule.Team_Assignment_FMT", who);
                assignedTo = Collections.singletonList(who);
            } else if (assignedTo == null || assignedTo.isEmpty()) {
                String who = resources.getString("Schedule.Team_Assignment");
                assignedTo = Collections.singletonList(who);
            }
        }
    }
    private boolean shouldFallback(Date orig, DateRange fallback) {
        return (EVCalculator.badDate(orig) && fallback != null);
    }
    private Date resolveStartDate(DateRange r) {
        if (actualStartDate != null && actualStartDate.before(r.getEnd()))
            return actualStartDate;
        else
            return r.getStart();
    }



    public void checkForNodeErrors(EVMetrics metrics, int depth,
                                   List rootChildList,
                                   List otherNodeList,
                                   boolean rootNodesOnly) {

        if (rootNodesOnly && !"".equals(this.fullName))
            return;

        if (depth > 0) {
            setTaskError(null);

            if (containsNode(rootChildList, this) ||
                containsNode(otherNodeList, this)) {
                EVTask nodeWithErr = this;

                if (depth > 1) {
                    int pos = indexOfNode(rootChildList, this);
                    if (pos != -1)
                        nodeWithErr = (EVTask) rootChildList.get(pos);
                }

                String errorMessage = resources.format(
                        "Task.Duplicate_Task.Error_Msg_FMT",
                        rootNodesOnly ? nodeWithErr.name : nodeWithErr.fullName);
                metrics.addError(errorMessage, nodeWithErr);
                nodeWithErr.setTaskError(resources
                        .getString("Task.Duplicate_Task.Error"));
            }

            if (depth == 1)
                rootChildList.add(this);
            else
                otherNodeList.add(this);
        }

        if (!rootNodesOnly) {
            if (hasTopDownBottomUpError()) {
                String errorMessage = resources.format(
                        "Task.Mismatch.Error_Msg_FMT", fullName, getPlanTimeText(),
                        FormatUtil.formatTime(bottomUpPlanTime));
                metrics.addError(errorMessage, this);
            }

            if (planTimeIsMissing()) {
                String errorMessage = resources.format(
                        "Task.Plan_Time_Missing.Error_Msg_FMT",
                         fullName);
                metrics.addError(errorMessage, this);
            }

            if (isLeafWithFutureCompletionDate()) {
                String errorMessage = resources.format(
                    "Task.Future_Completion_Date.Error_Msg_FMT",
                     fullName, getDateCompleted());
                metrics.addError(errorMessage, this);
            }

            if (isNodeTypeMissing()) {
                String errorMessage = resources.format(
                        "Task.Node_Type_Missing.Error_Msg_FMT",
                         fullName);
                metrics.addError(errorMessage, this);
            } else if (isNodeTypeInvalid()) {
                String processName = getNodeTypeSpecValue("processName", "");
                String errorMessage = resources.format(
                        "Task.Node_Type_Invalid.Error_Msg_FMT",
                        fullName, nodeType, processName);
                metrics.addError(errorMessage, this);
            }
        }

        for (int i = 0;   i < getNumChildren();   i++)
            getChild(i).checkForNodeErrors(metrics, depth+1, rootChildList,
                    otherNodeList, rootNodesOnly);
    }



    //
    // DataListener interface
    //

    public void dataValueChanged(DataEvent e) {
        if (handleEvent(e)) notifyListener(true);
    }

    public void dataValuesChanged(Vector v) {
        boolean needsNotify = false;
        for (int i = v.size();  i-- > 0; )
            if (handleEvent((DataEvent) v.elementAt(i)))
                needsNotify = true;

        if (needsNotify) notifyListener(true);
    }

    protected boolean handleEvent(DataEvent e) {
        String dataName = e.getName();
        if (!dataName.startsWith(fullName+"/")) return false;
        dataName = dataName.substring(fullName.length()+1);
        if (PLAN_TIME_DATA_NAME.equals(dataName))
            setPlanTime(e.getValue());
        else if (ACT_TIME_DATA_NAME.equals(dataName))
            setActualTime(e.getValue());
        else if (NODE_TYPE_DATA_NAME.equals(dataName))
            setNodeType(e.getValue());
        else if (DATE_COMPLETED_DATA_NAME.equals(dataName))
            setActualDate(e.getValue());
        else if (dataName.startsWith(LEVEL_OF_EFFORT_PREFIX))
            setLevelOfEffort(e.getValue());
        else
            return false;
        return true;
    }

    protected void notifyListener(boolean needsRecalc) {
        Listener l = listener;
        if (l != null) l.evNodeChanged(this, needsRecalc);
    }

    public void destroy() {
        if (listener != null)
            listener = null;
        if (listeningToData != null) {
            for (Iterator i = listeningToData.iterator(); i.hasNext();) {
                String dataName = (String) i.next();
                data.removeDataListener(dataName, this);
                i.remove();
            }
        }
        for (int i=children.size();   i-- > 0; )
            getChild(i).destroy();
        children.clear();
    }

    public void saveToXML(StringBuffer result) {
        saveToXML(result, false);
    }

    public void saveToXML(StringBuffer result, boolean whitespace) {
        String indent = (whitespace ? "  " : "");
        saveToXML(result, whitespace, indent, false);
    }

    protected void saveToXML(StringBuffer result, boolean whitespace,
            String indent, boolean includeNotes) {
        result.append(indent)
            .append("<task name='").append(XMLUtils.escapeAttribute(name))
            .append("' pt='").append(planValue)
            .append("' at='").append(actualTime);
        if (planTime != planValue)
            result.append("' ptt='").append(planTime);
        if (actualTime != actualDirectTime)
            result.append("' adt='").append(actualDirectTime);
        if (actualCurrentTime != actualDirectTime)
            result.append("' act='").append(actualCurrentTime);
        if (planStartDate != null)
            result.append("' psd='").append(EVSchedule.saveDate(planStartDate));
        if (actualStartDate != null)
            result.append("' asd='").append(EVSchedule.saveDate(actualStartDate));
        if (planDate != null)
            result.append("' pd='").append(EVSchedule.saveDate(planDate));
        if (replanDate != null)
            result.append("' rpd='").append(EVSchedule.saveDate(replanDate));
        if (forecastDate != null)
            result.append("' fd='").append(EVSchedule.saveDate(forecastDate));
        if (dateCompleted != null)
            result.append("' cd='").append(EVSchedule.saveDate(dateCompleted));
        if (isLevelOfEffortTask())
            result.append("' loe='").append(planLevelOfEffort);
        if (taskOrdinal != INFER_FROM_CONTEXT)
            result.append("' ord='").append(taskOrdinal);
        if (pruningFlag != INFER_FROM_CONTEXT &&
            pruningFlag != ANCESTOR_PRUNED)
            result.append("' prune='").append(pruningFlag);
        if (hasValue(taskIDs))
            result.append("' tid='").append(XMLUtils.escapeAttribute(
                    StringUtils.join(taskIDs, ",")));
        if (hasValue(assignedTo))
            result.append("' who='").append(XMLUtils.escapeAttribute(
                    StringUtils.join(assignedTo, ",")));
        if (XMLUtils.hasValue(flag))
            result.append("' flag='").append(XMLUtils.escapeAttribute(flag));
        if (nodeType != null)
            result.append("' nt='").append(XMLUtils.escapeAttribute(nodeType));
        if (nodeTypeSpec != null)
            result.append("' nts='").append(XMLUtils.escapeAttribute(
                    nodeTypeSpec.formatClean()));

        String newline = (whitespace ? "\n" : "");

        if (isLeaf() && !hasValue(dependencies)
                && !(includeNotes && noteData != null))
            result.append("'/>").append(newline);
        else {
            result.append("'>").append(newline);
            String subIndent = (whitespace ? (indent + "  ") : "");
            if (includeNotes && noteData != null) {
                HierarchyNote note = noteData.get(HierarchyNoteManager.NOTE_KEY);
                if (note != null)
                    result.append(subIndent).append(note.getAsXML()).append(
                        newline);
            }
            if (hasValue(dependencies))
                for (EVTaskDependency dep : dependencies)
                    dep.getAsXML(result, subIndent, true);
            for (int i = 0;   i < getNumChildren();   i++)
                getChild(i).saveToXML(result, whitespace, subIndent, includeNotes);
            result.append(indent).append("</task>").append(newline);
        }
    }

    static int indexOfNode(List list, EVTask node) {
        if (node != null && list != null && list.size() > 0) {
            int i = 0;
            for (Iterator j = list.iterator(); j.hasNext();) {
                EVTask oneNode = (EVTask) j.next();
                if (node.sameNode(oneNode))
                    return i;
                i++;
            }
        }

        return -1;
    }
    static boolean containsNode(List list, EVTask node) {
        return indexOfNode(list, node) != -1;
    }

    protected boolean isValuePruned() {
        return isLevelOfEffortTask() || isTotallyPruned() || isChronologicallyPruned();
    }

    public boolean isChronologicallyPruned() {
        return (dateCompleted != null && dateCompleted != COMPLETION_DATE_NA &&
                planDate == null && planValue == 0);
    }

    public boolean isUserPruned() {
        return (pruningFlag == USER_PRUNED || pruningFlag == ANCESTOR_PRUNED);
    }
    protected boolean isTotallyPruned() {
        return (isUserPruned() && planValue == 0);
    }

    public void setUserPruned(boolean prune) {
        if (prune)
            pruningFlag = USER_PRUNED;
        else if (pruningFlag == USER_PRUNED || pruningFlag == ANCESTOR_PRUNED)
            pruningFlag = USER_UNPRUNED;
    }

    public EVTask getTaskForPath(String fullPath) {
        // check to see if our fullName is a perfect match for this path.
        if (fullName != null && fullName.equals(fullPath)) return this;

        // if we could not possibly be the parent of a node matching
        // fullPath, return null.
        if (fullName != null) {
            if (fullPath.length() <= fullName.length()) return null;
            if (!fullPath.startsWith(fullName)) return null;
            if (fullPath.charAt(fullName.length()) != '/') return null;
        }

        // see if any of our children would like to claim fullPath as theirs.
        for (int i = children.size();   i-- > 0;  ) { // dispatch loop
            EVTask result = getChild(i).getTaskForPath(fullPath);
            if (result != null) return result;
        }

        // None of our children claimed the path.
        if (fullName == null || fullName.length() == 0)
            // if this is the root node, don't claim it either.
            return null;
        else
            // otherwise, claim it as our own.
            return this;
    }


    public static boolean taskIsPruned(DataRepository data,
                                       String taskListName,
                                       String taskPath)
    {
        SaveableData d = data.getInheritableValue
            (taskPath, TASK_PRUNING_PREFIX + taskListName);
        int pruningFlag = INFER_FROM_CONTEXT;

        if (d != null && d.getSimpleValue() instanceof NumberData)
            pruningFlag = ((NumberData) d.getSimpleValue()).getInteger();

        return pruningFlag == USER_PRUNED;
    }

    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // can't happen?
            throw new RuntimeException(e);
        }
    }


}
