// Copyright (C) 2002-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team;

public interface TeamDataConstants {

    public static final String PROJECT_ID = "Project_ID";
    public static final String PROCESS_ID = "Team_Process_PID";
    public static final String PROCESS_NAME = "Team_Process_Name";
    public static final String TEAM_DIRECTORY = "Team_Directory";
    public static final String TEAM_DIRECTORY_UNC = "Team_Directory_UNC";
    public static final String TEAM_DATA_DIRECTORY = "Team_Data_Directory";
    public static final String TEAM_DATA_DIRECTORY_URL = "Team_Data_Directory_URL";
    public static final String PROJECT_SCHEDULE_NAME = "Project_Schedule_Name";
    public static final String PROJECT_SCHEDULE_ID = "Project_Schedule_ID";
    public static final String PROJECT_SCHEDULE_SYNC_PDT =
        "Project_Schedule_PDT_Last_Synced_Val";
    public static final String PROJECT_SCHEDULE_SYNC_SCHEDULE =
        "Project_Schedule_Hours_Last_Synced_Val";
    public static final String SYNC_ROOT_ONLY = "Sync_Project_Root_Only";
    public static final String PROJECT_MILESTONES_INFO = "Project_Milestones_Info";
    public static final String PROJECT_COMPONENT_INFO = "Project_Component_Info";
    public static final String TEAM_MEMBER_COLORS = "Team_Member_Colors";
    public static final String WORKFLOW_PARAM_PREFIX = "Workflow_Param";
    public static final String MASTER_PROJECT_PATH = "Master_Project_Path";
    public static final String RELAUNCHED_PROJECT_FLAG =
        "Project_Was_Relaunched";
    public static final String RELAUNCH_SOURCE_PROJECT_ID =
        "Relaunch_Source_Project_ID";
    public static final String RELAUNCH_SOURCE_WBS_ID =
            "Relaunch_Source_WBS_ID";
    public static final String RELAUNCHABLE_SETTINGS = "Relaunchable_Settings";
    public static final String CLOSED_PROJECT_FLAG = "Project_Is_Closed";

    public static final String TEAM_PROJECT_URL = "Team_URL";
    public static final String INDIV_INITIALS = "Indiv_Initials";
    public static final String WBS_ID_DATA_NAME = "WBS_Unique_ID";
    public static final String CLIENT_ID_DATA_NAME = "Client_Unique_ID";
    public static final String WORKFLOW_ID_DATA_NAME = "Workflow_Source_ID";
    public static final String PROJECT_WORKFLOW_URLS_DATA_NAME =
        "Project_Workflow_URL_List";
    public static final String LAST_SYNC_TIMESTAMP = "WBS_Last_Sync_Timestamp";
    public static final String USER_ACTIVITY_TIMESTAMP = "Data_Activity_Timestamp";
    public static final String USER_DONE_TIMESTAMP = "Project_Is_Done";

    public static final String DISSEMINATION_DIRECTORY = "disseminate";
    public static final String OBSOLETE_DIR_MARKER_FILENAME =
        "00-This-Directory-is-Obsolete.txt";

    public static final String DATASET_OWNER_USERNAME_SYSPROP =
            "com.tuma_solutions.teamserver.datasetOwner.username";
    public static final String DATASET_OWNER_FULLNAME_SYSPROP =
            "com.tuma_solutions.teamserver.datasetOwner.fullname";

}
