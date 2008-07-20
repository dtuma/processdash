// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** The basic autocompleter supplied with scriptaculous assumes (unless
 * additional framework is built on top of it) that an autocompleted
 * value is the only value we need.  In our case, the autocompleted value
 * might be something that makes sense to the user, but not the dashboard.
 * (This is especially true when the language translator is operating.)
 * This class implements an autocompleter that can keep track of the difference
 * between the two.
 */
public class TranslatingAutocompleter {


    public static void writeEditor(Writer out, String fieldName,
            String fieldValue, String fieldAttrs, Collection internalValues,
            String onCompleteFunction) throws IOException {

        Map sorted = new TreeMap();
        for (Iterator i = internalValues.iterator(); i.hasNext();) {
            String s = (String) i.next();
            String t = translateDataName(s);
            sorted.put(t, s);
        }
        ArrayList sortedDisplay = new ArrayList(sorted.keySet());
        ArrayList sortedInternal = new ArrayList(sorted.values());

        writeEditor(out, fieldName, fieldValue, fieldAttrs, sortedDisplay,
                sortedInternal, onCompleteFunction);
    }


    public static String translateDataName(String s) {
        String t = Translator.translate(s);
        return StringUtils.findAndReplace(t, "/", " / ");
    }


    public static void writeEditor(Writer out, String fieldName,
            String fieldValue, String fieldAttrs, List displayValues,
            List internalValues, String onCompleteFunction) throws IOException {

        String displayValue;

        if (fieldValue == null || fieldValue.length() == 0) {
            fieldValue = displayValue = "";
        } else {
            int pos = internalValues.indexOf(fieldValue);
            if (pos == -1)
                displayValue = fieldValue;
            else
                displayValue = (String) displayValues.get(pos);
        }

        // write a field for the user to interact with
        out.write("<input type='text' id='GUI_");
        out.write(esc(fieldName));
        out.write("' value='");
        out.write(esc(displayValue));
        out.write("' ");
        if (fieldAttrs != null)
            out.write(fieldAttrs);
        out.write(" />");

        // write a hidden field to hold the untranslated value
        out.write("<input type='hidden' id='");
        out.write(esc(fieldName));
        out.write("' name='");
        out.write(esc(fieldName));
        out.write("' value='");
        out.write(esc(fieldValue));
        out.write("' />");

        // write the div to hold the autocompletion entries
        out.write("<div class='cmsAutocomplete' id='AC_");
        out.write(esc(fieldName));
        out.write("' style='display:none'>&nbsp;</div>");

        // create JSON objects to represent the values
        JSONArray valuesJson = new JSONArray();
        valuesJson.addAll(displayValues);
        JSONArray internalValuesJson = new JSONArray();
        internalValuesJson.addAll(internalValues);

        // write the script to setup the autocompletion
        out.write("<script type=\"text/javascript\">new Autocompleter.Local('GUI_");
        out.write(esc(fieldName));
        out.write("', 'cmsAutocomplete', ");
        out.write(valuesJson.toString());
        out.write(", { ");
        out.write(" selector:DashCMS.autocompletionSelector.bind(DashCMS),");
        out.write(" afterUpdateElement:DashCMS.autocompletionPost.bind(DashCMS, ");
        if (onCompleteFunction != null) {
            out.write("\"");
            out.write(JSONObject.escape(onCompleteFunction));
            out.write("\"");
        } else
            out.write("null");
        out.write("),");
        out.write(" internalValues:");
        out.write(internalValuesJson.toString());
        out.write(" });</script>");
    }

    private static String esc(String s) {
        return XMLUtils.escapeAttribute(s);
    }

}
