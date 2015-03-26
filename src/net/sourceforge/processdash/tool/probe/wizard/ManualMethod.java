// Copyright (C) 2002-2011 Tuma Solutions, LLC
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

import java.io.PrintWriter;

import net.sourceforge.processdash.util.StringUtils;


public class ManualMethod extends ProbeMethod {

    private int xColumn;

    /**
     * In certain circumstances (e.g., PROBE Method D for Size during the SEI
     * PSP Course), users should not be allowed to edit the value suggested
     * by this method.
     */
    private boolean isReadOnly;


    public ManualMethod(ProbeData data, MethodPurpose purpose, int xColumn,
            boolean readOnly) {
        super(data, "D", purpose);
        this.xColumn = xColumn;
        this.isReadOnly = readOnly;

        this.inputValue = data.getCurrentValue(xColumn);
        if (readOnly) {
            this.outputValue = this.inputValue;
        } else {
            this.outputValue = data.getCurrentValue(purpose.getTargetColumn());
            if (badDouble(this.outputValue))
                this.outputValue = inputValue * purpose.getExpectedBeta1();
        }

        this.beta1 = outputValue / inputValue;
        if (!badDouble(this.beta1)) this.beta0 = 0;
    }


    public void calc() {
        rating = PROBE_METHOD_D;
    }


    public void printOption(PrintWriter out) {
        if (isReadOnly) {
            super.printOption(out);
            return;
        }

        String purpose = methodPurpose.getKey();
        String letter = getMethodLetter();
        String qual = purpose + letter;

        out.print("<input ");
        if (isOnly)
            out.print("type='hidden' ");
        else {
            out.print("type='radio' ");
            if (isSelected) out.print("checked ");
        }
        out.print("name='" + purpose + "' ");
        out.print("id='" + purpose + "MethodD' ");
        out.print("value='" + letter + "'>");
        out.print("<input type='text' name='"+ qual+FLD_ESTIMATE +"' value='");
        if (Double.isNaN(outputValue))
            out.print("?????");
        else
            out.print(formatNumber(outputValue));
        out.print("' size='7'");
        if (!isOnly)
            out.print(" onFocus='select_"+purpose+"D();'");
        out.print("><tt>" + NBSP);
        out.print(methodPurpose.getUnits());
        out.print(NBSP + NBSP + NBSP + "</tt>\n");
        if (!isOnly)
            out.print(StringUtils.findAndReplace(SCRIPT, "PURPOSE", purpose));
        printField(out, FLD_BETA0, qual, Double.NaN);
        printField(out, FLD_BETA1, qual, Double.NaN);
        printField(out, FLD_RANGE, qual, Double.NaN);
        printField(out, FLD_PERCENT, qual, Double.NaN);
        printField(out, FLD_CORRELATION, qual, Double.NaN);
    }
    private static final String SCRIPT =
        "<script>\n"+
        "    function select_PURPOSED() {\n"+
        "      var PURPOSEDoption = document.getElementById(\"PURPOSEMethodD\");\n"+
        "      if (PURPOSEDoption) PURPOSEDoption.checked = true;\n"+
        "    }\n"+
        "</script>\n";


    public void printExplanation(PrintWriter out) {
        // this will print only the introductory statement (e.g. "your next
        // best option could be...")
        super.printExplanation(out);

        String resKey;
        if (isReadOnly)
            resKey = "Wizard.MethodD." + methodPurpose.getKey()
                    + ".Read_Only_Advice_FMT";
        else if (isBest || isOnly)
            resKey = "MethodD.Best_Advice_FMT";
        else
            resKey = "MethodD.Alternate_Advice_FMT";
        int inputColumn = this.methodPurpose.mapInputColumn(xColumn);
        out.print(resources.format(resKey,
            this.methodPurpose.getTargetName(),
            histData.getResultSet().getColName(inputColumn)));
    }


    public void printReportRow(PrintWriter out) {
        if (isSelected || isOnly)
            super.printReportRow(out);
    }


    protected String getTutorialLink() {
        return Tutorial.getManualLink(getPurposeLabel());
    }

}
