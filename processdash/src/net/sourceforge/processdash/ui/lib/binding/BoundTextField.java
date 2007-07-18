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
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.EventHandler;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JTextField;

import net.sourceforge.processdash.ui.lib.FormattedDocument;
import net.sourceforge.processdash.ui.lib.PaintUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundTextField extends JTextField {

    private BoundMap map;

    private String propertyName;

    private boolean allowBlank;

    private Format format;

    private Color normalBackground;

    public BoundTextField(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"),
                XMLUtils.getXMLInt(xml, "width"),
                xml.getAttribute("dataType"),
                "true".equalsIgnoreCase(xml.getAttribute("allowBlank")));
    }

    public BoundTextField(BoundMap map, String attributeName, int width,
            String type, boolean allowBlank) {
        super(width <= 0 ? 20 : width);
        setMinimumSize(getPreferredSize());

        this.map = map;
        this.propertyName = attributeName;
        this.allowBlank = allowBlank;

        if ("integer".equalsIgnoreCase(type))
            setNumberFormat(NumberFormat.getIntegerInstance());
        else if ("number".equalsIgnoreCase(type))
            setNumberFormat(NumberFormat.getInstance());

        this.normalBackground = getBackground();

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

    protected void setNumberFormat(NumberFormat fmt) {
        fmt.setGroupingUsed(false);
        setFormat(fmt);
    }

    protected void setFormat(Format fmt) {
        this.allowBlank = false;
        this.format = fmt;
        setDocument(new FormattedDocument(fmt));
    }

    public void updateFromMap() {
        setBackground(normalBackground);
        Object value = map.get(propertyName);
        String text = formatValue(value);
        setText(text);
    }

    protected String formatValue(Object value) {
        if (value == null)
            return "";

        else if (format != null)
            try {
                return format.format(value);
            } catch (Exception e) {}

        return StringUtils.asString(value);
    }

    public void updateFromText() {
        String text = getText();
        if (!allowBlank && !StringUtils.hasValue(text))
            text = null;
        Object val = null;
        if (text != null)
            val = parseText(text);
        if (val != INVALID_VALUE)
            map.put(propertyName, val);
    }

    protected Object parseText(String text) {
        if (format == null)
            return text;
        else
            try {
                setBackground(normalBackground);
                return format.parseObject(text);
            } catch (ParseException e) {
                Color c = map.getErrorColor(ErrorData.SEVERE);
                c = PaintUtils.mixColors(c, normalBackground, 0.2);
                setBackground(c);
                return INVALID_VALUE;
            }
    }
    private Object INVALID_VALUE = new Object();

}
