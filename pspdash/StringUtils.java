/*
StringUtils.java : A stand alone utility class.
Copyright (C) 2000 Justin P. McCarthy

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

To further contact the author please email jpmccar@gjt.org

*/
/*
    changes:
        Sep 18, created
*/
//package com.justinsbrain.gnu.util;
package pspdash;

import java.util.ArrayList;

/**
    Just some utility methods for working with Strings.
    @author Justin P. McCarthy
*/
public class StringUtils
{
    /**
        So no one can create one of me.
    */
    private StringUtils()
    {
    }

    /**
        This uses the Character.isLetterOrDigit method to
        determine whether the character needs to be escaped.
        If it does then it is precedded by a slash('\').

        @param toEscape The string to escape characters in.
        @return The new, escaped, string.
    */
    public static final String escapeString(String toEscape)
    {
        StringBuffer toReturn = new StringBuffer();

        for (int i = 0; i < toEscape.length(); i++)
        {
            if (!Character.isLetterOrDigit(toEscape.charAt(i)))
            {
                toReturn.append('\\');
            }
            toReturn.append(toEscape.charAt(i));
        }

        return toReturn.toString();
    }


    /**
        A simpl find and replace.
        @param text The original string.
        @param find The text to look for.
        @param replace What to replace the found text with.
        @return The new string.
    */
    public static final String findAndReplace(
        String text,
        String find,
        String replace)
    {
        if ((text == null) ||
            (find == null) ||
            (replace == null))
        {
            throw new NullPointerException(
                "findAndReplace doesn't work on nulls.");
        }

        // handle degenerate case: if no replacements need to be made,
        // return the original text unchanged.
        int replaceStart = text.indexOf(find);
        if (replaceStart == -1) return text;

        int findLength = find.length();
        StringBuffer toReturn = new StringBuffer();

        while (replaceStart != -1)
        {
            toReturn.append(text.substring(0, replaceStart)).append(replace);
            text = text.substring(replaceStart+findLength);
            replaceStart = text.indexOf(find);
        }

        toReturn.append(text);
        return toReturn.toString();

    }

    /**
        Breaks a string down to chunks of a given size or less.
        It will try and break chunks on standard symbols like the space,
        tab, and enter.  If it can't do it there it will try other
        characters frequently found but not usually in a word like a colon,
        semi-colon, parens, comma, etc...
        @param toBreakApart The big String.
        @param breakPoint The number of characters not to exceed on a line.
        @param shouldTrim Whether this should trim beginning and ending
            whitespace
        @return The array of little strings.
        @throws NullPointerException if the string to break apart is null.
        @throws IllegalArgumentException if the int break point is less than 1
    */
    public static final String[] breakDownString(
        String toBreakApart,
        int breakPoint,
        boolean shouldTrim)
    {
        // can't break apart a String that doesn't exist!
        if (toBreakApart == null)
        {
            throw new NullPointerException(
                "breakDownString can't break down nulls!");
        }

        // can't break strings on smaller than one character chunks.
        if (breakPoint < 1)
        {
            throw new IllegalArgumentException(
                "breakDownString can't break them smaller than one character!");
        }

        // if the string to break apart is smaller than what is needed
        // stop processing now
        if (toBreakApart.length() <= breakPoint)
        {
            return new String[] { toBreakApart };
        }

        StringBuffer sb = new StringBuffer(toBreakApart);
        ArrayList workArray = new ArrayList();
        int pointer = 0;

        // forever...
        while (true)
        {
            // if the string buffer is now less than the break point
            // add what's left to the working array and break
            // out of it all..
            if (sb.length() < breakPoint)
            {
                if (shouldTrim)
                {
                    workArray.add(sb.toString().trim());
                }
                else
                {
                    workArray.add(sb.toString());
                }
                break;
            }
            else
            {
                // else run through the string from the break point
                // down to zero and look for something good to break on
                // I use java.lang.Character's isLetterOrDigit(char) method
                // do determine if it is a good place to break.
                for (pointer = breakPoint; pointer > 0; pointer--)
                {
                    if (!Character.isLetterOrDigit(sb.charAt(pointer)))
                    {
                        // found something good to break on.
                        // add everything before it to the array,
                        // delete everything added from the stringbuffer
                        // and break out of this inner loop.
                        if (shouldTrim)
                        {
                            workArray.add(sb.substring(0, pointer).trim());
                        }
                        else
                        {
                            workArray.add(sb.substring(0, pointer));
                        }
                        sb.delete(0,pointer);
                        break;
                    }
                }

                // if pointer is 0 then we got all the way through without
                // find a good character to break on.  we'll go ahead
                // and take a full length line.
                if (pointer == 0)
                {
                    workArray.add(sb.substring(0, breakPoint));
                    sb.delete(0,breakPoint);
                }
            }
        }

        // if the last element of the array is nothing but blanks remove it...
        if (((String)workArray.get(workArray.size() - 1 )).trim().length() == 0)
        {
            workArray.remove(workArray.size() - 1);
        }

        // this is a strange method call in ArrayList.
        // you pass it the run-time type of array you want back from your
        // array list and if the objects can be cast correctly.  voila!
        return (String[])workArray.toArray(new String[0]);
    }

    /** Performs the equivalent of buf.toString().indexOf(str), but is
     *  much more memory efficient. */
    public static int indexOf(StringBuffer buf, String str) {
        return indexOf(buf, str, 0);
    }
    /** Performs the equivalent of buf.toString().indexOf(str, fromIndex),
     *  but is much more memory efficient. */
    public static int indexOf(StringBuffer buf, String str, int fromIndex) {
        int count = buf.length();
        int max = count - str.length();
        if (fromIndex >= count) {
            if (count == 0 && fromIndex == 0 && str.length() == 0)
                return 0;
            else
                return -1;
        }
        if (fromIndex < 0) fromIndex = 0;
        if (str.length() == 0) return fromIndex;

        char first = str.charAt(0);
        int i = fromIndex;

    startSearchForFirstChar:
        while (true) {
            /* Look for first character. */
            while (i <= max && buf.charAt(i) != first)
                i++;
            if (i > max)
                return -1;

            /* Found first character, now look at the rest of str */
            int j = i + 1;
            int end = j + str.length() - 1;
            int k = 1;
            while (j < end) {
                if (buf.charAt(j++) != str.charAt(k++)) {
                    i++;
                    /* Look for str's first char again. */
                    continue startSearchForFirstChar;
                }
            }
            return i; /* Found whole string. */
        }
    }

    /** Performs the equivalent of buf.toString().lastIndexOf(str), but is
     *  much more memory efficient. */
    public static int lastIndexOf(StringBuffer buf, String str) {
        return lastIndexOf(buf, str, buf.length());
    }

    /** Performs the equivalent of buf.toString().lastIndexOf(str, fromIndex),
     *  but is much more memory efficient. */
    public static int lastIndexOf(StringBuffer buf, String str, int fromIndex) {
        int count = buf.length();
        int rightIndex = count - str.length();
        if (fromIndex < 0) return -1;
        if (fromIndex > rightIndex) fromIndex = rightIndex;

        /* Empty string always matches. */
        if (str.length() == 0) return fromIndex;

        int strLastIndex = str.length() - 1;
        char strLastChar = str.charAt(strLastIndex);
        int min = str.length() - 1;
        int i = min + fromIndex;

    startSearchForLastChar:
        while (true) {
            /* Look for the last character */
            while (i >= min && buf.charAt(i) != strLastChar)
                i--;
            if (i < min) return -1;

            /* Found last character, now look at the rest of str. */
            int j = i - 1;
            int start = j - (str.length() - 1);
            int k = strLastIndex - 1;

            while (j > start) {
                if (buf.charAt(j--) != str.charAt(k--)) {
                    i--;
                    /* Look for str's last char again. */
                    continue startSearchForLastChar;
                }
            }

            return start + 1;    /* Found whole string. */
        }
    }

}
