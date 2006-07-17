// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** This class is used when a snippet editor needs to allow the user to
 * select a list of metrics.
 * 
 * @author Tuma
 *
 */
public class AutocompletingListEditor extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String listItemUri = getRequiredParam("itemUri");
        String namespace = getOptionalParam("ns", "$$$_");
        String itemType = getOptionalParam("itemType", "Metric");
        String internalValParam =  getOptionalParam("valParam", "DataName");
        String displayValParam = getParameter("displayParam");
        String prompt = getParameter("newItemPrompt");

        String valuesUri = getRequiredParam("valuesUri");
        List values = readValues(valuesUri);

        writeEditor(out, getTinyWebServer(), listItemUri, namespace, itemType,
                internalValParam, displayValParam, parameters, null, values,
                prompt);
    }

    protected String getRequiredParam(String name) throws IOException {
        String result = getParameter(name);
        if (result == null || result.trim().length() == 0)
            throw new IOException(name + " must be specified");
        return result;
    }

    protected String getOptionalParam(String name, String defaultVal) {
        String result = getParameter(name);
        if (result == null || result.trim().length() == 0)
            return defaultVal;
        else
            return result;
    }

    private List readValues(String valuesUri) throws IOException {
        if (!valuesUri.startsWith("/"))
            valuesUri = "/" + valuesUri;
        String fullUri = valuesUri;
        if (valuesUri.indexOf("//") == -1 && valuesUri.indexOf("/+/") == -1)
            fullUri = WebServer.urlEncodePath(getPrefix()) + "/" + valuesUri;
        return Arrays.asList(getRequestAsString(fullUri).split("\n"));
    }

    public static void writeEditor(Writer out, WebServer webServer,
            String listItemUri, String namespace, String itemType,
            String internalValParam, String displayValParam,
            Map parameters, List displayValues, Collection internalValues,
            String newItemPrompt)
            throws IOException {

        String newItemUri = "dash/snippets/snipEnum"
            + "?ns=" + namespace
            + "&type=" + itemType
            + "&uri=" + HTMLUtils.urlEncode(listItemUri)
            + "&defaults=t"
            + "&" + internalValParam + "=_VALUE_"
            + "&" + displayValParam + "=_DISPLAY_"
            + "&item=_ITEM_";

        out.write("<input type='hidden' name='cmsIgnored' id='");
        out.write(namespace);
        out.write(itemType);
        out.write("_uri' value='");
        out.write(esc(newItemUri));
        out.write("'/>");

        out.write("<div class='cmsListContainer'>");

        out.write("<div id='");
        out.write(namespace);
        out.write(itemType);
        out.write("_container'>");
        SnippetDataEnumerator.writeListOfItems(out, webServer, listItemUri,
                namespace, itemType, parameters);

        out.write("</div>");

        if (newItemPrompt != null) {
            out.write("<b>");
            out.write(newItemPrompt);
            out.write("</b>&nbsp;");
        }

        String fieldName = "auto" + namespace + itemType;
        if (displayValues == null)
            TranslatingAutocompleter.writeEditor(out, fieldName, null,
                    "size='80'", internalValues,
                    "DashCMS.addAutocompleteToList.bind(DashCMS)");
        else
            TranslatingAutocompleter.writeEditor(out, fieldName, null,
                    "size='80'", displayValues, (List) internalValues,
                    "DashCMS.addAutocompleteToList.bind(DashCMS)");

        out.write("</div>");
        out.write("<script>DashCMS.fixupSortableList('");
        out.write(namespace);
        out.write(itemType);
        out.write("_container', null);</script>");
    }

    private static String esc(String s) {
        return XMLUtils.escapeAttribute(s);
    }

}
