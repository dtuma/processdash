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

import pspdash.data.SimpleData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
