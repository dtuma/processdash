// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package teamdash.wbs;

import static teamdash.wbs.columns.TeamMemberTimeColumn.TEAM_MEMBER_TIME_SUFFIX;
import static teamdash.wbs.columns.TeamTimeColumn.TEAM_TIME_ATTR;

import java.util.Map;

import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.AttributeMergeWarning;
import teamdash.merge.AttributeMerger;
import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.DependentAttributeMerger;
import teamdash.merge.MergeWarning.Severity;
import teamdash.merge.ui.MergeConflictNotification.ModelType;
import teamdash.wbs.columns.AbstractNotesColumn;
import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WorkflowResourcesColumn;

public class WBSMerger extends AbstractWBSModelMerger<WBSModel> {

    public WBSMerger(TeamProject base, TeamProject main, TeamProject incoming) {
        this(base.getWBS(), main.getWBS(), incoming.getWBS());
    }

    public WBSMerger(WBSModel base, WBSModel main, WBSModel incoming) {
        super(base, main, incoming);
        ignoreAttributeConflicts("^Dependency Name ", "-Assignment Ordinal$");
        contentMerger.addHandler(
            new PatternList().addLiteralEndsWith(" (Top Down)"),
            TOP_DOWN_BOTTOM_UP_MERGER);
        contentMerger.addHandler(WorkflowResourcesColumn.ATTR_NAME,
            TeamTimeColumn.ROLE_PLACEHOLDER_MERGER);
        contentMerger.addHandler(TeamTimeColumn.KNOWN_ROLES_ATTR,
            TeamTimeColumn.KNOWN_ROLES_MERGER);
        contentMerger.addHandler(new PatternList()
                .addLiteralStartsWith(TeamTimeColumn.ROLE_ASSIGNMENT_PREFIX),
            TeamTimeColumn.ASSIGNED_ROLE_MERGER);
        addNoteAttrHandler(NotesColumn.VALUE_ATTR);
        addNoteAttrHandler(ErrorNotesColumn.VALUE_ATTR);
    }

    private void addNoteAttrHandler(String attrName) {
        contentMerger.addHandler(new PatternList().addLiteralEquals(attrName),
            new DependentAttributeMerger(Severity.CONFLICT).setDependentAttrs(
                attrName, AbstractNotesColumn.getMetadataAttrs(attrName)));
    }

    @Override
    protected WBSModel createWbsModel() {
        return new WBSModel();
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.Wbs;
    }

    @Override
    protected void tweakTreeNodeContent(WBSNodeContent content) {
        // When times are recorded on a task for individuals, a summed time is
        // also recorded on the node for the entire team.  If this sum is
        // present, and if it agrees with the sum of team member time, we
        // should not analyze it separately for conflict detection purposes.
        // The logic below will silently delete those redundant sums.
        //
        // However, if the team time is present and it is distinct from the
        // team member sum, it represents a directly entered time (for example,
        // on a node with no person assignments yet). In that case, the number
        // should be analyzed.

        double teamTime = parseNum(content.get(TEAM_TIME_ATTR));
        if (teamTime > 0) {
            double memberSumTime = 0;
            for (Map.Entry<String, String> e : content.entrySet()) {
                if (e.getKey().endsWith(TEAM_MEMBER_TIME_SUFFIX))
                    memberSumTime += parseNum(e.getValue());
            }

            if (Math.abs(teamTime - memberSumTime) < 0.01)
                content.remove(TEAM_TIME_ATTR);
        }
    }

    private double parseNum(String s) {
        try {
            if (s != null && s.length() > 0)
                return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
        }
        return 0;
    }



    private static class TopDownBottomUpAttrMerger implements
            AttributeMerger<Integer, String> {

        public String mergeAttribute(Integer nodeID, String attrName,
                String base, String main, String incoming,
                ErrorReporter<Integer> err) {

            if (eq(main, incoming))
                return main;
            else if (eq(base, incoming))
                return main;
            else if (eq(base, main))
                return incoming;

            // if the base value wasn't blank, but one of the incoming
            // branches is, this generally means that the branch in question
            // subdivided this node.  Subdivision makes the top-down value
            // irrelevant, so we will return null to avoid creating a
            // top-down-bottom-up mismatch on the merged node.
            if (isBlank(main) || isBlank(incoming))
                return null;

            // the two branches made conflicting edits.  Log a conflict,
            // then return the value from main.
            err.addMergeWarning(new AttributeMergeWarning<Integer>(
                    Severity.CONFLICT, "Attribute." + attrName, nodeID,
                    attrName, base, main, incoming));
            return main;
        }

        // compare two strings for equality, with the knowledge that they are
        // numbers.  If the two strings only differ in the 4th digit after
        // the decimal point, we will consider them to be equal.
        private boolean eq(String a, String b) {
            // perform a quick test to see if the strings are exactly equal
            if (NullSafeObjectUtils.EQ(a, b))
                return true;
            // if one of the strings is null, they aren't equal
            if (a == null || b == null)
                return false;
            // find the decimal point in string a.
            int dotPos = a.indexOf('.');
            if (dotPos == -1)
                return false;
            // match 3 digits past the decimal point.  If that match length
            // equals or exceeds the length of either number, then we know
            // from our earlier EQ test that they are not equal.
            int matchLen = dotPos+4;
            if (a.length() <= matchLen || b.length() <= matchLen)
                return false;
            // see if the strings are equal up to that point.
            return a.regionMatches(0, b, 0, matchLen);
        }

        private boolean isBlank(String s) {
            return s == null || s.length() == 0;
        }
    }

    private static final TopDownBottomUpAttrMerger TOP_DOWN_BOTTOM_UP_MERGER =
        new TopDownBottomUpAttrMerger();

}
