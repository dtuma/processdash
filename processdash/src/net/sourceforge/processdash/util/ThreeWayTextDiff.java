// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.ThreeWayDiff.ResultItem;

public class ThreeWayTextDiff {

    public static ThreeWayDiff.ResultItem<String>[] compareTextByWords(
            String baseText, String aText, String bText) {
        String[] baseWords = splitString(baseText);
        String[] aWords = splitString(aText);
        String[] bWords = splitString(bText);

        String[] baseNormalized = normalizeWords(baseWords);
        String[] aNormalized = normalizeWords(aWords);
        String[] bNormalized = normalizeWords(bWords);

        ThreeWayDiff<String> diff = new ThreeWayDiff<String>(baseNormalized,
                aNormalized, bNormalized);
        ResultItem<String>[] result = diff.getMergedResult();

        // now we need to perform a step where we denormalize the strings,
        // adding back the whitespace we removed earlier.
        for (int i = 0; i < result.length; i++) {
            ResultItem<String> item = result[i];

            // handle the change scenarios (add / delete) first.
            if (item.isInsertedByA())
                item.item = aWords[item.aPos];
            else if (item.isInsertedByB())
                item.item = bWords[item.bPos];
            else if (item.isDeleted())
                item.item = baseWords[item.basePos];

            // at this point, the item is "unmodified" by a and b, in an "ignore
            // whitespace" sense.  Use the whitespace version supplied by either
            // a or b, preferring a's if it was actually modified.
            else if (!aWords[item.aPos].equals(baseWords[item.basePos]))
                item.item = aWords[item.aPos];
            else
                item.item = bWords[item.bPos];
        }

        return result;
    }

    private static final String[] splitString(String s) {
        List<String> result = new ArrayList<String>();
        Matcher m = TOKEN_PATTERN.matcher(s);
        while (m.find())
            result.add(m.group());
        return result.toArray(new String[result.size()]);
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\s*\\S+");

    private static final String[] normalizeWords(String[] words) {
        String[] result = new String[words.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = words[i].trim();
        }
        return result;
    }

}
