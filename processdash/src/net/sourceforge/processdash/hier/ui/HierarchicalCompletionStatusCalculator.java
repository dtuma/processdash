// Copyright (C) 2007-2015 Tuma Solutions, LLC
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.util.LightweightSet;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

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

    private Map<String, Date> statusIn;

    private Map<String, Date> statusOut;

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
        return getDateCompleted(path) != null;
    }

    /** @since 2.1.7 */
    public Date getDateCompleted(String path) {
        if (statusOut == null)
            return null;
        else
            return statusOut.get(path);
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
            SimpleData dataValue = e.getValue();
            Date newValue = null;
            if (dataValue instanceof DateData && dataValue.test())
                newValue = ((DateData) dataValue).getValue();
            Date oldValue = statusIn.put(path, newValue);
            if (NullSafeObjectUtils.EQ(oldValue, newValue) == false)
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

    private Date recalc(PropertyKey key) {
        int numChildren = hierarchy.getNumChildren(key);
        String path = key.path();

        Date childResult = null;
        if (numChildren > 0) {
            childResult = recalc(hierarchy.getChildKey(key, 0));
            for (int i = numChildren; i-- > 1;)
                childResult = getMaxCompletionDate(childResult,
                    recalc(hierarchy.getChildKey(key, i)));
        }

        Date myStatus = statusIn.get(path);
        if (myStatus == null)
            myStatus = childResult;

        statusOut.put(path, myStatus);
        return myStatus;
    }

    private Date getMaxCompletionDate(Date a, Date b) {
        if (a == null || b == null) return null;
        return (a.compareTo(b) > 0 ? a : b);
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
