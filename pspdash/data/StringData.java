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


/* This class is the basis for all textual data stored in the repository */
public class StringData implements SimpleData {

    String value = null;
    boolean editable = true;
    boolean defined = true;

    public StringData() { value = ""; }

    public StringData(String s) throws MalformedValueException {
        if (s.charAt(0) == '?') {
            defined = false;
            s = s.substring(1);
        }
        if (s.charAt(0) == '\"')
            s = s.substring(1);
        else
            throw new MalformedValueException();

        value = unescapeString(s);
    }

    public static String unescapeString(String s) {
        StringBuffer val = new StringBuffer();
        int pos;

        while ((pos = s.indexOf('\\')) != -1) {
            val.append(s.substring(0, pos));
            switch (s.charAt(pos+1)) {
                case 'n':         val.append('\n'); break;
                case '\\':        val.append('\\'); break;
                case 't':         val.append('\t'); break;
                default:          val.append(s.charAt(pos+1)); break;
            };
            s = s.substring(pos+2);
        }

        val.append(s);

        return val.toString();
    }

    public static String escapeString(String s) {
        StringBuffer val = new StringBuffer();
        char c;

        for(int pos = 0;   pos < s.length();   pos++)
            switch (c = s.charAt(pos)) {
                case '\\':    val.append("\\\\"); break;
                case '\n':    val.append("\\n");  break;
                case '\t':    val.append("\\t");  break;
                default:      val.append(c);
            }

        return val.toString();
    }

    public String saveString()  {
        return (defined ? "" : "?") + saveString(this.value);
    }

    public static String saveString(String value) {
        return "\"" + escapeString(value);
    }

    public String getString() { return value; }

    public static StringData create(String value) {
        StringData result = new StringData();
        result.value = value;
        return result;
    }

    public SimpleData getSimpleValue() {
        StringData result = new StringData();
        result.value = value;
        result.editable = editable;
        result.defined = defined;
        return result;
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean e) { editable = e; }
    public boolean isDefined() { return defined; }
    public void setDefined(boolean d) { defined = d; }

    public void dispose() { value = null; }

    public String format() { return value; }
    public SimpleData parse(String val) throws MalformedValueException {
        return (val == null || val.length() == 0) ? null : create(value);
    }

    private int cmp(SimpleData val) {
        String v1 = value, v2 = ((StringData)val).value;
        if (v1 == null) v1 = "";
        if (v2 == null) v2 = "";
        return v1.compareTo(v2);
    }
    public boolean equals(SimpleData val) {
        return ((val instanceof StringData) && (cmp(val) == 0));
    }
    public boolean lessThan(SimpleData val) {
        return ((val instanceof StringData) && (cmp(val) < 0));
    }
    public boolean greaterThan(SimpleData val) {
        return ((val instanceof StringData) && (cmp(val) > 0));
    }
    public boolean test() {
        return (value != null && value.length() > 0);
    }
}
