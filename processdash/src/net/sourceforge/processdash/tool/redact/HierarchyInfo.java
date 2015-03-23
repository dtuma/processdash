// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/**
 * This class collects information about the nodes in the dashboard hierarchy of
 * a dataset that is being redacted.
 */
public class HierarchyInfo {

    /** A single node in the dashboard hierarchy */
    public class Node {

        public Node parent;

        public List<Node> children = new ArrayList<HierarchyInfo.Node>();

        public int depth;

        public String origName;

        public String newName;

        public String origPath;

        public String newPath;

        public boolean isPatternedName;

        public String templateId;

        public String dataFile;

        public String defectLog;

        public String projectId;

        public String wbsId;

        public String workflowSourceId;

        public String effectivePhase;

        public boolean isLeaf = true;

        public String remapRelativePath(String path) {
            return remapPathRelativeToNode(this, path);
        }

    }

    private List<Node> nodes;

    public HierarchyInfo(RedactFilterData data) throws IOException {
        buildNodeList(data);
    }

    public Node findNodeForDataFile(String dataFile) {
        if (StringUtils.hasValue(dataFile))
            for (Node n : nodes)
                if (dataFile.equals(n.dataFile))
                    return n;
        return null;
    }

    public Node findNodeForDefectFile(String defectFile) {
        if (StringUtils.hasValue(defectFile))
            for (Node n : nodes)
                if (defectFile.equals(n.defectLog))
                    return n;
        return null;
    }

    public Node findNodeForTeamProject(String projectId) {
        if (StringUtils.hasValue(projectId))
            for (Node n : nodes)
                if (projectId.equals(n.projectId))
                    return n;
        return null;
    }

    public void registerWorkflowNamesAsSafe(HierarchyNodeMapper nodeMapper) {
        for (Node n : nodes) {
            // When a workflow is applied in the WBS, the top-level node and the
            // generated children all receive workflow source IDs. In our case,
            // we want the names of generated children to be "safe," so we
            // look for a pattern where a parent and child both have source
            // IDs. in that case, we register the child's name as "safe".
            if (n.workflowSourceId != null && n.parent != null
                    && n.parent.workflowSourceId != null)
                nodeMapper.addSafeName(n.origName);
        }
    }

    public void remapNamesAndRegisterPatternedPaths(
            HierarchyNodeMapper nameMapper) {
        LinkedHashMap<String, String> patternedPaths = new LinkedHashMap();
        for (Node n : nodes) {
            if (n.isPatternedName == false)
                n.newName = nameMapper.mapName(n.origName);
            if (n.parent != null)
                n.newPath = n.parent.newPath + "/" + n.newName;
            if (n.isPatternedName) {
                patternedPaths.put(n.origPath, n.newPath);
                patternedPaths.put(n.origPath.substring(1),
                    n.newPath.substring(1));
            }
        }
        nameMapper.setPatternedPaths(patternedPaths);
    }


    private void buildNodeList(RedactFilterData data) throws IOException {
        try {
            Element xml = XMLUtils.parse(data.getStream("state"))
                    .getDocumentElement();

            nodes = new ArrayList<HierarchyInfo.Node>();
            Node rootNode = createRootNode();
            nodes.add(rootNode);

            createNodesForChildren(data, rootNode, xml);
        } catch (SAXException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    private Node createRootNode() {
        Node root = new Node();
        root.depth = 0;
        root.origName = root.origPath = root.newPath = "";
        root.dataFile = "global.dat";
        return root;
    }

    private void createNodesForChildren(RedactFilterData data, Node node,
            Element nodeXml) throws IOException {
        for (Element child : XMLUtils.getChildElements(nodeXml))
            createNodeForElement(data, node, child);
    }

    private void createNodeForElement(RedactFilterData data, Node parent,
            Element childXml) throws IOException {
        Node child = new Node();
        nodes.add(child);
        child.parent = parent;
        child.depth = parent.depth + 1;
        parent.children.add(child);
        parent.isLeaf = false;

        child.origName = childXml.getAttribute("name");
        child.origPath = parent.origPath + "/" + child.origName;
        child.templateId = nvl(childXml.getAttribute("templateID"));
        child.dataFile = nvl(childXml.getAttribute("dataFile"));
        child.defectLog = nvl(childXml.getAttribute("defectLog"));

        if (child.dataFile != null)
            scanDataFile(data, child);

        maybeAssignPatternedName(child);

        createNodesForChildren(data, child, childXml);
    }

    private void maybeAssignPatternedName(Node child) {
        if (child.projectId != null) {
            String newSuffix = "Team Project ";
            if (child.templateId != null
                    && child.templateId.contains("/Master"))
                newSuffix = "Master Project ";
            child.newName = newSuffix + child.projectId;
            child.isPatternedName = true;
        }
    }

    private String nvl(String s) {
        return ("".equals(s) ? null : s);
    }

    private void scanDataFile(RedactFilterData data, Node node)
            throws IOException {
        BufferedReader in = data.getFile(node.dataFile);
        String line;
        int pos;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("WBS_Unique_ID=\""))
                node.wbsId = line.substring(15);
            else if (line.startsWith("Project_ID=\""))
                node.projectId = line.substring(12);
            else if (line.startsWith("Workflow_Source_ID=\""))
                node.workflowSourceId = line.substring(20);
            else if ((pos = line.indexOf("/Effective_Phase=\"")) != -1)
                node.effectivePhase = line.substring(pos + 18);
        }
        in.close();
    }

    private String remapPathRelativeToNode(Node base, String path) {
        int basePrefixLen = base.origPath.length() + 1;
        Node subNode = findDeepestRelativeNode(base, basePrefixLen, path);
        if (subNode == base || subNode == null)
            return null;

        int matchLen = subNode.origPath.length() - basePrefixLen;
        String remainingText = path.substring(matchLen);

        String newSubpath = subNode.newPath.substring(base.newPath.length() + 1);
        return newSubpath + remainingText;
    }

    private Node findDeepestRelativeNode(Node underNode, int skipPrefixLen,
            String subpath) {

        for (Node child : underNode.children) {
            if (pathMatches(child.origPath, skipPrefixLen, subpath))
                return findDeepestRelativeNode(child, skipPrefixLen, subpath);
        }
        return underNode;
    }

    private boolean pathMatches(String fullPath, int skipPrefixLen,
            String relPath) {
        int remPathLen = fullPath.length() - skipPrefixLen;
        return (relPath.regionMatches(0, fullPath, skipPrefixLen, remPathLen)
                && (relPath.length() == remPathLen
                    || relPath.charAt(remPathLen) == '/'));
    }

}
