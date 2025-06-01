// Copyright (C) 2025 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleCollection;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class WBSSyncFilter {

    private BundledWorkingDirectorySync workingDir;

    private FileBundleID currentWbsBundle;

    private Map<String, FileBundleID> cachedAncestors;

    private Map<FileBundleID, BundleFilter> cachedFilters;

    private BundleFilter activeFilter;

    private static final Logger logger = Logger
            .getLogger(WBSSyncFilter.class.getName());



    public WBSSyncFilter(BundledWorkingDirectorySync workingDir) {
        this.workingDir = workingDir;
        this.currentWbsBundle = null;
        this.cachedAncestors = new HashMap();
        this.cachedFilters = new HashMap();
        this.activeFilter = NONE;
    }



    public void syncStarting() {
        try {
            // read the current working HEAD ref for the wbs bundle
            FileBundleID newWbsBundle = workingDir.getWorkingHeadRef("wbs");

            // if we didn't previously know the HEAD ref, or if the new ref
            // isn't a direct child of it, discard our cached ancestry data
            if (currentWbsBundle == null || newWbsBundle == null
                    || !isDirectParent(currentWbsBundle, newWbsBundle)) {
                cachedAncestors.clear();
            }

            // save the new HEAD ref for use by other logic
            currentWbsBundle = newWbsBundle;

        } catch (IOException ioe) {
            logger.log(Level.WARNING,
                "Could not read wbs working head ref for " + workingDir, ioe);
            currentWbsBundle = null;
            cachedAncestors.clear();
        }

        activeFilter = NONE;
    }

    private boolean isDirectParent(FileBundleID parent, FileBundleID child)
            throws IOException {
        return parent.equals(getDirectAncestor(parent, child));
    }



    public boolean acceptWbsNodeID(Integer nodeID) {
        // if the filter is disabled, accept any WBS node
        if (activeFilter == NONE || activeFilter == null)
            return true;
        else
            return activeFilter.recognizedWbsNodeIDs.contains(nodeID);
    }

    public boolean acceptSizeMetricsID(String sizeID) {
        // if the filter is disabled, accept any size metric
        if (activeFilter == NONE || activeFilter == null)
            return true;
        else
            return activeFilter.recognizedSizeMetricIDs.contains(sizeID);
    }



    public void setActiveDumpData(Element xml) {
        // if this PDASH file didn't specify the WBS bundle it synced to, or if
        // we don't know the HEAD for the working dir, disable filtering
        String pdashWbsBundleToken = xml.getAttribute("wbsBundleID");
        if (!XMLUtils.hasValue(pdashWbsBundleToken)
                || currentWbsBundle == null) {
            activeFilter = NONE;

        } else {
            // load/compute the filter that should be used for this PDASH file
            try {
                FileBundleID wbsAncestor = getWbsAncestor(pdashWbsBundleToken);
                activeFilter = getBundleFilter(wbsAncestor);
            } catch (IOException ioe) {
                logger.log(Level.WARNING,
                    "Could not read reverse sync node filter for "
                            + pdashWbsBundleToken,
                    ioe);
                activeFilter = NONE;
            }
        }
    }

    private FileBundleID getWbsAncestor(String pdashWbsBundleToken)
            throws IOException {
        // if we have a cached ancestor for this PDASH file, return it
        if (cachedAncestors.containsKey(pdashWbsBundleToken))
            return cachedAncestors.get(pdashWbsBundleToken);

        // find the bundle which is a direct ancestor of the current WBS and
        // the WBS this PDASH synced to. If no ancestor is found, or if the
        // bundles are direct ancestors of each other, no filtering is needed.
        FileBundleID pdashWbsBundle = new FileBundleID(pdashWbsBundleToken);
        FileBundleID wbsAncestor = getDirectAncestor(currentWbsBundle,
            pdashWbsBundle);
        if (wbsAncestor == null //
                || wbsAncestor.equals(currentWbsBundle) //
                || wbsAncestor.equals(pdashWbsBundle)) {
            wbsAncestor = null;
        }

        // cache our result and return it
        cachedAncestors.put(pdashWbsBundleToken, wbsAncestor);
        return wbsAncestor;
    }

    private FileBundleID getDirectAncestor(FileBundleID a, FileBundleID b)
            throws IOException {
        return workingDir.getForkTracker().findSharedAncestor(a, b, -1, true);
    }

    private BundleFilter getBundleFilter(FileBundleID wbsBundle)
            throws IOException {
        // a null bundle means no filter is needed
        if (wbsBundle == null)
            return NONE;

        // return a cached filter if one is found
        BundleFilter filter = cachedFilters.get(wbsBundle);
        if (filter != null)
            return filter;

        // create a filter for the given WBS bundle
        FileBundleCollection bundleFiles = workingDir.getBundleDirectory()
                .getBundleCollection(wbsBundle);
        try {
            filter = new BundleFilter();
            filter.recognizedWbsNodeIDs = loadIDsFromXml(bundleFiles,
                WBSFilenameConstants.WBS_FILENAME, WBSNode.ELEMENT_NAME, "id",
                true);
            filter.recognizedSizeMetricIDs = loadIDsFromXml(bundleFiles,
                WBSFilenameConstants.DATA_DUMP_FILE, "sizeMetric", "metricID",
                false);
        } finally {
            bundleFiles.close();
        }

        // cache the result and return it
        cachedFilters.put(wbsBundle, filter);
        return filter;
    }

    private Set loadIDsFromXml(FileBundleCollection files, String filename,
            String tagName, String attrName, boolean integer)
            throws IOException {

        // open the file for reading; abort if not found
        InputStream in = new BufferedInputStream(
                files.getInputStream(filename));

        // parse the file as XML
        Element xml;
        try {
            xml = XMLUtils.parse(in).getDocumentElement();
        } catch (SAXException sax) {
            throw new IOException(sax);
        } finally {
            FileUtils.safelyClose(in);
        }

        // scan the document for the given tag and extract the given attribute
        Set result = new HashSet();
        NodeList nl = xml.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++) {
            Element tag = (Element) nl.item(i);
            String value = tag.getAttribute(attrName);
            if (XMLUtils.hasValue(value)) {
                if (integer)
                    result.add(Integer.valueOf(value));
                else
                    result.add(value);
            }
        }
        return result;
    }



    private static class BundleFilter {

        Set<Integer> recognizedWbsNodeIDs;

        Set<String> recognizedSizeMetricIDs;

    }

    private static final BundleFilter NONE = new BundleFilter();

}
