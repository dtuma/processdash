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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import teamdash.wbs.ConflictCapableDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSModelMergeConflictNotificationFactory;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.WBSNodeColumn;

public class BlameNodeData {

    private BlamePoint addedBy;

    private Map<WBSNode, BlamePoint> deletedChildren;

    private BlameValueList nodeName;

    private BlameValueList parentPath;

    private Map<String, BlameValueList> attributes;

    public boolean isEmpty() {
        return !hasStructuralChange() && attributes == null;
    }

    public boolean hasStructuralChange() {
        return addedBy != null || deletedChildren != null || nodeName != null
                || parentPath != null;
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

    public BlameValueList getNodeNameChanges() {
        return nodeName;
    }

    public BlameValueList getParentPathChanges() {
        return parentPath;
    }

    public void addParentPathChange(BlamePoint newActor, String oldParentName,
            String newParentName) {
        if (parentPath == null)
            parentPath = new BlameValueList(oldParentName);
        parentPath.put(newActor, newParentName);
    }

    public Map<String, BlameValueList> getAttributes() {
        return attributes;
    }

    public void addAttributeChange(String attributeName, BlamePoint newActor,
            String oldValue, String newValue) {
        BlameValueList values;

        if (attributeName.equals(NODE_NAME)) {
            if (nodeName == null)
                nodeName = new BlameValueList(oldValue);
            values = nodeName;

        } else {
            if (attributes == null)
                attributes = new HashMap();
            values = attributes.get(attributeName);
            if (values == null) {
                values = new BlameValueList(oldValue);
                attributes.put(attributeName, values);
            }
        }

        values.put(newActor, newValue);
    }

    public void remapInitials(Map<String, String> changedInitialAttrs) {
        if (attributes == null)
            return;

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

    public void calcAffectedColumns(DataTableModel model) {
        if (attributes == null)
            return;

        Iterator<Entry<String, BlameValueList>> i = attributes.entrySet()
                .iterator();
        while (i.hasNext()) {
            Entry<String, BlameValueList> e = i.next();
            String attributeName = e.getKey();
            BlameValueList blameValueList = e.getValue();
            ConflictCapableDataColumn column = WBSModelMergeConflictNotificationFactory
                    .findColumnForAttribute(model, attributeName);
            if (column == null)
                i.remove();
            else
                blameValueList.setColumn(column);
        }

        if (attributes.isEmpty())
            attributes = null;
    }

    public boolean isColumnAffected(Object column) {
        if (column instanceof WBSNodeColumn) {
            return hasStructuralChange();

        } else if (attributes != null) {
            for (BlameValueList v : attributes.values()) {
                if (v.getColumn() == column)
                    return true;
            }
        }
        return false;
    }

    public boolean isColumnAffected(String columnID) {
        if (WBSNodeColumn.COLUMN_ID.equals(columnID)) {
            return hasStructuralChange();

        } else if (attributes != null) {
            for (BlameValueList v : attributes.values()) {
                if (v.columnMatches(columnID))
                    return true;
            }
        }
        return false;
    }

    public int countColumnsAffected(List<String> columnIDs) {
        int result = 0;
        for (String oneColumnID : columnIDs) {
            if (isColumnAffected(oneColumnID))
                result++;
        }
        return result;
    }

    public boolean clearAnnotations(BlameCaretPos caretPos) {
        boolean madeChange = false;
        for (String column : caretPos.getColumns()) {
            if (clearAnnotations(column))
                madeChange = true;
        }
        return madeChange;
    }

    private boolean clearAnnotations(String columnID) {
        if (WBSNodeColumn.COLUMN_ID.equals(columnID)) {
            if (!hasStructuralChange())
                return false;

            addedBy = null;
            deletedChildren = null;
            nodeName = null;
            parentPath = null;
            return true;

        } else if (attributes != null) {
            Iterator<BlameValueList> i = attributes.values().iterator();
            while (i.hasNext()) {
                BlameValueList v = i.next();
                if (v.columnMatches(columnID)) {
                    i.remove();
                    if (attributes.isEmpty())
                        attributes = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (addedBy != null)
            result.append("added by ").append(addedBy).append('\n');
        if (nodeName != null)
            result.append("node name ").append(nodeName).append('\n');
        if (parentPath != null)
            result.append("parent path ").append(parentPath).append('\n');
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
