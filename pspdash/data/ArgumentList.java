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

import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

class ArgumentList {

    private Vector args;
    private Enumeration enum;

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

        enum = args.elements();
    }

    public int size() { return args.size(); }
    public boolean hasMoreElements() { return enum.hasMoreElements(); }
    public String nextElement() throws NoSuchElementException {
        return (String) enum.nextElement();
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

