// Copyright (C) 2012-2017 Tuma Solutions, LLC
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Shape;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;


/**
 * This class implements a button that users can click to show or hide the
 * colunms in a JTable.
 * 
 * This button is designed to be added to the top-right corner of the scroll
 * pane that holds the table.
 */
public class JTableColumnVisibilityButton extends JButton {

    public JTableColumnVisibilityButton(JTable table, ResourceBundle resources,
            String readOnlyNamePat, int... readOnlyColumns) {

        super(new JTableColumnVisibilityAction(table, resources,
                readOnlyNamePat, readOnlyColumns));

        setDisabledIcon(new ColumnSelectorIcon(false));
        setBorder(new ChoppedEtchedBorder());
        setFocusable(false);
        setToolTipText(getText());
        setText(null);
    }

    @Override
    public JTableColumnVisibilityAction getAction() {
        return (JTableColumnVisibilityAction) super.getAction();
    }


    private class ChoppedEtchedBorder extends EtchedBorder {

        public void paintBorder(Component c, Graphics g, int x, int y,
                int width, int height) {
            Shape clipping = g.getClip();
            g.setClip(x, y, width, height);
            super.paintBorder(c, g, x, y, width, height + 1);
            g.setClip(clipping);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 1, 2);
        }
    }

    public JScrollPane install(JScrollPane sp) {
        sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, this);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return sp;
    }

}
