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

package pspdash.data;

import pspdash.DateFormatter;
import java.util.Date;


/* This class is the basis for all date data stored in the repository */
public class DateData implements SimpleData {

    Date value;
    boolean editable = true;
    boolean defined = true;


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

    public SimpleData getSimpleValue() {
        DateData result = new DateData(value, editable);
        if (!defined) result.defined = false;
        return result;
    }

    public void dispose() {
        value = null;
    }

    public static DateData create(String s) throws MalformedValueException {
        DateData result = new DateData();

        if ((result.value = DateFormatter.parseDateTime(s)) == null)
            throw new MalformedValueException();

        return result;
    }

    public String formatDate() {
        return DateFormatter.formatDateTime(value);
    }

    public Date getValue() {
        return new Date(value.getTime());
    }

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
}
