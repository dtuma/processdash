// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import pspdash.data.compiler.ExpressionContext;

import java.util.*;

class ActiveExpressionContext implements ExpressionContext, DataListener
{
    public interface Listener {
        void expressionContextChanged();
    }

    private class SubscriptionInfo {
        public Set checkList = null, missingList = null;
    }

    private DataRepository data;
    private Listener listener;
    private String name, prefix;
    private Map localValues, globalValues;
    private Set checkList = null, missingList = null;


    public ActiveExpressionContext(String name, String prefix,
                                   DataRepository r, Listener l)
    {
        this.data = r;
        this.name = name;
        this.prefix = prefix;
        this.listener = l;
        this.localValues = new HashMap();
        this.globalValues = new HashMap();
        //this.localValues = Collections.synchronizedMap(new HashMap());
        //this.globalValues = Collections.synchronizedMap(new HashMap());
    }

    public void dispose() {
        DataRepository r = data;
        if (r != null) r.deleteDataListener(this);
        data = null;
        listener = null;
        name = prefix = null;
        localValues = globalValues = null;
        checkList = missingList = null;
    }

    private Map currentMap = null;
    private String internalName, externalName_;

    private synchronized void setNames(String dataName) {
        if (dataName.startsWith(data.PARENT_PREFIX)) {
            // the name is making a reference to its parent. It should
            // be normalized and treated as global.
            currentMap = globalValues;
            internalName = externalName_ =
                data.createDataName(prefix, dataName);
        } else if (! dataName.startsWith("/")) {
            // if the name does not start with "/", it is local and should
            // be interpreted relative to the prefix.
            currentMap = localValues;
            internalName = dataName;
            externalName_ = null; // don't calc this yet - usually not needed
        } else if (dataName.startsWith(prefix)) {
            // if the name begins with the prefix, it should be treated
            // like a local value.
            currentMap = localValues;
            externalName_ = dataName;
            internalName = dataName.substring(prefix.length());
        } else {
            // otherwise, the name is a global name.
            currentMap = globalValues;
            internalName = externalName_ = dataName;
        }
    }

    public synchronized SimpleData get(String dataName) {
        setNames(dataName);

        if (checkList != null)
            checkList.remove(internalName);

        SimpleData result = (SimpleData) currentMap.get(internalName);

        if (result == null && !currentMap.containsKey(internalName)) {
            if (missingList != null)
                missingList.add(internalName);
            result = data.getSimpleValue(externalName());
            currentMap.put(internalName, result);
        }

        return result;
    }

    public synchronized void startCalculation() {
        if (checkList == null)
            checkList = new HashSet();
        else
            checkList.clear();
        checkList.addAll(localValues.keySet());
        checkList.addAll(globalValues.keySet());

        if (missingList == null)
            missingList = new HashSet();
        else
            missingList.clear();
    }

    public synchronized Object endCalculation() {
        SubscriptionInfo result = null;
        if (! checkList.isEmpty()) {
            result = new SubscriptionInfo();
            result.checkList = checkList;
            checkList = null;
        }
        if (! missingList.isEmpty()) {
            if (result == null) result = new SubscriptionInfo();
            result.missingList = missingList;
            missingList = null;
        }
        return result;
    }

    public void performSubscriptions(Object info) {
        if (! (info instanceof SubscriptionInfo)) return;

        // any items in finalCheckList are items which we have been
        // listening to, and no longer care about.
        Set list = ((SubscriptionInfo) info).checkList;
        Iterator i;
        if (list != null) {
            i = list.iterator();
            while (i.hasNext()) {
                setNames((String) i.next());

                data.removeDataListener(externalName(), this);
                currentMap.remove(internalName);
            }
        }

        // Any items in the finalMissingList are items which were used
        // in the last calculation, which we are not yet registered for.
        list = ((SubscriptionInfo) info).missingList;
        if (list != null) {
            i = list.iterator();
            while (i.hasNext()) {
                setNames((String) i.next());
                data.addActiveDataListener(externalName(), this, name);
            }
        }
    }

    private String externalName() {
        if (externalName_ == null)
            externalName_ = data.createDataName(prefix, internalName);
        return externalName_;
    }

    private boolean handleEvent(DataEvent e) {
        String dataName = e.getName();
        Map map;
        if (dataName.startsWith(prefix)) {
            map = localValues;
            dataName = dataName.substring(prefix.length() + 1);
        } else {
            map = globalValues;
        }
        SimpleData newValue = e.getValue();
        SimpleData oldValue = (SimpleData) map.put(dataName, newValue);

        // the next line makes the (not-quite-valid) assumption that
        // simple values are immutable, and that anytime a dynamically
        // calculated value recalculates, it will create a NEW
        // SimpleData object to represent the value.
        if (oldValue == newValue) return false;
        if (oldValue != null) return !oldValue.equals(newValue);
        if (newValue != null) return !newValue.equals(oldValue);
        return false;
    }

    public void dataValueChanged(DataEvent e) {
        if (handleEvent(e)) recalc();
    }

    public void dataValuesChanged(Vector v) {
        boolean needsRecalc = false;
        for (int i = v.size();  i-- > 0; )
            if (handleEvent((DataEvent) v.elementAt(i)))
                needsRecalc = true;

        if (needsRecalc) recalc();
    }

    public void recalc() {
        if (listener != null) listener.expressionContextChanged();
    }
}
