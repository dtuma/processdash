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
import pspdash.data.StringData;
import pspdash.data.NumberData;
import pspdash.data.DoubleData;
import pspdash.data.ListData;
import java.util.List;
import java.util.ArrayList;

public class AbstractFunction implements Function {

    public Object call(List arguments, ExpressionContext context) {
        return null;            // null operation, meant to be overridden.
    }

    protected static SimpleData getArg(List arguments, int pos) {
        return getArg(arguments, pos, null);
    }
    protected static SimpleData getArg(List arguments, int pos,
                                       SimpleData defVal) {
        if (arguments.size() > pos)
            return (SimpleData) arguments.get(pos);
        else
            return defVal;
    }

    protected static SimpleData getLocal(ExpressionContext context) {
        return context.get(LocalExpressionContext.LOCALVAR_NAME);
    }
    protected static SimpleData getArgOrLocal(List arguments, int pos,
                                              ExpressionContext context) {
        if (arguments.size() < pos+1)
            return getLocal(context);
        else
            return getArg(arguments, pos);
    }

    protected static ListData asList(SimpleData l) {
        if (l instanceof ListData)   return (ListData) l;
        if (l instanceof StringData) return ((StringData) l).asList();
        return null;
    }

    protected static List collapseLists(List arguments, int pos) {
        List result = new ArrayList();
        SimpleData arg;
        ListData argList;
        for (; pos < arguments.size();  pos++)
            appendToList(result, getArg(arguments, pos));
        return result;
    }

    private static void appendToList(List result, Object data) {
        if (data instanceof StringData)
            data = ((StringData) data).asList();
        if (data instanceof ListData)
            for (int i=0;  i < ((ListData) data).size();  i++)
                appendToList(result, ((ListData) data).get(i));
        else if (data != null)
            result.add(data);
    }

    protected static double asDouble(SimpleData s) {
        if (s instanceof NumberData)
            return ((NumberData) s).getDouble();
        else
            return 0.0;
    }

    protected static String asStringVal(Object o) {
        if (o instanceof String) return (String) o;
        if (o instanceof StringData) return ((StringData)o).getString();
        return null;
    }

    protected static String asString(Object o) {
        if (o == null) return null;
        if (o instanceof SimpleData) return ((SimpleData)o).format();
        return o.toString();
    }

    protected static SimpleData asSimpleData(Object o) {
        if (o == null) return null;
        if (o instanceof SimpleData) return (SimpleData) o;
        if (o instanceof String) return StringData.create((String) o);
        if (o instanceof Number)
            return new DoubleData(((Number) o).doubleValue());
        return null;
    }
}
