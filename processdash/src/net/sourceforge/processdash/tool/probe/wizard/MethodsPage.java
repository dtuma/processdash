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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.util.StringUtils;


public class MethodsPage extends WizardPage {

    private static final String SEL_METHOD = "Probe Method";
    public static final String SHOW_ALL_PARAM = "showAllMethods";
    protected static SimpleData N_A = StringData.create
        (resources.getString("Not_Applicable"));

    protected ProbeData histData;
    protected List probeMethods;
    MethodPurpose purpose;
    String targetDataElement;
    String selectedMethod;

    public MethodsPage() {}

    public void settingDone() {
        this.histData = new ProbeData(data, prefix);
    }

    public void writeHTMLContents() {
        buildMethods();

        String purposeLabel = resources.getString(purpose.getKey());
        writeStepTitle(purposeLabel);
        purposeLabel = purposeLabel.toLowerCase();

        out.print("<p><b>");
        out.print(resources.format("Method.Header_HTML_FMT", purposeLabel));
        out.println("</b><br><br></p>");

        out.println("<table>");
        out.println(DIVIDER);

        for (Iterator i = probeMethods.iterator(); i.hasNext();) {
            printPageRow((ProbeMethod) i.next());
            out.println(DIVIDER);
        }
        out.println("</table>");


        out.print("<p>");
        out.print(resources.format("Method.Footer_HTML_FMT", purposeLabel));
        if (probeMethods.size() == 1)
            writeStrictExplanation();
        out.println("</p>");
    }

    public boolean parseFormData() {
        buildMethods();

        String what = purpose.getKey();
        String method = (String) params.get(what);
        if (method == null) return false;
        String qual = what + method;

        SimpleData estimate, range, lpi, upi;

        // Save the chosen method
        putValue(getDataName(SEL_METHOD), StringData.create(method));

        // Save the estimated value
        estimate = getNum(qual, ProbeMethod.FLD_ESTIMATE);
        if (estimate == N_A) return false;
        putValue(targetDataElement, estimate);

        // Save beta0 and beta1
        putValue(getDataName("Beta0"), getNum(qual, ProbeMethod.FLD_BETA0));
        putValue(getDataName("Beta1"), getNum(qual, ProbeMethod.FLD_BETA1));
        // Save the range
        putValue(getDataName("Range"),
                 range = getNum(qual, ProbeMethod.FLD_RANGE));
        // Save the interval percent
        putValue(getDataName("Interval Percent"),
                             getNum(qual, ProbeMethod.FLD_PERCENT, 1));
        // Save the correlation
        putValue(getDataName("R Squared"),
                      getNum(qual, ProbeMethod.FLD_CORRELATION, 1));

        // Save the LPI and UPI
        if (range instanceof DoubleData) {
            double est   = ((DoubleData) estimate).getDouble();
            double rng = ((DoubleData) range).getDouble();
            upi = new DoubleData(est + rng);
            lpi = new DoubleData(Math.max(0, est - rng));
        } else
            upi = lpi = N_A;
        putValue(getDataName(purpose.getTargetDataElementMin()), lpi);
        putValue(getDataName(purpose.getTargetDataElementMax()), upi);

        return true;
    }

    protected SimpleData getNum(String qual, String name) {
        return getNum(qual, name, purpose.getMult());
    }

    protected SimpleData getNum(String qual, String name, double mult) {
        String inputFieldName = qual + name;
        String inputFieldValue = (String) params.get(inputFieldName);
        SimpleData result = N_A;
        try {
            double value = Double.parseDouble(inputFieldValue);
            if (value != -1) result = new DoubleData(value * mult);
        } catch (NumberFormatException nfe) { }
        return result;
    }


    public boolean writeReportSection() {
        buildMethods();

        String purposeLabel = resources.getString(purpose.getKey());
        writeSectionTitle(resources.format("Method.Report_Title_FMT", purposeLabel));

        out.println("<table border style='margin-left:1cm'>");
        ProbeMethod.writeReportTableHeader(out);
        for (Iterator i = probeMethods.iterator(); i.hasNext();)
            ((ProbeMethod) i.next()).printReportRow(out);
        out.println("</table>");

        return true;
    }

    protected String getRes(String key) {
        return resources.getString(purpose.getKey() + "." + key);
    }

    private void buildMethods() {
        targetDataElement = purpose.getTargetDataElement();
        selectedMethod = getSelectedMethod();
        probeMethods = new LinkedList();
        if (!onlyShowMethodD()) {
            addMethod(new RegressionMethod(histData, "A", purpose,
                                           ProbeData.EST_OBJ_LOC));
            addMethod(new RegressionMethod(histData, "B", purpose,
                                           ProbeData.EST_NC_LOC));
            addMethod(new AveragingMethod(histData, "C1", purpose,
                                          ProbeData.EST_OBJ_LOC));
            addMethod(new AveragingMethod(histData, "C2", purpose,
                                          ProbeData.EST_NC_LOC));
            buildExtraMethods(histData);
        }
        addMethod(new ManualMethod(histData, purpose, ProbeData.EST_OBJ_LOC));

        if (probeMethods.size() == 1) {
            ((ProbeMethod) probeMethods.get(0)).setOnly(true);
        } else {
            Collections.sort(probeMethods);
            ((ProbeMethod) probeMethods.get(0)).setBest(true);
        }
    }

    private String getDataName(String elemName) {
        return targetDataElement + "/" + elemName;
    }

    private String getSelectedMethod() {
        SimpleData val = getValue(getDataName(SEL_METHOD));
        if (val == null) return null;
        String result = val.format();
        if (result.endsWith("  ")) return null;
        return result.trim();
    }


    protected void buildExtraMethods(ProbeData histData) {}

    protected void addMethod(ProbeMethod m) {
        m.calc();
        if (m.getMethodLetter().equalsIgnoreCase(selectedMethod))
            m.setSelected(true);
        probeMethods.add(m);
    }

    private boolean onlyShowMethodD() {
        String dataElem = "PROBE_NO_" + purpose.getKey().toUpperCase();

        if (params.containsKey(SHOW_ALL_PARAM)) {
            putValue(dataElem, null);
            return false;
        }

        return (getValue(dataElem) != null);
    }

    /** If only one method was displayed, write a statement explaining why,
     * and allowing the user to see all the other methods.
     */
    private void writeStrictExplanation() {
        out.print("<i>");
        String purposeLabel = resources.getString(purpose.getKey());
        String html = resources.format
            ("MethodD.Strict_Explanation_HTML_FMT", purposeLabel);
        String url = Wizard.getPageURL(getClass()) + "&" + SHOW_ALL_PARAM;
        String link = "<a href=\"" + url + "\">";
        html = StringUtils.findAndReplace(html, "<a>", link);
        out.print(html);
        out.print("</i>");
    }

    protected void printPageRow(ProbeMethod method) {
        out.println("<tr>");
        out.print("<td valign=middle nowrap>");
        method.printOption(out);
        out.println("</td>");
        out.print("<td valign=middle>&nbsp;<br><i>");
        method.printExplanation(out);
        out.println("</i><br>&nbsp;</td>");
        out.print("<td valign=middle>");
        method.printChart(out);
        out.println("</td>");
        out.println("</tr>");
    }

    private static final String DIVIDER =
        "<tr><td></td><td bgcolor='gray'>" +
        "<img src='line.png' width=1 height=1></td><td></td></tr>";

}
