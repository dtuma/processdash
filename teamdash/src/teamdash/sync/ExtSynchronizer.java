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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.TeamProcess;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSClipSelection;
import teamdash.wbs.WBSFilenameConstants;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class ExtSynchronizer {

    private TeamProject teamProject;

    private WBSModel wbs;

    private String extSystemName;

    private String extSystemID;

    private String extIDAttr;

    private Map<String, WBSNode> extNodeMap;

    private Map<String, String> nameChanges;

    private boolean madeChange;

    public ExtSynchronizer(TeamProject teamProject, String extSystemName,
            String extSystemID) {
        this.teamProject = teamProject;
        this.wbs = this.teamProject.getWBS();
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.extIDAttr = this.extSystemID + " ID";
        this.madeChange = false;
        this.extNodeMap = buildExtNodeMap();
    }

    public void sync(List<ExtNode> extNodes) {
        createOrRenameNodes(extNodes);
    }

    public boolean wasChangeMade() {
        return madeChange;
    }


    private Map<String, WBSNode> buildExtNodeMap() {
        Map<String, WBSNode> result = new HashMap<String, WBSNode>();
        for (WBSNode node : wbs.getWbsNodes()) {
            String oneID = (String) node.getAttribute(extIDAttr);
            if (StringUtils.hasValue(oneID)) {
                if (result.containsKey(oneID)) {
                    // if the WBS contains two copies of a given external node,
                    // make the second one "normal." (This assists when the
                    // user has copied a node instead of moving it.)
                    node.removeAttribute(extIDAttr);
                    node.setReadOnly(false);
                    this.madeChange = true;
                } else {
                    result.put(oneID, node);
                }
            }
        }
        return result;
    }


    private void createOrRenameNodes(List<ExtNode> extNodes) {
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
            node.setAttribute(extIDAttr, extID);
            node.setReadOnly(true);
            wbs.addChild(parent, node);
            extNodeMap.put(extID, node);
            madeChange = true;

        } else if (!extName.equals(node.getName())) {
            // if the WBS node name doesn't match the name in the external
            // system, rename the node in the WBS
            node.setName(extName);
            nameChanges.put(Integer.toString(node.getUniqueID()), extName);
            madeChange = true;
        }
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
        return result;
    }


    private static final String INCOMING_PARENT_ID = "incoming";

    static final Logger log = Logger.getLogger(ExtSynchronizer.class.getName());

}
