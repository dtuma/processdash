// Copyright (C) 2007-2021 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundComboBox extends JComboBox {

    protected BoundMap map;

    protected String propertyName;

    private String selectionPrompt;

    private String listName;

    private String listIdName;

    private String listDisplayName;

    private Color implicitColor;


    public BoundComboBox(BoundMap map, Element xml) {
        this(map, xml.getAttribute("id"), //
                xml.getAttribute("choices"), //
                XMLUtils.getAttribute(xml, "choiceValue", "VALUE"), //
                XMLUtils.getAttribute(xml, "choiceDisplay", "DISPLAY"), //
                map.getAttrOrResource(xml, null, "Selection_Prompt", ""));
    }

    public BoundComboBox(BoundMap map, String propertyName,
            String listName) {
        this(map, propertyName, listName, "VALUE", "DISPLAY", null);
    }

    public BoundComboBox(BoundMap map, String propertyName,
            String listName, String listIdName, String listDisplayName,
            String selectionPrompt) {
        this.map = map;
        this.propertyName = propertyName;
        this.listName = listName;
        this.listIdName = listIdName;
        this.listDisplayName = listDisplayName;
        this.selectionPrompt = selectionPrompt;

        map.addPropertyChangeListener(listName, this, "updateListFromMap");
        updateListFromMap();

        map.addPropertyChangeListener(propertyName, this, "updateValueFromMap");
        updateValueFromMap();

        addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "updateMapFromValue"));

        implicitColor = getForeground().brighter().brighter();

        setRenderer(new ListRenderer());
    }

    public void updateValueFromMap() {
        Object value = map.get(propertyName);
        for (int i = 0; i < getItemCount(); i++) {
            Object obj = getItemAt(i);
            if (!(obj instanceof Item))
                continue;
            Item item = (Item) obj;
            if (item.valueMatches(value)) {
                setSelectedIndex(i);
                return;
            }
        }
        // if we reach this point, then someone has put a value in the Map that
        // doesn't appear in our list.  We need an implicit item to hold it.
        MessageItem newValue;
        if (getItemCount() > 0 && getItemAt(0) instanceof MessageItem) {
            // if the first item in the list is already a message item, reuse
            // it to hold the designated value.
            newValue = (MessageItem) getItemAt(0);
            newValue.value = value;
            setSelectedIndex(0);
        } else {
            // otherwise, we need to create a new value item to add to the list.
            newValue = new MessageItem(value, "", getForeground());
            insertItemAt(newValue, 0);
            setSelectedItem(newValue);
        }
    }

    public void updateMapFromValue() {
        Object obj = getSelectedItem();
        if (!(obj instanceof Item))
            return;
        Item item = (Item) obj;
        Object value = (item == null ? null : item.value);
        if (!(item instanceof MessageItem)) {
            for (int i = getItemCount(); i-- > 0;)
                if (getItemAt(i) instanceof MessageItem)
                    removeItemAt(i);
        }
        map.put(propertyName, value);
    }

    public void updateListFromMap() {
        Object value = map.get(propertyName);

        removeAllItems();

        Object listObj = map.get(listName);
        if (ErrorValue.isRealError(listObj) || !(listObj instanceof List)) {
            ErrorData errorData = map.getErrorDataForAttr(listName);
            if (errorData != null) {
                Color c = map.getErrorColor(errorData);
                addItem(new MessageItem(null, errorData.getError(), c));
                setSelectedIndex(0);
                updateMapFromValue();
            }
            return;
        }
        Item selectedValue = new MessageItem(value, selectionPrompt,
                implicitColor);
        addItem(selectedValue);
        setSelectedIndex(0);

        List list = (List) listObj;
        for (Iterator i = list.iterator(); i.hasNext();) {
            if (addListItem(i.next(), value)) {
                setSelectedIndex(getItemCount() - 1);
                if (selectedValue != null) {
                    removeItem(selectedValue);
                    selectedValue = null;
                }
            }
        }

        if (selectedValue != null) {
            selectedValue.value = null;
            updateMapFromValue();
        }
    }

    private boolean addListItem(Object item, Object selectedValue) {
        if (item instanceof Map) {
            Map m = (Map) item;
            Object value = m.get(listIdName);
            String display = StringUtils.asString(m.get(listDisplayName));
            addItem(new Item(value, display));
            return (eq(value, selectedValue));
        } else {
            addItem(new Item(item));
            return eq(item, selectedValue);
        }
    }

    protected boolean selectedItemIsImplicit() {
        return getSelectedItem() instanceof MessageItem;
    }

    private static class ListRenderer extends BasicComboBoxRenderer {

        private boolean inGet = false;

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            inGet = true;
            Component result = super.getListCellRendererComponent(list, value,
                    index, index < 0 ? false : isSelected, cellHasFocus);
            if (value instanceof MessageItem)
                result.setForeground(((MessageItem) value).getColor());
            inGet = false;
            return result;
        }

        public void setForeground(Color fg) {
            if (inGet)
                super.setForeground(fg);
        }

        public void setBackground(Color bg) {
            if (inGet)
                super.setBackground(bg);
        }

    }


    private class Item {
        public Object value;

        public String display;

        public Item(Object value) {
            this(value, StringUtils.asString(value));
        }

        public boolean valueMatches(Object val) {
            return eq(val, value);
        }

        public Item(Object value, String display) {
            this.value = value;
            this.display = display;
        }

        public String toString() {
            return display;
        }
    }

    private class MessageItem extends Item {
        public Color color;

        public MessageItem(Object value, String display, Color color) {
            super(value, display);
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }


    private static boolean eq(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b) || a.toString().equals(b.toString());
    }
}
