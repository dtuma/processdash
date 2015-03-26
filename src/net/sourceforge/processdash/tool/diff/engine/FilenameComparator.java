// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FilenameComparator implements Comparator<FilenameComparable> {

    private boolean ignoreCase;

    private List<String> headerSuffixes;

    public FilenameComparator(boolean ignoreCase, List<String> headerSuffixes) {
        this.ignoreCase = ignoreCase;
        this.headerSuffixes = headerSuffixes;
    }

    public FilenameComparator(boolean ignoreCase, String... headerSuffixes) {
        this(ignoreCase, Arrays.asList(headerSuffixes));
    }

    public int compare(FilenameComparable a, FilenameComparable b) {
        int result = cmp(a.getDirectory(), b.getDirectory());
        if (result != 0)
            return result;

        result = cmp(a.getFile(), b.getFile());
        if (result != 0)
            return result;

        int aHeader = headerSuffixes.indexOf(a.getSuffix());
        int bHeader = headerSuffixes.indexOf(b.getSuffix());
        if (aHeader == -1) {
            if (bHeader == -1)
                return cmp(a.getSuffix(), b.getSuffix());
            else
                return +1;
        } else {
            if (bHeader == -1)
                return -1;
            else
                return aHeader - bHeader;
        }
    }

    private int cmp(String a, String b) {
        if (ignoreCase)
            return a.compareToIgnoreCase(b);
        else
            return a.compareTo(b);
    }

    public static final FilenameComparator DEFAULT = new FilenameComparator(
            true, ".ads", ".h", ".hh", ".hxx", ".h++", ".hpp");

}
