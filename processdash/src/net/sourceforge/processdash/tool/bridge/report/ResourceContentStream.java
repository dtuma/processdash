// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
            FileUtils.copyFile(resourceContent, zipOut);
            resourceContent.close();
            zipOut.closeEntry();
        }

        zipOut.finish();
    }

}
