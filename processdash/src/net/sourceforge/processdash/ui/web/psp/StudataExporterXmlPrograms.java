// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.util.ResultSet;

public class StudataExporterXmlPrograms {

    XmlSerializer xml;

    List<String> projectPaths;

    ResultSet data;

    public StudataExporterXmlPrograms(XmlSerializer xml,
            List<String> projectPaths, ResultSet data) {
        this.xml = xml;
        this.projectPaths = projectPaths;
        this.data = data;
    }

    public void writeProgramData() throws IOException {
        int pos = 0;
        for (Iterator i = projectPaths.iterator(); i.hasNext();) {
            String path = (String) i.next();
            int row = indexOfPath(data, path);
            if (row != -1)
                writeProgramData(row, ++pos);
        }

        // write empty/zero assignments to bring the count up to 10.  The SEI
        // import is expecting this; otherwise it will not zero out leftover
        // data in the higher numbered assignments.
        while (pos < 10)
            writeProgramData(-1, ++pos);
    }

    private void writeProgramData(int row, int pos) throws IOException {
        xml.startTag(null, "Export_AssgtData");
        for (int i = 0; i < PROGRAM_FIELDS.length; i++) {
            DataExporter exp = (DataExporter) PROGRAM_FIELDS[i][0];
            exp.export(data, xml, row, pos, PROGRAM_FIELDS[i]);
        }
        xml.endTag(null, "Export_AssgtData");
    }

    private static int indexOfPath(ResultSet data, String path) {
        for (int i = data.numRows(); i > 0; i--) {
            if (path.equals(data.getRowName(i)))
                return i;
        }
        return -1;
    }

    private static int indexOfCol(ResultSet data, String columnName) {
        for (int i = data.numCols(); i > 0; i--) {
            if (columnName.equals(data.getColName(i)))
                return i;
        }
        return -1;
    }


    private static class DataExporter {

        public void export(ResultSet data, XmlSerializer xml, int row, int pos,
                Object[] instructions) throws IOException {

            String tagName = (String) instructions[1];

            int col = -1;
            if (instructions.length > 2) {
                String colName = (String) instructions[2];
                col = indexOfCol(data, colName);
            }

            export(data, row, col, pos, xml, tagName);
        }

        protected void export(ResultSet data, int row, int col, int pos,
                XmlSerializer xml, String tagName) throws IOException {
            writeTag(xml, tagName, getValue(data, row, col, pos));
        }

        protected String getValue(ResultSet data, int row, int col, int pos) {
            SimpleData sd = null;
            if (row > 0 && col > 0)
                sd = data.getData(row, col);

            double result;
            if (sd instanceof DoubleData) {
                result = ((DoubleData) sd).getDouble();
            } else {
                result = 0;
            }

            return StudataExporterXml.formatDouble(result);
        }

        protected void writeTag(XmlSerializer xml, String tagName, String text)
                throws IOException {
            xml.startTag(null, tagName);
            xml.text(text);
            xml.endTag(null, tagName);
        }
    }

    private static final DataExporter DOUBLE = new DataExporter();


    private static class LiteralExporter extends DataExporter {

        @Override
        public void export(ResultSet data, XmlSerializer xml, int row, int pos,
                Object[] instructions) throws IOException {

            String tagName = (String) instructions[1];
            String value = (String) instructions[2];
            writeTag(xml, tagName, value);
        }

    }

    private static final LiteralExporter LITERAL = new LiteralExporter();


    private static class AssignmentNumExporter extends DataExporter {

        private String suffix;

        public AssignmentNumExporter(String suffix) {
            this.suffix = suffix;
        }

        @Override
        protected String getValue(ResultSet data, int row, int col, int pos) {
            return pos + suffix;
        }

    }

    private static final DataExporter ASSGN_NUM = new AssignmentNumExporter("");
    private static final DataExporter PROG_NAME = new AssignmentNumExporter("A");


    private static final Object[][] PROGRAM_FIELDS = {

            { ASSGN_NUM, "PSPAssgtDataID" }, //
            { LITERAL, "PSPStuDataID", "1" }, //
            { LITERAL, "AssgtStatusID", "0" }, //
            { ASSGN_NUM, "AssgtSequence" }, //
            { PROG_NAME, "ProgramAssignment" }, //

            { DOUBLE, "EstLOC", "EstLOC" }, //
            { DOUBLE, "ActLOC", "ActLOC" }, //
            { DOUBLE, "EstMin", "EstMin" }, //
            { DOUBLE, "ActMin", "ActMin" }, //
            { DOUBLE, "EstDefRem", "EstDef" }, //
            { DOUBLE, "ActDefRem", "ActDef" }, //

            { DOUBLE, "EstMinPlan", "PlanEstTime" }, //
            { DOUBLE, "ActMinPlan", "PlanActTime" }, //
            { DOUBLE, "EstMinDsgn", "DldEstTime" }, //
            { DOUBLE, "ActMinDsgn", "DldActTime" }, //
            { DOUBLE, "EstMinDLDR", "DldrEstTime" }, //
            { DOUBLE, "ActMinDLDR", "DldrActTime" }, //
            { DOUBLE, "EstMinCode", "CodeEstTime" }, //
            { DOUBLE, "ActMinCode", "CodeActTime" }, //
            { DOUBLE, "EstMinCR", "CrEstTime" }, //
            { DOUBLE, "ActMinCR", "CrActTime" }, //
            { DOUBLE, "EstMinCompile", "CompEstTime" }, //
            { DOUBLE, "ActMinCompile", "CompActTime" }, //
            { DOUBLE, "EstMinTest", "TestEstTime" }, //
            { DOUBLE, "ActMinTest", "TestActTime" }, //
            { DOUBLE, "EstMinPM", "PmEstTime" }, //
            { DOUBLE, "ActMinPM", "PmActTime" }, //

            { DOUBLE, "EstDefInjPlan", "PlanEstInj" }, //
            { DOUBLE, "ActDefInjPlan", "PlanActInj" }, //
            { DOUBLE, "EstDefInjDsgn", "DldEstInj" }, //
            { DOUBLE, "ActDefInjDsgn", "DldActInj" }, //
            { DOUBLE, "EstDefInjDLDR", "DldrEstInj" }, //
            { DOUBLE, "ActDefInjDLDR", "DldrActInj" }, //
            { DOUBLE, "EstDefInjCode", "CodeEstInj" }, //
            { DOUBLE, "ActDefInjCode", "CodeActInj" }, //
            { DOUBLE, "EstDefInjCR", "CrEstInj" }, //
            { DOUBLE, "ActDefInjCR", "CrActInj" }, //
            { DOUBLE, "EstDefInjCompile", "CompEstInj" }, //
            { DOUBLE, "ActDefInjCompile", "CompActInj" }, //
            { DOUBLE, "EstDefInjTest", "TestEstInj" }, //
            { DOUBLE, "ActDefInjTest", "TestActInj" }, //

            { DOUBLE, "EstDefRemPlan", "PlanEstRem" }, //
            { DOUBLE, "ActDefRemPlan", "PlanActRem" }, //
            { DOUBLE, "EstDefRemDsgn", "DldEstRem" }, //
            { DOUBLE, "ActDefRemDsgn", "DldActRem" }, //
            { DOUBLE, "EstDefRemDLDR", "DldrEstRem" }, //
            { DOUBLE, "ActDefRemDLDR", "DldrActRem" }, //
            { DOUBLE, "EstDefRemCode", "CodeEstRem" }, //
            { DOUBLE, "ActDefRemCode", "CodeActRem" }, //
            { DOUBLE, "EstDefRemCR", "CrEstRem" }, //
            { DOUBLE, "ActDefRemCR", "CrActRem" }, //
            { DOUBLE, "EstDefRemCompile", "CompEstRem" }, //
            { DOUBLE, "ActDefRemCompile", "CompActRem" }, //
            { DOUBLE, "EstDefRemTest", "TestEstRem" }, //
            { DOUBLE, "ActDefRemTest", "TestActRem" }, //

    };

}
