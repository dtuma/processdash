// Copyright (C) 2007 Tuma Solutions, LLC
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JPasswordField;

import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundPasswordField extends JPasswordField {

    private BoundMap map;

    private String propertyName;

    public BoundPasswordField(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"));
    }

    public BoundPasswordField(BoundMap map, String attributeName) {
        super(20);
        setMinimumSize(getPreferredSize());

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
        String str = StringUtils.asString(value);
        str = map.unhashValue(str);
        setText(str);
    }

    public void updateFromText() {
        String str = new String(getPassword());
        str = map.hashValue(str);
        map.put(propertyName, str);
    }

}
