// Copyright (C) 2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.RobustFileOutputStream;

public class DaemonMetadata {

    public enum State {
        Sleep, Inbound, Outbound
    }


    private static final String LAST_PREFIX = "last";

    private static final String STATE_ATTR = "state";

    private static final String NEXT_TIMESTAMP_ATTR = "until";

    private File metadataDir;

    private File daemonStateFile;

    private Properties stateProps;

    private Logger log;


    public DaemonMetadata(String systemID, File wbsDir) {
        if (wbsDir != null) {
            metadataDir = new File(wbsDir, "sync");
            daemonStateFile = new File(metadataDir,
                    systemID + "-sync-daemon-state.txt");
        }
        this.stateProps = new Properties();
        this.log = Logger.getLogger(getClass().getName() + "." + systemID);
    }


    public void setState(State state, long expectedDuration)
            throws IOException {
        // if we're leaving an inbound/outbound state, record the current time
        // as the moment that operation finished.
        State oldState = getState();
        if (oldState != null && oldState != state && oldState != State.Sleep)
            stateProps.put(LAST_PREFIX + oldState, tstamp(0));

        // save the new state and expected duration
        stateProps.put(STATE_ATTR, state.toString());
        stateProps.put(NEXT_TIMESTAMP_ATTR, tstamp(expectedDuration));
        save();
    }

    public State getState() {
        try {
            return State.valueOf((String) stateProps.get(STATE_ATTR));
        } catch (Exception e) {
            return null;
        }
    }

    public long getLastTime(State state) {
        try {
            return Long.parseLong((String) stateProps.get(LAST_PREFIX + state));
        } catch (Exception e) {
            return -1;
        }
    }

    public long getNextTime() {
        try {
            return Long.parseLong((String) stateProps.get(NEXT_TIMESTAMP_ATTR));
        } catch (Exception e) {
            return -1;
        }
    }

    public void load() throws IOException {
        if (daemonStateFile != null && daemonStateFile.isFile()) {
            FileInputStream in = new FileInputStream(daemonStateFile);
            stateProps.clear();
            stateProps.load(in);
            in.close();
        }
    }

    public void save() throws IOException {
        if (daemonStateFile != null) {
            RobustFileOutputStream out = new RobustFileOutputStream(
                    daemonStateFile);
            stateProps.store(out, null);
            out.close();
            log.finer("Daemon state: " + stateProps.toString());
        }
    }

    private static String tstamp(long delay) {
        return Long.toString(System.currentTimeMillis() + delay);
    }

}
