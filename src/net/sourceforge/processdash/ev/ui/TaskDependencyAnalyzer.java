// Copyright (C) 2006-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JLabel;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.ui.icons.TaskScheduleIcons;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.HTMLUtils;

public class TaskDependencyAnalyzer {

    public static final int HAS_ERROR = 0;
    public static final int HAS_MISORDERED_INCOMPLETE = 1;
    public static final int HAS_MISORDERED_REVERSE = 2;
    public static final int HAS_INCOMPLETE = 3;
    public static final int ALL_COMPLETE = 4;
    public static final int HAS_REVERSE = 5;
    public static final int HAS_COMPLETED_REVERSE = 6;
    public static final int HAS_COLLAB = 7;
    public static final int NO_DEPENDENCIES = 8;

    public static final String[] RES_KEYS = { "Unresolved",
            "Incomplete_Misordered", "Reverse_Misordered", "Incomplete",
            "Complete", "Reverse", "Reverse_Completed", "Collaborators",
            "None" };

    private Collection dependencies;

    private boolean hideNames;

    public boolean hasDependency;

    public boolean hasError;

    public boolean hasIncomplete;

    public boolean hasReverse;

    public boolean hasMisorderedIncomplete;

    public boolean hasMisorderedReverse;

    public boolean hasCompletedReverse;

    public boolean hasCollab;

    private static Resources resources = Resources.getDashBundle("EV");

    public TaskDependencyAnalyzer(Object dependencies) {
        if (dependencies instanceof Collection)
            this.dependencies = (Collection) dependencies;
        else
            this.dependencies = null;
        hideNames = false;

        scanDependencies();
    }

    private void scanDependencies() {
        hasDependency = false;
        hasError = false;
        hasIncomplete = false;
        hasReverse = false;
        hasMisorderedIncomplete = false;
        hasMisorderedReverse = false;
        hasCollab = false;

        if (dependencies != null)
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                hasDependency = true;
                if (d.isUnresolvable()) {
                    hasError = true;
                } else if (d.isReverse()) {
                    if (d.isIncomplete()) {
                        hasReverse = true;
                        if (d.isMisordered())
                            hasMisorderedReverse = true;
                    } else {
                        hasCompletedReverse = true;
                    }
                } else if (d.isCollab()) {
                    hasCollab = true;
                } else if (d.getPercentComplete() < 1.0) {
                    hasIncomplete = true;
                    if (d.isMisordered())
                        hasMisorderedIncomplete = true;
                }
            }
    }

    public void setHideNames(boolean hideNames) {
        this.hideNames = hideNames;
    }

    public int getStatus() {
        if (hasError)
            return HAS_ERROR;
        else if (hasMisorderedIncomplete)
            return HAS_MISORDERED_INCOMPLETE;
        else if (hasMisorderedReverse)
            return HAS_MISORDERED_REVERSE;
        else if (hasIncomplete)
            return HAS_INCOMPLETE;
        else if (hasReverse)
            return HAS_REVERSE;
        else if (hasCompletedReverse)
            return HAS_COMPLETED_REVERSE;
        else if (hasCollab)
            return HAS_COLLAB;
        else if (hasDependency)
            return ALL_COMPLETE;
        else
            return NO_DEPENDENCIES;
    }

    public String getSortKey() {
        switch (getStatus()) {
        case HAS_ERROR:                 return "0";
        case HAS_MISORDERED_REVERSE:    return "1";
        case HAS_REVERSE:               return "2";
        case HAS_COMPLETED_REVERSE:     return "3";
        case HAS_COLLAB:                return "4";
        case ALL_COMPLETE:              return "5";
        case NO_DEPENDENCIES:           return "6";
        case HAS_INCOMPLETE:            return "7";
        case HAS_MISORDERED_INCOMPLETE: return "8";
        }
        return "0";
    }

    public String getHtmlTable(String tableAttrs, int iconSize, String stopUrl,
            String checkUrl, String reverseUrl, String misorderedUrl,
            String misorderedReverseUrl, String sep, boolean includeBodyTags,
            boolean includeTooltips) {
        if (dependencies == null || getStatus() == NO_DEPENDENCIES)
            return null;

        String iconSizeHtml = "' width='" + iconSize + "' height='" + iconSize;
        StringBuffer descr = new StringBuffer();
        if (includeBodyTags)
            descr.append("<html><body><b>").append(getRes("Explanation_All"))
                    .append("</b>");
        descr.append("<table");
        if (tableAttrs != null)
            descr.append(" ").append(tableAttrs);
        descr.append(">");
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            EVTaskDependency d = (EVTaskDependency) i.next();
            descr.append("<tr><td ");
            if (d.isUnresolvable()) {
                if (includeTooltips)
                    descr.append(getTooltip(HAS_ERROR));
                descr.append("style='text-align:center; "
                        + "color:red; font-weight:bold'>")
                        .append(resources.getHTML("Dependency.Unresolved.Text"))
                        .append("</td>");
            } else if (d.isReverse()) {
                if (d.isIncomplete() == false) {
                    if (includeTooltips)
                        descr.append(getTooltip(HAS_COMPLETED_REVERSE));
                    descr.append("style='text-align:center'><img src='")
                            .append(checkUrl).append(iconSizeHtml)
                            .append("'></td>");
                } else if (d.isMisordered()) {
                    if (includeTooltips)
                        descr.append(getTooltip(HAS_MISORDERED_REVERSE));
                    descr.append("style='text-align:center'><img src='")
                            .append(misorderedReverseUrl).append(iconSizeHtml)
                            .append("'></td>");
                } else {
                    if (includeTooltips)
                        descr.append(getTooltip(HAS_REVERSE));
                    descr.append("style='text-align:center'><img src='")
                            .append(reverseUrl).append(iconSizeHtml)
                            .append("'></td>");
                }
            } else if (d.isCollab()) {
                if (includeTooltips)
                    descr.append(getTooltip(HAS_COLLAB));
                descr.append("style='text-align:center'><img src='")
                        .append(reverseUrl).append(iconSizeHtml)
                        .append("'></td>");
            } else if (d.isIncomplete()) {
                if (d.isMisordered()) {
                    if (includeTooltips)
                        descr.append(getTooltip(HAS_MISORDERED_INCOMPLETE));
                    descr.append("style='text-align:center'><img src='")
                            .append(misorderedUrl).append(iconSizeHtml)
                            .append("'></td>");
                } else {
                    if (includeTooltips)
                        descr.append(getTooltip(HAS_INCOMPLETE));
                    descr.append("style='text-align:center'><img src='")
                            .append(stopUrl).append(iconSizeHtml)
                            .append("'></td>");
                }
            } else {
                if (includeTooltips)
                    descr.append(getTooltip(ALL_COMPLETE));
                descr.append("style='text-align:center'><img src='")
                        .append(checkUrl).append(iconSizeHtml)
                        .append("'></td>");
            }
            descr.append("<td style='text-align:left' nowrap>");
            if (d.isReverse()) {
                String key;
                if (d.isIncomplete() == false) {
                    key = "Dependency.Reverse_Completed.Display";
                } else if (d.isMisordered()) {
                    key = "Dependency.Reverse_Misordered.Display";
                } else {
                    key = "Dependency.Reverse.Display";
                }
                descr.append(resources.getHTML(key));
            } else if (d.isCollab()) {
                descr.append(getRes(HAS_COLLAB, "Display", true));
            } else {
                descr.append(nvl(d.getDisplayName()));
            }
            descr.append(getBriefDetails(d, sep, hideNames));
            descr.append("</td></tr>");
        }
        descr.append("</table>");
        if (includeBodyTags)
            descr.append("</body></html>");
        return descr.toString();
    }

    public static String getBriefDetails(EVTaskDependency d, String sep,
            boolean hideNames) {
        if (d.isUnresolvable())
            return "";

        StringBuffer result = new StringBuffer();
        if (hideNames == false)
            result.append(sep).append(nvl(d.getAssignedTo()));
        Date pd = d.getProjectedDate();
        if (pd != null && d.isIncomplete())
            result.append(sep).append(DATE_FORMAT.format(pd));
        if (!d.isReverse() && !d.isCollab())
            result.append(sep).append(
                    EVSchedule.formatPercent(d.getPercentComplete()));
        return result.toString();
    }

    private static DateFormat DATE_FORMAT = SimpleDateFormat
            .getDateInstance(DateFormat.SHORT);

    private static String nvl(String s) {
        return (s == null ? "" : HTMLUtils.escapeEntities(s));
    }

    private String getTooltip(int which) {
        return "title='" + getRes(which, "Explanation", true) + "' ";
    }

    private static String getTooltipAll(int which) {
        return "title='" + getRes(which, "Explanation_All", true) + "' ";
    }

    public String getRes(String type) {
        return getRes(getStatus(), type, true);
    }

    private static String getRes(int which, String type, boolean html) {
        String key = "Dependency." + RES_KEYS[which] + "." + type;
        return (html ? resources.getHTML(key) : resources.getString(key));
    }

    public static class GUI extends TaskDependencyAnalyzer {

        public GUI(Object dependencies) {
            super(dependencies);
        }

        public String getHtmlTable(String tableAttrs) {
            return super.getHtmlTable(tableAttrs,
                TaskScheduleIcons.getDependencyIconSize(),
                GUI_STOP_URL.toString(), GUI_CHECK_URL.toString(),
                GUI_REVERSE_URL.toString(), GUI_INCOMPLETE_MIS_URL.toString(),
                GUI_REVERSE_MIS_URL.toString(), GUI_SEP, true, false);
        }

        public void syncLabel(JLabel label) {
            label.setToolTipText(getHtmlTable(null));

            switch (getStatus()) {

            case TaskDependencyAnalyzer.NO_DEPENDENCIES:
                label.setIcon(null);
                label.setText(null);
                break;

            case TaskDependencyAnalyzer.HAS_ERROR:
                label.setIcon(null);
                label.setText("<html><body><b style='color:red'>"
                        + getRes("Text") + "</b></body></html>");
                break;

            case TaskDependencyAnalyzer.HAS_INCOMPLETE:
                label.setIcon(GUI_STOP_ICON);
                label.setText(null);
                break;

            case TaskDependencyAnalyzer.HAS_MISORDERED_INCOMPLETE:
                label.setIcon(GUI_INCOMPLETE_MIS_ICON);
                label.setText(null);
                break;

            case TaskDependencyAnalyzer.HAS_REVERSE:
            case TaskDependencyAnalyzer.HAS_COLLAB:
                label.setIcon(GUI_REVERSE_ICON);
                label.setText(null);
                break;

            case TaskDependencyAnalyzer.HAS_MISORDERED_REVERSE:
                label.setIcon(GUI_REVERSE_MIS_ICON);
                label.setText(null);
                break;

            case TaskDependencyAnalyzer.HAS_COMPLETED_REVERSE:
            case TaskDependencyAnalyzer.ALL_COMPLETE:
                label.setIcon(GUI_CHECK_ICON);
                label.setText(null);
                break;
            }

        }
    }

    private static final URL GUI_STOP_URL = TaskScheduleIcons.stopUrl();
    private static final Icon GUI_STOP_ICON = TaskScheduleIcons.stopIcon();
    private static final URL GUI_CHECK_URL = TaskScheduleIcons.checkUrl();
    private static final Icon GUI_CHECK_ICON = TaskScheduleIcons.checkIcon();
    private static final URL GUI_REVERSE_URL = TaskScheduleIcons.groupUrl();
    private static final Icon GUI_REVERSE_ICON = TaskScheduleIcons.groupIcon();
    private static final URL GUI_REVERSE_MIS_URL = TaskScheduleIcons.warningUrl();
    private static final Icon GUI_REVERSE_MIS_ICON = TaskScheduleIcons.warningIcon();
    private static final URL GUI_INCOMPLETE_MIS_URL = TaskScheduleIcons.warningRedUrl();
    private static final Icon GUI_INCOMPLETE_MIS_ICON = TaskScheduleIcons.warningRedIcon();

    private static final String GUI_SEP = "  \u25AA  ";

    public static class HTML extends TaskDependencyAnalyzer {

        public HTML(Object dependencies, boolean hideNames) {
            super(dependencies);
            setHideNames(hideNames);
        }

        public String getHtmlTable(String tableAttrs) {
            return super.getHtmlTable(tableAttrs, 14, HTML_STOP_URI,
                    HTML_CHECK_URI, HTML_REVERSE_URI, HTML_INCOMPLETE_MIS_URI,
                    HTML_REVERSE_MIS_URI, HTML_SEP, false, true);
        }

        public String getHtmlIndicator() {
            return HTML_INDICATORS[getStatus()];
        }
    }

    private static final String HTML_STOP_URI = "/Images/stop.gif";
    private static final String HTML_CHECK_URI = "/Images/check.png";
    private static final String HTML_REVERSE_URI = "/Images/group.gif";
    private static final String HTML_REVERSE_MIS_URI = "/Images/warning.gif";
    private static final String HTML_INCOMPLETE_MIS_URI = "/Images/warningRed.gif";
    static final String HTML_SEP = " &bull; ";
    private static final String HTML_INDICATOR_IMG_ATTRS = "' border='0' width='14' height='14' ";
    private static final String[] HTML_INDICATORS = new String[] {
            // HAS_ERROR
            "<span style='color:red; font-weight:bold' "
                    + getTooltipAll(HAS_ERROR) + ">"
                    + getRes(HAS_ERROR, "Text", true) + "</span>",
            // HAS_MISORDERED_INCOMPLETE
            "<img src='" + TaskDependencyAnalyzer.HTML_INCOMPLETE_MIS_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_MISORDERED_INCOMPLETE) + ">",
            // HAS_MISORDERED_REVERSE
            "<img src='" + TaskDependencyAnalyzer.HTML_REVERSE_MIS_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_MISORDERED_REVERSE) + ">",
            // HAS_INCOMPLETE
            "<img src='" + TaskDependencyAnalyzer.HTML_STOP_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_INCOMPLETE) + ">",
            // ALL_COMPLETE
            "<img src='" + TaskDependencyAnalyzer.HTML_CHECK_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(ALL_COMPLETE) + ">",
            // HAS_REVERSE
            "<img src='" + TaskDependencyAnalyzer.HTML_REVERSE_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_REVERSE) + ">",
            // HAS_COMPLETED_REVERSE
            "<img src='" + TaskDependencyAnalyzer.HTML_CHECK_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_COMPLETED_REVERSE) + ">",
            // HAS_COLLAB
            "<img src='" + TaskDependencyAnalyzer.HTML_REVERSE_URI
                    + HTML_INDICATOR_IMG_ATTRS
                    + getTooltipAll(HAS_COLLAB) + ">",
    };
    static final String HTML_INCOMPLETE_MISORD_IND = "<img src='"
            + TaskDependencyAnalyzer.HTML_INCOMPLETE_MIS_URI
            + HTML_INDICATOR_IMG_ATTRS + ">";
    static final String HTML_REVERSE_MISORD_IND = "<img src='"
            + TaskDependencyAnalyzer.HTML_REVERSE_MIS_URI
            + HTML_INDICATOR_IMG_ATTRS + ">";
}
