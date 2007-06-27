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

import java.awt.Color;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;

import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.SqlResultData;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class BoundLabel extends JLabel {

    private ObservableMap map;

    private String propertyName;

    private Color normalColor;

    public BoundLabel(ObservableMap map, Element xml) {
        this(map, xml.getAttribute("id"));
    }

    public BoundLabel(ObservableMap map, String attributeName) {
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
        ErrorData errorData = BoundForm.getErrorDataForAttr(map, propertyName);
        if (errorData != null) {
            setForeground(BoundForm.getErrorColor(map, errorData));
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
