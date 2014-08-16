// Copyright (C) 2007-2014 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.util.LightweightSet;

/** Looks at a hierarchy of tasks, and determines the completion status
 * of each task.
 * 
 * Completion status is determined via a combination of:
 * <ul>
 * <li>Looking up "Completed" elements in the data repository</li>
 * <li>Considering parents to be complete if all of their children are marked
 *     complete</li>
 * </ul>
 */
public class HierarchicalCompletionStatusCalculator implements DataListener {

    private static final String COMPLETED = "/Completed";

    private DataRepository data;

    private PropertyKeyHierarchy hierarchy;

    private PropertyKey root;

    private Map statusIn;

    private Map statusOut;

    private Set eventListeners;

    public HierarchicalCompletionStatusCalculator(DataRepository data,
            PropertyKeyHierarchy hierarchy, PropertyKey root) {
        this.data = data;
        this.hierarchy = hierarchy;
        this.root = root;
        this.eventListeners = new LightweightSet();

        reloadHierarchy();
    }

    public void addActionListener(ActionListener l) {
        eventListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        eventListeners.remove(l);
    }

    public boolean isCompleted(String path) {
        Boolean result = null;
        if (statusOut != null)
            result = (Boolean) statusOut.get(path);

        if (result == null)
            return false;
        else
            return result.booleanValue();
    }

    public void reloadHierarchy() {
        this.statusOut = null;
        loadData();
        this.statusOut = new HashMap();
        recalc();
    }

    public void dispose() {
        for (Iterator i = statusIn.keySet().iterator(); i.hasNext();) {
            String path = (String) i.next();
            String dataName = path + COMPLETED;
            data.removeDataListener(dataName, this);
        }
        data = null;
        hierarchy = null;
        statusIn = null;
        statusOut = null;
    }

    private void loadData() {
        this.statusIn = new HashMap();
        loadData(root);
    }

    private void loadData(PropertyKey key) {
        int numChildren = hierarchy.getNumChildren(key);
        for (int i = numChildren; i-- > 0;)
            loadData(hierarchy.getChildKey(key, i));

        String dataName = key.path() + COMPLETED;
        data.addDataListener(dataName, this, true);
    }

    public void dataValueChanged(DataEvent e) {
        if (handleEvent(e) && statusOut != null)
            recalc();
    }

    private boolean handleEvent(DataEvent e) {
        String dataName = e.getName();
        if (dataName.endsWith(COMPLETED)) {
            String path = dataName.substring(0, dataName.length()
                    - COMPLETED.length());
            SimpleData value = e.getValue();
            Object newValue;
            if (value == null)
                newValue = null;
            else if (value.test())
                newValue = Boolean.TRUE;
            else
                newValue = Boolean.FALSE;
            Object oldValue = statusIn.put(path, newValue);
            if (oldValue != newValue)
                return true;
        }

        return false;
    }

    public void dataValuesChanged(Vector v) throws RemoteException {
        boolean changed = false;
        for (Iterator i = v.iterator(); i.hasNext();) {
            DataEvent e = (DataEvent) i.next();
            if (handleEvent(e))
                changed = true;
        }
        if (changed && statusOut != null)
            recalc();
    }


    public void recalc() {
        recalc(root);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireActionPerformed();
            }});
    }

    private boolean recalc(PropertyKey key) {
        int numChildren = hierarchy.getNumChildren(key);
        String path = key.path();

        boolean childResult = true;
        for (int i = numChildren; i-- > 0;)
            childResult = recalc(hierarchy.getChildKey(key, i)) && childResult;

        Boolean myStatus = (Boolean) statusIn.get(path);
        if (myStatus == null) {
            if (numChildren == 0)
                myStatus = Boolean.FALSE;
            else
                myStatus = Boolean.valueOf(childResult);
        }

        statusOut.put(path, myStatus);
        return myStatus.booleanValue();
    }

    private void fireActionPerformed() {
        if (!eventListeners.isEmpty()) {
            ActionEvent e = new ActionEvent(this, 0, null);
            ArrayList toNotify = new ArrayList(eventListeners);
            for (Iterator i = toNotify.iterator(); i.hasNext();)
                ((ActionListener) i.next()).actionPerformed(e);
        }
    }

}
