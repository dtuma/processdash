// Copyright (C) 2002-2015 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Factory for icons used by the WBSEditor and its components.
 */
public class IconFactory {

    private static final Color DEFAULT_COLOR = new Color(204, 204, 255);

    // This class is a singleton.
    private IconFactory() {}



    // Icons to depict various type of nodes in the work breakdown structure

    public static Icon getProjectIcon() {
        return new ProjectIcon(DEFAULT_COLOR);
    }

    public static Icon getCommonWorkflowsIcon() {
        return new CommonWorkflowsIcon(DEFAULT_COLOR);
    }

    public static Icon getWorkflowIcon() {
        return new WorkflowIcon(DEFAULT_COLOR);
    }

    public static Icon getWorkflowTaskIcon(Color fill) {
        return new WorkflowTaskIcon(fill);
    }

    public static Icon getComponentIcon() {
        return new ComponentIcon(DEFAULT_COLOR);
    }

    public static Icon getComponentIcon(Color fill) {
        return new ComponentIcon(fill);
    }

    public static Icon getSoftwareComponentIcon() {
        return new SoftwareComponentIcon(DEFAULT_COLOR);
    }

    public static Icon getDocumentIcon(Color highlight) {
        return new DocumentIcon(highlight);
    }

    public static Icon getTaskIcon(Color fill) {
        return new TaskIcon(fill);
    }

    public static Icon getPSPTaskIcon(Color fill) {
        return new PSPTaskIcon(fill);
    }

    public static Icon getProbeTaskIcon() {
        return new ProbeTaskIcon();
    }

    public static Icon getProxyListIcon() {
        return new CommonWorkflowsIcon(DEFAULT_COLOR);
    }

    public static Icon getProxyTableIcon() {
        return loadIconResource("proxy-table.png");
    }

    public static Icon getProxyBucketIcon(int height) {
        return new ProxyBucketIcon(height, new Color(230, 173, 124));
    }

    public static Icon getCopyProxyIcon() {
        return loadIconResource("proxy-copy.png");
    }

    public static Icon getPasteProxyIcon() {
        return loadIconResource("proxy-paste.png");
    }

    public static Icon getMilestoneIcon() {
        return new MilestoneIcon(DEFAULT_COLOR);
    }

    public static Icon getPlusIcon() {
        return PLUS_ICON;
    }
    private static final Icon PLUS_ICON = new PlusIcon();

    public static Icon getMinusIcon() {
        return MINUS_ICON;
    }
    private static final Icon MINUS_ICON = new MinusIcon();

    public static Icon getEmptyIcon(int width, int height) {
        return new EmptyIcon(width, height);
    }



    // Icons used in toolbars and menus

    public static Icon getUndoIcon() {
        if (UNDO_ICON == null) UNDO_ICON = loadIconResource("undo.png");
        return UNDO_ICON;
    }
    private static Icon UNDO_ICON = null;

    public static Icon getRedoIcon() {
        if (REDO_ICON == null) REDO_ICON = loadIconResource("redo.png");
        return REDO_ICON;
    }
    private static Icon REDO_ICON = null;

    public static Icon getPromoteIcon() {
        if (PROMOTE_ICON == null) PROMOTE_ICON = loadIconResource("promote.png");
        return PROMOTE_ICON;
    }
    private static Icon PROMOTE_ICON = null;

    public static Icon getDemoteIcon() {
        if (DEMOTE_ICON == null) DEMOTE_ICON = loadIconResource("demote.png");
        return DEMOTE_ICON;
    }
    private static Icon DEMOTE_ICON = null;

    public static Icon getMoveUpIcon() {
        if (MOVE_UP_ICON == null) MOVE_UP_ICON = loadIconResource("moveup.png");
        return MOVE_UP_ICON;
    }
    private static Icon MOVE_UP_ICON = null;

    public static Icon getMoveDownIcon() {
        if (MOVE_DOWN_ICON == null) MOVE_DOWN_ICON = loadIconResource("movedown.png");
        return MOVE_DOWN_ICON;
    }
    private static Icon MOVE_DOWN_ICON = null;

    public static Icon getCutIcon() {
        if (CUT_ICON == null) CUT_ICON = loadIconResource("cut.gif");
        return CUT_ICON;
    }
    private static Icon CUT_ICON = null;

    public static Icon getCopyIcon() {
        if (COPY_ICON == null) COPY_ICON = loadIconResource("copy.png");
        return COPY_ICON;
    }
    private static Icon COPY_ICON = null;

    public static Icon getCopyMilestoneIcon() {
        if (COPY_MS_ICON == null) COPY_MS_ICON = loadIconResource("copyms.png");
        return COPY_MS_ICON;
    }
    private static Icon COPY_MS_ICON = null;

    public static Icon getPasteIcon() {
        if (PASTE_ICON == null) PASTE_ICON = loadIconResource("paste.png");
        return PASTE_ICON;
    }
    private static Icon PASTE_ICON = null;

    public static Icon getPasteMilestoneIcon() {
        if (PASTE_MS_ICON == null) PASTE_MS_ICON = loadIconResource("pastems.png");
        return PASTE_MS_ICON;
    }
    private static Icon PASTE_MS_ICON = null;

    public static Icon getDeleteIcon() {
        if (DELETE_ICON == null) DELETE_ICON = loadIconResource("delete.png");
        return DELETE_ICON;
    }
    private static Icon DELETE_ICON = null;

    public static Icon getFindIcon() {
        if (FIND_ICON == null) FIND_ICON = loadIconResource("find.png");
        return FIND_ICON;
    }
    private static Icon FIND_ICON = null;

    public static Icon getFilterOnIcon() {
        if (FILTER_ON_ICON == null) FILTER_ON_ICON = loadIconResource("filter-on.png");
        return FILTER_ON_ICON;
    }
    private static Icon FILTER_ON_ICON = null;

    public static Icon getFilterOffIcon() {
        if (FILTER_OFF_ICON == null) FILTER_OFF_ICON = loadIconResource("filter-off.png");
        return FILTER_OFF_ICON;
    }
    private static Icon FILTER_OFF_ICON = null;

    public static Icon getFilterDeleteIcon() {
        if (FILTER_DELETE_ICON == null) FILTER_DELETE_ICON = loadIconResource("filter-delete.png");
        return FILTER_DELETE_ICON;
    }
    private static Icon FILTER_DELETE_ICON = null;

    public static Icon getInsertOnEnterIcon() {
        if (INSERT_ON_ENTER_ICON == null)
            INSERT_ON_ENTER_ICON = loadIconResource("enter-key-insert-on.png");
        return INSERT_ON_ENTER_ICON;
    }
    private static Icon INSERT_ON_ENTER_ICON = null;

    public static Icon getNoInsertOnEnterIcon() {
        if (NO_INSERT_ON_ENTER_ICON == null)
            NO_INSERT_ON_ENTER_ICON = loadIconResource("enter-key-insert-off.png");
        return NO_INSERT_ON_ENTER_ICON;
    }
    private static Icon NO_INSERT_ON_ENTER_ICON = null;

    public static Icon getNewTabIcon() {
        if (NEW_TAB_ICON == null) NEW_TAB_ICON = loadIconResource("tab-new.png");
        return NEW_TAB_ICON;
    }
    private static Icon NEW_TAB_ICON = null;

    public static Icon getAddTabIcon() {
        if (ADD_TAB_ICON == null) ADD_TAB_ICON = loadIconResource("tab-add.png");
        return ADD_TAB_ICON;
    }
    private static Icon ADD_TAB_ICON = null;

    public static Icon getRemoveTabIcon() {
        if (REMOVE_TAB_ICON == null) REMOVE_TAB_ICON = loadIconResource("tab-remove.png");
        return REMOVE_TAB_ICON;
    }
    private static Icon REMOVE_TAB_ICON = null;

    public static Icon getDuplicateTabIcon() {
        if (DUPLICATE_TAB_ICON == null) DUPLICATE_TAB_ICON = loadIconResource("tab-duplicate.png");
        return DUPLICATE_TAB_ICON;
    }
    private static Icon DUPLICATE_TAB_ICON = null;

    public static Icon getImportWorkflowsIcon() {
        if (IMPORT_WORKFLOWS_ICON == null) {
            IMPORT_WORKFLOWS_ICON = new ConcatenatedIcon(getWorkflowIcon(),
                    new EmptyIcon(2, 1), getLeftArrowIcon(),
                    new EmptyIcon(1, 1), getOpenIcon());
        }
        return IMPORT_WORKFLOWS_ICON;
    }
    private static Icon IMPORT_WORKFLOWS_ICON = null;

    public static Icon getExportWorkflowsIcon() {
        if (EXPORT_WORKFLOWS_ICON == null) {
            EXPORT_WORKFLOWS_ICON = new ConcatenatedIcon(getWorkflowIcon(),
                    new EmptyIcon(2, 1), getRightArrowIcon(),
                    new EmptyIcon(1, 1), getOpenIcon());
        }
        return EXPORT_WORKFLOWS_ICON;
    }
    private static Icon EXPORT_WORKFLOWS_ICON = null;

    public static Icon getImportProxiesIcon() {
        if (IMPORT_PROXIES_ICON == null) {
            IMPORT_PROXIES_ICON = new ConcatenatedIcon(getProxyTableIcon(),
                    new EmptyIcon(2, 1), getLeftArrowIcon(),
                    new EmptyIcon(1, 1), getOpenIcon());
        }
        return IMPORT_PROXIES_ICON;
    }
    private static Icon IMPORT_PROXIES_ICON = null;

    public static Icon getExportProxiesIcon() {
        if (EXPORT_PROXIES_ICON == null) {
            EXPORT_PROXIES_ICON = new ConcatenatedIcon(getProxyTableIcon(),
                    new EmptyIcon(2, 1), getRightArrowIcon(),
                    new EmptyIcon(1, 1), getOpenIcon());
        }
        return EXPORT_PROXIES_ICON;
    }
    private static Icon EXPORT_PROXIES_ICON = null;

    public static Icon getOpenIcon() {
        if (OPEN_ICON == null) OPEN_ICON = loadIconResource("open.png");
        return OPEN_ICON;
    }
    private static Icon OPEN_ICON = null;

    public static Icon getLeftArrowIcon() {
        return getPromoteIcon();
    }
    public static Icon getRightArrowIcon() {
        return getDemoteIcon();
    }

    public static Icon getHorizontalArrowIcon(boolean right) {
        return (right ? getRightArrowIcon() : getLeftArrowIcon());
    }

    public static Icon getAcceptChangeIcon() {
        return loadIconResource("accept.png");
    }

    public static Icon getRejectChangeIcon() {
        return loadIconResource("reject.png");
    }

    public static Icon getExpandIcon() {
        if (EXPAND_ICON == null) EXPAND_ICON = loadIconResource("expand.png");
        return EXPAND_ICON;
    }
    private static Icon EXPAND_ICON = null;

    public static Icon getExpandAllIcon() {
        if (EXPAND_ALL_ICON == null) EXPAND_ALL_ICON = loadIconResource("expand-all.png");
        return EXPAND_ALL_ICON;
    }
    private static Icon EXPAND_ALL_ICON = null;

    public static Icon getCollapseIcon() {
        if (COLLAPSE_ICON == null) COLLAPSE_ICON = loadIconResource("collapse.png");
        return COLLAPSE_ICON;
    }
    private static Icon COLLAPSE_ICON = null;

    public static Icon getColumnsIcon() {
        if (COLUMNS_ICON == null) COLUMNS_ICON = loadIconResource("columns.png");
        return COLUMNS_ICON;
    }
    private static Icon COLUMNS_ICON = null;

    public static Icon getSortDatesIcon() {
        if (SORT_DATES_ICON == null) SORT_DATES_ICON = loadIconResource("sortdates.png");
        return SORT_DATES_ICON;
    }
    private static Icon SORT_DATES_ICON = null;

    public static Icon getHelpIcon() {
        if (HELP_ICON == null) HELP_ICON = loadIconResource("help.png");
        return HELP_ICON;
    }
    private static Icon HELP_ICON = null;

    /** Convenience method for mixing colors.
     * @param r the ratio to use when mixing; must be between 0.0 and 1.0 .
     *   A value of 1.0 would return a color equivalent to color a.
     *   A value of 0.0 would return a color equivalent to color b.
     *   Other values linearly mix a and b together, yielding a color
     *   equivalent to (r * a) + ((1-r) * b)
     */
    public static Color mixColors(Color a, Color b, float r) {
        float s = 1.0f - r;
        return new Color(colorComp(a.getRed()   * r + b.getRed()   * s),
                         colorComp(a.getGreen() * r + b.getGreen() * s),
                         colorComp(a.getBlue()  * r + b.getBlue()  * s));
    }
    private static final float colorComp(float f) {
        return Math.min(1f, Math.max(0f, f / 255f));
    }



    /** Simple class to buffer an icon image for quick repainting.
     */
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
                if (imageG instanceof Graphics2D) {
                    Graphics2D g2 = (Graphics2D) imageG;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                }
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



    /** Icon image representing a project.
     *
     * This draws a large square block.
     */
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



    /**
     * Icon image for the common team workflows root.
     * 
     * This draws four small boxes.
     */
    private static class CommonWorkflowsIcon extends PolygonIcon {

        Color highlight, shadow;

        public CommonWorkflowsIcon(Color fill) {
            this.xPoints = new int[] { 1, 1, 7, 7 };
            this.yPoints = new int[] { 1, 7, 7, 1 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            super.doPaint(c, g);
            g.translate(6, 0);
            super.doPaint(c, g);
            g.translate(0, 6);
            super.doPaint(c, g);
            g.translate(-6, 0);
            super.doPaint(c, g);
            g.translate(0, -6);

            g.setColor(Color.black);
            g.drawLine(7, 0, 7, 0);
            g.drawLine(0, 7, 0, 7);
            g.drawLine(7, 14, 7, 14);
            g.drawLine(14, 7, 14, 7);
        }

        protected void doHighlights(Component c, Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 1,  0, -1);
            drawHighlight(g, 2, -1,  0);

            g.setColor(highlight);
            drawHighlight(g, 0, 1, 0);
            drawHighlight(g, 3, 0, 1);
        }

    }


    /** Icon image representing a defined workflow. */
    private static class WorkflowIcon extends PolygonIcon {

        Color highlight, shadow;

        public WorkflowIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
            this.xPoints = new int[] { 0, 15, 15,  0 };
            this.yPoints = new int[] { 3,  3, 12, 12 };
        }

        @Override
        protected void doHighlights(Component c, Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 1, -1,  0);

            g.setColor(highlight);
            drawHighlight(g, 0,  0, 1);
            drawHighlight(g, 1, -3, 0);
            drawHighlight(g, 3,  4, 0);

            g.setColor(shadow);
            drawHighlight(g, 1, -4,  0);
            drawHighlight(g, 3,  3,  0);
            drawHighlight(g, 2,  0, -1);

            g.setColor(highlight);
            drawHighlight(g, 3, 1, 0);
        }

    }

    /** Icon image representing a task in a defined workflow */
    private static class WorkflowTaskIcon extends WorkflowIcon {

        public WorkflowTaskIcon(Color fill) {
            super(fill);
        }

        @Override
        protected void drawHighlight(Graphics g, int segment, int xDelta,
                int yDelta) {
            // skip the display of the interior highlights
            if (Math.abs(xDelta) < 2)
                super.drawHighlight(g, segment, xDelta, yDelta);
        }

    }



    /** Icon image representing a relative size bucket for proxy estimation.
     * 
     * This draws a vertical ruler with tick marks.
     */
    private static class ProxyBucketIcon extends BufferedIcon {

        private Color bgColor;

        public ProxyBucketIcon(int height, Color bgColor) {
            this.height = height;
            this.bgColor = bgColor;
        }

        protected void doPaint(Component c, Graphics g) {
            g.setColor(bgColor);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.black);
            g.drawLine(0, 0, 0, height - 1);
            g.drawLine(width - 1, 0, width - 1, height - 1);
            g.drawLine(0, height / 2, width / 2, height / 2);
            g.drawLine(0, height / 4, width / 4, height / 4);
            g.drawLine(0, height * 3 / 4, width / 4, height * 3 / 4);
        }

    }



    /** Icon image representing a project component.
     *
     * This draws a square block.
     */
    private static class ComponentIcon extends BufferedIcon {

        Color fillColor, highlight, shadow;

        public ComponentIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            if (g instanceof Graphics2D) {
                GradientPaint grad = new GradientPaint(-10, -10, Color.white,
                                                       16, 16, fillColor);
                ((Graphics2D) g).setPaint(grad);
            } else {
                g.setColor(fillColor);
            }
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



    /** Icon image representing a software component.
     *
     * This draws a floppy disk.
     */
    private static class SoftwareComponentIcon extends BufferedIcon {

        Color highlight;

        public SoftwareComponentIcon(Color highlight) {
            this.highlight = highlight;
        }

        protected void doPaint(Component c, Graphics g) {
            // fill in floppy
            g.setColor(highlight);
            g.fillRect(2,2, 12,12);

            // draw outline
            g.setColor(Color.black);
            g.drawLine( 1, 1, 13, 1);
            g.drawLine(14, 2, 14,14);
            g.drawLine( 1,14, 14,14);
            g.drawLine( 1, 1,  1,14);

            // draw interior lines
            g.setColor(Color.gray);
            g.fillRect(5,2, 6,5);
            g.drawLine(4,8, 11,8);
            g.drawLine(3,9, 3,13);
            g.drawLine(12,9, 12,13);

            // draw white parts
            g.setColor(Color.white);
            g.fillRect(8,3, 2,3);
            g.fillRect(4,9, 8,5);

            // draw text on floppy label
            g.setColor(highlight);
            g.drawLine(5,10, 9,10);
            g.drawLine(5,12, 8,12);
        }
    }



    /** Icon image representing a document
     *
     * This draws a page of paper.
     */
    private static class DocumentIcon extends BufferedIcon {

        Color highlight;

        public DocumentIcon(Color highlight) { this.highlight = highlight; }

        protected void doPaint(Component c, Graphics g) {

            int right = width - 1;
            int bottom = height - 1;

            // Draw fill
            if (g instanceof Graphics2D) {
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



    /** Generic icon to draw a polygon with 3D edge highlighting.
     */
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



    /** Icon image representing a work breakdown structure task.
     *
     * This draws a parallelogram.
     */
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



    /** Icon image representing a PSP task.
     *
     * This draws a pentagon.
     */
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
    }



    /**
     * Icon image representing a PROBE task.
     */
    private static class ProbeTaskIcon extends BufferedIcon {

        protected void doPaint(Component c, Graphics g) {
            int pad = 2, size = 11;
            g.setColor(Color.white);
            g.fillRect(pad, pad, size, size);

            g.setColor(Color.decode("#6b24b3"));
            g.drawLine(pad, pad + size - 2, pad + size, pad);

            g.setColor(Color.green.darker());
            g.fillRect(pad + 4, pad + 2, 2, 2);
            g.fillRect(pad + 4, pad + 8, 2, 2);
            g.fillRect(pad + 9, pad + 4, 2, 2);

            g.setColor(Color.black);
            g.drawRect(pad, pad, size, size);
        }

    }



    /**
     * Icon image representing a milestone.
     */
    private static class MilestoneIcon extends PolygonIcon {

        Color highlight, shadow;

        public MilestoneIcon(Color fill) {
            this.xPoints = new int[] { 7, 14, 7, 0, 7 };
            this.yPoints = new int[] { 1, 8, 15, 8, 1 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doHighlights(Component c, Graphics g) {
            g.setColor(Color.white);
            g.drawLine(1, 8, 7, 2); // top left highlight

            g.setColor(highlight);
            g.drawLine(8, 3, 12, 7); // top right highlight
            g.drawLine(2, 9, 6, 13); // bottom left highlight

            g.setColor(shadow);
            g.drawLine(7, 14, 13, 8); // bottom right shadow
        }

    }

    private static class MinusIcon implements Icon {
        public int getIconWidth()  { return 9; }
        public int getIconHeight() { return 9; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawRect(x, y, 8, 8);          // square box
            g.drawLine(x+2, y+4, x+6, y+4);  // minus symbol
        }
    }

    private static class PlusIcon extends MinusIcon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.drawLine(x+4, y+2, x+4, y+6);  // vertical bar of plus symbol
        }
    }

    private static class EmptyIcon implements Icon {
        int width, height;
        protected EmptyIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        public void paintIcon(Component c, Graphics g, int x, int y) {}
    }


    /** Icon capable of concatenating several other icons.
     */
    private static class ConcatenatedIcon implements Icon {

        private Icon[] icons;
        int width, height;

        public ConcatenatedIcon(Icon... icons) {
            this.icons = icons;
            this.width = this.height = 0;
            for (int i = 0; i < icons.length; i++) {
                this.width += icons[i].getIconWidth();
                this.height = Math.max(this.height, icons[i].getIconHeight());
            }
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            for (int i = 0; i < icons.length; i++) {
                icons[i].paintIcon(c, g, x, y);
                x += icons[i].getIconWidth();
            }
        }

    }




    public static final int PHANTOM_ICON = 1;
    public static final int ERROR_ICON = 2;
    public static final int DISABLED_ICON = 4;

    private static final Map[] MODIFIED_ICONS = new Map[4];
    static {
        MODIFIED_ICONS[0] = new HashMap();
        MODIFIED_ICONS[1] = new HashMap();
        MODIFIED_ICONS[2] = new HashMap();
        MODIFIED_ICONS[3] = new HashMap();
    }

    /** Create a modified version of an icon.
     *
     * @param i the icon to replicate
     * @param modifierFlags a bitwise-or of any collection of the
     * following flags:<ul>
     * <li>{@link #PHANTOM_ICON} to create a whitened-out icon, indicative
     *     of a cut operation
     * <li>{@link #ERROR_ICON} to create a reddened icon, indicative of an
     *     error condition.
     * <li>{@link #DISABLED_ICON} to create a grayed-out icon, indicative of a
     *     disabled action.  (This flag cannot be used in combination with
     *     either of the other two flags.)
     * </ul>
     * @return an icon which looks like the original, with the requested
     *  filters applied. <b>Note:</b> this method will automatically cache
     *  the icons it generates, and return a cached icon when appropriate.
     */
    public static Icon getModifiedIcon(Icon i, int modifierFlags) {
        if (modifierFlags < 1 || modifierFlags > 4)
            return i;

        Map destMap = MODIFIED_ICONS[modifierFlags - 1];
        Icon result = (Icon) destMap.get(i);
        if (result == null) {
            BufferedIcon bufIcon = new BufferedIcon(i);
            if ((modifierFlags & ERROR_ICON) > 0)
                bufIcon.applyFilter(RED_FILTER);
            if ((modifierFlags & PHANTOM_ICON) > 0)
                bufIcon.applyFilter(PHANTOM_FILTER);
            if ((modifierFlags & DISABLED_ICON) > 0)
                bufIcon.applyFilter(GRAY_FILTER);
            result = bufIcon;
            destMap.put(i, result);
        }
        return result;
    }

    // filter for creating "error" icons.  Converts to red monochrome.
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

    // filter for creating "phantom" icons.  Mixes all colors
    // half-and-half with white.
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

    // filter for creating "disabled" icons.
    private static GrayFilter GRAY_FILTER = new GrayFilter(true, 50);



    /** Fetch an icon from a file in the classpath. */
    private static Icon loadIconResource(String name) {
        return new ImageIcon(IconFactory.class.getResource(name));
    }

}
