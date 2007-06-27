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
import java.beans.PropertyChangeListener;

import javax.swing.JPasswordField;

import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundPasswordField extends JPasswordField {

    private ObservableMap map;

    private String propertyName;

    public BoundPasswordField(ObservableMap map, Element xml) {
        this(map, xml.getAttribute("id"));
    }

    public BoundPasswordField(ObservableMap map, String attributeName) {
        this.map = map;
        this.propertyName = attributeName;

        Object listener = EventHandler.create(PropertyChangeListener.class,
                this, "updateFromMap");
        map.addPropertyChangeListener(attributeName,
                (PropertyChangeListener) listener);

        listener = EventHandler.create(ActionListener.class, this,
                "updateFromText");
        addActionListener((ActionListener) listener);
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
        map.put(propertyName, new String(getPassword()));
    }

}
