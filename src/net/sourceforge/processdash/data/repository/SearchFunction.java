// Copyright (C) 2003-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.CompiledScript;


public class SearchFunction implements SaveableData, RepositoryListener,
        DataListener, Comparator, DataNameFilter.PrefixLocal
{
    protected SearchFactory factory;
    protected String name = null, prefix = null;
    protected String start, tag, tag2;
    protected CompiledScript script;
    protected DataRepository data;
    protected ListData value, externalValue;
    protected int chopTagLength;
    private volatile boolean valueQueried = false;

    /** a list of the conditions created by this SearchFunction. */
    protected Set condList = Collections.synchronizedSet(new HashSet());


    public SearchFunction(SearchFactory factory, String name, String start,
            String tag, CompiledScript expression, DataRepository data,
            String prefix) {
        this.factory = factory;
        this.name = name;
        this.start = start;
        storeTag(tag);
        this.script = expression;
        this.data = data;
        this.prefix = prefix;
        this.value = new ListData();
        this.externalValue = null;
        chopTagLength = (this.tag.startsWith("/") ? this.tag.length() : 0);

        this.value.setEditable(false);

        data.addRepositoryListener(this, start);
    }

    private void storeTag(String tagName) {
        if (tagName.startsWith("/")) {
            tag = tagName;
            tag2 = tagName.substring(1);
        } else if (tagName.startsWith(" ") || tagName.length() == 0) {
            tag = tag2 = tagName;
        } else {
            tag = "/" + tagName;
            tag2 = tagName;
        }
    }

    private static final String CONDITION_NAME = "_SearchCondition_///";
    private static volatile int UNIQUE_ID = 0;

    /** Create a unique name for a condition expression for the given prefix */
    private String getAnonymousConditionName(String prefix) {
        int id;
        synchronized (SearchFunction.class) {
            id = UNIQUE_ID++;
        }
        return DataRepository.createDataName
            (DataRepository.anonymousPrefix + prefix, CONDITION_NAME + id);
    }

    /** Return the prefix that corresponds to the named condition expression */
    private String getConditionPrefix(String conditionName) {
        int pos = conditionName.indexOf(CONDITION_NAME);
        if (pos == -1) return null;
        return conditionName.substring(DataRepository.anonymousPrefix.length(),
                                       pos-1);
    }

    /** If the dataName is a matching tag, return the corresponding prefix.
     *  Otherwise return null. */
    private String getTagPrefix(String dataName) {
        if (!dataName.endsWith(tag)) return null;
        if (!dataName.startsWith(start)) return null;
        return dataName.substring(0, dataName.length() - chopTagLength);
    }

    /** A collection of threads for whom we are currently handling
     *  thread events.
     */
    private Set eventThreads = Collections.synchronizedSet(new HashSet());

    /** Add a prefix to our list, doing our best to keep the list
     *  sorted in hierarchy order.
     *
     * @return true if the value of the list changed.
     */
    private boolean doAdd(String prefix) {
        // If the user edits their hierarchy, our list may no longer
        // be sorted correctly, so we have to be wary of that possibility.
        // The Collections.binarySearch() method requires that the list be
        // sorted in ascending order.  Therefore, if the binarySearch method
        // reports that the item is not in our list, we need to doublecheck
        // ourselves to make certain it really isn't there.
        int pos = Collections.binarySearch(value.asList(), prefix, this);
        if (pos >= 0)
            return false;
        else if (value.asList().contains(prefix)) {
            // If our doublecheck indicates that the item IS there, then
            // re-sort the list to avoid future problems.
            value.sortContents(this);
            return true;
        } else {
            value.insert(prefix, -1 - pos);
            return true;
        }
    }

    public boolean acceptPrefixLocalName(String prefix, String localName) {
        return (localName.equals(tag2) || localName.endsWith(tag));
    }

    public void dataAdded(String dataName) {
        String dataPrefix = getTagPrefix(dataName);
        if (dataPrefix == null) return;

        // If we got this far, this data element matches the start and the tag.

        /*
        if (eventThreads.add(Thread.currentThread()) == false)
            return;             // Guard against infinite loops.
        */
        if (script == null) {
            // Listen for changes to the data value.
            data.addActiveDataListener(dataName, this, name, false);

            // We don't have a script - just check to ensure that the
            // element is defined, and then add it.
            if (test2(data.getSimpleValue(dataName))) {
                if (doAdd(dataPrefix))
                    doNotify();
            }

        } else {
            // We need to see if this element matches the expression.
            String condName = getAnonymousConditionName(dataPrefix);
            SaveableData condition = new CompiledFunction
                (condName, script, data, dataPrefix);
            data.putValue(condName, condition);

            // Keep a list of the conditions we're watching.
            condList.add(condName);

            // Listen for changes to this condition expression.
            data.addActiveDataListener(condName, this, name, false);

            // If the condition evaluates to true, add this prefix to our
            // value.
            if (test(condition.getSimpleValue())) {
                if (doAdd(dataPrefix))
                    doNotify();
            }
        }

        eventThreads.remove(Thread.currentThread());
    }

    public void dataRemoved(String dataName) {
        String dataPrefix = getTagPrefix(dataName);
        if (dataPrefix == null) return;

        /*
        if (eventThreads.add(Thread.currentThread()) == false)
            return;             // Guard against infinite loops.
        */

        String condNamePrefix = dataPrefix + "/" + CONDITION_NAME;

        Iterator i = condList.iterator();
        String condName;
        while (i.hasNext()) {
            condName = (String) i.next();
            if (condName.startsWith(condNamePrefix)) {
                data.removeDataListener(condName, this);
                condList.remove(condName);
                break;
            }
        }

        // Note that it isn't necessary for us to removeDataListener(dataName)
        // because the element associated with dataName is disappearing even
        // as we speak

        if (value.remove(dataPrefix))
            doNotify();

        eventThreads.remove(Thread.currentThread());
    }


    private boolean test(SimpleData data) {
        return (data != null && data.test());
    }
    private boolean test2(SimpleData data) {
        return (data != null);
    }


    private boolean isCondition(String name) {
        return (condList.contains(name));
    }

    private boolean handleDataEvent(DataEvent e) {

        String dataName = e.getName();
        String dataPrefix;
        boolean include;
        if (isCondition(dataName)) {
            dataPrefix = getConditionPrefix(dataName);
            include = test(e.getValue());
        } else {
            dataPrefix = getTagPrefix(dataName);
            if (dataPrefix == null) return false;
            include = test2(e.getValue());
        }

        if (include)
            return doAdd(dataPrefix);
        else
            return value.remove(dataPrefix);
    }


    public void dataValueChanged(DataEvent e) {
        boolean needToNotify = handleDataEvent(e);

        if (needToNotify) doNotify();
    }

    public void dataValuesChanged(Vector v) {
        boolean needToNotify = false;
        if (v == null || v.size() == 0) return;
        for (int i = v.size();  i-- > 0; )
            if (handleDataEvent((DataEvent) v.elementAt(i)))
                needToNotify = true;

        if (needToNotify) doNotify();
    }

    protected void doNotify() {
        externalValue = null;
        if (valueQueried)
            data.valueRecalculated(name, this);
    }

    // The following methods define the SaveableData interface.

    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}
    public String saveString() { return ""; }

    public SimpleData getSimpleValue() {
        valueQueried = true;
        if (externalValue == null && value != null)
            externalValue = new ListData(value);
        return externalValue;
    }

    public void dispose() {
        if (data == null) return;
        data.removeRepositoryListener(this);
        data.deleteDataListener(this);
        condList = null;
        data = null;
        name = prefix = start = tag = null;
        script = null;
        value = null;
    }

    public SaveableData getEditable(boolean editable) { return this; }

    public int compare(Object o1, Object o2) {
        return data.compareNames(o1+"/tag", o2 + "/tag");
    }
}
