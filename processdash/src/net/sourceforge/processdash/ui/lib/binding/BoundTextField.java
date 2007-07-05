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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.EventHandler;

import javax.swing.JTextField;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundTextField extends JTextField {

    private BoundMap map;

    private String propertyName;

    private boolean allowBlank;

    public BoundTextField(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"),
                XMLUtils.getXMLInt(xml, "width"),
                "true".equalsIgnoreCase(xml.getAttribute("allowBlank")));
    }

    public BoundTextField(BoundMap map, String attributeName, int width,
            boolean allowBlank) {
        super(width <= 0 ? 20 : width);
        setMinimumSize(getPreferredSize());

        this.map = map;
        this.propertyName = attributeName;
        this.allowBlank = allowBlank;

        map.addPropertyChangeListener(attributeName, this, "updateFromMap");

        addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "updateFromText"));
        addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateFromText();
            }
        });

        updateFromMap();
    }

    public void updateFromMap() {
        Object value = map.get(propertyName);
        setText(StringUtils.asString(value));
    }

    public void updateFromText() {
        String text = getText();
        if (!allowBlank && !StringUtils.hasValue(text))
            text = null;
        Object val = null;
        if (text != null)
            val = parseText(text);
        map.put(propertyName, val);
    }

    protected Object parseText(String text) {
        return text;
    }

}
