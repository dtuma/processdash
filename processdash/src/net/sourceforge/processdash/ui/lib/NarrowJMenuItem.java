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

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

/**
 * Class that behaves just like a JMenuItem, but can voluntarily truncate its
 * label and display "..." (like a JLabel does) to take up less space.
 */
public class NarrowJMenuItem extends JMenuItem {

    public static final int MIN_WIDTH = 30;

    private Rectangle viewRect, textRect, iconRect;

    private boolean narrowingEnabled = true;

    private int lastWidth = -1;

    private int preferredWidth = 600;

    private String altTextForPainting = null;

    public NarrowJMenuItem() {
        this("");
    }

    public NarrowJMenuItem(String s) {
        viewRect = new Rectangle();
        textRect = new Rectangle();
        iconRect = new Rectangle();
        setText(s);
    }

    public boolean isNarrowingEnabled() {
        return narrowingEnabled;
    }

    public void setNarrowingEnabled(boolean narrowingEnabled) {
        this.narrowingEnabled = narrowingEnabled;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        recalcTextTruncation();
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return new Point(-40, getHeight() + 2);
    }

    @Override
    public String getText() {
        if (altTextForPainting != null)
            return altTextForPainting;
        else
            return super.getText();
    }

    public String getFullText() {
        return super.getText();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        recalcTextTruncation();
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        recalcTextTruncation();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width != lastWidth) {
            recalcTextTruncation();
            lastWidth = width;
        }
    }

    protected void recalcTextTruncation() {
        String menuText = super.getText();
        altTextForPainting = null;
        if (isNarrowingNeeded(menuText) == false) {
            setToolTipText(null);
            return;
        }

        getBounds(viewRect);
        viewRect.width = preferredWidth;

        String layoutText = getTextToLayout(menuText);
        String fitLayoutText = SwingUtilities.layoutCompoundLabel(
            getFontMetrics(getFont()), layoutText, null,
            getVerticalAlignment(), getHorizontalAlignment(),
            getVerticalTextPosition(), getHorizontalTextPosition(), viewRect,
            iconRect, textRect, 0);

        if (fitLayoutText.equals(layoutText)) {
            altTextForPainting = null;
            setToolTipText(null);
        } else {
            altTextForPainting = getTextToDisplay(menuText, fitLayoutText);
        }
    }

    private boolean isNarrowingNeeded(String text) {
        return isNarrowingEnabled() //
                && text != null && text.length() > 3 //
                && super.getPreferredSize().width > getPreferredWidth();
    }

    protected String getTextToLayout(String menuText) {
        return menuText;
    }

    protected String getTextToDisplay(String menuText, String fitLayoutText) {
        setToolTipText(menuText);
        return fitLayoutText;
    }

}
