// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.TaskNavigationSelector;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;

public class HierarchyMenu implements ActionListener, PropertyChangeListener,
        TaskNavigationSelector.NavMenuUI {

    ProcessDashboard parent;
    ActiveTaskModel activeTaskModel;
    JMenuBar menuBar = null;
    JMenu menu = null;
    HierarchyMenu child = null;
    PropertyKey self = null;
    boolean isFirstMenu;

    public HierarchyMenu(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model, PropertyKey useSelf) {
        this(dash, menuBar, model, useSelf, true);
    }

    private HierarchyMenu(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model, PropertyKey useSelf, boolean isFirstMenu) {
        super();
        this.parent = dash;
        this.menuBar = menuBar;
        this.activeTaskModel = model;
        this.self = useSelf;
        this.isFirstMenu = isFirstMenu;
        // if (self != null)
        // debug (self.toString());

        DashHierarchy props = parent.getProperties();

        int numChildren = props.getNumChildren(self);

        if (numChildren != 0) {
            menu = new NarrowJMenu();
            PCSH.enableHelpKey(menuBar, "HierarchyMenus");
            // PCSH.enableHelpKey(menu, "HierarchyMenus");

            menuBar.add(menu);

            addMenuItemsForChildren(props);

            syncSelectedChildToHierarchy();

        } else {
            activeTaskModel.setNode(self);
            parent.validate();
            parent.pack();
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
                JMenu moreMenu = new JMenu(
                        Resources.getGlobalBundle().getDlgString("More"));
                destMenu.insert(moreMenu, 0);
                destMenu.insertSeparator(1);
                destMenu = moreMenu;
            }

            destMenu.add(menuItem);
        }
    }

    private int getNumChildren() {
        return (menu == null ? 0 : menu.getMenuComponentCount());
    }

    public String getDisplayName() {
        if (self == PropertyKey.ROOT)
            return null;
        else
            return self.path();
    }

    public boolean selectNext() {
        // if this is the terminal HierarchyButton without a menu, we
        // cannot perform selectNext.  Return false.
        if (child == null) { return false; }

        // otherwise, try to delegate this task to our child.  If the child is
        // able to perform a selectNext, we're done.
        if ((child != null) && child.selectNext()) return true;

        // if our child had only one subchild (extremely common in team
        // projects), then it will never be able to selectNext.  We'll
        // perform a selectNext on its behalf.  But if our child had
        // multiple children, stop here.
        if (child.getNumChildren() > 1) { return false; }

        // calculate the number position of the next item to be selected.
        DashHierarchy props = parent.getProperties ();
        int sel = props.getSelectedChild (self) + 1;
        PropertyKey newSelKey = props.getChildKey(self, sel);

        // if that item is past the end of our list, we cannot perform
        // selectNext. Return false.
        if (newSelKey == null) { return false; }

        // select the next item on our menu.
        activeTaskModel.setNode(newSelKey);
        return true;
    }

    public void delete() {
        if (child != null) {
            child.delete();
            child = null;
            menuBar.remove(menu);
            for (int i = menu.getItemCount(); i-- > 0;)
                if (menu.getItem(i) instanceof MyMenuItem)
                    ((MyMenuItem) menu.getItem(i)).delete();
            menu = null;
        }
    }

    /**
     * Update our state with the child selection recorded in the hierarchy.
     * 
     * @return true if a change was made, false if no changes were needed.
     */
    private boolean syncSelectedChildToHierarchy() {

        PropertyKey activeTask = activeTaskModel.getNode();
        if (isFirstMenu && activeTask != null && !activeTask.isChildOf(self)
                && !activeTask.equals(self) && !self.isChildOf(activeTask)) {
            // The active task is outside our list of descendants!  This can
            // happen to the topmost HierarchyMenu if it is not anchored at the
            // root of the hierarchy.  In this case, we need special handling.
            if (child != null)
                child.delete();

            menu.setText(activeTask.path());
            child = new HierarchyMenu(parent, menuBar, activeTaskModel,
                    activeTask, false);
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
                false);
        return true;
    }



    private String getCompletedDataname(PropertyKey key) {
        return DataRepository.createDataName(key.path(), "Completed");
    }

    class MyMenuItem extends JMenuItem implements DataListener {
        String dataname;

        MyMenuItem(PropertyKey key) {
            super(key.name());
            addActionListener(HierarchyMenu.this);
            setHorizontalTextPosition(SwingConstants.LEFT);
            dataname = getCompletedDataname(key);
            if (SHOW_CHECKMARKS) {
                // System.out.println("addDataListener("+dataname+")");
                parent.getData().addDataListener(dataname, this);
            }
        }

        public void dataValueChanged(DataEvent e) {
            final Icon i = ((e.getValue() != null && e.getValue().test()) ? CHECKMARK_ICON
                    : null);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setIcon(i);
                }
            });
        }

        public void dataValuesChanged(Vector v) {
            for (int i = v.size(); i-- > 0;)
                dataValueChanged((DataEvent) v.get(i));
        }

        public void delete() {
            if (SHOW_CHECKMARKS) {
                // System.out.println("removeDataListener("+dataname+")");
                parent.getData().removeDataListener(dataname, this);
            }
        }
    }

    private static final String CHECKMARK_SETTING_NAME = "setting";

    private static boolean SHOW_CHECKMARKS = !"false".equalsIgnoreCase(Settings
            .getVal(CHECKMARK_SETTING_NAME));

    private static final Icon CHECKMARK_ICON = DashboardIconFactory
            .getCheckIcon();

    public void actionPerformed(ActionEvent e) {
        String childName = ((JMenuItem) e.getSource()).getText();
        PropertyKey newChild = new PropertyKey(self, childName);
        if (activeTaskModel.setNode(newChild) == false) {
            System.out.println("task model refused to change to "+newChild);
            activeTaskModel.setNode(newChild);
        }

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (syncSelectedChildToHierarchy() == false && child != null)
            child.propertyChange(evt);
    }

    public static final Icon HIER_ICON = new ImageIcon(HierarchyMenu.class
            .getResource("hier.gif"));
}
