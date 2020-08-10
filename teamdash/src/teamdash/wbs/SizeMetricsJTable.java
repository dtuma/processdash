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

import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenu;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;

public class SizeMetricsJTable extends WBSJTable {

    public SizeMetricsJTable(SizeMetricsDataModel model) {
        super(model, makeIconMap(), new JMenu());

        customizeColumns();
        tweakBehaviors();
    }

    private static Map makeIconMap() {
        Map result = new HashMap();
        result.put(SizeMetricsWBSModel.METRIC_LIST_TYPE,
            WBSZoom.icon(IconFactory.getSizeMetricListIcon()));
        result.put(SizeMetricsWBSModel.SIZE_METRIC_TYPE,
            WBSZoom.icon(IconFactory.getSizeMetricIcon()));
        result.put(null, WBSZoom.icon(IconFactory.getModifiedIcon(
            HierarchyIcons.getComponentIcon(), IconFactory.ERROR_ICON)));
        return result;
    }

    private void customizeColumns() {
        // customize the behavior and appearance of the columns.
        DataTableModel.installColumnCustomizations(this);
    }

    private void tweakBehaviors() {
        // do not allow indentation; size metrics are a flat list
        setIndentationDisabled(true);
    }

}
