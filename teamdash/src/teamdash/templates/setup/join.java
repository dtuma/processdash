// Copyright (C) 2002-2013 Tuma Solutions, LLC
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


package teamdash.templates.setup;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;


/** This class helps an individual to join a team project.
 */
public class join extends TinyCGIBase {

    private static final String JOIN_URL = "join.shtm";
    private static final String JOIN_XML = "joinxml.shtm";

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);

        maybeReroot();

        if (parameters.get("xml") != null)
            internalRedirect(JOIN_XML);
        else if (parameters.get("formdata") != null)
            printJoinFormData();
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

    protected void printRedirect(String filename) {
        out.print("Location: ");
        out.print(makeURI(filename));
        out.print("\r\n\r\n");
        out.flush();
    }

    private String makeURI(String filename) {
        return WebServer.urlEncodePath(getPrefix()) +
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

    protected String getProcessID() {
        String path = (String) env.get("SCRIPT_NAME");
        if (path == null || !path.startsWith("/")) return null;
        path = HTMLUtils.urlDecode(path).substring(1);
        int slashPos = path.indexOf('/');
        return path.substring(0, slashPos);
    }

}
