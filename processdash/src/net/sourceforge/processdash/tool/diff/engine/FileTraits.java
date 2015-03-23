// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import net.sourceforge.processdash.util.FileUtils;


public class FileTraits {

    public boolean binary;

    public boolean identical;

    public AccountingType changeType;

    public String initialContents;


    public static FileTraits examineFile(FileToAnalyze file, Charset charset)
            throws IOException {
        FileTraits result = new FileTraits();
        result.binary = false;
        result.identical = true;
        result.changeType = null;

        List versions = file.getVersions();
        InputStream a = file.getContents(versions.get(0));
        InputStream b = null;
        if (versions.size() > 1) {
            try {
                Object lastVersion = versions.get(versions.size() - 1);
                b = file.getContents(lastVersion);
            } catch (IOException ioe) {
                FileUtils.safelyClose(a);
                throw ioe;
            }
        }

        if (a == null) {
            result.identical = false;
            result.changeType = AccountingType.Added;
        } else if (b == null) {
            result.identical = false;
            result.changeType = AccountingType.Deleted;
        }

        Reader inA = getReader(a, charset);
        Reader inB = getReader(b, charset);
        StringBuilder contents = new StringBuilder();

        try {
            int charA = -2, charB = -2;
            while (true) {
                if (inA != null && charA != -1) charA = inA.read();
                if (inB != null && charB != -1) charB = inB.read();
                if (charA != charB) { result.identical = false; }
                if (charA == 0 || charB == 0) { result.binary = true; }
                if (charA < 0 && charB < 0) break;
                if (result.binary && !result.identical) break;
                collectContents(contents, charA, charB);
            }
        } finally {
            FileUtils.safelyClose(a);
            FileUtils.safelyClose(b);
        }

        if (versions.size() == 1) {
            result.identical = true;
            result.changeType = AccountingType.Base;
        }

        if (result.changeType == null) {
            if (result.identical)
                result.changeType = AccountingType.Base;
            else
                result.changeType = AccountingType.Modified;
        }

        result.initialContents = contents.toString();

        return result;
    }

    private static Reader getReader(InputStream in, Charset charset) {
        if (in == null)
            return null;
        else
            return new BufferedReader(new InputStreamReader(in, charset));
    }

    private static void collectContents(StringBuilder dest, int charA, int charB) {
        if (dest.length() < 1000) {
            int useChar = (charB == -2 ? charA : charB);
            if (useChar > 0)
                dest.append((char) useChar);
        }
    }

}
