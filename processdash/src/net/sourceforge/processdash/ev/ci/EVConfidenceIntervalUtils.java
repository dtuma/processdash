// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ci;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EVConfidenceIntervalUtils {

    public enum Purpose {
        TASK_COST(COST_INTERVAL_TAG), SCHEDULE_TIME_ERR(TIME_ERR_INTERVAL_TAG);

        private String tagName;

        private Purpose(String tagName) {
            this.tagName = tagName;
        }

        private String getTagName() {
            return tagName;
        }
    };

    private EVConfidenceIntervalUtils() {}


    /**
     * Analyze the data in a list of earned value schedules, produce confidence
     * intervals representing this historical dataset, and return the results of
     * this analysis as a simple string that can be persisted for later use.
     * 
     * @param ctx
     *                the dashboard context
     * @param taskListNames
     *                the names of earned value schedules that should be
     *                included in the analysis
     * @return a string for persistently storing the results of this analysis
     */
    public static String makeHistoricalConfidenceIntervals(
            DashboardContext ctx, Iterable<String> taskListNames) {
        if (taskListNames == null)
            return null;

        StringBuffer result = new StringBuffer();
        result.append("<" + HIST_INTERVALS_TAG //
                + " " + VERSION_ATTR + "='1'>");

        List<EVTaskList> taskLists = new ArrayList<EVTaskList>();
        for (String taskListName : taskListNames) {
            EVTaskList tl = EVTaskList.openExisting(taskListName,
                ctx.getData(), ctx.getHierarchy(), ctx.getCache(), false);
            if (tl == null)
                continue;

            tl.setDependencyCalculator(null);
            tl.recalc();
            taskLists.add(tl);
            result.append("<" + HIST_TASK_LIST_TAG + " " + NAME_ATTR + "='")
                    .append(XMLUtils.escapeAttribute(taskListName)) //
                    .append("' />");
        }
        if (taskLists.isEmpty())
            return null;

        EVTimeErrConfidenceInterval time = new EVTimeErrConfidenceInterval(
                taskLists);
        if (time.getViability() >= ConfidenceInterval.ACCEPTABLE)
            time.saveToXML(TIME_ERR_INTERVAL_TAG, result);

        EVCostConfidenceInterval cost = new EVCostConfidenceInterval(
                taskLists.toArray());
        if (cost.getViability() >= ConfidenceInterval.ACCEPTABLE)
            cost.saveToXML(COST_INTERVAL_TAG, result);

        result.append("</" + HIST_INTERVALS_TAG + ">");

        return result.toString();
    }



    /**
     * Return the names of the earned value schedules that were included in the
     * historical dataset used to perform a prior analysis.
     * 
     * @param histSetting
     *                the persistent string returned by the
     *                {@link #makeHistoricalConfidenceIntervals(DashboardContext, Iterable)}
     *                method
     * @return the names of the earned value schedules that were used in the
     *         analysis during that prior call.
     */
    public static List<String> getHistoricalTaskListNames(String histSetting) {
        if (!StringUtils.hasValue(histSetting))
            return null;

        try {
            List<String> result = new ArrayList<String>();
            Element doc = XMLUtils.parse(histSetting).getDocumentElement();
            NodeList elems = doc.getElementsByTagName(HIST_TASK_LIST_TAG);
            for (int i = 0; i < elems.getLength(); i++) {
                Element e = (Element) elems.item(i);
                result.add(e.getAttribute(NAME_ATTR));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return a confidence interval from a historical analysis.
     * 
     * @param setting
     *                the persistent string returned by the
     *                {@link #makeHistoricalConfidenceIntervals(DashboardContext, Iterable)}
     *                method
     * @param purpose
     *                the type of confidence interval desired
     * @return the confidence interval that was calculated during the historical
     *         analysis, or null if no such confidence interval was calculated.
     */
    public static ConfidenceInterval getConfidenceInterval(String setting,
            Purpose purpose) {
        if (!StringUtils.hasValue(setting))
            return null;

        try {
            Element doc = XMLUtils.parse(setting).getDocumentElement();
            return AbstractConfidenceInterval.readFromXML(doc
                    .getElementsByTagName(purpose.getTagName()));
        } catch (Exception e) {
            return null;
        }
    }



    private static final String HIST_INTERVALS_TAG = "histIntervals";

    private static final String VERSION_ATTR = "version";

    private static final String HIST_TASK_LIST_TAG = "withTaskList";

    private static final String NAME_ATTR = "name";

    private static final String TIME_ERR_INTERVAL_TAG = "timeErrInterval";

    private static final String COST_INTERVAL_TAG = "costInterval";

}
