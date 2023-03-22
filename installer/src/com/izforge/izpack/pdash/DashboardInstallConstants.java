package com.izforge.izpack.pdash;

public interface DashboardInstallConstants {

    /** The IzPack variable holding the path to the user's data directory. */
    String DATA_PATH = "DATA_PATH";

    /** The IzPack variable holding the path to the user's data directory,
     *  normalized with "~" if appropriate. */
    String DATA_PATH_NORMALIZED = "DATA_PATH_NORMALIZED";

    /** The IzPack variable holding the path to the team data directory. */
    String TEAM_DATA_PATH = "TEAM_DATA_PATH";

    /** The IzPack variable holding a flag indicating whether the team
     *  data directory is a local (non-http, non-bundled) location. */
    String TEAM_DATA_LOCAL_FLAG = "TEAM_DATA_LOCAL";

    /** The IzPack variable holding the max memory setting */
    String MAX_MEMORY = "MAX_MEMORY";

    /** The IzPack variable for recording the Mac OS X "Applications" dir */
    String MAC_APPLICATION_DIR = "MAC_APPL";

    /** Filename of the startup script we create on Unix */
    String UNIX_STARTUP_SCRIPT = "run-dash.sh";

    /** The Preferences node used to save the user values */
    String USER_VALUES_PREFS_NODE = "/net/sourceforge/processdash/installer";

}
