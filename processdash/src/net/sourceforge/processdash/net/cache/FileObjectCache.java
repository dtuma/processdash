// Copyright (C) 2002-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FileObjectCache implements ObjectCache,
                                        CachedObject.CachedDataProvider {

    protected static final String BACKUP_PREFIX =
        RobustFileOutputStream.BACKUP_PREFIX;

    protected File directory;
    protected String extension;
    protected int nextAvaliableID;

    /** Create an cache which stores its objects into a directory as
     * files with the given extension.
     * @param directory the directory where the files should be placed.
     * @param extension the filename suffix (starting with ".") which should
     *   be used for the files.
     * @exception NullPointerException if directory is null
     * @exception IOException if directory does not point to a valid
     *   directory
     */
    public FileObjectCache(File directory, String extension)
        throws IOException
    {
        if (!directory.isDirectory())
            throw new IOException("Invalid directory");

        // save the information
        this.directory = directory;
        if (!extension.startsWith("."))
            extension = "." + extension;
        this.extension = extension;

        // figure out the next available ID.
        nextAvaliableID = 0;
        int[] idList = getObjectIDs();
        for (int i = idList.length;   i-- > 0; )
            if (idList[i] >= nextAvaliableID)
                nextAvaliableID = idList[i] + 1;
    }

    /** Return the next available ID for use as a cached object
     * identifier.  This will not return the same number twice.
     */
    public synchronized int getNextID() {
        int result = nextAvaliableID;
        nextAvaliableID++;
        return result;
    }

    /** Retrieve a cached object by its ID. */
    public synchronized CachedObject getCachedObject(int id, double maxAge) {
        if (id < 0) return null;

        FileInputStream fis = null;
        try {
            File f = makeFile(id);
            if (!f.isFile()) return null;

            fis = new FileInputStream(f);
            Document d = XMLUtils.parse(fis);
            Element docRoot = d.getDocumentElement();

            CachedObject result =
                CachedObject.openXML(this, id, docRoot, this);
            result.refresh(maxAge);
            return result;

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
        return null;
    }


    /** Delete an object from the cache. */
    public synchronized void deleteCachedObject(int id) {
        File f = makeFile(id);
        f.delete();
        f = makeDataFile(id);
        f.delete();
    }

    /** Store an object in the cache. */
    public synchronized void storeCachedObject(CachedObject obj) {
        int id = obj.getID();
        if (id < 0)
            throw new IllegalArgumentException("Invalid id.");

        File f = null, backup = null;
        FileOutputStream fos = null;
        try {
            f = makeFile(id);
            backup = makeBackupFile(id);
            f.renameTo(backup);

            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version='1.0' encoding='UTF-8'?>\n");
            obj.getAsXML(xml);

            fos = new FileOutputStream(f);
            OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
            out.write(xml.toString());
            out.close();
            backup.delete();

            if (obj.dataProvider == null) {
                File df = makeDataFile(id);
                df.delete();

                byte[] data = obj.getBytes();
                if (data != null) {
                    fos = new FileOutputStream(df);
                    fos.write(data);
                    fos.close();
                }
            }

        } catch (IOException ioe) {
            if (fos != null) try { fos.close(); } catch (Exception e) {}

            if (backup.isFile()) {
                f.delete();
                backup.renameTo(f);
            }

            System.err.println(ioe);
            ioe.printStackTrace();
        }
    }


    /** Implementation of the CachedObject.CachedDataProvider interface.
     */
    public byte[] getData(CachedObject c) {
        FileInputStream fis = null;
        byte[] results = null;
        try {
            File df = makeDataFile(c.getID());
            if (!df.isFile()) return null;

            fis = new FileInputStream(df);
            results = FileUtils.slurpContents(fis, false);
        } catch (IOException ioe) {
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
        return results;
    }



    /** get a list of all the objects in the cache of the specified
     * type. If type is null, all objects are returned. */
    public synchronized CachedObject[] getObjects(String type) {
        int[] allIDs = getObjectIDs();

        int len = allIDs.length;
        CachedObject[] objects = new CachedObject[len];

        for (int i = len;  i-- > 0; ) {
            CachedObject co = getCachedObject(allIDs[i], -1);
            if (co == null || (type != null && !type.equals(co.getType()))) {
                len--;
                objects[i] = null;
            } else
                objects[i] = co;
        }

        CachedObject[] results = new CachedObject[len];
        if (len == 0) return results;

        for (int i = objects.length;   i-- > 0; )
            if (objects[i] != null)
                results[--len] = objects[i];
        return results;
    }


    /** get a list of ids for all the objects in the cache. */
    public synchronized int[] getObjectIDs() {
        String[] filenames = directory.list(new Filter());
        int[] results = new int[filenames.length];
        for (int i = filenames.length;   i-- > 0; ) try {
            String num = filenames[i];
            num = num.substring(0, num.length()-extension.length());
            results[i] = Integer.parseInt(num);
        } catch (Exception e) {
            results[i] = -1;
        }
        return results;
    }

    /** get a list of ids for all the objects in the cache of the
     * specified type. If type is null, all objects are returned. */
    public int[] getObjectIDs(String type) {
        if (type == null) return getObjectIDs();

        CachedObject[] objects = getObjects(type);
        int len = objects.length;
        int[] results = new int[len];
        for (int i = objects.length;   i-- > 0; )
            results[i] = objects[i].getID();
        return results;
    }


    /** Encapsulate the file naming logic in one place. */
    protected File makeFile(int id) {
        return new File(directory, id + extension);
    }
    protected File makeDataFile(int id) {
        return new File(directory, id + "d" + extension);
    }
    protected File makeBackupFile(int id) {
        return new File(directory, BACKUP_PREFIX + id + extension);
    }

    /** FilenameFilter which locates files with the correct extension. */
    private class Filter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return
                name.endsWith(extension) &&
                !name.endsWith("d" + extension) &&
                !name.startsWith(RobustFileOutputStream.BACKUP_PREFIX);
        }
    }
}
