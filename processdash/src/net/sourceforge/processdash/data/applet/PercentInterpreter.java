// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.data.applet;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.repository.Repository;
import net.sourceforge.processdash.util.FormatUtil;


public class PercentInterpreter extends DoubleInterpreter {

    public PercentInterpreter(Repository r, String name,
                              int numDigits, boolean readOnly) {
        super(r, name, numDigits, readOnly);
    }


    public String getString() {
        if (value instanceof DoubleData && value.isDefined())
            return FormatUtil.formatPercent(((DoubleData) value).getDouble(), numDigits);
        else
            return super.getString();
    }


    public void setString(String s) throws MalformedValueException {
        try {
            value = new DoubleData(FormatUtil.parsePercent(s.trim()), true);
        } catch (Exception e) {
            throw new MalformedValueException();
        }
    }

}
