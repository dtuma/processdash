
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.metal.MetalIconFactory;

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
            if (g instanceof Graphics2D) {
                Color col = mixColors(highlight, Color.white, 0.5f);
                GradientPaint grad = new GradientPaint(0, 0, Color.white,
                                                       16, 16, highlight);
                ((Graphics2D) g).setPaint(grad);
                g.fillRect( 3, 1, 9, 14 );
                g.fillRect(12, 4, 2, 11 );

            } else {
                g.setColor(Color.white);
                g.fillRect(4, 2, 9, 12 );

                // Draw highlight
                g.setColor(highlight);
                g.drawLine( 3, 1, 3, bottom - 1 );                  // left
                g.drawLine( 3, 1, right - 6, 1 );                   // top
                g.drawLine( right - 2, 7, right - 2, bottom - 1 );  // right
                g.drawLine( right - 5, 2, right - 3, 4 );           // slant
                g.drawLine( 3, bottom - 1, right - 2, bottom - 1 ); // bottom
            }

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


        }
    }

    private static class PolygonIcon extends BufferedIcon {
        int[] xPoints;
        int[] yPoints;
        Color fillColor;

        protected void doPaint(Component c, Graphics g) {
            // fill shape
            g.setColor(fillColor);
            g.fillPolygon(xPoints, yPoints, yPoints.length);

            // draw custom highlights
            doHighlights(c, g);

            // draw black outline
            g.setColor(Color.black);
            g.drawPolygon(xPoints, yPoints, yPoints.length);
        }

        protected void doHighlights(Component c, Graphics g) { }
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


    public static Icon getTaskIcon(Color fill) {
        return new TaskIcon(fill);
    }
    private static class TaskIcon extends PolygonIcon {

        Color highlight, shadow;

        public TaskIcon(Color fill) {
            this.xPoints = new int[] {  0, 5, 15, 10 };
            this.yPoints = new int[] { 14, 1,  1, 14 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doHighlights(Component c, Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 2, -1,  0);
            drawHighlight(g, 3,  0, -1);


            g.setColor(highlight);
            drawHighlight(g, 0, 1, 0);
            drawHighlight(g, 1, 0, 1);
        }
    }



    public static Icon getPSPTaskIcon(Color fill) {
        return new PSPTaskIcon(fill);
    }
    private static class PSPTaskIcon extends PolygonIcon {

        Color highlight, shadow;

        public PSPTaskIcon(Color fill) {
            this.xPoints = new int[] { 7, 0,  3, 12, 15, 8 };
            this.yPoints = new int[] { 1, 7, 15, 15,  7, 1 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }


        protected void doHighlights(Component c, Graphics g) {
            g.setColor(Color.white);
            drawHighlight(g, 1,  1, 0); // bottom left highlight
            drawHighlight(g, 4,  0, 1); // top right highlight

            g.setColor(highlight);
            drawHighlight(g, 0,  0, 1); // top left highlight
            drawHighlight(g, 0,  1, 1); // top left highlight

            g.setColor(shadow);
            drawHighlight(g, 2,  0, -1); // bottom shadow
            drawHighlight(g, 3, -1,  0); // right shadow
        }
        /*
        protected void olddoPaint(Component c, Graphics g) {
            int[] xPoints = new int[] { 7, 0,  3, 12, 15, 8 };
            int[] yPoints = new int[] { 1, 7, 15, 15,  7, 1 };

            g.setColor(fillColor);
            g.fillPolygon(xPoints, yPoints, yPoints.length);

            g.setColor(Color.white);
            // bottom left highlight
            g.drawLine(xPoints[1]+1, yPoints[1], xPoints[2]+1, yPoints[2]);
            // top right highlight
            g.drawLine(xPoints[4], yPoints[4]+1, xPoints[5], yPoints[5]+1);

            g.setColor(highlight);
            // top highlight
            g.drawLine(xPoints[0], yPoints[0]+1, xPoints[1], yPoints[1]+1);
            g.drawLine(xPoints[0]+1, yPoints[0]+1, xPoints[1]+1, yPoints[1]+1);

            g.setColor(shadow);
            // bottom shadow
            g.drawLine(xPoints[2], yPoints[2]-1, xPoints[3], yPoints[3]-1);
            // right shadow
            g.drawLine(xPoints[3]-1, yPoints[3], xPoints[4]-1, yPoints[4]);

            // draw black outline
            g.setColor(Color.black);
            g.drawPolygon(xPoints, yPoints, yPoints.length);
        }
        */
    }

    private static Icon getIcon(String name) {
        return new ImageIcon(IconFactory.class.getResource(name));
    }

    public static Icon getUndoIcon() {
        if (UNDO_ICON == null) UNDO_ICON = getIcon("undo.png");
        return UNDO_ICON;
    }
    public static Icon UNDO_ICON = null;

    public static Icon getRedoIcon() {
        if (REDO_ICON == null) REDO_ICON = getIcon("redo.png");
        return REDO_ICON;
    }
    public static Icon REDO_ICON = null;

    public static Icon getPromoteIcon() {
        if (PROMOTE_ICON == null) PROMOTE_ICON = getIcon("promote.png");
        return PROMOTE_ICON;
    }
    public static Icon PROMOTE_ICON = null;

    public static Icon getDemoteIcon() {
        if (DEMOTE_ICON == null) DEMOTE_ICON = getIcon("demote.png");
        return DEMOTE_ICON;
    }
    public static Icon DEMOTE_ICON = null;

    public static Icon getCutIcon() {
        if (CUT_ICON == null) CUT_ICON = getIcon("cut.gif");
        return CUT_ICON;
    }
    public static Icon CUT_ICON = null;

    public static Icon getCopyIcon() {
        if (COPY_ICON == null) COPY_ICON = getIcon("copy.png");
        return COPY_ICON;
    }
    public static Icon COPY_ICON = null;

    public static Icon getPasteIcon() {
        if (PASTE_ICON == null) PASTE_ICON = getIcon("paste.png");
        return PASTE_ICON;
    }
    public static Icon PASTE_ICON = null;

    public static Icon getDeleteIcon() {
        if (DELETE_ICON == null) DELETE_ICON = getIcon("delete.png");
        return DELETE_ICON;
    }
    public static Icon DELETE_ICON = null;



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

            return alpha | (red << 16) | (green << 8) | blue;
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
