// Copyright (C) 2010 Tuma Solutions, LLC
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class UUIDFile {

    private static final String ENCODING = "UTF-8";

    public static String getIdentifier(File file) throws IOException {
        File directory = file.getParentFile();
        if (directory == null)
            throw new IOException("No directory specified: '" + file + "'");
        if (!directory.isDirectory())
            throw new FileNotFoundException(directory.toString());
        if (file.isDirectory())
            throw new IOException("Not a plain file: '" + file + "'");

        String result = null;

        if (file.isFile())
            result = readIdentifier(file);

        if (result == null)
            result = assignAndWriteIdentifier(file);

        return result;
    }

    private static String readIdentifier(File f) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), ENCODING));
        String result = in.readLine();
        in.close();
        return result;
    }

    private static String assignAndWriteIdentifier(File f) throws IOException {
        UUID uuid = UUID.randomUUID();
        String identifier = uuid.toString();
        RobustFileWriter out = new RobustFileWriter(f);
        try {
            out.write(identifier + "\n");
            out.close();
            return identifier;
        } catch (IOException ioe) {
            out.abort();
            throw ioe;
        }
    }

}
