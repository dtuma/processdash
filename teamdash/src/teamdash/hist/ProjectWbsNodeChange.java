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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.Diff;
import net.sourceforge.processdash.util.HTMLUtils;

import teamdash.wbs.WBSNode;

public class ProjectWbsNodeChange extends ProjectWbsChange {

    private Map<WBSNode, Object> children;

    protected ProjectWbsNodeChange(WBSNode parent, WBSNode child,
            Object changeType, String author, Date timestamp,
            Comparator<WBSNode> nodeComparator) {
        super(parent, author, timestamp);
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
                fmt(getNode()), true));

        // now add rows for each of the affected children
        for (Entry<WBSNode, Object> e : children.entrySet()) {
            WBSNode child = e.getKey();
            Object changeType = e.getValue();
            String messageHtml;
            String icon, iconTooltip;
            if (changeType instanceof RowData) {
                RowData data = (RowData) changeType;
                icon = data.getIcon();
                iconTooltip = data.getIconTooltip();
                messageHtml = data.getMessageHtml(child.getName());
            } else {
                icon = "wbsChangeNode" + changeType;
                iconTooltip = resources.getString("Wbs.Node." + changeType
                        + "_Icon_Tooltip");
                messageHtml = HTMLUtils.escapeEntities(child.getName());
                if (messageHtml.trim().length() == 0)
                    messageHtml = "<i>(empty)</i>";
            }
            result.add(new ProjectChangeReportRow(2, false, icon, iconTooltip,
                    messageHtml, false));
        }

        return result;
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

}
