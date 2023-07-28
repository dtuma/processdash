// Copyright (C) 2012-2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.io.Reader;

import net.sourceforge.processdash.tool.redact.RedactFilter;
import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.util.PatternList;

public class FilterCensoredFiles implements RedactFilter {

    @EnabledFor(RedactFilterIDs.LABELS)
    private boolean deleteLabels;

    @EnabledFor(RedactFilterIDs.NOTES)
    private boolean deleteNotes;

    @EnabledFor(RedactFilterIDs.EXT_LINKS)
    private boolean deleteExtLinks;

    private PatternList filenamePatterns;

    public void afterPropertiesSet() {
        filenamePatterns = new PatternList()
            .addRegexp("^lic[^/]*\\.dat$")
            .addLiteralEquals("redact-metadata.txt")
            .addLiteralEndsWith("/changehistory.xml")
            .addLiteralEndsWith("/tabs.xml")
            .addLiteralEndsWith("/workflowdump.xml");

        if (deleteLabels || deleteNotes)
            filenamePatterns.addLiteralEndsWith("/columns.xml");

        if (deleteExtLinks)
            filenamePatterns
                .addLiteralEndsWith("-sync.pdash")
                .addLiteralEndsWith("/externals.xml");
    }

    public Reader filter(RedactFilterData data, String filename, Reader contents) {
        if (filenamePatterns.matches(filename))
            return null;
        else
            return contents;
    }

}
