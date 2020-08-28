// Copyright (C) 2020 Tuma Solutions, LLC
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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.ToolTipManager;


/**
 * This class provides a JMenuItem with an optional hotspot on its the far right
 * edge.
 * 
 * This object will behave like a normal JMenuItem until a hotspot is registered
 * with the {@link #setHotspot(Action)} method. Once a hotspot action is
 * registered, its icon will be displayed on the far-right when the menu item is
 * selected. A "hand" cursor is shown when the mouse is over the hotspot icon,
 * and if the user clicks there, the hotspot action is called (instead of
 * triggering the main JMenu).
 */
public class JHotspotMenuItem extends JMenuItem {

    private Action hotspot;

    private Icon hotspotIcon;

    private int hotspotIconWidth, hotspotIconHeight;

    private String hotspotTooltip;

    private int hotspotLeftGap = 5, hotspotRightGap = 5;

    private boolean mouseIsOverHotspot;


    public JHotspotMenuItem() {}

    public JHotspotMenuItem(Action a) {
        super(a);
    }

    public JHotspotMenuItem(String text) {
        super(text);
    }

    public JHotspotMenuItem(String text, Icon icon) {
        super(text, icon);
    }


    public JHotspotMenuItem setHotspot(Action hotspot) {
        if (hotspot == null) {
            this.hotspotIcon = null;
            this.hotspotIconWidth = this.hotspotIconHeight = 0;
            this.hotspotTooltip = null;
        } else {
            if (this.hotspot == null)
                addMouseMotionListener(new MouseMotionHandler());
            this.hotspotIcon = (Icon) hotspot.getValue(Action.SMALL_ICON);
            this.hotspotIconWidth = hotspotIcon.getIconWidth();
            this.hotspotIconHeight = hotspotIcon.getIconHeight();
            this.hotspotTooltip = (String) hotspot
                    .getValue(Action.SHORT_DESCRIPTION);
            if (hotspotTooltip != null)
                ToolTipManager.sharedInstance().registerComponent(this);
        }
        this.hotspot = hotspot;
        return this;
    }

    public int getHotspotLeftGap() {
        return hotspotLeftGap;
    }

    public void setHotspotLeftGap(int hotspotLeftGap) {
        this.hotspotLeftGap = hotspotLeftGap;
    }

    public int getHotspotRightGap() {
        return hotspotRightGap;
    }

    public void setHotspotRightGap(int hotspotRightGap) {
        this.hotspotRightGap = hotspotRightGap;
    }

    private int getIconLeft() {
        return getWidth() - hotspotIconWidth - hotspotRightGap;
    }


    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (hotspot != null) {
            d.width += hotspotIconWidth + hotspotLeftGap + hotspotRightGap;
            d.height = Math.max(d.height, hotspotIconHeight);
        }
        return d;
    }

    @Override
    public String getToolTipText() {
        if (mouseIsOverHotspot)
            return hotspotTooltip;
        else
            return super.getToolTipText();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (hotspotIcon != null && (isSelected() || isArmed())) {
            int x = getIconLeft();
            int y = (getHeight() - hotspotIconHeight) / 2;
            hotspotIcon.paintIcon(this, g, x, y);
        }
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        if (hotspot != null && mouseIsOverHotspot)
            hotspot.actionPerformed(event);
        else
            super.fireActionPerformed(event);
    }


    private class MouseMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseIsOverHotspot = hotspot != null && (e.getX() > getIconLeft());
            setCursor(mouseIsOverHotspot ? HAND : null);
        }
    }

    private static Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

}
