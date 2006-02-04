// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Class that behaves just like a JMenu, but can truncate its label and display
 * "..." (like a JLabel does) if it is given too little space.
 */
public class NarrowJMenu extends JMenu {

        private Rectangle viewRect, textRect, iconRect;

        private String altTextForPainting = null;


        public NarrowJMenu() {
                this("");
        }

        public NarrowJMenu(String s) {
                super(s);
                viewRect = new Rectangle();
                textRect = new Rectangle();
                iconRect = new Rectangle();
        }

        public Dimension getMinimumSize() {
                Dimension result = super.getPreferredSize();
                result.width = 30;
                return result;
        }

        public String getText() {
                if (altTextForPainting == null)
                        return super.getText();
                else
                        return altTextForPainting;
        }

        public void paint(Graphics g) {
                getBounds(viewRect);
                removeInsets(viewRect, getInsets());
                removeInsets(viewRect, getMargin());

                String menuText = super.getText();
                String textToDisplay = SwingUtilities.layoutCompoundLabel(
                                getFontMetrics(getFont()), menuText, null,
                                getVerticalAlignment(), getHorizontalAlignment(),
                                getVerticalTextPosition(), getHorizontalTextPosition(),
                                viewRect, iconRect, textRect, 0);
                setToolTipText(menuText.equals(textToDisplay) ? null : menuText);

                altTextForPainting = textToDisplay;
                super.paint(g);
                altTextForPainting = null;
        }

        private void removeInsets(Rectangle r, Insets i) {
                if (i != null) {
                        r.x += i.left;
                        r.y += i.top;
                        r.width -= (i.right + i.left);
                        r.height -= (i.bottom + i.top);
                }
        }
}
