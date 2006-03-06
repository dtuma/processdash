// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui;

import java.awt.Font;
import java.util.Collection;
import java.util.Iterator;

import javax.naming.ldap.HasControls;
import javax.swing.JComponent;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.HTMLUtils;

public class TaskDependencyAnalyzer {

    public static final int HAS_ERROR = 0;
    public static final int HAS_INCOMPLETE = 1;
    public static final int ALL_COMPLETE = 2;
    public static final int NO_DEPENDENCIES = 3;

    public static final String[] RES_KEYS = { "Unresolved", "Incomplete",
            "Complete", "None" };

    private Collection dependencies;

    public boolean hasDependency;

    public boolean hasError;

    public boolean hasIncomplete;

    private static Resources resources = Resources.getDashBundle("EV");

    public TaskDependencyAnalyzer(Object dependencies) {
        if (dependencies instanceof Collection)
            this.dependencies = (Collection) dependencies;
        else
            this.dependencies = null;

        scanDependencies();
    }

    private void scanDependencies() {
        hasDependency = false;
        hasError = false;
        hasIncomplete = false;

        if (dependencies != null)
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                hasDependency = true;
                if (d.isUnresolvable()) {
                    hasError = true;
                } else if (d.getPercentComplete() < 1.0) {
                    hasIncomplete = true;
                }
            }
    }

    public int getStatus() {
        if (hasError)
            return HAS_ERROR;
        else if (hasIncomplete)
            return HAS_INCOMPLETE;
        else if (hasDependency)
            return ALL_COMPLETE;
        else
            return NO_DEPENDENCIES;
    }

    public String getHtmlTable(String tableAttrs, String stopUrl,
            String checkUrl, String sep, boolean includeBodyTags,
            boolean includeTooltips) {
        if (dependencies == null)
            return null;

        StringBuffer descr = new StringBuffer();
        if (includeBodyTags)
            descr.append("<html><body>");
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
            } else if (d.getPercentComplete() < 1.0) {
                if (includeTooltips)
                    descr.append(getTooltip(HAS_INCOMPLETE));
                descr.append("style='text-align:center'><img src='")
                        .append(stopUrl).append("'></td>");
            } else {
                if (includeTooltips)
                    descr.append(getTooltip(ALL_COMPLETE));
                descr.append("style='text-align:center'><img src='")
                        .append(checkUrl).append("'></td>");
            }
            descr.append("<td style='text-align:left' nowrap>");
            descr.append(nvl(d.getDisplayName()));
            if (!d.isUnresolvable()) {
                descr.append(sep).append(nvl(d.getAssignedTo()));
                descr.append(sep).append(
                        EVSchedule.formatPercent(d.getPercentComplete()));
            }
            descr.append("</td></tr>");
        }
        descr.append("</table>");
        if (includeBodyTags)
            descr.append("</body></html>");
        return descr.toString();
    }

    private String nvl(String s) {
        return (s == null ? "" : HTMLUtils.escapeEntities(s));
    }

    private String getTooltip(int which) {
        String key = "Dependency." + RES_KEYS[which] + ".Explanation";
        return "title='" + resources.getHTML(key) + "' ";
    }

}
