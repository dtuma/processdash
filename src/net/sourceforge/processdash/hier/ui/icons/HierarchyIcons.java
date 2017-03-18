// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui.icons;

import java.awt.Color;

import javax.swing.Icon;

public class HierarchyIcons {

    public static final Color DEFAULT_COLOR = new Color(204, 204, 255);

    public static Icon getProjectIcon() {
        return new Rectangle3DIcon(16, 16, DEFAULT_COLOR, 1, 1, 1, 1);
    }

    public static Icon getComponentIcon() {
        return getComponentIcon(DEFAULT_COLOR);
    }

    public static Icon getComponentIcon(Color fill) {
        return new Rectangle3DIcon(16, 16, fill, 1, 1, 1, 1);
    }

    public static Icon getSoftwareComponentIcon() {
        return new FloppyDiskIcon(DEFAULT_COLOR);
    }

    public static Icon getDocumentIcon(Color highlight) {
        return new DocumentIcon(highlight);
    }

    public static Icon getTaskIcon() {
        return getTaskIcon(DEFAULT_COLOR);
    }

    public static Icon getTaskIcon(Color fill) {
        return new TaskIcon(fill);
    }

    public static Icon getWorkflowTaskIcon(Color fill) {
        return new Rectangle3DIcon(16, 16, fill, 3, 0, 3, 0);
    }

    public static Icon getPSPTaskIcon() {
        return getPSPTaskIcon(DEFAULT_COLOR);
    }

    public static Icon getPSPTaskIcon(Color fill) {
        return new PSPTaskIcon(fill);
    }

    public static Icon getProbeTaskIcon() {
        return new ProbeTaskIcon();
    }

}
