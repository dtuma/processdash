// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XorOutputStream;

public class RedactFilterer {

    public static final String REDACTED_PDASH_BACKUP_EXTENSION = "rpdbk";

    private Set<String> filterIDs;

    RedactFilterData data;

    private List<RedactFilter> filters;

    public RedactFilterer(Set<String> filterIDs) {
        this.filterIDs = filterIDs;
    }


    public void doFilter(File src, File dest) throws IOException {
        OutputStream out = new XorOutputStream(new BufferedOutputStream(
                new FileOutputStream(dest)),
                CompressedInstanceLauncher.PDASH_BACKUP_XOR_BITS);
        doFilter(src, out);
        out.close();
    }

    public void doFilter(File src, OutputStream dest) throws IOException {

        // Create an object to capture data about the filter operation
        ZipFile srcZip = new ZipFile(src);
        data = new RedactFilterData(srcZip, filterIDs);

        // Create standard and custom helpers for the filtering process
        RedactFilterHelperInit.createHelpers(data);

        // Create the set of filters we will use to transform the ZIP
        filters = RedactFilterUtils.getExtensions(data, "redact-filter");

        // Create the stream for the output ZIP
        ZipOutputStream out = new ZipOutputStream(dest);

        // filter entries from src to dest
        Enumeration<? extends ZipEntry> entries = srcZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry srcEntry = entries.nextElement();
            String filename = srcEntry.getName();

            if (filename.toLowerCase().endsWith(
                PersonMapper.PDASH_FILENAME_SUFFIX)) {
                filterPdashFile(srcEntry, filename, out);

            } else if (UNFILTERED_FILES.contains(filename)
                    || srcEntry.getSize() == 0) {
                copyUnfilteredEntry(srcEntry, out);

            } else {
                filterZipEntry(srcEntry, data.getFile(srcEntry), "", out);
            }
        }

        // clean up
        out.finish();
        srcZip.close();
    }

    private void filterPdashFile(ZipEntry srcEntry, String filename,
            ZipOutputStream out) throws IOException {

        // decide what filename to use for this PDASH file in the output ZIP.
        String destFilename;
        if (data.isFiltering(RedactFilterIDs.PEOPLE))
            destFilename = PersonMapper.renameZipEntry(filename);
        else
            destFilename = filename;

        // start a new ZIP entry in the output ZIP for this PDASH file
        ZipEntry destEntry = new ZipEntry(destFilename);
        destEntry.setTime(srcEntry.getTime());
        out.putNextEntry(destEntry);
        ZipOutputStream pdashOut = new ZipOutputStream(out);

        // read through the entries in the src PDASH file
        ZipInputStream pdashIn = new ZipInputStream(data.getStream(srcEntry));
        ZipEntry pdashEntry;
        String filenamePrefix = filename + "!";
        while ((pdashEntry = pdashIn.getNextEntry()) != null) {
            // process this PDASH entry from src to dest
            filterZipEntry(pdashEntry, new InputStreamReader(pdashIn, "UTF-8"),
                filenamePrefix, pdashOut);
        }

        // clean up the streams
        pdashIn.close();
        pdashOut.finish();
    }

    private void copyUnfilteredEntry(ZipEntry srcEntry, ZipOutputStream zipOut)
            throws IOException {
        // create a new entry in the dest ZIP
        String filename = srcEntry.getName();
        ZipEntry destEntry = new ZipEntry(filename);
        destEntry.setTime(srcEntry.getTime());
        zipOut.putNextEntry(destEntry);

        // copy bytes from src to dest
        InputStream in = data.getStream(srcEntry);
        FileUtils.copyFile(in, zipOut);
        zipOut.closeEntry();
        in.close();
    }


    private void filterZipEntry(ZipEntry srcEntry, Reader content,
            String filenamePrefix, ZipOutputStream zipOut) throws IOException {
        // ask our filters to process this file.
        String filename = srcEntry.getName();
        content = filterFile(filenamePrefix + filename, content);

        // if the filters deleted the file, don't add it to the dest ZIP.
        if (content == null)
            return;

        // otherwise, create a new entry in the dest ZIP
        ZipEntry destEntry = new ZipEntry(filename);
        destEntry.setTime(srcEntry.getTime());
        zipOut.putNextEntry(destEntry);

        // copy characters from src to dest
        Writer w = new OutputStreamWriter(zipOut, "UTF-8");
        char[] cbuf = new char[1024];
        int numChars;
        while ((numChars = content.read(cbuf)) > 0)
            w.write(cbuf, 0, numChars);

        // finish out the destination entry
        w.flush();
        zipOut.closeEntry();
    }

    private Reader filterFile(String filename, Reader content)
            throws IOException {
        filename = filename.toLowerCase();
        for (RedactFilter f : filters) {
            content = f.filter(data, filename, content);
            if (content == null)
                break;
        }
        return content;
    }

    private static final Set<String> UNFILTERED_FILES = Collections
            .unmodifiableSet(new HashSet(Arrays.asList("icon.ico")));

    public static void setDashboardContext(DashboardContext ctx) {
        TemplateInfo.setDashboardContext(ctx);
    }

}
