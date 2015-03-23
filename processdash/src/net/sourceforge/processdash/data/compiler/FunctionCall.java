// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.compiler;

import java.util.ArrayList;
import java.util.Hashtable;

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

    private static final String[] PACKAGE_PREFIXES = {
        "net.sourceforge.processdash.data.compiler.function.",
        "net.sourceforge.processdash.data.compiler.",
        "net.sourceforge.processdash.data."
    };

    private static Class getClass(String functionName)
        throws CompilationException
    {
        functionName = canonicalFunctionName(functionName);
        for (int i = 0;   i < PACKAGE_PREFIXES.length;   i++)
            try {
                return Class.forName(PACKAGE_PREFIXES[i] + functionName);
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
        if (result instanceof Function.DescribedValue)
            stack.push(((Function.DescribedValue) result).getValue(),
                       ((Function.DescribedValue) result).getDescriptor());
        else
            stack.push(result);
    }

    public String toString() { return functionName; }
}
