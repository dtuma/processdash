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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

/** This TinyCGI script generates an HTML fragment listing available snippets,
 * and inviting the user to select one for addition to the page.
 */
public class AddNewSnippet extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("CMS.Snippet.AddNew");

    protected void writeContents() throws IOException {

        out.write("<table border='0' cellpadding='0' cellspacing='0' " +
                        "width='95%'><tr><td valign='top' width='50%'>\n\n");

        out.write("<b>");
        out.write(resources.getHTML("Prompt"));
        out.write("</b><ul>\n");

        TreeSet snippets = getSortedSnippets();

        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnipData d = (SnipData) i.next();
            d.writeLink(out);
        }

        out.write("</ul>\n\n");

        out.write("<input type='hidden' name='");
        out.write(EditPageParameters.SNIPPET_DISCARDED_);
        out.write("$$$_' value='t'/>\n\n");

        out.write("<a href='#' onclick='DashCMS.deleteSnippet(this); return false;'>");
        out.write(resources.getHTML("Cancel"));
        out.write("</a>\n\n");

        out.write("</td><td class='cmsNewItemDescription' width='50%'><b>");
        out.write(resources.getHTML("Description"));
        out.write("</b><br/>\n");

        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnipData d = (SnipData) i.next();
            d.writeDescription(out);
        }

        out.write("</td></tr></table>\n\n");
    }

    private TreeSet getSortedSnippets() {
        DataContext ctx = getDataContext();

        Set allSnippets = SnippetDefinitionManager.getAllSnippets();
        TreeSet snippets = new TreeSet();
        for (Iterator i = allSnippets.iterator(); i.hasNext();) {
            SnippetDefinition d = (SnippetDefinition) i.next();
            if (!d.shouldHide() && d.matchesContext(ctx))
                snippets.add(new SnipData(d));
        }
        return snippets;
    }

    private static class SnipData implements Comparable {
        SnippetDefinition defn;
        String name;

        public SnipData(SnippetDefinition defn) {
            this.defn = defn;
            this.name = defn.getNameHtml();
        }

        public void writeLink(PrintWriter out) {
            out.write("<li><a href='#' id='link$$$_");
            out.write(HTMLUtils.urlEncode(defn.getId()));
            out.write("' onclick='DashCMS.selectSnippet(this); return false;'");
            out.write(" onmouseover='DashCMS.showAddItemDescr(this);'>");
            out.write(name);
            out.write("</a></li>");
        }

        public void writeDescription(PrintWriter out) {
            out.write("<div style='display:none' id='descr$$$_");
            out.write(HTMLUtils.urlEncode(defn.getId()));
            out.write("'>");
            try {
                out.write(defn.getDescriptionHtml());
            } catch (Exception e) {
                out.write(resources.getHTML("No_Description_Available"));
            }
            out.write("</div>\n");
        }

        public int compareTo(Object o) {
            SnipData that = (SnipData) o;
            return this.name.compareTo(that.name);
        }

    }

}
