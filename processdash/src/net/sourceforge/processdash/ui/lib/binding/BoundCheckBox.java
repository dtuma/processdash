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

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.XMLUtils;

public class BoundCheckBox extends JCheckBox {

    private ObservableMap map;

    private String propertyName;

    private Object trueValue;

    private Object falseValue;

    public BoundCheckBox(ObservableMap map, Element xml) {
        String propertyName = xml.getAttribute("id");

        Object trueValue = xml.getAttribute("trueValue");
        if (!XMLUtils.hasValue((String) trueValue))
            trueValue = Boolean.TRUE;

        Object falseValue = xml.getAttribute("falseValue");
        if (!XMLUtils.hasValue((String) falseValue))
            falseValue = Boolean.FALSE;

        init(map, propertyName, trueValue, falseValue);
    }

    public BoundCheckBox(ObservableMap map, String propertyName) {
        this(map, propertyName, Boolean.TRUE, Boolean.FALSE);
    }

    public BoundCheckBox(ObservableMap map, String propertyName,
            Object trueValue, Object falseValue) {
        init(map, propertyName, trueValue, falseValue);
    }

    protected void init(ObservableMap map, String propertyName,
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
