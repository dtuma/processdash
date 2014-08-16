// Copyright (C) 2006-2014 Tuma Solutions, LLC
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

public class ToolTipTimingCustomizer extends MouseAdapter {

    private int initialDelay;

    private int dismissDelay;

    public ToolTipTimingCustomizer() {
        this(50, 60000);
    }

    public ToolTipTimingCustomizer(int initialDelay, int dismissDelay) {
        this.initialDelay = initialDelay;
        this.dismissDelay = dismissDelay;
    }

    public void install(JComponent component) {
        component.addMouseListener(this);
    }

    public void uninstall(JComponent component) {
        component.removeMouseListener(this);
    }


    public void mouseEntered(MouseEvent e) {
        ToolTipManager.sharedInstance().setInitialDelay(initialDelay);
        ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
    }

    public void mouseExited(MouseEvent e) {
        ToolTipManager.sharedInstance().setInitialDelay(NORMAL_INITIAL_DELAY);
        ToolTipManager.sharedInstance().setDismissDelay(NORMAL_DISMISS_DELAY);
    }


    private static final int NORMAL_DISMISS_DELAY = ToolTipManager
            .sharedInstance().getDismissDelay();

    private static final int NORMAL_INITIAL_DELAY = ToolTipManager
            .sharedInstance().getInitialDelay();

    public static final ToolTipTimingCustomizer INSTANCE = new ToolTipTimingCustomizer();

}
