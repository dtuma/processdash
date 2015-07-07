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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
