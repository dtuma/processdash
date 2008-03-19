// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

public interface ResourceBridgeConstants {

    /** name of request parameter indicating communications protocol version */
    String VERSION_PARAM = "protocolVersion";

    /** name of response header indicating communications protocol version */
    String VERSION_HEADER = "X-Process-Dashboard-Bridge-Protocol-Version";

    /** name of request parameter indicating an action for the bridge to take */
    String ACTION_PARAM = "action";

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
     * value of action parameter indicating that the lock should be released on
     * this collection
     */
    String RELEASE_LOCK_ACTION = "releaseLock";

    /**
     * value of action parameter requesting the download of collection data
     */
    String DOWNLOAD_ACTION = "download";

    /**
     * value of action parameter asking to store data to the collection
     */
    String UPLOAD_ACTION = "upload";

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
     * when peforming write operations, this request parameter indicates the
     * human-readable name of the person performing the operation.
     */
    String EXTRA_INFO_PARAM = "extraInfo";

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
