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
import net.sourceforge.processdash.ui.icons.CheckIcon;
import net.sourceforge.processdash.ui.icons.ExternalLinkIcon;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
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


    public static Icon getHourglassIcon() {
        return loadAndScaleIcon("icons/hourglass", true);
    }

    public static Icon getEveryoneIcon() {
        return loadAndScaleIcon("icons/everyone", true);
    }

    public static Icon getGroupIcon() {
        return loadAndScaleIcon("icons/group", true);
    }

    public static Icon getIndividualIcon() {
        return loadAndScaleIcon("icons/individual", true);
    }

    public static Icon getCheckIcon() {
        return new CheckIcon(true);
    }

    public static Icon getLightCheckIcon() {
        return new CheckIcon(false);
    }

    public static Icon getExternalLinkIcon() {
        return new ExternalLinkIcon();
    }

    public static Icon getHelpIcon() {
        return getHelpIcon(STD_ICON_HEIGHT);
    }

    public static Icon getHelpIcon(int height) {
        return loadAndScaleIcon("icons/help", height, false);
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

    public static Icon getRestartRequiredIcon() {
        return loadAndScaleIcon("icons/restart", 16, false);
    }

    private static Icon loadAndScaleIcon(String name, boolean pad) {
        return loadAndScaleIcon(name, STD_ICON_HEIGHT, pad);
    }

    private static Icon loadAndScaleIcon(String name, int height, boolean pad) {
        ImageIcon big = (ImageIcon) loadNamedIcon(name, null);
        int newHeight = height - (pad ? 2 * STD_ICON_PAD : 0);
        Icon smallIcon = new ScalableImageIcon(newHeight, big);
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
