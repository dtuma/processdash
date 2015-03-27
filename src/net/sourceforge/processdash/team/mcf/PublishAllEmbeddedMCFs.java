// Copyright (C) 2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.process;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.util.FileUtils;

public class PublishAllEmbeddedMCFs {

    public static void main(String[] args) throws Exception {
        File zipFile = new File(args[0]);
        File dir = zipFile.getParentFile();

        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(zipFile)));
        ZipEntry e;
        while ((e = zipIn.getNextEntry()) != null) {
            if (isMcfSettingsFile(e))
                publishMcf(zipIn, dir);
        }
        zipIn.close();
    }

    private static boolean isMcfSettingsFile(ZipEntry e) {
        String name = e.getName();
        return name.startsWith("externalResources/mcf/")
                && name.endsWith("/settings.xml");
    }

    private static void publishMcf(ZipInputStream zipIn, File destDir)
            throws Exception {
        File tmpFile = File.createTempFile("settings", ".xml");
        FileUtils.copyFile(zipIn, tmpFile);
        GenerateProcess.main(new String[] { tmpFile.getPath(),
                destDir.getPath(), "do not exit" });
        tmpFile.delete();
    }

}
