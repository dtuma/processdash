// Copyright (C) 2009 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.StringUtils;

public class SizeEstimatingTemplate2 extends SizeEstimatingTemplate {

    private List<Section> sections = null;
    private Map<String, OutputStrategy> strategies;

    public SizeEstimatingTemplate2() {
        strategies = new HashMap<String, OutputStrategy>();
        strategies.put("Static", new StaticOutputStrategy());
        strategies.put("LegacyBase", new LegacyBaseOutputStrategy());
        strategies.put("LegacyBaseRows", new LegacyBaseRowsOutputStrategy());
        strategies.put("BaseParts", new BasePartsOutputStrategy());
        strategies.put("BasePartsRows", new BasePartsRowsOutputStrategy());
        strategies.put("NewPartsRows", new NewPartsRowsOutputStrategy());
        strategies.put("ReusedPartsRows", new ReusedPartsRowsOutputStrategy());
    }


    private void init() throws IOException {
        String srcLoc = (String) parameters.get("templateUri");
        String srcUri = resolveRelativeURI(srcLoc);
        String text = getRequestAsString(srcUri);
        List<Section> sections = new ArrayList<Section>();
        String[] sectionData = text.split("<!--@@");
        for (int i = 1; i < sectionData.length; i++) {
            sections.add(new Section(sectionData[i]));
        }
        this.sections = sections;
    }

    boolean isLegacy;

    boolean freezeActual;

    boolean freezePlan;

    protected void writeContents() throws IOException {
        if (sections == null || parameters.get("init") != null)
            init();
        if (parameters.containsKey("testzero"))
            uniqueNumber = 0;

        this.isLegacy = hasValue(BASE_ADDITIONS_DATANAME);
        this.freezeActual = hasValue("Completed");
        this.freezePlan = freezeActual || hasValue("Planning/Completed");

        for (Section s : sections)
            s.print();

        uniqueNumber++;
    }

    private String markReadOnlyFields(String html) {
        html = StringUtils.findAndReplace(html, PLAN_FLAG,
            freezePlan ? READONLY_FLAG : EDITABLE_FLAG);
        html = StringUtils.findAndReplace(html, ACTUAL_FLAG,
            freezeActual ? READONLY_FLAG : EDITABLE_FLAG);
        return html;
    }

    private static final String PLAN_FLAG = "]p";
    private static final String ACTUAL_FLAG = "]a";
    private static final String READONLY_FLAG = "]r";
    private static final String EDITABLE_FLAG = "]";

    private class Section {
        final String key;

        final String html;

        final OutputStrategy outputStrategy;

        public Section(String src) throws IOException {
            int pos = src.indexOf("-->");
            this.key = src.substring(0, pos).trim();
            this.html = src.substring(pos + 3);
            this.outputStrategy = strategies.get(key);
            if (outputStrategy == null)
                throw new IOException("Unrecognized section '" + key
                        + "' in Size Estimating Template");
        }

        public void print() throws IOException {
            outputStrategy.print(this);
        }
    }

    private abstract class OutputStrategy {
        public abstract void print(Section s) throws IOException;
    }


    private class StaticOutputStrategy extends OutputStrategy {
        @Override
        public void print(Section s) throws IOException {
            String html = s.html;
            html = replaceNum(html, uniqueNumber);
            html = markReadOnlyFields(html);
            out.print(html);
        }
    }

    private class LegacyBaseOutputStrategy extends StaticOutputStrategy {
        @Override
        public void print(Section s) throws IOException {
            if (isLegacy)
                super.print(s);
        }
    }

    private class BasePartsOutputStrategy extends StaticOutputStrategy {
        @Override
        public void print(Section s) throws IOException {
            if (isLegacy == false)
                super.print(s);
        }
    }

    private class RowsOutputStrategy extends OutputStrategy {
        private String[] dataElements;
        private String queryArg;
        private int addRows;

        RowsOutputStrategy(String[] dataElems, String queryArg, int addRows) {
            this.dataElements = dataElems;
            this.queryArg = queryArg;
            this.addRows = addRows;
        }

        @Override
        public void print(Section s) throws IOException {
            String row = markReadOnlyFields(s.html);
            writeTable(row, dataElements, queryArg, 1, 0, addRows);
        }
    }

    private class NewPartsRowsOutputStrategy extends RowsOutputStrategy {
        NewPartsRowsOutputStrategy() {
            super(newData, "moreNew", 5);
        }
    }

    private class ReusedPartsRowsOutputStrategy extends RowsOutputStrategy {
        ReusedPartsRowsOutputStrategy() {
            super(reusedData, "moreReused", 3);
        }
    }

    private class LegacyBaseRowsOutputStrategy extends RowsOutputStrategy {
        LegacyBaseRowsOutputStrategy() {
            super(baseData, "moreBaseAdd", 5);
        }

        @Override
        public void print(Section s) throws IOException {
            if (isLegacy)
                super.print(s);
        }
    }

    private class BasePartsRowsOutputStrategy extends RowsOutputStrategy {
        public BasePartsRowsOutputStrategy() {
            super(basePartsData, "moreBaseParts", 5);
        }

        @Override
        public void print(Section s) throws IOException {
            if (isLegacy == false)
                super.print(s);
        }
    }

    protected static final String [] basePartsData = {
        "Base_Parts_List",
        "Base_Parts/#//#/Description",
        "Base_Parts/#//#/Base",
        "Base_Parts/#//#/Deleted",
        "Base_Parts/#//#/Modified",
        "Base_Parts/#//#/Added",
        "Base_Parts/#//#/Actual Base",
        "Base_Parts/#//#/Actual Deleted",
        "Base_Parts/#//#/Actual Modified",
        "Base_Parts/#//#/Actual Added" };

}
