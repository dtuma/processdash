// Copyright (C) 2002-2017 Tuma Solutions, LLC
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
import java.util.Map;

import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.HTMLUtils;


public abstract class WizardPage {

    protected static final Resources resources = Wizard.resources;


    public abstract void writeHTMLContents();
    public abstract boolean parseFormData();
    public abstract boolean writeReportSection();


    protected PrintWriter out;
    protected Map params;
    protected DataRepository data;
    protected String prefix;
    protected String prevPage, currPage, nextPage;
    protected int stepNumber;

    public void setOut(PrintWriter out) { this.out = out; }
    public void setParams(Map params) { this.params = params; }
    public void setData(DataRepository data) { this.data = data; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setPrevPage(String page) { this.prevPage = page; }
    public void setCurrPage(String page) { this.currPage = page; }
    public void setNextPage(String page) { this.nextPage = page; }
    public void setStepNumber(int num) { this.stepNumber = num; }
    public void settingDone() {}

    public void writePage() {
        writeHTMLHeader();
        writePageTitle();
        writeHTMLContents();
        writeHTMLFooter();
    }

    protected void writeHTMLHeader() {
        writeHTMLHeader(resources.getHTML("Page_Title"), "style.css", "");
    }
    protected void writeHTMLHeader(String titleHTML, String cssHref,
                                   String extraHeaderTags) {
        out.print("<html><head><title>");
        out.print(titleHTML);
        out.println("</title>");
        out.println("<link rel=stylesheet type='text/css' href='"+cssHref+"'>");
        out.println("<script>");
        out.println("function popup(href) {");
        out.println("   var newWin = window.open(href,'popup'," +
                    "'width=475,height=330,dependent=1,scrollbars=1');");
        out.println("   newWin.focus();   return false;");
        out.println("}");
        out.println("</script>");
        out.print(extraHeaderTags);
        out.println("</head><body><form action='" + Wizard.URL +
                    "' method='post'>");
    }

    protected void writePageTitle() {
        String pageTitle = resources.format("Page_Title_FMT", prefix);
        out.print("<h1>");
        out.print(esc(pageTitle));
        out.println("</h1>");
    }

    protected void writeStepTitle(String stepTitleText) {
        out.print("<h2>");
        out.print(resources.format("Step_Num_FMT", new Integer(stepNumber)));
        out.print(" - ");
        out.print(esc(stepTitleText));
        out.println("</h2>");
    }

    protected void writeSectionTitle(String sectionTitleText) {
        out.print("<h3>");
        out.print(esc(sectionTitleText));
        out.println("</h3>");
    }

    protected void writeHTMLFooter() {
        if (currPage != null)
            out.print("<input type=hidden name="+Wizard.PAGE+
                      " value="+currPage+">");
        if (nextPage != null)
            out.print("<input type=hidden name="+Wizard.NEXT_PAGE+
                      " value="+nextPage+">");

        writeFooterButtons();
    }

    protected void writeFooterButtons() {
        // align the button(s) to the right.
        out.print("<table width='100%'><tr><td width='100%' align=right>");

        if (prevPage != null) {
            // maybe print a "back" button.
            out.print("<input type=button name=back value=\"");
            out.print(resources.getHTML("Back_Button"));
            out.print("\" onClick='window.location=\"" + Wizard.URL + "?"+
                      Wizard.PAGE+"="+prevPage+"\";'>&nbsp;&nbsp;");
        }

        if (nextPage != null) {
            // print a "continue" button to go to the next page.
            out.print("<input type=submit name=continue value=\"");
            out.print(resources.getHTML("Continue_Button"));
            out.print("\">");

        } else {
            // if there is no next page, print a "finish" button.
            out.print("<input type=button name=finish value=\"");
            out.print(resources.getHTML("Finish_Button"));
            out.print("\" onClick='window.close()'>");
        }

        out.println("</td></tr></table></form></body></html>");
    }

    protected void printField(String name, String value) {
        out.print("<input type=\"hidden\" name=\"");
        out.print(name);
        out.print("\" value=\"");
        out.print(esc(value));
        out.println("\">");
    }

    protected SimpleData getValue(String dataname) {
        String dataName = DataRepository.createDataName(prefix, dataname);
        return data.getSimpleValue(dataName);
    }

    protected void putValue(String name, SimpleData value) {
        String dataName = DataRepository.createDataName(prefix, name);
        data.userPutValue(dataName, value);
    }

    protected double getNumber(String dataName) {
        SimpleData d = getValue(dataName);
        if (d instanceof NumberData)
            return ((NumberData) d).getDouble();
        else
            return Double.NaN;
    }

    protected String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

}
