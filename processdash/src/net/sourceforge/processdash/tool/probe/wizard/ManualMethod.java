// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.probe.wizard;

import java.io.PrintWriter;

import net.sourceforge.processdash.util.StringUtils;


public class ManualMethod extends ProbeMethod {


    public ManualMethod(ProbeData data, MethodPurpose purpose, int xColumn) {
        super(data, "D", purpose);

        this.inputValue = data.getCurrentValue(xColumn);
        this.outputValue = data.getCurrentValue(purpose.getTargetColumn());
        if (badDouble(this.outputValue))
            this.outputValue = inputValue * purpose.getExpectedBeta1();

        this.beta1 = outputValue / inputValue;
        if (!badDouble(this.beta1)) this.beta0 = 0;
    }


    public void calc() {
        rating = PROBE_METHOD_D;
    }


    public void printOption(PrintWriter out) {
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
        printField(out, FLD_BETA0, qual, -1);
        printField(out, FLD_BETA1, qual, -1);
        printField(out, FLD_RANGE, qual, -1);
        printField(out, FLD_PERCENT, qual, -1);
        printField(out, FLD_CORRELATION, qual, -1);
    }
    private static final String SCRIPT =
        "<script>\n"+
        "    var PURPOSEDoption = document.forms[0].PURPOSE.length - 1;\n"+
        "    if (!(PURPOSEDoption > -1)) PURPOSEDoption = 0;\n"+
        "    function select_PURPOSED() {\n"+
        "      if (document.forms[0].PURPOSE[PURPOSEDoption])\n"+
        "        document.forms[0].PURPOSE[PURPOSEDoption].checked = true;\n"+
        "    }\n"+
        "</script>\n";


    public void printExplanation(PrintWriter out) {
        // this will print only the introductory statement (e.g. "your next
        // best option could be...")
        super.printExplanation(out);

        String resKey;
        if (isBest || isOnly)
            resKey = "MethodD.Best_Advice_FMT";
        else
            resKey = "MethodD.Alternate_Advice_FMT";
        out.print(resources.format
                      (resKey, this.methodPurpose.getTargetName()));
    }


    public void printReportRow(PrintWriter out) {
        if (isSelected || isOnly)
            super.printReportRow(out);
    }


    protected String getTutorialLink() {
        return Tutorial.getManualLink(getPurposeLabel());
    }

}
