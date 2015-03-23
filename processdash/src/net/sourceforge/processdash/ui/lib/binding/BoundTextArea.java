// Copyright (C) 2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class BoundTextArea extends JScrollPane {

    private static final int DEFAULT_COLS = 20;

    private static final int DEFAULT_ROWS = 5;

    private BoundMap map;

    private String propertyName;

    private JTextArea textArea;

    private boolean allowBlank;

    public BoundTextArea(BoundMap map, Element xml) {
        this(map, xml, "id");
    }

    public BoundTextArea(BoundMap map, Element xml, String propertyNameAttr) {
        this(map, xml.getAttribute(propertyNameAttr), //
                XMLUtils.getXMLInt(xml, "height"), //
                XMLUtils.getXMLInt(xml, "width"), //
                "true".equalsIgnoreCase(xml.getAttribute("allowBlank")), //
                !"false".equalsIgnoreCase(xml.getAttribute("editable")));
    }

    public BoundTextArea(BoundMap map, String propertyName, int rows, int cols,
            boolean allowBlank, boolean editable) {
        this.map = map;
        this.propertyName = propertyName;
        this.allowBlank = allowBlank;

        this.textArea = new JTextArea(rows <= 0 ? DEFAULT_ROWS : rows,
                cols <= 0 ? DEFAULT_COLS : cols);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(editable);
        if (!editable)
            textArea.setBackground(null);
        setViewportView(textArea);

        map.addPropertyChangeListener(propertyName, this, "updateFromMap");
        if (editable)
            addUpdateFromTextListeners();

        updateFromMap();
    }

    protected void addUpdateFromTextListeners() {
        textArea.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateFromText();
            }
        });
    }

    public void updateFromMap() {
        Object value = map.get(propertyName);
        String text = value == null ? "" : value.toString();

        if (!textArea.getText().equals(text)) {
            textArea.setText(text);
            textArea.setCaretPosition(0);
        }
    }

    public void updateFromText() {
        String text = textArea.getText();
        if (!allowBlank && !StringUtils.hasValue(text))
            text = null;
        Object oldVal = map.get(propertyName);
        if (!NullSafeObjectUtils.EQ(text, oldVal))
            map.put(propertyName, text);
    }

}
