// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import pspdash.data.DataRepository;
import pspdash.data.SaveableData;
import pspdash.data.StringData;
import javax.swing.*;
import java.util.*;

public class DefectTypeStandard extends OptionList {

    private String defectTypeName = null;

    private DefectTypeStandard(String s) { super(s); }

    public String getName() { return defectTypeName; }

    private static final String DATA_PREFIX = "/Defect Type Standard/";
    private static final String SETTING_DATA_NAME = "Defect Type Standard";

    /** Get the defect type standard for the named project/task */
    public static DefectTypeStandard get(String path, DataRepository r) {
        data = r;

        // get the defect type standard for this project.
        String defectTypeName = null;
        if (path == null) path = "";

        SaveableData defectSetting = data.getInheritableValue
            (path, SETTING_DATA_NAME);

        if (defectSetting != null)
            defectTypeName = defectSetting.getSimpleValue().format();
        if (defectTypeName == null)
            defectTypeName = Settings.getVal("defectTypeStandard");
        if (defectTypeName == null)
            defectTypeName = "PSP - text";

        return getByName(defectTypeName, r);
    }


    /** Get the named defect type standard. */
    public static DefectTypeStandard getByName(String defectTypeName,
                                               DataRepository r)
    {
        data = r;

        DefectTypeStandard result =
            (DefectTypeStandard) cache.get(defectTypeName);
        if (result == null) {
            String defectTypes = null;
            try {
                defectTypes = data.getSimpleValue
                    (DATA_PREFIX + defectTypeName).format();
            } catch (NullPointerException npe) {
                defectTypes = Settings.getVal("defectType." + defectTypeName);
                if (defectTypes == null) {
                    System.err.println("Could not find a defect type called " +
                                       defectTypeName);
                    defectTypes = DEFAULT_DEFECT_TYPES;
                }
            }

            // create a JComboBox with those defect types.
            result = new DefectTypeStandard(defectTypes);
            result.defectTypeName = defectTypeName;
            cache.put(defectTypeName, result);
        }
        return result;
    }

    public static String[] getDefinedStandards(DataRepository r) {
        LinkedList names = new LinkedList();
        data = r;
        Iterator k = r.getKeys();
        while (k.hasNext()) {
            String name = (String) k.next();
            if (name.startsWith(DATA_PREFIX))
                names.add(name.substring(DATA_PREFIX.length()));
        }
        String[] result = new String[names.size()];
        return (String[]) names.toArray(result);
    }

    public static void save(String defectTypeName,
                            DataRepository r,
                            String[] types,
                            String ignorableType) {

        StringBuffer buf = new StringBuffer();
        if (types != null)
            for (int i = 0;  i < types.length;   i++) {
                if (types[i] == null) continue;
                String t = types[i].trim();
                if (t.length() == 0) continue;
                if (t.equals(ignorableType)) continue;
                buf.append("|").append(t);
            }
        String saveValue = buf.toString();
        data = r;

        if (saveValue.length() == 0)
            data.putValue(DATA_PREFIX + defectTypeName, null);
        else
            data.putValue(DATA_PREFIX + defectTypeName,
                          StringData.create(saveValue.substring(1)));
        cache.remove(defectTypeName);
    }


    public static void saveDefault(DataRepository r, String path,
                                   String defectTypeName) {
        data = r;

        // if no defect type was passed in, do nothing.
        if (defectTypeName == null || defectTypeName.length() == 0)
            return;
        // if the named defect type does not exist, do nothing.
        if (data.getSimpleValue(DATA_PREFIX + defectTypeName) == null &&
            Settings.getVal("defectType." + defectTypeName) == null)
            return;

        if (path == null) path = "";

        String dataName = data.createDataName(path, SETTING_DATA_NAME);
        data.putValue(dataName, StringData.create(defectTypeName));
    }


    private static DataRepository data = null;


    /** Cache of previously created defect types. */
    private static Hashtable cache = new Hashtable();


    /** Defect standard to use if everything else fails */
    private static final String DEFAULT_DEFECT_TYPES =
        "Documentation (comments, messages)|" +
        "Syntax (spelling, punctuation, typos, instruction formats)|" +
        "Build, package (change management, library, version control)|" +
        "Assignment (declaration, duplicate names, scope, limits)|" +
        "Interface (procedure calls and reference, I/O, user formats)|" +
        "Checking (error messages, inadequate checks)|" +
        "Data (structure, content)|" +
        "Function (logic, pointers, loops, recursion, computation, " +
                  "function defects)|" +
        "System (configuration, timing, memory)|" +
        "Environment (design, compile, test, or other support "+
                     "system problems)";
}
