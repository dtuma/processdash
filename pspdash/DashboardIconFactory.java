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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Image;
import java.io.File;
import java.net.URL;

import javax.swing.Icon;

public class DashboardIconFactory {

    private static Image windowIconImage = null;
    public static Image getWindowIconImage() {
        if (windowIconImage == null) {
            windowIconImage = loadFileImage("icon32.gif");
            if (windowIconImage == null)
                windowIconImage = Toolkit.getDefaultToolkit().createImage
                    (DashboardIconFactory.class.getResource("icon32.gif"));
        }
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

    private static Icon smallDownArrowIcon = null;
    public static Icon getSmallDownArrowIcon() {
        if (smallDownArrowIcon == null)
            smallDownArrowIcon = new SmallDownArrow();
        return smallDownArrowIcon;
    }

    private static Icon smallDisDownArrowIcon = null;
    public static Icon getSmallDisabledDownArrowIcon() {
        if (smallDisDownArrowIcon == null)
            smallDisDownArrowIcon = new SmallDisabledDownArrow();
        return smallDisDownArrowIcon;
    }

    private static Icon loadNamedIcon(String name) {
        // TODO: try loading icons out of the Templates/resources directory,
        // to allow for localization of icons.
        return null;
    }

    private static Image loadFileImage(String filename) {
        try {
            File f = new File(filename);
            if (!f.isFile() || !f.canRead())
                return null;

            return Toolkit.getDefaultToolkit().createImage(filename);

        } catch (Exception e) {
            return null;
        }
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

    private static class SmallDownArrow implements Icon {

        Color arrowColor = Color.black;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(arrowColor);
            g.drawLine(x, y, x+4, y);
            g.drawLine(x+1, y+1, x+3, y+1);
            g.drawLine(x+2, y+2, x+2, y+2);
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 4;
        }

    }

    private static class SmallDisabledDownArrow extends SmallDownArrow {

        public SmallDisabledDownArrow() {
            arrowColor = new Color(140, 140, 140);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(Color.white);
            g.drawLine(x+3, y+2, x+4, y+1);
            g.drawLine(x+3, y+3, x+5, y+1);
        }
    }

}
