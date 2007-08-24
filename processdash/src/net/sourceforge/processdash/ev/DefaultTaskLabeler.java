// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

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

    private static final String LABELS_DATA_NAME = "Task_Labels";

    private static final String LABEL_PREFIX = "label:";

    private static final String NO_LABEL = LABEL_PREFIX + "none";


    private Map labelData;

    private Map resultCache;


    public DefaultTaskLabeler(DashboardContext ctx) {
        this(ctx.getHierarchy(), ctx.getData());
    }

    public DefaultTaskLabeler(PropertyKeyHierarchy hier, DataContext data) {
        labelData = new HashMap();
        resultCache = new HashMap();
        loadLabelData(hier, data, PropertyKey.ROOT);
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

            else if (currentLabel != null) {
                String taskID = item;
                Set labelsForTask = (Set) labelData.get(taskID);
                if (labelsForTask == null) {
                    labelsForTask = new HashSet();
                    labelData.put(taskID, labelsForTask);
                }
                labelsForTask.add(currentLabel);
            }
        }
    }


    public List getLabelsForTask(EVTask t) {
        if (t == null)
            return Collections.EMPTY_LIST;

        List result = (List) resultCache.get(t);
        if (result == null) {
            List taskIDs = t.getTaskIDs();

            // for PSP tasks, and in old-style team projects, the phase stubs
            // would be the items appearing in the flat view. However, phase
            // stubs themselves do not have task IDs.  Detect whether the
            // current task looks like a phase stub, and possibly use the
            // parent's task IDs instead.
            if (taskIDs == null && t.isLeaf() && t.nodeTypeIsImplicit()
                    && t.getParent() != null)
                taskIDs = t.getParent().getTaskIDs();

            result = getLabelsForTaskIDs(taskIDs);
            resultCache.put(t, result);
        }
        return result;
    }


    public List getLabelsForTaskIDs(Collection taskIDs) {
        if (taskIDs == null || taskIDs.isEmpty())
            return Collections.EMPTY_LIST;

        Set result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        for (Iterator i = taskIDs.iterator(); i.hasNext();) {
            String id = (String) i.next();
            Set labels = (Set) labelData.get(id);
            if (labels != null)
                result.addAll(labels);
        }

        return new ArrayList(result);
    }

}
