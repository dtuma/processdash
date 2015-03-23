// Copyright (C) 2011 Tuma Solutions, LLC
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

import java.util.List;

import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.util.Disposable;


/**
 * The result obtained when a single file is analyzed
 */
public class DiffResult implements Disposable {

    private FileToAnalyze file;

    private LanguageFilter languageFilter;

    private String options;

    private AccountingType changeType;

    private int[] locCounts;

    private List<DiffFragment> redlines;

    private boolean hasRedlines;

    public DiffResult(FileToAnalyze file, LanguageFilter languageFilter,
            String options, AccountingType changeType, int[] locCounts,
            List<DiffFragment> redlines) {
        this.file = file;
        this.languageFilter = languageFilter;
        this.options = options;
        this.changeType = changeType;
        this.locCounts = locCounts;
        this.redlines = redlines;
        this.hasRedlines = (redlines != null);
    }

    public FileToAnalyze getFile() {
        return file;
    }

    public LanguageFilter getLanguageFilter() {
        return languageFilter;
    }

    public String getOptions() {
        return options;
    }

    public AccountingType getChangeType() {
        return changeType;
    }

    public int[] getLocCounts() {
        return locCounts;
    }

    public int getLocCount(AccountingType type) {
        if (locCounts == null)
            return 0;
        else
            return locCounts[type.ordinal()];
    }

    public List<DiffFragment> getRedlines() {
        return redlines;
    }

    public boolean hasRedlines() {
        return hasRedlines;
    }

    public void dispose() {
        if (redlines != null)
            redlines.clear();
        redlines = null;
    }

}
