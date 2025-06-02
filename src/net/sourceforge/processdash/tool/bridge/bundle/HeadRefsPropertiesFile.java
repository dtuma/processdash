// Copyright (C) 2021-2025 Tuma Solutions, LLC
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
import java.util.Properties;

import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.SortedProperties;

public class HeadRefsPropertiesFile extends HeadRefsProperties {

    private File storage;

    private File published;

    private long lastModified, lastSize;

    public HeadRefsPropertiesFile(File file) {
        this.storage = file;
        this.published = null;
        this.lastModified = -1; // force an initial update
        this.lastSize = 0;
    }

    protected void setPublishedFile(File published) {
        this.published = published;
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
                || storage.length() != lastSize
                // also update if a published file exists with a different size
                || (published != null && published.isFile()
                        && published.length() != lastSize);
    }

    private void doUpdate() throws IOException {
        // make several attempts to update our data from the file
        int errCount = 0;
        while (true) {
            try {
                // if we update successfully, return
                maybeRestoreFromPublished();
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

    private void maybeRestoreFromPublished() throws IOException {
        // if no published file is configured, abort
        if (published == null)
            return;

        // abort if the parent directory is unavailable (e.g., lost network)
        if (published.getParentFile().isDirectory() == false)
            throw new FileNotFoundException(published.getParent());

        // don't restore if the published file does not exist, is empty, or
        // can't be read
        long publishedSize = published.length();
        long publishedTime = published.lastModified();
        if (publishedSize == 0 || publishedTime == 0)
            return;

        // Our main restoration scenarios will be:
        //
        // (a) the storage file has been unexpectedly lost. Restore from backup
        //
        // (b) this publish-aware logic is running for the first time. Our
        // storage location has changed, and published points to the location
        // where it was written by the old logic. Initialize our storage to
        // match what was written by the old software.
        //
        // (c) someone is still using the old version of the software
        // concurrently, and is writing data to the old location (which we now
        // consider "published"). Pick up their changes.
        //
        // In all of these, it only makes sense to restore if the published
        // file is at least a minute newer than our storage file.
        long storageTime = storage.lastModified();
        long newer = publishedTime - storageTime;
        if (newer < DateUtils.MINUTE)
            return;

        // Scenario (c) described above should be exceedingly rare. If it looks
        // like it's actually happening, double check the data inside the files
        // to make sure we're not being led astray by spurious file timestamps.
        if (storageTime > 0 && storage.length() > 0) {
            long publishedDataTime = getDataTimestamp(published);
            long storageDataTime = getDataTimestamp(storage);
            if (publishedDataTime <= storageDataTime)
                return;
        }

        // restore the storage file from the published version
        OutputStream out = FileBundleUtils.outputStream(storage);
        FileUtils.copyFile(published, out);
        out.close();
        storage.setLastModified(publishedTime);
    }

    private long getDataTimestamp(File f) throws IOException {
        // read the properties from the file
        Properties p = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        p.load(in);
        in.close();
        return getRefModTimestamp(p);
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
        } else if (lastSize > 0) {
            // the file was present in the past but seems to be missing. HEAD
            // files are not deleted by dashboard logic, so this is most likely
            // due to transient network issues. Abort
            throw new FileNotFoundException(storage.getPath());
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

        // publish a copy of the data, if so configured
        if (published != null) {
            out = FileBundleUtils.outputStream(published);
            FileUtils.copyFile(storage, out);
            out.close();
            published.setLastModified(lastModified);
        }
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
