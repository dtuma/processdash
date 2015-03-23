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
import net.sourceforge.processdash.data.compiler.AbstractFunction;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.util.Perl5Util;
import net.sourceforge.processdash.util.PerlPool;


public class Match extends AbstractFunction {

    /** Perform a procedure call.
     *
     * This method <b>must</b> be thread-safe.
     */
    public Object call(List arguments, ExpressionContext context)
    {
        String pattern = asString(getArg(arguments, 0));
        if (pattern == null || pattern.length() == 0)
            return ImmutableDoubleData.TRUE;

        String text = asString(getArgOrLocal(arguments, 1, context));
        if (text == null || text.length() == 0)
            return ImmutableDoubleData.FALSE;

        Perl5Util perl = PerlPool.get();
        try {
            if (perl.match(pattern, text))
                return ImmutableDoubleData.TRUE;
            else
                return ImmutableDoubleData.FALSE;

        } catch (Throwable t) {
            return null;
        } finally {
            PerlPool.release(perl);
        }
    }
}
