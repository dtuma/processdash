// Copyright (C) 2006-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

    private static final String BROKEN_SUFFIX = "!broken";
    private boolean verbose = false;
    private File dir;
    private String externalDirs;

    public void setDir(File d) {
        dir = d;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setExternalDirs(String externalDirs) {
        this.externalDirs = externalDirs;
    }

    public void execute() throws BuildException {
        Map<String, String> externalFiles = getExternalFileLocations();

        DirectoryScanner ds = getDirectoryScanner(dir);
        String[] srcFiles = ds.getIncludedFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            try {
                addTranslationsFromFile(new File(dir, srcFiles[i]),
                    externalFiles);
            } catch (IOException ioe) {
                if (verbose)
                    ioe.printStackTrace(System.out);
                System.out.println("'" + srcFiles[i] + "' does not appear to "
                        + "be a valid translations zipfile - skipping.");
            }
        }
    }

    private Map<String, String> getExternalFileLocations() {
        Map<String, String> result = new HashMap();
        if (externalDirs == null)
            return result;

        for (String oneDirName : externalDirs.split(",")) {
            oneDirName = oneDirName.trim();
            File oneDir = calcPossiblyRelativeFile(oneDirName);
            try {
                oneDir = oneDir.getCanonicalFile();
            } catch (IOException e) {
                throw new BuildException(e);
            }
            oneDir = new File(oneDir, "Templates/resources");
            if (!oneDir.isDirectory()) {
                System.out.println("Directory '" + oneDir
                        + "' does not exist - skipping.");
                continue;
            }
            for (String oneFile : oneDir.list()) {
                if (oneFile.endsWith(".properties")
                        && oneFile.indexOf('_') == -1) {
                    String baseName = oneFile.substring(0,
                        oneFile.length() - 11);
                    result.put(baseName, oneDir.getPath() + File.separatorChar);
                }
            }
        }
        return result;
    }

    private void addTranslationsFromFile(File file,
            Map<String, String> externalDirs) throws IOException {
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
            String basename = baseName(filename);

            if (filename.indexOf("jrc-editor") != -1) {
                mergePropertiesFile(zipIn,
                        "l10n-tool/src/" + terminalName(filename));

            } else if (externalDirs.containsKey(basename)) {
                mergePropertiesFile(zipIn, externalDirs.get(basename)
                        + terminalName(filename));

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

    private String baseName(String entryName) {
        String result = terminalName(entryName);
        int pos = result.indexOf('_');
        if (pos == -1)
            pos = result.indexOf('.');
        return (pos == -1 ? result : result.substring(0, pos));
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
        if (JFREE_EXISTING_LANGUAGES.contains(localeId(filename).substring(1)))
            return false;
        filename = maybeRenameJFreeFile(filename);

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

    private String maybeRenameJFreeFile(String filename) {
        for (int i = 0; i < JFREE_FILE_RENAMES.length; i++) {
            String oldPrefix = JFREE_FILE_RENAMES[i][0];
            String newPrefix = JFREE_FILE_RENAMES[i][1];
            if (filename.startsWith(oldPrefix)) {
                return newPrefix + filename.substring(oldPrefix.length());
            }
        }
        return filename;
    }
    private static final String[][] JFREE_FILE_RENAMES = {
        { "org/jfree/chart/ui/LocalizationBundle",
            "org/jfree/chart/editor/LocalizationBundle"}
    };

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

        File destFile = calcPossiblyRelativeFile(filename);
        Properties original = loadProperties(destFile);

        Properties merged = new SortedProperties();
        merged.putAll(original);

        for (Iterator i = incoming.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (key.endsWith(BROKEN_SUFFIX)) {
                String intendedKey = key.substring(0, key.length()
                        - BROKEN_SUFFIX.length());
                if (original.containsKey(intendedKey))
                    continue;
            }
            merged.put(key, value);
        }

        File englishFile = new File(dir, getEnglishFilename(filename));
        Properties english = loadProperties(englishFile);
        for (Iterator i = english.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            String englishValue = (String) e.getValue();
            String mergedValue = merged.getProperty(key);
            if (englishValue.equals(mergedValue)
                    && valueNeedsNoTranslation(englishValue))
                merged.remove(key);
        }

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

    private File calcPossiblyRelativeFile(String path) {
        File result = new File(path);
        if (!result.isAbsolute())
            result = new File(dir, path);
        return result;
    }

    private Properties loadProperties(File f) throws IOException {
        Properties result = new Properties();
        if (f.isFile()) {
            FileInputStream in = new FileInputStream(f);
            result.load(in);
            in.close();
        }
        return result;
    }

    private String getEnglishFilename(String filename) {
        while (true) {
            int underscorePos = filename.lastIndexOf('_');
            if (underscorePos == -1)
                return filename;

            int dotPos = filename.indexOf('.', underscorePos);
            if (dotPos == -1)
                return filename;

            filename = filename.substring(0, underscorePos)
                    + filename.substring(dotPos);
        }
    }

    private boolean valueNeedsNoTranslation(String value) {
        value = removeVariables(value);
        return valueContainsNoCharacters(value);
    }

    private String removeVariables(String value) {
        while (true) {
            int beg = value.indexOf("${");
            if (beg == -1) beg = value.indexOf('{');
            if (beg == -1) return value;

            int end = value.indexOf("}", beg);
            if (end == -1) return value;

            value = value.substring(0, beg) + value.substring(end + 1);
        }
    }

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private boolean valueContainsNoCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (ALPHA.indexOf(Character.toUpperCase(value.charAt(i))) != -1)
                return false;
        }
        return true;
    }

    private static final String PROP_FILE_HEADER =
        "Process Dashboard Resource Bundle";

    private static final Set<String> JFREE_EXISTING_LANGUAGES = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList(
                "de", "es", "fr", "it", "nl", "pl", "pt", "ru", "zh")));

    private class SortedProperties extends Properties {

        public synchronized Enumeration keys() {
            TreeSet keys = new TreeSet();
            for (Enumeration e = super.keys(); e.hasMoreElements();)
                keys.add(e.nextElement());
            return Collections.enumeration(keys);
        }

    }
}
