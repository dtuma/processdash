// Copyright (C) 2012 Tuma Solutions, LLC
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

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.WBSNode;

public class MilestoneVisibilityColumn extends MilestoneBooleanColumn {

    public static final String COLUMN_ID = "Milestone Hidden";

    private static final String ATTR_NAME = "Hidden";

    public MilestoneVisibilityColumn() {
        super(ATTR_NAME);
        columnID = COLUMN_ID;
        columnName = resources.getString("Milestones.Hidden.Name");
    }

    public static boolean isHidden(WBSNode node) {
        return (node.getAttribute(ATTR_NAME) != null);
    }

    public TableCellRenderer getCellRenderer() {
        return CELL_RENDERER;
    }

    public static final TableCellRenderer CELL_RENDERER = new CellRenderer(
            resources.getString("Milestones.Hidden.Tooltip"));

}
