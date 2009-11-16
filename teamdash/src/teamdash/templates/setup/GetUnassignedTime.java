package teamdash.templates.setup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class GetUnassignedTime extends TinyCGIBase implements TeamDataConstants {

    public GetUnassignedTime() {
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
        out.print("<unassignedTime forProject='");
        out.print(XMLUtils.escapeAttribute(projectPath));
        out.print("'>\n");
        writeXmlResults(result, 0);
        out.print("</unassignedTime>\n");
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
        else if (templateID.endsWith(sync.MASTER_ROOT))
            return true;
        else if (templateID.endsWith(sync.TEAM_ROOT))
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
        addUnassignedTime("", dumpXml, result);
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

        File f = new File(directory, sync.HIER_FILENAME);
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

    private void addUnassignedTime(String path, Element dumpXml, Node root) {
        double time = 0;
        time += getTime(dumpXml, TIME_ATTR, false);
        time += getTime(dumpXml, DEFERRED_TIME_ATTR, true);
        if (time > 0)
            root.getChild(path).time += time;

        for (Element childElem : XMLUtils.getChildElements(dumpXml)) {
            String childName = childElem.getAttribute("name");
            if (XMLUtils.hasValue(childName))
                addUnassignedTime(path + "/" + childName, childElem, root);
        }
    }

    private double getTime(Element xml, String attr, boolean includeAll) {
        double result = 0;
        String attrVal = xml.getAttribute(attr);
        if (XMLUtils.hasValue(attrVal)) {
            Matcher m = TIME_ASSIGNMENT.matcher(attrVal);
            while (m.find()) {
                if (includeAll || UNASSIGNED.equals(m.group(1))) {
                    result += Double.parseDouble(m.group(2));
                }
            }
        }
        return result;
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
        if (node.time > 0) {
            out.print("' pt='");
            out.print(formatNumber(node.time * 60));
        }
        if (node.children == null) {
            out.print("'/>\n");
        } else {
            out.print("'>\n");
            writeXmlResults(node, depth + 1);
            printIndent(depth + 1);
            out.print("</task>\n");
        }
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

        double time;

        Map<String, Node> children;

        private String errorMessage;

        public Node() {
            time = 0;
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
