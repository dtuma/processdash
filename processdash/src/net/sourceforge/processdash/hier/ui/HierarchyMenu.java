// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.NarrowJMenu;

public class HierarchyMenu implements ActionListener, PropertyChangeListener {

    ProcessDashboard parent;
    ActiveTaskModel activeTaskModel;
    JMenuBar menuBar = null;
    JMenu menu = null;
    HierarchyMenu child = null;
    PropertyKey self = null;

    private void debug(String msg) {
        // System.out.println(msg);
    }

    public HierarchyMenu(ProcessDashboard dash, JMenuBar menuBar,
            ActiveTaskModel model, PropertyKey useSelf) {
        super();
        this.parent = dash;
        this.menuBar = menuBar;
        this.activeTaskModel = model;
        this.self = useSelf;
        // if (self != null)
        // debug (self.toString());

        String s;
        DashHierarchy props = parent.getProperties();

        int numChildren = props.getNumChildren(self);

        if (numChildren != 0) {
            menu = new NarrowJMenu();
            PCSH.enableHelpKey(menuBar, "HierarchyMenus");
            // PCSH.enableHelpKey(menu, "HierarchyMenus");

            menuBar.add(menu);

            int i;
            for (i = 0; i < numChildren; i++) {
                PropertyKey key = props.getChildKey(self, i);
                menu.add(new MyMenuItem(key));
            }

            syncSelectedChildToHierarchy();

        } else {
            activeTaskModel.setNode(self);
            parent.validate();
            parent.pack();
        }

        if (PropertyKey.ROOT.equals(useSelf))
            activeTaskModel.addPropertyChangeListener(this);
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

            if (PropertyKey.ROOT.equals(self))
                activeTaskModel.removePropertyChangeListener(this);
        }
    }

    /**
     * Update our state with the child selection recorded in the hierarchy.
     * 
     * @return true if a change was made, false if no changes were needed.
     */
    private boolean syncSelectedChildToHierarchy() {
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

        child = new HierarchyMenu(parent, menuBar, activeTaskModel, childKey);
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

    /**
     * Scans the hierarchy tree and cleans up "garbage" completion flags that
     * were left around by completion-button logic in v1.3
     */
    public void cleanupCompletionFlags() {
        cleanupCompletionFlags(parent.getProperties(), PropertyKey.ROOT);
    }

    protected void cleanupCompletionFlags(DashHierarchy props, PropertyKey key) {
        Prop val = props.pget(key);

        // no need to cleanup flags defined in project-level datafiles
        String dataFile = val.getDataFile();
        if (dataFile != null && dataFile.length() > 0)
            return;

        // no need to cleanup flags for leaves of the hierarchy
        int i = props.getNumChildren(key);
        if (i == 0)
            return;

        // cleanup the flag for this node in the hierarchy
        String flagName = getCompletedDataname(key);
        if (parent.getData().getValue(flagName) != null)
            parent.getData().putValue(flagName, null);

        // recurse through the hierarchy tree.
        while (i-- > 0)
            cleanupCompletionFlags(props, props.getChildKey(key, i));
    }

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

}
