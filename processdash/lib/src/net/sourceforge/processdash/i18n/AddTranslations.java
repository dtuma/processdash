// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

/** Loads translations from a user-contributed zipfile, and merges them with
 * translations that are already present in the dashboard source tree.
 */
public class AddTranslations extends MatchingTask {

    private boolean verbose = false;
    private File dir;

    public void setDir(File d) {
        dir = d;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void execute() throws BuildException {
        DirectoryScanner ds = getDirectoryScanner(dir);
        String[] srcFiles = ds.getIncludedFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            try {
                addTranslationsFromFile(new File(dir, srcFiles[i]));
            } catch (IOException ioe) {
                if (verbose)
                    ioe.printStackTrace(System.out);
                System.out.println("'" + srcFiles[i] + "' does not appear to "
                        + "be a valid translations zipfile - skipping.");
            }
        }
    }

    private void addTranslationsFromFile(File file) throws IOException {
        if (verbose)
            System.out.println("Looking for translations in " + file);

        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(file)));

        File jFreeFile = new File(dir, ("lib/jfreechart"
                + localeId(file.getName()) + ".zip"));
        Map jFreeRes = loadJFreeResources(jFreeFile);
        boolean jFreeModified = false;

        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            if (entry.isDirectory())
                continue;

            String filename = entry.getName();

            if (filename.indexOf("jrc-editor") != -1) {
                mergePropertiesFile(zipIn,
                        "l10n-tool/src/" + terminalName(filename));

            } else if (filename.indexOf("jfree") != -1) {
                if (mergeJFreeProperties(jFreeRes, zipIn, filename))
                    jFreeModified = true;

            } else if (filename.startsWith("Templates")
                    && filename.endsWith(".properties")) {
                mergePropertiesFile(zipIn, filename);

            } else if (!filename.equals("ref.zip")
                    && !filename.equals("save-tags.txt")) {
                System.out.println("Warning - unrecognized file '" + filename
                        + "'");
            }
        }

        if (jFreeModified)
            saveJFreeResources(jFreeFile, jFreeRes);

        zipIn.close();
    }

    private String localeId(String filename) {
        int underlinePos = filename.indexOf('_');
        int dotPos = filename.indexOf('.', underlinePos);
        return filename.substring(underlinePos, dotPos);
    }

    private String terminalName(String entryName) {
        int slashPos = entryName.lastIndexOf('/');
        if (slashPos == -1)
            slashPos = entryName.lastIndexOf('\\');
        if (slashPos == -1)
            return entryName;
        else
            return entryName.substring(slashPos + 1);
    }

    private Map loadJFreeResources(File jFreeFile) throws IOException {
        Map result = new HashMap();
        if (jFreeFile.exists()) {
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(
                    jFreeFile));
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String filename = entry.getName();
                if (filename.toLowerCase().endsWith(".properties")) {
                    Properties p = new SortedProperties();
                    p.load(zipIn);
                    if (!p.isEmpty())
                        result.put(filename, p);
                }
            }
            zipIn.close();
        }
        return result;
    }

    private boolean mergeJFreeProperties(Map jFreeRes, ZipInputStream zipIn,
            String filename) throws IOException {
        Properties incoming = new Properties();
        incoming.load(zipIn);

        Properties original = (Properties) jFreeRes.get(filename);
        if (original == null) original = new Properties();

        Properties merged = new SortedProperties();
        merged.putAll(original);
        merged.putAll(incoming);

        if (original.equals(merged)) {
            if (verbose)
                System.out.println("    No new properties in '" + filename + "'");
            return false;

        } else {
            if (verbose) System.out.print("    ");
            System.out.println("Updating '" + filename + "'");
            jFreeRes.put(filename, merged);
            return true;
        }
    }

    private void saveJFreeResources(File jFreeFile, Map jFreeRes) throws IOException {
        if (jFreeRes.isEmpty())
            return;

        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(
                jFreeFile));

        for (Iterator i = jFreeRes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String filename = (String) e.getKey();
            Properties p = (Properties) e.getValue();
            zipOut.putNextEntry(new ZipEntry(filename));
            p.store(zipOut, PROP_FILE_HEADER);
            zipOut.closeEntry();
        }

        zipOut.close();
    }

    private void mergePropertiesFile(ZipInputStream zipIn, String filename)
            throws IOException {
        Properties incoming = new Properties();
        incoming.load(zipIn);

        File destFile = new File(dir, filename);
        Properties original = new Properties();
        if (destFile.exists())
            original.load(new FileInputStream(destFile));

        Properties merged = new SortedProperties();
        merged.putAll(original);
        merged.putAll(incoming);

        if (original.equals(merged)) {
            if (verbose)
                System.out.println("    No new properties in '" + filename + "'");
            return;
        }

        if (verbose) System.out.print("    ");
        System.out.println("Updating '" + filename + "'");
        FileOutputStream out = new FileOutputStream(destFile);
        merged.store(out, PROP_FILE_HEADER);
        out.close();
    }

    private static final String PROP_FILE_HEADER =
        "Process Dashboard Resource Bundle";

    private class SortedProperties extends Properties {

        public synchronized Enumeration keys() {
            TreeSet keys = new TreeSet();
            for (Enumeration e = super.keys(); e.hasMoreElements();)
                keys.add(e.nextElement());
            return Collections.enumeration(keys);
        }

    }
}
