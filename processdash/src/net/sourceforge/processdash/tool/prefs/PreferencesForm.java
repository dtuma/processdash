// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.prefs;

import java.util.SortedSet;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This form is used to modify the user preferences under a specific category.
 */
public class PreferencesForm extends JPanel {

    JLabel label = new JLabel();

    public PreferencesForm() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(label);
    }

    public void setCategory(PreferencesCategory category) {

        if (category != null) {
            StringBuffer labelText = new StringBuffer("<html><h2>Spec Files :</h2><p>");

            SortedSet<PreferencesPane> panes = category.getPanes();

            for (PreferencesPane pane : panes) {
                labelText.append(pane.getSpecFile() + "<br />");
            }

            labelText.append("</p></html>");
            label.setText(labelText.toString());
        }
    }

}
