// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.team;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.event.EventListenerList;

/**
 * Data model for storing information about subteams
 */
public class SubteamDataModel {

    /** Interface for objects that would like to listen for subteam changes */
    public interface Listener extends EventListener {

        public void subteamDataModelChanged(Event e);

    }

    /** Event object used with the SubteamDataModel.Listener interface */
    public static class Event extends EventObject {

        public Event(SubteamDataModel source) {
            super(source);
        }

    }


    private Map<String, Set<Integer>> namedSubteams;

    private EventListenerList listenerList;


    public SubteamDataModel() {
        // create a map to hold subteams. The map is sorted by subteam name,
        // and uses a case insensitive comparison; this means that storing an
        // existing subteam with a new name that differs only in capitalization
        // will replace the old subteam and update the name.
        namedSubteams = new TreeMap<String, Set<Integer>>(
                String.CASE_INSENSITIVE_ORDER);
        listenerList = new EventListenerList();
    }

    public void saveSubteam(String subteamName, Set<Integer> subteamFilter) {
        namedSubteams.remove(subteamName);
        if (subteamFilter != null)
            namedSubteams.put(subteamName, subteamFilter);
        fireChangeEvent();
    }

    public Set<Integer> getSubteamFilter(String subteamName) {
        return namedSubteams.get(subteamName);
    }

    public List<String> getSubteamNames() {
        return new ArrayList(namedSubteams.keySet());
    }

    public void addSubteamDataModelListener(Listener l) {
        listenerList.add(Listener.class, l);
    }

    public void removeSubteamDataModelListener(Listener l) {
        listenerList.remove(Listener.class, l);
    }

    private void fireChangeEvent() {
        Event evt = new Event(this);
        for (Listener l : listenerList.getListeners(Listener.class))
            l.subteamDataModelChanged(evt);
    }

}
