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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import teamdash.merge.ModelType;
import teamdash.wbs.WBSNode;

public class BlameModelData extends HashMap<Integer, BlameNodeData> {

    private List<BlameModelDataListener> listeners;

    public BlameModelData() {}

    public BlameNodeData getNodeData(Integer nodeID) {
        BlameNodeData result = get(nodeID);
        if (result == null) {
            result = new BlameNodeData();
            put(nodeID, result);
        }
        return result;
    }

    public void purgeUnchangedNodes() {
        for (Iterator<BlameNodeData> i = values().iterator(); i.hasNext();) {
            if (i.next().isEmpty())
                i.remove();
        }
        fireBlameModelDataEvent();
    }

    public int countAnnotations(BlameCaretPos caretPos) {
        int result = 0;
        for (Integer nodeID : caretPos.getNodes()) {
            BlameNodeData nodeData = get(nodeID);
            if (nodeData != null)
                result += nodeData.countColumnsAffected(caretPos.getColumns());
        }
        return result;
    }

    public boolean clearAnnotations(BlameCaretPos caretPos) {
        boolean madeChange = false;
        for (Integer nodeID : caretPos.getNodes()) {
            BlameNodeData nodeData = get(nodeID);
            if (nodeData != null && nodeData.clearAnnotations(caretPos)) {
                madeChange = true;
                if (nodeData.isEmpty())
                    remove(nodeID);
            }
        }
        if (madeChange)
            fireBlameModelDataEvent();
        return madeChange;
    }

    public BlameCaretPos findNextAnnotation(List<WBSNode> wbsNodes,
            List<String> columns, BlameCaretPos currentCaret,
            boolean searchForward) {
        if (isEmpty())
            return null;

        int nodePos = findNodePos(wbsNodes, currentCaret, searchForward);
        int columnPos = findColumnPos(columns, currentCaret, searchForward);
        int increment = searchForward ? +1 : -1;

        while (true) {
            // search forward/backward for the next column.
            columnPos += increment;

            // if we run off the end of the column list, wrap and then move
            // to another node.
            if (outOfRange(columns, columnPos)) {
                columnPos = (columnPos + columns.size()) % columns.size();

                // search forward/backward for the next node with annotations.
                // if we run off the end of the node list, return null.
                while (true) {
                    nodePos += increment;
                    if (outOfRange(wbsNodes, nodePos))
                        return null;
                    if (containsKey(wbsNodes.get(nodePos).getTreeNodeID()))
                        break;
                }
            }

            String columnID = columns.get(columnPos);
            int nodeID = wbsNodes.get(nodePos).getTreeNodeID();
            BlameNodeData nodeData = get(nodeID);
            if (nodeData != null && nodeData.isColumnAffected(columnID)) {
                ModelType type = wbsNodes.get(0).getWbsModel().getModelType();
                return new BlameCaretPos(type,
                        Collections.singletonList(nodeID),
                        Collections.singletonList(columnID));
            }
        }
    }

    private int findNodePos(List<WBSNode> wbsNodes, BlameCaretPos currentCaret,
            boolean searchForward) {
        if (currentCaret != null) {
            int caretNodeID = currentCaret.getSingleNode();
            for (int i = wbsNodes.size(); i-- > 0;) {
                if (wbsNodes.get(i).getTreeNodeID() == caretNodeID)
                    return i;
            }
        }
        return searchForward ? 0 : wbsNodes.size() - 1;
    }

    private int findColumnPos(List<String> columns, BlameCaretPos currentCaret,
            boolean searchForward) {
        if (currentCaret != null) {
            int pos = columns.indexOf(currentCaret.getSingleColumn());
            if (pos != -1)
                return pos;
        }
        return searchForward ? -1 : columns.size();
    }

    private boolean outOfRange(List list, int pos) {
        return pos < 0 || pos >= list.size();
    }

    public void addBlameModelDataListener(BlameModelDataListener l) {
        if (listeners == null)
            listeners = new ArrayList<BlameModelDataListener>();
        listeners.add(l);
    }

    public void removeBlameModelDataListener(BlameModelDataListener l) {
        if (listeners != null)
            listeners.remove(l);
    }

    protected void fireBlameModelDataEvent() {
        if (listeners != null) {
            BlameModelDataEvent e = new BlameModelDataEvent(this);
            for (BlameModelDataListener l : listeners)
                l.blameDataChanged(e);
        }
    }

}
