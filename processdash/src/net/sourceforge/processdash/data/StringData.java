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

package net.sourceforge.processdash.data;



/* This class is the basis for all textual data stored in the repository */
public class StringData implements SimpleData {

    String value = null;
    boolean editable = true;
    boolean defined = true;
    ListData asList = null;

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
                case '\r': case '\n':
                    // a backslash immediately before an EOL is a continuation
                    // line.  Advance past any whitespace following the EOL.
                    while (pos+2 < s.length() && s.charAt(pos+2) <= ' ')
                        pos++;
                    break;

                case 'n':         val.append('\n'); break;
                case 'r':         val.append('\r'); break;
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
        if (s == null || s.length() == 0) return s;
        StringBuffer val = new StringBuffer();
        char c;

        for(int pos = 0;   pos < s.length();   pos++)
            switch (c = s.charAt(pos)) {
                case '\\':    val.append("\\\\"); break;
                case '\r':    val.append("\\r");  break;
                case '\n':    val.append("\\n");  break;
                case '\t':    val.append("\\t");  break;
                default:      val.append(c);
            }

        return val.toString();
    }

    public String saveString()  {
        return (isDefined() ? "" : "?") + saveString(getString());
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
        result.value    = getString();
        result.editable = isEditable();
        result.defined  = isDefined();
        return result;
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean e) { editable = e; }
    public boolean isDefined() { return defined; }
    public void setDefined(boolean d) { defined = d; }

    public void dispose() {}

    public String toString() { return format(); }
    public String format() { return getString(); }
    public SimpleData parse(String val) throws MalformedValueException {
        return (val == null || val.length() == 0) ? null : create(val);
    }

    private int cmp(SimpleData val) {
        String v1 = nvl(getString());
        String v2 = nvl(((StringData)val).getString());
        int result = v1.compareToIgnoreCase(v2);
        if (result != 0)
            return result;
        else
            return v1.compareTo(v2);
    }
    private String nvl(String s) {
        return (s == null ? "" : s);
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
        return (getString() != null && getString().length() > 0);
    }

    // Converting a string to a list will be a very common action,
    // particularly by functions like "sumFor".  Rather than reparsing
    // the string and building a new ListData object each time, this
    // routine makes it possible to save the cached list object.
    //
    // Any program logic which changes this object's "value" property
    // should null out the "asList" property at the same time.
    public ListData asList() {
        if (asList == null)
            asList = new ListData(getString());
        return asList;
    }

    public SaveableData getEditable(boolean editable) {
        SimpleData result = getSimpleValue();
        result.setEditable(editable);
        return result;
    }
}
