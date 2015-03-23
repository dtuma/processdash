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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.reports.snippets.ProcessAdvisor;
import net.sourceforge.processdash.util.HTMLUtils;

/** This TinyCGI script generates an HTML fragment listing available snippets,
 * and inviting the user to select one for addition to the page.
 */
public class AddNewSnippet extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("CMS.Snippet.AddNew");

    private static final String[] HIDDEN_CATEGORIES = {
        ProcessAdvisor.ADVISOR_SNIPPET_CATEGORY
    };

    private static final String[] CATEGORIES = { "General", "Forms", "Charts",
        "Reports", "Instructor" };

    protected void writeContents() throws IOException {

        out.write("<table border='0' cellpadding='0' cellspacing='0' " +
                        "width='95%'><tr><td valign='top' width='50%'>\n\n");

        out.write("<b>");
        out.write(resources.getHTML("Prompt"));
        out.write("</b><ul>\n");

        TreeSet snippets = getSortedSnippets();

        TreeSet snipsToWrite = new TreeSet(snippets);

        // discard any snippets that are in known, hidden categories.
        for (int i = 0; i < HIDDEN_CATEGORIES.length; i++)
            extractSnipsForCategory(snipsToWrite, HIDDEN_CATEGORIES[i]);

        // write out the general-purpose snips first, with no category heading.
        TreeSet generalSnips = extractSnipsForCategory(snipsToWrite,
                CATEGORIES[0]);
        writeSnipLinks(generalSnips);

        // write out the snips for each known category.
        for (int i = 1; i < CATEGORIES.length; i++) {
            String catKey = CATEGORIES[i];
            TreeSet snipsForCategory = extractSnipsForCategory(snipsToWrite,
                    catKey);
            if (!snipsForCategory.isEmpty())
                writeSnipCategory(catKey, snipsForCategory);
        }

        // if any snips didn't name a category (or named an unrecognized
        // category) write them out under an "Other" heading.
        if (!snipsToWrite.isEmpty())
            writeSnipCategory("Other", snipsToWrite);

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
        String[] deny = (String[]) parameters.get("deny_ALL");

        Set allSnippets = SnippetDefinitionManager.getAllSnippets();
        TreeSet snippets = new TreeSet();
        for (Iterator i = allSnippets.iterator(); i.hasNext();) {
            SnippetDefinition d = (SnippetDefinition) i.next();
            if (!d.shouldHide() && !denied(d, deny) && d.matchesContext(ctx))
                snippets.add(new SnipData(d));
        }
        return snippets;
    }

    private boolean denied(SnippetDefinition d, String[] denialList) {
        if (denialList != null) {
            for (int i = 0; i < denialList.length; i++) {
                if (d.getId().equals(denialList[i]))
                    return true;
            }
        }
        return false;
    }

    private TreeSet extractSnipsForCategory(Set snips, String category) {
        TreeSet result = new TreeSet();
        for (Iterator i = snips.iterator(); i.hasNext();) {
            SnipData d = (SnipData) i.next();
            if (d.matchesCategory(category)) {
                result.add(d);
                i.remove();
            }
        }
        return result;
    }

    private void writeSnipCategory(String catKey, TreeSet snipsForCategory) {
        String catName = resources.getHTML("Snippet.Categories."+catKey);
        out.print("<li>");
        out.print(catName);
        out.print("</li>\n<ul>\n");
        writeSnipLinks(snipsForCategory);
        out.print("</ul>\n");
    }

    private void writeSnipLinks(TreeSet snippets) {
        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnipData d = (SnipData) i.next();
            d.writeLink(out);
        }
    }

    private static class SnipData implements Comparable {
        SnippetDefinition defn;
        String name;

        public SnipData(SnippetDefinition defn) {
            this.defn = defn;
            this.name = defn.getNameHtml();
        }

        public boolean matchesCategory(String category) {
            return defn.matchesCategory(category);
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
            if (o == this)
                return 0;

            SnipData that = (SnipData) o;
            int nameCmp = this.name.compareTo(that.name);
            if (nameCmp != 0)
                return nameCmp;
            else
                return this.defn.getId().compareTo(that.defn.getId());
        }

    }

}
