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

package net.sourceforge.processdash.tool.export.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.util.XMLDepthFirstIterator;
import net.sourceforge.processdash.util.XMLUtils;

public class DefectImporterXMLv1 implements ArchiveMetricsFileImporter.Handler,
        ArchiveMetricsXmlConstants, DefectXmlConstantsv1 {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_DEFECTS.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {
        if (isDefectImportDisabled(caller))
            return;

        InputStreamReader reader = new InputStreamReader(in, ENCODING);
        String prefix = caller.getPrefix();
        importDefectsFromStream(reader, prefix);
    }

    private boolean isDefectImportDisabled(ArchiveMetricsFileImporter caller) {
        Element spec = caller.getImportSpec(FILE_TYPE_DEFECTS);
        return spec != null
                && spec.getElementsByTagName("importDisabled").getLength() > 0;
    }

    public void importDefectsFromStream(Reader reader, String prefix) {
        try {
            Document doc = XMLUtils.parse(reader);
            DefectReader r = new DefectReader();
            r.run(doc.getDocumentElement());

            ImportedDefectManager.importDefects(prefix, r.defects);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class DefectReader extends XMLDepthFirstIterator {

        List defects;

        public DefectReader() {
            this.defects = new LinkedList();
        }

        public void caseElement(Element e, List k) {
            if (DEFECT_TAG.equals(e.getTagName())) {
                String path = e.getAttribute(PATH_ATTR);

                Defect d = new Defect();
                d.date = XMLUtils.getXMLDate(e, DATE_ATTR);
                d.number = ""; // e.getAttribute(NUM_ATTR);
                d.defect_type = e.getAttribute(DEFECT_TYPE_ATTR);
                d.injected = getPhaseAttr(e, INJECTED_ATTR);
                d.phase_injected = d.injected.legacyPhase;
                d.removed = getPhaseAttr(e, REMOVED_ATTR);
                d.phase_removed = d.removed.legacyPhase;
                d.fix_time = e.getAttribute(FIX_TIME_ATTR);
                int fixCount = XMLUtils.getXMLInt(e, FIX_COUNT_ATTR);
                d.fix_count = (fixCount < 0 ? 1 : fixCount);
                d.fix_defect = massageFixDefect(e.getAttribute(FIX_DEFECT_ATTR));
                d.fix_pending = "true".equals(e.getAttribute(FIX_PENDING_ATTR));
                d.description = e.getAttribute(DESCRIPTION_ATTR);

                ImportedDefectManager.ImportedDefect importedDefect =
                    new ImportedDefectManager.ImportedDefect();
                importedDefect.path = path;
                importedDefect.defect = d;
                defects.add(importedDefect);
            }
        }

        private String massageFixDefect(String fd) {
            if (fd != null && fd.trim().length() > 0)
                return "Yes";
            else
                return " ";
        }

        private DefectPhase getPhaseAttr(Element e, String attr) {
            String legacyPhase = e.getAttribute(attr);
            DefectPhase result = new DefectPhase(legacyPhase);

            String id = e.getAttribute(attr + ID_ATTR_SUFFIX);
            if (XMLUtils.hasValue(id)) {
                result.phaseID = id;

                String processName = "";
                String phaseName = e.getAttribute(attr + NAME_ATTR_SUFFIX);
                int slashPos = phaseName.indexOf('/');
                if (slashPos != -1) {
                    processName = phaseName.substring(0, slashPos);
                    phaseName = phaseName.substring(slashPos + 1);
                }
                result.processName = processName;
                result.phaseName = phaseName;
            }

            return result;
        }

    }

}
