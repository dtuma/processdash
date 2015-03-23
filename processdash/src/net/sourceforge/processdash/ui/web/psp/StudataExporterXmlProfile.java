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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;

public class StudataExporterXmlProfile {

    XmlSerializer xml;

    DataContext profileData;

    public StudataExporterXmlProfile(XmlSerializer xml, DataContext profileData) {
        this.xml = xml;
        this.profileData = profileData;
    }

    public void writeXmlProfileData() throws IOException {
        xml.startTag(null, "Export_Student");
        for (int i = 0; i < PROFILE_FIELDS.length; i++) {
            DataElementExporter exp = (DataElementExporter) PROFILE_FIELDS[i][0];
            exp.export(profileData, xml, PROFILE_FIELDS[i]);
        }
        xml.endTag(null, "Export_Student");
    }


    private abstract static class DataElementExporter {

        public void export(DataContext src, XmlSerializer dest,
                Object[] instructions) throws IOException {
            String tagName = (String) instructions[1];
            String elemName;
            if (instructions.length > 2)
                elemName = (String) instructions[2];
            else
                elemName = tagName;
            export(src, elemName, dest, tagName);
        }

        protected void export(DataContext src, String elemName,
                XmlSerializer dest, String tagName) throws IOException {
            SimpleData sd = src.getSimpleValue(elemName);
            writeTag(dest, tagName, sd);
        }

        protected void writeTag(XmlSerializer dest, String tagName,
                SimpleData sd) throws IOException {
            dest.startTag(null, tagName);
            writeContent(dest, sd);
            dest.endTag(null, tagName);
        }

        protected abstract void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException;
    }


    private static class LiteralExporter extends DataElementExporter {

        @Override
        public void export(DataContext src, String elemName,
                XmlSerializer dest, String tagName) throws IOException {
            dest.startTag(null, tagName);
            dest.text(elemName);
            dest.endTag(null, tagName);
        }

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {}

    }

    private static final LiteralExporter LITERAL = new LiteralExporter();


    private static class StringExporter extends DataElementExporter {

        int maxLen;

        public StringExporter(int maxLen) {
            this.maxLen = maxLen;
        }

        @Override
        protected void writeTag(XmlSerializer dest, String tagName,
                SimpleData sd) throws IOException {
            if (sd != null && sd.test())
                super.writeTag(dest, tagName, sd);
        }

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {
            String text = sd.format();
            if (text.length() > maxLen)
                text = text.substring(0, maxLen);
            dest.text(text);
        }

    }

    private static final StringExporter STRING_50 = new StringExporter(50);
    private static final StringExporter STRING_100 = new StringExporter(100);


    private static class DoubleExporter extends DataElementExporter {

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {
            double val = 0;
            if (sd instanceof DoubleData)
                val = ((DoubleData) sd).getDouble();
            if (Double.isInfinite(val) || Double.isNaN(val))
                val = 0;
            writeContent(dest, val);
        }

        protected void writeContent(XmlSerializer dest, double val)
                throws IOException {
            dest.text(StudataExporterXml.formatDouble(val));
        }
    }

    private static final DoubleExporter DOUBLE = new DoubleExporter();


    private static class IntegerExporter extends DoubleExporter {

        @Override
        protected void writeContent(XmlSerializer dest, double val)
                throws IOException {
            dest.text(Long.toString(Math.round(val)));
        }

    }

    private static final IntegerExporter INTEGER = new IntegerExporter();


    private static class BooleanExporter extends DataElementExporter {

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {
            boolean booleanValue = (sd != null && sd.test());
            dest.text(booleanValue ? "1" : "0");
        }

    }

    private static final BooleanExporter BOOLEAN = new BooleanExporter();


    private static class DateExporter extends DataElementExporter {

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {
            Date value = null;
            if (sd instanceof DateData)
                value = ((DateData) sd).getValue();
            if (value == null)
                value = new Date();

            dest.text(StudataExporterXml.DATE_FMT.format(value));
        }
    }

    private static final DateExporter DATE = new DateExporter();


    private static class EmpSoftwareIndustryExporter extends DataElementExporter {

        private Map<String, String> textValues;

        public EmpSoftwareIndustryExporter() {
            textValues = new HashMap();
            textValues.put("exec", "1");
            textValues.put("mgmt", "2");
            textValues.put("srtech", "3");
            textValues.put("tech", "4");
            textValues.put("supp", "5");
            textValues.put("other", "6");
        }

        @Override
        protected void writeContent(XmlSerializer dest, SimpleData sd)
                throws IOException {
            String value = "";
            if (sd != null)
                value = sd.format();

            String text = textValues.get(value);
            if (text == null)
                text = "0";

            dest.text(text);
        }

    }
    private static final DataElementExporter EMP_SW_IND = new EmpSoftwareIndustryExporter();


    private static final Object[][] PROFILE_FIELDS = {
            { LITERAL, "PSPStuDataID", "1" }, //
            { LITERAL, "PSPClassID", "1" }, //
            { LITERAL, "Number", "1" }, //
            { STRING_50, "Name", "/Owner" }, //
            { LITERAL, "Registered", "1" }, //
            { LITERAL, "Duplicate", "0" }, //
            { LITERAL, "InterimReport", "0" }, //
            { LITERAL, "FinalReport", "0" }, //
            { DATE, "Date", "Profile_Date" }, //

            { EMP_SW_IND, "EmpSoftwareIndustry" }, //

            { DOUBLE, "ExpPresentOrganization" }, //
            { DOUBLE, "ExpPresentPosition" }, //
            { DOUBLE, "ExpOverall" }, //
            { DOUBLE, "ExpSWRequirements" }, //
            { DOUBLE, "ExpSWDesign" }, //
            { DOUBLE, "ExpSWCodeUT" }, //
            { DOUBLE, "ExpSWIST" }, //
            { DOUBLE, "ExpSQA" }, //
            { DOUBLE, "ExpSCM" }, //
            { DOUBLE, "ExpSPIQM" }, //

            { DOUBLE, "PercentRequirementsYR" }, //
            { DOUBLE, "PercentDesignYR" }, //
            { DOUBLE, "PercentCodeUTYR" }, //
            { DOUBLE, "PercentISTYR" }, //
            { DOUBLE, "PercentSQAYR" }, //
            { DOUBLE, "PercentSCMYR" }, //
            { DOUBLE, "PercentSPIQMYR" }, //
            { DOUBLE, "PercentOther" }, //
            { STRING_100, "PercentOtherDescription" }, //

            { BOOLEAN, "LangExpAssembly" }, //
            { BOOLEAN, "LangExpAda" }, //
            { BOOLEAN, "LangExpAlgol" }, //
            { BOOLEAN, "LangExpBasic" }, //
            { BOOLEAN, "LangExpC" }, //
            { BOOLEAN, "LangExpCPP" }, //
            { BOOLEAN, "LangExpCobol" }, //
            { BOOLEAN, "LangExpFortran" }, //
            { BOOLEAN, "LangExpJava" }, //
            { BOOLEAN, "LangExpJovial" }, //
            { BOOLEAN, "LangExpModula" }, //
            { BOOLEAN, "LangExpObjectPascal" }, //
            { BOOLEAN, "LangExpPascal" }, //
            { BOOLEAN, "LangExpPerl" }, //
            { BOOLEAN, "LangExpSQL" }, //
            { BOOLEAN, "LangExpVB" }, //
            { BOOLEAN, "LangExpVCPP" }, //
            { BOOLEAN, "LangExpVCS" }, //
            { BOOLEAN, "LangExpOther", "LangOther" }, //
            { STRING_50, "LangOther" }, //

            { INTEGER, "HighLevelLangCount" }, //

            // Note that we examine the "Project/Language" field in the parent
            // node directly, instead of using the calculation that appears
            // in the student profile datafile.  This allows the element to be
            // exported even for older versions of the course materials that
            // do not have a "Student Profile" node.
            { STRING_50, "ProjectLang", "../Project/Language" }, //

            { INTEGER, "LOCProjectLangExp" }, //
            { INTEGER, "KLOCTotalExp" }, //
            { INTEGER, "KLOCTotalYRProjectLang" }, //
            { INTEGER, "KLOCTotalYRAllLang" }, //

            { STRING_100, "HighestDegree" }, //
            { STRING_50, "MajorFieldStudy" }, //
            { BOOLEAN, "CourseExpStatistics" }, //
            { BOOLEAN, "CourseExpPhysicalSciences" }, //
            { BOOLEAN, "CourseExpSWPM" }, //
            { BOOLEAN, "CourseExpFormalSWMethods" }, //

    };

}
