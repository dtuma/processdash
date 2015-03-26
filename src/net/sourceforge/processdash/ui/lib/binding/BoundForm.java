// Copyright (C) 2007-2015 Tuma Solutions, LLC
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.util.HTMLUtils;
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

    /**
     * Used to add form elements to a different Container than the one
     *  in this BoundForm.
     */
    public List addFormElements(Container container, Element xml) {
        Container originalContainer = this.container;

        this.container = container;
        List elements = super.addFormElements(xml);
        this.container = originalContainer;

        return elements;
    }

    protected void addFormElement(Object element, Element xml) {
        super.addFormElement(element, xml);

        if (element instanceof JComponent) {
            JComponent component = (JComponent) element;
            addFormComponent(component, xml);
        }
    }


    protected void addFormComponent(JComponent component, Element xml) {
        String label = getAttrOrResource(xml, null, "Label", null);
        String suffix = getAttrOrResource(xml, null, "Suffix", null);

        addFormComponent(component, label, suffix);

        String tooltip = getAttrOrResource(xml, null, "Tooltip", null);
        if (StringUtils.hasValue(tooltip))
            setComponentToolTip(component, tooltip);

        String enablingProperty = xml.getAttribute("enabledIf");
        String disablingProperty = xml.getAttribute("disabledIf");

        EnablePropertyListener enableListener = new EnablePropertyListener(component,
                                                                           enablingProperty,
                                                                           disablingProperty);

        maybeAddEnablePropertyChangeListener(enablingProperty, enableListener);
        maybeAddEnablePropertyChangeListener(disablingProperty, enableListener);
    }

    private void setComponentToolTip(JComponent component, String tooltip) {
        if (StringUtils.hasValue(tooltip)) {
            if (!tooltip.startsWith("<html>")) {
                tooltip = "<html><div width='300'>"
                    + HTMLUtils.escapeEntities(tooltip)
                    + "</div></html>";
            }
            component.setToolTipText(tooltip);
        }
    }

    private void maybeAddEnablePropertyChangeListener(String property,
                                                      EnablePropertyListener enableListener) {
        if (StringUtils.hasValue(property)) {
            this.addPropertyChangeListener(property, enableListener);

            // Forcing a PropertyChangeEvent to enable or disable the widget based on the
            //  current value of the property
            enableListener.propertyChange(
                new PropertyChangeEvent(this,
                                        property,
                                        null,
                                        this.get(property)));
        }
    }

    protected void addFormComponent(JComponent component, String label,
            String suffix) {
        if (suffix != null)
            component = BoxUtils.hbox(component, 5, suffix);

        addFormComponent(component, label);
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
            configComponentStretch(component, c);
            c.insets = new Insets(5, 5, 5, 5);
            container.add(component, c);
        }

        componentCount++;
    }

    @SuppressWarnings("static-access")
    private void configComponentStretch(JComponent component,
            GridBagConstraints c) {
        Dimension pref = component.getPreferredSize();
        if (pref == null)
            return;
        c.weighty = pref.getHeight();

        Dimension max = component.getMaximumSize();
        if (max == null)
            return;
        boolean stretchX = (max.getWidth() > pref.getWidth());
        boolean stretchY = (max.getHeight() > pref.getHeight());
        c.fill = (stretchX ? (stretchY ? c.BOTH     : c.HORIZONTAL)
                           : (stretchY ? c.VERTICAL : c.NONE));
    }

}
