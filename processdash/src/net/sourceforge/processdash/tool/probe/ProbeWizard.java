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

package net.sourceforge.processdash.tool.probe;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

import pspdash.Settings;
import pspdash.data.DataRepository;
import pspdash.data.DoubleData;
import pspdash.data.NumberData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;


public class ProbeWizard extends TinyCGIBase {

    protected void doPost() throws IOException {
        parseFormData();
        savePostedData();

        String nextPage = getParameter(NEXT_PAGE);
        if (nextPage == null || nextPage.length() == 0)
            nextPage = INPUT_PAGE;
        out.print("Location: probe.class?"+PAGE+"=");
        out.print(nextPage);
        out.print("\r\n\r\n");
    }
    private static final String NEXT_PAGE = "nextPage";
    protected void savePostedData() {
        maybeSavePosted("size", ESTM_NC_LOC, 1,
                        "Estimated Min LOC", "Estimated Max LOC");
        maybeSavePosted("time", ESTM_TIME, 60,
                        "Estimated Min Time", "Estimated Max Time");
        HistData.savePostedData(getDataRepository(), getPrefix(), parameters);
    }
    protected void maybeSavePosted(String what, String where, double mult,
                                   String lpiName, String upiName) {
        String method = getParameter(what);
        if (method == null) return;

        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String base = DataRepository.createDataName(prefix, where);
        String qual = what + method;
        String dataName;
        SimpleData e, r, lpi, upi;

        // Save the chosen method
        data.putValue(base+"/Probe Method", StringData.create(method));
        // Save the value
        e = getNum(qual, Method.FLD_ESTIMATE, mult);
        if (e == N_A) {
            parameters.put
                (NEXT_PAGE, "size".equals(what) ? SIZE_PAGE : TIME_PAGE);
            return;
        }
        data.putValue(base, e);
        // Save beta0 and beta1
        data.putValue(base+"/Beta0", getNum(qual, Method.FLD_BETA0, mult));
        data.putValue(base+"/Beta1", getNum(qual, Method.FLD_BETA1, mult));
        // Save the range
        data.putValue(base+"/Range", r = getNum(qual, Method.FLD_RANGE, mult));
        // Save the interval percent
        data.putValue(base+"/Interval Percent",
                      getNum(qual, Method.FLD_PERCENT, 1));
        // Save the correlation
        data.putValue(base+"/R Squared",
                      getNum(qual, Method.FLD_CORRELATION, 1));

        if (r instanceof DoubleData) {
            double est   = ((DoubleData) e).getDouble();
            double range = ((DoubleData) r).getDouble();
            upi = new DoubleData(est + range);
            lpi = new DoubleData(Math.max(0, est - range));
        } else
            upi = lpi = N_A;
        data.putValue(prefix+"/"+lpiName, lpi);
        data.putValue(prefix+"/"+upiName, upi);
    }
    protected SimpleData getNum(String qual, String name, double mult) {
        String inputFieldName = qual + name;
        String inputFieldValue = getParameter(inputFieldName);
        SimpleData result = N_A;
        try {
            double value = Double.parseDouble(inputFieldValue);
            if (value != -1) result = new DoubleData(value * mult);
        } catch (NumberFormatException nfe) { }
        return result;
    }
    protected static final SimpleData N_A = StringData.create("N/A");

    protected static final String PAGE = "page";
    //protected static final String FULL_PAGE = "full";
    protected static final String INPUT_PAGE = "inputs";
    protected static final String HIST_PAGE = "hist";
    protected static final String SIZE_PAGE = "size";
    protected static final String TIME_PAGE = "time";
    protected static final String CHECK_PAGE = "check";
    protected static final String REPORT = "report";


    protected void doGet() throws IOException {
        if (getParameter(PAGE) == null) {
            out.print("Location: intro.shtm\r\n\r\n");
        } else {
            super.doGet();
        }
    }

    protected void writeHeader() {
        out.print("Expires: 0\r\n");
        super.writeHeader();
    }

    protected void writeContents() throws IOException {
        String page = getParameter(PAGE);

        // if report, print report and exit.
        if (REPORT.equals(page)) {
            printReport();
            return;
        }

        printHeader();

        // Get the estimated object loc which the user entered on
        // their Size Estimating Template
        double estObjLOC = getNumber(ESTM_OBJ_LOC);
        double estNCLOC  = getNumber(ESTM_NC_LOC);
        double estTime   = getNumber(ESTM_TIME);

        if (INPUT_PAGE.equals(page))
            printEstObjLOC(getPrefix(), estObjLOC);
        else if (HIST_PAGE.equals(page))
            printHistData(getPrefix(),
                          new HistData(getDataRepository(), getPrefix(),
                                       getParameter(SUBSET_PREFIX_NAME)));
        else {
            HistData data = new HistData(getDataRepository(), getPrefix());
            if (SIZE_PAGE.equals(page))
                printSizeSection(data, estObjLOC, estNCLOC);
            else if (TIME_PAGE.equals(page))
                printTimeSection(data, estObjLOC, estNCLOC, estTime);
            else if (CHECK_PAGE.equals(page))
                printCheckPage(data, estNCLOC, estTime);
        }

        printFooter();
    }
    private static final String ESTM_OBJ_LOC = "Estimated Object LOC";
    private static final String ESTM_NC_LOC  = "Estimated New & Changed LOC";
    private static final String ESTM_TIME    = "Estimated Time";

    protected SimpleData getValue(String dataname) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String dataName = DataRepository.createDataName(prefix, dataname);
        return data.getSimpleValue(dataName);
    }
    protected double getNumber(String dataName) {
        SimpleData d = getValue(dataName);
        if (d instanceof NumberData)
            return ((NumberData) d).getDouble();
        else
            return Double.NaN;
    }

    private static final String WIZARD_HEADER_HTML =
        "<html><head><title>PROBE Wizard</title>\n"+
        "<link rel=stylesheet type='text/css' href='style.css'>\n";
    private static final String POPUP_SCRIPT =
        "<script>function popup() {\n"+
        "   var newWin = window.open('','popup','width=450,height=330,"+
                                              "dependent=1,scrollbars=1');\n"+
        "   newWin.focus();\n"+
        "}</script>\n";
    private static final String REPORT_HEADER_HTML =
        "<html><head><title>PROBE Report</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n"+
        "<style>\n" +
        "    A.plain:link    { color:black; text-decoration:none }\n" +
        "    A.plain:visited { color:black; text-decoration:none }\n" +
        "    A.plain:hover   { color:blue;  text-decoration:underline }\n" +
        "</style>\n";

    protected void printHeader() {
        out.print(WIZARD_HEADER_HTML);
        out.print(POPUP_SCRIPT);
        out.print("</head><body>\n"+
                  "<h1>PROBE - ");
        out.print(HTMLUtils.escapeEntities(getPrefix()));
        out.print("</h1>\n<form action='probe.class' method=post>");
    }

    protected void printEstObjLOC(String prefix, double estObjLOC) {

        boolean planComplete = (getValue("Planning/Completed") != null);
        boolean projectComplete = (getValue("Completed") != null);

        if (planComplete || projectComplete) {
            out.print("<h2>Step 1: Verify PROBE Readiness</h2>\n" +
                      "<p>The PROBE process should be performed as part " +
                      "of the \"Planning\" activities for a project.  You " +
                      "have marked ");
            if (planComplete) out.print("the planning phase ");
            if (planComplete && projectComplete)
                out.print("<b><u>and</u></b> ");
            if (projectComplete) out.print("this project ");
            out.print("complete, which indicates to the dashboard that " +
                      "you should no longer be performing planning " +
                      "activities like PROBE.  If you were to use the "+
                      "PROBE wizard at this point, some of your data "+
                      "calculations would be incorrect.\n" +
                      "<p>Therefore, before you can use PROBE, you must "+
                      "first");
            if (planComplete) {
                out.print(" mark the planning phase incomplete (using the "+
                          "completion checkbox)");
                if (projectComplete)
                    out.print
                        (" <b><u>and</u></b> ensure that this project is "+
                         "not marked as complete on the Project Plan "+
                         "Summary");
            } else
                out.print(" mark the project incomplete (on the Project Plan "+
                          "Summary)");
            out.print(".");
            printContinueButton(null, null);
            return;
        }

        out.print("<h2>Step 1: Verify Estimated Object LOC</h2>\n");

        if (estObjLOC > 0) {
            out.print("<p>From your Size Estimating Template, your Estimated "+
                      "Object LOC is <tt><b>");
            out.print(formatNumber(estObjLOC));
            out.print("</b></tt>.");

            out.print("<p>If you want to alter your Estimated Object LOC, " +
                      "you should return to the Size Estimating Template " +
                      "now and make the necessary changes.\n"+
                      "<p>If you are satisfied with your Estimated Object "+
                      "LOC, press the continue button.\n");
            printContinueButton(null, HIST_PAGE);

        } else {
            out.print("<p>The PROBE process uses Estimated Object LOC as "+
                      "the basis for generating final estimates for size "+
                      "and time.  Before you can proceed, you <b>must</b> "+
                      "estimate the object LOC for this project.\n"+
                      "<p>Press the Finish button to close this window, "+
                      "then use the Size Estimating Template to generate "+
                      "your estimate for object LOC.\n");
            printContinueButton(null, null);
        }
    }

    protected void printHistData(String prefix, HistData data) {
        out.print("<h2>Step 2: Verify Historical Data</h2>\n");

        // precedence: posted item, saved item, default 1, general default
        /*
            String uri = "/0" + env.get("SCRIPT_NAME");
            uri = uri.substring(0, uri.length() - 6) + ".htm";
            String text = new String(getRequest(uri, true));
        */

        data.printDataTable(out, false);

        out.print("<p>If all the information above is correct, press the "+
                  "continue button.\n");

        printContinueButton(INPUT_PAGE, SIZE_PAGE);
    }

    protected void printContinueButton(String prevPage, String nextPage) {
        if (nextPage != null)
            out.print("<input type=hidden name="+NEXT_PAGE+
                      " value="+nextPage+">");

        // align the button(s) to the right.
        out.print("<table width='100%'><tr><td width='100%' align=right>");

        if (prevPage != null)
            // maybe print a "back" button.
            out.print("<input type=button name=back value='Back'"+
                      "       onClick='window.location=\"probe.class?"+
                              PAGE+"="+prevPage+"\";'>&nbsp;&nbsp;");

        if (nextPage != null)
            // print a "continue" button to go to the next page.
            out.print("<input type=submit name=continue value='Continue'>");
        else
            // if there is no next page, print a "finish" button.
            out.print("<input type=button name=finish "+
                      "value='Finish' onClick='window.close()'>");

        out.println("</td></tr></table>");
    }

    protected void printSizeSection(HistData data, double estObjLOC,
                                    double estNCLOC) {

        out.print("<h2>Step 3: Size</h2><b>To create your final size "+
                  "estimate, <font color='#0000ff'>use your engineering "+
                  "judgement</font> to choose from the following PROBE "+
                  "methods:</b><br><br>\n");
        String selectedMethod = getSelectedMethod(ESTM_NC_LOC);

        printMethods(getSizeMethods(data, estObjLOC, estNCLOC, selectedMethod),
                     selectedMethod);

        out.print("<p>Choose your size estimate from the options above, "+
                  "then click the Continue button.");
        printContinueButton(HIST_PAGE, TIME_PAGE);
    }

    /** Calculate data for each of the PROBE methods for size. */
    protected ArrayList getSizeMethods(HistData data, double estObjLOC,
                                       double estNCLOC,
                                       String selectedMethod) {

        ArrayList sizeMethods = new ArrayList();
        sizeMethods.add(new RegressionMethod (data, estObjLOC, EST_OBJ,
                                              ACT_NC, "A", "size"));
        sizeMethods.add(new RegressionMethod (data, estObjLOC, EST_NC,
                                              ACT_NC, "B", "size"));
        sizeMethods.add(new AveragingMethod  (data, estObjLOC, EST_OBJ,
                                              ACT_NC, "C1", "size"));
        sizeMethods.add(new AveragingMethod  (data, estObjLOC, EST_NC,
                                              ACT_NC, "C2", "size"));
        double methodDSize;
        if ("D".equals(selectedMethod) && !Double.isNaN(estNCLOC))
            methodDSize = estNCLOC;
        else
            methodDSize = estObjLOC;
        sizeMethods.add(new MethodD (data, methodDSize, "size"));

        return sizeMethods;
    }

    protected void printTimeSection(HistData data, double estObjLOC,
                                    double estNCLOC, double estTime) {
        boolean onlyMethodD = showOnlyTimeD();

        out.print("<h2>Step 4: Time</h2>");
        if (!onlyMethodD)
            out.print("<b>To create your time estimate,\n" +
                      "<font color='#0000ff'>use your engineering "+
                      "judgement</font> to choose from the following PROBE "+
                      " methods:</b><br><br>\n");
        String selectedMethod = getSelectedMethod(ESTM_TIME);

        printMethods(getTimeMethods(data, estObjLOC, estNCLOC, estTime,
                                    selectedMethod, onlyMethodD),
                     selectedMethod);

        if (onlyMethodD)
            out.print("<p>Enter your time estimate above, ");
        else
            out.print("<p>Choose your time estimate from the options above, ");
        out.print("then click the Continue button.");
        printContinueButton(SIZE_PAGE, CHECK_PAGE);
    }

    /** Calculate data for each of the PROBE methods for time. */
    protected ArrayList getTimeMethods(HistData data, double estObjLOC,
                                       double estNCLOC, double estTime,
                                       String selectedMethod,
                                       boolean onlyMethodD) {

        ArrayList timeMethods = new ArrayList();
        if (!onlyMethodD) {
            boolean strictMethods = true;
            if ("false".equalsIgnoreCase
                (Settings.getVal("probeWizard.strictTimeMethods")))
                strictMethods = false;
            double altInput = strictMethods ? estObjLOC : estNCLOC;
            Method m;

            timeMethods.add(new RegressionMethod     (data, estObjLOC, EST_OBJ,
                                                      ACT_TIME, "A", "time"));
            timeMethods.add(m = new RegressionMethod (data, altInput, EST_NC,
                                                      ACT_TIME, "B", "time"));
            if (strictMethods) m.useAltTutorial();
            timeMethods.add(new AveragingMethod      (data, estObjLOC, EST_OBJ,
                                                      ACT_TIME, "C1", "time"));
            timeMethods.add(m = new AveragingMethod (data, altInput, EST_NC,
                                                     ACT_TIME, "C2", "time"));
            if (strictMethods) m.useAltTutorial();
            timeMethods.add(m = new AveragingMethod  (data, altInput, ACT_NC,
                                                      ACT_TIME, "C3", "time") {
                    public double getRating() {
                        observations.clear();
                        return (rating < 0.0 ? rating
                                : Method.PROBE_METHOD_D + 0.00001); } });
            if (strictMethods) m.useAltTutorial();
        }
        double methodDTime;
        if ("D".equals(selectedMethod))
            methodDTime = estTime / 60;
        else
            methodDTime = Double.NaN;
        MethodD m = new MethodD (data, methodDTime, "time");
        m.setIsOnly(onlyMethodD);
        timeMethods.add(m);

        return timeMethods;
    }

    protected boolean showOnlyTimeD() {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String name = DataRepository.createDataName(prefix, "PROBE_NO_TIME");
        return (data.getSimpleValue(name) != null);
    }

    protected String getSelectedMethod(String what) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String name = DataRepository.createDataName(prefix, what);
        name = DataRepository.createDataName(name, "Probe Method");
        SimpleData d = data.getSimpleValue(name);
        if (d instanceof StringData) {
            String result = d.format();
            if (result.endsWith("  ")) return null;
            return result.trim();
        } else
            return null;
    }

    protected void printMethods(ArrayList methods, String selected) {

        out.print("<table>\n" + DIVIDER);

        Collections.sort(methods);
        Iterator i = methods.iterator();
        boolean isBest = true, isSelected;
        Method m;
        while (i.hasNext()) {
            m = (Method) i.next();
            if (selected == null)
                isSelected = isBest;
            else
                isSelected = selected.equalsIgnoreCase(m.getMethodLetter());
            m.printRow(out, isBest, isSelected);
            out.print(DIVIDER);
            isBest = false;
        }

        out.print("</table>\n\n\n");
    }

    protected void printMethodsTable(ArrayList methods, String selected) {

        out.print("<table border style='margin-left:1cm'><tr>" +
                  "<th ALIGN='RIGHT'>Method</th>" +
                  "<th>Estimate</th>" +
                  "<th>&nbsp;" + RegressionMethod.RSQ + "&nbsp;</th>" +
                  "<th>" + Method.BETA0 + "</th>" +
                  "<th>" + Method.BETA1 + "</th>" +
                  "<th>Range (70%)</th>" +
                  "<th>&nbsp;LPI&nbsp;</th>" +
                  "<th>&nbsp;UPI&nbsp;</th>" +
                  "<th>Variance</th>" +
                  "<th>Std. Dev.</th>" +
                  "<th>Comments</th></tr>\n");

        Collections.sort(methods);
        Iterator i = methods.iterator();
        boolean isSelected;
        Method m;
        while (i.hasNext()) {
            m = (Method) i.next();
            m.printTableRow(out,
                            selected != null &&
                            selected.equalsIgnoreCase(m.getMethodLetter()));
        }

        out.print("</table>\n\n\n");
    }

    protected void printCheckPage(HistData data, double estNCLOC,
                                  double estTime) {
        out.print("<h2>Step 5: Check Estimates</h2>You have estimated "+
                  "that this project will require:<ul>\n");
        out.print("<li>"+formatNumber(estNCLOC)+" New and Changed LOC\n");
        out.print("<li>"+formatNumber(estTime/60)+" Total Hours\n</ul>\n");

        // check to see if their estimates are reasonable.
        double estProductivity = estNCLOC * 60 / estTime;
        double histProductivity = data.getProductivity();
        double histDev = data.getProdStddev();
        // handle the case where they have only one historical data
        // point, by assuming a 30% variation in productivity.
        if (histDev == 0 || badDouble(histDev))
            histDev = histProductivity * 0.30;
        double delta = estProductivity - histProductivity;

        if (badDouble(histProductivity)) {
            out.print("<p>This translates into a planned productivity of " +
                      formatNumber(estProductivity)+" LOC/Hr.");
            printContinueButton(TIME_PAGE, null);

        } else if (Math.abs(delta) > histDev) {
            out.print("<p>This translates into a planned productivity of " +
                      formatNumber(estProductivity)+" LOC/Hr, which is much ");
            out.print(delta > 0 ? "higher" : "lower");
            out.print(" than your 'To Date' productivity of " +
                      formatNumber(histProductivity) + " LOC/Hr.  This " +
                      "is usually a warning flag, indicating that you have " +
                      "probably ");
            if (delta > 0)
                out.println("overestimated size and/or underestimated time.");
            else
                out.println("underestimated size and/or overestimated time.");

            out.print("<p>You should re-evaluate your estimates, and "+
                      "possibly adjust your "+
                      "<a href='probe.class?"+PAGE+"="+SIZE_PAGE+"'>size "+
                      "estimate</a> and/or "+
                      "<a href='probe.class?"+PAGE+"="+TIME_PAGE+"'>time "+
                      "estimate</a>.");
            printContinueButton(TIME_PAGE, SIZE_PAGE);

        } else {
            out.print("<p>This translates into a planned productivity of " +
                      formatNumber(estProductivity)+" LOC/Hr, which is " +
                      "consistent with your 'To Date' productivity of " +
                      formatNumber(histProductivity) + " LOC/Hr (" +
                      PLUS_MINUS + " " + formatNumber(histDev) + ").\n" +
                      "<p><b>Congratulations!</b> You have completed the "+
                      "PROBE process. Press the Finish button to close "+
                      "this window.\n");
            printContinueButton(TIME_PAGE, null);
        }
    }
    private static final String PLUS_MINUS = "&plusmn;";

    protected void printReport() {
        String prefix = getPrefix();
        double estObjLOC = getNumber(ESTM_OBJ_LOC);
        double estNCLOC  = getNumber(ESTM_NC_LOC);
        double estTime   = getNumber(ESTM_TIME);
        HistData data = new HistData(getDataRepository(), getPrefix(),
                                     getParameter(SUBSET_PREFIX_NAME));
        data.calcProductivity();

        // print report-specific HTML header
        out.print(REPORT_HEADER_HTML);
        out.print(POPUP_SCRIPT);
        out.print("</head><body>\n");

        // print report title with the current project path
        out.print("<h1>PROBE Report</h1>\n"+
                  "<h2>" + HTMLUtils.escapeEntities(prefix) + "</h2>\n");

        // print the Estimated Object LOC
        if (estObjLOC > 0) {
            out.print("<h3>Estimated Object LOC</h3>\n" +
                      "<p style='margin-left:1cm'>From your Size Estimating "+
                      "Template, your Estimated Object LOC is <tt><b>");
            out.print(formatNumber(estObjLOC));
            out.print("</b></tt>.");
        } else {
            out.print("<p><font color='red'>The PROBE process uses " +
                      "Estimated Object LOC as the basis for generating " +
                      "final estimates for size and time.  Before you " +
                      "can generate a PROBE Report, you <b>must</b> "+
                      "estimate the object LOC for this project. Please "+
                      "return to your Size Estimating Template.</font></p>\n" +
                      "</body></html>\n");
            return;
        }

        // print table of historical data
        out.print("<h3>Historical Data</h3>\n<form>\n");
        data.printDataTable(out, true);
        out.print("</form>\n");

        // print data on the various methods for size
        String selectedMethod = getSelectedMethod(ESTM_NC_LOC);
        ArrayList methods =
            getSizeMethods(data, estObjLOC, estNCLOC, selectedMethod);
        out.print("<h3>PROBE Methods for Size</h3>\n");
        printMethodsTable(methods, selectedMethod);

        // print data on the various methods for time
        selectedMethod = getSelectedMethod(ESTM_TIME);
        boolean onlyMethodD = showOnlyTimeD();
        methods = getTimeMethods(data, estObjLOC, estNCLOC, estTime,
                                 selectedMethod, onlyMethodD);
        out.print("<h3>PROBE Methods for Time</h3>\n");
        printMethodsTable(methods, selectedMethod);

        // copy logic from printCheckPage
        if (estNCLOC > 0 && estTime > 0) {
            double estProductivity = estNCLOC * 60 / estTime;
            double histProductivity = data.getProductivity();
            double histDev = data.getProdStddev();
            double workDev = (histDev > 0 ? histDev : histProductivity * 0.3);

            out.print("<h3>Estimated Productivity</h3>\n" +
                      "<p style='margin-left:1cm'>Your estimates for size "+
                      "and time translate into a planned productivity of ");
            out.print(formatNumber(estProductivity));
            out.print(" LOC/Hr.  ");
            if (!badDouble(histProductivity)) {
                out.print("This is ");
                if (estProductivity > histProductivity + workDev)
                    out.print("significantly greater than");
                else if (estProductivity < histProductivity - workDev)
                    out.print("significantly less than");
                else
                    out.print("comparable to");
                out.print(" your historical productivity of ");
                out.print(formatNumber(histProductivity));
                out.print(" LOC/Hr");
                if (histDev > 0)
                    out.print(" (" +PLUS_MINUS+" "+formatNumber(histDev)+")");
                out.print(".</p>\n");
            }
        }

        out.print("</body></html>\n");
    }

    protected void printFooter() {
        out.print("</form></body></html>");
    }
    private boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    protected static String formatNumber(double number) {
        return HistData.formatNumber(number);
    }
    private static final String DIVIDER =
        "<tr><td></td><td bgcolor='gray'>" +
        "<img src='line.png' width=1 height=1></td><td></td></tr>\n";

    public static String LINK_ATTRS =
        " target='popup' onClick='popup();' class='plain' ";


    public static final int EST_OBJ  = HistData.EST_OBJ_LOC;
    public static final int EST_NC   = HistData.EST_NC_LOC;
    public static final int ACT_NC   = HistData.ACT_NC_LOC;
    public static final int EST_TIME = HistData.EST_TIME;
    public static final int ACT_TIME = HistData.ACT_TIME;

    public static final String SUBSET_PREFIX_NAME =
        HistData.SUBSET_PREFIX_NAME;

}
