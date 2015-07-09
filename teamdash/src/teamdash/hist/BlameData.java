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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map.Entry;

import teamdash.merge.ModelType;

public class BlameData extends HashMap<ModelType, BlameModelData> {

    private BlameCaretPos caretPos;

    private PropertyChangeSupport changeSupport;

    public BlameData() {
        changeSupport = new PropertyChangeSupport(this);
    }

    public boolean isEmpty() {
        for (BlameModelData modelData : values()) {
            if (!modelData.isEmpty())
                return false;
        }
        return true;
    }

    public BlameModelData getOrCreate(ModelType type) {
        BlameModelData result = get(type);
        if (result == null) {
            result = new BlameModelData();
            put(type, result);
        }
        return result;
    }

    public BlameCaretPos getCaretPos() {
        return caretPos;
    }

    public void setCaretPos(BlameCaretPos caretPos) {
        BlameCaretPos oldCaretPos = this.caretPos;
        this.caretPos = caretPos;
        changeSupport.firePropertyChange("caretPos", oldCaretPos, caretPos);
    }

    public int countAnnotations(BlameCaretPos caretPos) {
        BlameModelData modelData = get(caretPos.getModelType());
        if (modelData == null)
            return 0;
        else
            return modelData.countAnnotations(caretPos);
    }

    public boolean clearAnnotations(BlameCaretPos caretPos) {
        BlameModelData modelData = get(caretPos.getModelType());
        if (modelData == null)
            return false;
        else
            return modelData.clearAnnotations(caretPos);
    }


    /*
     * Property change listener support
     */

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String property,
            PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(property, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String property,
            PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(property, l);
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Entry<ModelType, BlameModelData> e : entrySet()) {
            if (e.getValue().isEmpty())
                continue;
            result.append(SEPARATOR).append(e.getKey()).append('\n');
            result.append(SEPARATOR).append(e.getValue()).append('\n');
        }
        result.append(SEPARATOR);
        return result.toString();
    }

    public static BlameModelData getModel(BlameData blameData, ModelType type) {
        return (blameData == null ? null : blameData.get(type));
    }

    private static final String SEPARATOR = //
    "----------------------------------------------------------------------\n";

}
