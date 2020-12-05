package teamdash.wbs.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import net.sourceforge.processdash.ui.lib.AbstractRecolorableIcon;

public class RenameIcon extends AbstractRecolorableIcon {

    private Color box, bg, select, caret;

    public RenameIcon() {
        box = new Color(150, 150, 150);
        bg = Color.white;
        select = new Color(128, 128, 255);
        caret = Color.black;
        width = 17;
    }

    @Override
    protected void paintIcon(Graphics2D g2, Shape clip, float scale) {
        // draw the white background rectangle for the text box
        g2.setColor(bg);
        g2.fillRect(0, 4, width, 7);

        // draw a box representing a text selection
        g2.setColor(select);
        g2.fillRect(2, 6, 7, 4);

        // draw the outline around the text box
        g2.setStroke(new BasicStroke(1));
        g2.setColor(box);
        g2.draw(new Rectangle2D.Float(0.5f, 4.5f, width - 1, 7));

        // draw the text insertion caret
        g2.clipRect(9, 0, 7, height);
        g2.setColor(caret);
        g2.draw(new RoundRectangle2D.Float(12.5f, 0.5f, 9, height - 1.5f, 4, 5));
        g2.draw(new RoundRectangle2D.Float(3.5f, 0.5f, 9, height - 1.5f, 4, 5));
    }

}
