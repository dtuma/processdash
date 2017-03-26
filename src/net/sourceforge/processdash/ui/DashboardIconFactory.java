// Copyright (C) 2003-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.image4j.codec.ico.ICODecoder;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.icons.TaskOverflowIcon;
import net.sourceforge.processdash.ui.lib.BufferedIcon;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;
import net.sourceforge.processdash.util.FallbackObjectFactory;

public class DashboardIconFactory {

    /** Find an ICO file containing the icons to display for this application,
     * and return the contents of that file as a binary stream.
     * 
     * This method allows a user to customize the icon used by the application
     * via the "window.icon" setting in their configuration file.  The default
     * value of that setting will allow them to customize the icon simply by
     * placing a file called "icon.ico" in their data directory. If that file
     * is present, it will be returned; otherwise, the default icons shipped
     * with the dashboard will be returned.
     */
    public static InputStream getApplicationIconData() {
        // first, we check to see if the user has selected a preferred icon
        // file.  If so, we return that file.
        try {
            String iconFile = Settings.getFile("window.icon");
            if (iconFile != null && iconFile.length() > 0) {
                File f = new File(iconFile);
                if (f.exists())
                    return new FileInputStream(f);
            }
        } catch (IOException e) {}

        // if the user has not overridden the application icon, or the file
        // cannot be read, use the default icons shipped with the dashboard.
        String iconFile = Settings.isPersonalMode() ? "dashicon.ico"
                : "teamicon.ico";
        return DashboardIconFactory.class.getResourceAsStream(iconFile);
    }


    /** Apply the dashboard application icon to the given window. */
    public static void setWindowIcon(Window w) {
        windowIconImageSetter = getWindowIconImageSetter(windowIconImageSetter,
            getApplicationIcons());
        windowIconImageSetter.setWindowIconImage(w);
    }
    private static WindowIconImageSetter windowIconImageSetter = null;





    /** Return an application icon that is as close as possible to the
     * given preferred size.
     * 
     * @param preferredSize the ideal width/height of the icon.
     * @param preferSmaller if no icon exactly matches the given size, should
     *    a smaller image be preferred over a larger one?
     * @return the application image most closely matching the requirements.
     */
    public static Image getApplicationImage(int preferredSize,
            boolean preferSmaller) {
        return selectImageClosestToSize(getApplicationIcons(), preferredSize,
            preferSmaller);
    }

    /** @deprecated */
    public static Image getWindowIconImage() {
        return selectImageClosestToSize(getApplicationIcons(), 32, true);
    }

    private static List<? extends Image> getApplicationIcons() {
        if (APPLICATION_ICONS == null) {
            try {
                APPLICATION_ICONS = ICODecoder.read(getApplicationIconData());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return APPLICATION_ICONS;
    }
    private static List<? extends Image> APPLICATION_ICONS = null;


    public static void setLauncherWindowIcon(Window w) {
        try {
            InputStream iconSrc = DashboardIconFactory.class
                    .getResourceAsStream("launcher.ico");
            List<? extends Image> icons = ICODecoder.read(iconSrc);
            WindowIconImageSetter wiis = getWindowIconImageSetter(null, icons);
            wiis.setWindowIconImage(w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int STD_ICON_HEIGHT = 17;
    private static int STD_ICON_PAD = 1;
    public static void setStandardIconSize(int height) {
        if (height < 17)
            height = 17; // enforce minimum size
        else if (height % 2 == 0)
            height--; // ensure height is odd for clean center lines
        STD_ICON_HEIGHT = height;
        STD_ICON_PAD = height / 9;
    }
    public static int getStandardIconSize() {
        return STD_ICON_HEIGHT;
    }
    public static int getStandardIconPad() {
        return STD_ICON_PAD;
    }


    private static Icon projectIcon = null;
    public static Icon getProjectIcon() {
        if (projectIcon == null) projectIcon = new ProjectIcon(new Color(204, 204, 255));
        return projectIcon;
    }

    public static Icon getTaskIcon() {
        return getTaskIcon(new Color(204, 204, 255));
    }

    public static Icon getTaskIcon(Color c) {
        return new TaskIcon(c);
    }

    public static Icon getHourglassIcon() {
        return new ScalableImageIcon(STD_ICON_HEIGHT,
                DashboardIconFactory.class, "icons/hourglass.png");
    }

    public static Icon getGroupIcon() {
        return loadAndScaleIcon("group", true);
    }

    public static Icon getIndividualIcon() {
        return loadAndScaleIcon("individual", true);
    }

    public static Icon getCheckIcon() {
        Icon result = loadNamedIcon("check", null);
        if (result == null) result = new CheckIcon(true);
        return result;
    }

    public static Icon getLightCheckIcon() {
        return new CheckIcon(false);
    }

    public static Icon getScriptIcon() {
        String name = (STD_ICON_HEIGHT > 18 ? "script19" : "script");
        return loadNamedIcon(name, null);
    }

    private static Icon extLinkIcon = null;
    public static Icon getExternalLinkIcon() {
        return extLinkIcon = loadNamedIcon("externalLink", extLinkIcon);
    }

    public static Icon getHelpIcon(int height) {
        return loadAndScaleIcon("help", height, false);
    }

    public static Icon getTaskOverflowIcon() {
        return new TaskOverflowIcon();
    }

    public static Icon getAddIcon() {
        return new ScalableImageIcon(STD_ICON_HEIGHT,
                DashboardIconFactory.class, "icons/add-128.png",
                "icons/add-17.png");
    }

    public static Icon getAddRolloverIcon() {
        return new ScalableImageIcon(STD_ICON_HEIGHT,
            DashboardIconFactory.class, "icons/add-glow-128.png",
            "icons/add-glow-17.png");
    }

    private static Icon restartRequiredIcon = null;
    public static Icon getRestartRequiredIcon() {
        return restartRequiredIcon = loadNamedIcon("restartIcon", restartRequiredIcon);
    }

    private static Icon loadAndScaleIcon(String name, boolean pad) {
        return loadAndScaleIcon(name, STD_ICON_HEIGHT, pad);
    }

    private static Icon loadAndScaleIcon(String name, int height, boolean pad) {
        ImageIcon big = (ImageIcon) loadNamedIcon(name, null);
        Image bigImage = big.getImage();
        int newHeight = height - (pad ? 2 * STD_ICON_PAD : 0);
        int newWidth = (int) Math.round((double) big.getIconWidth() * newHeight
                / big.getIconHeight());
        Image smallImage = bigImage.getScaledInstance(newWidth, newHeight,
            Image.SCALE_AREA_AVERAGING);
        Icon smallIcon = new ImageIcon(smallImage);
        if (pad)
            smallIcon = new PaddedIcon(smallIcon, STD_ICON_PAD, STD_ICON_PAD,
                    STD_ICON_PAD, STD_ICON_PAD);
        return smallIcon;
    }

    private static Icon loadNamedIcon(String name, Icon current) {
        if (current != null)
            return current;

        // future enhancement: try loading icons out of the Templates/resources
        // directory, to allow for localization of icons.

        // look for the icon in the classpath
        URL iconUrl = DashboardIconFactory.class.getResource(name + ".png");
        if (iconUrl == null) return null;
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(iconUrl));
    }


    /** 
     * From a list of images, select the one that is closest to the preferred
     * size.
     * 
     *
     * 
     * @param images a list of images to choose from.
     * @param preferredSize
     * @return
     */
    public static Image selectImageClosestToSize(
            Collection<? extends Image> images, int preferredSize,
            boolean preferSmaller) {
        // if no images were provided, return null.
        if (images == null || images.isEmpty())
            return null;

        // for our purposes, the images will almost always be loaded in full
        // by the time this method is called.  But it doesn't hurt to make
        // certain that they are ready to go.
        JLabel bogusImageObserver = new JLabel();
        MediaTracker t = new MediaTracker(bogusImageObserver);
        int id = 0;
        for (Image i : images)
            t.addImage(i, id++);
        try {
            t.waitForAll();
        } catch (InterruptedException e) {}

        // keep track of the "too big" image closest to the preferred size
        Image bigResult = null;
        int bigDelta = 1000;
        // keep track of the "too small" image closest to the preferred size
        Image smallResult = null;
        int smallDelta = 1000;
        // iterate over the images looking for the best match.
        for (Image image : images) {
            int size = Math.max(image.getWidth(bogusImageObserver),
                image.getHeight(bogusImageObserver));

            if (size < 0)
                // this image must be broken, since getWidth/getHeight returned
                // -1 even after the MediaTracker said it was completely loaded.
                continue;

            else if (size == preferredSize)
                // we've found a perfect match!  Return it.
                return image;

            else if (size < preferredSize) {
                // this image is too small.  But see if it is closer to the
                // preferred size than the best small image we've seen so far.
                int delta = preferredSize - size;
                if (delta < smallDelta) {
                    smallResult = image;
                    smallDelta = delta;
                }

            } else {
                // this image is too big.  But see if it is closer to the
                // preferred size than the best big image we've seen so far.
                int delta = size - preferredSize;
                if (delta < bigDelta) {
                    bigResult = image;
                    bigDelta = delta;
                }
            }
        }

        if (preferSmaller) {
            return (smallResult != null ? smallResult : bigResult);
        } else {
            return (bigResult != null ? bigResult : smallResult);
        }
    }



    private static class CheckIcon implements Icon {

        private boolean bold;

        protected CheckIcon(boolean bold) {
            this.bold = bold;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x,   y+2, x,   y+6);
            if (bold)
                g.drawLine(x+1, y+2, x+1, y+6);
            g.drawLine(x+1, y+5, x+6, y);
            g.drawLine(x+1, y+6, x+6, y+1);
        }

        public int getIconHeight() {
            return 7;
        }

        public int getIconWidth() {
            return 7;
        }

    }


    /** Icon image representing a project.
    *
    * This draws a large square block.
    */
    private static class ProjectIcon implements Icon {

        Color fillColor, highlight, shadow;

        public ProjectIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = PaintUtils.mixColors(fill, Color.white, 0.3f);
            this.shadow    = PaintUtils.mixColors(fill, Color.black, 0.7f);
        }

        public int getIconWidth() { return 16; }

        public int getIconHeight() { return 18; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(fillColor);
            g.fillRect(x+3,  y+4,  10, 10);

            g.setColor(shadow);
            g.drawLine(x+13, y+4,  x+13, y+14); // right shadow
            g.drawLine(x+3,  y+14, x+13, y+14); // bottom shadow

            g.setColor(highlight);
            g.drawLine(x+2,  y+3,  x+2,  y+14); // left highlight
            g.drawLine(x+2,  y+3,  x+13, y+3); // top highlight

            g.setColor(Color.black);
            g.drawRect(x+1, y+2, 13, 13);
        }
    }

    @SuppressWarnings("unused")
    private static final Color blue = new Color(102, 102, 153);


    /** Generic icon to draw a polygon with 3D edge highlighting.
     */
    private static class PolygonIcon extends BufferedIcon {
        int[] xPoints;
        int[] yPoints;
        Color fillColor;

        protected void doPaint(Graphics g) {
            // fill shape
            g.setColor(fillColor);
            g.fillPolygon(xPoints, yPoints, yPoints.length);

            // draw custom highlights
            doHighlights(g);

            // draw black outline
            g.setColor(Color.black);
            g.drawPolygon(xPoints, yPoints, yPoints.length);
        }

        protected void doHighlights(Graphics g) { }
        protected void drawHighlight(Graphics g, int segment,
                                     int xDelta, int yDelta) {
            int segStart = segment;
            int segEnd = (segment + 1) % xPoints.length;

            g.drawLine(xPoints[segStart] + xDelta,
                       yPoints[segStart] + yDelta,
                       xPoints[segEnd]   + xDelta,
                       yPoints[segEnd]   + yDelta);
        }
    }

    /** Icon image representing a work breakdown structure task.
     *
     * This draws a parallelogram.
     */
    private static class TaskIcon extends PolygonIcon {

        Color highlight, shadow;

        public TaskIcon(Color fill) {
            this.xPoints = new int[] { 0, 5, 15, 10 };
            this.yPoints = new int[] { 14, 1, 1, 14 };
            this.fillColor = fill;
            this.highlight = PaintUtils.mixColors(fill, Color.white, 0.3f);
            this.shadow = PaintUtils.mixColors(fill, Color.black, 0.7f);
            renderIcon(16, 16);
        }

        protected void doHighlights(Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 2, -1, 0);
            drawHighlight(g, 3, 0, -1);

            g.setColor(highlight);
            drawHighlight(g, 0, 1, 0);
            drawHighlight(g, 1, 0, 1);
        }
    }


   @SuppressWarnings("unused")
    private static class BlockArrowIcon implements Icon {

        Color bg, fg;
        private int width, height;
        private int[] xpoints;
        private int[] ypoints;

        public BlockArrowIcon(int width, int height,
                              boolean direction,
                              Color fg, Color bg) {
            this.width = width;
            this.height = height;
            this.fg = fg;
            this.bg = bg;

            int y0 = (direction ? 0 : height-1);
            int d  = (direction ? 1 : -1);
            int baseSize = (width+1)/2;

            xpoints = new int[8];
            ypoints = new int[8];
            xpoints[0] = baseSize/2;  ypoints[0] = y0;
            xpoints[1] = baseSize/2;  ypoints[1] = y0 + baseSize * d;
            xpoints[2] = 0;           ypoints[2] = y0 + baseSize * d;
            xpoints[3] = baseSize-1;  ypoints[3] = y0 + (height-1) * d;
            for (int i = 0;   i < 4;   i++) {
                xpoints[7-i] = width - xpoints[i] - 1;
                ypoints[7-i] = ypoints[i];
            }
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(bg);
            g.fillPolygon(xpoints, ypoints, 8);
            g.setColor(Color.black);
            g.drawPolygon(xpoints, ypoints, 8);
            g.translate(-x, -y);
        }

    }


    private static WindowIconImageSetter getWindowIconImageSetter(
            WindowIconImageSetter current, List<? extends Image> icons) {
        if (current != null)
            return current;

        WindowIconImageSetter result =
                new FallbackObjectFactory<WindowIconImageSetter>(
                    WindowIconImageSetter.class) //
                    .add("WindowIconImageSetter16") //
                    .add("WindowIconImageSetter15") //
                    .get();
        result.init(icons);
        return result;
    }

}
