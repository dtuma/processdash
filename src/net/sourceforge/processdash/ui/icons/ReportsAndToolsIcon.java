// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sourceforge.processdash.ui.lib.PaintUtils;

public class ReportsAndToolsIcon implements Icon {

    private int width, height, bufHeight;

    private ImageIcon[] icons;

    private Image buffered;

    public ReportsAndToolsIcon(int height) {
        this.height = height;
        this.width = (int) Math.ceil(height * 1.4);
        this.icons = new ImageIcon[BITMAP_NAMES.length];
        for (int i = icons.length; i-- > 0;)
            this.icons[i] = loadImage(BITMAP_NAMES[i]);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // create a new graphics context, scaled for pixel precision
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform t = g2.getTransform();
        int pw = (int) (width * t.getScaleX());
        int ph = (int) (height * t.getScaleY());
        g2.translate(x, y);
        g2.scale(1 / t.getScaleX(), 1 / t.getScaleY());

        if (ph < 28) {
            // if one of our prerendered images fits the target size, use it
            ImageIcon fit = getLargestFitIcon(ph);
            int padLeft = (pw - fit.getIconWidth()) / 2;
            int padTop = (ph - fit.getIconHeight()) / 2;
            fit.paintIcon(c, g2, padLeft, padTop);
        } else {
            // use a more complex algorithm for large icon sizes
            paintLargeIcon(g2, pw, ph);
        }
    }

    private ImageIcon getLargestFitIcon(int desiredHeight) {
        for (int i = icons.length; i-- > 0;) {
            if (icons[i].getIconHeight() <= desiredHeight)
                return icons[i];
        }
        return null;
    }

    private void paintLargeIcon(Graphics2D g2, int pw, int ph) {
        if (ph != bufHeight) {
            buffered = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            paintLargeIconImpl((Graphics2D) buffered.getGraphics(), pw, ph);
            bufHeight = ph;
        }

        g2.drawImage(buffered, 0, 0, null);
    }

    private void paintLargeIconImpl(Graphics2D g2, int pw, int ph) {
        // draw a white page with a black outline.
        int pageHeight = ph - 1;
        int pageWidth = ph * 9 / 11;
        g2.setColor(Color.white);
        g2.fillRect(0, 0, pageWidth, pageHeight);
        g2.setColor(Color.gray);
        g2.drawRect(0, 0, pageWidth, pageHeight);

        // draw text onto the page
        int pad = 1 + ph / 20;
        float lineSpacing = Math.max(2, ph / 14);
        float fontSize = 0.85f * lineSpacing;
        float y = pad + lineSpacing;
        g2.setClip(pad + 1, pad, pageWidth - 2 * pad, pageHeight - 2 * pad);
        g2.setFont(new Font("Dialog", Font.PLAIN, 10).deriveFont(fontSize));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.darkGray);
        String s = TEXT;
        while (y <= ph) {
            g2.drawString(s, pad + 1, y);
            s = s.substring(20);
            if (s.charAt(0) == ' ')
                s = s.substring(1);
            y += lineSpacing;
        }

        // calculate the geometry for the chart in the lower-left corner
        int barWidth = pageWidth / 5;
        int chartHeight = (int) (pageHeight * 0.4);
        int chartTop = pageHeight - pad - chartHeight;
        Rectangle2D[] bars = new Rectangle2D[BAR_DELTA.length];
        for (int i = bars.length; i-- > 0;) {
            float barGap = chartHeight * BAR_DELTA[i];
            bars[i] = new Rectangle2D.Float(pad + 1 + barWidth * i,
                    chartTop + barGap, barWidth, chartHeight);
        }

        // draw white areas to ensure the text doesn't run into the bars
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(Math.max(3, 1 + pad * 1.2f)));
        for (int i = bars.length; i-- > 0;)
            g2.draw(bars[i]);

        // draw the bars themselves
        for (int i = bars.length; i-- > 0;) {
            Color light = PaintUtils.mixColors(BAR_COLORS[i], Color.white, 0.7);
            g2.setPaint(new GradientPaint((int) bars[i].getX(), 0,
                    BAR_COLORS[i], (int) bars[i].getMaxX(), 0, light));
            g2.fill(bars[i]);
        }

        // draw the calculator
        ImageIcon calc = loadImage("calc.png");
        int calcHeight = ph * 2 / 3;
        float calcScale = calcHeight / (float) calc.getIconHeight();
        int calcWidth = (int) (0.5 + calc.getIconWidth() * calcScale);
        int calcLeft = (int) (pageWidth - pad * 2 / 3);
        int calcTop = ph / 4;
        Image scaledCalc = new ImageIcon(calc.getImage().getScaledInstance( //
            calcWidth, calcHeight, Image.SCALE_SMOOTH)).getImage();
        g2.setClip(null);
        g2.drawImage(scaledCalc, calcLeft, calcTop, null);
    }

    private ImageIcon loadImage(String name) {
        return new ImageIcon(ReportsAndToolsIcon.class.getResource(name));
    }

    private static final String[] BITMAP_NAMES = { "script-15.png",
            "script-17.png", "script-19.png", "script-21.png", "script-23.png",
            "script-25.png", "script-27.png" };

    private static final String TEXT = "Lorem ipsum dolor sit amet, "
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt "
            + "ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex "
            + "ea commodo consequat. Duis aute irure dolor in reprehenderit "
            + "in voluptate velit esse cillum dolore eu fugiat nulla "
            + "pariatur. Excepteur sint occaecat cupidatat non proident, "
            + "sunt in culpa qui officia deserunt mollit anim id est laborum.";

    private static final float[] BAR_DELTA = { 0.2f, 0f, 0.4f };

    private static final Color[] BAR_COLORS = { new Color(0, 100, 255),
            new Color(22, 130, 0), new Color(200, 100, 50) };

}
