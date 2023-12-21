// Copyright (C) 2001-2023 Tuma Solutions, LLC
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class LineFilteringReader extends Reader {

    BufferedReader in;
    StringMapper lineFilter;
    String nextLine;
    int usedChars;

    public LineFilteringReader(Reader in, StringMapper lineFilter)
            throws IOException {
        if (in instanceof BufferedReader)
            this.in = (BufferedReader) in;
        else
            this.in = new BufferedReader(in);

        this.lineFilter = lineFilter;
        getNextLine();
    }

    private void getNextLine() throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null) {
                nextLine = null;
                break;
            }

            line = lineFilter.getString(line);
            if (line == null)
                continue;
            else {
                nextLine = line + "\n";
                usedChars = 0;
                break;
            }
        }
    }

    @Override
    public void reset() throws IOException {
        in.reset();
        getNextLine();
    }

    @Override
    public void close() throws IOException {
        in.close();
        nextLine = null;
    }

    @Override
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

    @Override
    public boolean ready() throws IOException {
        return nextLine != null;
    }

}
