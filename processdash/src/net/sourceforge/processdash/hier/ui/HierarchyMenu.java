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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyTreeModel.HierarchyTreeNode;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;

public class HierarchyMenu implements ActionListener, PropertyChangeListener,
        TaskNavigationSelector.NavMenuUI {

    ProcessDashboard parent;
    ActiveTaskModel activeTaskModel;
    HierarchicalCompletionStatusCalculator statusCalc;
    JMenuBar menuBar = null;
    JMenu menu = null;
    HierarchyMenu child = null;
    PropertyKey self = null;
    boolean isFirstMenu;

    public HierarchyMenu(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model) {
        this(dash, menuBar, model, getRootKeyFromSetting(dash), null, true);
    }

    private HierarchyMenu(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model, PropertyKey useSelf,
            HierarchicalCompletionStatusCalculator statusCalc,
            boolean isFirstMenu) {
        super();
        this.parent = dash;
        this.menuBar = menuBar;
        this.activeTaskModel = model;
        this.self = useSelf;
        this.isFirstMenu = isFirstMenu;

        if (isFirstMenu && SHOW_CHECKMARKS && statusCalc == null)
            statusCalc = new HierarchicalCompletionStatusCalculator(dash
                    .getData(), dash.getHierarchy(), self);
        this.statusCalc = statusCalc;

        DashHierarchy props = parent.getProperties();

        int numChildren = props.getNumChildren(self);

        if (numChildren != 0) {
            menu = new NarrowJMenu();
            PCSH.enableHelpKey(menuBar, "HierarchyMenus");
            // PCSH.enableHelpKey(menu, "HierarchyMenus");

            menuBar.add(menu);

            addMenuItemsForChildren(props);

            syncSelectedChildToHierarchy(false);

            if (statusCalc != null) {
                statusCalc.addActionListener(this);
                updateCompletionStatus();
            }
        } else {
            activeTaskModel.setNode(self);
            parent.validate();
            parent.windowSizeRequirementsChanged();
        }
    }

    private void addMenuItemsForChildren(DashHierarchy props) {
        JMenu destMenu = menu;
        int maxItemsPerMenu = Settings.getInt("hierarchyMenu.maxItems", 20);
        int numChildren = props.getNumChildren(self);

        for (int i = 0; i < numChildren; i++) {
            PropertyKey key = props.getChildKey(self, i);
            MyMenuItem menuItem = new MyMenuItem(key);

            if (destMenu.getItemCount()+1 >= maxItemsPerMenu) {
                JMenu moreMenu = new MyMoreSubmenu();
                destMenu.insert(moreMenu, 0);
                destMenu.insertSeparator(1);
                destMenu = moreMenu;
            }

            destMenu.add(menuItem);
        }
    }

    public void hierarchyChanged() {
        menu.removeAll();
        addMenuItemsForChildren(parent.getHierarchy());
        syncSelectedChildToHierarchy(false);
    }

    public void activeTaskChanged() {
        propertyChange(null);
    }

    public boolean selectNext() {
        return selectNextImpl() == SelectNextResult.HANDLED;
    }

    private enum SelectNextResult { REJECTED, HANDLED, DEFER_TO_PARENT };

    private SelectNextResult selectNextImpl() {
        // if this is the terminal HierarchyButton without a menu, we
        // cannot perform selectNext. Ask our parent to handle it.
        if (child == null)
            return SelectNextResult.DEFER_TO_PARENT;

        // otherwise, try to delegate this task to our child. If the child is
        // able to handle the request, we're done.
        SelectNextResult result = child.selectNextImpl();
        if (result != SelectNextResult.DEFER_TO_PARENT)
            return result;

        // calculate the number position of the next item to be selected.
        DashHierarchy props = parent.getProperties ();
        int sel = props.getSelectedChild (self) + 1;
        PropertyKey newSelKey = props.getChildKey(self, sel);

        // if that item is past the end of our list, we can't handle the request
        if (newSelKey == null) {
            if (sel == 1)
                // if we only had one child (not uncommon in team projects),
                // defer the selectNext operation to our parent.
                return SelectNextResult.DEFER_TO_PARENT;
            else
                // if we had more than one child, but the final child was
                // already selected, abort the selectNext operation.
                return SelectNextResult.REJECTED;
        }

        // select the next item on our menu.
        activeTaskModel.setNode(newSelKey);
        return SelectNextResult.HANDLED;
    }

    public void delete() {
        if (child != null) {
            child.delete();
            child = null;
            menuBar.remove(menu);
            menu.removeAll();
            menu = null;
        }
        if (statusCalc != null) {
            statusCalc.removeActionListener(this);
            if (isFirstMenu)
                statusCalc.dispose();
            statusCalc = null;
        }
    }

    /**
     * Update our state with the child selection recorded in the hierarchy.
     * 
     * @return true if a change was made, false if no changes were needed.
     */
    private boolean syncSelectedChildToHierarchy(boolean preserveActiveTask) {

        PropertyKey activeTask = activeTaskModel.getNode();
        if (preserveActiveTask && isFirstMenu && activeTask != null
                && !activeTask.isChildOf(self)
                && !activeTask.equals(self) && !self.isChildOf(activeTask)) {
            // The active task is outside our list of descendants!  This can
            // happen to the topmost HierarchyMenu if it is not anchored at the
            // root of the hierarchy.  In this case, we need special handling.
            if (child != null)
                child.delete();

            menu.setText(TaskNavigationSelector.prettifyPath(activeTask));
            child = new HierarchyMenu(parent, menuBar, activeTaskModel,
                    activeTask, statusCalc, false);
            return true;
        }

        DashHierarchy props = parent.getProperties();
        int numChildren = props.getNumChildren(self);
        if (numChildren == 0)
            return false;

        int sel = props.getSelectedChild(self);
        if (sel < 0 || sel >= numChildren)
            sel = 0;
        PropertyKey childKey = props.getChildKey(self, sel);

        if (child != null) {
            if (childKey.equals(child.self))
                return false;
            else
                child.delete();
        }

        menu.setText(childKey.name());

        child = new HierarchyMenu(parent, menuBar, activeTaskModel, childKey,
                statusCalc, false);
        return true;
    }



    private void updateCompletionStatus() {
        updateCompletionStatus(menu);
    }

    private void updateCompletionStatus(JMenu menu) {
        if (menu != null && statusCalc != null)
            for (int i = menu.getItemCount(); i-- > 0;) {
                JMenuItem item = menu.getItem(i);
                if (item instanceof MyMenuItem)
                    ((MyMenuItem) item).updateStatus();
                else if (item instanceof MyMoreSubmenu)
                    updateCompletionStatus((MyMoreSubmenu) item);
            }
    }

    class MyMenuItem extends JMenuItem {

        String path;

        MyMenuItem(PropertyKey key) {
            super(key.name());
            path = key.path();
            addActionListener(HierarchyMenu.this);
            setHorizontalTextPosition(SwingConstants.LEFT);
        }

        public void updateStatus() {
            Icon i = (statusCalc.isCompleted(path) ? CHECKMARK_ICON : null);
            setIcon(i);
        }

    }

    class MyMoreSubmenu extends JMenu {

        MyMoreSubmenu() {
            super(Resources.getGlobalBundle().getDlgString("More"));
        }

    }

    private static final String CHECKMARK_SETTING_NAME = "setting";

    private static boolean SHOW_CHECKMARKS = !"false".equalsIgnoreCase(Settings
            .getVal(CHECKMARK_SETTING_NAME));

    private static final Icon CHECKMARK_ICON = DashboardIconFactory
            .getCheckIcon();

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof HierarchicalCompletionStatusCalculator) {
            updateCompletionStatus();

        } else if (e.getSource() instanceof MyMenuItem) {
            String childName = ((JMenuItem) e.getSource()).getText();
            PropertyKey newChild = new PropertyKey(self, childName);
            if (activeTaskModel.setNode(newChild) == false) {
                System.out.println("task model refused to change to "+newChild);
                activeTaskModel.setNode(newChild);
            }
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (syncSelectedChildToHierarchy(true) == false && child != null)
            child.propertyChange(evt);
    }

    public static boolean chooseHierarchyNavigator(Component parent,
            DashboardContext context, Resources resources) {
        HierarchyTreeModel model = new HierarchyTreeModel(context.getHierarchy());
        model.setRootName(resources.getString("Hierarchy.Root_Node_Name"));
        JTree tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setSelectionInterval(0, 0);
        tree.setToggleClickCount(4);
        new JOptionPaneClickHandler().install(tree);

        JScrollPane sp = new JScrollPane(tree);
        sp.setPreferredSize(new Dimension(200, 200));

        String title = resources.getString("Hierarchy.Dialog.Title");
        Object[] message = new Object[] {
                resources.getStrings("Hierarchy.Dialog.Prompt"), sp,
                new JOptionPaneTweaker.MakeResizable() };
        String path = null;
        if (JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            path = getSelectedPathForHierarchyNavigator(tree, context);
        tree.setModel(null);

        if (path != null) {
            InternalSettings.set(HIERARCHY_ROOT_PATH_SETTING, path);
            return true;
        }
        return false;
    }

    private static String getSelectedPathForHierarchyNavigator(JTree tree,
            DashboardContext context) {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath == null)
            // no node was selected
            return null;

        HierarchyTreeNode node = (HierarchyTreeNode) selectedPath
                .getLastPathComponent();
        if (node == null)
            // not sure if this can happen, but be safe.
            return null;

        if (node.getChildCount() == 0) {
            // don't allow the user to select leaf nodes.  The HierarchyMenu
            // class can't handle that very well.  Instead, use the parent of
            // the leaf node.
            maybeSelectChildWithinParent(context, node.getPath());
            node = (HierarchyTreeNode) node.getParent();
        }

        if (node == null)
            return null;
        else
            return node.getPath();
    }

    /**
     * If the user selected a leaf node, we will choose to navigate within the
     * parent instead. In that scenario, if we will be changing the active task
     * to the user's selection (instead of preserving the active task), we'll
     * arrange for the user's chosen leaf to be the selected child of its parent.
     */
    private static void maybeSelectChildWithinParent(DashboardContext context,
            String path) {
        DashHierarchy hier = context.getHierarchy();
        PropertyKey childKey = hier.findExistingKey(path);
        if (childKey == null)
            return;
        PropertyKey parent = childKey.getParent();
        if (parent == null)
            return;
        for (int i = hier.getNumChildren(parent);  i-- > 0; ) {
            if (hier.getChildKey(parent, i).equals(childKey)) {
                hier.setSelectedChild(parent, i);
                break;
            }
        }
    }

    private static PropertyKey getRootKeyFromSetting(ProcessDashboard dash) {
        String path = Settings.getVal(HIERARCHY_ROOT_PATH_SETTING, "/");
        PropertyKey key = dash.getProperties().findExistingKey(path);
        if (key == null)
            key = PropertyKey.ROOT;
        return key;
    }

    public static final Icon HIER_ICON = new ImageIcon(HierarchyMenu.class
            .getResource("hier.gif"));
    private static final String HIERARCHY_ROOT_PATH_SETTING =
            "navigator.hierarchyPath";
}
