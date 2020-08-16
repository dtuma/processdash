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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.event.TableModelEvent;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ModelType;


public class SizeMetricsWBSModel extends WBSModel {

    public static final String METRIC_LIST_TYPE = "Size Metric List";

    public static final String SIZE_METRIC_TYPE = "Size Metric";

    public static final String HIST_ID_ATTR_PREFIX = "Prev ID ";

    public static final int LOC_ID = 1;


    private TeamProcess processToUpdate = null;

    private Map<String, SizeMetric> idToMetricMap = null;

    private Map<String, String> nameToIdMap = null;


    protected SizeMetricsWBSModel() {
        super(METRIC_LIST_TYPE, false);
    }

    public SizeMetricsWBSModel(Element e) {
        super(e);
    }

    public SizeMetricsWBSModel(TeamProcess process) {
        this();
        for (SizeMetric sm : process.getSizeMetricMap().values()) {
            String metricName = sm.getName();
            WBSNode metricNode = new WBSNode(this, metricName, SIZE_METRIC_TYPE,
                    1, false);
            if (!"LOC".equals(metricName)) {
                metricNode.setAttribute(WORK_PRODUCTS_ATTR, getKeysMappingTo(
                    process.getWorkProductSizeMap(), metricName));
                metricNode.setAttribute(PHASE_LIST_ATTR, getKeysMappingTo(
                    process.getPhaseSizeMap(), metricName));
            }
            add(metricNode);
        }
    }

    void registerProcessToUpdate(TeamProcess process) {
        this.processToUpdate = process;
        maybeUpdateProcess();
    }

    private void maybeUpdateProcess() {
        if (processToUpdate != null)
            processToUpdate.setSizeMetricMaps(getIdToMetricMap(),
                getProcessSizeMetricsMap(WORK_PRODUCTS_ATTR),
                getProcessSizeMetricsMap(PHASE_LIST_ATTR));
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

    public Map<String, SizeMetric> getIdToMetricMap() {
        if (idToMetricMap == null)
            rebuildMaps();
        return idToMetricMap;
    }

    public String getIdForMetric(String name) {
        if (!StringUtils.hasValue(name))
            return null;
        if (nameToIdMap == null)
            rebuildMaps();
        return nameToIdMap.get(name);
    }

    @Override
    public void fireTableChanged(TableModelEvent e) {
        idToMetricMap = null;
        nameToIdMap = null;
        maybeUpdateProcess();
        super.fireTableChanged(e);
    }

    private void rebuildMaps() {
        Map<String, SizeMetric> newIdToMetricMap = new LinkedHashMap<String, SizeMetric>();
        Map<String, String> newNameToIdMap = new TreeMap<String, String>(
                String.CASE_INSENSITIVE_ORDER);
        for (WBSNode node : getChildren(getRoot())) {
            String metricID = getMetricID(node);
            String name = node.getName();
            if (StringUtils.hasValue(name) && !newNameToIdMap.containsKey(name)) {
                newIdToMetricMap.put(metricID, new SizeMetric(metricID, name));
                newNameToIdMap.put(name, metricID);
            }
        }
        this.idToMetricMap = Collections.unmodifiableMap(newIdToMetricMap);
        this.nameToIdMap = Collections.unmodifiableMap(newNameToIdMap);
    }


    private static final String WORK_PRODUCTS_ATTR = "Work Product List";

    private static final String PHASE_LIST_ATTR = "Phase List";

    private static String getKeysMappingTo(Map<String, String> map,
            String metricName) {
        StringBuilder result = new StringBuilder();
        for (Entry<String, String> e : map.entrySet()) {
            if (metricName.equals(e.getValue()))
                result.append("/").append(e.getKey());
        }
        return (result.length() == 0 ? null : result.substring(1));
    }

    private Map<String, String> getProcessSizeMetricsMap(String attr) {
        Map<String, String> result = new HashMap<String, String>();
        for (WBSNode node : getChildren(getRoot())) {
            String itemList = (String) node.getAttribute(attr);
            if (StringUtils.hasValue(itemList)) {
                for (String item : itemList.split("/"))
                    result.put(item, getMetricID(node));
            }
        }
        return result;
    }


    public static String getMetricID(WBSNode node) {
        if ("LOC".equalsIgnoreCase(node.getName()))
            return "LOC";
        else
            return "Size-" + node.getUniqueID();
    }

}
