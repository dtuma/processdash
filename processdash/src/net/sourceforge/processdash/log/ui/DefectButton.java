// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.log.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JButton;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.DefectLogID;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;

public class DefectButton extends JButton implements ActionListener,
        PropertyChangeListener, DashHierarchy.Listener {
    ProcessDashboard parent = null;
    Icon enabled_icon = null;
    Icon disabled_icon = null;
    String defectLogFileName = null;
    PropertyKey defectPath = null;

    public DefectButton(ProcessDashboard dash) {
        super();
        PCSH.enableHelp(this, "EnteringDefects");
        try {
            enabled_icon = DashboardIconFactory.getDefectIcon();
            disabled_icon = DashboardIconFactory.getDisabledDefectIcon();
            setIcon(enabled_icon);
            setDisabledIcon(disabled_icon);
        } catch (Exception e) {
            setText("Defect");
        }
        setMargin(new Insets(1, 2, 1, 2));
        parent = dash;
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

    public void setPaths(DefectLogID defectLog) {
        if (defectLog == null) {
            defectLogFileName = null;
            defectPath = null;
        } else {
            defectLogFileName = defectLog.filename;
            defectPath = defectLog.path;
        }
        setEnabled(defectLogFileName != null);
    }

    public void actionPerformed(ActionEvent e) {
        if (defectLogFileName != null) {
            // pop up a defect log dialog
            DefectDialog d = new DefectDialog(parent, defectLogFileName,
                    defectPath);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateAll();
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        updateAll();
    }
}
