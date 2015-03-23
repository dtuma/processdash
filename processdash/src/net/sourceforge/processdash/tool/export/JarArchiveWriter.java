// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.export.jarsurf.JarData;
import net.sourceforge.processdash.tool.export.jarsurf.Main;


public class JarArchiveWriter extends ZipArchiveWriter {

    JarData jarData = new JarData();



    protected ZipOutputStream createArchiveOutputStream(OutputStream out) throws IOException {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        attrs.putValue("Main-Class", Main.class.getName());

        return new JarOutputStream(out, mf);
    }

    protected void addingZipEntry(String path, String contentType) {
        jarData.setContentType(path, contentType);
        super.addingZipEntry(path, contentType);
    }

    public void finishArchive() throws IOException {
        writeJarData();
        writeJarSurfClassfiles();
        super.finishArchive();
    }

    private void writeJarData() throws IOException {
        jarData.setDefaultFile(defaultPath);

        zipOut.putNextEntry(new ZipEntry(Main.JARDATA_FILENAME.substring(1)));
        ObjectOutputStream objOut = new ObjectOutputStream(zipOut);
        objOut.writeObject(jarData);
        objOut.flush();
        zipOut.closeEntry();
    }

    private void writeJarSurfClassfiles() throws IOException {
        BufferedReader in = new BufferedReader
            (new InputStreamReader
             (getClass().getResourceAsStream("jarsurf/classes.txt")));
        String filename;
        while ((filename = in.readLine()) != null)
            writeClassFile(filename);
    }

    private void writeClassFile(String filename) throws IOException {
        InputStream in = JarArchiveWriter.class.getResourceAsStream(filename);
        if (in == null)
            throw new FileNotFoundException(filename);

        zipOut.putNextEntry(new ZipEntry(filename.substring(1)));
        int b;
        while ((b = in.read()) != -1)
            zipOut.write(b);
        zipOut.closeEntry();
    }

}
