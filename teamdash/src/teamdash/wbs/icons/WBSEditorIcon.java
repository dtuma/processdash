// Copyright (C) 2019 Tuma Solutions, LLC
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

package teamdash.wbs.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.ui.lib.AbstractPixelAwareRecolorableIcon;

import teamdash.wbs.TeamProcess;

/**
 * Icon for the WBS Editor window
 */
public class WBSEditorIcon extends AbstractPixelAwareRecolorableIcon {

    public WBSEditorIcon() {
        this(32);
    }

    public WBSEditorIcon(int iconSize) {
        this.width = iconSize;
        this.height = iconSize;
    }

    @Override
    protected void paintIcon(Graphics2D g2, int width, int height,
            float ignored) {
        // determine how much of a vertical gap to put between the tasks
        float scale = height / 32f;
        int taskGap = (height < 20 ? -1 : (int) Math.round(scale + 0.55));

        // determine the width and height of the task rectangles
        int taskWidth = width * 18 / 32;
        int taskLeft = width - taskWidth;
        int taskHeight = (height - (2 * taskGap)) / 3;
        if ((height - (2 * taskGap) - (3 * taskHeight)) == 2)
            taskGap++;

        // determine the size of the component block
        int cs = 2 * (taskHeight + taskGap + 1);
        int compTop = (height - cs) / 2;
        int compSize = (2 * taskGap) + (3 * taskHeight) - (2 * compTop);
        float compBevel = (width < 50 ? 2 : 1.5f) * Math.max(scale, 0.8f);

        // draw the component
        paint3DRectangle(g2, 0, compTop, compSize, compSize, compBevel,
            HierarchyIcons.DEFAULT_COLOR);

        // create and draw the three tasks
        for (int i = 3; i-- > 0;)
            paint3DRectangle(g2, taskLeft, (taskHeight + taskGap) * i,
                taskWidth, taskHeight, Math.max(scale, 1), TASK_COLORS[i]);
    }

    private void paint3DRectangle(Graphics2D g2, int x, int y, int width,
            int height, float bevelScale, Color fill) {
        // the painting logic in this method is adapted from the Rectangle3DIcon
        // class. Unfortunately, that logic could not be used directly because
        // rounding errors arose from the overlapping zoom levels.

        // save the original clip
        Shape clip = g2.getClip();

        // compute rectangle geometry
        int rightEdge = x + width - 1;
        int bottomEdge = y + height - 1;
        int bev = (int) (3 * bevelScale) + 1;

        Shape rect = new Rectangle(x, y, width - 1, height - 1);
        Shape topLeft = shape(x, y, rightEdge, y, rightEdge - bev, y + bev, //
            x + bev, bottomEdge - bev, x, bottomEdge);
        Shape bottomRight = shape(rightEdge, bottomEdge, x, bottomEdge, //
            x + bev, bottomEdge - bev, rightEdge - bev, y + bev, rightEdge, y);

        // fill the rectangle
        g2.setColor(fill);
        g2.fill(rect);

        // draw highlights
        g2.setStroke(new BasicStroke(3 * bevelScale));
        g2.setColor(highlight(fill));
        clip(g2, clip, topLeft);
        g2.draw(rect);

        // draw shadows
        g2.setColor(shadow(fill));
        clip(g2, clip, bottomRight);
        g2.draw(rect);

        // draw edge
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.black);
        g2.setClip(clip);
        g2.draw(rect);
    }

    private static final Color[] TASK_COLORS = {
            TeamProcess.getPhaseColor(3, 50), //
            TeamProcess.getPhaseColor(15, 50), //
            TeamProcess.getPhaseColor(32, 50) };



    /**
     * Configure the given window to use the WBS Editor icon.
     */
    public static void setWindowIcon(Window w) {
        try {
            if (WINDOW_ICONS == null)
                WINDOW_ICONS = createWindowIcons();
            w.setIconImages(WINDOW_ICONS);
        } catch (Exception e) {
        }
    }

    private static List<Image> WINDOW_ICONS;

    private static List<Image> createWindowIcons() {
        List<Image> result = new ArrayList<Image>();
        WBSEditorIcon icon = new WBSEditorIcon();
        for (int i = 16; i < 48; i += 2)
            result.add(getIconImage(icon, i));
        for (int i = 48; i < 64; i += 4)
            result.add(getIconImage(icon, i));
        for (int i = 64; i <= 128; i += 16)
            result.add(getIconImage(icon, i));
        return Collections.unmodifiableList(result);
    }

    private static Image getIconImage(WBSEditorIcon icon, int size) {
        BufferedImage img = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        icon.paintIcon(img.createGraphics(), size, size, 1);
        return img;
    }

}
