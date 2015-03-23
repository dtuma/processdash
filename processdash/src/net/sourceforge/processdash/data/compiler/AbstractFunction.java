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

package net.sourceforge.processdash.data.compiler;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;


public class AbstractFunction implements Function {

    protected class DescribedValue implements Function.DescribedValue {
        private Object value, descriptor;
        public DescribedValue(Object v, Object d) {
            value = v; descriptor = d; }
        public Object getValue()      { return value;      }
        public Object getDescriptor() { return descriptor; }
    }

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
