// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.PSPProperties;
import pspdash.PropertyKey;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;
import java.io.*;
import java.util.*;

public class summary extends pspdash.TinyCGIBase {

    public String[] staticPieces;
    public String[] dynamicPieces;
    private boolean needsInit = true;

    private static final String BEGIN_CUT = "<!--PhaseStart-->";
    private static final String END_CUT   = "<!--PhaseEnd-->";
    private static final String PHASE_TAG = "/PHASE/";
    private static final String UNIT_TAG  = "/UNIT/";
    private static final String UNITS_TAG = "/UNITS/";

    private static final String SHOW_SIZE_NAME = "Generic - Show Size";
    private static final String SHOW_DEF_NAME  = "Generic - Show Defects";
    private static final String UNITS_NAME     = "Units";

    private boolean showSize = true;
    private boolean showDefects = true;
    private boolean showFriendlyHelp = false;
    private String unit, units;


    private synchronized void init() throws IOException {
        try {
            String uri = "/0" + env.get("SCRIPT_NAME");
            uri = uri.substring(0, uri.length() - 6) + ".htm";
            String text = new String(getRequest(uri, true));

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

    /** Get the list of phases under the current generic project. */
    private String[] getPhaseList() {
        String prefix = (String) env.get("PATH_TRANSLATED");
        PSPProperties props = getPSPProperties();
        PropertyKey self = props.findExistingKey(prefix);
        int numPhases = props.getNumChildren (self);
        showFriendlyHelp = (numPhases == 2);
        String [] result = new String[numPhases];
        while (numPhases-- > 0)
            result[numPhases] = props.getChildKey(self, numPhases).name();

        return result;
    }

    /** Load process customization settings from data for this project. */
    private void loadGenericVariables() {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        SimpleData d;
        d = data.getSimpleValue(prefix + "/" + SHOW_SIZE_NAME);
        showSize = (d != null && d.test());
        d = data.getSimpleValue(prefix + "/" + SHOW_DEF_NAME);
        showDefects = (d != null && d.test());
        d = data.getSimpleValue(prefix + "/" + UNITS_NAME);
        if (d == null) {
            unit = "Unit"; units = "Units";
        } else {
            units = d.format();
            int semicolonPos = units.indexOf(';');
            if (semicolonPos > -1) {
                unit  = units.substring(0, semicolonPos);
                units = units.substring(semicolonPos+1);
            } else if (units.endsWith("s")) {
                unit = units.substring(0, units.length() - 1);
            } else {
                unit = units;
            }
        }
    }

    /** Print an expanding portion of the form. */
    private void printDynamicPart(String[] phaseList, int partNum)
        throws IOException
    {
        String text = dynamicPieces[partNum];

        String phaseName, phaseText;
        for (int i=0;  i < phaseList.length;  i++) {
            phaseName = phaseList[i];
            phaseText = replace(text, PHASE_TAG, phaseName);
            printStaticPart(phaseText);
        }
    }

    /** Print a static portion of the form. */
    private void printStaticPart(String text) throws IOException
    {
        if (showFriendlyHelp) {
            text = replace(text, "<!--friendlyHelpStart", "");
            text = replace(text, "friendlyHelpEnd-->",    "");
        }
        if (!showSize) {
            text = replace(text, "<!--sizeStart-->", "<!--sizeStart");
            text = replace(text, "<!--sizeEnd-->",   "sizeEnd-->");
            text = replace(text, "<!--sizeDefStart-->", "");
            text = replace(text, "<!--sizeDefEnd-->", "");
        } else if (!showDefects) {
            text = replace(text, "<!--sizeDefStart-->", "<!--sizeDefStart");
            text = replace(text, "<!--sizeDefEnd-->", "sizeDefEnd-->");
        }
        if (!showDefects) {
            text = replace(text, "<!--defStart-->", "<!--defStart");
            text = replace(text, "<!--defEnd-->",   "defEnd-->");
            text = replace(text, "<!--defSizeStart-->", "");
            text = replace(text, "<!--defSizeEnd-->", "");
        } else if (!showSize) {
            text = replace(text, "<!--defSizeStart-->", "<!--sizeDefStart");
            text = replace(text, "<!--defSizeEnd-->", "sizeDefEnd-->");
        }
        text = replace(text, UNIT_TAG, unit);
        text = replace(text, UNITS_TAG, units);
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

        String [] phaseList = getPhaseList();
        loadGenericVariables();

        for (int i=0;   i < dynamicPieces.length;  i++) {
            printStaticPart(staticPieces[i]);
            printDynamicPart(phaseList, i);
        }
        printStaticPart(staticPieces[staticPieces.length - 1]);
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
