// Copyright (C) 2016 Tuma Solutions, LLC
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

package teamdash.templates.tools;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;

import teamdash.templates.tools.WorkflowMappingManager.Workflow;
import teamdash.wbs.ChangeHistory;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSEditor;
import teamdash.wbs.WorkflowWBSModel;

public class WorkflowMappingAltererProjectDir extends WorkflowMappingAlterer {

    private static final int PURPOSE = WorkingDirectoryFactory.PURPOSE_WBS
            + WorkingDirectoryFactory.PURPOSE_TEMP;

    private static final int LOCK_TIMEOUT_SECONDS = 20;

    private String userName;

    private WorkingDirectory dir;


    WorkflowMappingAltererProjectDir(String userName, String... locations) {
        this.userName = userName;
        this.dir = WorkingDirectoryFactory.getInstance()
                .get(PURPOSE, locations);
    }

    @Override
    public void applyChanges(Workflow workflow, Workflow target,
            Map<String, String> changes) throws WorkflowMappingException {

        String description = "[[" + dir.getDescription() + "]]";
        try {
            applyChangesImpl(workflow, target, changes);

        } catch (FileNotFoundException fnfe) {
            throw new WorkflowMappingException(fnfe, "Cannot_Find_Files_FMT",
                    description);

        } catch (IOException e) {
            String type = (description.startsWith("[[http") ? "Server" : "File");
            String errorCode = "Cannot_Save_" + type + "_FMT";
            throw new WorkflowMappingException(e, errorCode, description);

        } catch (AlreadyLockedException e) {
            throw new WorkflowMappingException(e, "Concurrent_Use.Message_FMT",
                    WBSEditor.getOtherLockHolder(e));

        } catch (ReadOnlyLockFailureException e) {
            throw new WorkflowMappingException(e, "Read_Only_Files_FMT",
                    description);

        } catch (LockFailureException e) {
            throw new WorkflowMappingException(e, "Cannot_Create_Lock_FMT",
                    description);
        }
    }

    @Override
    public void startChanges() throws IOException, LockFailureException {

        // request a process lock. We are writing to a temporary directory, so
        // this should always succeed; but subsequent working directory
        // operations require the process lock to be in place.
        dir.acquireProcessLock("Save workflow mappings", null);

        // prepare the working directory for use.
        dir.prepare();

        // lock the files in the directory, so only we can modify them.
        acquireWriteLock();

        // the dir.prepare() operation above will have copied down files if
        // necessary. But if we had to wait any length of time for a lock, it is
        // possible that the files have changed in the meantime. Call update()
        // again to ensure we have the most recent project data.
        if (dir instanceof BridgedWorkingDirectory) {
            ((BridgedWorkingDirectory) dir).setAllowUpdateWhenLocked(true);
            dir.update();
        }
    }

    private void acquireWriteLock() throws LockFailureException {
        long timeoutTimestamp = System.currentTimeMillis()
                + (LOCK_TIMEOUT_SECONDS * 1000);
        Random r = null;
        AlreadyLockedException ale = null;

        while (System.currentTimeMillis() < timeoutTimestamp) {
            try {
                dir.acquireWriteLock(null, userName);
                return;

            } catch (AlreadyLockedException e) {
                // if someone else is holding the lock, wait for a moment to see
                // if they release it. Then try again.
                ale = e;
                try {
                    // wait a randomly selected amount of time between 0.5 and
                    // 1.5 seconds. Randomness is included in case several
                    // processes are attempting to get the lock at the same time
                    if (r == null)
                        r = new Random();
                    Thread.sleep(500 + r.nextInt(1000));
                } catch (InterruptedException e1) {
                }
            }
        }

        // we were unable to secure a lock within a reasonable amount of time.
        throw (ale != null ? ale : new LockFailureException());
    }

    @Override
    public WorkflowWBSModel loadWorkflows() throws IOException {
        try {
            BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(workflowFile()));
            Element xml = XMLUtils.parse(in).getDocumentElement();
            return new WorkflowWBSModel(xml);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void saveWorkflows(WorkflowWBSModel workflows) throws IOException,
            LockFailureException {

        // save the workflows
        RobustFileWriter out = new RobustFileWriter(workflowFile(), "UTF-8");
        BufferedWriter buf = new BufferedWriter(out);
        workflows.getAsXML(buf);
        buf.flush();
        out.close();

        // add an entry to the change history file
        File changeHistoryFile = new File(dir.getDirectory(),
                TeamProject.CHANGE_HISTORY_FILE);
        ChangeHistory changeHistory = new ChangeHistory(changeHistoryFile);
        changeHistory.addEntry(userName);
        changeHistory.write(changeHistoryFile);

        // flush data to the server
        if (dir.flushData() == false)
            throw new IOException("Unable to save data");

        // save a data backup with the new contents
        dir.doBackup("saved_mappings");
    }

    private File workflowFile() {
        return new File(dir.getDirectory(), TeamProject.FLOW_FILENAME);
    }

    @Override
    public void finishChanges() throws IOException, LockFailureException {
        dir.releaseLocks();
        if (dir instanceof BridgedWorkingDirectory)
            FileUtils.deleteDirectory(dir.getDirectory(), true);
    }

}
