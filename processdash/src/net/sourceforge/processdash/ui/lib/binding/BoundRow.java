// Copyright (C) 2009-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.binding;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.w3c.dom.Element;

/**
 * A widget used to group several other widgets in a horizontal row
 */
public class BoundRow extends JPanel {

    public BoundRow(BoundMap map, Element xml) {
        BoxLayout l = new BoxLayout(this, BoxLayout.X_AXIS);
        this.setLayout(l);

        if (map instanceof BoundForm) {
            ((BoundForm) map).addFormElements(this, xml);
        }
    }

}
