// Copyright (C) 2008-2018 Tuma Solutions, LLC
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

import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;

public interface EVSnippetEnvironment extends SnippetEnvironment {

    /** Key that maps to the earned value task list being displayed. */
    String TASK_LIST_KEY = "evTaskList";

    /** Key that maps to the earned value schedule being displayed. */
    String SCHEDULE_KEY = "evSchedule";

    /** Key that maps to the task filter in effect . */
    String TASK_FILTER_KEY = "evTaskFilter";

    /** Key that holds the user-assigned name of the EV snippet. */
    String EV_CUSTOM_SNIPPET_NAME_KEY = "evCustomSnippetName";

    /** Key indicating that we are generating HTML output */
    String HTML_OUTPUT_KEY = "evHtmlOutputTag";

    /** Data context key indicating that earned value data is being drawn */
    String EV_CONTEXT_KEY = "Earned Value Context";

    /** Data context key indicating that earned value data is being drawn
     * for a rollup task list */
    String ROLLUP_EV_CONTEXT_KEY = "Rollup Earned Value Context";

    /** Data context key indicating that earned value data is being drawn for
     * a filtered task list */
    String FILTERED_EV_CONTEXT_KEY = "Filtered Earned Value Context";

    /** Data context key indicating that earned value data is being drawn for
     * an audience that should not see the names of individuals */
    String ANON_EV_CONTEXT_KEY = "Anonymous Earned Value Context";

    /** Data context key indicating that earned value data is being drawn for
     * an audience that should not see cost information */
    String COST_FREE_EV_CONTEXT_KEY = "Cost Free Earned Value Context";

}
