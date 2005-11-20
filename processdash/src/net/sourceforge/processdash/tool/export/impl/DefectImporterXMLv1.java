// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.log.Defect;
import net.sourceforge.processdash.log.ImportedDefectManager;
import net.sourceforge.processdash.util.XMLDepthFirstIterator;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefectImporterXMLv1 implements ArchiveMetricsFileImporter.Handler,
        ArchiveMetricsXmlConstants, DefectXmlConstantsv1 {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_DEFECTS.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {

        InputStreamReader reader = new InputStreamReader(in, ENCODING);
        String prefix = caller.getPrefix();
        importDefectsFromStream(reader, prefix);
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
                d.phase_injected = e.getAttribute(INJECTED_ATTR);
                d.phase_removed = e.getAttribute(REMOVED_ATTR);
                d.fix_time = e.getAttribute(FIX_TIME_ATTR);
                d.fix_defect = massageFixDefect(e.getAttribute(FIX_DEFECT_ATTR));
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

    }

}
