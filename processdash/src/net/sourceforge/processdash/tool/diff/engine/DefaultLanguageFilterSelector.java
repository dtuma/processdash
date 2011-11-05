// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.tool.diff.DefaultFilter;
import net.sourceforge.processdash.tool.diff.LanguageFilter;

public class DefaultLanguageFilterSelector implements LanguageFilterSelector {

    private List languageFilters;

    public DefaultLanguageFilterSelector(List languageFilters) {
        this.languageFilters = languageFilters;
    }

    public LanguageFilter selectLanguageFilter(FileToAnalyze file,
            Charset charset, String initialContents, String options) {
        LanguageFilter resultFilter = null;

        String filename = file.getFilename();

        if (languageFilters != null) {
            Iterator i = languageFilters.iterator();
            int currentRating, resultRating = 0;
            while (i.hasNext()) {
                LanguageFilter currentFilter = null;
                try {
                    currentFilter = (LanguageFilter) i.next();
                } catch (ClassCastException e) {
                }
                if (currentFilter == null)
                    continue;

                currentRating = currentFilter.languageMatches(filename,
                    initialContents, options);

                if (currentRating > resultRating) {
                    resultRating = currentRating;
                    resultFilter = currentFilter;
                }
            }
        }

        if (resultFilter == null) {
            resultFilter = new DefaultFilter();
        }

        return resultFilter;
    }

}
