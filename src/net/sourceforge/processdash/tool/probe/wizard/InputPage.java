// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;


import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.FormatUtil;


public class InputPage extends WizardPage {

    private static final String INPUT_VAL = "inputValue";

    public void writeHTMLContents() {
        boolean planningComplete, projectComplete;
        if (testValue("Planning/node")) {
            planningComplete = testValue("Planning/Completed");
            projectComplete = testValue("Completed");
        } else {
            planningComplete = testValue("Completed");
            projectComplete = false;
        }

        if (planningComplete || projectComplete)
            writeError(planningComplete, projectComplete);
        else {
            boolean success = writeVerifyInput(true, false);
            if (success == false) {
                setPrevPage(null);
                setNextPage(null);
            }
        }
    }

    private boolean testValue(String dataname) {
        SimpleData sd = getValue(dataname);
        return sd != null && sd.test();
    }

    public boolean parseFormData() {
        if (params.containsKey(INPUT_VAL))
            try {
                double inputVal = FormatUtil.parseNumber
                    ((String) params.get(INPUT_VAL));

                ProcessUtil processUtil = new ProcessUtil(data, prefix);
                String probeInputElem =
                    processUtil.getProcessString(ProbeData.PROBE_INPUT_METRIC);
                putValue(probeInputElem, new DoubleData(inputVal));

                return (inputVal > 0);
            } catch (Exception e) {
                return false;
            }

        return true;
    }


    public boolean writeReportSection() {
        return writeVerifyInput(false, true);
    }


    private void writeError(boolean planningComplete, boolean projectComplete) {
        writeStepTitle(resources.getString("Input.Planning_Error.Title"));

        String resKey = "";
        if (planningComplete) resKey = "Planning_";
        if (projectComplete) resKey += "Project_";

        out.print("<p>");
        out.println(resources.getString("Input.Planning_Error.1_Header_HTML"));
        out.println(resources.getString
                    ("Input.Planning_Error.2."+resKey+"Complete_HTML"));
        out.println(resources.getString("Input.Planning_Error.3_Middle_HTML"));
        out.println(resources.getString
                    ("Input.Planning_Error.4."+resKey+"Instr_HTML"));
        out.println(resources.getString("Input.Planning_Error.5_Footer_HTML"));
        out.println("</p>");

        setPrevPage(null);
        setNextPage(null);
    }


    private boolean writeVerifyInput(boolean full, boolean checkMismatch) {
        ProcessUtil processUtil = new ProcessUtil(data, prefix);
        String probeInputElem =
            processUtil.getProcessString(ProbeData.PROBE_INPUT_METRIC);
        String probeInputElemDisplay = Translator.translate(probeInputElem);
        String elemNameHTML = esc(probeInputElemDisplay);
        boolean editInputAllowed = full &&
            "".equals(processUtil.getProcessString("PROBE_NO_EDIT_INPUT"));

        if (full)
            writeStepTitle(resources.format
                           ("Input.Verify_Title_FMT", probeInputElemDisplay));
        else
            writeSectionTitle(probeInputElemDisplay);

        double inputVal = getNumber(probeInputElem);
        double lastInputVal = getNumber(ProbeData.PROBE_LAST_RUN_PREFIX
                + probeInputElem);

        if (editInputAllowed) {
            if (Double.isNaN(inputVal)) inputVal = 0;

            // write the prompt
            out.print(resources.format
                      ("Input.Provide_Input_HTML_FMT", elemNameHTML));
            // write a field for entry of the data
            out.print("<p>");
            out.print(elemNameHTML);
            out.print("<input type='text' name='"+INPUT_VAL+"' value='");
            out.print(FormatUtil.formatNumber(inputVal));
            out.print("'>&nbsp;");
            out.print(esc(processUtil.getSizeAbbrLabel()));
            out.print("</p>");

        } else if (checkMismatch == true
                && !Double.isNaN(lastInputVal)
                && (Math.abs(inputVal - lastInputVal) > 0.1)) {

            String inputValHTML =
                "<tt><b>" + FormatUtil.formatNumber(inputVal) + "</b></tt>";
            String lastInputValHTML =
                "<tt><b>" + FormatUtil.formatNumber(lastInputVal) + "</b></tt>";
            out.print("<div class=\"alertError\">");
            out.print(resources.format("Input.Input_Mismatch_Error_HTML_FMT",
                elemNameHTML, lastInputValHTML, inputValHTML));
            out.println("</div>");

        } else if (inputVal > 0) {
            String inputValHTML =
                "<tt><b>" + FormatUtil.formatNumber(inputVal) + "</b></tt>";
            out.print(full ? "<p>" : "<p style='margin-left:1cm'>");
            out.print(resources.format("Input.Verify_Input_HTML_FMT",
                                       elemNameHTML, inputValHTML));
            out.println("</p>");
            if (full) {
                out.print("<p>");
                out.print(resources.format("Input.Verify_Input_Instr_HTML_FMT",
                                           elemNameHTML, inputValHTML));
                out.println("</p>");
            }

        } else {
            out.print("<p>");
            if (!full) out.print("<font color='red'><b>");
            out.print(resources.format("Input.Input_Missing_Error_HTML_FMT",
                                       elemNameHTML));
            if (!full) out.print("</b></font>");
            out.println("</p>");
            if (full) {
                out.print("<p>");
                out.print(resources.format
                          ("Input.Input_Missing_Error_Instr_HTML_FMT",
                           elemNameHTML));
                out.println("</p>");
            }
            return false;
        }

        return true;
    }

}
