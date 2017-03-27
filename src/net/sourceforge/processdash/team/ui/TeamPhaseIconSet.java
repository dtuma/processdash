// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.Icon;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.ui.lib.PaintUtils;

public class TeamPhaseIconSet extends LinkedHashMap<String, Icon> {

    public TeamPhaseIconSet(List<String> phaseNames) {
        int numPhases = phaseNames.size();
        for (int i = 0; i < numPhases; i++) {
            String onePhase = phaseNames.get(i);
            Color c = getPhaseColor(i, numPhases);
            put(onePhase, HierarchyIcons.getTaskIcon(c));
        }
    }

    /**
     * Calculate the appropriate color for displaying a particular phase.
     * 
     * Copied from teamdash.wbs.TeamProcess
     */
    private Color getPhaseColor(int phaseNum, int numPhases) {
        float r = (phaseNum * (COLOR_SPECTRUM.length - 1)) / (float) numPhases;
        int offset = (int) Math.floor(r);
        r -= offset;

        return PaintUtils.mixColors(COLOR_SPECTRUM[offset + 1],
            COLOR_SPECTRUM[offset], r);
    }

    private static final Color[] COLOR_SPECTRUM = {
        Color.orange,
        Color.yellow,
        Color.green,
        Color.cyan,
        new Color(  0, 63, 255),   // blue
        new Color(170, 85, 255) }; // purple

}
