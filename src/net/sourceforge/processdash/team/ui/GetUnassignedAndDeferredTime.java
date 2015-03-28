// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.EditSubprojectList;
import net.sourceforge.processdash.team.sync.SyncWBS;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.XMLUtils;


public class GetUnassignedAndDeferredTime extends TinyCGIBase implements
        TeamDataConstants {

    public GetUnassignedAndDeferredTime() {
        this.charset = "utf-8";
    }

    @Override
    protected void writeHeader() {
        out.print("Content-type: application/xml; charset=utf-8\r\n\r\n");
    }

    @Override
    protected void writeContents() throws IOException {
        Node result = new Node();
        String projectPath = getPrefix();
        getUnassignedTimeForProject(result, projectPath);

        out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
        out.print("<unassignedAndDeferredTime forProject='");
        out.print(XMLUtils.escapeAttribute(projectPath));
        out.print("'>\n");
        writeXmlResults(result, 0);
        out.print("</unassignedAndDeferredTime>\n");
    }

    private void getUnassignedTimeForProject(Node result, String projectPath)
            throws IOException {
        if (isMaster(projectPath)) {
            ListData subprojects = ListData.asListData(getData(projectPath,
                EditSubprojectList.SUBPROJECT_PATH_LIST));
            if (subprojects != null)
                for (int i = 0; i < subprojects.size(); i++)
                    addUnassignedTime((String) subprojects.get(i), result);
        } else {
            addUnassignedTime(projectPath, result);
        }
    }

    private boolean isMaster(String projectPath) throws IOException {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(projectPath);
        String templateID = null;
        if (key != null)
            templateID = hierarchy.getID(key);

        if (templateID == null)
            throw new TinyCGIException(404, "Not a team project");
        else if (templateID.endsWith(SyncWBS.MASTER_ROOT))
            return true;
        else if (templateID.endsWith(SyncWBS.TEAM_ROOT))
            return false;
        else
            throw new TinyCGIException(404, "Not a team project");
    }

    private void addUnassignedTime(String projectPath, Node result)
            throws IOException {
        try {
            addUnassignedTimeImpl(projectPath, result);
        } catch (DataProblem dp) {
            result.getImmediateChild(projectPath).errorMessage = dp.getMessage();
        }
    }

    private void addUnassignedTimeImpl(String projectPath, Node result)
            throws IOException {
        ImportDirectory dir = getImportDir(projectPath);
        Element dumpXml = getProjectDump(dir);
        Map<String, String> team = getTeamMemberNames(dumpXml);
        addUnassignedTime("", dumpXml, team, result);
    }

    private ImportDirectory getImportDir(String projectPath) {
        return ImportDirectoryFactory.getInstance().get(
            getStringData(projectPath, TEAM_DATA_DIRECTORY_URL),
            getStringData(projectPath, TEAM_DATA_DIRECTORY));
    }

    private Element getProjectDump(ImportDirectory importDir) {
        File directory = null;
        if (importDir != null)
            directory = importDir.getDirectory();
        if (directory == null || !directory.isDirectory())
            throw new DataProblem(
                    "Could not find the directory containing WBS data");

        File f = new File(directory, SyncWBS.HIER_FILENAME);
        if (!f.isFile())
            throw new DataProblem("No WBS data was found");

        Element result;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            result = XMLUtils.parse(in).getDocumentElement();
        } catch (Exception e) {
            throw new DataProblem("Could not parse the WBS data file");
        }

        String version = result.getAttribute("dumpFileVersion");
        if (XMLUtils.hasValue(version) == false
                || DashPackage.compareVersions(version, MIN_WBS_VERSION) < 0)
            throw new DataProblem(
                    "Team members need to upgrade the Process Dashboard");

        return result;
    }

    private Map<String, String> getTeamMemberNames(Element dumpXml) {
        Map<String, String> result = new HashMap<String, String>();
        NodeList nl = dumpXml.getElementsByTagName("teamMember");
        for (int i = 0;  i < nl.getLength();  i++) {
            Element member = (Element) nl.item(i);
            String initials = member.getAttribute("initials");
            String name = member.getAttribute("name");
            result.put(initials, name);
        }
        result.put(UNASSIGNED, UNASSIGNED);
        return result;
    }

    private void addUnassignedTime(String path, Element dumpXml,
            Map<String, String> team, Node root) {
        getTime(path, dumpXml, TIME_ATTR, false, team, root);
        getTime(path, dumpXml, DEFERRED_TIME_ATTR, true, team, root);

        for (Element childElem : XMLUtils.getChildElements(dumpXml)) {
            String childName = childElem.getAttribute("name");
            if (XMLUtils.hasValue(childName))
                addUnassignedTime(path + "/" + childName, childElem, team, root);
        }
    }

    private void getTime(String path, Element xml, String attr,
            boolean includeAll, Map<String, String> team, Node root) {
        String attrVal = xml.getAttribute(attr);
        if (XMLUtils.hasValue(attrVal)) {
            Matcher m = TIME_ASSIGNMENT.matcher(attrVal);
            while (m.find()) {
                String initials = m.group(1);
                if (includeAll || UNASSIGNED.equals(initials)) {
                    String name = team.get(initials);
                    double minutes = Double.parseDouble(m.group(2)) * 60;
                    root.recordTime(path, name, minutes);
                }
            }
        }
    }

    private static final Pattern TIME_ASSIGNMENT = Pattern
            .compile(",(\\D+)=([0-9.]+)(?=,)");



    private void writeXmlResults(Node node, int depth) {
        if (node.children != null) {
            for (Map.Entry<String, Node> childEntry : node.children.entrySet()) {
                String name = childEntry.getKey();
                Node child = childEntry.getValue();
                if (child.errorMessage != null)
                    printXmlErrorNode(depth, name, child.errorMessage);
                else
                    printXmlResultNode(depth, name, child);
            }
        }
    }

    private void printXmlErrorNode(int depth, String path, String errorMessage) {
        printIndent(depth + 1);
        out.print("<error forProject='");
        out.print(XMLUtils.escapeAttribute(path));
        out.print("'>");
        out.print(XMLUtils.escapeAttribute(errorMessage));
        out.print("</error>\n");
    }

    private void printXmlResultNode(int depth, String name, Node node) {
        printIndent(depth + 1);
        out.print("<task name='");
        out.print(XMLUtils.escapeAttribute(name));
        out.print("'>\n");

        if (node.time != null) {
            for (Entry<String, Double> e : node.time.entrySet()) {
                printIndent(depth + 2);
                out.print("<cost who='");
                out.print(XMLUtils.escapeAttribute(e.getKey()));
                out.print("' pt='");
                out.print(formatNumber(e.getValue()));
                out.print("'/>\n");
            }
        }
        if (node.children != null) {
            writeXmlResults(node, depth + 1);
        }

        printIndent(depth + 1);
        out.print("</task>\n");
    }

    private String formatNumber(double num) {
        if (Math.floor(num) == num)
            return Integer.toString((int) num);
        else
            return Double.toString(num);
    }

    private void printIndent(int depth) {
        for (int i = depth; i-- > 0;)
            out.print(SPACER);
    }

    private static final String SPACER = "   ";



    private String getStringData(String prefix, String elem) {
        SimpleData val = getData(prefix, elem);
        return (val == null ? null : val.format());
    }

    private SimpleData getData(String prefix, String elem) {
        String dataName = DataRepository.createDataName(prefix, elem);
        return getDataRepository().getSimpleValue(dataName);
    }



    private class Node {

        Map<String, Double> time;

        Map<String, Node> children;

        private String errorMessage;

        public Node() {
            time = null;
            children = null;
        }

        public Node getChild(String name) {
            if (name == null || name.length() == 0 || name.equals("/"))
                return this;

            int slashPos = name.indexOf('/');
            if (slashPos == -1)
                return getImmediateChild(name);

            Node child = getImmediateChild(name.substring(0, slashPos));
            return child.getChild(name.substring(slashPos + 1));
        }

        private Node getImmediateChild(String name) {
            if (name == null || name.length() == 0)
                return this;
            if (children == null)
                children = new LinkedHashMap<String, Node>();
            Node result = children.get(name);
            if (result == null) {
                result = new Node();
                children.put(name, result);
            }
            return result;
        }

        public void recordTime(String path, String who, double minutes) {
            if (minutes > 0 && who != null)
                getChild(path).recordTime(who, minutes);
        }

        private void recordTime(String who, double minutes) {
            if (time == null)
                time = new HashMap<String, Double>();
            Double currentTime = time.get(who);
            if (currentTime == null)
                currentTime = 0.0;
            double newTime = currentTime + minutes;
            time.put(who, newTime);
        }

    }


    private class DataProblem extends RuntimeException {
        public DataProblem(String message) {
            super(message);
        }
    }

    /** This version of the WBS Editor was the first to write unassigned
     * time data in the dump file.  If we see a dump file written by an
     * earlier version, the unassigned time will be missing. */
    private static final String MIN_WBS_VERSION = "3.8.2";

    private static final String UNASSIGNED = "unassigned";

    private static final String TIME_ATTR = "time";

    private static final String DEFERRED_TIME_ATTR = "deferredTime";

}
