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

package pspdash.data.compiler;

import java.util.Hashtable;
import java.util.ArrayList;

class FunctionCall implements Instruction {

    /** This singleton object is pushed on the expression stack to
     *  delineate the end of a function's argument list. */
    private static final Object STACK_MARKER = new Object();

    public static final Instruction PUSH_STACK_MARKER = new Instruction() {
            public void execute(Stack s, ExpressionContext c) {
                s.push(STACK_MARKER); }
            public String toString() {
                return "push FUNCTION-CALL-STACK-MARKER"; } };

    private String functionName;
    private Function f = null;

    private static final Hashtable functionCache = new Hashtable();
    public static FunctionCall get(String functionName)
        throws CompilationException
    {
        Object result = functionCache.get(functionName);
        if (result instanceof FunctionCall)
            return (FunctionCall) result;
        if (result instanceof CompilationException)
            throw (CompilationException) result;

        try {
            result = new FunctionCall(functionName);
            functionCache.put(functionName, result);
            return (FunctionCall) result;
        } catch (CompilationException e) {
            functionCache.put(functionName, e);
            throw e;
        }
    }

    private FunctionCall(String functionName) throws CompilationException {
        this.functionName = functionName;

        Class functionClass = getClass(functionName);
        try {
            this.f = (Function) functionClass.newInstance();
        } catch (Throwable t) {
            throw new CompilationException
                ("Couldn't create an instance of function '" +
                 functionName + "': " + t);
        }
    }
    private static String canonicalFunctionName(String funcName) {
        if (funcName == null) return null;
        if (funcName.length() < 2) return funcName.toUpperCase();
        return funcName.substring(0, 1).toUpperCase() +
            funcName.substring(1).toLowerCase();
    }

    private static Class getClass(String functionName)
        throws CompilationException
    {
        functionName = canonicalFunctionName(functionName);
        try {
            return Class.forName("pspdash.data.compiler." + functionName);
        } catch (Throwable t) {}
        try {
            return Class.forName("pspdash.data." + functionName);
        } catch (Throwable t) {}
        try {
            return Class.forName(functionName);
        } catch (Throwable t) {}
        throw new CompilationException
            ("Couldn't find the class for function '" + functionName + "'.");
    }

    public void execute(Stack stack, ExpressionContext context)
        throws ExecutionException
    {
        if (f == null)
            throw new ExecutionException("No definition for function " +
                                         functionName);

        ArrayList arguments = new ArrayList();
        Object arg = null;
        while (true) {
            if (stack.empty())
                throw new ExecutionException
                    ("Couldn't find stack marker when executing " +
                     functionName);

            arg = stack.pop();
            if (arg == STACK_MARKER) break;

            // insert the new arg at the beginning of the arg list.
            arguments.add(0, arg);
        }

        Object result = f.call(arguments, context);
        stack.push(result);
    }

    public String toString() { return functionName; }
}
