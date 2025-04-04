// Copyright (C) 2018-2025 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;
import teamdash.wbs.ExternalSystemManager.ExtSystem;

public class ExternalNodeOwnerColumn extends AbstractDataColumn
        implements ExternalSystemPrimaryColumn, CalculatedDataColumn {

    private final Map<String, String> valueAttrs;

    public ExternalNodeOwnerColumn(Collection<ExtSystem> systems) {
        this.columnID = "External Owner";
        this.columnName = resources.getString("External_Owner.Name");
        this.preferredWidth = 200;

        Map<String, String> valueAttrs = new HashMap<String, String>();
        for (ExtSystem sys : systems) {
            String systemID = sys.getID();
            valueAttrs.put(systemID, ExtSyncUtil.getExtOwnerAttr(systemID));
        }
        this.valueAttrs = Collections.unmodifiableMap(valueAttrs);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        Object systemID = node.getAttribute(EXT_SYSTEM_ID_ATTR);
        if (systemID == null)
            return null;

        String valueAttr = valueAttrs.get(systemID);
        Object value = node.getAttribute(valueAttr);
        return (value == null ? null : new ReadOnlyValue(value));
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {}

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

}
