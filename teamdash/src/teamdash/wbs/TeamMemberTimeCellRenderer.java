package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import javax.swing.JTable;

public class TeamMemberTimeCellRenderer extends DataTableCellNumericRenderer {

    private Color theColor;
    private Paint paint;
    private static Paint unselectedPaint = null;
    private static Paint selectedPaint = null;

    public TeamMemberTimeCellRenderer() { }

    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {

        Component result =
            super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);


        if (value instanceof TeamMemberTime)
            theColor = ((TeamMemberTime) value).color;
        else
            theColor = Color.white;


        paint = hasFocus ? null : getPaint(isSelected);

        return result;
    }

    protected Paint getPaint(boolean isSelected) {
        if (isSelected) {
            if (selectedPaint == null)
                selectedPaint = getGradient();
            return selectedPaint;
        } else {
            if (unselectedPaint == null)
                unselectedPaint = getGradient();
            return unselectedPaint;
        }
    }

    /** Construct a Paint that can be used to fade to the background color.
     */
    protected Paint getGradient() {
        Color backgroundColor = getBackground();
        BufferedImage i =
            new BufferedImage(gradientWidth, 1, BufferedImage.TYPE_INT_ARGB);
        WritableRaster alpha = i.getAlphaRaster();
        for (int x = gradientWidth; x-- > 0;) {
            i.setRGB(x, 0, backgroundColor.getRGB());
            double sample = gradientWidth - x;
            sample = sample / gradientWidth;
            sample = (1.0 - sample*sample) * 255;
            alpha.setSample(x, 0, 0, sample);
        }
        // Although a GradientPaint would normally be used for this purpose,
        // I create a custom TexturePaint instead because the redraws are
        // approximately four times faster. This also gives me the flexibility
        // to perform a quadratic fade instead of a linear fade.
        return new TexturePaint(i, new Rectangle(leftBorder, 0, gradientWidth, 1));
    }

    public boolean isOpaque() {
        // overriding this method to return false stops our superclass from
        // painting a solid background over our colored gradient.
        return false;
    }

    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (paint != null && g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(theColor);
            g2.fillRect(0, 0, leftBorder + gradientWidth, getHeight());

            g2.setPaint(paint);
            g2.fillRect(leftBorder, 0, gradientWidth, getHeight());
        }

        super.paint(g);
    }

    private static final int leftBorder = 3;
    private static final int gradientWidth = 50;
}
