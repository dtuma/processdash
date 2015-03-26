// Copyright (C) 2000-2003 Tuma Solutions, LLC
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
