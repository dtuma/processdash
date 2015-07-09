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

import static teamdash.wbs.WBSFilenameConstants.TEAM_LIST_FILENAME;
import static teamdash.wbs.WBSFilenameConstants.WBS_FILENAME;
import static teamdash.wbs.columns.TeamMemberTimeColumn.ASSIGNED_WITH_ZERO_SUFFIX;
import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX;
import static teamdash.wbs.columns.TopDownBottomUpColumn.TOP_DOWN_ATTR_SUFFIX;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.merge.TreeDiff;
import teamdash.merge.TreeNode;
import teamdash.team.TeamMember;
import teamdash.wbs.AbstractWBSModelMerger.WBSNodeContent;
import teamdash.wbs.WBSMerger;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class ProjectDiff {

    protected static final Resources resources = Resources
            .getDashBundle("WBSEditor.ChangeLog");

    protected ProjectHistory hist;

    protected String author;

    protected Date timestamp;

    protected Object versionA, versionB;

    protected Set<String> indivTimeAttrs;

    protected Map<String, String> memberZeroAttrs;

    protected Map<String, String> teamMemberNames;

    protected Map<String, String> changedInitialAttrs;

    protected WBSModel wbsA, wbsB;

    protected TreeDiff<Integer, WBSNodeContent> diff;

    public ProjectDiff(ProjectHistory hist, Object versionA, Object versionB,
            ProjectDiff other) throws IOException {
        this.hist = hist;
        this.versionA = versionA;
        this.versionB = versionB;
        this.author = hist.getVersionAuthor(versionB);
        this.timestamp = hist.getVersionDate(versionB);

        this.wbsB = getWbsModel(versionB, other);
        this.wbsA = getWbsModel(versionA, other);
        loadTeamMemberData();
    }

    private void loadTeamMemberData() throws IOException {
        Map<String, String> members = new HashMap<String, String>();
        indivTimeAttrs = new HashSet();
        memberZeroAttrs = new HashMap();
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
            String timeAttr = initials + TEAM_MEMBER_TIME_SUFFIX;
            String zeroAttr = initials + ASSIGNED_WITH_ZERO_SUFFIX;
            indivTimeAttrs.add(timeAttr);
            memberZeroAttrs.put(initials, zeroAttr);
            memberZeroAttrs.put(timeAttr, zeroAttr);
            teamMemberNames.put(initials, name);
            teamMemberNames.put(timeAttr, name);
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
            if (newInitials != null && !oldInitials.equals(newInitials)) {
                changedInitialAttrs.put(oldInitials + TEAM_MEMBER_TIME_SUFFIX,
                    newInitials + TEAM_MEMBER_TIME_SUFFIX);
                changedInitialAttrs.put(
                    oldInitials + ASSIGNED_WITH_ZERO_SUFFIX, //
                    newInitials + ASSIGNED_WITH_ZERO_SUFFIX);
            }
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


    protected Comparator<WBSNode> wbsNodeComparator = new Comparator<WBSNode>() {
        public int compare(WBSNode a, WBSNode b) {
            int aID = a.getTreeNodeID();
            int bID = b.getTreeNodeID();
            if (aID == bID)
                return 0;
            int result = getOrdinal(aID) - getOrdinal(bID);
            if (result == 0)
                result = a.getName().compareTo(b.getName());
            if (result == 0)
                result = aID - bID;
            return result;
        }
    };

    private Map<Integer, Integer> nodeOrdinals;

    private int getOrdinal(int nodeID) {
        if (nodeOrdinals == null) {
            nodeOrdinals = new HashMap<Integer, Integer>();
            collectNodeOrdinals(diff.getModifiedRoot());
        }
        Integer result = nodeOrdinals.get(nodeID);
        return (result == null ? -1 : result);
    }

    private void collectNodeOrdinals(TreeNode<Integer, WBSNodeContent> node) {
        nodeOrdinals.put(node.getID(), nodeOrdinals.size());
        for (TreeNode<Integer, WBSNodeContent> child : node.getChildren())
            collectNodeOrdinals(child);
    }



    protected class WBSDiffCalc extends WBSMerger {

        public WBSDiffCalc() {
            super(wbsA, wbsB, null);
        }

        @Override
        protected void tweakTreeNodeContent(WBSNodeContent content) {
            super.tweakTreeNodeContent(content);

            // a historical bug randomly wrote extraneous zeros into various
            // top-down attributes. Discard these to avoid false mismatches.
            for (Iterator i = content.entrySet().iterator(); i.hasNext();) {
                Entry<String, String> e = (Entry<String, String>) i.next();
                if (e.getKey().endsWith(TOP_DOWN_ATTR_SUFFIX)) {
                    if ("0.0".equals(e.getValue()) || "0".equals(e.getValue()))
                        i.remove();
                }
            }

            // if any team members changed initials, perform those remappings
            // in the data content node for the old WBS.
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

            // older versions of the WBS stored "assigned with zero" attrs for
            // a person with real time. Discard these to avoid false mismatches,
            // and otherwise normalize the handling of "assigned with zero"
            // attributes to simplify downstream comparison logic: put "0.0"
            // for assigned with zero people, and null for unassigned people.
            for (String indivAttr : indivTimeAttrs) {
                // see whether this person has an "assigned with zero" flag
                String zeroAttr = memberZeroAttrs.get(indivAttr);
                boolean hasAssignedZeroFlag = (content.remove(zeroAttr) != null);

                // if this person is assigned with zero, write "0.0" for time
                if (hasAssignedZeroFlag && !content.containsKey(indivAttr))
                    content.put(indivAttr, "0.0");
            }
        }

    }

}
