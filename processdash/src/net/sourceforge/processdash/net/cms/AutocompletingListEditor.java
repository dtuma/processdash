// Copyright (C) 2006-2009 Tuma Solutions, LLC
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
import java.util.Arrays;
import java.util.List;

import net.sourceforge.processdash.net.http.WebServer;

/** This class is used when a snippet editor needs to allow the user to
 * select a list of items, with autocompletion support.
 * 
 * @author Tuma
 *
 */
public class AutocompletingListEditor extends AbstractListEditor {

    @Override
    protected String getExtraNewItemParams() {
        String internalValParam =  getOptionalParam("valParam", "DataName");
        String displayValParam = getParameter("displayParam");

        return "&" + internalValParam + "=_VALUE_"
             + "&" + displayValParam + "=_DISPLAY_";
    }

    private List readValues(String valuesUri) throws IOException {
        if (!valuesUri.startsWith("/"))
            valuesUri = "/" + valuesUri;
        String fullUri = valuesUri;
        if (valuesUri.indexOf("//") == -1 && valuesUri.indexOf("/+/") == -1)
            fullUri = WebServer.urlEncodePath(getPrefix()) + "/" + valuesUri;
        return Arrays.asList(getRequestAsString(fullUri).split("\n"));
    }

    /**
     * Write the HTML for the GUI that the user will interact with to create
     * a new item in the list.
     */
    @Override
    protected void writeItemAdditionGui(String namespace, String itemType)
            throws IOException {
        String newItemPrompt = getParameter("newItemPrompt");
        String valuesUri = getRequiredParam("valuesUri");
        List internalValues = readValues(valuesUri);

        if (newItemPrompt != null) {
            out.write("<b>");
            out.write(newItemPrompt);
            out.write("</b>&nbsp;");
        }

        String fieldName = "auto" + namespace + itemType;
        TranslatingAutocompleter.writeEditor(out, fieldName, null,
                    "size='80'", internalValues,
                    "DashCMS.addAutocompleteToList.bind(DashCMS)");
    }

}
