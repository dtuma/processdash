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

package net.sourceforge.processdash.data.repository;

import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.Compiler;


class PercentageFunction extends CompiledFunction {

    public static final String PERCENTAGE_FLAG = "/%/";


    public PercentageFunction(String name, DataRepository r)
        throws MalformedValueException
    {
        super(name, compileScript(name), r, "");
    }

    private static CompiledScript compileScript(String name)
            throws MalformedValueException {
        int flagPos = name.indexOf(PERCENTAGE_FLAG);
        if (flagPos <= 0)
            throw new MalformedValueException
                ("PercentageFunction: '" + name + "' does not contain '" +
                 PERCENTAGE_FLAG + "'.");

        String dataName = name.substring(flagPos + PERCENTAGE_FLAG.length());
        String childPrefix = name.substring(0, flagPos);
        String numeratorName = DataRepository.createDataName(childPrefix,
            dataName);

        int slashPos = childPrefix.lastIndexOf('/');
        if (slashPos == -1)
            throw new MalformedValueException
                ("PercentageFunction: '" + name + "' does not have a parent.");

        String parentPrefix = name.substring(0, slashPos);
        String denominatorName = DataRepository.createDataName(parentPrefix,
            dataName);

        return Compiler.divisionExpr(numeratorName, denominatorName);
    }


    static boolean isPercentageDataName(String name) {
        return (name != null && name.indexOf(PERCENTAGE_FLAG) > 0);
    }
}
