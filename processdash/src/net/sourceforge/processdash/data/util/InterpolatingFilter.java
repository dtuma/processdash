// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

import java.io.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;


import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.repository.DataRepository;


public class InterpolatingFilter extends PushbackInputStream {

    DataRepository data;
    String prefix;
    HashMap localDefs;
    boolean ignoreNulls = false;

    public InterpolatingFilter(InputStream i, DataRepository data,
                               String prefix) {
        super(i, 1024);
        this.data = data;
        this.prefix = prefix;
        this.localDefs = defaultLocalDefs();
    }

    public void setLocalVar(String varName, String value) {
        localDefs.put(varName, value);
    }
    public void setIgnoreNulls(boolean i) { ignoreNulls = i; }


    private static final int MAX_NAME_LEN = 200;
    private byte[] NAME_BUF = new byte[MAX_NAME_LEN];
    private boolean sawBinaryData = false;

    public int read() throws IOException {
        int c = super.read();
        if (sawBinaryData) return c;
        if (c == 0 || c > 127) sawBinaryData = true;
        if (c != '[') return c;

        c = super.read();
        if (c != '[') { unread(c); return '['; }

        // at this point, we've read "[[" - the possible start of an
        // identifier name.

        boolean almostThere = false;

        for (int pos = 0;   pos < MAX_NAME_LEN;  pos++) {
            NAME_BUF[pos] = (byte) (c = super.read());

            // If we reached the end of the stream, abort.
            if (c == -1) { pos--; break; }

            // If we see any unusual characters, abort.
            if (c == '[') break;

            if (almostThere) {
                if (c == ']') {
                    // okay, we're there! We just read ']]' - interpolate
                    // the variable name we found.
                    interpolateVariable(new String(NAME_BUF, 0, pos-1));
                    return super.read();
                } else {
                    // the preceeding ']' was not immediately followed by
                    // another ']', so this must have been a false alarm.
                    break;
                }
            } else
                if (c == ']') almostThere = true;
        }

        // The attempt to interpolate a variable was unsuccessful. Undo the
        // damage and return.
        unread(NAME_BUF, 0, pos+1);
        unread('[');
        return '[';
    }

    private void interpolateVariable(String varName) throws IOException {
        String value = lookupValue(varName);
        if (value == null) {
            if (ignoreNulls) return;
            unread(']'); unread(']');
            unread(varName.getBytes());
            unread('['); unread('[');
        } else
            unread(value.getBytes());
    }

    // names of special variables
    public static final String PREFIX_VAR = "Project/Task";
    public static final String TODAY_VAR  = "Today";
    public static final String NOW_VAR    = "Now";

    private String lookupValue(String varName) {
        // See if there is a local setting for this variable.
        String result = (String) localDefs.get(varName);

        if (result == null)
            // failing that, look in the data repository.
            result = lookupRepositoryValue(varName);

        return result;
    }
    private String lookupRepositoryValue(String dataName) {
        SaveableData s = data.getInheritableValue(prefix, dataName);
        if (s == null) return null;
        SimpleData sd = s.getSimpleValue();
        if (sd == null) return null;
        return sd.format();
    }
    private HashMap defaultLocalDefs() {
        HashMap result = new HashMap();
        Date now = new Date();
        result.put(TODAY_VAR,  dateFormatter.format(now));
        result.put(NOW_VAR,    dateTimeFormatter.format(now));
        result.put(PREFIX_VAR, prefix);
        return result;
    }
    private static DateFormat dateFormatter =
        DateFormat.getDateInstance(DateFormat.MEDIUM);
    private static DateFormat dateTimeFormatter =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);


    // The routines below exist solely to ensure that all reads go through
    // our method above.
    public int read(byte[] b) throws IOException {
        if (sawBinaryData) return super.read(b);
        return read(b, 0, b.length);
    }
    public int read(byte[] b, int off, int len) throws IOException {
        if (sawBinaryData) return super.read(b, off, len);
        if ((off < 0) || (len < 0) || (off + len) > b.length)
            throw new IndexOutOfBoundsException();
        else if (len == 0)
            return 0;

        int c = read();
        if (c == -1) return -1;
        b[off] = (byte)c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                if ((c = read()) == -1) break;
                b[off + i] = (byte)c;
            }
        } catch (IOException ioe) { }
        return i;
    }
    public long skip(long n) throws IOException {
        if (sawBinaryData) return super.skip(n);
        long count = 0;
        while (count < n) {
            if (read() == -1) return count;
            count++;
        }
        return count;
    }
}
