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

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;


/** This class creates a local namespace around another expression
 *  context, defining a new default prefix for data elements.
 */
public class RelativeExpressionContext implements ExpressionContext {

    private ExpressionContext context;
    private String prefix;

    public RelativeExpressionContext(ExpressionContext context,
                                     String prefix) {
        this.context = context;
        this.prefix = prefix;
    }

    public SimpleData get(String dataName) {
        if (dataName.equals(PREFIXVAR_NAME))
            return StringData.create(prefix);
        else
            return context.get(resolveName(dataName));
    }

    public String resolveName(String dataName) {
        if (dataName.startsWith("{") && dataName.endsWith("}"))
            // Names bracketed with {curly braces} should be interpreted
            // relative to the prefix of the original expression context.
            dataName = dataName.substring(1, dataName.length()-1);

        else if (dataName.equals(LocalExpressionContext.LOCALVAR_NAME))
            // If the dataName is the local variable [_], don't try to
            // localize it or change it in any way
            ;

        else
            // Non-bracketed names should be interpreted in the context
            // of our prefix.  (Note that createDataName() is already
            // smart enough to leave absolute names - e.g. "/foo" - alone.)
            dataName = DataRepository.createDataName(prefix, dataName);

        return dataName;
    }
}
