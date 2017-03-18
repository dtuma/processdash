// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

import net.sourceforge.processdash.ui.lib.ColorFilter;
import net.sourceforge.processdash.ui.lib.RecolorableIcon;

import teamdash.wbs.icons.CommonWorkflowsIcon;
import teamdash.wbs.icons.ExpansionToggleIcon;
import teamdash.wbs.icons.MilestoneIcon;
import teamdash.wbs.icons.ProxyBucketIcon;
import teamdash.wbs.icons.ProxyTableIcon;
import teamdash.wbs.icons.WorkflowIcon;

/** Factory for icons used by the WBSEditor and its components.
 */
public class IconFactory {

    private static final Color DEFAULT_COLOR = new Color(204, 204, 255);

    // This class is a singleton.
    private IconFactory() {}



    // Icons to depict various type of nodes in the work breakdown structure

    public static Icon getCommonWorkflowsIcon() {
        return new CommonWorkflowsIcon(DEFAULT_COLOR);
    }

    public static Icon getWorkflowIcon() {
        return new WorkflowIcon(DEFAULT_COLOR);
    }

    public static Icon getProxyListIcon() {
        return new CommonWorkflowsIcon(DEFAULT_COLOR);
    }

    public static Icon getProxyTableIcon() {
        return new ProxyTableIcon();
    }

    public static Icon getProxyBucketIcon(int height) {
        return new ProxyBucketIcon(height);
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
    private static final Icon PLUS_ICON = new ExpansionToggleIcon(true);

    public static Icon getMinusIcon() {
        return MINUS_ICON;
    }
    private static final Icon MINUS_ICON = new ExpansionToggleIcon(false);

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

    public static Icon getImportOrgWorkflowsIcon() {
        return new ConcatenatedIcon(getWorkflowIcon(),
                    new EmptyIcon(2, 1), getLeftArrowIcon(),
                    new EmptyIcon(1, 1), getProcessAssetIcon());
    }

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

    public static Icon getImportOrgProxiesIcon() {
        return new ConcatenatedIcon(getProxyTableIcon(),
                    new EmptyIcon(2, 1), getLeftArrowIcon(),
                    new EmptyIcon(1, 1), getProcessAssetIcon());
    }

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
    public static Icon getProcessAssetIcon() {
        return loadIconResource("process-asset.png");
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

    public static Icon getCollapseAllIcon() {
        if (COLLAPSE_ALL_ICON == null) COLLAPSE_ALL_ICON = loadIconResource("collapse-all.png");
        return COLLAPSE_ALL_ICON;
    }
    private static Icon COLLAPSE_ALL_ICON = null;

    public static Icon getColumnsIcon() {
        if (COLUMNS_ICON == null) COLUMNS_ICON = loadIconResource("columns.png");
        return COLUMNS_ICON;
    }
    private static Icon COLUMNS_ICON = null;

    public static Icon getAddColumnIcon() {
        return loadIconResource("column-add.png");
    }

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
    private static class BufferedIcon implements RecolorableIcon {
        protected Image image = null;
        protected int width = 16, height = 16;

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

        public RecolorableIcon recolor(RGBImageFilter filter) {
            ImageProducer prod =
                new FilteredImageSource(image.getSource(), filter);
            this.image = Toolkit.getDefaultToolkit().createImage(prod);
            return this;
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
            RecolorableIcon r;
            if (i instanceof RecolorableIcon)
                r = (RecolorableIcon) i;
            else
                r = new BufferedIcon(i);
            if ((modifierFlags & ERROR_ICON) > 0)
                r = r.recolor(RED_FILTER);
            if ((modifierFlags & PHANTOM_ICON) > 0)
                r = r.recolor(PHANTOM_FILTER);
            if ((modifierFlags & DISABLED_ICON) > 0)
                r = r.recolor(GRAY_FILTER);
            result = r;
            destMap.put(i, result);
        }
        return result;
    }

    // filter for creating "error" icons.  Converts to red monochrome.
    private static RGBImageFilter RED_FILTER = ColorFilter.Red;

    // filter for creating "phantom" icons.  Mixes all colors
    // half-and-half with white.
    private static RGBImageFilter PHANTOM_FILTER = ColorFilter.Phantom;

    // filter for creating "disabled" icons.
    private static GrayFilter GRAY_FILTER = new GrayFilter(true, 50);



    /** Fetch an icon from a file in the classpath. */
    private static Icon loadIconResource(String name) {
        return new ImageIcon(IconFactory.class.getResource(name));
    }

}
