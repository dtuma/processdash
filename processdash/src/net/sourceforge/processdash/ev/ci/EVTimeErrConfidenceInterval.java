// Copyright (C) 2003 Tuma Solutions, LLC
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

import java.util.Date;

import net.sourceforge.processdash.ev.EVSchedule;



public class EVTimeErrConfidenceInterval extends DelegatingConfidenceInterval {


    public EVTimeErrConfidenceInterval(EVSchedule sched, boolean center) {
        AbstractConfidenceInterval interval = null;
        if (center)
            // if it is more important to reduce the bias of the resulting
            // interval, use the centered logarithmic approach. (This will
            // ensure that the interval is centered around the appropriate
            // value, at the expense of creating a wider confidence interval.)
            interval = new LogCenteredConfidenceInterval();
        else
            // if it is more important to estimate the width of the confidence
            // interval, use the parametric logarithmic approach.  (This will
            // attempt to generate the most accurate interval possible,
            // although it may center the interval around a number that
            // is different from the one predicted by traditional/linear
            // earned value extrapolations.)
            interval = new LognormalConfidenceInterval();
        delegate = interval;

        Date effDate = sched.getEffectiveDate();

        double plan, act, autoPlan = 0;
        for (int i = 0;   i < sched.getRowCount();   i++) {
            EVSchedule.Period p = sched.get(i);

            // only use completed periods.  If the period in question
            // ends after the effective date, we're done.
            if (p.getEndDate(false).compareTo(effDate) > 0)
                break;

            if (p.planDirectTime() > 0 || p.actualDirectTime() > 0) {
                plan = p.planDirectTime();
                act = p.actualDirectTime();
                if (p.isAutomatic())
                    plan = autoPlan;
                else
                    autoPlan = plan;

                interval.addDataPoint(plan, act);
            }
        }
        interval.dataPointsComplete();
    }

}
