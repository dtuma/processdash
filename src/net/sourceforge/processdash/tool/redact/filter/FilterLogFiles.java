// Copyright (C) 2023 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.util.PatternList;

@EnabledFor(RedactFilterIDs.LOG_FILES)
public class FilterLogFiles extends AbstractLineBasedFilter {

    private PatternList linesToKeep;

    public FilterLogFiles() {
        this.filenamePatterns = new PatternList() //
                .addLiteralEquals("log.txt") //
                .addLiteralEquals("histlog.txt");
        this.linesToKeep = new PatternList()
                .addLiteralStartsWith("Process Dashboard - logging started at")
                .addLiteralContains("Process Dashboard version ")
                .addRegexp("^-------------------------+$");
    }

    @Override
    public String getString(String line) {
        if (line != null && linesToKeep.matches(line))
            return line;
        else
            return null;
    }

}
