// Copyright (C) 2001-2006 Tuma Solutions, LLC
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.CompiledFunction;


public class CompiledScript implements Serializable {

    private String saveString = null;
    private List instructions = new ArrayList();
    private boolean committed = false;

    CompiledScript() {}

    CompiledScript(CompiledScript s) {
        instructions.addAll(s.instructions);
    }

    void add(Instruction i) throws IllegalStateException {
        if (committed) throw new IllegalStateException();
        instructions.add(i);
    }

    void commit() {
        if (!committed)
            instructions = Collections.unmodifiableList(instructions);
        committed = true;
    }

    public void run(Stack stack, ExpressionContext context)
        throws ExecutionException, IllegalStateException
    {
        if (!committed) throw new IllegalStateException();

        synchronized (stack) {
            synchronized (context) {
                Iterator iter = instructions.iterator();
                Instruction instr;
                while (iter.hasNext()) {
                    instr = (Instruction) iter.next();
                    instr.execute(stack, context);
                }
            }
        }
    }

    public SimpleData getConstant() throws IllegalStateException {
        if (!committed) throw new IllegalStateException();

        if (!isConstant()) return null;
        PushConstant p = (PushConstant) instructions.get(0);
        return p.getConstant();
    }


    public boolean isConstant() throws IllegalStateException {
        if (!committed) throw new IllegalStateException();
        return (instructions.size() == 1 &&
                instructions.get(0) instanceof PushConstant);
    }

    public boolean matches(Object o) {
        if (o instanceof CompiledFunction) {
            CompiledFunction cf = (CompiledFunction) o;
            return cf.getScript() == this;
        }
        return false;
    }

    void setSaveString(String str) throws IllegalStateException {
        if (committed) throw new IllegalStateException();
        saveString = str;
        if (!saveString.startsWith("{"))
            saveString = "{" + saveString;
    }

    public String saveString() { return saveString; }

    public String toString() {
        StringBuffer result = new StringBuffer();
        Iterator i = instructions.iterator();
        while (i.hasNext())
            result.append("\t").append(i.next().toString()).append("\n");
        return result.toString();
    }
}
