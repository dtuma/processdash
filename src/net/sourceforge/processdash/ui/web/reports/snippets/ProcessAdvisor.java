// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.net.cms.PageContentTO;
import net.sourceforge.processdash.net.cms.SnippetInstanceTO;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.ui.snippet.SnippetPageFilter;

public class ProcessAdvisor implements SnippetPageFilter {

    public static final String ADVISOR_SNIPPET_CATEGORY = "Advice";

    public void filter(PageContentTO page, SnippetInstanceTO snippet,
            String mode) {
        List snippets = page.getSnippets();
        int pos = snippets.indexOf(snippet);
        if (pos == -1)
            return;

        List advisorSnippets = getAdvisorSnippets(snippet.getPageRegion());
        snippets.remove(pos);
        snippets.addAll(pos, advisorSnippets);
    }

    /**
     * @param pageRegion
     * @return a list of all snippets in the "advice" category
     */
    private static List getAdvisorSnippets(int pageRegion) {
        List result = new ArrayList();
        Set snippets = SnippetDefinitionManager.getAllSnippets();
        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnippetDefinition snipDef = (SnippetDefinition) i.next();
            if (snipDef.matchesCategory(ADVISOR_SNIPPET_CATEGORY)) {
                SnippetInstanceTO snip = new SnippetInstanceTO();
                snip.setSnippetID(snipDef.getId());
                snip.setDefinition(snipDef);
                snip.setPageRegion(pageRegion);
                result.add(snip);
            }
        }
        return result;
    }

}
