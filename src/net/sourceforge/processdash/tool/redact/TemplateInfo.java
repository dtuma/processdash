// Copyright (C) 2012-2023 Tuma Solutions, LLC
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.StringUtils;

public class TemplateInfo {

    public static final void addSafeNamesOfProcessPhases(RedactFilterData data,
            HierarchyNodeMapper m) throws IOException {
        addMcfPhasesFromBackup(data, m);
        addMcfPhasesFromDashTemplates(m);
    }

    private static void addMcfPhasesFromBackup(RedactFilterData data,
            HierarchyNodeMapper m) throws IOException {
        for (ZipEntry e : data.getEntries(MCF_ENTRY_PAT)) {
            addMcfPhasesFromMcfSettingsFile(data.getFile(e), m);
        }
    }

    private static void addMcfPhasesFromMcfSettingsFile(BufferedReader file,
            HierarchyNodeMapper m) throws IOException {
        String line;
        while ((line = file.readLine()) != null) {
            if (line.trim().startsWith("<phase "))
                addPhaseNamesFromXmlAttrs(m, line, "name", "longName");
        }
        file.close();
    }

    private static void addPhaseNamesFromXmlAttrs(HierarchyNodeMapper m,
            String xmlTag, String... attrNames) {
        for (String attrName : attrNames) {
            String attrValue = RedactFilterUtils.getXmlAttr(xmlTag, attrName);
            if (StringUtils.hasValue(attrValue))
                m.addSafeName(attrValue);
        }
    }

    private static void addMcfPhasesFromDashTemplates(HierarchyNodeMapper m) {
        if (TEMPLATES == null)
            return;

        for (Iterator i = TEMPLATES.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            PropertyKey key = (PropertyKey) e.getKey();
            Prop prop = (Prop) e.getValue();
            if (PROCESS_PHASE_STATUS.equals(prop.getStatus())) {
                String phaseName = Prop.unqualifiedName(key.name());
                m.addSafeName(phaseName);
            }
        }
    }

    public static void setDashboardContext(DashboardContext ctx) {
        if (ctx instanceof ProcessDashboard) {
            TEMPLATES = ((ProcessDashboard) ctx).getTemplateProperties();
        }
    }

    private static DashHierarchy TEMPLATES = null;

    private static final String PROCESS_PHASE_STATUS = "ME<>";

    private static final String MCF_ENTRY_PAT = "^externalResources/mcf/.*/settings.xml$";

}
