// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import static teamdash.hist.ProjectDiff.resources;
import static teamdash.hist.ProjectWbsTimeChange.eq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.Diff;
import net.sourceforge.processdash.util.HTMLUtils;

import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.TeamTimeColumn;

public class ProjectWbsNodeChange extends ProjectWbsChange {

    private Set<String> indivTimeAttrs;

    private Map<String, String> memberZeroAttrs;

    private Map<String, String> teamMemberNames;

    private Map<WBSNode, Object> children;

    protected ProjectWbsNodeChange(WBSNode parent, WBSNode child,
            Object changeType, Set<String> indivTimeAttrs,
            Map<String, String> memberZeroAttrs,
            Map<String, String> teamMemberNames, String author, Date timestamp,
            Comparator<WBSNode> nodeComparator) {
        super(parent, author, timestamp);
        this.indivTimeAttrs = indivTimeAttrs;
        this.memberZeroAttrs = memberZeroAttrs;
        this.teamMemberNames = teamMemberNames;
        this.children = new TreeMap<WBSNode, Object>(nodeComparator);
        addChild(child, changeType);
    }

    public void addChild(WBSNode child, Object changeType) {
        children.put(child, changeType);
    }

    public Map<WBSNode, Object> getChildren() {
        return children;
    }

    public String getDescription() {
        return getNode().getName() + " -> " + children;
    }

    @Override
    public List<ProjectChangeReportRow> buildReportRows() {
        List<ProjectChangeReportRow> result = new ArrayList();

        // add an initial row naming the parent node
        result.add(new ProjectChangeReportRow(0, true, "wbsChange", null,
                fmt(getNode()), true, "wbs/" + getNode().getTreeNodeID()));

        // now add rows for each of the affected children
        for (Entry<WBSNode, Object> e : children.entrySet()) {
            WBSNode child = e.getKey();
            Object changeType = e.getValue();
            if (changeType instanceof RowData) {
                RowData data = (RowData) changeType;
                result.add(new ProjectChangeReportRow(2, false, //
                        data.getIcon(), data.getIconTooltip(), //
                        data.getMessageHtml(child.getName()), false, null));
                addChildRows(result, child, 2);
            } else {
                String icon = "wbsChangeNode" + changeType;
                String iconTooltip = resources.getString("Wbs.Node."
                        + changeType + "_Icon_Tooltip");
                WbsNodeTimeReportRow row = new WbsNodeTimeReportRow(child, 2,
                        icon, iconTooltip);
                result.add(row);
                addChildRows(result, row, child, 2);
            }
        }

        return result;
    }

    private void addChildRows(List<ProjectChangeReportRow> result,
            WBSNode node, int indent) {
        indent++;
        for (WBSNode child : node.getWbsModel().getChildren(node)) {
            result.add(new ProjectChangeReportRow(indent, false, null, null,
                    getNameHtml(child), false, null));
            addChildRows(result, child, indent);
        }
    }

    private void addChildRows(List<ProjectChangeReportRow> result,
            WbsNodeTimeReportRow parentRow, WBSNode parentNode, int indent) {
        indent++;
        for (WBSNode child : parentNode.getWbsModel().getChildren(parentNode)) {
            WbsNodeTimeReportRow childRow = new WbsNodeTimeReportRow(child,
                    indent, null, null);
            result.add(childRow);
            addChildRows(result, childRow, child, indent);
            parentRow.sumChildData(childRow);
        }
        parentRow.loadDirectTimeData(parentNode);
    }

    private class WbsNodeTimeReportRow extends ProjectChangeReportRow {

        private double totalTime, totalIndivTime;

        private Map<String, Double> indivTimes;

        protected WbsNodeTimeReportRow(WBSNode node, int indent, String icon,
                String iconTooltip) {
            super(indent, false, icon, iconTooltip, getNameHtml(node), false,
                    null);
            indivTimes = new TreeMap<String, Double>();
        }

        public void sumChildData(WbsNodeTimeReportRow childRow) {
            this.totalTime += childRow.totalTime;
            this.totalIndivTime += childRow.totalIndivTime;

            for (Entry<String, Double> e : childRow.indivTimes.entrySet()) {
                String name = e.getKey();
                Double newIndivTime = e.getValue();
                Double oldIndivTime = this.indivTimes.get(name);
                if (oldIndivTime != null)
                    newIndivTime += oldIndivTime;
                this.indivTimes.put(name, newIndivTime);
            }
        }

        public void loadDirectTimeData(WBSNode node) {
            // load the overall team time estimate for this node
            if (totalTime == 0)
                totalTime = getDouble(node, TeamTimeColumn.TEAM_TIME_ATTR, 0.0);

            // look for time estimates to assigned individuals
            if (indivTimes.isEmpty()) {
                for (String indivAttr : indivTimeAttrs) {
                    double indivTime = getTimeAssignment(node, indivAttr);
                    if (!Double.isNaN(indivTime)) {
                        String name = teamMemberNames.get(indivAttr);
                        indivTimes.put(name, indivTime);
                        totalIndivTime += indivTime;
                    }
                }
            }
        }

        private double getDouble(WBSNode node, String attr, double defaultVal) {
            String val = (String) node.getAttribute(attr);
            return (val == null ? defaultVal : Double.parseDouble(val));
        }

        private double getTimeAssignment(WBSNode node, String timeAttr) {
            double result = getDouble(node, timeAttr, 0);
            if (result == 0) {
                String zeroAttr = memberZeroAttrs.get(timeAttr);
                if (node.getAttribute(zeroAttr) == null)
                    result = Double.NaN;
            }
            return result;
        }

        @Override
        public String getHtml() {
            StringBuilder result = new StringBuilder();
            result.append(super.getHtml());

            // maybe append the total time estimate for this component/task
            if (!eq(totalTime, totalIndivTime) || indivTimes.size() != 1) {
                result.append(TIME_SEPARATOR);
                result.append(HTMLUtils.escapeEntities(resources.format(
                    "Wbs.Node.Hours_FMT", ProjectWbsTimeChange.fmt(totalTime))));
            }

            // append time estimates for each individual
            boolean needsComma = false;
            for (Entry<String, Double> e : indivTimes.entrySet()) {
                result.append(needsComma ? ", " : TIME_SEPARATOR);
                result.append(HTMLUtils.escapeEntities(e.getKey()));
                result.append("&nbsp;(");
                result.append(ProjectWbsTimeChange.fmt(e.getValue()));
                result.append(")");
                needsComma = true;
            }

            return result.toString();
        }
    }

    private interface RowData {
        public String getIcon();

        public String getIconTooltip();

        public String getMessageHtml(String nodeName);
    }

    public static class Moved implements RowData {

        private WBSNode oldParent;

        public Moved(WBSNode oldParent) {
            this.oldParent = oldParent;
        }

        public WBSNode getOldParent() {
            return oldParent;
        }

        public String toString() {
            return "Moved from " + oldParent;
        }

        public String getIcon() {
            return "wbsChangeNodeMove";
        }

        public String getIconTooltip() {
            return resources.format("Wbs.Node.Move_Icon_Tooltip_FMT",
                fmt(oldParent));
        }

        public String getMessageHtml(String nodeName) {
            return resources.format("Wbs.Node.Move_Message_HTML_FMT",
                HTMLUtils.escapeEntities(nodeName),
                HTMLUtils.escapeEntities(fmt(oldParent)));
        }

    }

    public static class Renamed implements RowData {

        private String oldName;

        public Renamed(String oldName) {
            this.oldName = oldName;
        }

        public String getOldName() {
            return oldName;
        }

        public String toString() {
            return "Renamed from " + oldName;
        }

        public String getIcon() {
            return "wbsChangeNodeRename";
        }

        public String getIconTooltip() {
            return resources
                    .format("Wbs.Node.Rename_Icon_Tooltip_FMT", oldName);
        }

        public String getMessageHtml(String nodeName) {
            StringBuilder result = new StringBuilder();
            String[] oldWords = splitWords(oldName);
            String[] newWords = splitWords(nodeName);
            Diff diff = new Diff(oldWords, newWords);
            Diff.change c = diff.diff_2(false);
            int pos = 0;
            while (c != null) {
                // append normal, unmodified words before this change
                addWordsHtml(result, newWords, pos, c.line1, null);
                // append any words deleted by this change
                addWordsHtml(result, oldWords, c.line0, c.line0 + c.deleted,
                    "deletedWord");
                // append any words added by this change
                pos = c.line1 + c.inserted;
                addWordsHtml(result, newWords, c.line1, pos, "addedWord");
                // go to the next change.
                c = c.link;
            }
            // append the unmodified region at the end of the node name
            addWordsHtml(result, newWords, pos, newWords.length, null);

            return result.toString();
        }

        private String[] splitWords(String text) {
            List<String> words = new ArrayList<String>();
            Matcher m = WORD_PAT.matcher(text);
            while (m.find())
                words.add(m.group());
            return words.toArray(new String[words.size()]);
        }

        private void addWordsHtml(StringBuilder result, String[] words,
                int start, int end, String css) {
            if (start < end) {
                if (css != null)
                    result.append("<span class=\"").append(css).append("\">");
                for (int pos = start; pos < end;)
                    result.append(HTMLUtils.escapeEntities(words[pos++]));
                if (css != null)
                    result.append("</span>");
            }
        }

    }

    private static final Pattern WORD_PAT = Pattern.compile("\\S+\\s*");

    private static final String TIME_SEPARATOR = "<span class=\"separator\">&mdash;</span>";

}
