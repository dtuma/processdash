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

import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.compiler.ExecutionException;
import net.sourceforge.processdash.data.compiler.ListStack;

public class CompiledFunction implements SaveableData, AliasedData,
                                  ActiveExpressionContext.Listener
{
    private static final SimpleData UNCALCULATED_VALUE =
        new DoubleData(Double.NaN, false);

    protected String name = null, prefix, aliasFor;
    protected CompiledScript script;
    protected DataRepository data;
    protected ActiveExpressionContext context = null;
    protected ListStack stack = null;
    protected SimpleData value = UNCALCULATED_VALUE;
    protected boolean currentlyCalculating = false;

    public CompiledFunction(String name, String script,
                            DataRepository r, String prefix)
        throws MalformedValueException
    {
        try {
            if (script.charAt(0) == '{') script = script.substring(1);
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

    public CompiledScript getScript() { return script; }

    public void expressionContextChanged() {
        recalc();
    }

    protected void recalc() {
        boolean needsNotify = (value != UNCALCULATED_VALUE);

        if (context == null) synchronized(this) {
            if (context == null)
                context=new ActiveExpressionContext(name, prefix, data, this);
            if (stack == null)
                stack = new ListStack();
        }

        Object calculationInfo;
        synchronized (stack) {
            synchronized (context) {
                if (currentlyCalculating) {
                    System.err.println("Encountered recursively defined data "+
                                       "when calculating "+name+" - ABORTING");
                    return;             // break out of infinite loops.
                }

                context.startCalculation();
                stack.clear();
                try {
                    currentlyCalculating = true;
                    script.run(stack, context);
                    currentlyCalculating = false;
                    aliasFor = (String) stack.peekDescriptor();
                    value = (SimpleData) stack.pop();
                    if (value != null && aliasFor == null)
                        value = (SimpleData) value.getEditable(false);
                } catch (ExecutionException e) {
                    System.err.println("Error executing " + name + ": " + e);
                    value = null;
                }
                calculationInfo = context.endCalculation();
            }
        }

        if (needsNotify)
            data.valueRecalculated(name, this);

        if (calculationInfo != null)
            context.performSubscriptions(calculationInfo);
    }

    // The following methods define the SaveableData interface.

    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}
    public String saveString() { return script.saveString(); }

    public String getAliasedDataName() {
        if (value == UNCALCULATED_VALUE)
            recalc();

        return aliasFor;
    }

    public SimpleData getSimpleValue() {
        if (value == UNCALCULATED_VALUE)
            recalc();

        return value;
    }

    public void dispose() {
        name = null;
        script = null;
        stack = null;
        value = null;
        ActiveExpressionContext c = context;
        context = null;
        if (c != null) c.dispose();
    }

    public SaveableData getEditable(boolean editable) { return this; }
}
