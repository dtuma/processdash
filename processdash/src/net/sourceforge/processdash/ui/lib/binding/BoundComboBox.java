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
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.sourceforge.processdash.util.ObservableMap;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundComboBox extends JComboBox {

    private ObservableMap map;

    private String propertyName;

    private String selectionPrompt;

    private String listName;

    private String listIdName;

    private String listDisplayName;

    private Color implicitColor;


    public BoundComboBox(ObservableMap map, Element xml) {
        this(map, xml.getAttribute("id"), //
                xml.getAttribute("choices"), //
                XMLUtils.getAttribute(xml, "choiceValue", "VALUE"), //
                XMLUtils.getAttribute(xml, "choiceDisplay", "DISPLAY"), //
                xml.getAttribute("selectionPrompt"));
    }

    public BoundComboBox(ObservableMap map, String propertyName,
            String listName) {
        this(map, propertyName, listName, "VALUE", "DISPLAY", null);
    }

    public BoundComboBox(ObservableMap map, String propertyName,
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
        // doesn't appear in the list! What should we do?
        // setSelectedItem(null);
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
            ErrorData errorData = BoundForm.getErrorDataForAttr(map, listName);
            if (errorData != null) {
                Color c = BoundForm.getErrorColor(map, errorData);
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

    private static class ListRenderer extends BasicComboBoxRenderer {

        private boolean inGet = false;

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            inGet = true;
            Component result = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            if (value instanceof MessageItem)
                result.setForeground(((MessageItem) value).getColor());
            inGet = false;
            return result;
        }

        public void setForeground(Color fg) {
            if (inGet)
                super.setForeground(fg);
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
        if (a == null)
            return false;
        return a.equals(b);
    }
}
