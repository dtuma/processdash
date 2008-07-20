// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.systray;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.log.time.TimeLoggingModel;

/**
 * Manages changes to the image for the dashboard tray icon.
 * 
 * This class registers as a listener for relevant changes in application
 * state, and updates the tray icon's image appropriately.
 * 
 * @author tuma
 */
public class IconImageHandler {

    private TrayIcon trayIcon;

    private TimeLoggingModel timeLoggingModel;

    private Image normalImage;

    private Image timingImage;

    private Image armedImage;

    public IconImageHandler(ProcessDashboard pdash, TrayIcon icon) {
        this.timeLoggingModel = pdash.getTimeLoggingModel();
        this.trayIcon = icon;

        // create the images
        normalImage = trayIcon.getImage();
        timingImage = drawTimingImage(normalImage);
        armedImage = drawArmedImage(normalImage);

        // register for change notification
        PropertyChangeListener pcl = EventHandler.create(
            PropertyChangeListener.class, this, "update");
        timeLoggingModel.addPropertyChangeListener(pcl);

        update();
    }


    public void update() {
        if (timeLoggingModel.isPaused())
            trayIcon.setImage(normalImage);
        else
            trayIcon.setImage(timingImage);
    }

    public void showArmedIcon() {
        trayIcon.setImage(armedImage);
    }



    private Image drawTimingImage(Image baseImage) {
        ImageIcon baseIcon = new ImageIcon(baseImage);
        Image result = new BufferedImage(baseIcon.getIconWidth(), baseIcon
                .getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Component c = new Component() {};
        Graphics2D g = (Graphics2D) result.getGraphics();
        baseIcon.paintIcon(c, g, 0, 0);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.black);
        int x = baseIcon.getIconWidth()/4;
        int dx = baseIcon.getIconWidth()/2 - 2;
        int y = baseIcon.getIconHeight()/2;
        int dy = baseIcon.getIconHeight()/4 - 1;
        int[] xx = new int[] { x, x+dx, x };
        int[] yy = new int[] { y, y+dy, y+2*dy };
        g.fillPolygon(xx, yy, 3);

        return result;
    }


    private Image drawArmedImage(Image baseImage) {
        ImageIcon baseIcon = new ImageIcon(baseImage);
        Image result = new BufferedImage(baseIcon.getIconWidth(), baseIcon
                .getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Component c = new Component() {};
        Graphics2D g = (Graphics2D) result.getGraphics();
        baseIcon.paintIcon(c, g, 0, 0);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.black);
        int x = baseIcon.getIconWidth()/4;
        int dx = baseIcon.getIconWidth()/2 - 2;
        int y = baseIcon.getIconHeight()/2;
        int dy = baseIcon.getIconHeight()/4 - 1;
        g.fillOval(x, y, dx, dy*2);

        return result;
    }

}
