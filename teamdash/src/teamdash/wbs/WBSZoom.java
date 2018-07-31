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

import javax.swing.Icon;

import net.sourceforge.processdash.ui.lib.ZoomLevelCapable;
import net.sourceforge.processdash.ui.lib.ZoomManager;

/**
 * Singleton to manage the global zoom level for WBS Editor user interfaces
 */
public class WBSZoom {

    public static ZoomManager get() {
        return ZOOM;
    }

    public static Icon icon(Object icon) {
        ((ZoomLevelCapable) icon).setZoom(ZOOM);
        return (Icon) icon;
    }

    private static final ZoomManager ZOOM = new ZoomManager();

    static {
        ZOOM.setMinZoom(0.5);
        ZOOM.setMaxZoom(6.0);
        ZOOM.addType(new TableFontHandler());
    }

}
