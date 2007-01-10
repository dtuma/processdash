// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

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
