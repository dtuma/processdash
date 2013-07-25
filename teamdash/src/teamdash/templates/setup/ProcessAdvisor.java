// Copyright (C) 2009-2013 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.templates.setup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.net.cms.PageContentTO;
import net.sourceforge.processdash.net.cms.SnippetInstanceTO;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.ui.snippet.SnippetPageFilter;

/**
 * Class to provide process advice for team project plan summaries.
 * 
 * This class is nearly identical to the standard dashboard class
 * net.sourceforge.processdash.ui.web.reports.snippets.ProcessAdvisor; the main
 * difference is that this class does not delete the source snippet from the
 * page as part of filtering. This is important because we must attach the
 * filter to an existing snippet which we know will always be on the page.
 */
public class ProcessAdvisor implements SnippetPageFilter {

    public static final String ADVISOR_SNIPPET_CATEGORY = "Advice";

    public void filter(PageContentTO page, SnippetInstanceTO snippet,
            String mode) {
        List snippets = page.getSnippets();
        int pos = snippets.indexOf(snippet);
        if (pos == -1)
            return;

        List advisorSnippets = getAdvisorSnippets(snippet.getPageRegion());
        snippets.addAll(pos + 1, advisorSnippets);
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
