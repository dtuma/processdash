// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.border.EmptyBorder;

public class OptionCheckbox extends JCheckBox implements ActionListener {

    private String checkedArg;

    private String uncheckedArg;

    private DashboardProcessFactory factory;

    public OptionCheckbox(String resKey, String checkedArg,
            String uncheckedArg, DashboardProcessFactory factory,
            boolean defaultVal) {
        super(QuickLauncher.resources.getString("Options." + resKey + ".Label"));
        this.checkedArg = checkedArg;
        this.uncheckedArg = uncheckedArg;
        this.factory = factory;
        if (defaultVal)
            setSelected(true);
        addActionListener(this);
        setBorder(new EmptyBorder(0, 20, 0, 0));
        setFocusPainted(false);
        actionPerformed(null);
    }

    public void actionPerformed(ActionEvent e) {
        if (isSelected()) {
            factory.removeVmArg(uncheckedArg);
            factory.addVmArg(checkedArg);
        } else {
            factory.removeVmArg(checkedArg);
            factory.addVmArg(uncheckedArg);
        }
    }



}
