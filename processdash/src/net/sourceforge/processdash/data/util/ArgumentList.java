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

package net.sourceforge.processdash.data.util;

import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

class ArgumentList {

    private Vector args;
    private Enumeration enumeration;

    public ArgumentList(String arguments) {

        args = new Vector();

        StringTokenizer tok = new StringTokenizer(arguments, "\t");
        StringBuffer argument = new StringBuffer();
        boolean incompleteArgument = false;

        while (tok.hasMoreTokens()) {
            if (incompleteArgument) argument.append("\t");
            argument.append(tok.nextToken());

            if (completeValue(argument.toString())) {
                args.addElement(argument.toString());
                argument = new StringBuffer();
                incompleteArgument = false;
            } else
                incompleteArgument = true;
        }

        enumeration = args.elements();
    }

    public int size() { return args.size(); }
    public boolean hasMoreElements() { return enumeration.hasMoreElements(); }
    public String nextElement() throws NoSuchElementException {
        return (String) enumeration.nextElement();
    }

    public static int countChars(char[] chars, char c) {
        int count=0;

        for(int i=chars.length; i-- > 0;)
            if (chars[i] == c) count++;

        return count;
    }

    private boolean completeValue(String s) {
        // the argument is complete if the curly braces are balanced.
        char[] chars = s.toCharArray();

        return (countChars(chars, '{') == countChars(chars, '}'));
    }

}

