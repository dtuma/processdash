// Copyright (C) 2012 Tuma Solutions, LLC
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

import java.util.List;
import java.util.regex.Pattern;

import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;

public class WBSFilterFactory {

    public static int IGNORE_CASE = 1;
    public static int WHOLE_WORDS = 2;
    public static int ENTIRE_VALUE = 4;

    public static WBSFilter createAnd(final List<WBSFilter> filters) {
        return createAnd(toArray(filters));
    }

    public static WBSFilter createAnd(final WBSFilter... filters) {
        return new WBSFilter() {
            public boolean match(WBSNode node) {
                for (WBSFilter f : filters)
                    if (f.match(node) == false)
                        return false;
                return true;
            }
        };
    }


    public static WBSFilter createOr(final List<WBSFilter> filters) {
        return createOr(toArray(filters));
    }

    public static WBSFilter createOr(final WBSFilter... filters) {
        return new WBSFilter() {
            public boolean match(WBSNode node) {
                for (WBSFilter f : filters)
                    if (f.match(node))
                        return true;
                return false;
            }
        };
    }

    private static WBSFilter[] toArray(List<WBSFilter> filters) {
        return filters.toArray(new WBSFilter[filters.size()]);
    }


    public static WBSFilter createTextFilter(String... tokens) {
        return createOr(createNodeNameFilter(tokens),
            createNoteFilter(tokens));
    }


    public static WBSFilter createNodeNameFilter(String... tokens) {
        return new TextFilter(IGNORE_CASE, tokens) {
            protected String getNodeText(WBSNode node) {
                return node.getName();
            }
        };
    }


    public static WBSFilter createNoteFilter(String... tokens) {
        return createOr(createWbsNoteFilter(tokens),
            createErrorNoteFilter(tokens));
    }

    public static WBSFilter createWbsNoteFilter(String... tokens) {
        return new TextFilter(IGNORE_CASE, tokens) {
            protected String getNodeText(WBSNode node) {
                return NotesColumn.getTextAt(node);
            }
        };
    }

    public static WBSFilter createErrorNoteFilter(String... tokens) {
        return new TextFilter(IGNORE_CASE, tokens) {
            protected String getNodeText(WBSNode node) {
                return ErrorNotesColumn.getTextAt(node);
            }
        };
    }


    public static WBSFilter createDataColumnFilter(final DataColumn column,
            int mask, String... tokens) {
        return new TextFilter(mask, tokens) {
            protected String getNodeText(WBSNode node) {
                Object value = column.getValueAt(node);
                value = WrappedValue.unwrap(value);
                return (value == null ? null : value.toString());
            }
        };
    }

    public static final WBSFilter IS_LEAF = new WBSFilter() {
        public boolean match(WBSNode node) {
            return node.getWbsModel().isLeaf(node);
        }
    };


    public enum TaskStatus { Not_Started, In_Progress, Completed };

    public static WBSFilter createTaskStatusFilter(final TaskStatus... statuses) {
        return new WBSFilter() {
            public boolean match(WBSNode node) {
                TaskStatus status = getStatus(node);
                if (status != null) {
                    for (TaskStatus oneStatus : statuses)
                        if (status == oneStatus)
                            return true;
                }
                return false;
            }
        };
    }

    private static TaskStatus getStatus(WBSNode node) {
        if (node.getWbsModel().isLeaf(node) == false)
            return null;
        else if (PercentCompleteColumn.isComplete(node))
            return TaskStatus.Completed;
        else if (TeamActualTimeColumn.hasActualTime(node))
            return TaskStatus.In_Progress;
        else
            return TaskStatus.Not_Started;
    }


    private static abstract class TextFilter implements WBSFilter {

        int mask;

        String[] tokens;

        Pattern[] wholeWordPatterns;

        public TextFilter(int mask, String[] tokens) {
            this.mask = mask;
            this.tokens = new String[tokens.length];
            this.wholeWordPatterns = new Pattern[tokens.length];

            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                if (is(WHOLE_WORDS))
                    t = t.trim();
                if (is(IGNORE_CASE))
                    t = t.toLowerCase();
                this.tokens[i] = (t.length() > 0 ? t : null);
            }
        }

        public boolean match(WBSNode node) {
            String text = getNodeText(node);
            if (text == null || text.length() == 0)
                return false;

            if (is(IGNORE_CASE))
                text = text.toLowerCase();

            for (int i = 0; i < tokens.length; i++) {
                String tok = tokens[i];
                if (tok != null) {
                    if (is(ENTIRE_VALUE)) {
                        if (text.equals(tok))
                            return true;
                    } else if (text.contains(tok)) {
                        if (!is(WHOLE_WORDS))
                            return true;
                        else if (containsWholeWord(text, i))
                            return true;
                    }
                }
            }

            return false;
        }

        private boolean containsWholeWord(String text, int pos) {
            if (wholeWordPatterns[pos] == null) {
                String tok = tokens[pos];
                String regexp = "\\Q" + tok + "\\E";
                if (Character.isLetterOrDigit(tok.charAt(0)))
                    regexp = "\\b" + regexp;
                if (Character.isLetterOrDigit(tok.charAt(tok.length()-1)))
                    regexp = regexp + "\\b";
                wholeWordPatterns[pos] = Pattern.compile(regexp);
            }
            return wholeWordPatterns[pos].matcher(text).find();
        }

        private boolean is(int flag) {
            return (mask & flag) > 0;
        }

        protected abstract String getNodeText(WBSNode node);

    }

}
