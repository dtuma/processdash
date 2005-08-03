// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;


public class FileUtils {

    public static long computeChecksum(File file, Checksum verify) throws IOException {
        InputStream in = new BufferedInputStream(new CheckedInputStream
                (new FileInputStream(file), verify));
        int b;
        while ((b = in.read()) != -1)
            ; // do nothing
        in.close();

        return verify.getValue();
    }

    /** Utility routine: slurp an entire file from an InputStream. */
    public static byte[] slurpContents(InputStream in, boolean close)
        throws IOException
    {
        byte [] result = null;
        ByteArrayOutputStream slurpBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
            slurpBuffer.write(buffer, 0, bytesRead);
        result = slurpBuffer.toByteArray();
        if (close) try { in.close(); } catch (IOException ioe) {}
        return result;
    }

}
