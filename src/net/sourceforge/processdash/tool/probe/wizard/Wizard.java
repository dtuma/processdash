// Copyright (C) 2002-2009 Tuma Solutions, LLC
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

import java.io.IOException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;


public class Wizard extends TinyCGIBase {

    static final Resources resources = Resources.getDashBundle("PROBE.Wizard");


    // HTTP URL parameters used by this control servlet
    public static final String URL = "probe.class";
    public static final String PAGE = "page";
    public static final String NEXT_PAGE = "nextPage";
    public static final String REPORT = "report";


    /** A list of the pages in the wizard */
    private static final Class[] WIZARD_PAGES = {
        WelcomePage.class,
        InputPage.class,
        HistDataPage.class,
        SizePage.class,
        TimePage.class,
        CheckPage.class
    };


    /** Display one of the pages in the wizard */
    protected void doGet() throws IOException {
        writeHeader();
        String pageKey = getParameter(Wizard.PAGE);
        if (REPORT.equals(pageKey))
            writeReport();
        else {
            WizardPage page = getPage(pageKey);
            page.writePage();
        }
    }


    /** Handle data posted from one of the wizard pages */
    protected void doPost() throws IOException {
        parseFormData();

        String pageKey = getParameter(Wizard.PAGE);
        WizardPage page = getPage(pageKey);

        String nextPage = getParameter(NEXT_PAGE);
        if (!page.parseFormData())
            nextPage = pageKey;

        out.print("Location: " + URL + "?" + PAGE + "=");
        out.print(nextPage);
        out.print("&rl=");
        out.print(Integer.toString(REFRESH_TOKEN++));
        out.print("\r\n\r\n");
    }
    private static int REFRESH_TOKEN = 0;


    private void writeReport() throws IOException {
        for (int i = 0; i < WIZARD_PAGES.length; i++) {
            if (!getPage(i).writeReportSection())
                break;
        }
        out.print("</form></body></html>");
    }


    /** Get a page by its string key */
    private WizardPage getPage(String pageKey) throws IOException {
        int pagePos = 0;
        if (pageKey != null)
            for (int i = 0; i < PAGE_KEYS.length; i++)
                if (pageKey.equals(PAGE_KEYS[i]))
                    pagePos = i;

        return getPage(pagePos);
    }


    /** Get a page by its numbered position */
    private WizardPage getPage(int pagePos) throws IOException {
        try {
            WizardPage result =
                (WizardPage) WIZARD_PAGES[pagePos].newInstance();
            result.setOut(out);
            result.setParams(parameters);
            result.setData(getDataRepository());
            result.setPrefix(getPrefix());

            if (pagePos > 0) result.setPrevPage(PAGE_KEYS[pagePos - 1]);
            result.setCurrPage(PAGE_KEYS[pagePos]);
            result.setNextPage(PAGE_KEYS[pagePos + 1]);
            result.setStepNumber(pagePos);
            result.settingDone();

            return result;

        } catch (Throwable t) {
            t.printStackTrace();
            TinyCGIException e = new TinyCGIException(500, "Internal Error");
            e.initCause(t);
            throw e;
        }
    }



    private static final String[] PAGE_KEYS = new String[WIZARD_PAGES.length+1];
    static {
        for (int i = 0; i < WIZARD_PAGES.length; i++)
            PAGE_KEYS[i] = getPageKey(WIZARD_PAGES[i]);
        PAGE_KEYS[PAGE_KEYS.length-1] = null;
    }
    public static String getPageKey(Class pageClass) {
        String name = pageClass.getName();
        int pos = name.lastIndexOf('.');
        return name.substring(pos+1, name.length()-4);
    }
    public static String getPageURL(Class pageClass) {
        return URL + "?" + PAGE + "=" + getPageKey(pageClass);
    }

}
