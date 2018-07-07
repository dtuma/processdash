// Copyright (C) 2007-2018 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class JOptionPaneTweaker extends Component {

    private int delay;

    public JOptionPaneTweaker() {
        this(0);
    }

    public JOptionPaneTweaker(int delay) {
        this.delay = delay;
    }

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
            final JDialog dialog = (JDialog) window;
            if (delay <= 0) {
                doTweak(dialog);
            } else {
                Timer t = new Timer(delay, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        doTweak(dialog);
                    }});
                t.setRepeats(false);
                t.start();
            }
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
            super(100);
            this.c = c;
        }

        public void doTweak(JDialog dialog) {
            c.requestFocus();
        }
    }

    public static class DisableKeys extends JOptionPaneTweaker {

        public DisableKeys() {
            super(100);
        }

        public void addNotify() {
            super.addNotify();

            JOptionPane pane = (JOptionPane) SwingUtilities
                    .getAncestorOfClass(JOptionPane.class, this);
            if (pane != null) {
                SwingUtilities.replaceUIInputMap(pane,
                    JComponent.WHEN_IN_FOCUSED_WINDOW, null);
                SwingUtilities.replaceUIActionMap(pane, null);
            }
        }

        public void doTweak(JDialog dialog) {
            dialog.getRootPane().setDefaultButton(null);
        }

    }

    public static class ToFront extends JOptionPaneTweaker {

        public ToFront() {
            super(100);
        }

        @Override
        public void doTweak(JDialog dialog) {
            dialog.toFront();
        }

    }

}
