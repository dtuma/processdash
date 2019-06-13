// Copyright (C) 2019 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A class that manages a Properties-like file, where values could be very
 * large.
 * 
 * Each line of the file is expected to be of the form key=value. Keys must not
 * contain the equals character, and neither keys nor values may contain the
 * newline character.
 * 
 * Operations on the file are performed via a scan, to avoid holding more than
 * one large value in memory at a time.
 */
public class LargeKeyValueFile {

    /**
     * Interface for a callback object that can receive information about the
     * key/value pairs in a LargeKeyValueFile
     */
    public interface Scanner {

        /**
         * Callback method to receive information about a single key/value pair
         * 
         * @param oneKey
         *            the portion of a file line that preceded the equals
         *            character
         * @param oneValue
         *            the portion of a file line that followed the equals
         *            character. If a given line did not contain an equals sign,
         *            this will be null.
         * 
         * @throws IOException
         *             if I/O problems are encountered while handling this
         *             key/value
         * @throws ScannerAbortException
         *             if this object has received the data it was looking for,
         *             and does not need to scan the remainder of the file
         */
        public void handleKeyValueEntry(String oneKey, String oneValue)
                throws IOException, ScannerAbortException;

        /**
         * Objects implementing the {@link Scanner} interface can throw this
         * exception from their handleKeyValueEntry method to indicate that they
         * have successfully retrieved the data they need, and do not need to
         * scan the remainder of a file.
         */
        public static class ScannerAbortException extends RuntimeException {
            public ScannerAbortException() {}
            public ScannerAbortException(Throwable cause) { super(cause); }
        }

    }



    private File file;

    private String charset;

    private ReentrantReadWriteLock lock;

    public LargeKeyValueFile(File file) {
        this(file, "UTF-8");
    }

    public LargeKeyValueFile(File file, String charset) {
        this.file = file;
        this.charset = charset;
        this.lock = new ReentrantReadWriteLock();
    }



    /**
     * Scan the contents of a LargeKeyValueFile and pass each of its key/value
     * pairs to a {@link Scanner} object
     * 
     * @param s
     *            the Scanner which would like to receive the data. Its
     *            handleKeyValueEntry method will be called repeatedly, once for
     *            each entry in the file.
     * @throws IOException
     *             if I/O problems are encountered
     */
    public void scan(Scanner s) throws IOException {
        // if the directory containing our file does not exist, it's likely due
        // to network connectivity problems. Throw an IOException
        if (!file.getParentFile().isDirectory())
            throw new FileNotFoundException(file.getParent());

        // if the file does not exist or is empty, no values need to be passed
        // to the scanner.
        if (!file.exists() || file.length() == 0)
            return;

        BufferedReader in = null;
        boolean needsReadLock = !lock.isWriteLockedByCurrentThread();
        try {
            if (needsReadLock)
                lock.readLock().lock();

            in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), charset));

            String line;
            while ((line = in.readLine()) != null) {
                String oneKey, oneValue;

                int equalsPosition = line.indexOf('=');
                if (equalsPosition == -1) {
                    oneKey = line;
                    oneValue = null;
                } else {
                    oneKey = line.substring(0, equalsPosition);
                    oneValue = line.substring(equalsPosition + 1);
                }

                try {
                    s.handleKeyValueEntry(oneKey, oneValue);
                } catch (Scanner.ScannerAbortException ssae) {
                    break;
                }
            }

        } finally {
            if (needsReadLock)
                lock.readLock().unlock();
            FileUtils.safelyClose(in);
        }
    }



    /**
     * Get a collection of all the keys that appear in the file.
     * 
     * @return a Set containing all of the keys in the file
     * @throws IOException
     *             if an I/O error is encountered
     */
    public Set<String> getKeys() throws IOException {
        final Set<String> result = new HashSet<String>();

        this.scan(new Scanner() {
            public void handleKeyValueEntry(String oneKey, String oneValue) {
                result.add(oneKey);
            }
        });

        return result;
    }



    /**
     * Get the value assigned to a given key
     * 
     * @param key
     *            the key to search for
     * @return the value associated with this key in the file. If the file does
     *         not contain an entry for this key, returns null
     * @throws IOException
     *             if an I/O error is encountered
     */
    public String getValue(final String key) throws IOException {
        final String[] result = new String[1];

        this.scan(new Scanner() {
            public void handleKeyValueEntry(String oneKey, String oneValue) {
                if (key.equals(oneKey)) {
                    result[0] = oneValue;
                    throw new ScannerAbortException();
                }
            }
        });

        return result[0];
    }



    /**
     * Save a key/value pair into the file
     * 
     * If value is null, any existing entry in the file with this key will be
     * deleted.
     * 
     * Otherwise, a key/value pair will be written into the file with the new
     * data. If the file already contained an entry for this key, it will be
     * replaced.
     * 
     * @param key
     *            the key to write
     * @param value
     *            the value to store
     * @throws IOException
     *             if an I/O error is encountered
     */
    public void putValue(final String key, String value) throws IOException {
        // check the parameters and reject forbidden characters
        if (key.indexOf('=') != -1 || key.indexOf('\n') != -1)
            throw new IllegalArgumentException(
                    "Invalid key - equals sign and newline forbidden");
        if (value != null && value.indexOf('\n') != -1)
            throw new IllegalArgumentException(
                    "Invalid value - newline forbidden");

        // if the directory containing our file does not exist, it's likely due
        // to network connectivity problems. Throw an IOException
        if (!file.getParentFile().isDirectory())
            throw new FileNotFoundException(file.getParent());

        // if we're being asked to write a null value into a nonexistent or
        // empty file, nothing needs to be done
        if (value == null && (!file.exists() || file.length() == 0))
            return;

        try {
            lock.writeLock().lock();
            final RobustFileWriter out = new RobustFileWriter(file, charset);
            try {

                // write the new key/value pair into the file. We place the new
                // value at the beginning of the file, with the idea that the
                // most recently written values are potentially more likely to
                // be read, so this will improve the performance of the getValue
                // method
                if (value != null)
                    writeEntry(out, key, value);

                // copy the remaining entries into the new file
                this.scan(new Scanner() {
                    public void handleKeyValueEntry(String oneKey,
                            String oneValue) throws IOException {
                        if (!key.equals(oneKey)) {
                            writeEntry(out, oneKey, oneValue);
                        }
                    }
                });

                // close the file
                out.close();

            } catch (IOException e) {
                // if errors are encountered, abort() our RobustFileWriter
                // operation. This will leave the original file unchanged.
                out.abort();
                throw e;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void writeEntry(RobustFileWriter out, String oneKey,
            String oneValue) throws IOException {
        out.write(oneKey);
        out.write('=');
        out.write(oneValue);
        out.write('\n');
    }

}
