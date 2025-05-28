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

package net.sourceforge.processdash.team.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleManifest;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class SyncPastBundleMerges implements TeamDataConstants {

    private BundledWorkingDirectorySync wbsDir;

    private String dataDirectory;

    private DashHierarchy hierarchy;

    private DataRepository data;

    private String projectRoot;

    private String projectID;

    private static final Logger log = Logger
            .getLogger(SyncPastBundleMerges.class.getName());


    public SyncPastBundleMerges(BundledWorkingDirectorySync wbsDir,
            String dataDirectory, DashHierarchy hierarchy, DataRepository data,
            String projectRoot, String projectID) {
        this.wbsDir = wbsDir;
        this.dataDirectory = dataDirectory;
        this.hierarchy = hierarchy;
        this.data = data;
        this.projectRoot = projectRoot;
        this.projectID = projectID;
    }


    public boolean needsUpdate() {
        try {
            return getLineageToUpdate() != null;
        } catch (IOException ioe) {
            // if we can't perform the I/O to check for an update, we won't be
            // successful performing an update either. But return true so a sync
            // will be attempted and we can display an error to the user.
            return true;
        }
    }


    public boolean run() throws IOException {
        List<FileBundleID> lineage = getLineageToUpdate();
        if (lineage == null)
            return false;

        applyChanges(lineage);
        return true;
    }


    private List<FileBundleID> getLineageToUpdate() throws IOException {
        // get our working dir's head ref for the "wbs" bundle
        FileBundleID newBundle = wbsDir.getWorkingHeadRef(WBS_BUNDLE_NAME);
        if (newBundle == null)
            return null;
        StringData newToken = StringData.create(newBundle.getToken());

        // see which head ref we've synced to in the past. If no change, abort
        DataContext data = this.data.getSubcontext(projectRoot);
        SimpleData lastToken = data.getSimpleValue(LAST_SYNC_BUNDLE_ID);
        if (lastToken == null) {
            data.putValue(LAST_SYNC_BUNDLE_ID, newToken);
            return null;
        } else if (newToken.equals(lastToken)) {
            return null;
        }

        // get the lineage from our past bundle to the current one. If there is
        // no lineage between the two, abort
        FileBundleID oldBundle = new FileBundleID(lastToken.format());
        List<FileBundleID> lineage = wbsDir.getForkTracker()
                .getLineage(oldBundle, newBundle);
        if (lineage == null || lineage.size() < 2) {
            data.putValue(LAST_SYNC_BUNDLE_ID, newToken);
            return null;
        } else {
            return lineage;
        }
    }


    private void applyChanges(List<FileBundleID> lineage) throws IOException {
        // examine each past bundle and apply merges that occurred
        for (int i = 1; i < lineage.size(); i++) {
            FileBundleID parent = lineage.get(i - 1);
            FileBundleID child = lineage.get(i);
            applyChanges(parent, child);
            data.putValue(projectRoot + "/" + LAST_SYNC_BUNDLE_ID,
                StringData.create(child.getToken()));
        }
    }


    private void applyChanges(FileBundleID parent, FileBundleID child)
            throws IOException {
        // read the manifest for the child bundle. If it didn't contain any
        // merge metadata, there are no merge changes to apply
        FileBundleManifest manifest = wbsDir.getCachedManifests()
                .getManifest(child);
        if (!manifest.getMetadata().contains(MERGE_METADATA_FILE))
            return;

        // check the parentage to see if this child was the main parent, or the
        // incoming one. If our parent was the main parent, abort.
        if (manifest.getParents().indexOf(parent) < 1)
            return;

        // read the merge metadata file and apply its changes
        try {
            log.info("Applying merge changes for bundle " + child);
            InputStream in = wbsDir.getBundleDirectory().readMetadata(child,
                MERGE_METADATA_FILE);
            Element xml = XMLUtils.parse(in).getDocumentElement();
            applyChanges(xml);
            log.info("Finished applying merge changes for bundle " + child);
        } catch (SAXException se) {
            throw new IOException(se);
        }
    }


    private void applyChanges(Element mergeMetadata) throws IOException {
        PropertyKey projKey = hierarchy.findExistingKey(projectRoot);

        Map<String, String> wbsIDs = getIdChanges(mergeMetadata, WBS_MODEL_TAG);
        replaceDataValues(projKey, wbsIDs, WBS_ID_DATA_NAME);

        Map<String, String> wbsUIDs = makeQualified(wbsIDs);
        replaceDataValues(projKey, wbsUIDs, "EV_Task_IDs");
        replaceDataValues(PropertyKey.ROOT, wbsUIDs, "EV_Task_Dependencies");
        updateEVBaselines(wbsUIDs);
    }

    private Map<String, String> getIdChanges(Element mergeMetadata,
            String modelTag) {
        NodeList nl = mergeMetadata.getElementsByTagName(modelTag);
        if (nl == null || nl.getLength() == 0)
            return Collections.EMPTY_MAP;

        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < nl.getLength(); i++) {
            Element model = (Element) nl.item(i);
            NodeList changes = model.getElementsByTagName(ID_CHANGE_TAG);
            for (int j = 0; j < changes.getLength(); j++) {
                Element change = (Element) changes.item(j);
                String oldID = change.getAttribute(OLD_ID_ATTR);
                String newID = change.getAttribute(NEW_ID_ATTR);
                if (XMLUtils.hasValue(oldID) && XMLUtils.hasValue(newID)) {
                    result.put(oldID, newID);
                }
            }
        }
        if (!result.isEmpty())
            log.info("Applying node ID changes in " + modelTag);
        return result;
    }

    private Map<String, String> makeQualified(Map<String, String> ids) {
        if (ids.isEmpty())
            return Collections.EMPTY_MAP;

        Map<String, String> result = new HashMap<String, String>();
        for (Entry<String, String> e : ids.entrySet()) {
            String oldFullID = projectID + ":" + e.getKey();
            String newFullID = projectID + ":" + e.getValue();
            result.put(oldFullID, newFullID);
        }
        return result;
    }


    /**
     * Recursively scan the project hierarchy starting at a given node, and
     * replace strings within the values of specific named data elements
     */
    private void replaceDataValues(PropertyKey node,
            Map<String, String> replacements, String... dataNames) {
        // abort if no replacements need to be made
        if (replacements.isEmpty())
            return;

        // scan this node for data elements and replace values inside
        DataContext data = this.data.getSubcontext(node.path());
        replaceDataValues(node.path(), data, replacements, dataNames);

        // recurse over hierarchy children
        for (int i = hierarchy.getNumChildren(node); i-- > 0;) {
            PropertyKey child = hierarchy.getChildKey(node, i);
            replaceDataValues(child, replacements, dataNames);
        }
    }


    /**
     * Propagate WBS node ID changes into locally saved baselines
     */
    private void updateEVBaselines(Map<String, String> wbsUIDs)
            throws IOException {
        if (!wbsUIDs.isEmpty()) {
            updateBaselinesInData(wbsUIDs);
            updateBaselinesInExtData(wbsUIDs);
        }
    }

    private void updateBaselinesInData(Map<String, String> wbsUIDs) {
        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        while (i.hasNext()) {
            String dataName = (String) i.next();
            if (SNAPSHOT_DATA_NAME_PAT.matcher(dataName).find()) {
                replaceDataValues("", data, wbsUIDs, dataName);
            }
        }
    }

    private void updateBaselinesInExtData(final Map<String, String> wbsUIDs)
            throws IOException {
        Set<String> files = EVTaskList.getAllExtDataFilenames(data);
        for (String file : files) {
            if (replaceAllInFile(file, wbsUIDs))
                log.info("Updated baselines in " + file);
        }
    }


    /**
     * Scan a file and replace all occurrences of the given strings
     */
    private boolean replaceAllInFile(String filename,
            Map<String, String> replacements) throws IOException {
        // identify the file to modify. If it doesn't exist or is empty, abort
        File dest = new File(dataDirectory, filename);
        if (!dest.isFile() || dest.length() == 0 || replacements.isEmpty())
            return false;

        // open streams for reading and writing
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(dest), "UTF-8"));
        RobustFileWriter out = new RobustFileWriter(dest, "UTF-8");

        // copy the file, applying changes
        boolean madeChange = false;
        String line;
        while ((line = in.readLine()) != null) {
            String newLine = replaceAll(line, replacements);
            if (!newLine.equals(line))
                madeChange = true;
            out.write(newLine);
            out.write(NEWLINE);
        }

        // close and finalize the streams
        in.close();
        if (madeChange)
            out.close();
        else
            out.abort();

        // let our caller know if a change was made
        return madeChange;
    }


    /**
     * Scan a set of named data elements for a specific node of the hierarchy,
     * and replace strings within the values of those elements
     */
    private static void replaceDataValues(String prefix, DataContext data,
            Map<String, String> replacements, String... dataNames) {
        if (!replacements.isEmpty()) {
            for (String oneName : dataNames) {
                SimpleData orig = data.getSimpleValue(oneName);
                if (orig instanceof StringData || orig instanceof ListData) {
                    String origValue = orig.format();
                    String newValue = replaceAll(origValue, replacements);
                    if (!newValue.equals(origValue)) {
                        log.info(
                            "Change data [" + prefix + "/" + oneName + "]: \""
                                    + origValue + "\" -> \"" + newValue + "\"");
                        StringData newVal = StringData.create(newValue);
                        newVal.setEditable(orig.isEditable());
                        data.putValue(oneName, newVal);
                    }
                }
            }
        }
    }


    private static String replaceAll(String text,
            Map<String, String> replacements) {
        if (replacements.isEmpty())
            return text;

        String result = text;
        for (Entry<String, String> e : replacements.entrySet()) {
            result = StringUtils.findAndReplace(result, e.getKey(),
                e.getValue());
        }
        return result;
    }


    private static final String WBS_BUNDLE_NAME = "wbs";

    private static final String MERGE_METADATA_FILE = "mergeMetadata.xml";

    private static final String WBS_MODEL_TAG = "wbs";

    private static final String ID_CHANGE_TAG = "nodeIdChange";

    private static final String OLD_ID_ATTR = "old";

    private static final String NEW_ID_ATTR = "new";

    private static final Pattern SNAPSHOT_DATA_NAME_PAT = Pattern
            .compile("^/Task-Schedule/.*/Snapshot/");

    private static final String NEWLINE = System.getProperty("line.separator");

}
