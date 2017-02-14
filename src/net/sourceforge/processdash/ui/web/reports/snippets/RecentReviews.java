// Copyright (C) 2015-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.db.PersonFilter;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.util.AdaptiveNumberFormat;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.FormatUtil;

public class RecentReviews extends HttpServlet {

    private static final String PERMISSION = "pdash.reports.scanner";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PDashServletUtils.registerSnippetBundle(req);
        loadReviewData(req);

        RequestDispatcher disp = getServletContext().getRequestDispatcher(
            "/dash/snippets/recentReviews.jsp");
        disp.forward(req, resp);
    }

    private void loadReviewData(HttpServletRequest req) throws IOException {
        // retrieve metadata about the team process
        PDashContext pdash = (PDashContext) req
                .getAttribute(PDashContext.REQUEST_ATTR);
        PDashData data = pdash.getData();
        String processID = data.getString(TeamDataConstants.PROCESS_ID);
        List<String> reviewPhases = data.getList("/" + processID
                + "/Review_Phase_List");

        // ensure the user has permission to view recent reviews
        PDashQuery query = pdash.getQuery();
        PersonFilter privacyFilter = new PersonFilter(PERMISSION, query);
        if (privacyFilter.isBlock()) {
            req.setAttribute("blocked", Boolean.TRUE);
            return;
        }

        // query the database for data about recently completed reviews
        String[] hql = getHql(req);
        List<Object[]> taskData = query.query(hql[0], processID, reviewPhases);
        List<Integer> planItemKeys = QueryUtils.pluckColumn(taskData, 0);
        List<Object[]> defectCounts = query.query(hql[1], planItemKeys);

        // build objects to hold the resulting data
        List<ReviewRow> reviews = new ArrayList<RecentReviews.ReviewRow>();
        for (Object[] oneRow : taskData) {
            if (privacyFilter.include(oneRow[8]))
                reviews.add(new ReviewRow(oneRow));
        }
        for (Object[] oneRow : defectCounts)
            storeDefectCounts(reviews, oneRow);
        req.setAttribute("reviews", reviews);

        // flag older reviews if necessary
        if (!reviews.isEmpty()) {
            Date newestDate = reviews.get(reviews.size() - 1).completionDate;
            long cutoff = newestDate.getTime() - 2 * DateUtils.WEEKS;
            boolean oneHidden = false;
            for (ReviewRow review : reviews) {
                if (review.setCutoff(cutoff))
                    oneHidden = true;
            }
            req.setAttribute("hasHiddenRows", oneHidden);
        }
    }

    private String[] getHql(HttpServletRequest req) throws IOException {
        URL url = new URL(WebServer.DASHBOARD_PROTOCOL
                + ":/dash/snippets/recentReviews.hql");
        String allHql = new String(FileUtils.slurpContents(url.openStream(),
            true), "UTF-8");
        return allHql.split(";\\s*");
    }

    private void storeDefectCounts(List<ReviewRow> reviews, Object[] oneRow) {
        int planItemKey = (Integer) oneRow[0];
        for (ReviewRow review : reviews) {
            if (planItemKey == review.planItemKey) {
                review.numDefects = ((Number) oneRow[1]).intValue();
                break;
            }
        }
    }


    public class ReviewRow {

        private int planItemKey;

        private String taskName;

        private Date completionDate;

        private double planTime, actualTime, timeRatio;

        private int numDefects;

        private String personName;

        private boolean hidden;

        private ReviewRow(Object[] taskStatus) {
            this.planItemKey = (Integer) taskStatus[0];
            this.taskName = taskStatus[2] + "/" + taskStatus[3];
            this.planTime = num(taskStatus, 4);
            this.actualTime = num(taskStatus, 5);
            this.timeRatio = actualTime / planTime;
            this.completionDate = (Date) taskStatus[6];
            this.personName = (String) taskStatus[7];
        }

        private double num(Object[] row, int col) {
            return ((Number) row[col]).doubleValue();
        }

        private boolean setCutoff(long cutoff) {
            hidden = completionDate.getTime() < cutoff;
            return hidden;
        }

        public boolean isHidden() {
            return hidden;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getPersonName() {
            return personName;
        }

        public Date getCompletionDate() {
            return completionDate;
        }

        public String getPlanTime() {
            return FormatUtil.formatTime(planTime);
        }

        public String getActualTime() {
            return FormatUtil.formatTime(actualTime);
        }

        public String getTimeRatio() {
            if (planTime > 0)
                return FormatUtil.formatPercent(timeRatio);
            else
                return AdaptiveNumberFormat.INF_STRING;
        }

        public int getNumDefects() {
            return numDefects;
        }

        public String getDefectsPerHour() {
            if (actualTime > 0)
                return FormatUtil.formatNumber(numDefects * 60 / actualTime);
            else
                return AdaptiveNumberFormat.INF_STRING;
        }

    }

}
