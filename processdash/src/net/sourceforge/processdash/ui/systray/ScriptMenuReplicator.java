// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.systray;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.process.ScriptID;

public class ScriptMenuReplicator {

    private DashHierarchy hierarchy;

    private ActiveTaskModel activeTaskModel;

    private PopupMenu popupMenu;

    public static void replicate(ProcessDashboard pdash, PopupMenu dest) {
        new ScriptMenuReplicator(pdash, dest);
    }

    public ScriptMenuReplicator(ProcessDashboard pdash, PopupMenu popupMenu) {
        this.popupMenu = popupMenu;
        popupMenu.add(new ScriptMenuSeparator());

        // listen to application state & mirror the contents of the script menu
        hierarchy = pdash.getHierarchy();
        hierarchy.addHierarchyListener(EventHandler.create(
            DashHierarchy.Listener.class, this, "updateScriptMenuItems"));
        activeTaskModel = pdash.getActiveTaskModel();
        activeTaskModel.addPropertyChangeListener(EventHandler.create(
            PropertyChangeListener.class, this, "updateScriptMenuItems"));
        updateScriptMenuItems();
    }

    public void updateScriptMenuItems() {
        // look for the last "script menu separator" item to determine where
        // the script menu entries appear in our popup menu.
        int pos = popupMenu.getItemCount() - 1;
        while (!(popupMenu.getItem(pos) instanceof ScriptMenuSeparator)) {
            pos--;
        }

        // now, discard any script menu items that are currently present.
        while (pos-- > 0) {
            if (popupMenu.getItem(pos) instanceof ScriptItemTag)
                popupMenu.remove(pos);
            else
                break;
        }

        // retrieve the current list of script menu items from the dashboard
        PropertyKey currentPhase = activeTaskModel.getNode();
        List scriptItems = hierarchy.getScriptIDs(currentPhase);

        if (scriptItems != null && scriptItems.size() > 1) {
            // insert a separator at the top of the list of script items
            popupMenu.insert(new ScriptMenuSeparator(), ++pos);
            Iterator i = scriptItems.iterator();
            // the first item in the list is a "default script" which does not
            // need to be displayed. But we will retrieve the initial data
            // path from that item to know what we're displaying scripts for.
            String currentPath = ((ScriptID) i.next()).getDataPath();
            // now insert a menu item for each script.
            while (i.hasNext()) {
                ScriptID scriptID = (ScriptID) i.next();
                // if the data path changed with this item, insert a separator
                String newPath = scriptID.getDataPath();
                if (!newPath.equals(currentPath))
                    popupMenu.insert(new ScriptMenuSeparator(), ++pos);
                currentPath = newPath;
                // now insert a menu item for this script.
                popupMenu.insert(new ScriptMenuItem(scriptID), ++pos);
            }
        }
    }

    private interface ScriptItemTag {
    }

    private static class ScriptMenuItem extends MenuItem implements
            ScriptItemTag {
        private ScriptID target;

        public ScriptMenuItem(ScriptID target) {
            super(target.getDisplayName());
            this.target = target;
            addActionListener(DISPATCHER);
        }

        public void display() {
            target.display();
        }

        private static final ActionListener DISPATCHER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((ScriptMenuItem) e.getSource()).display();
            }
        };

    }

    private static class ScriptMenuSeparator extends MenuItem implements
            ScriptItemTag {
        public ScriptMenuSeparator() {
            super("-");
        }
    }

}
