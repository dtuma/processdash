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

import pspdash.Perl5Util;
import pspdash.PerlPool;
import pspdash.data.ImmutableDoubleData;

import java.util.List;


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
