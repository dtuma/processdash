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

    private static final int DEFAULT_NB_OF_COLUMNS = 20;

    private BoundMap map;

    private String propertyName;

    private boolean allowBlank;

    private Format format;

    private Color normalBackground;

    public BoundTextField(BoundMap map, Element xml) {
        this(map, xml, "id");
    }

    public BoundTextField(BoundMap map, Element xml, String propertyNameAttr) {
        this(map, xml.getAttribute(propertyNameAttr),
             XMLUtils.getXMLInt(xml, "width"),
             xml.getAttribute("dataType"),
             "true".equalsIgnoreCase(xml.getAttribute("allowBlank")));
    }

    public BoundTextField(BoundMap map, String attributeName, int width,
            String type, boolean allowBlank) {
        this.setColumns(width <= 0 ? DEFAULT_NB_OF_COLUMNS : width);
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
        addUpdateFromTextListeners();

        updateFromMap();
    }

    protected void addUpdateFromTextListeners() {
        addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "updateFromText"));
        addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateFromText();
            }
        });
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

        if (!getText().equals(text))
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
        if (val != INVALID_VALUE) {
            Object oldVal = map.get(propertyName);
            if (!eq(val, oldVal))
                map.put(propertyName, val);
        }
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

    private static boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            Number aa = (Number) a;
            Number bb = (Number) b;
            double diff = Math.abs(aa.doubleValue() - bb.doubleValue());
            return diff < 0.00001;
        }
        return a.equals(b);
    }
}
