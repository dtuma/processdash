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

import pspdash.data.compiler.CompiledScript;
import pspdash.data.compiler.Compiler;
import pspdash.data.compiler.ExecutionException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Vector;

class SearchFunction implements SaveableData, RepositoryListener, DataListener
{
    protected String name = null, prefix = null;
    protected String start, tag;
    protected CompiledScript script;
    protected DataRepository data;
    protected ListData value, externalValue;
    protected int chopTagLength;
    private volatile boolean valueQueried = false;

    /** a list of the conditions created by this SearchFunction. */
    protected Set condList = Collections.synchronizedSet(new HashSet());


    public SearchFunction(String name, String start, String tag,
                          CompiledScript expression,
                          DataRepository data, String prefix) {
        this.name = name;
        this.start = start;
        this.tag = maybeAddSlash(tag);
        this.script = expression;
        this.data = data;
        this.prefix = prefix;
        this.value = this.externalValue = new ListData();
        chopTagLength = (this.tag.startsWith("/") ? this.tag.length() : 0);

        this.value.setEditable(false);

        data.addRepositoryListener(this, start);
    }

    private String maybeAddSlash(String name) {
        if (name.startsWith("/") || name.startsWith(" ") || name.length() == 0)
            return name;
        else
            return "/" + name;
    }

    private static final String CONDITION_NAME = "_SearchCondition_///";
    private static volatile int UNIQUE_ID = 0;

    /** Create a unique name for a condition expression for the given prefix */
    private String getAnonymousConditionName(String prefix) {
        int id;
        synchronized (SearchFunction.class) {
            id = UNIQUE_ID++;
        }
        return data.createDataName(DataRepository.anonymousPrefix + prefix,
                                   CONDITION_NAME + id);
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

    public void dataAdded(DataEvent e) {
        String dataName = e.getName();
        String dataPrefix = getTagPrefix(dataName);
        if (dataPrefix == null) return;

        // If we got this far, this data element matches the start and the tag.

        /*
        if (eventThreads.add(Thread.currentThread()) == false)
            return;             // Guard against infinite loops.
        */
        if (script == null) {
            // We don't have a script - all elements should be implicitly
            // added.
            if (value.setAdd(dataPrefix))
                doNotify();

        } else {
            // We need to see if this element matches the expression.
            String condName = getAnonymousConditionName(dataPrefix);
            SaveableData condition = new CompiledFunction
                (condName, script, data, dataPrefix);
            data.putValue(condName, condition);

            // Keep a list of the conditions we're watching.
            condList.add(condName);

            // If the condition evaluates to true, add this prefix to our
            // value.
            if (test(condition.getSimpleValue())) {
                if (value.setAdd(dataPrefix))
                    doNotify();
            }

            // Listen for changes to this condition expression.
            data.addActiveDataListener(condName, this, name);
        }

        eventThreads.remove(Thread.currentThread());
    }

    public void dataRemoved(DataEvent e) {
        String dataName = e.getName();
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

        if (value.remove(dataPrefix))
            doNotify();

        eventThreads.remove(Thread.currentThread());
    }


    private boolean test(SimpleData data) {
        return (data != null && data.test());
    }


    private boolean isCondition(String name) {
        return (condList.contains(name));
    }

    private boolean handleDataEvent(DataEvent e) {

        String dataName = e.getName();
        if (!isCondition(dataName)) return false;

        String dataPrefix = getConditionPrefix(dataName);
        if (test(e.getValue()))
            return value.setAdd(dataPrefix);
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
        if (valueQueried) {
            externalValue = new ListData(value);
            data.putValue(name, this);
        }
    }

    // The following methods define the SaveableData interface.

    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}
    public String saveString() { return ""; }

    public SimpleData getSimpleValue() {
        valueQueried = true;
        return externalValue;
    }

    public void dispose() {
        if (data == null) return;
        data.removeRepositoryListener(this);
        if (script != null)
            data.deleteDataListener(this);
        condList = null;
        data = null;
        name = prefix = start = tag = null;
        script = null;
        value = null;
    }

    public SaveableData getEditable(boolean editable) { return this; }
}
