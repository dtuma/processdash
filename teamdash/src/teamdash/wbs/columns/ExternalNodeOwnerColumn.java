// Copyright (C) 2018 Tuma Solutions, LLC
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

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class ExternalNodeOwnerColumn extends AbstractDataColumn
        implements ExternalSystemPrimaryColumn, CalculatedDataColumn {

    private String valueAttr;

    public ExternalNodeOwnerColumn(String systemID, String systemName) {
        this.columnID = systemID + " Owner";
        this.columnName = resources.format("External_Owner.Name_FMT",
            systemName);
        this.preferredWidth = 200;
        this.valueAttr = ExtSyncUtil.getExtOwnerAttr(systemID);
    }

    @Override
    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    @Override
    public Object getValueAt(WBSNode node) {
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
