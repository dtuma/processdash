// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.merge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.SAXException;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyIterator;
import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.StringUtils;

public class StateMergeHandlerTeam implements DashboardFileMergeHandler {

    public static final StateMergeHandlerTeam INSTANCE = new StateMergeHandlerTeam();

    private StateMergeHandlerTeam() {}

    @Override
    public void mergeFile(String filename, ReadableResourceCollection parent,
            ReadableResourceCollection first, ReadableResourceCollection second,
            ResourceCollection dest) throws IOException {

        DashHierarchy parentHier = loadHierarchyFile(filename, parent);
        DashHierarchy firstHier = loadHierarchyFile(filename, first);
        DashHierarchy secondHier = loadHierarchyFile(filename, second);

        DashHierarchy mergedHier;
        if (firstHier == INVALID_XML) {
            mergedHier = secondHier;
        } else if (secondHier == INVALID_XML) {
            mergedHier = firstHier;
        } else {
            mergedHier = mergeHierarchies(parentHier, firstHier, secondHier);
        }

        saveHierarchyFile(filename, dest, mergedHier);
    }

    private DashHierarchy loadHierarchyFile(String filename,
            ReadableResourceCollection src) throws IOException {
        DashHierarchy hier = new DashHierarchy(".");
        DashHierarchy templates = new DashHierarchy(null);

        InputStream in = src.getInputStream(filename);
        try {
            hier.loadXML(in, templates);
            return hier;
        } catch (SAXException se) {
            return INVALID_XML;
        } finally {
            in.close();
        }
    }

    private DashHierarchy mergeHierarchies(DashHierarchy parentHier,
            DashHierarchy firstHier, DashHierarchy secondHier) {
        // use the first hierarchy as our preferred final product
        DashHierarchy merged = new DashHierarchy(".");
        merged.putAll(firstHier);

        // gather the known data files in the parent & first hierarchies
        Set<String> knownFiles = parentHier.getDataAndDefectFilesInUse();
        knownFiles.addAll(firstHier.getDataAndDefectFilesInUse());

        // scan the second hierarchy, looking for added projects
        Iterator i = new PropertyKeyIterator(secondHier, PropertyKey.ROOT);
        while (i.hasNext()) {
            PropertyKey key = (PropertyKey) i.next();
            Prop prop = secondHier.pget(key);
            String df = prop.getDataFile();
            if (StringUtils.hasValue(df) && !knownFiles.contains(df)) {
                // this project in the second hierarchy wasn't in the parent.
                // it must have been added; add it to the merged result too.
                String path = key.path();

                // if the given path already exists in the merged result, add
                // a suffix like "(2)" to make it unique. (We don't want to
                // clobber a project with the exact same name, added in the
                // first hierarchy.)
                PropertyKey existing = merged.findExistingKey(path);
                int numSuffix = 1;
                while (existing != null) {
                    path = key.path() + " (" + (++numSuffix) + ")";
                    existing = merged.findExistingKey(path);
                }

                // add an entry to the merged result with the same prop
                PropertyKey mkey = HierarchyAlterer.doAddNode(merged, path);
                merged.put(mkey, new Prop(prop));
                logger.info(
                    "Merging added project " + mkey.path() + "into hierarchy");
            }
        }

        return merged;
    }

    private void saveHierarchyFile(String filename, ResourceCollection dest,
            DashHierarchy mergedHier) throws IOException {
        OutputStream out = dest.getOutputStream(filename, 0);
        mergedHier.saveXML(out, "hierarchical work breakdown structure");
        out.close();
    }

    private static final DashHierarchy INVALID_XML = new DashHierarchy(null);

}
