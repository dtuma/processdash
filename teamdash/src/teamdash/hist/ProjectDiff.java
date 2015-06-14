// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import static teamdash.wbs.AbstractWBSModelMerger.NODE_NAME;
import static teamdash.wbs.WBSFilenameConstants.TEAM_LIST_FILENAME;
import static teamdash.wbs.WBSFilenameConstants.WBS_FILENAME;
import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.merge.TreeDiff;
import teamdash.merge.TreeNodeChange;
import teamdash.merge.TreeNodeChange.Type;
import teamdash.team.TeamMember;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSMerger;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class ProjectDiff {

    private ProjectHistory hist;

    private String author;

    private Date timestamp;

    private Object versionA, versionB;

    private Set<String> indivTimeAttrs;

    private Map<String, String> teamMemberNames;

    private Map<String, String> changedInitialAttrs;

    private WBSModel wbsA, wbsB;

    private TreeDiff<Integer, WBSNodeContent> diff;

    public ProjectDiff(ProjectHistory hist, Object versionA, Object versionB,
            ProjectDiff other) throws IOException {
        this.hist = hist;
        this.versionA = versionA;
        this.versionB = versionB;
        this.author = hist.getVersionAuthor(versionB);
        this.timestamp = hist.getVersionDate(versionB);

        this.wbsA = getWbsModel(versionA, other);
        this.wbsB = getWbsModel(versionB, other);
        loadTeamMemberData();

        WBSDiffCalc wbsDiffCalc = new WBSDiffCalc();
        diff = wbsDiffCalc.getMainDiff();
    }

    public List<ProjectChange> getChanges() {
        List<ProjectChange> result = new ArrayList<ProjectChange>();
        Map<Integer, ProjectWbsNodeChange> nodeChanges = new HashMap();

        for (TreeNodeChange<Integer, WBSNodeContent> tnc : diff.getChanges()) {
            switch (tnc.getType()) {
            case Add:
                addNodeChange(tnc, nodeChanges, wbsB);
                break;
            case Delete:
                addNodeChange(tnc, nodeChanges, wbsA);
                break;
            case Move:
                addNodeMove(tnc, nodeChanges);
                break;
            case Reorder:
                break;
            case Edit:
                buildChangesForEditedNode(tnc, nodeChanges, result);
                break;
            }
        }
        result.addAll(nodeChanges.values());
        return result;
    }

    private void addNodeChange(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges, WBSModel wbs) {
        Type type = tnc.getType();
        Integer parentID = tnc.getParentID();
        if (diff != null && diff.getChangedNodeIDs(type).contains(parentID)) {
            // When an entire branch of the tree was added or deleted, we
            // only want to report the root node of the change.
            return;
        }

        addNodeChange(tnc, nodeChanges, wbs, type);
    }

    private void addNodeMove(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges) {
        Integer nodeID = tnc.getNodeID();
        WBSNode node = wbsA.getNodeMap().get(nodeID);
        WBSNode oldParent = wbsA.getParent(node);
        Object changeType = new ProjectWbsNodeChange.Moved(oldParent);
        addNodeChange(tnc, nodeChanges, wbsB, changeType);
    }

    private void buildChangesForEditedNode(
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges,
            List<ProjectChange> result) {
        Integer nodeID = tnc.getNodeID();
        if (nodeID < 0)
            return; // don't look for edits on the root node.

        WBSNodeContent base = diff.getBaseRoot().findNode(nodeID).getContent();
        WBSNodeContent mod = tnc.getNode().getContent();

        Set<String> attrNames = new HashSet<String>(base.keySet());
        attrNames.addAll(mod.keySet());
        Set<String> unchangedIndivTimes = new HashSet();
        Set<String> changedIndivTimes = new HashSet();
        for (String attr : attrNames) {
            String baseVal = base.get(attr);
            String modVal = mod.get(attr);
            if (baseVal != null && baseVal.equals(modVal)) {
                if (indivTimeAttrs.contains(attr))
                    unchangedIndivTimes.add(attr);
                continue;
            } else if (NODE_NAME.equals(attr)) {
                Object changeType = new ProjectWbsNodeChange.Renamed(baseVal);
                addNodeChange(tnc, nodeChanges, wbsB, changeType);
            } else if (indivTimeAttrs.contains(attr)) {
                changedIndivTimes.add(attr);
            }
        }

        if (!changedIndivTimes.isEmpty()) {
            WBSNode node = wbsB.getNodeMap().get(nodeID);
            result.add(new ProjectWbsTimeChange(node, base, mod,
                    unchangedIndivTimes, changedIndivTimes, teamMemberNames,
                    author, timestamp));
        }
    }

    private void addNodeChange(TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectWbsNodeChange> nodeChanges, WBSModel wbs,
            Object changeType) {
        Integer parentID = tnc.getParentID();
        Integer nodeID = tnc.getNodeID();
        WBSNode parent = wbs.getNodeMap().get(parentID);
        WBSNode node = wbs.getNodeMap().get(nodeID);
        if (node == null || parent == null)
            return; // shouldn't happen

        ProjectWbsNodeChange result = nodeChanges.get(parentID);
        if (result != null) {
            result.addChild(node, changeType);
        } else {
            result = new ProjectWbsNodeChange(parent, node, changeType, author,
                    timestamp);
            nodeChanges.put(parentID, result);
        }
    }

    private void loadTeamMemberData() throws IOException {
        Map<String, String> members = new HashMap<String, String>();
        indivTimeAttrs = new HashSet();
        teamMemberNames = new HashMap();
        Element teamB = parseXML(versionB, TEAM_LIST_FILENAME);
        NodeList indivNodes = teamB.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0; i < indivNodes.getLength(); i++) {
            Element indiv = (Element) indivNodes.item(i);
            String id = indiv.getAttribute(TeamMember.ID_ATTR);
            String initials = indiv.getAttribute(TeamMember.INITIALS_ATTR);
            String name = indiv.getAttribute(TeamMember.NAME_ATTR);
            if (XMLUtils.hasValue(id))
                members.put(id, initials);
            indivTimeAttrs.add(initials + TEAM_MEMBER_TIME_SUFFIX);
            teamMemberNames.put(initials, name);
            teamMemberNames.put(initials + TEAM_MEMBER_TIME_SUFFIX, name);
        }
        if (members.isEmpty())
            return;

        Element teamA = null;
        try {
            teamA = parseXML(versionA, TEAM_LIST_FILENAME);
        } catch (IOException ioe) {
            return;
        }
        Map changedInitialAttrs = new HashMap();
        indivNodes = teamA.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0; i < indivNodes.getLength(); i++) {
            Element indiv = (Element) indivNodes.item(i);
            String id = indiv.getAttribute(TeamMember.ID_ATTR);
            String oldInitials = indiv.getAttribute(TeamMember.INITIALS_ATTR);
            String newInitials = members.get(id);
            if (newInitials != null && !oldInitials.equals(newInitials))
                changedInitialAttrs.put(oldInitials + TEAM_MEMBER_TIME_SUFFIX,
                    newInitials + TEAM_MEMBER_TIME_SUFFIX);
        }
        if (!changedInitialAttrs.isEmpty())
            this.changedInitialAttrs = changedInitialAttrs;
    }

    private WBSModel getWbsModel(Object version, ProjectDiff other)
            throws IOException {
        if (other != null && version.equals(other.versionA))
            return other.wbsA;
        else if (other != null && version.equals(other.versionB))
            return other.wbsB;
        else {
            try {
                return new WBSModel(parseXML(version, WBS_FILENAME));
            } catch (FileNotFoundException fnfe) {
                if (version == hist.getVersions().get(0))
                    return new WBSModel("WBS", false);
                else
                    throw fnfe;
            }
        }
    }

    private Element parseXML(Object version, String filename)
            throws IOException {
        try {
            InputStream xml = hist.getVersionFile(version, filename);
            if (xml == null)
                throw new FileNotFoundException(filename + " / " + version);
            else
                return XMLUtils.parse(xml).getDocumentElement();
        } catch (SAXException se) {
            throw new IOException(se);
        }
    }

    public static List<ProjectChange> getChanges(ProjectHistory hist,
            Date beforeDate, int minNumChanges) throws IOException {
        List<ProjectChange> result = new ArrayList<ProjectChange>();
        List versions = hist.getVersions();
        String lastDateStr = null;
        ProjectDiff lastDiff = null;
        for (int i = versions.size(); i-- > 1;) {
            Object oneVersion = versions.get(i);
            Date versionDate = hist.getVersionDate(oneVersion);
            if (beforeDate != null && !versionDate.before(beforeDate))
                continue;

            String thisDateStr = ProjectChange.DATE_FMT.format(versionDate);
            if (minNumChanges > 0 && result.size() >= minNumChanges
                    && !thisDateStr.equals(lastDateStr))
                return result;
            lastDateStr = thisDateStr;

            Object prevVersion = versions.get(i - 1);
            ProjectDiff diff = new ProjectDiff(hist, prevVersion, oneVersion,
                    lastDiff);
            result.addAll(diff.getChanges());
            lastDiff = diff;
        }
        if (!result.isEmpty())
            result.get(result.size() - 1).setLastChangeFlag(true);
        return result;
    }

    private class WBSDiffCalc extends WBSMerger {

        public WBSDiffCalc() {
            super(wbsA, wbsB, null);
        }

        @Override
        protected void tweakTreeNodeContent(WBSNodeContent content) {
            super.tweakTreeNodeContent(content);
            if (changedInitialAttrs != null
                    && content.getWBSNode().getWbsModel() == wbsA) {
                Map<String, String> remapped = null;
                for (Entry<String, String> change : changedInitialAttrs
                        .entrySet()) {
                    String oldInitialsAttr = change.getKey();
                    String renamedTimeVal = content.remove(oldInitialsAttr);
                    if (renamedTimeVal != null) {
                        if (remapped == null)
                            remapped = new HashMap<String, String>();
                        String newInitialsAttr = change.getValue();
                        remapped.put(newInitialsAttr, renamedTimeVal);
                    }
                }
                if (remapped != null)
                    content.putAll(remapped);
            }
        }

    }

}
