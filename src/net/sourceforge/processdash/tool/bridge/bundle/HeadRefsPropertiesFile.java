// Copyright (C) 2021-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ConcurrentModificationException;

import net.sourceforge.processdash.util.SortedProperties;

public class HeadRefsPropertiesFile extends HeadRefsProperties {

    private File storage;

    private long lastModified, lastSize;

    public HeadRefsPropertiesFile(File file) {
        this.storage = file;
        this.lastModified = this.lastSize = 0;
    }


    @Override
    protected void update() throws IOException {
        // update the file if it has changed
        if (needsUpdate())
            doUpdate();
    }

    protected boolean needsUpdate() {
        // an update is needed if the file's size or timestamp have changed
        return storage.lastModified() != lastModified
                || storage.length() != lastSize;
    }

    private void doUpdate() throws IOException {
        // make several attempts to update our data from the file
        int errCount = 0;
        while (true) {
            try {
                // if we update successfully, return
                doUpdateImpl();
                return;

            } catch (FileNotFoundException fnfe) {
                // abort if network connection to the parent directory is lost
                throw fnfe;

            } catch (ConcurrentModificationException cme) {
                // if another process was modifying the file while we were
                // reading it, pause for a moment and try again
                retryOrAbort(0, null, 500);

            } catch (IOException ioe) {
                // if other transient problems occur, wait and retry. if out of
                // retries, abort.
                retryOrAbort(errCount++, ioe, 100, 250, 500, 500);
            }
        }
    }

    private void doUpdateImpl() throws IOException {
        // abort if the parent directory is unavailable (e.g., lost network)
        if (storage.getParentFile().isDirectory() == false)
            throw new FileNotFoundException(storage.getParent());

        // check the size of the file before reading
        long startSize = storage.length();

        // read the properties from the file, if it exists
        SortedProperties newProps = new SortedProperties();
        if (storage.isFile() && startSize > 0) {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(storage));
            newProps.load(in);
            in.close();
        }

        // get the size of the file after reading. Ensure no change
        long endSize = storage.length();
        if (startSize != endSize)
            throw new ConcurrentModificationException();

        // store the new properties, as well as the file time/size
        this.props = newProps;
        this.lastModified = storage.lastModified();
        this.lastSize = endSize;
    }


    @Override
    protected void flush() throws IOException {
        // modify the nonce value in the file
        tweakNonce();

        // write the properties to our storage file
        OutputStream out = FileBundleUtils.outputStream(storage);
        props.store(out, null);
        out.close();

        // make a note of the storage file's time/size
        this.lastModified = storage.lastModified();
        this.lastSize = storage.length();
    }

    private void tweakNonce() {
        // we write an extra property into the file whose value is unimportant,
        // but whose size changes with each modification. This will cause the
        // size of the stored file to change, allowing for more robust detection
        // of changes by other clients (who might not see timestamps reliably)
        String nonceVal = props.getProperty(NONCE_PROP);
        if (nonceVal != null && nonceVal.length() < 60)
            nonceVal += "X";
        else
            nonceVal = "X";
        props.put(NONCE_PROP, nonceVal);
    }


    protected void retryOrAbort(int errCount, Exception ex, int... delays)
            throws IOException {
        if (errCount < delays.length) {
            try {
                Thread.sleep(delays[errCount]);
            } catch (InterruptedException ie) {
            }
        } else if (ex instanceof IOException) {
            throw (IOException) ex;
        } else {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private static final String NONCE_PROP = "_nonce";

}
