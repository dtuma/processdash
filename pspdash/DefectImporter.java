// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pspdash.DefectAnalyzer.Task;
import pspdash.data.DataRepository;
import pspdash.data.HashTree;
import pspdash.data.SaveableData;
import pspdash.data.SimpleData;


public class DefectImporter implements DefectXmlConstants {

    private static final String DEFECT_LIST_ELEM = "/defects";

    static HashTree importedDefects = new HashTree();


    public synchronized static void closeDefects(String prefix) {
        importedDefects.remove(prefix+"/");
    }

    public synchronized static void importDefects(BufferedReader in, String prefix) {
        prefix = prefix + "/";
        importedDefects.remove(prefix);

        try {
            Document doc = XMLUtils.parse(in);
            DefectReader r =
                new DefectReader(getContext(importedDefects, prefix));
            r.run(doc.getDocumentElement());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashTree getContext(HashTree base, String path) {
        String fakeItem = path + "foo";
        base.put(fakeItem, "bar");
        HashTree result = (HashTree) base.get(path);
        base.remove(fakeItem);
        return result;
    }

    static class DefectReader extends XMLDepthFirstIterator {
        HashTree context;

        public DefectReader(HashTree context) {
            this.context = context;
        }

        public void caseElement(Element e, List k) {
            if (DEFECT_TAG.equals(e.getTagName())) {
                String path = e.getAttribute(PATH_ATTR);
                List l = getList(path);

                Defect d = new Defect();
                d.date = XMLUtils.getXMLDate(e, DATE_ATTR);
                d.number = e.getAttribute(NUM_ATTR);
                d.defect_type = e.getAttribute(TYPE_ATTR);
                d.phase_injected = e.getAttribute(INJECTED_ATTR);
                d.phase_removed = e.getAttribute(REMOVED_ATTR);
                d.fix_time = e.getAttribute(FIX_TIME_ATTR);
                d.fix_defect = e.getAttribute(FIX_DEFECT_ATTR);
                d.description = e.getAttribute(DESCRIPTION_ATTR);

                l.add(d);
            }
        }

        private List getList(String path) {
            String name = path.substring(1) + DEFECT_LIST_ELEM;
            List result = (List) context.get(name);
            if (result == null) {
                result = new LinkedList();
                context.put(name, result);
            }
            return result;
        }
    }

    public static void run(PSPProperties props, DataRepository data, String[] prefixes, Task t) {
        Map wbsIdMap = buildWbsIdMap(props, data);

        Set keys = new HashSet();

        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i] + "/";
            HashTree context = (HashTree) importedDefects.get(prefix);
            if (context != null) {
                Iterator j = context.getAllKeys();
                while (j.hasNext())
                    keys.add(prefix + j.next());
            }
        }

        Iterator i = keys.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            if (key.endsWith(DEFECT_LIST_ELEM)) {
                List defectList = (List) importedDefects.get(key);
                String defectPath = key.substring
                    (0, key.length() - DEFECT_LIST_ELEM.length());
                defectPath = rerootPath(data, defectPath, wbsIdMap);
                for (Iterator j = defectList.iterator(); j.hasNext();)
                    t.analyze(defectPath, (Defect) j.next());
            }
        }
    }

    private static Map buildWbsIdMap(PSPProperties props, DataRepository data) {
        Map result = new HashMap();
        buildWbsIdMap(result, props, PropertyKey.ROOT, data);
        return result;
    }

    private static void buildWbsIdMap(Map result, PSPProperties props, PropertyKey pKey, DataRepository data) {
        Prop prop = props.pget (pKey);
        String path = pKey.path();

        String wbsId = getWbsId(data, path);
        if (!result.containsKey(wbsId))
            result.put(wbsId, path);

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

    private static String rerootPath(DataRepository data, String defectPath, Map wbsIdMap) {
        SaveableData wbsIdValue = data.getInheritableValue(defectPath, "Project_WBS_ID");
        if (wbsIdValue == null)
            return defectPath;
        SimpleData wbsIdSimpleValue = wbsIdValue.getSimpleValue();
        if (wbsIdSimpleValue == null)
            return defectPath;

        String wbsID = wbsIdSimpleValue.format();
        String result = (String) wbsIdMap.get(wbsID);
        if (result == null)
            return defectPath;
        else
            return result;
    }
}
