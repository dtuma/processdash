// Copyright (C) 2000-2015 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.defects.DefectLogID;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

public class DefectButton extends JButton implements ActionListener,
        PropertyChangeListener, DashHierarchy.Listener {
    ProcessDashboard parent = null;
    Icon enabled_icon = null;
    Icon disabled_icon = null;
    List forbiddenPaths;
    String defectLogFileName = null;
    PropertyKey defectPath = null;

    public DefectButton(ProcessDashboard dash) {
        super();
        PCSH.enableHelp(this, "EnteringDefects");
        enabled_icon = DashboardIconFactory.getDefectIcon();
        disabled_icon = DashboardIconFactory.getDisabledDefectIcon();
        if (MacGUIUtils.isMacOSX()) {
            enabled_icon = new PaddedIcon(enabled_icon, 0, 2, 0, 2);
            disabled_icon = new PaddedIcon(disabled_icon, 0, 2, 0, 2);
        } else {
            setMargin(new Insets(0, 2, 0, 2));
        }
        setIcon(enabled_icon);
        setDisabledIcon(disabled_icon);

        parent = dash;
        forbiddenPaths = dash.getBrokenDataPaths();
        setEnabled(false);
        setFocusPainted(false);
        addActionListener(this);
        dash.getActiveTaskModel().addPropertyChangeListener(this);
        dash.getHierarchy().addHierarchyListener(this);

        updateAll();
    }

    private void updateAll() {
        PropertyKey currentPhase = parent.getActiveTaskModel().getNode();
        setPaths(parent.getHierarchy().defectLog(currentPhase , parent.getDirectory()));
    }

    private boolean shouldAllowDefectLogging() {
        return defectLogFileName != null
            && defectPath != null
            && !Filter.matchesFilter(forbiddenPaths, defectPath.path());
    }

    public void setPaths(DefectLogID defectLog) {
        if (defectLog == null) {
            defectLogFileName = null;
            defectPath = null;
        } else {
            defectLogFileName = defectLog.filename;
            defectPath = defectLog.path;
        }
        setEnabled(shouldAllowDefectLogging());
    }

    public void actionPerformed(ActionEvent e) {
        if (shouldAllowDefectLogging()) {
            // pop up a defect log dialog
            new DefectDialog(parent, defectLogFileName, defectPath);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateAll();
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        updateAll();
    }
}
