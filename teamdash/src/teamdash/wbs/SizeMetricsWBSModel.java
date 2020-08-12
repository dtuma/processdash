// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ModelType;


public class SizeMetricsWBSModel extends WBSModel {

    public static final String METRIC_LIST_TYPE = "Size Metric List";

    public static final String SIZE_METRIC_TYPE = "Size Metric";

    public static final String HIST_ID_ATTR_PREFIX = "Prev ID ";

    public static final int LOC_ID = 1;


    private Map<Integer, String> nameMap = null;

    private Map<String, Integer> idMap = null;


    public SizeMetricsWBSModel(Element e) {
        super(e);
    }

    public SizeMetricsWBSModel(TeamProcess process) {
        super(METRIC_LIST_TYPE, false);
        for (String metric : process.getSizeMetrics()) {
            add(new WBSNode(this, metric, SIZE_METRIC_TYPE, 1, false));
        }
    }

    public ModelType getModelType() {
        return ModelType.SizeMetrics;
    }

    public boolean isNodeTypeEditable(WBSNode node) {
        return false;
    }

    @Override
    protected void tweakNodeForInsertion(WBSNode node) {
        if (size() == 0) {
            // this is the root node
            node.setType(METRIC_LIST_TYPE);
        } else {
            // all other nodes
            node.setIndentLevel(1);
            node.setType(SIZE_METRIC_TYPE);
            node.setUniqueID(getDefaultIdForMetric(node));
        }
    }

    private int getDefaultIdForMetric(WBSNode node) {
        // assign a standard/constant unique ID for LOC
        String nameLC = node.getName().toLowerCase();
        if ("loc".equals(nameLC))
            return LOC_ID;

        // if this metric existed in the past but was deleted, try to
        // reuse the same ID it had before.
        String prevIdAttr = HIST_ID_ATTR_PREFIX + nameLC;
        Integer prevId = getRoot().getIntegerAttribute(prevIdAttr);
        if (prevId != null) {
            getRoot().removeAttribute(prevIdAttr);
            return prevId;
        }

        // use a deterministic formula to choose a default ID for the metric
        int result = nameLC.hashCode() & 0xffffff;
        return (result == LOC_ID ? 100 : result);
    }

    public void renameMetric(WBSNode node, String newName) {
        String oldName = node.getName();
        int nodePos = getIndexOfNode(node);
        if (newName == null || newName.equals(oldName) || nodePos == -1)
            return;

        if ("LOC".equalsIgnoreCase(oldName) || "LOC".equalsIgnoreCase(newName)) {
            // if we are changing to or from "LOC", the assigned unique ID
            // should change. Use delete/reinsert operations to accomplish that
            deleteNodes(Collections.singletonList(node), false);
            node.setName(newName);
            add(nodePos, node);
        } else {
            // if neither name is "LOC", perform the rename normally
            node.setName(newName);
        }

        // fire a table event to signal the name change
        int row = getRowForNode(node);
        fireNodeAppearanceChanged(row, row);
    }

    @Override
    public boolean deleteNodes(List<WBSNode> nodesToDelete, boolean notify) {
        // store away the ID that was used for each metric before we delete it
        for (WBSNode node : nodesToDelete) {
            String nameLC = node.getName().toLowerCase();
            String prevIdAttr = HIST_ID_ATTR_PREFIX + nameLC;
            getRoot().setAttribute(prevIdAttr, node.getUniqueID());
        }

        // call the superclass method to perform the actual deletion
        return super.deleteNodes(nodesToDelete, notify);
    }

    public List<String> getMetricNames() {
        return new ArrayList(getMetricNameMap().values());
    }

    public String getNameForMetric(Integer id) {
        return getMetricNameMap().get(id);
    }

    public Map<Integer, String> getMetricNameMap() {
        if (nameMap == null)
            rebuildMaps();
        return nameMap;
    }

    public Integer getIdForMetric(String name) {
        if (!StringUtils.hasValue(name))
            return null;
        if (idMap == null)
            rebuildMaps();
        return idMap.get(name.toLowerCase());
    }

    @Override
    public void fireTableChanged(TableModelEvent e) {
        nameMap = null;
        idMap = null;
        super.fireTableChanged(e);
    }

    private void rebuildMaps() {
        Map<Integer, String> newNameMap = new LinkedHashMap<Integer, String>();
        Map<String, Integer> newIdMap = new HashMap<String, Integer>();
        for (WBSNode node : getChildren(getRoot())) {
            int id = node.getUniqueID();
            String name = node.getName();
            String nameLC = name.toLowerCase();
            if (StringUtils.hasValue(name) && !newIdMap.containsKey(nameLC)) {
                newNameMap.put(id, name);
                newIdMap.put(nameLC, id);
            }
        }
        this.nameMap = Collections.unmodifiableMap(newNameMap);
        this.idMap = Collections.unmodifiableMap(newIdMap);
    }

}
