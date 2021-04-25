// Copyright (C) 2020-2021 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;


public class DaemonMetadata {

    public enum State {
        Error, Sleep, Export, Inbound, Outbound
    }


    private static final String LAST_PREFIX = "last";

    private static final String STATE_ATTR = "state";

    private static final String NEXT_TIMESTAMP_ATTR = "until";

    private static final String REFRESH_ATTR = "refreshInterval";

    protected Properties stateProps;

    protected Logger log;


    public DaemonMetadata(String systemID) {
        this.stateProps = new Properties();
        this.log = Logger.getLogger(getClass().getName() + "." + systemID);
    }


    public boolean isSyncRequestSupported() {
        return false;
    }

    public void setSyncRequestPending(boolean pending) throws IOException {}

    public boolean isSyncRequestPending() throws IOException {
        return false;
    }


    public void setRefreshInterval(int milliseconds) {
        stateProps.put(REFRESH_ATTR, Integer.toString(milliseconds));
    }

    public int getRefreshInterval() {
        try {
            return Integer.parseInt(((String) stateProps.get(REFRESH_ATTR)));
        } catch (Exception e) {
            return -1;
        }
    }


    public void setState(State state, long expectedDuration)
            throws IOException {
        // if we're leaving an inbound/outbound state, record the current time
        // as the moment that operation finished.
        State oldState = getState();
        if (oldState == state)
            ; // no change in state; nothing to record
        else if (state == State.Error)
            stateProps.put(LAST_PREFIX + State.Error, tstamp(0));
        else if (oldState != null && oldState.compareTo(State.Sleep) > 0)
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


    public void load() throws IOException {}

    public void save() throws IOException {}


    protected static String tstamp(long delay) {
        return Long.toString(System.currentTimeMillis() + delay);
    }

}
