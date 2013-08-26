// Copyright (C) 2003-2013 Tuma Solutions, LLC
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
import net.sourceforge.processdash.ui.lib.PaintUtils;
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
        return DashboardIconFactory.class.getResourceAsStream("dashicon.ico");
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


    private static Icon projectIcon = null;
    public static Icon getProjectIcon() {
        if (projectIcon == null) projectIcon = new ProjectIcon(new Color(204, 204, 255));
        return projectIcon;
    }

    private static Icon timingIcon = null;
    public static Icon getCompactTimingIcon() {
        return timingIcon = loadNamedIcon("timingCompact", timingIcon);
    }

    private static Icon timingDisabledIcon = null;
    public static Icon getCompactTimingDisabledIcon() {
        return timingDisabledIcon = loadNamedIcon("timingDisabledCompact",
            timingDisabledIcon);
    }

    private static Icon pausedIcon = null;
    public static Icon getCompactPausedIcon() {
        return pausedIcon = loadNamedIcon("pausedCompact", pausedIcon);
    }

    private static Icon pauseDisabled = null;
    public static Icon getPauseDisabledIcon() {
        return pauseDisabled = loadNamedIcon("pauseDisabled", pauseDisabled);
    }

    private static Icon pauseBlack = null;
    public static Icon getPauseBlackIcon() {
        return pauseBlack = loadNamedIcon("pauseBlack", pauseBlack);
    }

    private static Icon pauseGlowing = null;
    public static Icon getPauseGlowingIcon() {
        return pauseGlowing = loadNamedIcon("pauseGlowing", pauseGlowing);
    }

    private static Icon playDisabled = null;
    public static Icon getPlayDisabledIcon() {
        return playDisabled = loadNamedIcon("playDisabled", playDisabled);
    }

    private static Icon playBlack = null;
    public static Icon getPlayBlackIcon() {
        return playBlack = loadNamedIcon("playBlack", playBlack);
    }

    private static Icon playGlowing = null;
    public static Icon getPlayGlowingIcon() {
        return playGlowing = loadNamedIcon("playGlowing", playGlowing);
    }

    public static Icon getCheckIcon() {
        Icon result = loadNamedIcon("check", null);
        if (result == null) result = new CheckIcon();
        return result;
    }

    public static Icon getDefectIcon() {
        Icon result = loadNamedIcon("defect", null);
        if (result == null) result = new DefectIcon();
        return result;
    }

    public static Icon getDisabledDefectIcon() {
        Icon result = loadNamedIcon("defect-dis", null);
        if (result == null) result = new DisabledDefectIcon();
        return result;
    }

    public static Icon getScriptIcon() {
        Icon result = loadNamedIcon("script", null);
        if (result == null) result = new ScriptIcon();
        return result;
    }

    public static Icon getDisabledScriptIcon() {
        Icon result = loadNamedIcon("script-dis", null);
        if (result == null) result = new DisabledScriptIcon();
        return result;
    }

    private static Icon commentIcon = null;
    public static Icon getCommentIcon() {
        return commentIcon = loadNamedIcon("comment", commentIcon);
    }

    private static Icon noCommentIcon = null;
    public static Icon getNoCommentIcon() {
        return noCommentIcon = loadNamedIcon("commentNone", noCommentIcon);
    }
    public static Icon getWhiteCommentIcon() {
        return getNoCommentIcon();
    }

    private static Icon commentErrorIcon = null;
    public static Icon getCommentErrorIcon() {
        return commentErrorIcon = loadNamedIcon("commentError",
            commentErrorIcon);
    }

    private static Icon restartRequiredIcon = null;
    public static Icon getRestartRequiredIcon() {
        return restartRequiredIcon = loadNamedIcon("restartIcon", restartRequiredIcon);
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
            g.fillRect(x+4, y+4, 17, 5);
            g.drawLine(x+9, y+3, x+17, y+3);
            g.drawLine(x+9, y+9, x+17, y+9);

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
