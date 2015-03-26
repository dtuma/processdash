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

package net.sourceforge.processdash.data.compiler.function;

import java.util.List;

import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

public class Indexof extends AbstractFunction {

    private static final Object NOT_FOUND = new ImmutableDoubleData(-1, false,
            true);

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context) {
        SimpleData item = getArg(arguments, 0);
        List args = collapseLists(arguments, 1);
        for (int i = 0; i < args.size(); i++) {
            if (eq(item, asSimpleData(args.get(i))))
                return new ImmutableDoubleData(i, false, true);
        }

        return NOT_FOUND;
    }

    private boolean eq(SimpleData a, SimpleData b) {
        if (a == b) return true;
        if (a == null) return false;
        return a.equals(b);
    }

}
