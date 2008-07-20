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

import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;

/** This class creates a local namespace around another expression
 *  context, allowing the data element [_] to be locally set.
 */
public class LocalExpressionContext implements ExpressionContext {

    public static final String LOCALVAR_NAME = "_";

    private ExpressionContext context;
    private SimpleData localValue = null;

    public LocalExpressionContext(ExpressionContext context) {
        this.context = context;
    }

    public void setLocalValue(SimpleData value) {
        localValue = value;
    }

    public void setLocalValue(Object v) {
        if (v instanceof SimpleData)
            setLocalValue((SimpleData) v);
        else if (v instanceof String)
            setLocalValue(StringData.create((String) v));
        else if (v instanceof SaveableData)
            setLocalValue(((SaveableData) v).getSimpleValue());
        else
            setLocalValue(null);
    }

    public SimpleData get(String dataName) {
        if ("_".equals(dataName))
            return localValue;
        else
            return context.get(dataName);
    }

    public String resolveName(String dataName) {
        if ("_".equals(dataName))
            return null;
        else
            return context.resolveName(dataName);
    }
}
