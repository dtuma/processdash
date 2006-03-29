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

package net.sourceforge.processdash.data.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.compiler.ExecutionException;
import net.sourceforge.processdash.data.compiler.ListStack;

public class CompiledFunction implements SaveableData, AliasedData,
        DataListener {


    /** The name of the data element we are calculating */
    protected String name = null;

    /** The data name prefix for performing calculations */
    protected String prefix;

    /** The script to run to perform the calculation */
    protected CompiledScript script;

    /** The data repository */
    protected DataRepository data;

    /** The value we obtained when we ran our calculation last. */
    protected SimpleData value;

    /** Track the aliased name of our final value.
     * {@see net.sourceforge.processdash.data.repository.AliasedData } */
    protected String aliasFor;

    /** True if we have ever told anyone our calculated value. */
    protected volatile boolean valueQueried = false;

    /** A list of the names of all data elements in the repository that we
     * are currently listening to. */
    protected Set currentSubscriptions;

    /** A mutex that helps us keep track of the need to recalculate */
    protected DirtyTracker extChanges = new DirtyTracker();


    public CompiledFunction(String name, String script, DataRepository r,
            String prefix) throws MalformedValueException {
        try {
            if (script.charAt(0) == '{')
                script = script.substring(1);
            this.script = Compiler.compile(script);
            this.data = r;
            this.name = name;
            this.prefix = prefix;
        } catch (Exception ce) {
            throw new MalformedValueException();
        }
    }

    public CompiledFunction(String name, CompiledScript script,
            DataRepository r, String prefix) {
        this.script = script;
        this.data = r;
        this.name = name;
        this.prefix = prefix;
    }

    public CompiledScript getScript() {
        return script;
    }

    protected void recalc() {
        Set calcNameSet = (Set) CURRENTLY_CALCULATING.get();
        if (calcNameSet.contains(name)) {
            System.err.println("Encountered recursively defined data "
                    + "when calculating " + name + " - ABORTING");
            return; // break out of infinite loops.
        }

        if (valueQueried == false || currentSubscriptions == null) {
            synchronized (this) {
                if (currentSubscriptions == null)
                    currentSubscriptions = Collections
                            .synchronizedSet(new HashSet(4));
            }
        }

        SimpleData oldValue = value;
        SimpleData newValue = null;
        String newAlias = null;
        SubscribingExpressionContext context = null;

        // attempt to perform the calculation up to 10 times.  (This should
        // be more than generous - even one retry should be rare.)
        int retryCount = 10;
        while (retryCount-- > 0 && extChanges.isDirty()) {
            context = new SubscribingExpressionContext(data, prefix, this,
                    name, currentSubscriptions);
            ListStack stack = new ListStack();
            int changeCount = -1;

            try {
                calcNameSet.add(name);
                changeCount = extChanges.getUnhandledChangeCount();
                script.run(stack, context);
                newAlias = (String) stack.peekDescriptor();
                newValue = (SimpleData) stack.pop();
                if (newValue != null && newAlias == null)
                    newValue = (SimpleData) newValue.getEditable(false);
            } catch (ExecutionException e) {
                System.err.println("Error executing " + name + ": " + e);
                newValue = null;
            } finally {
                calcNameSet.remove(name);
            }

            if (extChanges.maybeClearDirty(changeCount, newValue, newAlias))
                break;
            else if (retryCount > 0)
                System.err.println("Retrying calculating " + name);
        }

        if (retryCount <= 0)
            System.err.println("Ran out of retries while calculating " + name);

        context.removeOldSubscriptions();

        if (valueQueried && !eq(oldValue, value))
            data.valueRecalculated(name, this);

    }

    private boolean eq(SimpleData a, SimpleData b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        return (a.isEditable() == b.isEditable()
                && a.isDefined() == b.isDefined()
                && a.equals(b));
    }

    public void dataValueChanged(DataEvent e) {
        setDirty();
    }

    public void dataValuesChanged(Vector v) {
        setDirty();
    }

    private void setDirty() {
        extChanges.setDirty();
        data.valueRecalculated(name, this);
    }


    // The following methods define the SaveableData interface.

    public boolean isEditable() {
        return false;
    }

    public void setEditable(boolean e) {
    }

    public boolean isDefined() {
        return true;
    }

    public void setDefined(boolean d) {
    }

    public String saveString() {
        return script.saveString();
    }

    public String getAliasedDataName() {
        if (extChanges.isDirty())
            recalc();

        valueQueried = true;
        return aliasFor;

    }

    public SimpleData getSimpleValue() {
        if (extChanges.isDirty())
            recalc();

        valueQueried = true;
        return value;
    }

    public void dispose() {
        name = prefix = aliasFor = null;
        script = null;
        data = null;
        value = null;
        currentSubscriptions = null;
        extChanges = null;
    }

    public SaveableData getEditable(boolean editable) {
        return this;
    }

    /** Class for tracking the need to recalculate.
     * 
     * This class tracks the number of external change notifications received.
     * Before a calculation, this value is queried.  If the calculation
     * finishes and this count hasn't changed, then the calculation is
     * considered a success.  If the count <b>did</b> change during the
     * calculation, it means that we received an external change notification
     * while calculating. Thus, the calculated value is suspect and should be
     * recomputed.
     * 
     * As a side note, this work is performed by this object instead of by
     * the CompiledFunction object, because we want to avoid holding
     * synchronization locks on the CompiledFunction object whenever possible.
     * (This helps avoid potential deadlocks within the DataRepository.)
     */
    protected class DirtyTracker {

        private int unhandledChangeCount = 1;

        public synchronized void setDirty() {
            unhandledChangeCount++;
        }

        public synchronized boolean isDirty() {
            return unhandledChangeCount > 0;
        }

        public synchronized int getUnhandledChangeCount() {
            return unhandledChangeCount;
        }

        public synchronized boolean maybeClearDirty(int newHandledCount,
                SimpleData newValue, String newAlias) {

            if (newHandledCount == unhandledChangeCount) {
                value = newValue;
                aliasFor = newAlias;
                unhandledChangeCount = 0;
                return true;
            } else {
                return false;
            }
        }
    }

    /** A list of the data names that are currently being calculated by this
     * thread (to prevent infinite circular recursion)
     */
    protected static ThreadLocal CURRENTLY_CALCULATING = new ThreadLocal() {
        protected Object initialValue() {
            return new HashSet();
        }};
}
