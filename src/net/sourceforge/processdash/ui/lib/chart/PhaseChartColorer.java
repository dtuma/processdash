// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.Color;
import java.util.List;

import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.LocalizedString;
import net.sourceforge.processdash.util.StringUtils;

/**
 * When displaying charts of phase-related data, it is useful to apply a
 * consistent color scheme to the various phases (so Code is always a particular
 * color, for example). This object assists with that task.
 */
public abstract class PhaseChartColorer {

    private ProcessUtil procUtil;

    private List itemKeys;

    public PhaseChartColorer(ProcessUtil procUtil) {
        this(procUtil, null);
    }

    public PhaseChartColorer(ProcessUtil procUtil, List itemKeys) {
        this.procUtil = procUtil;
        this.itemKeys = itemKeys;
    }

    public List getItemKeys() {
        return itemKeys;
    }

    public abstract void setItemColor(Object key, int pos, Color c);

    public void run() {
        List keys = getItemKeys();
        for (int i = 0; i < keys.size(); i++) {
            Object itemKey = keys.get(i);
            if (itemKey == null)
                continue;

            String phaseName = getPhaseNameFromKey(itemKey);
            if (!StringUtils.hasValue(phaseName))
                continue;

            Color phaseColor = getColorForPhase(phaseName);
            if (phaseColor != null)
                setItemColor(itemKey, i, phaseColor);
        }
    }

    protected String getPhaseNameFromKey(Object key) {
        if (key instanceof LocalizedString) {
            return ((LocalizedString) key).getUnlocalizedString();
        } else {
            return String.valueOf(key);
        }
    }

    protected Color getColorForPhase(String phaseName) {
        String phaseColorString = getColorStringForPhase(phaseName);
        try {
            if (StringUtils.hasValue(phaseColorString))
                return Color.decode(phaseColorString);
        } catch (Exception e) {
        }
        return null;
    }

    protected String getColorStringForPhase(String phaseName) {
        return procUtil.getProcessString(phaseName + "/Phase_Color");
    }

}
