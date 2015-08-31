// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import net.sourceforge.processdash.util.RobustFileWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import teamdash.XMLUtils;


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
        load();
    }

    public WBSLibrary(File f, TeamProcess process) throws IOException {
        if (f.exists())
            throw new IOException("File already exists.");

        this.file = f;
        this.processName = process.getProcessName();
        this.processVersion = process.getProcessVersion();
        this.wbs = createEmptyWbs();
        // The constructor for WBSModel will create a 'default' WBS which we
        // don't want.  Delete those contents, leaving only the root.
        WBSNode[] children = this.wbs.getDescendants(this.wbs.getRoot());
        this.wbs.deleteNodes(Arrays.asList(children));
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

    public boolean isValid() {
        return (processName != null && processVersion != null && wbs != null);
    }

    public boolean compatible(TeamProcess process) {
        return getProcessName().equals(process.getProcessName()) &&
            getProcessVersion().equals(process.getProcessVersion());
    }


    private void load() throws IOException {
        try {
            Document doc = XMLUtils.parse(new FileInputStream(file));
            Element xml = doc.getDocumentElement();
            if (getRootTag().equals(xml.getTagName())) {
                processName = xml.getAttribute(PROCESS_NAME_ATTR);
                processVersion = xml.getAttribute(PROCESS_VERSION_ATTR);
                libraryID = xml.getAttribute(LIBRARY_ID_ATTR);
                Element wbsElement = (Element) xml.getElementsByTagName
                    (WBSModel.WBS_MODEL_TAG).item(0);
                wbs = loadFromXml(wbsElement);

                if (!XMLUtils.hasValue(libraryID))
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
        return file.getName();
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


    public static class Workflows extends WBSLibrary {

        public Workflows(File f) throws IOException {
            super(f);
        }

        public Workflows(File f, TeamProcess process) throws IOException {
            super(f, process);
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

    }

    public static class Proxies extends WBSLibrary {

        public Proxies(File f) throws IOException {
            super(f);
        }

        public Proxies(File f, TeamProcess process) throws IOException {
            super(f, process);
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
