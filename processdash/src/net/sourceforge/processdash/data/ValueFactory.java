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


import net.sourceforge.processdash.data.repository.CompiledFunction;
import net.sourceforge.processdash.data.repository.DataRepository;


public class ValueFactory {

    public static String regexpQuote(String s) {
        final String metachars = ".[]\\()?*+{}|^$";

        StringBuffer result = new StringBuffer();
        int length = s.length();
        char c ;

        for (int i=0;   i < length;   i++) {
            c = s.charAt(i);
            if (metachars.indexOf(c) == -1)
                result.append(c);
            else {
                result.append('\\');
                result.append(c);
            }
        }

        return result.toString();
    }



    public static SaveableData create
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        return createQuickly(name, value, r, prefix);
    }

    private static char flagChar(String s, int pos) {
        char result = s.charAt(pos);
        if (result == '?') result = s.charAt(pos+1);
        return result;
    }


    public static SaveableData createQuickly
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        if (value.charAt(0) == '#')
            return new FrozenData(name, value, r, prefix);
        else if (value.charAt(0) == '{')
            return new CompiledFunction(name, value, r, prefix);
        else
            return createSimple(value);
    }



    public static SimpleData createSimple(String value) throws
        MalformedValueException {

        if ("null".equals(value))
            return null;
        else {
            switch (flagChar(value, 0)) {
            case '@': return new DateData(value);
            case '"': return new StringData(value);
            case 'T': return TagData.getInstance();
            default:  return new DoubleData(value);
            }
        }
    }



    private static void debug(String s) {
        System.out.println("ValueFactory: "+s);
    }


}
