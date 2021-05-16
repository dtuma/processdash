// Copyright (C) 2005-2021 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sourceforge.processdash.DashController;
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
        DashboardIconFactory.setWindowIcon(frame);

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
        DashController.setRelativeLocation(frame, 100, 100);
        frame.setVisible(true);
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
