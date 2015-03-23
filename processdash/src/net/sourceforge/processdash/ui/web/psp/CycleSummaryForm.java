// Copyright (C) 2003 Tuma Solutions, LLC
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

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class CycleSummaryForm extends TinyCGIBase {

    public String[] staticPieces;
    public String[] dynamicPieces;
    private boolean needsInit = true;

    private static final String BEGIN_CUT = "<!--beg-->";
    private static final String END_CUT   = "<!--end-->";
    private static final String CYCLE_TAG = "/CYCLE/";
    private static final String ABBR_TAG  = "/ABBR/";
    private static final String PLAN_TAG  = "/PLAN/";
    private static final String EST_TEXT  = "Estimated ";


    private synchronized void init() throws IOException {
        try {
            String uri = (String) env.get("SCRIPT_NAME");
            uri = uri.substring(0, uri.length() - 6) + ".htm";
            String text = getRequestAsString(uri);

            ArrayList staticList  = new ArrayList();
            ArrayList dynamicList = new ArrayList();

            int beg = 0, end;
            while ((end = text.indexOf(BEGIN_CUT, beg)) != -1) {
                staticList.add(text.substring(beg, end));
                beg = end + BEGIN_CUT.length();
                end = text.indexOf(END_CUT, beg);
                dynamicList.add(text.substring(beg, end));
                beg = end + END_CUT.length();
            }
            staticList.add(text.substring(beg));

            String [] type = new String[1];
            staticPieces  = (String []) staticList.toArray(type);
            dynamicPieces = (String []) dynamicList.toArray(type);
            needsInit = false;

        } catch (IndexOutOfBoundsException ioob) {
            needsInit = true;
            throw new IOException();
        }
    }

    /** Get the list of PSP3 cycle names under the current PSP3 task. */
    private String[] getCycleList() {
        String prefix = (String) env.get("PATH_TRANSLATED");
        DashHierarchy props = getPSPProperties();
        PropertyKey self = props.findExistingKey(prefix);
        // WARNING: the "4" on the next line is a magic number which
        // depends on the structure of the PSP3 template.
        int numCycles = props.getNumChildren (self) - 4;
        if (numCycles < 0) numCycles = 0;
        String [] result = new String[numCycles];
        while (numCycles-- > 0)
            // WARNING: the "3" on the next line is a magic number which
            // depends on the structure of the PSP3 template.
            result[numCycles] = props.getChildKey(self, numCycles+3).name();

        return result;
    }

    /** Print a horizontally expanding portion of the form. */
    private void printDynamicPart(String[] cycleList, int partNum,
                                  boolean planning)
        throws IOException
    {
        String text = dynamicPieces[partNum];
        if (planning) {
            text = replace(text, PLAN_TAG, "Plan");
        } else {
            text = replace(text, PLAN_TAG, "Actual");
            text = replace(text, EST_TEXT, "");
        }

        String cycleName, cycleText;
        for (int i=0;  i < cycleList.length;  i++) {
            cycleName = cycleList[i];
            cycleText = replace(text, CYCLE_TAG, cycleName);
            if (cycleText.indexOf(ABBR_TAG) != -1) {
                if (cycleName.length() > 10)
                    cycleName = cycleName.substring(0, 7) + "...";
                cycleText = replace(cycleText, ABBR_TAG, cycleName);
            }
            out.print(cycleText);
        }
    }

    /** Print a static portion of the form. */
    private void printStaticPart(String text, boolean planning)
        throws IOException
    {
        if (planning) {
            text = replace(text, PLAN_TAG, "Plan");
        } else {
            text = replace(text, PLAN_TAG, "Actual");
            text = replace(text, EST_TEXT, "");
        }
        text = replace(text, "PATH_TRANSLATED",
                       (String) env.get("PATH_TRANSLATED"));
        out.print(text);
    }


    /** Generate CGI script output.
     *
     * This method should be overridden by child classes to generate
     * the contents of the script.
     */
    protected void writeContents() throws IOException {
        if (needsInit || parameters.get("init") != null) init();

        String [] cycleList = getCycleList();
        boolean planning = (parameters.get("plan") != null);

        for (int i=0;   i < dynamicPieces.length;  i++) {
            printStaticPart(staticPieces[i], planning);
            printDynamicPart(cycleList, i, planning);
        }
        printStaticPart(staticPieces[staticPieces.length - 1], planning);
    }



    /** find and replace occurrences of a string within buf */
    protected String replace(String template, String text, String replacement)
    {
        if (replacement == null) return template;
        int pos, len = text.length();
        StringBuffer buf = new StringBuffer(template);
        while ((pos = buf.toString().indexOf(text)) != -1)
            buf.replace(pos, pos+len, replacement);
        return buf.toString();
    }
}
