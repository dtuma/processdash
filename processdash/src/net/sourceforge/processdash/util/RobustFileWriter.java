// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 - 2005 Software Process Dashboard Initiative
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RobustFileWriter extends OutputStreamWriter {

    public static final String OUT_PREFIX = RobustFileOutputStream.OUT_PREFIX;
    public static final String BACKUP_PREFIX = RobustFileOutputStream.BACKUP_PREFIX;

    RobustFileOutputStream outStream;

    public RobustFileWriter(String destFile) throws IOException {
        this(new RobustFileOutputStream(destFile));
    }

    public RobustFileWriter(File destFile) throws IOException {
        this(new RobustFileOutputStream(destFile));
    }

    protected RobustFileWriter(RobustFileOutputStream outStream)
            throws IOException {
        super(outStream);
        this.outStream = outStream;
    }

    public RobustFileWriter(String destFile, String encoding)
            throws IOException {
        this(new RobustFileOutputStream(destFile), encoding);
    }

    public RobustFileWriter(File destFile, String encoding) throws IOException {
        this(new RobustFileOutputStream(destFile), encoding);
    }

    protected RobustFileWriter(RobustFileOutputStream outStream, String encoding)
            throws IOException {
        super(outStream, encoding);
        this.outStream = outStream;
    }

    public void abort() throws IOException, IllegalStateException {
        outStream.abort();
    }

    public long getChecksum() {
        return outStream.getChecksum();
    }

}
