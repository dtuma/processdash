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

package teamdash.wbs;

import java.awt.Component;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ZoomAction;

import teamdash.ActionCategoryComparator;
import teamdash.wbs.icons.WBSImageIcon;

public class WBSZoomAction extends ZoomAction {

    public static final String WBS_ACTION_CATEGORY_ZOOM = "zoom";

    public WBSZoomAction(Component parent) {
        super(WBSZoom.get(), parent,
                Resources.getDashBundle("WBSEditor.Edit.Zoom"),
                new WBSImageIcon("zoom.png"));
        putValue(ActionCategoryComparator.ACTION_CATEGORY,
            WBS_ACTION_CATEGORY_ZOOM);

    }

}
