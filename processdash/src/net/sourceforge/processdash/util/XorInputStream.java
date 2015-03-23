// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XorInputStream extends FilterInputStream {

    private int xorBits;

    public XorInputStream(InputStream in, int xorBits) {
        super(in);
        this.xorBits = xorBits;
    }

    public int read() throws IOException {
        int result = super.read();
        if (result != -1)
            result = frob(result);
        return result;
    }

    public int read(byte b[], int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead > 0)
            for (int i = off + bytesRead; i-- > off;)
                b[i] = frob(b[i]);
        return bytesRead;
    }

    private byte frob(int b) {
        return (byte) ((b ^ xorBits) & 0xff);
    }
}
