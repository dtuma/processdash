// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.TeamProcess;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSClipSelection;
import teamdash.wbs.WBSFilenameConstants;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSSynchronizer;

public class ExtSynchronizer {

    private TeamProject teamProject;

    private WBSModel wbs;

    private String extSystemName;

    private String extSystemID;

    private String extIDAttr;

    private SyncMetadata metadata;

    private Map<String, WBSNode> extNodeMap;

    private List<ExtChange> extChangesNeeded;

    private Set<String> newExtNodes;

    private Map<String, String> nameChanges;

    private Map<String, String> timeChanges;

    private boolean wbsChanged;

    public ExtSynchronizer(TeamProject teamProject, String extSystemName,
            String extSystemID, SyncMetadata metadata) {
        this.teamProject = teamProject;
        this.wbs = this.teamProject.getWBS();
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.extIDAttr = EXT_ATTR_PREFIX + extSystemID + " ID";
        this.metadata = metadata;
        this.wbsChanged = false;
        this.extNodeMap = buildExtNodeMap();
    }

    public void sync(List<ExtNode> extNodes) {
        this.extChangesNeeded = new ArrayList<ExtChange>();
        runReverseSync();
        createOrRenameNodes(extNodes);
        syncTimeEstimates(extNodes);
    }

    public boolean wasWbsChanged() {
        return wbsChanged;
    }

    public List<ExtChange> getExtChangesNeeded() {
        return extChangesNeeded;
    }


    private Map<String, WBSNode> buildExtNodeMap() {
        Map<String, WBSNode> result = new HashMap<String, WBSNode>();
        for (WBSNode node : wbs.getWbsNodes()) {
            String oneID = (String) node.getAttribute(extIDAttr);
            if (StringUtils.hasValue(oneID)) {
                if (result.containsKey(oneID)) {
                    // if the WBS contains two copies of a given external node,
                    // make the second one "normal."
                    ExtSyncUtil.removeExtNodeAttributes(node);
                    this.wbsChanged = true;
                } else {
                    result.put(oneID, node);
                }
            }
        }
        return result;
    }


    private void runReverseSync() {
        // If PDASH files contain unpublished modifications, apply them. This
        // will ensure that the estimate sync logic will be working against the
        // most up-to-date values.
        WBSSynchronizer reverseSync = new WBSSynchronizer(teamProject, null);
        reverseSync.run();
    }


    private void createOrRenameNodes(List<ExtNode> extNodes) {
        this.newExtNodes = new HashSet<String>();
        this.nameChanges = new HashMap<String, String>();
        WBSNode parent = getIncomingNodeParent();
        for (ExtNode extNode : extNodes)
            createOrRenameNode(extNode, parent);
    }

    private WBSNode getIncomingNodeParent() {
        WBSNode result = extNodeMap.get(INCOMING_PARENT_ID);
        if (result == null) {
            result = new WBSNode(wbs, "Incoming Items from " + extSystemName,
                    TeamProcess.COMPONENT_TYPE, 1, true);
            result.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR, extSystemID);
            result.setAttribute(extIDAttr, INCOMING_PARENT_ID);
            wbs.add(result);
        }
        result.setReadOnly(true);
        return result;
    }

    private void createOrRenameNode(ExtNode extNode, WBSNode parent) {
        // retrieve information about an external node
        String extID = extNode.getID();
        String extName = WBSClipSelection.scrubName(extNode.getName());

        // look for the external node in our WBS
        WBSNode node = extNodeMap.get(extID);
        if (node == null) {
            // if the node does not exist, create it.
            node = new WBSNode(wbs, extName, TeamProcess.COMPONENT_TYPE, 2,
                    false);
            node.setAttribute(ExtSyncUtil.EXT_SYSTEM_ID_ATTR, extSystemID);
            node.setAttribute(extIDAttr, extID);
            node.setReadOnly(true);
            wbs.addChild(parent, node);
            extNodeMap.put(extID, node);
            newExtNodes.add(extID);
            wbsChanged = true;

        } else if (!extName.equals(node.getName())) {
            // if the WBS node name doesn't match the name in the external
            // system, rename the node in the WBS
            node.setName(extName);
            nameChanges.put(Integer.toString(node.getUniqueID()), extName);
            wbsChanged = true;
        }
    }


    private void syncTimeEstimates(List<ExtNode> extNodes) {
        this.timeChanges = new HashMap<String, String>();
        WBSUtil wbsUtil = new WBSUtil(wbs, timeChanges,
                teamProject.getTeamMemberList());
        for (ExtNode extNode : extNodes) {
            syncTimeEstimate(wbsUtil, extNode);
        }
    }

    private void syncTimeEstimate(WBSUtil wbsUtil, ExtNode extNode) {
        // see if the external system has a time estimate for this node. If
        // not, make no changes.
        Double extTime = extNode.getEstimatedHours();
        if (extTime == null)
            return;

        // Look up the WBS node corresponding to this external node
        String extID = extNode.getID();
        WBSNode node = extNodeMap.get(extID);
        if (node == null)
            return;

        // if the user has placed one external node inside another in the WBS,
        // don't try to sync both time estimates.
        if (isNestedExtNode(node))
            return;

        // retrieve the WBS time estimate and the last sync values
        double wbsTime = wbsUtil.getEstimatedTime(node);
        double lastSyncTime = newExtNodes.contains(extID) ? 0.0
                : metadata.getNum(0.0, extID, EST_TIME, LAST_SYNC);

        // compare value pairs to identify changes that have been made
        boolean valuesMatch = eq(extTime, wbsTime);
        boolean extEdited = !eq(extTime, lastSyncTime);
        boolean wbsEdited = !eq(wbsTime, lastSyncTime);

        // compare estimates and determine if changes are needed
        if (valuesMatch) {
            // the time in the WBS agrees with the external system, so no
            // changes are needed to the estimates themselves. Update the
            // tracking metdata if needed.
            if (!eq(wbsTime, lastSyncTime))
                metadata.setNum(wbsTime, extID, EST_TIME, LAST_SYNC);
            metadata.setNum(null, extID, EST_TIME, OUTBOUND_VALUE);

        } else if (extEdited) {
            // the value in the external system has been edited since the last
            // sync. Copy the new value into the WBS.
            wbsUtil.changeTimeEstimate(node, wbsTime, extTime);
            metadata.setNum(extTime, extID, EST_TIME, LAST_SYNC);
            metadata.setNum(null, extID, EST_TIME, OUTBOUND_VALUE);
            wbsChanged = true;

        } else if (wbsEdited) {
            // if the value in the WBS has been edited since the last sync, we
            // need to copy that value back to the external system. Start by
            // recording a "pending outbound value" in the metadata.
            metadata.setNum(wbsTime, extID, EST_TIME, OUTBOUND_VALUE);

            // create an object to record the external change that is needed
            ExtChange change = new ExtChange();
            change.extNode = extNode;
            change.attrValues = Collections.singletonMap(EST_TIME,
                (Object) wbsTime);
            change.metadata = new SyncMetadata();
            change.metadata.setNum(wbsTime, extID, EST_TIME, LAST_SYNC);
            change.metadata.setStr(SyncMetadata.DELETE_METADATA, extID,
                EST_TIME, OUTBOUND_VALUE);
            extChangesNeeded.add(change);
        }
    }

    private boolean isNestedExtNode(WBSNode node) {
        // scan the WBS descendants of this node to see if any came from the
        // external system. If so, we will sync estimates for those children,
        // but not for this parent.
        for (WBSNode desc : wbs.getDescendants(node)) {
            if (desc.getAttribute(extIDAttr) != null)
                return true;
        }
        return false;
    }

    private boolean eq(double a, double b) {
        return (Math.abs(a - b) < 0.001);
    }


    public void updateProjDump() throws IOException {
        // get a collection of filters that should be applied to projDump.xml
        List<ProjDumpFilter> filters = getDumpFilters();
        if (filters.isEmpty())
            return;

        // open the dump file for reading/writing
        File dumpFile = new File(teamProject.getStorageDirectory(),
                WBSFilenameConstants.DATA_DUMP_FILE);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(dumpFile), "UTF-8"));
        RobustFileWriter out = new RobustFileWriter(dumpFile);

        // scan the lines in the dump file, filtering as we go
        String line;
        while ((line = in.readLine()) != null) {
            if (line.contains(" tid=")) {
                String nodeID = FilterUtils.getXmlAttr(line, "id");
                for (ProjDumpFilter f : filters)
                    line = f.filterLine(line, nodeID);
            }
            out.write(line);
            out.write("\n");
        }

        // close the streams, saving our changes
        in.close();
        out.close();
    }

    private List<ProjDumpFilter> getDumpFilters() {
        List<ProjDumpFilter> result = new ArrayList<ProjDumpFilter>();
        if (!nameChanges.isEmpty())
            result.add(new NodeRenameFilter(nameChanges));
        if (!timeChanges.isEmpty())
            result.add(new TimeChangeFilter(timeChanges));
        return result;
    }


    static final String EXT_ATTR_PREFIX = ExtSyncUtil.EXT_ATTR_PREFIX;

    private static final String INCOMING_PARENT_ID = "incoming";

    private static final String LAST_SYNC = "lastSyncValue";

    private static final String OUTBOUND_VALUE = "pendingOutboundValue";

    private static final String EST_TIME = ExtChange.EST_TIME_ATTR;

    static final Logger log = Logger.getLogger(ExtSynchronizer.class.getName());

}
