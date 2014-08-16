// Copyright (C) 2006-2014 Tuma Solutions, LLC
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

/**
 * Class that behaves just like a JMenu, but can truncate its label and display
 * "..." (like a JLabel does) if it is given too little space.
 */
public class NarrowJMenu extends JMenu {

    public static final int MIN_WIDTH = 30;

    private Rectangle viewRect, textRect, iconRect;

    private boolean narrowingEnabled = true;

    private String altTextForPainting = null;

    public NarrowJMenu() {
        this("");
    }

    public NarrowJMenu(String s) {
        setText(s);
        viewRect = new Rectangle();
        textRect = new Rectangle();
        iconRect = new Rectangle();
    }

    public boolean isNarrowingEnabled() {
        return narrowingEnabled;
    }

    public void setNarrowingEnabled(boolean narrowingEnabled) {
        this.narrowingEnabled = narrowingEnabled;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension result = super.getPreferredSize();
        if (narrowingEnabled)
            result.width = MIN_WIDTH;
        return result;
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return TOOLTIP_LOCATION;
    }

    @Override
    public String getText() {
        if (altTextForPainting == null)
            return super.getText();
        else
            return altTextForPainting;
    }

    @Override
    public void paint(Graphics g) {
        String menuText = super.getText();
        if (isNarrowingNeeded(menuText) == false) {
            setToolTipText(null);
            super.paint(g);
            return;
        }

        getBounds(viewRect);
        Insets insets = getInsets();
        viewRect.width -= insets.left + insets.top + 2 * getIconTextGap();

        String layoutText = getTextToLayout(menuText);
        String fitLayoutText = SwingUtilities.layoutCompoundLabel(
            getFontMetrics(getFont()), layoutText, null,
            getVerticalAlignment(), getHorizontalAlignment(),
            getVerticalTextPosition(), getHorizontalTextPosition(), viewRect,
            iconRect, textRect, 0);

        if (fitLayoutText.equals(layoutText)) {
            setToolTipText(null);
            super.paint(g);
        } else {
            altTextForPainting = getTextToDisplay(menuText, fitLayoutText);
            super.paint(g);
            altTextForPainting = null;
        }
    }

    private boolean isNarrowingNeeded(String text) {
        return isNarrowingEnabled() //
                && text != null && text.length() > 3 //
                && getPreferredSize().width > getWidth();
    }

    protected String getTextToLayout(String menuText) {
        return menuText;
    }

    protected String getTextToDisplay(String menuText, String fitLayoutText) {
        setToolTipText(menuText);
        return fitLayoutText;
    }

    public static final Point TOOLTIP_LOCATION = getTooltipOffset();

    private static Point getTooltipOffset() {
        JToolTip tip = new JToolTip();
        tip.setTipText("X");
        int delta = tip.getPreferredSize().height + 2;
        return new Point(0, -delta);
    }

}
