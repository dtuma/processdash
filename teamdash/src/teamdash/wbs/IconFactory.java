// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.ui.icons.ExternalLinkIcon;
import net.sourceforge.processdash.ui.lib.ColorFilter;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.lib.RecolorableIcon;

import teamdash.wbs.icons.AddColumnIcon;
import teamdash.wbs.icons.BlockArrowIcon;
import teamdash.wbs.icons.CollapseAllIcon;
import teamdash.wbs.icons.CollapseIcon;
import teamdash.wbs.icons.CommonWorkflowsIcon;
import teamdash.wbs.icons.CopyNodeIcon;
import teamdash.wbs.icons.DeleteFilterIcon;
import teamdash.wbs.icons.EnterKeyToggleIcon;
import teamdash.wbs.icons.ExpandAllIcon;
import teamdash.wbs.icons.ExpandIcon;
import teamdash.wbs.icons.ExpansionToggleIcon;
import teamdash.wbs.icons.FilterIcon;
import teamdash.wbs.icons.FolderIcon;
import teamdash.wbs.icons.MilestoneIcon;
import teamdash.wbs.icons.PasteIcon;
import teamdash.wbs.icons.ProxyBucketIcon;
import teamdash.wbs.icons.ProxyTableIcon;
import teamdash.wbs.icons.SortDatesIcon;
import teamdash.wbs.icons.TrashCanIcon;
import teamdash.wbs.icons.UndoIcon;
import teamdash.wbs.icons.WBSImageIcon;
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

    public static RecolorableIcon getProxyTableIcon() {
        return new ProxyTableIcon();
    }

    public static Icon getProxyBucketIcon(int height) {
        return new ProxyBucketIcon(height);
    }

    public static Icon getCopyProxyIcon() {
        return new CopyNodeIcon(getProxyTableIcon(), 10f / 15, 1, 1, 4, 4);
    }

    public static Icon getPasteProxyIcon() {
        return new PasteIcon(getProxyTableIcon(), 10f / 16, 5, 7);
    }

    public static RecolorableIcon getMilestoneIcon() {
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
        return new UndoIcon(DEFAULT_COLOR, false);
    }

    public static Icon getRedoIcon() {
        return new UndoIcon(DEFAULT_COLOR, true);
    }

    public static Icon getPromoteIcon() {
        return new BlockArrowIcon(DEFAULT_COLOR, BlockArrowIcon.LEFT);
    }

    public static Icon getDemoteIcon() {
        return new BlockArrowIcon(DEFAULT_COLOR, BlockArrowIcon.RIGHT);
    }

    public static Icon getMoveUpIcon() {
        return new BlockArrowIcon(DEFAULT_COLOR, BlockArrowIcon.UP);
    }

    public static Icon getMoveDownIcon() {
        return new BlockArrowIcon(DEFAULT_COLOR, BlockArrowIcon.DOWN);
    }

    public static Icon getCutIcon() {
        return CUT_ICON;
    }
    private static Icon CUT_ICON = new WBSImageIcon("cut.png");

    public static Icon getCopyIcon() {
        return new CopyNodeIcon((RecolorableIcon) HierarchyIcons.getTaskIcon(),
                10f / 14, 1, 0.6f, 3f, 3.4f);
    }

    public static Icon getCopyWorkflowIcon() {
        RecolorableIcon i = (RecolorableIcon) HierarchyIcons
                .getWorkflowTaskIcon(DEFAULT_COLOR);
        return new CopyNodeIcon(i, 10f / 14, 0, 0, 4, 4);
    }

    public static Icon getCopyMilestoneIcon() {
        return new CopyNodeIcon(getMilestoneIcon(), 10f / 14, 0, 2, 5, 1);
    }

    public static Icon getPasteIcon() {
        return new PasteIcon((RecolorableIcon) HierarchyIcons.getTaskIcon(),
                10f / 16, 5, 7);
    }

    public static Icon getPasteWorkflowIcon() {
        return new PasteIcon((RecolorableIcon) HierarchyIcons
                .getWorkflowTaskIcon(DEFAULT_COLOR), 10f / 16, 4, 6);
    }

    public static Icon getPasteMilestoneIcon() {
        return new PasteIcon(getMilestoneIcon(), 11f / 16, 5, 5);
    }

    public static Icon getDeleteIcon() {
        return new TrashCanIcon(Color.white);
    }

    public static Icon getFindIcon() {
        return new WBSImageIcon("find.png");
    }

    public static Icon getExternalLinkIcon() {
        return new PaddedIcon(new ExternalLinkIcon(), 2, 4, 4, 2);
    }

    public static Icon getFilterOnIcon() {
        return new FilterIcon(Color.yellow, new Color(255, 150, 0));
    }

    public static Icon getFilterOffIcon() {
        return new FilterIcon(DEFAULT_COLOR, null);
    }

    public static Icon getFilterDeleteIcon() {
        return new DeleteFilterIcon();
    }

    public static Icon getInsertOnEnterIcon() {
        return new EnterKeyToggleIcon(true);
    }

    public static Icon getNoInsertOnEnterIcon() {
        return new EnterKeyToggleIcon(false);
    }

    public static Icon getNewTabIcon() {
        return new WBSImageIcon("tab-new.png");
    }

    public static Icon getAddTabIcon() {
        return new WBSImageIcon("tab-add-128.png", "tab-add-16.png");
    }

    public static Icon getRemoveTabIcon() {
        return new WBSImageIcon("tab-remove.png");
    }

    public static Icon getDuplicateTabIcon() {
        return new WBSImageIcon("tab-duplicate.png");
    }

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

    public static Icon getSaveIcon() {
        return new WBSImageIcon("save-32.png", "save-16.png");
    }

    public static Icon getSaveAsIcon() {
        return new WBSImageIcon("save-as-32.png", "save-as-16.png");
    }

    public static Icon getSaveCopyIcon() {
        return new WBSImageIcon("save-copy.png");
    }

    public static Icon getOpenIcon() {
        return OPEN_ICON;
    }
    private static Icon OPEN_ICON = new FolderIcon(new Color(205, 210, 172));

    public static Icon getRefreshIcon() {
        return new WBSImageIcon("reload.png");
    }

    public static Icon getExcelIcon() {
        return new WBSImageIcon("excel.png");
    }

    public static Icon getLeftArrowIcon() {
        return getPromoteIcon();
    }

    public static Icon getRightArrowIcon() {
        return getDemoteIcon();
    }

    public static Icon getHorizontalArrowIcon(boolean right) {
        return (right ? getRightArrowIcon() : getLeftArrowIcon());
    }

    public static Icon getProcessAssetIcon() {
        return new WBSImageIcon("process-asset-128.png", "process-asset-32.png",
                "process-asset-16.png");
    }

    public static Icon getAcceptChangeIcon() {
        return new WBSImageIcon("accept-128.png", "accept-16.png");
    }

    public static Icon getRejectChangeIcon() {
        return new WBSImageIcon("reject-128.png", "reject-16.png");
    }

    public static Icon getExpandIcon() {
        return new ExpandIcon(DEFAULT_COLOR);
    }

    public static Icon getExpandAllIcon() {
        return new ExpandAllIcon(DEFAULT_COLOR);
    }

    public static Icon getCollapseIcon() {
        return new CollapseIcon(DEFAULT_COLOR);
    }

    public static Icon getCollapseAllIcon() {
        return new CollapseAllIcon(DEFAULT_COLOR);
    }

    public static Icon getAddColumnIcon() {
        return new AddColumnIcon();
    }

    public static Icon getSortDatesIcon() {
        return new SortDatesIcon();
    }

    public static Icon getHelpIcon() {
        return new WBSImageIcon("help.png");
    }

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



    private static class EmptyIcon implements RecolorableIcon {
        int width, height;
        protected EmptyIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        public void paintIcon(Component c, Graphics g, int x, int y) {}
        public RecolorableIcon recolor(RGBImageFilter filter) { return this; }
    }


    /** Icon capable of concatenating several other icons.
     */
    private static class ConcatenatedIcon implements RecolorableIcon {

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

        @Override
        public RecolorableIcon recolor(RGBImageFilter filter) {
            Icon[] newIcons = new Icon[icons.length];
            for (int i = icons.length; i-- > 0;) {
                if (icons[i] instanceof RecolorableIcon)
                    newIcons[i] = ((RecolorableIcon) icons[i]).recolor(filter);
                else
                    newIcons[i] = icons[i];
            }
            return new ConcatenatedIcon(newIcons);
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
                r = r.recolor(ColorFilter.Red);
            if ((modifierFlags & PHANTOM_ICON) > 0)
                r = r.recolor(ColorFilter.Phantom);
            if ((modifierFlags & DISABLED_ICON) > 0)
                r = r.recolor(ColorFilter.Disabled);
            result = r;
            destMap.put(i, result);
        }
        return result;
    }

    public static <T extends AbstractButton> T setDisabledIcon(T b) {
        Icon icon = b.getIcon();
        if (icon != null && !(icon instanceof ImageIcon)) {
            b.setDisabledIcon(getModifiedIcon(icon, DISABLED_ICON));
        }
        return b;
    }

}
