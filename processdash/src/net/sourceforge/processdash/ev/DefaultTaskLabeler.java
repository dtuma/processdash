// Copyright (C) 2007-2013 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.util.StringUtils;

public class DefaultTaskLabeler implements TaskLabeler {

    private Map<String, Set> labelData;

    private Map resultCache;

    private Set<String> hiddenLabels;


    public DefaultTaskLabeler(DashboardContext ctx) {
        this(ctx.getHierarchy(), ctx.getData());
    }

    public DefaultTaskLabeler(PropertyKeyHierarchy hier, DataContext data) {
        labelData = new HashMap();
        resultCache = new HashMap();
        loadLabelData(hier, data, PropertyKey.ROOT);
        buildHiddenLabelList();
    }

    private void loadLabelData(PropertyKeyHierarchy hier, DataContext data,
            PropertyKey node) {
        if (node != null) {
            loadLabelData(data, node.path());
            for (int i = hier.getNumChildren(node); i-- > 0;)
                loadLabelData(hier, data, hier.getChildKey(node, i));
        }
    }

    private void loadLabelData(DataContext data, String path) {
        String dataName = DataRepository.createDataName(path, LABELS_DATA_NAME);
        ListData list = ListData.asListData(data.getSimpleValue(dataName));
        if (list == null || list.size() == 0)
            return;

        String currentLabel = null;
        for (int i = 0; i < list.size(); i++) {
            String item = StringUtils.asString(list.get(i));
            if (item == null || item.length() == 0)
                continue;

            else if (NO_LABEL.equals(item) || LABEL_PREFIX.equals(item))
                currentLabel = null;

            else if (item.startsWith(LABEL_PREFIX))
                currentLabel = item.substring(LABEL_PREFIX.length());

            else {
                String taskID = item;
                Set labelsForTask = (Set) labelData.get(taskID);
                if (labelsForTask == null) {
                    labelsForTask = new HashSet();
                    labelData.put(taskID, labelsForTask);
                }
                if (currentLabel != null)
                    labelsForTask.add(currentLabel);
            }
        }
    }

    private void buildHiddenLabelList() {
        hiddenLabels = new HashSet<String>();
        List h = getLabelsForTaskIDs(Collections.singleton(LABEL_HIDDEN_MARKER));
        if (h != null)
            hiddenLabels.addAll(h);
        for (String oneLabel : labelData.keySet())
            if (oneLabel != null && oneLabel.startsWith("_"))
                hiddenLabels.add(oneLabel);
        hiddenLabels = Collections.unmodifiableSet(hiddenLabels);
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
            //   * the phases of PSP tasks
            //   * phase stubs in old-style team projects
            //   * tasks that do not appear in the WBS
            // These tasks will either (a) not have a list of task IDS, or
            // (b) have a list of task IDs that are not recognized from any
            // known WBS.  The getLabelsForTaskIDs method will return null
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

}
