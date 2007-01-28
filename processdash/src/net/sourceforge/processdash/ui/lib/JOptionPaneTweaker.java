// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class JOptionPaneTweaker extends Component {


    public Dimension getMaximumSize() {
        return new Dimension(0,0);
    }

    public Dimension getMinimumSize() {
        return getMaximumSize();
    }

    public Dimension getPreferredSize() {
        return getMaximumSize();
    }

    public void addNotify() {
        super.addNotify();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog) {
            JDialog dialog = (JDialog) window;
            doTweak(dialog);
        }
    }

    public void doTweak(JDialog dialog) {
    }

    public static class MakeResizable extends JOptionPaneTweaker {

        public void doTweak(JDialog dialog) {
            dialog.setResizable(true);
        }

    }

    public static class GrabFocus extends JOptionPaneTweaker {
        private JComponent c;

        public GrabFocus(JComponent c) {
            this.c = c;
        }

        public void doTweak(JDialog dialog) {
            c.requestFocus();
        }
    }

}
