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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

/**
 * A widget used to group several other widgets with a TitledBorder
 */
public class BoundGrouping extends JPanel {

    private Insets insets;

    public BoundGrouping(BoundMap map, Element xml) {
        String label = map.getResource(xml.getAttribute("id") + ".Border_Label");

        TitledBorder border = new TitledBorder(label);
        this.setBorder(border);
        this.setLayout(new GridBagLayout());

        this.insets = parseInsetsSpec(xml.getAttribute("insets"));

        if (map instanceof BoundForm) {
            ((BoundForm) map).addFormElements(this, xml);
        }
    }

    private Insets parseInsetsSpec(String spec) {
        if (!XMLUtils.hasValue(spec))
            return null;

        try {
            String[] values = spec.split(",");
            return new Insets(parseInt(values[0]), parseInt(values[1]),
                    parseInt(values[2]), parseInt(values[3]));
        } catch (Exception e) {}

        return null;
    }

    private int parseInt(String s) {
        return Integer.parseInt(s.trim());
    }

    @Override
    public void add(Component comp, Object constraints) {
        if (insets != null && constraints instanceof GridBagConstraints) {
            GridBagConstraints gbc = (GridBagConstraints) constraints;
            gbc.insets = this.insets;
        }
        super.add(comp, constraints);
    }

}
