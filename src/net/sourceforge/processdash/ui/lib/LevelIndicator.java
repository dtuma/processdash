// Copyright (C) 2007-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

public class LevelIndicator extends JComponent {

    private Color bgColor = Color.black;

    private Color gridColor = Color.darkGray;

    private int gridSpacing = 2;

    private Color barColor = Color.gray;

    private Color barHighlight = Color.white;

    private boolean vertical = true;

    private boolean paintBarRect = true;

    private double level = 0;

    private Color[] barGradient = null;

    public LevelIndicator(boolean vertical) {
        this.vertical = vertical;
    }

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = Math.min(1, Math.max(0, level));
        repaint();
    }

    public Color getBarColor() {
        return barColor;
    }

    public void setBarColor(Color barColor) {
        this.barColor = barColor;
        this.barGradient = null;
        repaint();
    }

    public Color getBarHighlight() {
        return barHighlight;
    }

    public void setBarHighlight(Color barHighlight) {
        this.barHighlight = barHighlight;
        this.barGradient = null;
        repaint();
    }

    public Color getBgColor() {
        return bgColor;
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
        repaint();
    }

    public int getGridSpacing() {
        return gridSpacing;
    }

    public void setGridSpacing(int gridSpacing) {
        this.gridSpacing = gridSpacing;
        repaint();
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
        repaint();
    }

    public boolean isPaintBarRect() {
        return paintBarRect;
    }

    public void setPaintBarRect(boolean paintBarRect) {
        this.paintBarRect = paintBarRect;
    }

    public void setColors(Color barColor, Color barHighlight, Color gridColor,
            Color bgColor) {
        this.barColor = barColor;
        this.barHighlight = barHighlight;
        this.gridColor = gridColor;
        this.bgColor = bgColor;
        this.barGradient = null;
        repaint();
    }

    public void setBarColors(Color barColor, Color barHighlight) {
        if (this.barColor != barColor || this.barHighlight != barHighlight)
            setColors(barColor, barHighlight, PaintUtils.mixColors(barColor,
                    Color.black, 0.5), Color.black);
    }

    private Color[] getBarGradient(int gradSize) {
        if (barGradient == null || barGradient.length != gradSize)
            barGradient = PaintUtils.getGlassGradient(gradSize, barColor,
                    barHighlight);
        return barGradient;
    }

    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }

    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w < 1 || h < 1)
            return;

        // fill with the background color.
        g.setColor(bgColor);
        g.fillRect(0, 0, w, h);
        
        // create a new graphics context that removes DPI-based scaling, so we
        // can perform our remaining operations with pixel precision
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform t = g2.getTransform();
        w = (int) (0.5 + w * t.getScaleX());
        h = (int) (0.5 + h * t.getScaleY());
        g2.scale(1 / t.getScaleX(), 1 / t.getScaleY());
        g2.setStroke(new BasicStroke(1));

        // draw the grid
        g2.setColor(gridColor);
        int space = (int) (gridSpacing * t.getScaleX());
        for (int x = space / 2; x < w; x += space)
            g2.drawLine(x, 0, x, h - 1);
        for (int y = 1; y < h; y += space)
            g2.drawLine(-1, y, w - 1, y);

        // how wide/tall should the bar be, based on the current level?
        int extent = (int) Math.round((vertical ? h : w) * level);
        if (extent == 0 && level > 0)
            extent = 1;

        if (extent > 0) {
            if (vertical) {
                Color[] gradient = getBarGradient(w + 1);
                for (int x = 0; x < gradient.length; x++) {
                    g2.setColor(gradient[x]);
                    g2.drawLine(x - 1, h - extent, x - 1, h - 1);
                }
                g2.setColor(barColor);
                if (paintBarRect)
                    g2.drawRect(0, h - extent, w - 1, extent - 1);
                else if (extent < h)
                    g2.drawLine(-1, h - extent, w - 1, h - extent);
            } else {
                Color[] gradient = getBarGradient(h + 1);
                for (int y = 0; y < gradient.length; y++) {
                    g2.setColor(gradient[y]);
                    g2.drawLine(0, y - 1, extent - 1, y - 1);
                }
                g2.setColor(barColor);
                if (paintBarRect)
                    g2.drawRect(0, 0, extent - 1, h - 1);
                else if (extent < w)
                    g2.drawLine(extent - 1, -1, extent - 1, h - 1);
            }
        }
    }

}
