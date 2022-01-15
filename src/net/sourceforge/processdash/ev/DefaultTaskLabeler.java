// Copyright (C) 2007-2022 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.ev.MilestoneDataConstants.MILESTONES_DATA_NAME;
import static net.sourceforge.processdash.ev.MilestoneDataConstants.MILESTONE_ID_LABEL_PREFIX;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class DefaultTaskLabeler implements TaskLabeler, MilestoneProvider, DataListener {

    private PropertyKeyHierarchy hier;

    private DataRepository data;

    private Listener listener;

    private Set<String> listeningToData;

    private Map<String, Set> labelData;

    private Map<String, Milestone> milestoneData;

    private Map resultCache;

    private Map milestoneResultCache;

    private Set<String> hiddenLabels;


    public DefaultTaskLabeler(DashboardContext ctx) {
        this(ctx.getHierarchy(), ctx.getData(), null);
    }

    public DefaultTaskLabeler(PropertyKeyHierarchy hier, DataRepository data,
            Listener listener) {
        this.hier = hier;
        this.data = data;
        this.listener = listener;
        this.listeningToData = new HashSet();
    }

    public void dataValueChanged(DataEvent e) {
        listener.taskLabelsChanged();
    }

    public void dataValuesChanged(Vector v) {
        listener.taskLabelsChanged();
    }

    private void maybeListenForDataChanges(String dataName) {
        if (listener != null && listeningToData.add(dataName))
            data.addDataListener(dataName, this, false);
    }

    public void dispose() {
        for (String dataName : listeningToData)
            data.removeDataListener(dataName, this);
    }

    public void recalculate() {
        labelData = new HashMap();
        milestoneData = new LinkedHashMap();
        resultCache = new HashMap();
        milestoneResultCache = new HashMap();
        hiddenLabels = new HashSet<String>();
        loadData(PropertyKey.ROOT);
        hiddenLabels = Collections.unmodifiableSet(hiddenLabels);
    }

    private void loadData(PropertyKey node) {
        if (node != null) {
            loadLabelData(node.path());
            loadMilestoneData(node.path());
            int numChildren = hier.getNumChildren(node);
            for (int i = 0; i < numChildren; i++)
                loadData(hier.getChildKey(node, i));
        }
    }

    private void loadLabelData(String path) {
        String dataName = DataRepository.createDataName(path, LABELS_DATA_NAME);
        ListData list = ListData.asListData(data.getSimpleValue(dataName));
        if (list == null || list.size() == 0)
            return;

        maybeListenForDataChanges(dataName);

        String currentLabel = null;
        String currentMilestone = null;
        for (int i = 0; i < list.size(); i++) {
            String item = StringUtils.asString(list.get(i));
            if (item == null || item.length() == 0) {
                continue;

            } else if (NO_LABEL.equals(item) || LABEL_PREFIX.equals(item)) {
                currentLabel = currentMilestone = null;

            } else if (item.startsWith(LABEL_PREFIX)) {
                currentLabel = item.substring(LABEL_PREFIX.length());
                currentMilestone = null;
                if (currentLabel.startsWith("_"))
                    hiddenLabels.add(currentLabel);

            } else if (item.startsWith(MILESTONE_ID_LABEL_PREFIX)) {
                currentMilestone = item;
                hiddenLabels.add(currentMilestone);
                if (currentLabel.startsWith("Milestone:"))
                    hiddenLabels.add(currentLabel.substring(10));

            } else if (LABEL_HIDDEN_MARKER.equals(item)) {
                hiddenLabels.add(currentLabel);

            } else {
                String taskID = item;
                Set labelsForTask = (Set) labelData.get(taskID);
                if (labelsForTask == null) {
                    labelsForTask = new HashSet();
                    labelData.put(taskID, labelsForTask);
                }
                if (currentLabel != null)
                    labelsForTask.add(currentLabel);
                if (currentMilestone != null)
                    labelsForTask.add(currentMilestone);
            }
        }
    }

    private void loadMilestoneData(String path) {
        String dataName = DataRepository.createDataName(path,
            MILESTONES_DATA_NAME);
        SimpleData val = data.getSimpleValue(dataName);
        if (val == null || !val.test())
            return;

        Element xml;
        try {
            xml = XMLUtils.parse(val.format()).getDocumentElement();
        } catch (Exception e) {
            return;
        }

        maybeListenForDataChanges(dataName);

        XmlMilestone previous = null;
        for (Element m : XMLUtils.getChildElements(xml)) {
            XmlMilestone xm = new XmlMilestone(m, milestoneData.size());
            String id = xm.getMilestoneID();
            if (XMLUtils.hasValue(id) && !milestoneData.containsKey(id)) {
                milestoneData.put(id, xm);
                xm.setPrevious(previous);
                previous = xm;
            }
        }
    }

    public Set<String> getHiddenLabels() {
        return hiddenLabels;
    }


    public List<String> getLabelsForTask(EVTask t) {
        if (t == null)
            return Collections.EMPTY_LIST;

        List result = (List) resultCache.get(t);
        if (result == null) {
            List taskIDs = t.getTaskIDs();
            result = getLabelsForTaskIDs(taskIDs);

            // Some tasks should inherit labels from their parent. For example:
            // * the phases of PSP tasks
            // * phase stubs in old-style team projects
            // * tasks that do not appear in the WBS
            // These tasks will either (a) not have a list of task IDS, or
            // (b) have a list of task IDs that are not recognized from any
            // known WBS. The getLabelsForTaskIDs method will return null
            // for these scenarios, as an indicator that we should inherit
            // from our parent.
            if (result == null) {
                if (t.getParent() != null)
                    result = getLabelsForTask(t.getParent());
                else
                    result = Collections.EMPTY_LIST;
            }

            resultCache.put(t, result);
        }
        return result;
    }


    private List getLabelsForTaskIDs(Collection taskIDs) {
        if (taskIDs == null || taskIDs.isEmpty())
            return null;

        boolean foundMatch = false;
        Set result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        for (Iterator i = taskIDs.iterator(); i.hasNext();) {
            String id = (String) i.next();
            Set labels = (Set) labelData.get(id);
            if (labels != null) {
                foundMatch = true;
                result.addAll(labels);
            }
        }

        if (!foundMatch)
            return null;
        else if (result.isEmpty())
            return Collections.EMPTY_LIST;
        else
            return Collections.unmodifiableList(new ArrayList(result));
    }

    public List<Milestone> getMilestonesForProject(String projectID) {
        List<Milestone> result = new ArrayList();
        for (Entry<String, Milestone> e : milestoneData.entrySet()) {
            String milestoneID = e.getKey();
            if (milestoneID.startsWith(projectID))
                result.add(e.getValue());
        }
        return result;
    }

    public List<Milestone> getMilestonesForTask(EVTask task) {
        List<Milestone> result = (List<Milestone>) milestoneResultCache
                .get(task);

        if (result == null) {
            for (String label : getLabelsForTask(task)) {
                if (label.startsWith(MILESTONE_ID_LABEL_PREFIX)) {
                    String milestoneID = label
                            .substring(MILESTONE_ID_LABEL_PREFIX.length());
                    Milestone m = milestoneData.get(milestoneID);
                    if (m != null) {
                        if (result == null)
                            result = new ArrayList();
                        result.add(m);
                    }
                }
            }
            if (result == null)
                result = Collections.EMPTY_LIST;
            milestoneResultCache.put(task, result);
        }

        return result;
    }

    private static class XmlMilestone implements Milestone,
            MilestoneDataConstants {

        private Element xml;

        private int ordinal;

        private XmlMilestone previousMilestone, nextMilestone;

        protected XmlMilestone(Element xml, int ordinal) {
            this.xml = xml;
            this.ordinal = ordinal;
        }

        private void setPrevious(XmlMilestone previous) {
            this.previousMilestone = previous;
            if (previous != null)
                previous.nextMilestone = this;
        }

        public String getName() {
            return xml.getAttribute(MILESTONE_NAME_ATTR);
        }

        public String getMilestoneID() {
            return xml.getAttribute(MILESTONE_FULL_ID_ATTR);
        }

        public Date getCommitDate() {
            String d = xml.getAttribute(MILESTONE_DATE_ATTR);
            if (XMLUtils.hasValue(d)) {
                try {
                    return CALENDAR_FMT.parse(d);
                } catch (Exception e) {
                }
            }
            return null;
        }

        public Color getColor() {
            String c = xml.getAttribute(MILESTONE_COLOR_ATTR);
            if (XMLUtils.hasValue(c)) {
                try {
                    return Color.decode(c);
                } catch (Exception e) {
                }
            }
            return null;
        }

        public boolean isHidden() {
            return "true".equals(xml.getAttribute(MILESTONE_HIDDEN_ATTR));
        }

        public boolean isDeferred() {
            return "true".equals(xml.getAttribute(MILESTONE_DEFERRED_ATTR));
        }

        public int getSortOrdinal() {
            return ordinal;
        }

        public Milestone getPreviousMilestone() {
            return previousMilestone;
        }

        public Milestone getNextMilestone() {
            return nextMilestone;
        }

        @Override
        public int hashCode() {
            return getMilestoneID().hashCode();
        }

        public int compareTo(Milestone m) {
            XmlMilestone that = (XmlMilestone) m;
            return this.ordinal - that.ordinal;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof XmlMilestone) {
                XmlMilestone that = (XmlMilestone) obj;
                return this.getMilestoneID().equals(that.getMilestoneID());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return getName();
        }

    }

    private static final DateFormat CALENDAR_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");

}
