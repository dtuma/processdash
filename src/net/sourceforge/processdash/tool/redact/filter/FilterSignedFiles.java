// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.processdash.security.TamperDeterrent;
import net.sourceforge.processdash.tool.redact.RedactFilter;
import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.TempFileFactory;

public class FilterSignedFiles implements RedactFilter {

    @Override
    public Reader filter(RedactFilterData data, String filename,
            Reader contents) throws IOException {

        // see if this is a signed file. If not, return with no changes
        TamperDeterrent.FileType t;
        if (filename.startsWith("externalresources/")
                && filename.endsWith("/settings.xml"))
            t = TamperDeterrent.FileType.WBS;
        else if (SIGNED_FILES.contains(filename))
            t = TamperDeterrent.FileType.XML;
        else
            return contents;

        // save the data to a temporary file
        File tempFile = TempFileFactory.get().createTempFile("redact", ".tmp");
        Writer w = new BufferedWriter(new RobustFileWriter(tempFile, "UTF-8"));
        int b;
        while ((b = contents.read()) != -1)
            w.write(b);
        w.close();

        // sign the data, then read the signed content
        TamperDeterrent.getInstance().addThumbprint(tempFile, tempFile, t);
        return new InputStreamReader(new TempInputStream(tempFile), "UTF-8");
    }


    private class TempInputStream extends FilterInputStream {

        private File file;

        TempInputStream(File file) throws IOException {
            super(new BufferedInputStream(new FileInputStream(file)));
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            return checkEOF(super.read());
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return checkEOF(super.read(b, off, len));
        }

        private int checkEOF(int b) throws IOException {
            if (b == -1)
                close();
            return b;
        }

        @Override
        public void close() throws IOException {
            super.close();
            file.delete();
        }

    }


    private static final Set<String> SIGNED_FILES = Collections.unmodifiableSet(
        new HashSet(Arrays.asList("users.dat", "roles.dat", "groups.dat")));

}
