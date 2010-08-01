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

package net.sourceforge.processdash.tool.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.TempFileFactory;

/**
 * Archive writer that writes files directly to the filesystem.
 * 
 * @since 1.12.1.1
 */
public class DirArchiveWriter extends ZipArchiveWriter {

    public static final String DEST_DIR_SETTING = DirArchiveWriter.class
            .getName() + ".destDir";

    public static final String EMBED_HEADERS_SETTING = DirArchiveWriter.class
            .getName() + ".embedHttpHeaders";


    protected OutputStream out;
    protected File destDir;
    boolean embedHeaders;


    @Override
    public void finishArchive() throws IOException {
        out.write(destDir.getPath().getBytes("UTF-8"));
    }

    @Override
    protected void init(OutputStream outstream) throws IOException {
        super.init(outstream);
        out = outstream;
        destDir = createDestDir();
        embedHeaders = Settings.getBool(EMBED_HEADERS_SETTING, true);
    }

    private File createDestDir() throws IOException {
        String basePath = Settings.getVal(DEST_DIR_SETTING);
        if (basePath == null || basePath.trim().length() == 0)
            throw new IOException("No base directory configured");

        File baseDir = new File(basePath);
        if (!baseDir.isDirectory())
            throw new IOException("Directory does not exist: " + basePath);

        TempFileFactory tff = new TempFileFactory();
        tff.setTempDirectory(baseDir);
        tff.setMaxAgeDays(1);
        File result = tff.createTempDirectory("dirArchive_", ".tmp");
        return result;
    }

    @Override
    protected ZipOutputStream createArchiveOutputStream(OutputStream out) {
        return null;
    }

    @Override
    protected void writeEntry(String uri, String contentType, byte[] content,
            int offset) throws IOException {
        String safeURI = getSafeURI(uri, contentType);

        File dest = new File(destDir, safeURI);
        FileOutputStream out = new FileOutputStream(dest);

        if (embedHeaders) {
            String header = "Content-Type: " + contentType + "\r\n\r\n";
            out.write(header.getBytes(HTTPUtils.DEFAULT_CHARSET));
        }

        out.write(content, offset, content.length - offset);
        out.close();
    }

}
