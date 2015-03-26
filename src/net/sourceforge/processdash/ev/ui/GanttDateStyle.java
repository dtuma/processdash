// Copyright (C) 2009 Tuma Solutions, LLC
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

import java.util.Date;

import net.sourceforge.processdash.ev.EVTask;

public enum GanttDateStyle {

    BASELINE {
        public Date getStartDate(EVTask t) {
            return t.getBaselineStartDate();
        }

        public Date getFinishDate(EVTask t) {
            return t.getBaselineDate();
        }
    },

    PLAN {
        public Date getStartDate(EVTask t) {
            return t.getPlanStartDate();
        }

        public Date getFinishDate(EVTask t) {
            return t.getPlanDate();
        }
    },

    REPLAN {
        public Date getStartDate(EVTask t) {
            return t.getReplanStartDate();
        }

        public Date getFinishDate(EVTask t) {
            return t.getReplanDate();
        }
    },

    FORECAST {
        public Date getStartDate(EVTask t) {
            return t.getForecastStartDate();
        }

        public Date getFinishDate(EVTask t) {
            return t.getForecastDate();
        }
    };

    public abstract Date getStartDate(EVTask t);

    public abstract Date getFinishDate(EVTask t);

}
