
package teamdash.wbs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.image.WritableRaster;
import java.awt.image.RGBImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.FilteredImageSource;

public class IconFactory {

    private IconFactory() {}

    public static Icon getProjectIcon() {
        return new ProjectIcon(new Color(204, 204, 255));
    }

    public static Icon getSoftwareComponentIcon() {
        return new BufferedIcon(MetalIconFactory.getTreeFloppyDriveIcon());
    }

    public static Icon getDocumentIcon(Color highlight) {
        return new DocumentIcon(highlight);
    }

    private static class BufferedIcon implements Icon {
        protected Image image = null;
        protected int width = 16, height = 16;

        public BufferedIcon() {}

        public BufferedIcon(Icon originalIcon) {
            width = originalIcon.getIconWidth();
            height = originalIcon.getIconHeight();
            image = new BufferedImage(width, height,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics imageG = image.getGraphics();
            originalIcon.paintIcon(null, imageG, 0, 0);
            imageG.dispose();
        }

        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        protected void doPaint(Component c, Graphics g) {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) {
                image = new BufferedImage(getIconWidth(), getIconHeight(),
                                          BufferedImage.TYPE_INT_ARGB);
                Graphics imageG = image.getGraphics();
                doPaint(c,imageG);
                imageG.dispose();
            }
            g.drawImage(image, x, y, null);
        }

        public void applyFilter(RGBImageFilter filter) {
            ImageProducer prod =
                new FilteredImageSource(image.getSource(), filter);
            this.image = Toolkit.getDefaultToolkit().createImage(prod);
        }
    }

    private static class ProjectIcon extends BufferedIcon {

        Color fillColor, highlight, shadow;

        public ProjectIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            g.setColor(fillColor);
            g.fillRect(3,  3,  10, 10);

            g.setColor(shadow);
            g.drawLine(13, 3,  13, 13); // right shadow
            g.drawLine(3,  13, 13, 13); // bottom shadow

            g.setColor(highlight);
            g.drawLine(2,  2,  2, 13); // left highlight
            g.drawLine(2,  2,  13, 2); // top highlight

            g.setColor(Color.black);
            g.drawRect(1, 1, 13, 13);
        }
    }


    private static class DocumentIcon extends BufferedIcon {

        Color highlight;

        public DocumentIcon(Color highlight) { this.highlight = highlight; }

        protected void doPaint(Component c, Graphics g) {

            int right = width - 1;
            int bottom = height - 1;

            // Draw fill
            g.setColor(Color.white);
            g.fillRect(4, 2, 9, 12 );

            // Draw outline
            g.setColor(Color.black);
            g.drawLine( 2, 0, 2, bottom );                 // left
            g.drawLine( 2, 0, right - 4, 0 );              // top
            g.drawLine( 2, bottom, right - 1, bottom );    // bottom
            g.drawLine( right - 1, 6, right - 1, bottom ); // right
            g.drawLine( right - 6, 2, right - 2, 6 );      // slant 1
            g.drawLine( right - 5, 1, right - 4, 1 );      // part of slant 2
            g.drawLine( right - 3, 2, right - 3, 3 );      // part of slant 2
            g.drawLine( right - 2, 4, right - 2, 5 );      // part of slant 2

            // Draw highlight
            g.setColor(highlight);
            g.drawLine( 3, 1, 3, bottom - 1 );                  // left
            g.drawLine( 3, 1, right - 6, 1 );                   // top
            g.drawLine( right - 2, 7, right - 2, bottom - 1 );  // right
            g.drawLine( right - 5, 2, right - 3, 4 );           // slant
            g.drawLine( 3, bottom - 1, right - 2, bottom - 1 ); // bottom
        }
    }


    public static Icon getTaskIcon(Color fill) {
        return new TaskIcon(fill);
    }
    private static class TaskIcon extends BufferedIcon {

        Color fillColor, highlight, shadow;

        public TaskIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            g.setColor(fillColor);
            g.fillRect(4,  2,  8, 12);
            g.drawLine(12, 2, 12,  9);
            g.drawLine(13, 2, 13,  6);
            g.drawLine(2,  9,  2, 13);
            g.drawLine(3,  6,  3, 13);

            g.setColor(shadow);
            g.drawLine(14, 0,  10, 14); // right shadow
            g.drawLine(2,  14, 9,  14); // bottom shadow

            g.setColor(highlight);
            g.drawLine(1,  15, 5,  1); // left highlight
            g.drawLine(6,  1,  14, 1); // top highlight

            g.setColor(Color.black);
            g.drawLine(0, 15,  4,  1); // left side
            g.drawLine(15, 0, 11, 14); // right side
            g.drawLine(5,  0, 15,  0); // top side
            g.drawLine(0, 15, 10, 15); // bottom side
        }
    }

    public static final int PHANTOM_ICON = 1;
    public static final int ERROR_ICON = 2;

    private static final Map[] MODIFIED_ICONS = new Map[3];
    static {
        MODIFIED_ICONS[0] = new HashMap();
        MODIFIED_ICONS[1] = new HashMap();
        MODIFIED_ICONS[2] = new HashMap();
    }

    public static Icon getModifiedIcon(Icon i, int modifierFlags) {
        if (modifierFlags < 1 || modifierFlags > 3)
            return i;

        Map destMap = MODIFIED_ICONS[modifierFlags - 1];
        Icon result = (Icon) destMap.get(i);
        if (result == null) {
            BufferedIcon bufIcon = new BufferedIcon(i);
            if ((modifierFlags & ERROR_ICON) > 0)
                bufIcon.applyFilter(RED_FILTER);
            if ((modifierFlags & PHANTOM_ICON) > 0)
                bufIcon.applyFilter(PHANTOM_FILTER);
            result = bufIcon;
            destMap.put(i, result);
        }
        return result;
    }

    private static RedFilter RED_FILTER = new RedFilter();
    private static class RedFilter extends RGBImageFilter {
        public RedFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)((0.30 * ((rgb >> 16) & 0xff) +
                              0.59 * ((rgb >> 8) & 0xff) +
                              0.11 * (rgb & 0xff)) / 3);

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            return (rgb & 0xffff0000) | (gray << 8) | (gray << 0);
        }
    }

    private static PhantomFilter PHANTOM_FILTER = new PhantomFilter();
    private static class PhantomFilter extends RGBImageFilter {
        public PhantomFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb & 0xff000000;
            int red   = filt((rgb >> 16) & 0xff);
            int green = filt((rgb >> 8)  & 0xff);
            int blue  = filt((rgb >> 0)  & 0xff);

            return alpha | (red << 16) | (blue << 8) | green;
        }
        public int filt(int component) {
            return (component + 0xff) / 2;
        }
    }

    public static Color mixColors(Color a, Color b, float r) {
        float s = 1.0f - r;
        return new Color((a.getRed()   * r + b.getRed()   * s) / 255f,
                         (a.getGreen() * r + b.getGreen() * s) / 255f,
                         (a.getBlue()  * r + b.getBlue()  * s) / 255f);
    }


}
