// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.merge.ModelType;

import org.w3c.dom.Element;


public class MilestonesWBSModel extends WBSModel {

    public static final String MILESTONE_TYPE = "Milestone";

    private Map<Integer, String> nameMap = null;
    private Map<String, Integer> idMap = null;


    public MilestonesWBSModel(Element e) {
        super(e);
    }

    public MilestonesWBSModel(String rootNodeName) {
        super(rootNodeName, true);
        getNodeForRow(1).setName("");
    }

    public ModelType getModelType() {
        return ModelType.Milestones;
    }

    public boolean isNodeTypeEditable(WBSNode node) {
        return false;
    }

    @Override
    protected void tweakNodeForInsertion(WBSNode node) {
        if (size() == 0) {
            // this is the root node
            node.setType("Milestone List");
        } else {
            // all other nodes
            node.setIndentLevel(1);
            node.setType(MILESTONE_TYPE);
        }
    }

    public boolean isEmpty() {
        return getMilestoneNames().isEmpty();
    }

    public WBSNode[] getMilestones() {
        return getDescendants(getRoot());
    }

    public List<String> getMilestoneNames() {
        List<String> result = new ArrayList<String>();
        getMilestoneNames(result, getRoot());
        return result;
    }

    private void getMilestoneNames(List<String> result, WBSNode node) {
        for (WBSNode child : getChildren(node)) {
            String name = child.getName();
            if (StringUtils.hasValue(name) && !result.contains(name))
                result.add(name);
            // the current line isn't strictly necessary until such time that
            // we decide to allow hierarchical milestones.  But it doesn't
            // hurt for it to stay.
            getMilestoneNames(result, child);
        }
    }

    public String getNameForMilestone(Integer id) {
        return getMilestoneNameMap().get(id);
    }

    public Map<Integer, String> getMilestoneNameMap() {
        if (nameMap == null)
            rebuildMaps();
        return nameMap;
    }

    public Integer getIdForMilestone(String name) {
        return getMilestoneIdMap().get(name);
    }

    public Map<String, Integer> getMilestoneIdMap() {
        if (idMap == null)
            rebuildMaps();
        return idMap;
    }

    @Override
    public void fireTableChanged(TableModelEvent e) {
        nameMap = null;
        idMap = null;
        super.fireTableChanged(e);
    }

    private void rebuildMaps() {
        Map<Integer, String> newNameMap = new HashMap<Integer, String>();
        Map<String, Integer> newIdMap = new HashMap<String, Integer>();
        for (Map.Entry<Integer, WBSNode> e : getNodeMap().entrySet()) {
            Integer id = e.getKey();
            String name = e.getValue().getName();
            newNameMap.put(id, name);
            if (!newIdMap.containsKey(name))
                newIdMap.put(name, id);
        }
        this.nameMap = newNameMap;
        this.idMap = newIdMap;
    }

    public void sortMilestones(Comparator<WBSNode> c) {
        sortAllNonRootNodes(c);
    }

}
