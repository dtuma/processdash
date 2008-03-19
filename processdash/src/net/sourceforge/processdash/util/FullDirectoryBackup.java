// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FullDirectoryBackup extends DirectoryBackup {

    @Override
    protected void doBackup(File outputZipFile) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(outputZipFile)));

        List<String> filenames = getFilenamesToBackup();
        if (filenames.size() == 0) {
            zipOut.putNextEntry(new ZipEntry("No_Files_Found"));
        } else {
            for (String filename : filenames) {
                backupFile(zipOut, filename);
            }
        }

        if (extraContentSupplier != null)
            extraContentSupplier.addExtraContentToBackup(zipOut);

        zipOut.close();
    }

    private void backupFile(ZipOutputStream zipOut, String filename)
            throws IOException {
        ZipEntry e = new ZipEntry(filename);
        File file = new File(srcDirectory, filename);
        e.setTime(file.lastModified());
        zipOut.putNextEntry(e);
        FileUtils.copyFile(file, zipOut);
        zipOut.closeEntry();
    }

}
