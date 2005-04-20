// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Foundation, Inc., 59 Temple Place -Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.*;

public class CppFilterReader extends Reader {

    BufferedReader in;
    CppFilter cppFilter;
    String nextLine;

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
        } else
            nextLine = line + "\n";
    }

    public void close() throws IOException {
        cppFilter.dispose();
        in.close();
        nextLine = null;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (nextLine == null)
            return -1;

        int numChars = Math.min(nextLine.length(), len);
        nextLine.getChars(0, numChars, cbuf, off);
        if (nextLine.length() > numChars)
            nextLine = nextLine.substring(numChars);
        else
            getNextLine();

        return numChars;
    }

    public boolean ready() throws IOException {
        return nextLine != null;
    }

}
