// Copyright (C) 2008-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

public interface ResourceBridgeConstants {

    /** 
     * name of request parameter indicating communications protocol version
     */
    String VERSION_PARAM = "protocolVersion";

    /**
     * name of response header indicating communications protocol version
     */
    String VERSION_HEADER = "X-Process-Dashboard-Bridge-Protocol-Version";

    /**
     * name of request parameter indicating a unique identifier for the
     * computer or system where the request originated
     */
    String SOURCE_IDENTIFIER = "sourceId";

    /** 
     * name of request parameter indicating an action for the bridge to take
     */
    String ACTION_PARAM = "action";

    /**
     * value of action parameter indicating that a client might like to initiate
     * a session with a particular server; the server should reply with an
     * acknowledgement if it is accepting new sessions
     */
    String SESSION_START_INQUIRY = "newSessionInquiry";

    /**
     * Optional parameter that can be included with a session start inquiry to
     * ask whether the user has a particular permission level for a given
     * collection. If they do not, the request will thrown a "403 Forbidden"
     * error code.
     * 
     * @since PDES 3.8.0
     */
    String ASSERT_PERMISSION_PARAM = "assertPermission";

    /**
     * Possible values for the ASSERT_PERMISSION_PARAM parameter.
     */
    enum Permission { read, write, admin };

    /**
     * value of action parameter indicating a collection hashcode should be
     * returned
     */
    String HASHCODE_ACTION = "hashcode";

    /**
     * value of action parameter indicating an XML collection listing should be
     * returned
     */
    String LIST_ACTION = "list";

    /**
     * value of action parameter indicating that a lock should be acquired on
     * this collection
     */
    String ACQUIRE_LOCK_ACTION = "aquireLock";

    /**
     * value of action parameter telling the bridge that we are still using the
     * lock on this collection
     */
    String PING_LOCK_ACTION = "pingLock";

    /**
     * value of action parameter requesting the bridge to assert the validity of
     * the lock on this collection
     */
    String ASSERT_LOCK_ACTION = "assertLock";

    /**
     * value of action parameter requesting the bridge to enable this lock
     * for offline use
     */
    String ENABLE_OFFLINE_LOCK_ACTION = "enableOfflineLock";

    /**
     * value of action parameter requesting the bridge to disable this lock
     * for offline use
     */
    String DISABLE_OFFLINE_LOCK_ACTION = "disableOfflineLock";

    /**
     * value of action parameter indicating that the lock should be released on
     * this collection
     */
    String RELEASE_LOCK_ACTION = "releaseLock";

    /**
     * when peforming lock operations, this request parameter indicates extra
     * information that should be used to uniquely identify the lock request
     */
    String EXTRA_LOCK_DATA = "extraLockData";

    /**
     * value of action parameter requesting the download of collection data
     */
    String DOWNLOAD_ACTION = "download";

    /**
     * value of action parameter asking to store data to the collection
     */
    String UPLOAD_ACTION = "upload";

    /**
     * when uploading files, a parameter name for a ZIP file containing all of
     * the files that should be uploaded
     * @since PDES 3.6.1
     */
    String UPLOAD_ZIP_PARAM = "uploadedFilesZip";

    /**
     * when uploading files, a prefix to append to filenames to produce a
     * parameter name carrying the modification time of the file
     */
    String UPLOAD_TIMESTAMP_PARAM_PREFIX = "timestamp|";

    /**
     * value of action parameter requesting the deletion of data from the
     * collection
     */
    String DELETE_ACTION = "delete";

    /**
     * for a deletion, this request parameter will name the resource to delete
     */
    String DELETE_FILE_PARAM = "deleteFile";

    /**
     * value of action parameter requesting that collection data be backed up
     */
    String BACKUP_ACTION = "backup";

    /**
     * for a backup, this request parameter will indicate the occasion for the
     * backup operation.
     */
    String BACKUP_QUALIFIER_PARAM = "qualifier";

    /**
     * value of action parameter requesting to download the most recent backup
     */
    String GET_BACKUP_ACTION = "getBackup";

    /**
     * value of an action parameter to start or check on an ext-sync operation
     * @since PDES 3.9.0
     */
    String EXT_SYNC_ACTION = "extSync";

    /**
     * optional parameter indicating that an ext-sync operation can proceed at
     * lower priority
     */
    String EXT_SYNC_LAZY_PARAM = "lazy";

    /**
     * optional parameter indicating that we would like to wait for an ext-sync
     * operation to finish
     */
    String EXT_SYNC_WAIT_PARAM = "maxWait";

    /**
     * optional parameter indicating a min timestamp that ext-sync operations
     * must complete after
     */
    String EXT_SYNC_MIN_TIMESTAMP_PARAM = "after";

    /**
     * value of action parameter requesting to retrieve a token that identifies
     * the location of this collection.
     */
    String LOCATION_TOKEN_ACTION = "getLocationToken";

    /**
     * when peforming write operations, this request parameter indicates the
     * human-readable name of the person performing the operation.
     */
    String EXTRA_INFO_PARAM = "extraInfo";

    /**
     * when peforming write operations, this request parameter indicates the
     * login id of the person performing the operation.
     */
    String USER_ID_PARAM = "effUserId";

    /**
     * when performing read operations, this optional request parameter
     * indicates the effective date of the data to retrieve.
     */
    String EFFECTIVE_DATE_PARAM = "effDate";

    /**
     * value of action parameter requesting that a new collection be created
     */
    String NEW_COLLECTION_ACTION = "newCollection";

    /**
     * for a new collection action, this request parameter indicates the type of
     * collection to be created.  Value should be a string representation of
     * a constant from {@link ResourceCollectionType}.
     */
    String NEW_COLLECTION_TYPE_PARAM = "newCollectionType";

    /**
     * for a new collection action, this request parameter indicates the desired
     * ID for the collection to be created.
     */
    String NEW_COLLECTION_ID_PARAM = "newCollectionId";

    /**
     * name of a cookie for passing a token used with state-changing requests
     */
    String REQUEST_TOKEN_COOKIE = "processDashboardRequestToken";

    /**
     * name of a parameter sent by the client with state-changing requests
     */
    String REQUEST_TOKEN_PARAM = REQUEST_TOKEN_COOKIE;

    /**
     * name of a header sent by the client with state-changing requests
     */
    String REQUEST_TOKEN_HEADER = "X-Process-Dashboard-Request-Token";

    /**
     * name of a header sent by the server to provide extended error info
     * 
     * @since PDES 3.8.3
     */
    String EXTENDED_ERROR_HEADER = "X-Process-Dashboard-Extended-Error";

    /**
     * value for the extended error header when a user has account flags
     */
    String USER_ACCOUNT_FLAG_ERROR_VALUE = "user-account-flag";

    /**
     * After a successful lock action, this HTTP response header will contain a
     * value from the {@link OfflineLockStatus} enumeration to indicate whether
     * the lock is enabled for offline use. (Older versions of the protocol,
     * which do not support offline mode, will not include this header.)
     * 
     * @since PDES 2.6
     */
    String OFFLINE_LOCK_HEADER = "X-Process-Dashboard-Lock-Offline-Status";

    /**
     * When a lock action or write action could not be completed, this HTTP
     * response header will contain the class name of the lock exception.
     */
    String LOCK_EXCEPTION_HEADER = "X-Process-Dashboard-Lock-Exception-Class";

    /**
     * When a lock action or write action could not be completed because someone
     * else owns the lock, this HTTP response header will contain the name of
     * the person holding the lock.
     */
    String ALREADY_LOCKED_HEADER = "X-Process-Dashboard-Locked-By";

}
