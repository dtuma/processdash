// Copyright (C) 2007-2009 Tuma Solutions, LLC
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

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundCheckBox extends JCheckBox {

    /** XML tag specifying if the widget has inverted "true" and "false" values */
    private static final String INVERTED_TAG = "inverted";

    protected BoundMap map;

    protected String propertyName;

    private Object trueValue;

    private Object falseValue;


    public BoundCheckBox(BoundMap map, Element xml) {
        this(map, xml, "id", Boolean.TRUE, Boolean.FALSE);
    }

    protected BoundCheckBox(BoundMap map, Element xml, String propertyNameAttr,
            Object defaultTrueValue, Object defaultFalseValue) {
        String propertyName = xml.getAttribute(propertyNameAttr);

        if (Boolean.parseBoolean(xml.getAttribute(INVERTED_TAG))) {
            Object tmp = defaultTrueValue;
            defaultTrueValue = defaultFalseValue;
            defaultFalseValue = tmp;
        }

        Object trueValue = xml.getAttribute("trueValue");
        if (!XMLUtils.hasValue((String) trueValue))
            trueValue = defaultTrueValue;

        Object falseValue = xml.getAttribute("falseValue");
        if (!XMLUtils.hasValue((String) falseValue))
            falseValue = defaultFalseValue;

        init(map, propertyName, trueValue, falseValue);

        String rightHandLabel = map.getAttrOrResource(xml, null,
            "Checkbox_Label", null);
        if (rightHandLabel != null)
            setText(rightHandLabel);
    }

    public BoundCheckBox(BoundMap map, String propertyName) {
        this(map, propertyName, Boolean.TRUE, Boolean.FALSE);
    }

    public BoundCheckBox(BoundMap map, String propertyName,
            Object trueValue, Object falseValue) {
        init(map, propertyName, trueValue, falseValue);
    }

    protected void init(BoundMap map, String propertyName,
            Object trueValue, Object falseValue) {
        this.map = map;
        this.propertyName = propertyName;
        this.trueValue = trueValue;
        this.falseValue = falseValue;

        Object listener = EventHandler.create(PropertyChangeListener.class,
                this, "updateFromMap");
        map.addPropertyChangeListener(propertyName,
                (PropertyChangeListener) listener);

        listener = EventHandler.create(ActionListener.class, this,
                "updateFromState");
        addActionListener((ActionListener) listener);

        updateFromMap();
    }

    public void updateFromMap() {
        Object value = map.get(propertyName);
        if (value == null)
            setSelected(false);
        else if (value.equals(trueValue)
                || (trueValue == Boolean.TRUE && value.equals("true")))
            setSelected(true);
        else
            setSelected(false);
    }

    public void updateFromState() {
        Object value = isSelected() ? trueValue : falseValue;
        map.put(propertyName, value);
    }

}
