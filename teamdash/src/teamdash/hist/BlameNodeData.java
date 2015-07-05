// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import static teamdash.wbs.AbstractWBSModelMerger.NODE_NAME;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import teamdash.wbs.WBSNode;

public class BlameNodeData {

    public static final String PARENT_PATH = "Parent_Path";

    private BlamePoint addedBy;

    private Map<WBSNode, BlamePoint> deletedChildren;

    private Map<String, BlameValueList> attributes;

    public boolean hasStructuralChange() {
        if (addedBy != null || deletedChildren != null)
            return true;
        if (attributes == null)
            return false;
        return attributes.containsKey(PARENT_PATH)
                || attributes.containsKey(NODE_NAME);
    }

    public BlamePoint getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(BlamePoint added) {
        this.addedBy = added;
    }

    public Map<WBSNode, BlamePoint> getDeletedChildren() {
        return deletedChildren;
    }

    public void addDeletedChild(WBSNode node, BlamePoint actor) {
        if (deletedChildren == null)
            deletedChildren = new LinkedHashMap<WBSNode, BlamePoint>();
        deletedChildren.put(node, actor);
    }

    public Map<String, BlameValueList> getAttributes() {
        return attributes;
    }

    public void addAttributeChange(String attributeName, BlamePoint newActor,
            String oldValue, String newValue) {
        if (attributes == null)
            attributes = new HashMap();
        BlameValueList values = attributes.get(attributeName);
        if (values == null) {
            values = new BlameValueList(oldValue);
            attributes.put(attributeName, values);
        }
        values.put(newActor, newValue);
    }

    public void remapInitials(Map<String, String> changedInitialAttrs) {
        Map<String, BlameValueList> remapped = null;
        for (Entry<String, String> change : changedInitialAttrs.entrySet()) {
            String oldAttrName = change.getKey();
            BlameValueList attrVal = attributes.remove(oldAttrName);
            if (attrVal != null) {
                if (remapped == null)
                    remapped = new HashMap<String, BlameValueList>();
                String newAttrName = change.getValue();
                remapped.put(newAttrName, attrVal);
            }
        }
        if (remapped != null)
            attributes.putAll(remapped);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (addedBy != null)
            result.append("added by ").append(addedBy).append('\n');
        if (deletedChildren != null)
            maybePad(result).append("deleted children: ")
                    .append(deletedChildren).append('\n');
        if (attributes != null) {
            for (Entry<String, BlameValueList> e : attributes.entrySet())
                maybePad(result).append("attr ").append(e.getKey())
                        .append(": ").append(e.getValue()).append('\n');
        }
        return result.toString();
    }

    private StringBuilder maybePad(StringBuilder result) {
        if (result.length() > 0)
            result.append("        ");
        return result;
    }

}
