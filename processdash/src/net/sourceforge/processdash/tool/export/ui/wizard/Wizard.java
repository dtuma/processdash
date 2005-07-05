// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;

public class Wizard {

    static final Resources resources = Resources
            .getDashBundle("ImportExport.Wizard");

    JFrame frame;

    JPanel panelHolder;

    Vector panels;

    int panelPos;

    public Wizard(String titleKey) {
        frame = new JFrame(resources.getString(titleKey));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());

        panelHolder = new JPanel(new BorderLayout());
        panelHolder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.getContentPane().add(panelHolder);

        panels = new Vector();
        panelPos = -1;
    }

    public void setSize(int width, int height) {
        frame.setSize(new Dimension(width, height));
    }

    public void show() {
        frame.show();
    }

    public void close() {
        frame.dispose();
    }

    public boolean hasPanels() {
        return panels.size() > 0;
    }

    public void goBackward() {
        if (panelPos > 0)
            setPanel(panelPos - 1);
    }

    public void goForward(WizardPanel panel) {
        int currentPos = panels.indexOf(panel);
        if (currentPos != panelPos + 1) {
            panels.setSize(panelPos + 1);
            panels.add(panel);
        }
        setPanel(panelPos + 1);
    }

    private void setPanel(int i) {
        if (i < 0 || i >= panels.size())
            return;

        panelPos = i;
        panelHolder.setVisible(false);
        panelHolder.removeAll();
        panelHolder.add((Component) panels.get(panelPos));
        panelHolder.setVisible(true);
    }

}
