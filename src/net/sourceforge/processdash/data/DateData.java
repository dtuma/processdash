// Copyright (C) 2000-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data;

import java.util.Date;


import net.sourceforge.processdash.util.FormatUtil;


/* This class is the basis for all date data stored in the repository */
public class DateData implements SimpleData {

    Date value;
    boolean editable = true;
    boolean defined = true;
    boolean formatAsDateOnly = false;


    public DateData() { value = new Date(); }

    public DateData(Date v, boolean e) { value = v;   editable = e; }

    public DateData(String s) throws MalformedValueException {
        try {
            if (s.charAt(0) == '?') {
                defined = false;
                s = s.substring(1);
            }
            if (s.charAt(0) != '@')
                throw new MalformedValueException();
            if (s.equalsIgnoreCase("@now"))
                value = new Date();
            else
                value = new Date(Long.parseLong(s.substring(1)));
        } catch (NumberFormatException e) {
            throw new MalformedValueException();
        }
    }

    public String saveString() {
        return (defined ? "@" : "?@") + value.getTime();
    }
    public boolean isEditable()         { return editable; }
    public void setEditable(boolean e)  { editable = e; }
    public boolean isDefined() { return defined; }
    public void setDefined(boolean d) { defined = d; }
    public void setFormatAsDateOnly(boolean b) { formatAsDateOnly = b; }
    public boolean isFormatAsDateOnly() { return formatAsDateOnly; }

    public SimpleData getSimpleValue() {
        DateData result = new DateData(value, editable);
        if (!defined) result.defined = false;
        return result;
    }

    public void dispose() {}

    public static DateData create(String s) throws MalformedValueException {
        DateData result = new DateData();

        if ((result.value = FormatUtil.parseDateTime(s)) == null)
            throw new MalformedValueException();

        return result;
    }


    public String formatDate() {
        if (formatAsDateOnly)
            return FormatUtil.formatDate(value);
        else
            return FormatUtil.formatDateTime(value);
    }

    public Date getValue() {
        return new Date(value.getTime());
    }

    public String toString() { return format(); }
    public String format() { return formatDate(); }
    public SimpleData parse(String val) throws MalformedValueException {
        return (val == null || val.length() == 0) ? null : create(val);
    }
    public boolean equals(SimpleData val) {
        return ((val instanceof DateData) &&
                (value != null) && (value.equals(((DateData)val).value)));
    }
    public boolean lessThan(SimpleData val) {
        return ((val instanceof DateData) &&
                (value != null) && (value.before(((DateData)val).value)));
    }
    public boolean greaterThan(SimpleData val) {
        return ((val instanceof DateData) &&
                (value != null) && (value.after(((DateData)val).value)));
    }
    public boolean test() {
        return (value != null && value.getTime() > 0);
    }

    public SaveableData getEditable(boolean editable) {
        SimpleData result = getSimpleValue();
        result.setEditable(editable);
        return result;
    }

    public static Date valueOf(SaveableData value) {
        SimpleData sd = null;
        if (value instanceof SimpleData)
            sd = (SimpleData) value;
        else if (value != null)
            sd = value.getSimpleValue();

        if (sd instanceof DateData)
            return ((DateData) sd).getValue();
        else
            return null;
    }
}
