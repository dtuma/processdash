// Copyright (C) 2002-2022 Tuma Solutions, LLC
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


package net.sourceforge.processdash.team.setup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;


/** This class helps an individual to join a team project.
 */
public class JoinTeamProject extends TinyCGIBase implements TeamDataConstants {

    private static final String JOIN_URL = "join.shtm";
    private static final String JOIN_XML = "joinxml.shtm";
    private static final String WORKING_DIR_DATA_NAME = "Join//Working_Dir";
    private static final String SHARED_FOLDER_DATA_NAME = "Join//Shared_Folder";
    private static final String REQTS_DATA_NAME = "Join//Package_Reqts";

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        charset = "UTF-8";
        super.service(in, out, env);

        maybeReroot();
        storeSharedFolderPath();
        storePackageRequirements();

        if (parameters.get("xml") != null)
            internalRedirect(JOIN_XML);
        else if (parameters.get("formdata") != null)
            printJoinFormData();
        else if (parameters.get("invitation") != null)
            printRelaunchJoinInvitation();
        else if (parameters.get("pdash") != null)
            writePdashJoinInvitation();
        else
            printRedirect(JOIN_URL);
    }

    /** the HTTPURLConnection in JRE1.4.1 can't seem to handle the
     * redirection instructions sent by the dashboard. Since xml
     * output from this script is read by the JRE rather than a web
     * browser, this causes problems.  Work around the problems by
     * performing an "internal redirect" ourselves.
     */
    protected void internalRedirect(String filename) throws IOException {
        String uri = makeURI(filename);
        byte[] contents = getTinyWebServer().getRequest(uri, false);
        outStream.write(contents);
        outStream.flush();
    }

    /**
     * The "join team project" page includes a form with a number of hidden
     * data elements.  These elements contain the same information as the
     * attributes in the joining document.  Read the XML joining document
     * and generate an HTML fragment that communicates the same data.
     */
    private void printJoinFormData() throws IOException {
        super.writeHeader();

        Element joinXml;
        try {
            String uri = makeURI(JOIN_XML);
            String joinInfo = getTinyWebServer().getRequestAsString(uri);
            joinXml = XMLUtils.parse(joinInfo).getDocumentElement();
        } catch (SAXException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }

        NamedNodeMap attributes = joinXml.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.item(i).getNodeName();
            String value = attributes.item(i).getNodeValue();
            out.print("<input type=\"hidden\" name=\"");
            out.print(HTMLUtils.escapeEntities(name));
            out.print("\" value=\"");
            out.print(HTMLUtils.escapeEntities(value));
            out.print("\">\n");
        }
        out.flush();
    }

    /**
     * When a project is relaunched, this method prints an XML file containing a
     * message inviting the individual to join the new project. The file is in
     * 'pdash message' format, ready to be embedded in a PDASH file.
     */
    private void printRelaunchJoinInvitation() throws IOException {
        super.writeHeader();

        out.write(XML_HEADER);
        out.write("<messages>\n");

        // if this project was relaunched, print an invitation to join the
        // new project. Otherwise print no message, leaving this file empty.
        maybeWriteRelaunchInvitation();

        out.write("</messages>\n");
        out.flush();
    }

    private void maybeWriteRelaunchInvitation() throws IOException {
        // only print invitations if this project was relaunched.
        if (getData(TeamDataConstants.RELAUNCHED_PROJECT_FLAG) == null)
            return;

        // do not print rejoin invitations if this project is hosted by a
        // team server. (The team server will send those invitations.)
        if (getData(TeamDataConstants.TEAM_DATA_DIRECTORY_URL) != null)
            return;

        // find the new project that this project was relaunched to.
        String projectID = getData(TeamDataConstants.PROJECT_ID);
        String newProjectPath = findProjectWithRelaunchSourceID(projectID,
            getPSPProperties(), PropertyKey.ROOT);
        if (newProjectPath == null)
            return;

        // get the ID of the new relaunched project.
        String newProjectID = getData(newProjectPath,
            TeamDataConstants.PROJECT_ID);

        // retrieve the joining document for the new relaunched project
        String uri = makeURI(newProjectPath, "join.class?xml");
        String joinInfo = getTinyWebServer().getRequestAsString(uri);
        int pos = joinInfo.indexOf("?>");
        if (pos != -1)
            joinInfo = joinInfo.substring(pos + 2); // strip XML prolog

        // write out a message with a join invitation.
        out.write("\n<message type='pdash.joinTeamProject' msgId='"
                + newProjectID + "'>\n");
        out.write(joinInfo);
        out.write("</message>\n\n");
    }

    private String findProjectWithRelaunchSourceID(String relaunchSourceID,
            DashHierarchy hier, PropertyKey node) {
        // is this node a team project with the given relaunch source ID? If
        // so, return its path.
        String nodeRelaunchSourceID = getData(node.path(),
            TeamDataConstants.RELAUNCH_SOURCE_PROJECT_ID);
        if (relaunchSourceID.equals(nodeRelaunchSourceID))
            return node.path();

        // recurse over children, looking for the given project.
        for (int i = hier.getNumChildren(node); i-- > 0;) {
            PropertyKey child = hier.getChildKey(node, i);
            String childResult = findProjectWithRelaunchSourceID(
                relaunchSourceID, hier, child);
            if (childResult != null)
                return childResult;
        }

        // no project was found with this relaunch source ID.
        return null;
    }

    /**
     * Write a PDASH file containing an invitation for joining this project
     */
    private void writePdashJoinInvitation() throws IOException {
        // write the HTTP header fields, including a suggested name for the file
        String projectName = getPrefix();
        projectName = projectName.substring(projectName.lastIndexOf('/') + 1);
        String filename = "join_project_" + FileUtils.makeSafe(projectName);
        out.print("Content-type: application/octet-stream\r\n");
        out.print("Content-Disposition: attachment; " + "filename=\""
                + filename + ".pdash\"\r\n\r\n");
        out.flush();

        // create a ZIP output stream for the data
        ZipOutputStream zipOut = new ZipOutputStream(outStream);

        // write the PDASH manifest file
        zipOut.putNextEntry(new ZipEntry("manifest.xml"));
        zipOut.write((MANIFEST_XML_1 + System.currentTimeMillis()
                + MANIFEST_XML_2).getBytes("UTF-8"));
        zipOut.closeEntry();

        // write the message document containing the joining invitation
        zipOut.putNextEntry(new ZipEntry("joinProject.xml"));
        zipOut.write(getXmlJoinMessageDocument().getBytes("UTF-8"));
        zipOut.closeEntry();

        zipOut.finish();
        zipOut.close();
    }

    private String getXmlJoinMessageDocument() throws IOException {
        // open the document
        StringBuilder result = new StringBuilder();
        result.append(XML_HEADER);
        result.append("<messages>\n");

        // write out a message with a join invitation.
        String projectID = getData(TeamDataConstants.PROJECT_ID);
        result.append("\n<message type='pdash.joinTeamProject' msgId='"
                + projectID + "." + System.currentTimeMillis() + ".*'>\n");

        // retrieve the XML joining document
        String uri = makeURI(JOIN_XML);
        String joinInfo = getTinyWebServer().getRequestAsString(uri);
        int pos = joinInfo.indexOf("?>");
        if (pos != -1)
            joinInfo = joinInfo.substring(pos + 2); // strip XML prolog
        result.append(joinInfo);

        // close the document
        result.append("</message>\n\n");
        result.append("<deleteEnclosingArchiveFromImport/>\n\n");
        result.append("</messages>\n");
        return result.toString();
    }

    private String getData(String dataName) {
        return getData(getPrefix(), dataName);
    }

    private String getData(String prefix, String name) {
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData sd = getDataRepository().getSimpleValue(dataName);
        return ((sd != null && sd.test()) ? sd.format() : null);
    }

    protected void printRedirect(String filename) {
        out.print("Location: ");
        out.print(makeURI(filename));
        out.print("\r\n\r\n");
        out.flush();
    }

    private String makeURI(String filename) {
        return makeURI(getPrefix(), filename);
    }

    private String makeURI(String prefix, String filename) {
        return WebServer.urlEncodePath(prefix) +
            "//" + getProcessID() + "/setup/" + filename;
    }

    /** If the current prefix doesn't name the root of a team project,
     * search upward through the hierarchy to find the project root,
     * and change the active prefix to name that node. */
    protected void maybeReroot() throws TinyCGIException {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        boolean rerooted = false;
        String projectRoot = null;
        do {
            String templateID = hierarchy.getID(key);
            if (templateID != null && templateID.endsWith("/TeamRoot")) {
                projectRoot = key.path();
                break;
            }
            rerooted = true;
            key = key.getParent();
        } while (key != null);

        if (rerooted) {
            if (projectRoot != null)
                parameters.put("hierarchyPath", projectRoot);
            else
                throw new TinyCGIException(404, "Not Fount");
        }
    }

    private void storeSharedFolderPath() {
        DataContext data = getDataContext();

        // store the target dir of our working directory
        File targetDir = getDashboardContext().getWorkingDirectory()
                .getTargetDirectory();
        data.putValue(WORKING_DIR_DATA_NAME, targetDir == null ? null
                : StringData.create(targetDir.getAbsolutePath()));

        // try remapping the team directory as a shared folder path
        String sf = getSharedFolderFor(data, TeamDataConstants.TEAM_DIRECTORY);
        if (sf == null)
            sf = getSharedFolderFor(data, TeamDataConstants.TEAM_DIRECTORY_UNC);
        data.putValue(SHARED_FOLDER_DATA_NAME,
            sf == null ? null : StringData.create(sf));
    }

    private String getSharedFolderFor(DataContext data, String dataName) {
        // get the path stored in this data name (could be null)
        String path = getString(data, dataName);
        if (".".equals(path))
            // resolve "." if necessary
            path = getString(data, WORKING_DIR_DATA_NAME);

        // try reencoding the path as a shared folder. If that succeeded,
        // return the encoded value. Otherwise return null.
        String encoded = FolderMappingManager.getInstance().encodePath(path);
        if (encoded == null || encoded.equals(path))
            return null;
        else
            return encoded;
    }

    private void storePackageRequirements() {
        DataContext data = getDataContext();
        String reqts = calculatePackageRequirements(data);
        data.putValue(REQTS_DATA_NAME, //
            reqts == null ? null : StringData.create(reqts));
    }

    private String calculatePackageRequirements(DataContext data) {
        // see if this project is using custom size metrics
        boolean isCustomSize = testData(data, WBS_CUSTOM_SIZE_DATA_NAME);

        // if the project is using bundled file format, use the minimum
        // requirements for the effective bundle mode
        FileBundleMode bundleMode = getProjectBundleMode(data);
        if (bundleMode != null) {
            Map<String, String> reqts = new TreeMap<String, String>(
                    bundleMode.getMinVersions());
            reqts.remove(isCustomSize ? "teamTools" : "teamToolsB");
            return DataVersionChecker.formatRequirementsString(reqts);
        }

        // if the project is using dynamic custom size metrics, return the
        // version string for custom size support
        if (isCustomSize)
            return "pspdash version 2.6.3; teamToolsB version 6.0.0";

        // if the project is using WBS managed size, return the appropriate
        // version string
        if (testData(data, WBS_SIZE_DATA_NAME))
            return "pspdash version 2.5.3; teamTools version 5.0.0";

        // no specific version requirements found
        return null;
    }

    private FileBundleMode getProjectBundleMode(DataContext data) {
        // if this project is hosted by a PDES, it is not bundled
        if (testData(data, TEAM_DATA_DIRECTORY_URL))
            return null;

        // get the directory where the data is stored
        String projDirPath = getString(data, TEAM_DATA_DIRECTORY);
        String remapped = ExternalResourceManager.getInstance()
                .remapFilename(projDirPath);
        if (remapped == null)
            return null;
        try {
            return FileBundleUtils.getBundleMode(new File(remapped));
        } catch (IOException ioe) {
            return null;
        }
    }

    private boolean testData(DataContext data, String name) {
        return getString(data, name) != null;
    }

    private String getString(DataContext data, String name) {
        SimpleData sd = data.getSimpleValue(name);
        return (sd != null && sd.test() ? sd.format() : null);
    }

    protected String getProcessID() {
        String path = (String) env.get("SCRIPT_NAME");
        if (path == null || !path.startsWith("/")) return null;
        path = HTMLUtils.urlDecode(path).substring(1);
        int slashPos = path.indexOf('/');
        return path.substring(0, slashPos);
    }

    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n";

    private static final String MANIFEST_XML_1 = XML_HEADER
            + "<archive type='dashboardDataExport'>\n"
            + "   <exported when='@";
    private static final String MANIFEST_XML_2 = "' />\n"
            + "   <file name='joinProject.xml' type='messages' version='1' />\n"
            + "</archive>";

}
