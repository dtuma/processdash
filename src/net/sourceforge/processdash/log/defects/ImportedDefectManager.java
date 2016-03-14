// Copyright (C) 2004-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.defects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.tool.export.impl.DefectXmlConstantsv1;
import net.sourceforge.processdash.util.HashTree;
import net.sourceforge.processdash.util.XMLUtils;

public class ImportedDefectManager implements DefectXmlConstantsv1 {

    private static final String DEFECT_LIST_SUFFIX = "/defects";
    private static final String DEFECT_LIST_ELEM = DEFECT_LIST_SUFFIX.substring(1);

    static HashTree importedDefects = new HashTree();

    public static class ImportedDefect {
        public String path;

        public Defect defect;
    }

    /** Remove all defects associated with the given prefix from our cache.
     */
    public synchronized static void closeDefects(String prefix) {
        importedDefects.remove(prefix + "/");
    }

    /** Import a list of defects, and associate them with the given prefix.
     * 
     * If other defects are already present in the cache with the same prefix,
     * these defects will be added to that list.  (To replace the defects with
     * a given prefix, call {@link #closeDefects(String)} first.)
     * 
     * @param prefix the prefix where the defects should be mounted
     * @param defects a List of {@link ImportedDefect} objects
     */
    public synchronized static void importDefects(String prefix, List defects) {
        HashTree context = getContext(importedDefects, prefix + "/");
        for (Iterator iter = defects.iterator(); iter.hasNext();) {
            ImportedDefect d = (ImportedDefect) iter.next();
            List l = getList(context, d.path);
            l.add(d.defect);
        }
    }

    private static List getList(HashTree context, String path) {
        String name = path.substring(1) + DEFECT_LIST_SUFFIX;
        List result = (List) context.get(name);
        if (result == null) {
            result = new LinkedList();
            context.put(name, result);
        }
        return result;
    }

    private static HashTree getContext(HashTree base, String path) {
        String fakeItem = path + "foo";
        base.put(fakeItem, "bar");
        HashTree result = (HashTree) base.get(path);
        base.remove(fakeItem);
        return result;
    }



    /**
     * Run a defect analysis against a set of defects that were imported via the
     * {@link #importDefects(String, List)} method of this class.
     */
    public static void run(DashHierarchy props, DataRepository data,
            String[] prefixes, boolean includeChildren, DefectAnalyzer.Task t) {
        Map wbsIdMap = buildWbsIdMap(props, data);

        Set keys = new HashSet();

        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i] + "/";
            HashTree context = (HashTree) importedDefects.get(prefix);
            if (context != null) {
                if (includeChildren) {
                    Iterator j = context.getAllKeys();
                    while (j.hasNext())
                        keys.add(prefix + j.next());
                } else {
                    if (context.get(DEFECT_LIST_ELEM) != null)
                        keys.add(prefix + DEFECT_LIST_ELEM);
                }
            }
        }

        List defects = new LinkedList();
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (key.endsWith(DEFECT_LIST_SUFFIX)) {
                List defectList = (List) importedDefects.get(key);
                String defectPath = key.substring(0, key.length()
                        - DEFECT_LIST_SUFFIX.length());
                defectPath = rerootPath(data, defectPath, wbsIdMap);
                for (Iterator j = defectList.iterator(); j.hasNext();) {
                    defects.add(new DefectToAnalyze(defectPath, (Defect) j
                            .next()));
                }
            }
        }

        Collections.sort(defects);
        for (Iterator i = defects.iterator(); i.hasNext();) {
            DefectToAnalyze defect = (DefectToAnalyze) i.next();
            t.analyze(defect.path, defect.defect);
        }
    }

    private static Map buildWbsIdMap(DashHierarchy props, DataRepository data) {
        Map result = new HashMap();
        buildWbsIdMap(result, props, PropertyKey.ROOT, data);
        return result;
    }

    private static void buildWbsIdMap(Map result, DashHierarchy props,
            PropertyKey pKey, DataRepository data) {
        Prop prop = props.pget(pKey);
        String path = pKey.path();

        // Get the WBS ID of this node in the hierarchy.
        String wbsId = getWbsId(data, path);
        if (!result.containsKey(wbsId))
            result.put(wbsId, path);

        // Check to see if this node supplies subcomponent info
        Map componentInfo = getWbsSubcomponentInfo(data, path, wbsId);
        if (componentInfo != null)
            result.putAll(componentInfo);

        // recursively scan all the children of this node.
        for (int i = 0; i < prop.getNumChildren(); i++)
            buildWbsIdMap(result, props, prop.getChild(i), data);
    }

    private static String getWbsId(DataRepository data, String path) {
        String dataName = DataRepository.createDataName(path, "Project_WBS_ID");
        SimpleData val = data.getSimpleValue(dataName);
        if (val == null)
            return null;
        else
            return val.format();
    }

    private static Map getWbsSubcomponentInfo(DataRepository data, String path,
            String wbsId) {
        String dataName = DataRepository.createDataName(path,
            "Project_Component_Info");
        SimpleData val = data.getSimpleValue(dataName);
        if (val == null)
            return null;

        Element xml;
        try {
            xml = XMLUtils.parse(val.format()).getDocumentElement();
        } catch (Exception e) {
            return null;
        }

        Map result = new HashMap();
        getWbsComponentInfo(result, xml, path, wbsId);
        return result;
    }

    private static void getWbsComponentInfo(Map result, Element parent,
            String pathPrefix, String wbsIdPrefix) {
        for (Element node : XMLUtils.getChildElements(parent)) {
            String name = node.getAttribute("name");
            String nodePath = pathPrefix + "/" + name;
            String nodeWbsId = wbsIdPrefix + "/" + name;
            result.put(nodeWbsId, nodePath);
            getWbsComponentInfo(result, node, nodePath, nodeWbsId);
        }
    }

    private static String rerootPath(DataRepository data, String defectPath,
            Map wbsIdMap) {
        SaveableData wbsIdValue = data.getInheritableValue(defectPath,
                "Project_WBS_ID");
        if (wbsIdValue == null)
            return defectPath;
        SimpleData wbsIdSimpleValue = wbsIdValue.getSimpleValue();
        if (wbsIdSimpleValue == null)
            return defectPath;

        String wbsID = wbsIdSimpleValue.format();
        while (wbsID != null && wbsID.length() > 0) {
            String result = (String) wbsIdMap.get(wbsID);
            if (result != null)
                return result;
            wbsID = DataRepository.chopPath(wbsID);
        }
        return defectPath;
    }



    /**
     * Run a defect analysis against a set of defects that were imported by
     * the database plugin
     * 
     * @param plugin the database plugin
     * @param dbCriteria the criteria to use against the database
     * @param processID the ID of the base process to map phase data to
     * @param t the analysis task to run
     */
    public static void run(DatabasePlugin plugin, List dbCriteria,
            String processID, DefectAnalyzer.Task t) {
        QueryRunner queryRunner = plugin.getObject(QueryRunner.class);
        if (queryRunner == null)
            return;

        StringBuilder query = new StringBuilder(DEFECT_HQL_QUERY);
        List args = new ArrayList();
        args.add(processID);
        args.add(processID);
        QueryUtils.addCriteriaToHql(query, "d", args, dbCriteria);

        List<DefectToAnalyze> defects = new ArrayList();
        List<Object[]> rawData = queryRunner.queryHql(query.toString(),
            args.toArray());
        for (Object[] oneRow : rawData) {
            String path = getDefectPathFromHqlResultRow(oneRow);
            Defect d = getDefectFromHqlResultRow(oneRow);
            defects.add(new DefectToAnalyze(path, d));
        }

        Collections.sort(defects);
        for (DefectToAnalyze defect : defects)
            t.analyze(defect.path, defect.defect);
    }

    private static final String DEFECT_HQL_QUERY = "select " //
            + "d.planItem.project.name, " // 0
            + "d.planItem.wbsElement.name, " // 1
            + "d.foundDate, " // 2
            + "d.defectType.name, " // 3
            + "injPhase.shortName, " // 4
            + "d.injectedPhase.identifier, " // 5
            + "d.injectedPhase.process.name, " // 6
            + "d.injectedPhase.shortName, " // 7
            + "remPhase.shortName, " // 8
            + "d.removedPhase.identifier, " // 9
            + "d.removedPhase.process.name, " // 10
            + "d.removedPhase.shortName, " // 11
            + "d.fixPending, " // 12
            + "d.fixTimeMin, " // 13
            + "d.fixCount, " // 14
            + "d.fixDefect, " // 15
            + "description.text " // 16
            + "from DefectLogFact d " //
            + "left outer join d.description as description " //
            + "join d.injectedPhase.mapsToPhase injPhase " //
            + "join d.removedPhase.mapsToPhase remPhase " //
            + "where d.versionInfo.current = 1 " //
            + "and injPhase.process.identifier in (?, '*Unspecified*', '*INVALID*') " //
            + "and remPhase.process.identifier in (?, '*Unspecified*', '*INVALID*')";

    private static String getDefectPathFromHqlResultRow(Object[] row) {
        String projectName = asString(row[0]);
        if (!projectName.startsWith("/"))
            projectName = "/Project/" + projectName;
        String result = projectName;

        String wbsElementName = asString(row[1]);
        if (!"All WBS Elements".equals(wbsElementName))
            result = projectName + "/" + wbsElementName;

        return result;
    }

    private static Defect getDefectFromHqlResultRow(Object[] row) {
        Defect d = new Defect();
        d.number = "";
        d.date = (Date) row[2];
        d.defect_type = asString(row[3]);
        d.phase_injected = asString(row[4]);
        d.injected = getDefectPhase(row, 4);
        d.phase_removed = asString(row[8]);
        d.removed = getDefectPhase(row, 8);
        d.fix_pending = (row[12] == Boolean.TRUE);
        d.fix_time = asString(row[13]);
        d.fix_count = ((Number) row[14]).intValue();
        d.fix_defect = (row[15] == null ? " " : "Yes");
        d.description = asString(row[16]);
        return d;
    }

    private static DefectPhase getDefectPhase(Object[] row, int pos) {
        DefectPhase result = new DefectPhase(asString(row[pos]));

        String phaseID = asString(row[pos + 1]);
        if (phaseID.startsWith("WF:")) {
            result.processName = asString(row[pos + 2]);
            result.phaseName = asString(row[pos + 3]);
            result.phaseID = unpackWorkflowPhaseIdentifier(phaseID);
        }

        return result;
    }

    private static String unpackWorkflowPhaseIdentifier(String phaseID) {
        int colonPos = phaseID.indexOf(':', 3);
        String projectID = phaseID.substring(3, colonPos);
        String[] nodeIDs = phaseID.substring(colonPos + 1).split("/");
        StringBuilder result = new StringBuilder();
        for (String oneID : nodeIDs) {
            if ("0123456789".indexOf(oneID.charAt(0)) != -1)
                result.append(",").append(projectID).append(":");
            else
                result.append("/");
            result.append(oneID);
        }
        return result.substring(1);
    }

    private static String asString(Object obj) {
        return (obj == null ? "" : obj.toString());
    }



    private static class DefectToAnalyze implements Comparable {
        public String path;

        public Defect defect;

        public DefectToAnalyze(String path, Defect defect) {
            this.path = path;
            this.defect = defect;
        }

        public int compareTo(Object o) {
            DefectToAnalyze that = (DefectToAnalyze) o;
            int result = this.path.compareTo(that.path);
            if (result != 0)
                return result;

            Date thisDate = this.defect.date;
            Date thatDate = that.defect.date;
            if (thisDate != null) {
                if (thatDate != null)
                    result = thisDate.compareTo(thatDate);
                else
                    result = 1;
            } else {
                if (thatDate != null)
                    result = -1;
                else
                    result = 0;
            }

            return result;
        }
    }
}
