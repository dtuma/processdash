// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.mgr;

import java.util.List;

import org.w3c.dom.Document;

import net.sourceforge.processdash.util.XMLUtils;
import junit.framework.TestCase;

public class ExportMetricsFileInstructionTest extends TestCase {

    private static final String SETTING_XML_HEADER = "<?xml version='1.1' standalone='yes' ?>";

    private static final String OLD_STYLE_PATHS_XML = "<exportMetricsFile file='foobar.pdash' "
            + "paths='&#2;"
            + "/Non Project&#2;"
            + "/Project/PSP0/Postmortem&#2;"
            + "/Project/PSP0/Test&#2;"
            + "/Project/PSP0/Design&#2;'/>";

    private static final String NEW_STYLE_PATHS_XML = "<exportMetricsFile file='foobar.pdash'><paths>"
            + "<path>/Non Project</path>"
            + "<path>/Project/PSP0/Postmortem</path>"
            + "<path>/Project/PSP0/Test</path>"
            + "<path>/Project/PSP0/Design</path>"
            + "</paths></exportMetricsFile>";

    private static final String MERGE_EXTRA_XML = "<exportMetricsFile file='foobarbaz.pdash'><paths>"
            + "<path>/Project/PSP0/Code</path>"
            + "</paths><metricsFilter>"
            + "<include>/Time$</include>"
            + "<include>/Estimated Time$</include>"
            + "<exclude> To Date$</exclude>"
            + "</metricsFilter></exportMetricsFile>";

    private static final String MERGED_XML = "<exportMetricsFile file='foobarbaz.pdash'><paths>"
            + "<path>/Non Project</path>"
            + "<path>/Project/PSP0/Postmortem</path>"
            + "<path>/Project/PSP0/Test</path>"
            + "<path>/Project/PSP0/Design</path>"
            + "<path>/Project/PSP0/Code</path>"
            + "</paths><metricsFilter>"
            + "<include>/Time$</include>"
            + "<include>/Estimated Time$</include>"
            + "<exclude> To Date$</exclude>"
            + "</metricsFilter></exportMetricsFile>";

    public void testTranslateOldPathsToNew() throws Exception {
        Document doc = XMLUtils.parse(SETTING_XML_HEADER + OLD_STYLE_PATHS_XML);
        ExportMetricsFileInstruction instr = new ExportMetricsFileInstruction(
                doc.getDocumentElement());

        StringBuffer asXML = new StringBuffer();
        instr.getAsXML(asXML);
        assertEquals(NEW_STYLE_PATHS_XML, asXML.toString());
    }

    public void testParseNewPaths() throws Exception {
        Document doc = XMLUtils.parse(SETTING_XML_HEADER + NEW_STYLE_PATHS_XML);
        ExportMetricsFileInstruction instr = new ExportMetricsFileInstruction(
                doc.getDocumentElement());

        assertEquals("foobar.pdash", instr.getFile());
        List paths = instr.getPaths();
        assertEquals(4, paths.size());
        assertEquals(paths.get(0), "/Non Project");
        assertEquals(paths.get(1), "/Project/PSP0/Postmortem");
        assertEquals(paths.get(2), "/Project/PSP0/Test");
        assertEquals(paths.get(3), "/Project/PSP0/Design");

        StringBuffer asXML = new StringBuffer();
        instr.getAsXML(asXML);
        assertEquals(NEW_STYLE_PATHS_XML, asXML.toString());
    }

    public void testMergeXML() throws Exception {
        Document doc = XMLUtils.parse(SETTING_XML_HEADER + NEW_STYLE_PATHS_XML);
        ExportMetricsFileInstruction instr = new ExportMetricsFileInstruction(
                doc.getDocumentElement());

        doc = XMLUtils.parse(SETTING_XML_HEADER + MERGE_EXTRA_XML);
        instr.mergeXML(doc.getDocumentElement());
        assertEquals("foobarbaz.pdash", instr.getFile());
        List paths = instr.getPaths();
        assertEquals(5, paths.size());
        assertEquals(paths.get(4), "/Project/PSP0/Code");
        List includes = instr.getMetricsIncludes();
        assertEquals(2, includes.size());
        assertEquals("/Time$", includes.get(0));
        assertEquals("/Estimated Time$", includes.get(1));
        List excludes = instr.getMetricsExcludes();
        assertEquals(1, excludes.size());
        assertEquals(" To Date$", excludes.get(0));

        StringBuffer asXML = new StringBuffer();
        instr.getAsXML(asXML);
        assertEquals(MERGED_XML, asXML.toString());

    }
}
