// Copyright (C) 1999-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;

public class HierarchyNavigator implements TaskNavigationSelector.NavMenuUI,
        ActionListener, PropertyChangeListener {

    private ProcessDashboard dash;

    private DashHierarchy hier;

    private JMenuBar menuBar;

    private ActiveTaskModel activeTaskModel;

    private HierarchicalCompletionStatusCalculator statusCalc;

    private ResizeHandler resizeHandler;

    private JMenu overflowMenu;

    private List<HierMenu> hierMenus;

    private Font[] fonts;

    private Border pathSepBorder;

    private int pathSepWidth;

    private boolean useStrikethrough;


    // constants for use with the fonts[] array

    private static final int PLAIN = 0;

    private static final int SELECTED = 1;

    private static final int COMPLETED = 2;


    public HierarchyNavigator(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model) {
        this.dash = dash;
        this.hier = dash.getHierarchy();
        this.menuBar = menuBar;
        this.activeTaskModel = model;
        this.statusCalc = new HierarchicalCompletionStatusCalculator(
                dash.getData(), dash.getHierarchy(), PropertyKey.ROOT);
        this.resizeHandler = new ResizeHandler();
        this.overflowMenu = menuBar.getMenu(0);
        this.hierMenus = new ArrayList<HierMenu>();
        this.fonts = createFonts();
        this.pathSepBorder = BorderFactory.createEmptyBorder(0, 1, 0, 2);
        this.pathSepWidth = getPathSep().getPreferredSize().width;
        this.useStrikethrough = Settings.getBool(STRIKETHROUGH_SETTING, false);

        rebuildMenus();

        statusCalc.addActionListener(this);
        menuBar.addComponentListener(resizeHandler);
        InternalSettings.addPropertyChangeListener(STRIKETHROUGH_SETTING, this);
    }

    public void hierarchyChanged() {
        statusCalc.reloadHierarchy();
        if (hierMenus.get(0).updateToMatchHierarchy())
            relayoutMenus();
        hierMenus.get(0).updateCompletionCheckmarks();
    }

    public void activeTaskChanged() {
        if (hierMenus.get(0).updateToMatchActiveTask())
            relayoutMenus();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                useStrikethrough = Settings.getBool(STRIKETHROUGH_SETTING,
                    false);
                if (!hierMenus.isEmpty())
                    hierMenus.get(0).updateCompletionCheckmarks();
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == statusCalc)
            hierMenus.get(0).updateCompletionCheckmarks();
    }

    public boolean selectNext() {
        return hierMenus.get(0).selectNext() == SelectNextResult.HANDLED;
    }

    public void delete() {
        menuBar.removeComponentListener(resizeHandler);
        statusCalc.removeActionListener(this);
        statusCalc.dispose();
        InternalSettings.removePropertyChangeListener(STRIKETHROUGH_SETTING,
            this);
        hierMenus.get(0).dispose();
        hierMenus.clear();
    }

    private Font[] createFonts() {
        Font[] fonts = new Font[4];
        Font base = new JLabel().getFont();
        fonts[PLAIN] = base.deriveFont(Font.PLAIN);
        fonts[SELECTED] = base.deriveFont(Font.BOLD);
        Map strike = Collections.singletonMap(TextAttribute.STRIKETHROUGH,
            TextAttribute.STRIKETHROUGH_ON);
        fonts[COMPLETED] = fonts[PLAIN].deriveFont(strike);
        fonts[SELECTED + COMPLETED] = fonts[SELECTED].deriveFont(strike);
        return fonts;
    }

    private void rebuildMenus() {
        if (!hierMenus.isEmpty())
            hierMenus.get(0).dispose();
        hierMenus.clear();
        new HierMenu(PropertyKey.ROOT);
        relayoutMenus();
    }

    private void relayoutMenus() {
        menuBar.removeAll();
        menuBar.add(overflowMenu);
        menuBar.add(Box.createHorizontalStrut(4));

        int remainingWidth = menuBar.getWidth() - 4
                - overflowMenu.getPreferredSize().width;
        boolean needsSeparator = false;
        int pos = hierMenus.size() - 1;
        while (pos-- > 0) {
            if (needsSeparator)
                remainingWidth -= pathSepWidth;

            if (remainingWidth < NarrowJMenu.MIN_WIDTH)
                break;

            HierMenu oneMenu = hierMenus.get(pos);
            if (needsSeparator)
                menuBar.add(getPathSep(), 1);
            menuBar.add(oneMenu, 1);

            int oneMenuWidth = oneMenu.getPreferredSize().width;
            oneMenu.setNarrowingEnabled(remainingWidth < oneMenuWidth);
            remainingWidth -= oneMenuWidth;
            needsSeparator = true;
        }

        overflowMenu.setToolTipText(pos < 0 ? null : //
                hierMenus.get(pos).getPrettyPath());
        while (overflowMenu.getItem(0) instanceof HierMenu)
            overflowMenu.remove(0);
        for (int i = 0; i <= pos; i++) {
            HierMenu oneMenu = hierMenus.get(i);
            oneMenu.setNarrowingEnabled(false);
            overflowMenu.add(oneMenu, 0);
        }
        menuBar.validate();
        menuBar.repaint();
    }

    private JLabel getPathSep() {
        JLabel result = new JLabel("/");
        result.setFont(fonts[PLAIN]);
        result.setBorder(pathSepBorder);
        return result;
    }


    private class ResizeHandler extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            relayoutMenus();
        }
    }


    private class HierMenu extends NarrowJMenu {

        PropertyKey node;

        String nodePrettyPath;

        HierMenu nextMenu;

        List<HierMenuItem> allItems;

        HierMenuItem selectedItem;

        public HierMenu(PropertyKey node) {
            this.node = node;
            String path = node.path();
            if (path.length() > 0)
                nodePrettyPath = TaskNavigationSelector.prettifyPath(path
                        .substring(1));

            setIconTextGap(1);
            setNarrowingEnabled(false);
            ToolTipTimingCustomizer.INSTANCE.install(this);
            hierMenus.add(this);
            addMenuItemsForChildren();
            if (allItems.isEmpty()) {
                activeTaskModel.setNode(node);
                relayoutMenus();
            }
        }

        @Override
        protected String getTextToDisplay(String menuText, String fitLayoutText) {
            setToolTipText(getPrettyPath());
            return fitLayoutText;
        }

        private String getPrettyPath() {
            return (nextMenu == null ? null : nextMenu.nodePrettyPath);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension result = super.getMinimumSize();

            // only the first menu in our navigator is narrowable. Here we tell
            // Swing that it can be shrunk down to zero width. In reality, that
            // would be accomplished by removing it from the menu bar; but Swing
            // needs to hear this so it knows it can let the parent menu bar
            // shrink beyond the size requirements of this narrowed menu.
            if (isNarrowingEnabled())
                result.width = 0;

            return result;
        }

        private void dispose() {
            if (nextMenu != null) {
                nextMenu.dispose();
                nextMenu = null;
            }
            hierMenus.remove(this);
            ToolTipTimingCustomizer.INSTANCE.uninstall(this);
        }

        private void addMenuItemsForChildren() {
            JMenu destMenu = this;
            int maxItemsPerMenu = Settings.getInt("hierarchyMenu.maxItems", 20);
            int numChildren = hier.getNumChildren(node);
            int selectedChild = hier.getSelectedChild(node);
            HierMenuItem selectedItem = null;
            allItems = new ArrayList<HierarchyNavigator.HierMenuItem>();

            for (int i = 0; i < numChildren; i++) {
                PropertyKey key = hier.getChildKey(node, i);
                HierMenuItem menuItem = new HierMenuItem(this, key);
                if (i == selectedChild)
                    selectedItem = menuItem;
                allItems.add(menuItem);

                if (destMenu.getItemCount() + 1 >= maxItemsPerMenu) {
                    JMenu moreMenu = new HierMoreSubmenu();
                    destMenu.insert(moreMenu, 0);
                    destMenu.insertSeparator(1);
                    destMenu = moreMenu;
                }

                destMenu.add(menuItem);
            }

            setSelectedItem(selectedItem);
        }

        private boolean setSelectedItem(HierMenuItem item) {
            // no change in selection? do nothing
            if (item == null || item == selectedItem)
                return false;

            // update the fonts for old and new selected items
            if (selectedItem != null)
                selectedItem.markSelected(false);
            selectedItem = item;
            setText(item.node.name());
            item.markSelected(true);

            // if the next menu is already appropriate, exit
            if (nextMenu != null && nextMenu.node.equals(item.node))
                return false;

            // dispose and recreate the subsequent menus
            if (nextMenu != null)
                nextMenu.dispose();
            nextMenu = new HierMenu(item.node);
            return true;
        }

        private boolean updateToMatchActiveTask() {
            int pos = hier.getSelectedChild(node);
            return (pos < allItems.size() && setSelectedItem(allItems.get(pos)))
                    || (nextMenu != null && nextMenu.updateToMatchActiveTask());
        }

        private void updateCompletionCheckmarks() {
            for (HierMenuItem item : allItems)
                item.updateCompletionCheckmark();
            if (nextMenu != null)
                nextMenu.updateCompletionCheckmarks();
        }

        private boolean updateToMatchHierarchy() {
            boolean needsResize = false;
            HierMenu origNextMenu = nextMenu;
            if (needsMenuItemUpdate()) {
                String origText = getText();
                removeAll();
                addMenuItemsForChildren();
                if (!origText.equals(getText()))
                    needsResize = true;
            }
            if (nextMenu != origNextMenu)
                needsResize = true;
            else if (nextMenu != null && nextMenu.updateToMatchHierarchy())
                needsResize = true;

            return needsResize;
        }

        private boolean needsMenuItemUpdate() {
            int numChildren = hier.getNumChildren(node);
            if (numChildren != allItems.size())
                return true;

            for (int i = numChildren; i-- > 0;) {
                PropertyKey childKey = hier.getChildKey(node, i);
                if (!allItems.get(i).node.equals(childKey))
                    return true;
            }

            return false;
        }

        private SelectNextResult selectNext() {
            // if this is the terminal element without a visible menu, we
            // cannot perform selectNext. Ask our parent to handle it.
            if (nextMenu == null)
                return SelectNextResult.DEFER_TO_PARENT;

            // otherwise, try to delegate this task to the next menu. If it is
            // able to handle the request, we're done.
            SelectNextResult result = nextMenu.selectNext();
            if (result != SelectNextResult.DEFER_TO_PARENT)
                return result;

            // calculate the position of the next item, and select it
            int sel = hier.getSelectedChild(node) + 1;
            if (sel < allItems.size()) {
                setSelectedItem(allItems.get(sel));
                return SelectNextResult.HANDLED;
            }

            // The final item in our menu is already selected. Pass the request
            // along to our parent if that parent and our siblings were created
            // from a team workflow.
            String path = node.path();
            String parentPath = DataRepository.chopPath(path);
            String dataName = DataRepository.createDataName(parentPath,
                WORKFLOW_SOURCE_ID);
            SimpleData sd = dash.getData().getSimpleValue(dataName);
            if (sd != null && sd.test())
                return SelectNextResult.DEFER_TO_PARENT;
            else
                return SelectNextResult.REJECTED;
        }

    }

    private enum SelectNextResult {
        REJECTED, HANDLED, DEFER_TO_PARENT
    };


    private class HierMenuItem extends JMenuItem implements ActionListener {
        private HierMenu menu;

        private PropertyKey node;

        private boolean selected, completed;

        public HierMenuItem(HierMenu menu, PropertyKey node) {
            this.menu = menu;
            this.node = node;
            this.selected = this.completed = false;
            setText(node.name());
            setHorizontalTextPosition(SwingConstants.LEFT);
            updateCompletionCheckmark();
            addActionListener(this);
        }

        private void markSelected(boolean selected) {
            this.selected = selected;
            updateAppearance();
        }

        private void updateCompletionCheckmark() {
            completed = statusCalc.isCompleted(node.path());
            updateAppearance();
        }

        public void actionPerformed(ActionEvent e) {
            menu.setSelectedItem(this);
        }

        private void updateAppearance() {
            Icon icon = null;
            int font = (selected ? SELECTED : PLAIN);
            if (completed) {
                if (useStrikethrough)
                    font += COMPLETED;
                else
                    icon = CHECKMARK_ICON;
            }
            setIcon(icon);
            setFont(fonts[font]);
        }
    }


    private class HierMoreSubmenu extends JMenu {
        private HierMoreSubmenu() {
            super(Resources.getGlobalBundle().getDlgString("More"));
            setFont(fonts[PLAIN]);
        }
    }


    private static final String STRIKETHROUGH_SETTING = "userPref.taskSelector.useStrikethrough";

    private static final String WORKFLOW_SOURCE_ID = "Workflow_Source_ID";

    private static final Icon CHECKMARK_ICON = DashboardIconFactory
            .getLightCheckIcon();

}
