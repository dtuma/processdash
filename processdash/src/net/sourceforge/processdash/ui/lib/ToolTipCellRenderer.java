// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ui.lib;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import java.util.*;

public class ToolTipCellRenderer extends JLabel implements ListCellRenderer {
    Map toolTips, translations;
    Color highlight = Color.blue.darker().darker();

    public ToolTipCellRenderer(Map toolTips, Map translations) {
        this.toolTips = toolTips;
        this.translations = translations;
        setOpaque(true);
    }

    public Component getListCellRendererComponent (JList list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus) {
        String val = value.toString();
        String display = val;
        if (translations != null) {
            String trans = (String) translations.get(val);
            if (trans != null) display = trans;
        }
        setText(display + "  ");
        if (isSelected && toolTips != null) {
            list.setToolTipText((String) toolTips.get(val));
        }
        setBackground(isSelected ? highlight : Color.white);
        setForeground(isSelected ? Color.white : Color.black);
        return this;
    }
}
