// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;

public class DefectButton extends JButton implements ActionListener {
    PSPDashboard parent = null;
    ImageIcon enabled_icon = null;
    ImageIcon disabled_icon = null;
    String defectLogFileName = null;
    PropertyKey defectPath = null;

    DefectButton(PSPDashboard dash) {
    super();
    try {
        enabled_icon = new ImageIcon(getClass().getResource("defect.gif"));
        disabled_icon = new ImageIcon(getClass().getResource("defectd.gif"));
        setIcon(enabled_icon);
        setDisabledIcon(disabled_icon);
    } catch (Exception e) {
        setText("Defect");
    }
    setMargin (new Insets (1,2,1,2));
    parent = dash;
    setEnabled(false);
    addActionListener(this);
    dash.getContentPane().add(this);
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
            DefectDialog d=new DefectDialog(parent, defectLogFileName, defectPath);
        }
    }
}
