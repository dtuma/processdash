// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.FileUtils;


/**
 * A report which generates a ZIP archive of the resources in a collection.
 */
public class ResourceContentStream implements CollectionReport {

    public static final ResourceContentStream INSTANCE = new ResourceContentStream();

    public static final String MANIFEST_FILENAME = "manifest.xml";

    public String getContentType() {
        return "application/zip";
    }

    public void runReport(ResourceCollection c, List<String> resources,
            OutputStream out) throws IOException {

        ZipOutputStream zipOut = new ZipOutputStream(out);
        zipOut.setLevel(9);

        ZipEntry e = new ZipEntry(MANIFEST_FILENAME);
        zipOut.putNextEntry(e);
        XmlCollectionListing.INSTANCE.runReport(c, resources, zipOut);
        zipOut.closeEntry();

        for (String resourceName : resources) {
            long lastMod = c.getLastModified(resourceName);
            if (lastMod < 1)
                continue;

            Long checksum = c.getChecksum(resourceName);
            if (checksum == null)
                continue;

            e = new ZipEntry(resourceName);
            e.setTime(lastMod);
            zipOut.putNextEntry(e);
            InputStream resourceContent = c.getInputStream(resourceName);
            try {
                FileUtils.copyFile(resourceContent, zipOut);
            } finally {
                resourceContent.close();
            }
            zipOut.closeEntry();
        }

        zipOut.finish();
    }

}
