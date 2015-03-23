// Copyright (C) 2008-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

/**
 * This interface holds constant string keys that are used to lookup and store
 * metadata in an EVTaskList
 * 
 * @see EVTaskList#setMetadata(String, String)
 * @see EVTaskList#getMetadata(String)
 */
public interface EVMetadata {

    public interface TimeZone {

        /** Metadata key for the timezone ID */
        String ID = "Timezone";

        public interface RollupStrategy {

            String SETTING = "Timezone.Rollup_Strategy";

            String NO_CHANGE = "noChange";

            String REALIGN_TO_CALENDAR = "alignToCalendar";

        }

    }

    /** EVTaskList metadata keys associated with earned value baselines */
    public interface Baseline {
        String SNAPSHOT_ID = "Baseline_Snapshot_ID";
    }

    /** EVTaskList metadata keys associated with forecast calculations */
    public interface Forecast {

        /**
         * EVTaskList metadata keys associated with forecast range calculations
         */
        public interface Ranges {

            /**
             * Use plan and actual data from the current task list to generate
             * forecast ranges
             */
            String USE_CURRENT_PLAN = "Forecast.Range.Use_Current";

            /**
             * Use data from historical task lists to generate forecast ranges
             */
            String USE_HIST_DATA = "Forecast.Range.Use_Historical_Data";

            /**
             * The saved historical data to use
             */
            String SAVED_HIST_DATA = "Forecast.Range.Historical_Data";

            /**
             * Use hand-entered estimating errors to generate forecast ranges
             */
            String USE_EST_ERRORS = "Forecast.Range.Use_Estimating_Errors";

        }


    }

    /**
     * Reset metrics to zero on the start date, factoring out work done before
     * the schedule began.
     */
    public String REZERO_ON_START_DATE = "Rezero_On_Start_Date";

    /**
     * Data about notes attached to the periods in the EV schedule
     */
    public String SCHEDULE_NOTES = "Schedule_Notes";
}
