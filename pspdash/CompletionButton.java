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

import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.*;
import pspdash.data.DateData;

class CompletionButton extends JCheckBox implements ActionListener {
    PSPDashboard parent = null;
    String dataName = null;

    CompletionButton(PSPDashboard dash) {
        super();
        parent = dash;
        setMargin (new Insets (0,2,0,2));
        addActionListener(this);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = g.BOTH;
        dash.getContentPane().add(this);
    }

    public void setPath(String p) {
        dataName = p + "/Completed";
        DateData d = (DateData) parent.data.getValue(dataName);
        if (d == null) {
            setSelected(false);
            setToolTipText("Click to mark this phase completed");
        } else {
            setSelected(true);
            setToolTipText(d.formatDate());
        }
    }


    public void actionPerformed(ActionEvent e) {
        if (isSelected())
            parent.hierarchy.selectNext();
        else {
            parent.data.putValue(dataName, null);
            setToolTipText("Click to mark this phase completed");
        }
    }

}
