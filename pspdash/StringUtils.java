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

        int findLength = find.length();
        int replaceStart;
        StringBuffer toReturn = new StringBuffer();

        replaceStart = text.indexOf(find);
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
}
