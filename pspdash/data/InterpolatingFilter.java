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

import java.io.*;
import pspdash.StringUtils;


public class InterpolatingFilter extends PushbackInputStream {

    DataRepository data;
    String prefix;

    public InterpolatingFilter(InputStream i, DataRepository data,
                               String prefix) {
        super(i, 1024);
        this.data = data;
        this.prefix = prefix;
    }

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

    // names of special variables
    public static final String PREFIX = "Project/Task";

    private void interpolateVariable(String varName) throws IOException {
        String dataName = StringUtils.findAndReplace(varName, "\n", "");
        if (PREFIX.equals(dataName)) {
            unread(prefix.getBytes()); return;
        }
        SaveableData s = data.getInheritableValue(prefix, dataName);
        if (s == null) { fail(varName); return; }
        SimpleData sd = s.getSimpleValue();
        if (sd == null) { fail(varName); return; }
        String str = sd.format();
        unread(str.getBytes());
    }
    private void fail(String varName) throws IOException {
        unread(']'); unread(']');
        unread(varName.getBytes());
        unread('['); unread('[');
    }


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
