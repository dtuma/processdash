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

package net.sourceforge.processdash.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Image;

import javax.swing.Icon;

public class DashboardIconFactory {

    private static Image windowIconImage = null;
    public static Image getWindowIconImage() {
        if (windowIconImage == null)
            windowIconImage = Toolkit.getDefaultToolkit().createImage
                (DashboardIconFactory.class.getResource("icon32.gif"));
        return windowIconImage;
    }

    public static Icon getContinueIcon() {
        Icon result = loadNamedIcon("continue");
        if (result == null) result = new ContinueIcon();
        return result;
    }

    public static Icon getPauseIcon() {
        Icon result = loadNamedIcon("pause");
        if (result == null) result = new PauseIcon();
        return result;
    }

    public static Icon getCheckIcon() {
        Icon result = loadNamedIcon("check");
        if (result == null) result = new CheckIcon();
        return result;
    }

    public static Icon getDefectIcon() {
        Icon result = loadNamedIcon("defect");
        if (result == null) result = new DefectIcon();
        return result;
    }

    public static Icon getDisabledDefectIcon() {
        Icon result = loadNamedIcon("defect-dis");
        if (result == null) result = new DisabledDefectIcon();
        return result;
    }

    public static Icon getScriptIcon() {
        Icon result = loadNamedIcon("script");
        if (result == null) result = new ScriptIcon();
        return result;
    }

    public static Icon getDisabledScriptIcon() {
        Icon result = loadNamedIcon("script-dis");
        if (result == null) result = new DisabledScriptIcon();
        return result;
    }


    private static Icon loadNamedIcon(String name) {
        // TODO: try loading icons out of the Templates/resources directory,
        // to allow for localization of icons.
        return null;
    }

    private static class ContinueIcon implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x, y,    x+12, y+6);
            g.drawLine(x, y+12, x+11, y+7);

            int[] xx = new int[] { x, x,    x+12 };
            int[] yy = new int[] { y, y+12, y+6  };
            g.fillPolygon(xx, yy, 3);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    private static class PauseIcon implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.fillRect(x, y, 5, 13);
            g.fillRect(x+8, y, 5, 13);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }

    }

    private static class CheckIcon implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x,   y+2, x,   y+6);
            g.drawLine(x+1, y+2, x+1, y+6);
            g.drawLine(x+2, y+4, x+6, y);
            g.drawLine(x+2, y+5, x+6, y+1);
        }

        public int getIconHeight() {
            return 7;
        }

        public int getIconWidth() {
            return 7;
        }

    }

    private static class DefectIcon implements Icon {

        private Color blue = new Color(0, 49, 156);
        private Color green = new Color(0, 98, 49);

        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 23;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            // fill body with blue
            g.setColor(blue);
            g.fillOval(x+3, y+2, 19, 9);

            // add green stripes
            g.setColor(green);
            paintStripe(g, x+6, y+3, 7, 1);
            paintStripe(g, x+11, y+3, 7, 1);
            paintStripe(g, x+16, y+4, 5, 0);

            // trace body with black
            g.setColor(Color.black);
            g.drawLine(x+3, y+5, x+3, y+7);
            paintBodySide(g, x, y, 1);
            paintBodySide(g, x, y+12, -1);
            g.drawLine(x+21, y+5, x+21, y+7);
        }

        private void paintStripe(Graphics g, int x, int y, int h, int offs) {
            int xx, yy;
            for (int i = 0;   i < h;   i++) {
                yy = y + i;
                xx = x + ((i + offs) & 1);
                g.drawLine(xx, yy, xx+2, yy);
            }
        }

        protected void paintBodySide(Graphics g, int x, int y, int d) {
            // paint antennae
            g.drawLine(x, y+3*d, x+1, y+3*d);
            g.drawLine(x+2, y+4*d, x+2, y+4*d);

            // paint edge of body
            g.drawLine(x+4, y+4*d, x+4, y+4*d);
            g.drawLine(x+5, y+3*d, x+7, y+3*d);
            g.drawLine(x+8, y+2*d, x+16, y+2*d);
            g.drawLine(x+17, y+3*d, x+19, y+3*d);
            g.drawLine(x+20, y+4*d, x+20, y+4*d);

            // paint legs
            paintLeg(g, x+6,  y+d, 3, d);
            paintLeg(g, x+12, y+d, 4, d);
            paintLeg(g, x+18, y+2*d, 4, d);
        }

        protected void paintLeg(Graphics g, int x, int y, int len, int d) {
            g.drawLine(x-1, y+d, x, y);
            g.drawLine(x+1, y-d, x+len, y-d);
        }
    }


    private static class DisabledDefectIcon extends DefectIcon {

        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 24;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.white);
            paintBody(g, x+1, y+1);
            g.setColor(Color.gray);
            paintBody(g, x, y);
        }

        private void paintBody(Graphics g, int x, int y) {
            g.drawLine(x+3, y+5, x+3, y+6);
            paintBodySide(g, x, y, 1);
            paintBodySide(g, x, y+11, -1);
            g.drawLine(x+21, y+5, x+21, y+6);
        }

    }

    private static class ScriptIcon implements Icon {

        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 10;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.white);
            g.fillRect(x+1, y+1, 9, 12);
            g.setColor(Color.black);
            g.drawRect(x, y, 9, 12);
            for (int i = 1;   i < 6;   i++)
                g.drawLine(x+2, y+2*i, x+7, y+2*i);
        }

    }

    private static class DisabledScriptIcon implements Icon {

        private Color lightGray = new Color(229, 229, 229);

        public int getIconHeight() {
            return 13;
        }

        public int getIconWidth() {
            return 11;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.white);
            paintPage(g, x+1, y+1);
            g.setColor(Color.gray);
            paintPage(g, x, y);
            g.setColor(lightGray);
            g.fillRect(x+3, y+5, 6, 2);
        }
        private void paintPage(Graphics g, int x, int y) {
            g.drawRect(x, y, 9, 11);
            g.drawLine(x+2, y+2, x+7, y+2);
            g.drawLine(x+2, y+4, x+7, y+4);
            g.drawLine(x+2, y+7, x+7, y+7);
            g.drawLine(x+2, y+9, x+7, y+9);
        }

    }
}
