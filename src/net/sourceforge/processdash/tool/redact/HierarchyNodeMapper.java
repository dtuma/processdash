// Copyright (C) 2012-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import net.sourceforge.processdash.hier.Filter;

public class HierarchyNodeMapper {

    private Set<String> safeNames;

    private LinkedHashMap<String, String> patternedPaths;

    private HierarchyNameMapper nameMapper;

    private DefectWorkflowPhaseMapper defectWorkflowPhaseMapper;

    private HierarchyPathMapper pathMapper;

    public HierarchyNodeMapper() {
        safeNames = new HashSet<String>(Arrays.asList(DEFAULT_SAFE_NAMES));

        nameMapper = new HierarchyNameMapper() {
            public String getString(String str) {
                return mapName(str);
            }
        };

        defectWorkflowPhaseMapper = new DefectWorkflowPhaseMapper() {
            public String getString(String str) {
                return mapDefectWorkflowPhase(str);
            }
        };

        pathMapper = new HierarchyPathMapper() {
            public String getString(String str) {
                return mapPath(str);
            }
        };

    }

    public void addSafeName(String name) {
        safeNames.add(name);
    }

    public void setPatternedPaths(LinkedHashMap<String, String> patternedPaths) {
        this.patternedPaths = patternedPaths;
    }


    public HierarchyNameMapper getNameMapper() {
        return nameMapper;
    }

    public String mapName(String name) {
        if (safeNames.contains(name) || name.endsWith(" ") || name.startsWith(" "))
            return name;
        else
            return RedactFilterUtils.hash(name);
    }


    public DefectWorkflowPhaseMapper getDefectWorkflowPhaseMapper() {
        return defectWorkflowPhaseMapper;
    }

    public String mapDefectWorkflowPhase(String phase) {
        if (phase.endsWith(" ")) {
            phase = phase.substring(0, phase.length() - 1);
            return mapPath(phase) + " ";
        } else {
            return mapPath(phase);
        }
    }


    public HierarchyPathMapper getPathMapper() {
        return pathMapper;
    }

    public String mapPath(String path) {
        if (path == null || path.length() == 0)
            return path;

        StringBuilder result = new StringBuilder();

        Entry<String, String> patternedPathPrefix = getPatternedPathPrefix(path);
        if (patternedPathPrefix != null) {
            result.append(patternedPathPrefix.getValue());
            path = path.substring(patternedPathPrefix.getKey().length());
        }

        StringTokenizer tok = new StringTokenizer(path, "/", true);
        while (tok.hasMoreTokens()) {
            String oneTok = tok.nextToken();
            if ("/".equals(oneTok))
                result.append("/");
            else
                result.append(mapName(oneTok));
        }

        return result.toString();
    }

    private Map.Entry<String, String> getPatternedPathPrefix(String path) {
        Map.Entry<String, String> result = null;
        for (Entry<String, String> e : patternedPaths.entrySet()) {
            if (Filter.pathMatches(path, e.getKey()))
                result = e;
        }
        return result;
    }


    private static final String[] DEFAULT_SAFE_NAMES = { null, "", "Project",
            "Non Project", "PSP", "PSP0", "PSP0.1", "PSP1", "PSP1.1", "PSP2",
            "PSP2.1", "PSP3", "Planning", "HLD", "HLD Review", "Design",
            "Design Review", "Code", "Code Review", "Compile", "Test",
            "Postmortem", "PM", "Before Development", "After Development",
            "Analyze Rollup Data", "PROBE_Last_Run_Value" };

}
