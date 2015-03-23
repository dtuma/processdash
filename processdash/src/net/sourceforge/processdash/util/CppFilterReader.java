// Copyright (C) 2001-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.*;

public class CppFilterReader extends Reader {

    BufferedReader in;
    CppFilter cppFilter;
    String nextLine;
    int usedChars;

    public CppFilterReader(BufferedReader in) throws IOException {
        this(in, null);
    }

    public CppFilterReader(BufferedReader in, String preDefinitions)
        throws IOException
    {
        this.in = in;
        cppFilter = new CppFilter(in, preDefinitions);
        getNextLine();
    }

    private void getNextLine() throws IOException {
        String line = cppFilter.readLine();
        if (line == null) {
            nextLine = null;
        } else {
            nextLine = line + "\n";
            usedChars = 0;
        }
    }

    public void close() throws IOException {
        cppFilter.dispose();
        in.close();
        nextLine = null;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (nextLine == null)
            return -1;

        int numChars = Math.min(nextLine.length() - usedChars, len);
        nextLine.getChars(usedChars, usedChars + numChars, cbuf, off);
        usedChars += numChars;
        if (!(usedChars < nextLine.length()))
            getNextLine();

        return numChars;
    }

    public boolean ready() throws IOException {
        return nextLine != null;
    }

}
