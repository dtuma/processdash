// Copyright (C) 2019-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.LargeKeyValueFile;

/**
 * A class that manages a data file containing SimpleData definitions that do
 * not need to be visible in the repository.
 * 
 * This class is useful for storage-only data values that are not referenced by
 * calculations, are read infrequently, and are possibly very large. This class
 * allows those values to be stored on disk rather than held in memory.
 * 
 * Operations on the file are performed via a scan, to avoid holding more than
 * one large value in memory at a time.
 */
public class ExternalDataFile {

    /**
     * Interface for a callback object that can receive information about the
     * data name/value pairs in an ExternalDataFile
     */
    public interface Scanner {

        /**
         * Callback method to receive information about a single data name/value
         * pair
         * 
         * @param dataName
         *            the name of a data element in the file
         * @param dataValue
         *            the data value associated with this name in the file
         * 
         * @throws IOException
         *             if I/O problems are encountered while the callback logic
         *             is handling this key/value
         * @throws ScannerAbortException
         *             if this object has received the data it was looking for,
         *             and does not need to scan the remainder of the file
         */
        public void handleDataFileEntry(String dataName, SimpleData dataValue)
                throws IOException, ScannerAbortException;

        /**
         * Objects implementing the {@link Scanner} interface can throw this
         * exception from their handleDataFileEntry method to indicate that they
         * have successfully retrieved the data they need, and do not need to
         * scan the remainder of a file.
         */
        public static class ScannerAbortException extends RuntimeException {
        }

    }



    private LargeKeyValueFile file;

    ExternalDataFile(File file) {
        this.file = new LargeKeyValueFile(file, "UTF-8");
    }



    /**
     * Scan the contents of an ExternalDataFile and pass each of its data
     * name/value pairs to a {@link Scanner} object
     * 
     * @param s
     *            the Scanner which would like to receive the data. Its
     *            handleDataFileEntry method will be called repeatedly, once for
     *            each entry in the file.
     * @throws IOException
     *             if I/O problems are encountered
     */
    public void scan(final Scanner s) throws IOException {
        file.scan(new LargeKeyValueFile.Scanner() {
            public void handleKeyValueEntry(String oneKey, String oneValue)
                    throws IOException {
                String dataName = keyToName(oneKey);
                SimpleData dataValue = parseValue(oneValue);
                try {
                    s.handleDataFileEntry(dataName, dataValue);
                } catch (ExternalDataFile.Scanner.ScannerAbortException sae) {
                    throw new LargeKeyValueFile.Scanner.ScannerAbortException(
                            sae);
                }
            }
        });
    }



    /**
     * Get a collection of all the data names that appear in the file.
     * 
     * @return a Set containing all of the data names in the file.
     * @throws IOException
     *             if an I/O error is encountered
     */
    public Set<String> getDataNames() throws IOException {
        final Set<String> result = new HashSet<String>();

        file.scan(new LargeKeyValueFile.Scanner() {
            public void handleKeyValueEntry(String oneKey, String oneValue) {
                String dataName = keyToName(oneKey);
                result.add(dataName);
            }
        });

        return result;
    }



    /**
     * Get the data value assigned to a given name
     * 
     * @param dataName
     *            the data name to search for
     * @return the value associated with this name in the file. If the file does
     *         not contain an entry for this name, returns null
     * @throws IOException
     *             if an I/O error is encountered
     */
    public SimpleData getDataValue(String dataName) throws IOException {
        String key = nameToKey(dataName);
        String value = file.getValue(key);
        return parseValue(value);
    }



    /**
     * Save a data name/value pair into the file
     * 
     * If value is null, any existing entry in the file with this name will be
     * deleted.
     * 
     * Otherwise, a name/value pair will be written into the file with the new
     * data. If the file already contained an entry for this name, it will be
     * replaced.
     * 
     * @param dataName
     *            the data name to write
     * @param dataValue
     *            the value to store
     * @throws IOException
     *             if an I/O error is encountered
     */
    public void putDataValue(String dataName, SimpleData dataValue)
            throws IOException {
        String key = nameToKey(dataName);
        String value = (dataValue == null ? null : dataValue.saveString());
        file.putValue(key, value);
    }



    /**
     * Delete the data file this object is using for storage.
     */
    public void delete() {
        file.delete();
    }



    private static String nameToKey(String name) {
        return name.replace('=', DataRepository.EQUALS_SIGN_REPL);
    }

    private static String keyToName(String key) {
        return key.replace(DataRepository.EQUALS_SIGN_REPL, '=');
    }

    private SimpleData parseValue(String value) {
        if (value == null)
            return null;
        try {
            return ValueFactory.createSimple(value);
        } catch (MalformedValueException mve) {
            return new MalformedData(value);
        }
    }



    private static File dataDirectory;

    private static Map<String, ExternalDataFile> cache;

    public static void setDataDirectory(File dir) {
        if (dataDirectory != null)
            throw new IllegalStateException("dataDirectory is already set");

        dataDirectory = dir;
        cache = new HashMap<String, ExternalDataFile>();
    }

    public static ExternalDataFile get(String name) {
        if (dataDirectory == null || cache == null)
            throw new IllegalStateException("dataDirectory has not been set");

        synchronized (cache) {
            String filename = name + ".dat";
            ExternalDataFile result = cache.get(filename);

            if (result == null) {
                File f = new File(dataDirectory, filename);
                if (!f.getParentFile().equals(dataDirectory))
                    throw new IllegalArgumentException("bad file name " + name);
                result = new ExternalDataFile(f);
                cache.put(filename, result);
            }

            return result;
        }
    }

}
