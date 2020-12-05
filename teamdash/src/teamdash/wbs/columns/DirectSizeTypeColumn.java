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

package teamdash.wbs.columns;

import java.util.Map;

import teamdash.wbs.SizeMetric;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

/**
 * This column outputs the metricID value for PSP and PROBE tasks.
 */
public class DirectSizeTypeColumn extends AbstractDataColumn {

    public static final String COLUMN_ID = "Direct Size Units";

    private Map<String, SizeMetric> sizeMetrics;

    public DirectSizeTypeColumn(Map<String, SizeMetric> sizeMetrics) {
        this.sizeMetrics = sizeMetrics;
        this.columnID = this.columnName = COLUMN_ID;
    }

    public boolean isCellEditable(WBSNode node) { return false; }
    public void setValueAt(Object aValue, WBSNode node) {}

    public Object getValueAt(WBSNode node) {
        if (TeamProcess.isProbeTask(node.getType())) {
            return WorkflowSizeUnitsColumn.getSizeMetricForProbeTask(node,
                sizeMetrics);

        } else if (TeamProcess.isPSPTask(node.getType())) {
            return "LOC";

        } else {
            return null;
        }
    }

}
