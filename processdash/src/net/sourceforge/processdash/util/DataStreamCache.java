// Copyright (C) 2008-2010 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A generic cache for streams of binary data.
 * 
 * Clients of this object can store binary data using the following steps:
 * <ol>
 * <li>Call {@link #getOutputStream()}. (Each stream returned by this object
 * will have a unique <code>hashCode</code> that can be used to retrieve the
 * data later.)</li>
 * <li>Write data to the resulting <code>OutputStream</code></li>
 * <li>Close the <code>OutputStream</code></li>
 * </ul>
 * 
 * Clients can retrieve previously stored binary data using the following steps:
 * <ol>
 * <li>Call {@link #getInputStream(int)}. (For the parameter of this method,
 * pass in the <code>hashCode</code> of a stream that was used earlier for the
 * storage of data.)</li>
 * <li>Read from the resulting <code>InputStream</code>
 * </ul>
 * 
 * <p>
 * Behind the scenes, this class actually stores data in a 2-tiered cache. When
 * data is first stored in this object, it will be retained in memory. At some
 * later point, it may be moved from memory to disk. Instances of
 * {@link DataStreamCache.CacheCleanupPolicy} can be registered to control the
 * lifecycle of objects in the memory/disk cache.
 * 
 * @author Tuma
 * 
 */
public class DataStreamCache {

    public interface CacheCleanupPolicy {
        public boolean shouldCleanup(long creationTime, List<Long> accessTimes);
    }

    private int nextStreamID;

    private Map<Integer, MemCachedStream> memCachedData;

    private Set<CacheFile> cacheFiles;

    private Map<Integer, SavedStream> savedData;

    private CacheCleanupPolicy memoryCleanupPolicy;

    private CacheCleanupPolicy fileCleanupPolicy;

    private StreamSaver streamSaver;



    public DataStreamCache() {
        this(true);
    }

    public DataStreamCache(boolean enableFilesystemCache) {
        // generate an arbitrary starting stream ID. The calculation below is
        // designed to produce numbers that *probably* weren't used in previous
        // invocations of the application.
        this.nextStreamID = (int) (System.currentTimeMillis() & ((1 << 25) - 1));
        this.memCachedData = Collections
                .synchronizedMap(new HashMap<Integer, MemCachedStream>());
        this.cacheFiles = Collections.synchronizedSet(new HashSet<CacheFile>());
        this.savedData = Collections
                .synchronizedMap(new HashMap<Integer, SavedStream>());
        this.memoryCleanupPolicy = USE_ONCE;
        this.fileCleanupPolicy = DELETE_OLD;
        if (enableFilesystemCache) {
            this.streamSaver = new StreamSaver();
            this.streamSaver.start();
        }
    }



    /**
     * Gets an <code>OutputStream</code> that can be used to store data into
     * this cache.
     * 
     * Data can be written to this output stream. When the stream is closed, the
     * data will be added to this cache.
     * 
     * Each <code>OutputStream</code> returned by this cache will have a
     * unique <code>hashCode</code>. After data is written to the output
     * stream and the stream is closed, the data can subsequently be retrieved
     * by calling {@link #getInputStream(int)} with the same
     * <code>hashCode</code> as the parameter.
     * 
     * @return an <code>OutputStream</code> for saving data.
     */
    public synchronized OutputStream getOutputStream() {
        return new CachingOutputStream(nextStreamID++);
    }



    /**
     * Retrieves an <code>InputStream</code> that can be used to retrieve data
     * which was stored in this cache earlier.
     * 
     * @param hashCode
     *                the <code>hashCode</code> of the
     *                <code>OutputStream</code> (returned from
     *                {@link #getOutputStream()}) that was used to store the
     *                data.
     * @return an <code>InputStream</code> that can be used to reread the
     *         data. If no matching item is found in this cache, returns null.
     *         (This could occur if an invalid <code>hashCode</code> is passed
     *         in, or if the stream in question has already been purged from the
     *         disk cache.)
     * @throws IOException
     *                 if an IO error occurs when attempting to retrieve the
     *                 stream
     */
    public InputStream getInputStream(int hashCode) throws IOException {
        MemCachedStream data = memCachedData.get(hashCode);
        if (data != null)
            return data.getInputStream();

        SavedStream saved = savedData.get(hashCode);
        if (saved != null)
            return saved.getInputStream();

        return null;
    }



    /**
     * Sets the policy used to decide when data should be purged from memory.
     * 
     * By default, this class uses a policy based on an assumption that cached
     * data will normally be read only once. Thus, after a data stream has been
     * read (using the {@link #getInputStream(int)} method), the policy will
     * move the stream to the disk-based cache. Since streams may sometimes be
     * written and then abandoned, the policy will also move streams to the disk
     * cache if they have not been read within one minute of having been
     * registered.
     * 
     * @param policy
     *                this policy object will be consulted to determine whether
     *                a stream should be deleted from memory. The policy object
     *                will be called with the timestamp of the stream's
     *                creation, and the list of times that the stream has been
     *                accessed since its creation. Note: <code>null</code> is
     *                permitted, and will be interpreted to mean that data
     *                should never be purged from the memory cache.
     */
    public void setMemoryCacheCleanupPolicy(CacheCleanupPolicy policy) {
        this.memoryCleanupPolicy = policy;
    }



    /**
     * Sets the policy used to decide when files of cached data should be
     * deleted from the disk.
     * 
     * This class will create temporary files to hold data streams. A single
     * file can hold data for many cached streams. Whenever a temporary file
     * exceeds a certain preset size, a new file is created.
     * 
     * All temporary files will be deleted when the application terminates.
     * However, for long-running applications, the temporary files may need to
     * be cleaned up periodically. This method allows the client to control the
     * policy for the cleanup of those temporary files.
     * 
     * By default, this class uses a policy based on an assumption that cached
     * data will be used fairly quickly. Thus, if a particular temporary file
     * has not been read for over an hour, it will be deleted. Subsequent
     * attempts to read data streams that were held in that file will return
     * null.
     * 
     * @param policy
     *                this policy object will be consulted to determine whether
     *                a temporary file should be deleted from the hard disk. The
     *                policy object will be called with the timestamp of the
     *                temp file's creation, and the list of times that the data
     *                from the file has been accessed since its creation.
     */
    public void setFileCacheCleanupPolicy(CacheCleanupPolicy policy) {
        this.fileCleanupPolicy = policy;
    }



    /**
     * Request an immediate cleanup/purge of items, as dictacted by the
     * registered cache cleanup policies.
     */
    public void cleanupCache() {
        cleanupCachedStreams();
        cleanupCachedFiles();
    }



    /**
     * Discard streams from memory as dictated by the memory cleanup policy.
     */
    protected void cleanupCachedStreams() {
        List<MemCachedStream> cache;
        synchronized (memCachedData) {
            cache = new ArrayList<MemCachedStream>(memCachedData.values());
        }
        for (MemCachedStream cachedStream : cache) {
            cachedStream.maybeCleanup();
        }
    }



    /**
     * Discard streams from disk as dictated by the file cleanup policy.
     */
    protected void cleanupCachedFiles() {
        List<CacheFile> files;
        synchronized (cacheFiles) {
            files = new ArrayList<CacheFile>(cacheFiles);
        }
        for (CacheFile cacheFile : files) {
            cacheFile.maybeCleanup();
        }
    }



    private void addDataToCache(CachingOutputStream s) {
        MemCachedStream data = new MemCachedStream(s.streamID, s.toByteArray());
        memCachedData.put(s.streamID, data);
        if (streamSaver != null)
            streamSaver.addStreamToSave(data);
        cleanupCachedStreams();
    }

    private void removeDataFromCache(MemCachedStream s) {
        memCachedData.remove(s.streamID);
    }

    private void removeFileFromCache(CacheFile f) {
        cacheFiles.remove(f);
    }



    /**
     * An <code>OutputStream</code> that knows how to save data in this cache
     * when it is closed.
     */
    private class CachingOutputStream extends ByteArrayOutputStream {

        private int streamID;

        public CachingOutputStream(int streamID) {
            this.streamID = streamID;
        }

        @Override
        public void close() throws IOException {
            super.close();
            addDataToCache(this);
        }

        @Override
        public int hashCode() {
            return streamID;
        }

    }



    /**
     * A single cached stream, with contents held in memory.
     */
    private class MemCachedStream {

        int streamID;

        byte[] data;

        long creationTime;

        List<Long> accessTimes;

        boolean saved;

        public MemCachedStream(int streamID, byte[] data) {
            this.streamID = streamID;
            this.data = data;
            this.creationTime = System.currentTimeMillis();
            this.accessTimes = new ArrayList<Long>();
            this.saved = false;
        }

        public InputStream getInputStream() {
            ByteArrayInputStream result = new ByteArrayInputStream(data);
            touch();
            return result;
        }

        public synchronized void touch() {
            this.accessTimes.add(System.currentTimeMillis());
            maybeCleanup();
        }

        public synchronized void setSaved() {
            this.saved = true;
            maybeCleanup();
        }

        private void maybeCleanup() {
            if (shouldCleanup())
                removeDataFromCache(this);
        }

        private boolean shouldCleanup() {
            if (!saved)
                return false;
            if (memoryCleanupPolicy == null)
                return false;
            return memoryCleanupPolicy.shouldCleanup(creationTime, accessTimes);
        }

    }



    /**
     * A file on disk that holds data for several disk-cached streams
     */
    private class CacheFile {

        File cacheFile;

        long creationTime;

        List<Long> accessTimes;

        OutputStream outputStream;

        public CacheFile() throws IOException {
            this(TempFileFactory.get().createTempFile("streamCache", ".tmp"));
            this.cacheFile.deleteOnExit();
        }

        public CacheFile(File cacheFile) {
            this.cacheFile = cacheFile;
            this.creationTime = System.currentTimeMillis();
            this.accessTimes = new ArrayList<Long>();
            cacheFiles.add(this);
            cleanupCachedFiles();
        }

        public InputStream getInputStream() throws IOException {
            FileInputStream fis = new FileInputStream(cacheFile);
            BufferedInputStream in = new BufferedInputStream(fis);
            touch();
            return in;
        }

        public synchronized OutputStream openOutputStream() throws IOException {
            if (outputStream == null) {
                FileOutputStream fos = new FileOutputStream(cacheFile, true);
                outputStream = new BufferedOutputStream(fos);
            }
            touch();
            return outputStream;
        }

        public synchronized void closeOutputStream() {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                } catch (IOException e) {
                }
            }

        }

        public synchronized void touch() {
            this.accessTimes.add(System.currentTimeMillis());
        }

        private void maybeCleanup() {
            if (shouldCleanup()) {
                removeFileFromCache(this);
                cacheFile.delete();
            }
        }

        private synchronized boolean shouldCleanup() {
            if (fileCleanupPolicy == null)
                return false;
            return fileCleanupPolicy.shouldCleanup(creationTime, accessTimes);
        }
    }



    /**
     * Record-keeping information about a stream that has been written to disk.
     */
    private class SavedStream {

        CacheFile cacheFile;

        long startPos;

        int dataLen;

        public SavedStream(int streamID, CacheFile cacheFile, long startPos,
                int dataLen) {
            this.cacheFile = cacheFile;
            this.startPos = startPos;
            this.dataLen = dataLen;
        }

        public InputStream getInputStream() throws IOException {
            if (!cacheFiles.contains(cacheFile))
                // this saved stream was in a temporary cache file that has been
                // deleted.
                return null;

            InputStream in = cacheFile.getInputStream();
            in.skip(startPos);
            byte[] data = new byte[dataLen];
            FileUtils.readAndFillArray(in, data);
            in.close();
            return new ByteArrayInputStream(data);
        }
    }



    /**
     * A background daemon that saves data streams to disk.
     */
    private class StreamSaver extends Thread {

        private List<MemCachedStream> streamsToSave;

        private CacheFile cacheFile;

        private long streamPos;

        private StreamSaver() {
            super("DataStreamCache.StreamSaver");
            this.setDaemon(true);
            this.streamsToSave = new ArrayList<MemCachedStream>();
        }

        public synchronized void addStreamToSave(MemCachedStream s) {
            streamsToSave.add(s);
            this.notify();
        }

        private synchronized MemCachedStream getNextStreamToSave() {
            // if we don't seem to have any work to do, pause for a fraction
            // of a second to see if more work arrives.
            if (streamsToSave.isEmpty())
                tryWait(500);

            // after that pause, if there still isn't any work to do, close our
            // internal output stream. (We don't want to hold it open, because
            // that would prevent the cache file from being deleted when the
            // JVM shuts down.)
            if (streamsToSave.isEmpty() && cacheFile != null)
                cacheFile.closeOutputStream();

            // now, wait around until there is work in our queue.
            while (streamsToSave.isEmpty())
                tryWait(0);

            // work has appeared in our queue! Return the first item.
            return streamsToSave.remove(0);
        }

        private void tryWait(long timeout) {
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
            }
        }


        @Override
        public void run() {
            while (true) {
                MemCachedStream s = getNextStreamToSave();
                saveStream(s);
            }
        }


        private void saveStream(MemCachedStream s) {
            for (int i = MAX_SAVE_ATTEMPTS; i-- > 0;) {
                try {
                    initCacheFile();
                    doSaveStream(s);
                    return;
                } catch (IOException ioe) {
                    cacheFile = null;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        private void initCacheFile() throws IOException {
            if (cacheFile == null) {
                cacheFile = new CacheFile();
                streamPos = 0;
            }
        }

        private void doSaveStream(MemCachedStream s) throws IOException {
            SavedStream result = new SavedStream(s.streamID, this.cacheFile,
                    this.streamPos, s.data.length);

            OutputStream out = cacheFile.openOutputStream();
            out.write(s.data);
            out.flush();
            this.streamPos += s.data.length;

            savedData.put(s.streamID, result);
            s.setSaved();

            if (this.streamPos > SAVED_CACHE_CHUNK_SIZE) {
                this.cacheFile.closeOutputStream();
                this.cacheFile = null;
            }
        }

    }


    private static class DefaultCleanupPolicy implements CacheCleanupPolicy {

        private int cleanupAfterNumUses;

        private long cleanupAfterElapsedTime;

        public DefaultCleanupPolicy(int cleanupAfterNumUses,
                long cleanupAfterElapsedTime) {
            this.cleanupAfterNumUses = cleanupAfterNumUses;
            this.cleanupAfterElapsedTime = cleanupAfterElapsedTime;
        }

        public boolean shouldCleanup(long creationTime, List<Long> accessTimes) {
            if (cleanupAfterNumUses > 0 && accessTimes != null
                    && accessTimes.size() >= cleanupAfterNumUses)
                return true;

            long lastUse;
            if (accessTimes.isEmpty())
                lastUse = creationTime;
            else
                lastUse = accessTimes.get(accessTimes.size() - 1);
            long age = System.currentTimeMillis() - lastUse;
            if (cleanupAfterElapsedTime > 0 && age > cleanupAfterElapsedTime)
                return true;

            return false;
        }

    }

    public static final CacheCleanupPolicy USE_ONCE = new DefaultCleanupPolicy(
            1, 60 * 1000);

    public static final CacheCleanupPolicy DELETE_OLD = new DefaultCleanupPolicy(
            0, 60 * 60 * 1000);


    private static final int MAX_SAVE_ATTEMPTS = 10;

    private static final long SAVED_CACHE_CHUNK_SIZE = 5 * 1000 * 1000;
}
