// Copyright (C) 2005 Tuma Solutions, LLC
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

import java.text.NumberFormat;
import java.text.ParseException;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.repository.Repository;
import net.sourceforge.processdash.util.AdaptiveNumberFormat;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.TimeNumberFormat;

class TimeInterpreter extends DoubleInterpreter {

    public static final NumberFormat FMT = new AdaptiveNumberFormat(
            new TimeNumberFormat(), 1);

    public TimeInterpreter(Repository r, String name, int numDigits_ignored,
            boolean readOnly) {
        super(r, name, 1, readOnly);
    }

    public void setString(String s) throws MalformedValueException {
        try {
            Number n = FMT.parse(s);
            value = new DoubleData(n.doubleValue());
        } catch (Exception pe) {
            throw new MalformedValueException();
        }
    }

    public String getString() {
        if (value instanceof DoubleData && value.isDefined()) {
            double time = ((DoubleData) value).getDouble();
            return FMT.format(time);
        } else
            return super.getString();
    }

}
