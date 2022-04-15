// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;


import static net.sourceforge.processdash.tool.bridge.ResourceFilterFactory.INCLUDE_PARAM;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.OfflineLockStatus;
import net.sourceforge.processdash.tool.bridge.OfflineLockStatusListener;
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionType;
import net.sourceforge.processdash.tool.bridge.ResourceFilterFactory;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.bridge.impl.TLSConfig;
import net.sourceforge.processdash.tool.bridge.report.ListingHashcodeCalculator;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.tool.bridge.report.ResourceContentStream;
import net.sourceforge.processdash.tool.bridge.report.XmlCollectionListing;
import net.sourceforge.processdash.util.ClientFormRequest;
import net.sourceforge.processdash.util.ClientHttpRequest;
import net.sourceforge.processdash.util.ClientPostRequest;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.NotLockedException;
import net.sourceforge.processdash.util.lock.ReadOnlyUrlLockFailureException;

public class ResourceBridgeClient implements ResourceBridgeConstants {

    private static final String CLIENT_VERSION = TeamServerSelector.CLIENT_VERSION;

    ResourceCollection localCollection;

    FilenameFilter syncDownOnlyFiles;

    String remoteUrl;

    String serverVersion;

    String userName;

    String userId;

    String effectiveDate;

    String sourceIdentifier;

    String extraLockData;

    OfflineLockStatus offlineLockStatus;

    OfflineLockStatusListener offlineLockStatusListener;

    private static final Logger logger = Logger
            .getLogger(ResourceBridgeClient.class.getName());


    public ResourceBridgeClient(ResourceCollection localCollection,
            String remoteUrl, FilenameFilter syncDownOnlyFiles) {
        this.localCollection = localCollection;
        this.remoteUrl = remoteUrl;
        this.syncDownOnlyFiles = syncDownOnlyFiles;
        this.userId = getUserId();
        this.effectiveDate = System
                .getProperty(HistoricalMode.DATA_EFFECTIVE_DATE_PROPERTY);
        this.offlineLockStatus = OfflineLockStatus.NotLocked;
    }

    public synchronized void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public synchronized void setExtraLockData(String extraLockData) {
        this.extraLockData = extraLockData;
    }

    public void setOfflineLockStatusListener(OfflineLockStatusListener l) {
        this.offlineLockStatusListener = l;
    }

    public OfflineLockStatus getOfflineLockStatus() {
        return offlineLockStatus;
    }

    private void setOfflineLockStatus(OfflineLockStatus s) {
        this.offlineLockStatus = s;
        if (offlineLockStatusListener != null)
            offlineLockStatusListener.setOfflineLockStatus(s);
    }

    public synchronized boolean syncDown() throws IOException {
        return syncDown(null);
    }

    public synchronized boolean syncDown(SyncFilter filter) throws IOException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.syncDown["
                + remoteUrl + "]");
        // compare hashcodes to see if the local and remote directories have
        // identical contents
        if (hashesMatch()) {
            pt.click("checked hashes - match");
            return false;
        }
        pt.click("checked hashes - mismatch");

        // as an optimization, download any files from the server that were
        // created/modified after our most recently changed file.
        long mostRecentLocalModTime = getMostRecentLocalModTime();
        downloadFiles(makeGetRequest(DOWNLOAD_ACTION,
            ResourceFilterFactory.LAST_MOD_PARAM, mostRecentLocalModTime));
        pt.click("downloaded recent changes");

        // now make a complete comparison of local-vs-remote changes.
        ResourceCollectionDiff diff = getDiff();
        applySyncFilter(diff, filter);
        pt.click("Computed local-vs-remote diff");
        if (diff == null || diff.noDifferencesFound())
            return false;

        // if any files are present only in our local collection (but not in
        // the remote collection), delete the local files.
        for (String resourceName : diff.getOnlyInA()) {
            logger.fine("deleting local resource " + resourceName);
            localCollection.deleteResource(resourceName);
        }

        // copy down files that are only present in the remote collection, as
        // well as any files that have changed
        List filesToDownload = new ArrayList();
        filesToDownload.addAll(diff.getOnlyInB());
        filesToDownload.addAll(diff.getDiffering());
        downloadFilesNamed(filesToDownload);

        pt.click("Copied down changes");
        return true;
    }

    /**
     * Apply changes to the collection on the server to make it match the
     * contents of the local collection.
     * 
     * @return true if any changes were made, false if the collections were
     *         already in sync.
     * @throws LockFailureException
     *                 if this client does not own a write lock on the server
     *                 collection
     * @throws IOException
     *                 if any other problem prevents the sync from succeeding
     */
    public synchronized boolean syncUp() throws IOException, LockFailureException {
        return syncUp(null);
    }

    public synchronized boolean syncUp(SyncFilter filter) throws IOException,
            LockFailureException {
        if (userName == null)
            throw new NotLockedException();

        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.syncUp["
                + remoteUrl + "]");
        ResourceCollectionDiff diff = getDiff();
        applySyncFilter(diff, filter);
        pt.click("Computed local-vs-remote diff");
        if (diff == null || diff.noDifferencesFound()) {
            logger.finer("no changes to sync up");
            return false;
        }

        boolean madeChange = false;
        List<String> filesToDownload = new ArrayList<String>();

        // decide what to do for each file that is present only in the remote
        // collection (but not in our local collection)
        if (!diff.getOnlyInB().isEmpty()) {
            List<String> params = new ArrayList<String>();
            for (String resourceName : diff.getOnlyInB()) {
                if (isSyncDownOnly(resourceName)) {
                    filesToDownload.add(resourceName);
                } else {
                    logger.fine("deleting remote resource " + resourceName);
                    params.add(DELETE_FILE_PARAM);
                    params.add(resourceName);
                }
            }
            if (!params.isEmpty()) {
                doPostRequest(DELETE_ACTION, (Object[]) params.toArray());
                pt.click("Deleted remote resources");
                madeChange = true;
            }
        }

        // upload files that need to be created or updated in the remote
        // collection
        if (!diff.getOnlyInA().isEmpty() || !diff.getDiffering().isEmpty()) {
            List params = new ArrayList();
            for (String resourceName : diff.getOnlyInA()) {
                if (isSyncDownOnly(resourceName)) {
                    logger.fine("deleting local resource " + resourceName);
                    localCollection.deleteResource(resourceName);
                    madeChange = true;
                } else {
                    logger.fine("uploading new resource " + resourceName);
                    addFileUploadParamsWithBatching(params, resourceName);
                    madeChange = true;
                }
            }
            for (String resourceName : diff.getDiffering()) {
                if (isSyncDownOnly(resourceName)) {
                    filesToDownload.add(resourceName);
                } else {
                    logger.fine("uploading modified resource " + resourceName);
                    addFileUploadParamsWithBatching(params, resourceName);
                    madeChange = true;
                }
            }
            if (!params.isEmpty()) {
                startZipUploadThread(params);
                doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
                pt.click("Uploaded new/modified resources");
                madeChange = true;
            }
        }

        if (!filesToDownload.isEmpty()) {
            List params = addMultiple(null, INCLUDE_PARAM, filesToDownload);
            downloadFiles(makeGetRequest(DOWNLOAD_ACTION, params));
            madeChange = true;
        }

        if (!madeChange) {
            logger.finer("no changes to sync up");
        }

        return madeChange;
    }

    public synchronized void saveDefaultExcludedFiles() throws IOException, LockFailureException {
        if (userName == null)
            throw new NotLockedException();

        List params = new ArrayList();
        for (String name : ResourceFilterFactory.DEFAULT_EXCLUDE_FILENAMES) {
            addFileUploadParams(params, name);
        }
        if (!params.isEmpty()) {
            startZipUploadThread(params);
            doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
            logger.fine("Uploaded default excluded files");
        }
    }

    public synchronized URL doBackup(String qualifier) throws IOException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.doBackup["
                + remoteUrl + "]");
        try {
            doPostRequest(BACKUP_ACTION, BACKUP_QUALIFIER_PARAM, qualifier);
            pt.click("backup finished, qualifer = " + qualifier);
        } catch (LockFailureException e) {
            // shouldn't happen
            logger.log(Level.SEVERE, "Received unexpected exception", e);
            pt.click("backup failed");
        }
        StringBuffer result = new StringBuffer(remoteUrl);
        HTMLUtils.appendQuery(result, VERSION_PARAM, CLIENT_VERSION);
        HTMLUtils.appendQuery(result, ACTION_PARAM, GET_BACKUP_ACTION);
        return new URL(result.toString());
    }

    public synchronized void acquireLock(String userName) throws LockFailureException {
        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.acquireLock["
                + remoteUrl + "]");
        try {
            this.userName = userName;
            doLockPostRequest(ACQUIRE_LOCK_ACTION);
            pt.click("Acquired bridged lock");
        } catch (LockFailureException lfe) {
            this.userName = null;
            throw lfe;
        } catch (Exception e) {
            this.userName = null;
            setOfflineLockStatus(OfflineLockStatus.NotLocked);
            throw new LockFailureException(e);
        }
    }

    /**
     * Reassert a lock that was enabled for offline use during a previous
     * session.
     * 
     * If the lock could be reobtained, this will return successfully - even if
     * the lock is no longer enabled for offline use. After calling this method,
     * clients should call {@link #getOfflineLockStatus()} to ensure that the
     * lock is in the mode they expect.
     */
    public synchronized void resumeOfflineLock(String userName)
            throws LockFailureException {
        if (!StringUtils.hasValue(extraLockData))
            throw new IllegalStateException("No extra lock data has been set");

        ProfTimer pt = new ProfTimer(logger,
                "ResourceBridgeClient.resumeOfflineLock[" + remoteUrl + "]");
        try {
            this.userName = userName;
            doLockPostRequest(ASSERT_LOCK_ACTION);
            pt.click("Resumed offline bridged lock");
        } catch (LockFailureException lfe) {
            this.userName = null;
            throw lfe;
        } catch (Exception e) {
            // when operating in offline mode, it is not unusual for the server
            // to be unreachable, which could result in IOExceptions or other
            // errors.  Give caller the benefit of the doubt and mark the lock
            // as offline; then continue normally.
            setOfflineLockStatus(OfflineLockStatus.Enabled);
        }
    }

    public synchronized void pingLock() throws LockFailureException {
        if (userName == null)
            throw new NotLockedException();

        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient.pingLock["
                + remoteUrl + "]");
        try {
            doLockPostRequest(PING_LOCK_ACTION);
            pt.click("Pinged bridged lock");
        } catch (LockFailureException lfe) {
            throw lfe;
        } catch (Exception e) {
            throw new LockUncertainException(e);
        }
    }

    public synchronized void assertLock() throws LockFailureException {
        doAssertLock(ASSERT_LOCK_ACTION);
    }

    public synchronized void setOfflineLockEnabled(boolean offlineEnabled)
            throws LockFailureException {
        if (!StringUtils.hasValue(extraLockData))
            throw new IllegalStateException("No extra lock data has been set");

        if (offlineEnabled)
            doAssertLock(ENABLE_OFFLINE_LOCK_ACTION);
        else
            doAssertLock(DISABLE_OFFLINE_LOCK_ACTION);
    }

    private synchronized void doAssertLock(String action)
            throws LockFailureException {
        if (userName == null)
            throw new NotLockedException();

        ProfTimer pt = new ProfTimer(logger, "ResourceBridgeClient." + action
                + "[" + remoteUrl + "]");
        try {
            doLockPostRequest(action);
            pt.click("Asserted bridged lock");
        } catch (LockFailureException lfe) {
            throw lfe;
        } catch (Exception e) {
            throw new LockUncertainException(e);
        }
    }

    public synchronized void releaseLock() {
        if (userName == null)
            return;

        ProfTimer pt = new ProfTimer(logger,
                "ResourceBridgeClient.releaseLock[" + remoteUrl + "]");
        try {
            doLockPostRequest(RELEASE_LOCK_ACTION);
            setOfflineLockStatus(OfflineLockStatus.NotLocked);
            pt.click("Released bridged lock");
        } catch (Exception e) {
            // We don't throw any error here, because if we fail to release
            // the bridged lock, the worst case scenario is that it will time
            // out on the server after 5 minutes or so.  Log a message for
            // posterity.
            logger.log(Level.FINE, "Unable to release bridged lock", e);
        }
    }

    /**
     * Save a single file to the server.
     * 
     * @param remoteUrl the url of the team server
     * @param resourceName the name of the resource to save the data as
     * @param data the data to save to the server
     * @return the checksum of the file, as written to the server
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required.
     */
    public static Long uploadSingleFile(URL remoteUrl, String resourceName,
            InputStream data) throws IOException,
            LockFailureException {
        byte[] response = doAnonymousPostRequest(remoteUrl, UPLOAD_ACTION,
            resourceName, data);
        ResourceCollectionInfo remoteList = XmlCollectionListing
                .parseListing(new ByteArrayInputStream(response));
        return remoteList.getChecksum(resourceName);
    }

    /**
     * Delete a single file from the server.
     * 
     * @param remoteUrl the url of the team server
     * @param resourceName the name of the resource to delete
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required.
     */
    public static boolean deleteSingleFile(URL remoteUrl, String resourceName)
            throws IOException, LockFailureException {
        doAnonymousPostRequest(remoteUrl, DELETE_ACTION, DELETE_FILE_PARAM,
            resourceName);
        // the statement above will throw an exception if the deletions could
        // not be performed. If it completes normally, we can assume the files
        // were deleted (or never existed to begin with).
        return true;
    }

    /**
     * Create a new data collection on the server.
     * 
     * @param remoteUrl the url of the team server
     * @param type the type of resource collection to create
     * @return the ID of the newly created connection
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required.
     */
    public static String createNewCollection(URL remoteUrl,
            ResourceCollectionType type) throws IOException,
            LockFailureException {
        return createNewCollection(remoteUrl, type, null);
    }

    /**
     * Create a new data collection on the server.
     * 
     * @param remoteUrl the url of the team server
     * @param type the type of resource collection to create
     * @param desiredID the ID that should be used for the new collection
     * @return the ID of the newly created collection
     * @throws IOException if an IO error occurs
     * @throws LockFailureException if the team server rejects the request
     *      because a lock is required.
     */
    public static String createNewCollection(URL remoteUrl,
            ResourceCollectionType type, String desiredID) throws IOException,
            LockFailureException {
        Object[] params = new Object[desiredID == null ? 2 : 4];
        params[0] = NEW_COLLECTION_TYPE_PARAM;
        params[1] = type.toString();
        if (desiredID != null) {
            params[2] = NEW_COLLECTION_ID_PARAM;
            params[3] = desiredID;
        }

        byte[] results = doAnonymousPostRequest(remoteUrl,
            NEW_COLLECTION_ACTION, params);
        // the statement above will throw an exception if the collection could
        // not be created. If it completes normally, it will return the ID of
        // the newly created collection. Ensure the syntax meets expectations
        String result = new String(results, "UTF-8").trim();
        if (!result.matches("[0-9a-z]{1,10}"))
            throw new IOException("Server returned invalid collection token '"
                    + StringUtils.limitLength(result, 200) + "'");
        return result;
    }

    private boolean isSyncDownOnly(String resourceName) {
        return (syncDownOnlyFiles != null
                && syncDownOnlyFiles.accept(null, resourceName));
    }

    private boolean hashesMatch() throws IOException {
        // start by initiating the HTTP connection to the server
        URLConnection conn = makeGetRequest(HASHCODE_ACTION);
        conn.connect();
        // now, while the server is thinking, do our calculations locally.
        long localHash = ListingHashcodeCalculator.getListingHashcode(
            localCollection, ResourceFilterFactory.DEFAULT_FILTER);
        // finally, retrieve the value from the server and compare the two.
        HttpException.checkValid(conn);
        serverVersion = conn.getHeaderField(VERSION_HEADER);
        String hashResult = HTTPUtils.getResponseAsString(conn);
        try {
            long remoteHash = Long.valueOf(hashResult);
            return (localHash == remoteHash);
        } catch (NumberFormatException nfe) {
            throw new IOException("Server returned invalid collection hash '"
                    + StringUtils.limitLength(hashResult, 200) + "'");
        }
    }

    private long getMostRecentLocalModTime() {
        // find the most recent file modification time of all local files
        long result = 0;
        for (String name : localCollection.listResourceNames()) {
            long oneModTime = localCollection.getLastModified(name);
            result = Math.max(result, oneModTime);
        }
        return result;
    }

    private ResourceCollectionDiff getDiff() throws IOException {
        // start by initiating the HTTP connection to the server
        URLConnection conn = makeGetRequest(LIST_ACTION);
        conn.connect();
        // now, while the server is thinking, do our calculations locally.
        localCollection.validate();
        ResourceCollectionInfo localList = new ResourceListing(localCollection,
                ResourceFilterFactory.DEFAULT_FILTER);
        // finally, retrieve the list from the server and compare the two.
        serverVersion = conn.getHeaderField(VERSION_HEADER);
        ResourceCollectionInfo remoteList = XmlCollectionListing
                .parseListing(new BufferedInputStream(conn.getInputStream()));
        return new ResourceCollectionDiff(localList, remoteList);
    }

    private void applySyncFilter(ResourceCollectionDiff diff, SyncFilter filter) {
        if (diff != null && filter != null) {
            applySyncFilter(diff, filter, diff.getOnlyInA());
            applySyncFilter(diff, filter, diff.getOnlyInB());
            applySyncFilter(diff, filter, diff.getDiffering());
        }
    }

    private void applySyncFilter(ResourceCollectionDiff diff,
            SyncFilter filter, List<String> resourceNames) {
        for (Iterator<String> i = resourceNames.iterator(); i.hasNext();) {
            String resourceName = i.next();
            long localTime = diff.getA().getLastModified(resourceName);
            long remoteTime = diff.getB().getLastModified(resourceName);
            if (!filter.shouldSync(resourceName, localTime, remoteTime))
                i.remove();
        }
    }

    private void downloadFilesNamed(List filesToDownload) throws IOException {
        while (!filesToDownload.isEmpty()) {
            // Some web servers will reject an HTTP request if it includes too
            // many HTTP parameters. If we need to download more than 450
            // files, break the download request up into batches.
            List oneBatch;
            if (filesToDownload.size() < 450)
                oneBatch = filesToDownload;
            else
                oneBatch = filesToDownload.subList(0, 450);
            List params = new ArrayList();
            addMultiple(params, INCLUDE_PARAM, oneBatch);
            downloadFiles(makeGetRequest(DOWNLOAD_ACTION, params));
            oneBatch.clear();
        }
    }

    private ResourceCollectionInfo downloadFiles(URLConnection conn)
            throws IOException {
        ResourceCollectionInfo info = null;

        InputStream response = new BufferedInputStream(conn.getInputStream());
        ZipInputStream zipIn = new ZipInputStream(response);
        ZipEntry e;
        while ((e = zipIn.getNextEntry()) != null) {
            String name = e.getName();
            long modTime = 0;
            if (info != null)
                modTime = info.getLastModified(name);
            if (modTime <= 0)
                modTime = e.getTime();

            if (ResourceContentStream.MANIFEST_FILENAME.equals(name)) {
                InputStream infoIn = new ByteArrayInputStream(FileUtils
                        .slurpContents(zipIn, false));
                info = XmlCollectionListing.parseListing(infoIn);
                continue;
            }

            OutputStream out = localCollection.getOutputStream(name, modTime);
            if (out == null)
                // this would occur if we receive a file we don't recognize as
                // a member of our collection. Discard it.
                continue;
            logger.fine("downloading resource " + name);
            FileUtils.copyFile(zipIn, out);
            out.close();
            zipIn.closeEntry();
        }
        zipIn.close();
        return info;
    }

    private void addFileUploadParamsWithBatching(List params, String resourceName)
            throws IOException, LockFailureException {
        if (params.size() >= 100) {
            startZipUploadThread(params);
            doPostRequest(UPLOAD_ACTION, (Object[]) params.toArray());
            params.clear();
        }
        addFileUploadParams(params, resourceName);
    }

    private void addFileUploadParams(List params, String resourceName)
            throws IOException {
        if (addFileToZipUploadStream(params, resourceName) == false) {
            InputStream in = localCollection.getInputStream(resourceName);
            if (in == null)
                return;

            params.add(resourceName);
            params.add(in);
        }

        long modTime = localCollection.getLastModified(resourceName);
        if (modTime > 0) {
            params.add(UPLOAD_TIMESTAMP_PARAM_PREFIX + resourceName);
            params.add(Long.toString(modTime));
        }
    }

    private boolean addFileToZipUploadStream(List params, String resourceName)
            throws IOException {
        if (userName == null || isFileUploadZipSupported() == false)
            return false;
        if (localCollection.getLastModified(resourceName) <= 0)
            return false;

        ZipUploadStream zipStream;
        if (params.size() > 1 && params.get(1) instanceof ZipUploadStream) {
            zipStream = (ZipUploadStream) params.get(1);
        } else {
            zipStream = new ZipUploadStream();
            params.add(0, zipStream);
            params.add(0, UPLOAD_ZIP_PARAM);
        }
        zipStream.addFilename(resourceName);

        return true;
    }

    private boolean isFileUploadZipSupported() {
        if (fileUploadZipSupported == null) {
            fileUploadZipSupported = serverVersion != null
                    && VersionUtils.compareVersions(serverVersion,
                        ZIP_UPLOAD_MIN_SERVER_VERSION) >= 0;
        }
        return fileUploadZipSupported;
    }

    private Boolean fileUploadZipSupported;

    private static void startZipUploadThread(List params) {
        if (params.size() > 1 && params.get(1) instanceof ZipUploadStream)
            ((ZipUploadStream) params.get(1)).start();
    }

    private HttpURLConnection makeGetRequest(String action, List parameters)
            throws IOException {
        return makeGetRequest(action, parameters.toArray());
    }

    private HttpURLConnection makeGetRequest(String action,
            Object... parameters) throws IOException {
        StringBuffer request = new StringBuffer(remoteUrl);
        HTMLUtils.appendQuery(request, VERSION_PARAM, CLIENT_VERSION);
        HTMLUtils.appendQuery(request, EFFECTIVE_DATE_PARAM, effectiveDate);
        HTMLUtils.appendQuery(request, ACTION_PARAM, action);
        for (int i = 0; i < parameters.length; i += 2) {
            HTMLUtils.appendQuery(request, String.valueOf(parameters[i]),
                String.valueOf(parameters[i + 1]));
        }
        String requestStr = request.toString();

        URLConnection conn;
        if (requestStr.length() < MAX_URL_LENGTH) {
            URL url = new URL(requestStr);
            conn = url.openConnection();

        } else {
            int queryPos = requestStr.indexOf('?');
            byte[] query = requestStr.substring(queryPos + 1).getBytes(
                HTTPUtils.DEFAULT_CHARSET);
            URL url = new URL(requestStr.substring(0, queryPos));
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer
                    .toString(query.length));
            OutputStream outputStream = new BufferedOutputStream(conn
                    .getOutputStream());
            outputStream.write(query);
            outputStream.close();
        }

        return (HttpURLConnection) conn;
    }

    private void doPostRequest(String action, Object... params)
        throws IOException, LockFailureException {
        doPostRequest(action, null, params);
    }

    private void doLockPostRequest(String action, Object... params)
            throws IOException, LockFailureException {
        try {
            doPostRequest(action, offlineLockStatusResponseAnalyzer, params);
        } catch (HttpException.Forbidden f) {
            setOfflineLockStatus(OfflineLockStatus.NotLocked);
            throw new ReadOnlyUrlLockFailureException(f);
        }
    }

    private void doPostRequest(String action,
            HttpResponseAnalyzer responseAnalyzer, Object... params)
            throws IOException, LockFailureException {
        try {
            doPostRequest(new URL(remoteUrl), userName, userId,
                sourceIdentifier, extraLockData, responseAnalyzer, action,
                params);
        } catch (LockFailureException lfe) {
            if (lfe.isFatal())
                setOfflineLockStatus(OfflineLockStatus.NotLocked);
            throw lfe;
        }
    }

    private static byte[] doAnonymousPostRequest(URL remoteUrl, String action,
            Object... params) throws IOException, LockFailureException {
        return doPostRequest(remoteUrl, null, null, null, null, null, action,
            params);
    }

    private interface HttpResponseAnalyzer {
        public void analyze(URLConnection conn);
    }

    private static byte[] doPostRequest(URL remoteUrl, String userName,
            String userId, String sourceIdentifier, String extraLockData,
            HttpResponseAnalyzer responseAnalyzer, String action,
            Object... params) throws IOException,
            LockFailureException {

        if (HistoricalMode.isHistoricalModeEnabled())
            throw new IOException("Changes are not allowed in historical mode");

        if (userId == null)
            userId = getUserId();
        if (userId != null && userId.length() > 15)
            userId = userId.substring(0, 14) + "*";

        maybeRunPreflightGet(remoteUrl);

        ClientPostRequest request;
        if (needsMultipart(params))
            request = new ClientHttpRequest(remoteUrl);
        else
            request = new ClientFormRequest(remoteUrl);
        setRequestToken(request.getConnection());
        request.setParameter(VERSION_PARAM, CLIENT_VERSION);
        request.setParameter(ACTION_PARAM, action);
        maybeSetParameter(request, EXTRA_INFO_PARAM, userName);
        maybeSetParameter(request, USER_ID_PARAM, userId);
        maybeSetParameter(request, SOURCE_IDENTIFIER, sourceIdentifier);
        maybeSetParameter(request, EXTRA_LOCK_DATA, extraLockData);
        try {
            request.setParameters(params);
            InputStream in = request.post();
            if (responseAnalyzer != null)
                responseAnalyzer.analyze(request.getConnection());
            return FileUtils.slurpContents(in, true);
        } catch (IOException ioe) {
            checkForLockException(request.getConnection());
            throw HttpException.maybeWrap(request.getConnection(), ioe);
        }
    }

    private static void maybeRunPreflightGet(URL remoteUrl) throws IOException {
        if (Boolean.getBoolean(TLSConfig.POST_PREFLIGHT_PROP)) {
            String str = remoteUrl.toString();
            int pos = str.indexOf("/DataBridge");
            if (pos > 0) {
                String preflightUrl = str.substring(0, pos)
                        + "/pub/lib/jacsblank.html?n=" + NONCE++;
                URLConnection conn = new URL(preflightUrl).openConnection();
                FileUtils.slurpContents(conn.getInputStream(), true);
            }
        }
    }
    private static int NONCE = 1;

    private static boolean needsMultipart(Object[] params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof InputStream)
                    return true;
            }
        }
        return false;
    }

    private static void maybeSetParameter(ClientPostRequest request,
            String paramName, String paramValue) throws IOException {
        if (paramValue != null)
            request.setParameter(paramName, paramValue);
    }


    /**
     * Look at the headers from an HTTP response, and see if they specify an
     * offline lock status.  Based on the presence or absence of that header,
     * return the offline lock status that is in effect.
     * 
     * This method treats the absence of the offline lock status header as the
     * status "unsupported". Therefore, this should only be used on POST actions
     * that are expected to return the header (e.g., lock actions).
     */
    private static OfflineLockStatus getOfflineLockStatus(URLConnection conn) {
        OfflineLockStatus status = OfflineLockStatus.Unsupported;
        try {
            String statusHeader = conn.getHeaderField(OFFLINE_LOCK_HEADER);
            if (StringUtils.hasValue(statusHeader))
                status = OfflineLockStatus.valueOf(statusHeader);
        } catch (Exception e) {
            status = OfflineLockStatus.Unsupported;
        }
        return status;
    }

    private HttpResponseAnalyzer offlineLockStatusResponseAnalyzer =
        new HttpResponseAnalyzer() {
            public void analyze(URLConnection conn) {
                setOfflineLockStatus(getOfflineLockStatus(conn));
            }
        };


    /**
     * Look at the headers from an HTTP response, and see if they specify a lock
     * failure exception. If so, create an appropriate exception and throw it.
     * 
     * If the response headers do <b>not</b> specify a lock failure, this
     * method will do nothing and return.
     * 
     * @param conn
     *                a connection to a URL
     * @throws LockFailureException
     *                 if the connection was an HTTP connection and if the
     *                 response headers indicate a lock failure
     * @throws IOException
     *                 if an error occurred when attempting to examine the
     *                 response headers
     */
    private static void checkForLockException(URLConnection conn)
            throws IOException, LockFailureException {
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) conn;
            int code = http.getResponseCode();
            if (code != HttpURLConnection.HTTP_CONFLICT)
                return;

            String exceptionClass = http.getHeaderField(LOCK_EXCEPTION_HEADER);
            if (!StringUtils.hasValue(exceptionClass))
                return;

            if (exceptionClass.equals(AlreadyLockedException.class.getName())) {
                String extraInfo = http.getHeaderField(ALREADY_LOCKED_HEADER);
                throw new AlreadyLockedException(extraInfo);
            }

            LockFailureException lfe;
            try {
                Class clazz = Class.forName(exceptionClass);
                lfe = (LockFailureException) clazz.newInstance();
            } catch (Throwable t) {
                lfe = new LockFailureException(exceptionClass + ", "
                        + http.getResponseMessage());
            }
            throw lfe;
        }
    }

    private static List addMultiple(List l, String paramName, List values) {
        if (l == null)
            l = new ArrayList();
        for (Object value : values) {
            l.add(paramName);
            l.add(value);
        }
        return l;
    }

    private static String getUserId() {
        String result = null;
        try {
            result = HttpAuthenticator.getLastUsername();
        } catch (Throwable t) {
        }
        if (result == null)
            result = System.getProperty("user.name");
        return result;
    }

    // when constructing a URL to retrieve via the HTTP GET method, this
    // is our limit on the maximum allowed length of that URL. If the URL
    // is more than this many characters, it will be converted into a POST
    // request instead.
    private static final int MAX_URL_LENGTH = 512;

    /**
     * The version number of the protocol when the server first began supporting
     * ZIP upload content
     * 
     * (This was originally 3.6.1; but a bug was present in that initial logic.
     * We only want to use the corrected logic, which was released in 3.6.9.)
     */
    private static final String ZIP_UPLOAD_MIN_SERVER_VERSION = "3.6.9";

    private class ZipUploadStream extends PipedInputStream implements Runnable {

        private PipedOutputStream out;

        private Set<String> filenames;

        private ZipUploadStream() throws IOException {
            super(32768);
            out = new PipedOutputStream(this);
            filenames = new HashSet<String>();
        }

        public void addFilename(String filename) {
            filenames.add(filename);
        }

        public void start() {
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

        public void run() {
            try {
                writeFilesToZip();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                FileUtils.safelyClose(out);
                FileUtils.safelyClose(this);
            }
        }

        private void writeFilesToZip() throws IOException {
            ZipOutputStream zipOut = new ZipOutputStream(out);

            for (String oneName : filenames) {
                InputStream file = localCollection.getInputStream(oneName);
                if (file == null)
                    continue;

                try {
                    ZipEntry e = new ZipEntry(oneName);
                    long modTime = localCollection.getLastModified(oneName);
                    if (modTime > 0)
                        e.setTime(modTime);
                    zipOut.putNextEntry(e);
                    FileUtils.copyFile(file, zipOut);
                    zipOut.closeEntry();
                } finally {
                    FileUtils.safelyClose(file);
                }
            }

            zipOut.finish();
            zipOut.close();
        }
    }

    /**
     * @since 2.4.3
     */
    public static void setRequestToken(URLConnection conn) {
        String token = getRequestTokenFromCookie(conn);
        if (token != null)
            conn.setRequestProperty(REQUEST_TOKEN_HEADER, token);
    }

    private static String getRequestTokenFromCookie(URLConnection conn) {
        try {
            URI requestUri = conn.getURL().toURI();
            CookieManager cm = (CookieManager) CookieHandler.getDefault();
            for (HttpCookie c : cm.getCookieStore().get(requestUri)) {
                if (REQUEST_TOKEN_COOKIE.equals(c.getName()))
                    return c.getValue();
            }
        } catch (Exception e) {
        }
        return null;
    }

}
