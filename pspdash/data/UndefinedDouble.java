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


/* This class provides a special flavor of Double values whose
 * semantic meaning is, "this data value still needs to be manually
 * entered by the user."  Constants for zero and NaN allow quick
 * access to the two most common cases.  Of course, these numbers will
 * behave like zero or NaN when used in calculations; they can be
 * displayed specially on forms where users are editing data values.
 */
public class UndefinedDouble extends DoubleData implements UndefinedData {

    public static final UndefinedDouble ZERO = new UndefinedDouble(0.0);
    public static final UndefinedDouble NaN  = new UndefinedDouble(Double.NaN);

    /** Create an instance of UndefinedDouble.
     *
     *  It will have a double value of value, and will be editable.
     */
    public UndefinedDouble(double value) { super(value, true); }

    public UndefinedDouble(String saveString) throws MalformedValueException {
        super(Double.NaN, true);
        if (!saveString.equals("?#")) try {
            value = Double.valueOf(saveString.substring(1)).doubleValue();
        } catch (NumberFormatException nfe) {
            throw new MalformedValueException();
        }
    }

    public String saveString() {
        return (Double.isNaN(value) ? "?#" : "?" + value);
    }
    public void setEditable(boolean e) { /* do nothing */ }
    public SimpleData getSimpleValue() { return this;     }
}
