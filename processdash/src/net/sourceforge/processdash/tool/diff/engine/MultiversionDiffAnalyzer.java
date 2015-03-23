// Copyright (C) 1997-2011 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Added;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Base;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Deleted;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Modified;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Total;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.tool.diff.engine.RedlinedDocument.Block;
import net.sourceforge.processdash.util.Diff;

public class MultiversionDiffAnalyzer extends LocDiffUtils implements
        DiffAnalyzer {

    public static final MultiversionDiffAnalyzer INSTANCE =
            new MultiversionDiffAnalyzer();

    public DiffResult analyze(DiffAnalysisRequest r) throws IOException {

        List versions = r.file.getVersions();
        if (versions == null || versions.isEmpty())
            return null;

        Iterator i = versions.iterator();
        Object baselineVersion = i.next();
        WhitespaceCompareString[] baselineContent = getFileContent(r.file
                .getContents(baselineVersion), r.filter, r.charset);

        RedlinedDocument<WhitespaceCompareString> redlines =
            new RedlinedDocument<WhitespaceCompareString>(baselineContent);
        WhitespaceCompareString[] lastContent = baselineContent;
        boolean trackChange = true;
        while (i.hasNext()) {
            Object nextVersion = i.next();
            WhitespaceCompareString[] nextContent = getFileContent(r.file
                    .getContents(nextVersion), r.filter, r.charset);

            applyDiffs(redlines, lastContent, nextContent, trackChange);

            lastContent = nextContent;
            trackChange = !trackChange;
        }

        if (lastContent != baselineContent)
            // up to now, our comparisons have ignored whitespace.  This
            // means that if a line changed in whitespace only, our final
            // document would show the old whitespace instead of the final
            // whitespace.  The following line updates all lines so they
            // reflect the whitespace in the final document.
            redlines.updateContent(lastContent);

        int[] locCounts = new int[AccountingType.values().length];
        List<DiffFragment> fragments = new ArrayList<DiffFragment>();
        for (RedlinedDocument.Block block : redlines.getRedlineBlocks()) {
            countLinesInBlock(locCounts, r.filter, block);
            fragments.addAll(block.convertToFragments());
        }

        AccountingType changeType = getFileChangeType(r.traits.changeType,
            locCounts);

        return new DiffResult(r.file, r.filter, r.options, changeType,
                locCounts, fragments);
    }

    private void applyDiffs(RedlinedDocument<WhitespaceCompareString> redlines,
            WhitespaceCompareString[] a, WhitespaceCompareString[] b,
            boolean trackChange) {
        Diff diff = new Diff(a, b);
        Diff.change change = diff.diff_2(true);
        while (change != null) {
            redlines.applyChange(change.line0, change.deleted, b, change.line1,
                change.inserted, trackChange);
            change = change.link;
        }
    }

    private void countLinesInBlock(int[] locCounts, LanguageFilter filter,
            Block block) {
        // count the number of unmodified base lines in the block.
        int unmodifiedLines = countLines(block.baseLines, filter);
        locCounts[Base.ordinal()] += unmodifiedLines;
        locCounts[Total.ordinal()] += unmodifiedLines;

        if (block.addedLines == null) {
            // no added lines in this block?  Count the deletions plainly.
            int deletedLines = countLines(block.deletedLines, filter);
            locCounts[Base.ordinal()] += deletedLines;
            locCounts[Deleted.ordinal()] += deletedLines;

        } else if (block.deletedLines == null) {
            // no deleted lines in this block?  Count the additions plainly.
            int addedLines = countLines(block.addedLines, filter);
            locCounts[Added.ordinal()] += addedLines;
            locCounts[Total.ordinal()] += addedLines;

        } else {
            // lines were both added and deleted.
            Object[] deleted = stripComments(block.deletedLines);
            int totalDeleted = countLines(deleted, filter);
            locCounts[Base.ordinal()] += totalDeleted;

            Object[] added = stripComments(block.addedLines);
            int totalAdded = countLines(added, filter);
            locCounts[Total.ordinal()] += totalAdded;

            // The comparison so far has treated changes in comments as
            // significant.  If a "changed" line only included a change to a
            // comment, we don't want that to count toward our counts for
            // added/deleted/modified LOC.  To detect this scenario, we will
            // perform a second-pass comparison that ignores comments.  Then,
            // we will use the diff blocks in that comparison to compute the
            // added, deleted, and modified LOC counts.
            Diff d = new Diff(deleted, added);
            Diff.change chg = d.diff_2(false);
            while (chg != null) {
                int del = countLines(deleted, chg.line0, chg.deleted, filter);
                int add = countLines(added, chg.line1, chg.inserted, filter);
                int mod = Math.min(del, add);
                add -= mod;
                del -= mod;

                locCounts[Added.ordinal()] += add;
                locCounts[Deleted.ordinal()] += del;
                locCounts[Modified.ordinal()] += mod;

                chg = chg.link;
            }
        }
    }

}
