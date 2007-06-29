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

package net.sourceforge.processdash.ui.lib.binding;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundForm extends BoundMap {

    private Container container;
    private int componentCount;


    public BoundForm() {
        this(new JPanel(new GridBagLayout()));
    }

    public BoundForm(Container container) {
        this.container = container;
        this.componentCount = 0;
    }

    public Container getContainer() {
        return container;
    }

    public void disposeForm() {
        disposeMap();
    }



    protected void addFormElement(Object element, Element xml) {
        super.addFormElement(element, xml);

        if (element instanceof JComponent) {
            JComponent component = (JComponent) element;
            addFormComponent(component, xml);
        }
    }


    protected void addFormComponent(JComponent component, Element xml) {
        String label = xml.getAttribute("label");

        addFormComponent(component, label);

        String tooltip = xml.getAttribute("tooltip");
        if (StringUtils.hasValue(tooltip))
            component.setToolTipText(tooltip);
    }

    protected void addFormComponent(JComponent component, String label) {
        Container container = getContainer();

        if (StringUtils.hasValue(label)) {
            JLabel l = new JLabel(label);
            l.setLabelFor(component);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;  c.gridy = componentCount;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(5, 5, 5, 5);
            container.add(l, c);

            c = new GridBagConstraints();
            c.gridx = 1; c.gridy = componentCount;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(5, 5, 5, 5);
            c.weightx = 1; c.ipady = 5;
            container.add(component, c);

        } else {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = componentCount;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1; c.gridwidth = 2;
            c.insets = new Insets(5, 5, 5, 5);
            container.add(component, c);
        }

        componentCount++;
    }

}
