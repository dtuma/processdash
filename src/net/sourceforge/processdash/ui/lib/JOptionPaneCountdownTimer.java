// Copyright (C) 2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class JOptionPaneCountdownTimer extends JProgressBar
        implements ActionListener {

    public JOptionPaneCountdownTimer(int seconds) {
        super(0, seconds);
        setValue(seconds);
    }

    public void addNotify() {
        super.addNotify();
        new Timer(1000, this).start();
    }

    public void actionPerformed(ActionEvent e) {
        // decrement the time remaining
        int value = getValue() - 1;
        setValue(value);

        // when the time remaining reaches zero, dispose the window containing
        // this component. (This will dismiss the JOptionPane as if the user
        // had closed the window manually.)
        if (value < 1)
            SwingUtilities.getWindowAncestor(this).dispose();
    }

}
