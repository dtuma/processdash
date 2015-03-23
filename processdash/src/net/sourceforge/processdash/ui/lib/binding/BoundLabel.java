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

import java.awt.Color;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;

import net.sourceforge.processdash.util.SqlResultData;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundLabel extends JLabel {

    private BoundMap map;

    private String propertyName;

    private Color normalColor;

    public BoundLabel(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"));
    }

    public BoundLabel(BoundMap map, String attributeName) {
        this.map = map;
        this.propertyName = attributeName;
        this.normalColor = getForeground();

        Object listener = EventHandler.create(PropertyChangeListener.class,
                this, "updateFromMap");
        map.addPropertyChangeListener(attributeName,
                (PropertyChangeListener) listener);
        updateFromMap();
    }

    public void updateFromMap() {
        ErrorData errorData = map.getErrorDataForAttr(propertyName);
        if (errorData != null) {
            setForeground(map.getErrorColor(errorData));
            setText(errorData.getError());
            return;
        }

        Object value = map.get(propertyName);

        if (value instanceof SqlResultData) {
            SqlResultData data = (SqlResultData) value;
            value = data.getSingleValue();
        }

        String text = StringUtils.asString(value);
        if (text != null && text.indexOf('\n') != -1)
            text = "<html><body><pre>" + text + "</pre></body></html>";

        setForeground(normalColor);
        setText(text);
    }
}
