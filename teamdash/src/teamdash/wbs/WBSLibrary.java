// Copyright (C) 2002-2016 Tuma Solutions, LLC
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


package teamdash.wbs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.processdash.util.RobustFileWriter;

import teamdash.XMLUtils;
import teamdash.templates.tools.WorkflowMappingManager;


public abstract class WBSLibrary {

    private static final String PROCESS_NAME_ATTR = "processName";
    private static final String PROCESS_VERSION_ATTR = "processVersion";
    private static final String LIBRARY_ID_ATTR = "libraryID";
    private static final String EXPORT_TIMESTAMP_ATTR = "exportTime";

    private File file;
    private String processName;
    private String processVersion;
    private String libraryID;
    private WBSModel wbs;



    public WBSLibrary(File f) throws IOException {
        this.file = f;
        load(new FileInputStream(f));
    }

    public WBSLibrary(File f, TeamProcess process) throws IOException {
        if (f.exists())
            throw new IOException("File already exists.");

        this.file = f;
        this.processName = process.getProcessName();
        this.processVersion = process.getProcessVersion();
        this.wbs = createCleanWbs();
    }

    public WBSLibrary(String[] urls, TeamProcess process) {
        WBSModel mergedWbs = createCleanWbs();

        for (String oneUrl : urls) {
            try {
                // load the libraries, one at a time; skip incompatible ones
                load(new URL(oneUrl).openStream());
                if (!XMLUtils.hasValue(libraryID) || !compatible(process))
                    continue;

                // merge each compatible library into the target object
                setImportSourceIDs();
                mergedWbs.replaceBaseItems(wbs, getAllDefinitionNames(wbs));
            } catch (IOException ioe) {
                // skip URLs that cause problems
            }
        }

        // store the merged library WBS into this object
        this.wbs = mergedWbs;
        this.processName = process.getProcessName();
        this.processVersion = process.getProcessVersion();
    }

    private WBSModel createCleanWbs() {
        WBSModel result = createEmptyWbs();
        // The constructor for WBSModel will create a 'default' WBS which we
        // don't want.  Delete those contents, leaving only the root.
        WBSNode[] children = result.getDescendants(result.getRoot());
        result.deleteNodes(Arrays.asList(children));
        return result;
    }

    protected abstract String getRootTag();

    protected abstract WBSModel createEmptyWbs();

    protected abstract WBSModel loadFromXml(Element xml);

    public String getProcessName() {
        return processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public WBSModel getWbs() {
        return wbs;
    }

    public boolean isNotEmpty() {
        return !wbs.isLeaf(wbs.getRoot());
    }

    public boolean isValid() {
        return (processName != null && processVersion != null && wbs != null);
    }

    public boolean compatible(TeamProcess process) {
        return getProcessName().equals(process.getProcessName()) &&
            getProcessVersion().equals(process.getProcessVersion());
    }

    public void setImportSourceIDs() {
        for (WBSNode node : wbs.getWbsNodes()) {
            String sourceID = libraryID + ":" + node.getUniqueID();
            node.setAttribute("importSourceID", sourceID);
            node.removeAttribute("exportSourceID");
            node.removeAttribute("relaunchSourceID");
        }
    }


    private void load(InputStream in) throws IOException {
        try {
            Document doc = XMLUtils.parse(in);
            Element xml = doc.getDocumentElement();
            if (getRootTag().equals(xml.getTagName())) {
                processName = xml.getAttribute(PROCESS_NAME_ATTR);
                processVersion = xml.getAttribute(PROCESS_VERSION_ATTR);
                libraryID = xml.getAttribute(LIBRARY_ID_ATTR);
                Element wbsElement = (Element) xml.getElementsByTagName
                    (WBSModel.WBS_MODEL_TAG).item(0);
                wbs = loadFromXml(wbsElement);

                if (file != null && !XMLUtils.hasValue(libraryID))
                    save();
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to load library.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void save() throws IOException {
        try {
            if (file == null)
                throw new IOException("Attempting to save nonfile library");

            RobustFileWriter out = new RobustFileWriter(file, "UTF-8");
            out.write("<" + getRootTag() + " ");
            out.write(PROCESS_NAME_ATTR + "='" + XMLUtils.escapeAttribute(processName) + "' ");
            out.write(PROCESS_VERSION_ATTR + "='" + XMLUtils.escapeAttribute(processVersion) + "' ");
            out.write(LIBRARY_ID_ATTR + "='" + XMLUtils.escapeAttribute(getLibraryID()) + "' ");
            out.write(EXPORT_TIMESTAMP_ATTR + "='" + XMLUtils.saveDate(new Date()) + "'>\n");
            wbs.getAsXML(out);
            out.write("</" + getRootTag() + ">");
            out.close();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to save library.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public String getFileName() {
        return file == null ? null : file.getName();
    }

    public String getLibraryID() {
        if (!XMLUtils.hasValue(libraryID))
            libraryID = generateLibraryID();
        return libraryID;
    }

    private String generateLibraryID() {
        StringBuilder result = new StringBuilder("lib0");
        Random r = new Random();
        for (int i = 5; i-- > 0;)
            result.append(Integer.toString(r.nextInt(Character.MAX_RADIX),
                Character.MAX_RADIX));
        return result.toString();
    }


    static List<String> getAllDefinitionNames(WBSModel wbs) {
        HashSet<String> names = new LinkedHashSet<String>();
        for (WBSNode node : wbs.getChildren(wbs.getRoot()))
            names.add(node.getName());
        names.remove("");
        return new ArrayList<String>(names);
    }


    public static class Workflows extends WBSLibrary {

        public Workflows(File f) throws IOException {
            super(f);
        }

        public Workflows(File f, TeamProcess process) throws IOException {
            super(f, process);
        }

        Workflows(String[] urls, TeamProcess process) {
            super(urls, process);
        }

        @Override
        protected String getRootTag() {
            return "workflowLibrary";
        }

        @Override
        protected WBSModel createEmptyWbs() {
            return new WorkflowWBSModel("Archived Workflows");
        }

        @Override
        protected WBSModel loadFromXml(Element xml) {
            return new WorkflowWBSModel(xml);
        }

        @Override
        public void setImportSourceIDs() {
            // set the generic import source IDs
            super.setImportSourceIDs();

            // now, record phase mapping attributes for each phase.
            String libraryID = getLibraryID();
            String mappingAttr = null;
            for (WBSNode node : getWbs().getWbsNodes()) {
                if (node.getIndentLevel() == 1) {
                    mappingAttr = WorkflowMappingManager.PHASE_MAPPING_PREFIX
                            + "WF:" + libraryID + ":" + node.getUniqueID();
                } else if (mappingAttr != null && node.getIndentLevel() > 1) {
                    String mappingVal = "WF:" + libraryID + ":"
                            + node.getUniqueID();
                    node.setAttribute(mappingAttr, mappingVal);
                }
            }
        }
    }

    public static class Proxies extends WBSLibrary {

        public Proxies(File f) throws IOException {
            super(f);
        }

        public Proxies(File f, TeamProcess process) throws IOException {
            super(f, process);
        }

        Proxies(String[] urls, TeamProcess process) {
            super(urls, process);
        }

        @Override
        protected String getRootTag() {
            return "proxyLibrary";
        }

        @Override
        protected WBSModel createEmptyWbs() {
            return new ProxyWBSModel("Archived Estimation Tables");
        }

        @Override
        protected WBSModel loadFromXml(Element xml) {
            return new ProxyWBSModel(xml);
        }

    }

}
