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

import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** This class is used when a snippet editor needs to allow the user to
 * select a list of items.
 * 
 * @author Tuma
 *
 */
public abstract class AbstractListEditor extends TinyCGIBase {

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

    @Override
    protected void writeContents() throws IOException {
        writeEditor();
    }

    protected void writeEditor() throws IOException {
        String listItemUri = getRequiredParam("itemUri");
        String namespace = getOptionalParam("ns", "$$$_");
        String itemType = getOptionalParam("itemType", "Metric");

        String newItemUri = "dash/snippets/snipEnum"
            + "?ns=" + namespace
            + "&type=" + itemType
            + "&uri=" + HTMLUtils.urlEncode(listItemUri)
            + "&defaults=true"
            + getExtraNewItemParams()
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
        SnippetDataEnumerator.writeListOfItems(out, getTinyWebServer(),
            listItemUri, namespace, itemType, parameters);
        if ("true".equals(parameters.get("createNewItem")))
            SnippetDataEnumerator.writeOneItem(out, getTinyWebServer(),
                listItemUri, namespace, itemType, parameters, "defaults=true",
                "autoNew");

        out.write("</div>");

        writeItemAdditionGui(namespace, itemType);

        out.write("</div>");
        out.write("<script type=\"text/javascript\">DashCMS.fixupSortableList('");
        out.write(namespace);
        out.write(itemType);
        out.write("_container', null);</script>");
    }

    protected String getExtraNewItemParams() {
        return "";
    }

    /**
     * Write the HTML for the GUI that the user will interact with to create
     * a new item in the list.
     */
    protected abstract void writeItemAdditionGui(String namespace,
            String itemType) throws IOException;

    protected static String esc(String s) {
        return XMLUtils.escapeAttribute(s);
    }

}
