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

import static teamdash.wbs.WBSFilenameConstants.WBS_FILENAME;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.merge.TreeDiff;
import teamdash.merge.TreeNodeChange;
import teamdash.merge.TreeNodeChange.Type;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSMerger;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class ProjectDiff {

    private ProjectHistory hist;

    private String author;

    private Date timestamp;

    private Object versionA, versionB;

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

        WBSMerger wbsMerger = new WBSMerger(wbsA, wbsB, null);
        diff = wbsMerger.getMainDiff();
    }

    public List<ProjectChange> getChanges() {
        Map<Integer, ProjectChangedNode> nodeChanges = new HashMap();

        for (TreeNodeChange<Integer, WBSNodeContent> tnc : diff.getChanges()) {
            switch (tnc.getType()) {
            case Add:
            case Move:
                getNodeChange(diff, tnc, nodeChanges, wbsB);
                break;
            case Delete:
                getNodeChange(diff, tnc, nodeChanges, wbsA);
                break;
            case Reorder:
                break;
            case Edit:
                break;
            }
        }
        List<ProjectChange> result = new ArrayList<ProjectChange>(
                nodeChanges.values());
        return result;
    }

    private void getNodeChange(TreeDiff<Integer, WBSNodeContent> diff,
            TreeNodeChange<Integer, WBSNodeContent> tnc,
            Map<Integer, ProjectChangedNode> nodeChanges, WBSModel wbs) {
        Type type = tnc.getType();
        Integer parentID = tnc.getParentID();
        if (diff != null && diff.getChangedNodeIDs(type).contains(parentID)) {
            // When an entire branch of the tree was added or deleted, we
            // only want to report the root node of the change.
            return;
        }

        Integer nodeID = tnc.getNodeID();
        WBSNode parent = wbs.getNodeMap().get(parentID);
        WBSNode node = wbs.getNodeMap().get(nodeID);
        if (node == null || parent == null)
            return; // shouldn't happen

        ProjectChangedNode result = nodeChanges.get(parentID);
        if (result != null) {
            result.addChild(node, type);
        } else {
            result = new ProjectChangedNode(parent, node, type, author,
                    timestamp);
            nodeChanges.put(parentID, result);
        }
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

}
