// Copyright (C) 2005-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

public class FileUtils {

    public static long computeChecksum(File file, Checksum verify)
            throws IOException {
        InputStream in = new BufferedInputStream(new CheckedInputStream(
                new FileInputStream(file), verify));
        while (in.read() != -1)
            ; // do nothing
        in.close();

        return verify.getValue();
    }

    /** Utility routine: slurp an entire file from an InputStream. */
    public static byte[] slurpContents(InputStream in, boolean close)
            throws IOException {
        byte[] result = null;
        ByteArrayOutputStream slurpBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
            slurpBuffer.write(buffer, 0, bytesRead);
        result = slurpBuffer.toByteArray();
        if (close)
            try {
                in.close();
            } catch (IOException ioe) {
            }
        return result;
    }

    public static void copyFile(File src, File dest) throws IOException {
        InputStream inputStream = new FileInputStream(src);
        copyFile(inputStream, dest);
        inputStream.close();
    }

    public static void copyFile(InputStream src, File dest) throws IOException {
        OutputStream outputStream = new FileOutputStream(dest);
        copyFile(src, outputStream);
        outputStream.close();
    }

    public static void copyFile(File src, OutputStream dest) throws IOException {
        InputStream inputStream = new FileInputStream(src);
        copyFile(inputStream, dest);
        inputStream.close();
    }

    public static void copyFile(InputStream src, OutputStream dest) throws IOException {
        BufferedInputStream in = new BufferedInputStream(src);
        BufferedOutputStream out = new BufferedOutputStream(dest);
        int c;
        while ((c = in.read()) != -1)
            out.write(c);
        out.flush();
    }

    public static void deleteDirectory(File dir) throws IOException {
        deleteDirectory(dir, false);
    }

    public static void deleteDirectory(File dir, boolean recurse)
            throws IOException {
        if (!dir.isDirectory())
            return;

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(".") || files[i].getName().equals(".."))
                continue;
            else if (files[i].isDirectory() && recurse)
                deleteDirectory(files[i], recurse);
            else
                files[i].delete();
        }

        dir.delete();
    }

    /** Create a tweaked version of the string that should be ultra-safe to
     * use as part of a filename.
     */
    public static String makeSafe(String s) {
        if (s == null) s = "";
        s = s.trim();
        // perform a round-trip through the default platform encoding.
        s = new String(s.getBytes());

        StringBuffer result = new StringBuffer(s);
        for (int i = result.length();   i-- > 0; )
            if (-1 == ULTRA_SAFE_CHARS.indexOf(result.charAt(i)))
                result.setCharAt(i, '_');

        return result.toString();
    }
    private static final String ULTRA_SAFE_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "0123456789" + "_" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Create a tweaked version of a string that should be a legal filename,
     * discarding as little data as possible even if that means sacrificing
     * some legibility.
     */
    public static String makeSafeIdentifier(String s) {
        // begin by URL-encoding the filename.  This will clean up a multitude
        // of unsafe characters.
        StringBuffer result = new StringBuffer();
        try {
            result.append(URLEncoder.encode(s.trim(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // can't happen
        }

        // represent forward and backward slash characters with a comma.
        StringUtils.findAndReplace(result, "%2f", ",");
        StringUtils.findAndReplace(result, "%2F", ",");
        StringUtils.findAndReplace(result, "%5c", ",");
        StringUtils.findAndReplace(result, "%5C", ",");
        // represent colon characters with a semicolon
        StringUtils.findAndReplace(result, "%3a", ";");
        StringUtils.findAndReplace(result, "%3A", ";");
        // asterisk characters (unchanged by url-encoding) are not file safe
        StringUtils.findAndReplace(result, "*", "%2a");

        return result.toString();
    }

    /**
     * Perform a task similar to File.list(), but recurse into subdirectories.
     * 
     * @param directory
     *                the starting directory
     * @param filter
     *                a filter that can determine whether files should be
     *                included in the result. This filter will be called in a
     *                particular way:
     *                <ul>
     *                <li>the <code>dir</code> parameter will always be
     *                passed the same value that was passed into this method.
     *                Thus, it will contain the base of the search, rather than
     *                the deepest subdirectory containing the file in question</li>
     *                <li>for files in subdirectories, the <code>name</code>
     *                parameter will begin with a slash-separated list of
     *                subdirectory names. Thus, it will still indicate the name
     *                of the file relative to the base directory. Forward
     *                slashes will always be used, regardless of the
     *                platform-specific separator character.</li>
     *                <li>The filter will also be called for each subdirectory,
     *                as an inquiry about whether the logic should recurse into
     *                that subdirectory. Subdirectory names will be indicated
     *                by a terminal "/" character on the <code>name</code>
     *                parameter.</li>
     *                </ul>

     * @return a list of filenames in and underneath the given directory that
     *     match the filter.  the forward slash will be used as a directory
     *     separator, regardless of the platform-specific separator character.
     */
    public static final List<String> listRecursively(File directory,
            FilenameFilter filter) {
        ArrayList<String> result = new ArrayList<String>();
        listFilesRecursively(result, directory, filter, directory, "");
        return result;
    }

    private static void listFilesRecursively(List<String> result, File baseDir,
            FilenameFilter filter, File dir, String prefix) {

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = prefix + f.getName();
            if (f.isDirectory()) {
                name = name + "/";
                if (filter.accept(baseDir, name))
                    listFilesRecursively(result, baseDir, filter, f, name);
            } else if (filter.accept(baseDir, name)) {
                result.add(name);
            }
        }
    }

}
