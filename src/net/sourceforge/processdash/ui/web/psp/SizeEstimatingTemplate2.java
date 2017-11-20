// Copyright (C) 2009-2011 Tuma Solutions, LLC
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
import java.util.SortedMap;
import java.util.Map.Entry;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.tool.probe.SizePerItemTable;
import net.sourceforge.processdash.tool.probe.SizePerItemTable.RelativeSize;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class SizeEstimatingTemplate2 extends SizeEstimatingTemplate {

    private List<Section> sections = null;
    private Map<String, OutputStrategy> strategies;

    private static final Resources resources = Resources.getDashBundle("PROBE");

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
        text = performTextualReplacements(text);
        List<Section> sections = new ArrayList<Section>();
        String[] sectionData = text.split("<!--@@");
        for (int i = 1; i < sectionData.length; i++) {
            sections.add(new Section(sectionData[i]));
        }
        this.sections = sections;
    }

    private String performTextualReplacements(String text) {
        for (int i = 1;  true;  i++) {
            String instruction = getParameter("replace" + i);
            if (instruction == null)
                break;
            int delimPos = instruction.indexOf("->");
            String find = instruction.substring(0, delimPos);
            String replace = instruction.substring(delimPos+2);
            text = StringUtils.findAndReplace(text, find, replace);
        }
        return text;
    }


    String sizeTypeInit;

    String sizeTypeOptions;

    boolean isLegacy;

    boolean isMismatch;

    boolean freezeActual;

    boolean freezePlan;

    protected void writeContents() throws IOException {
        if (sections == null || parameters.get("init") != null)
            init();
        if (parameters.containsKey("testzero"))
            uniqueNumber = 0;

        if (!StringUtils.hasValue(getPrefix())
                && parameters.containsKey("title")) {
            writeEmptyTitleDocument();
            return;
        }

        this.isLegacy = hasValue(BASE_ADDITIONS_DATANAME);
        this.isMismatch = hasValue(MISMATCH_FLAG_DATANAME);
        boolean disableFreezing = hasValue(DISABLE_FREEZING_DATANAME);
        this.freezeActual = !disableFreezing && hasValue("Completed");
        this.freezePlan = !disableFreezing &&
                (freezeActual || hasValue("Planning/Completed"));
        initSizeTypeData();

        for (Section s : sections)
            s.print();

        uniqueNumber++;
    }

    private void writeEmptyTitleDocument() throws IOException {
        String title = getParameter("title");
        out.print("<html><head><title>");
        out.print(esc(title));
        out.print("</title></head></html>");
    }

    private void initSizeTypeData() {
        StringBuffer options = new StringBuffer("<option>\n");
        StringBuffer sizeData = new StringBuffer(
            "<script>DashSET.itemSizes = { \n");

        String sizeUnits = new ProcessUtil(getDataContext()).getSizeUnits();
        SortedMap<String, SizePerItemTable> tables = SizePerItemTable
                .getDefinedTables(getDataRepository(), sizeUnits);
        for (Entry<String, SizePerItemTable> e : tables.entrySet()) {
            String sizePerItemTableName = e.getKey();
            SizePerItemTable sizePerItemTable = e.getValue();
            String valuePrefix = getCategoryValuePrefix(sizePerItemTableName);

            options.append("<optgroup label=\"")
                .append(esc(sizePerItemTableName)).append("\">\n");

            for (String category : sizePerItemTable.getCategoryNames()) {
                String fullCat = valuePrefix + category;

                options.append("<option value=\"").append(esc(fullCat))
                    .append("\">").append(esc(category)).append("\n");

                for (RelativeSize relSize : RelativeSize.values()) {
                    sizeData.append('"')
                        .append(StringUtils.javaEncode(fullCat))
                        .append("/").append(REL_SIZE_NAMES[relSize.ordinal()])
                        .append("\" : ")
                        .append(sizePerItemTable.getSize(category, relSize))
                        .append(",\n");
                }
            }

            options.append("</optgroup>\n");
        }
        if (tables.isEmpty()) {
            String messageHtml = resources
                    .getHTML("SizePerItem.Errors.No_Types_Message");
            String tooltip = resources.format(
                "SizePerItem.Errors.No_Types_Tooltip_FMT", sizeUnits);
            options.append("<option value=\"-\" title=\"").append(esc(tooltip))
                    .append("\">").append(messageHtml).append("\n");
        }

        sizeData.setLength(sizeData.length()-2);
        sizeData.append(" };\n");
        if (USE_COMMA)
            sizeData.append("DashSET.useCommaForDecimal = true;\n");
        if (freezePlan || Settings.getBool("sizeEst.disableBasePartsGraph", false))
            sizeData.append("DashSET.disableBasePartsGraph = true;\n");
        sizeData.append("</script>\n");

        this.sizeTypeInit = sizeData.toString();
        this.sizeTypeOptions = options.toString();
    }
    private static String getCategoryValuePrefix(String sizePerItemTableName) {
        if (SizePerItemTable.LEGACY_DEFAULT_TYPE_NAME.equals(sizePerItemTableName))
            return "";
        else
            return sizePerItemTableName + "/";
    }
    private static final String esc(String s) {
        return HTMLUtils.escapeEntities(s);
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

    private String insertLiteralProjectPath(String html) {
        html = StringUtils.findAndReplace(html, PROJECT_PATH_TOKEN,
            HTMLUtils.escapeEntities(getPrefix()));
        return html;
    }

    private static final String PROJECT_PATH_TOKEN = "<!-- PROJECT_PATH -->";

    private String insertSizeTypeData(String html) {
        html = StringUtils.findAndReplace(html, SIZE_INIT_TOKEN,
            this.sizeTypeInit);
        html = StringUtils.findAndReplace(html, SIZE_OPTIONS_TOKEN,
            this.sizeTypeOptions);
        return html;
    }

    private static final String SIZE_INIT_TOKEN = "<!-- SIZE_TYPE_INIT -->";
    private static final String SIZE_OPTIONS_TOKEN = "<!-- SIZE_TYPE_OPTIONS -->";

    private String fixWizardLink(String html) {
        html = StringUtils.findAndReplace(html, "DashSET.wizard();", 
            "DashSET.wizard(this.href); return false;");
        return html;
    }

    private String flagInputMismatch(String html) {
        if (isMismatch) {
            html = StringUtils.findAndReplace(html, NO_INPUT_MISMATCH_TOKEN,
                INPUT_MISMATCH_TOKEN);
        }
        return html;
    }

    private static final String MISMATCH_FLAG_DATANAME = "PROBE_Input_Mismatch";
    private static final String NO_INPUT_MISMATCH_TOKEN = "ifInputMismatch";
    private static final String INPUT_MISMATCH_TOKEN = "inputMismatch";


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
            html = insertLiteralProjectPath(html);
            html = insertSizeTypeData(html);
            html = fixWizardLink(html);
            html = flagInputMismatch(html);
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
            row = insertSizeTypeData(row);
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

    private static final String[] REL_SIZE_NAMES = {
        "Very Small", "Small", "Medium", "Large", "Very Large"
    };

    private static boolean USE_COMMA = FormatUtil.formatNumber(1.1)
            .indexOf(',') != -1;
}
