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
import pspdash.data.compiler.ExecutionException;
import pspdash.data.compiler.ExpressionContext;
import pspdash.data.compiler.ListStack;

class CompiledFunction implements SaveableData,
                                  ActiveExpressionContext.Listener
{
    private static final SimpleData UNCALCULATED_VALUE =
        new DoubleData(Double.NaN, false);

    protected String name = null, prefix;
    protected CompiledScript script;
    protected DataRepository data;
    protected ActiveExpressionContext context = null;
    protected ListStack stack = null;
    protected SimpleData value = UNCALCULATED_VALUE;

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
        if (name == null) return;
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
                context.startCalculation();
                stack.clear();
                try {
                    script.run(stack, context);
                    value = (SimpleData) stack.pop();
                    if (value != null)
                        value = (SimpleData) value.getEditable(false);
                } catch (ExecutionException e) {
                    System.err.println("Error executing " + name + ": " + e);
                    value = null;
                }
                calculationInfo = context.endCalculation();
            }
        }

        if (needsNotify)
            data.putValue(name, this);

        if (calculationInfo != null)
            context.performSubscriptions(calculationInfo);
    }

    // The following methods define the SaveableData interface.

    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}
    public String saveString() { return script.saveString(); }

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
