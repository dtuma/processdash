// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier;

import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.HierarchyNote.InvalidNoteSpecification;

public class HierarchyNoteManager {

    public static final String NOTE_KEY = "Team_Note";

    public static final String NOTE_BASE_KEY = "Team_Note.Base";

    public static final String NOTE_CONFLICT_KEY = "Team_Note.Conflict";


    public static Map<String, HierarchyNote> getNotesForPath(DataContext data,
            String path) {
        String dataName = DataRepository.createDataName(path,
            TEAM_NOTE_DATA_NAME);
        SimpleData val = data.getSimpleValue(dataName);
        if (val == null)
            return null;

        Map<String, HierarchyNote> result;
        result = new LinkedHashMap<String, HierarchyNote>();
        try {
            result.put(NOTE_KEY, new HierarchyNote(val));

            dataName = DataRepository.createDataName(path,
                TEAM_NOTE_CONFLICT_DATA_NAME);
            val = data.getSimpleValue(dataName);
            if (val != null)
                result.put(NOTE_CONFLICT_KEY, new HierarchyNote(val));

            dataName = DataRepository.createDataName(path,
                TEAM_NOTE_LAST_SYNC_DATA_NAME);
            val = data.getSimpleValue(dataName);
            if (val != null)
                result.put(NOTE_BASE_KEY, new HierarchyNote(val));

            return result;

        } catch (InvalidNoteSpecification e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveNotesForPath(DataContext data, String path,
            Map<String, HierarchyNote> noteData) {
        for (Map.Entry<String, HierarchyNote> e : noteData.entrySet()) {
            String dataName = null;
            if (NOTE_KEY.equals(e.getKey()))
                dataName = TEAM_NOTE_DATA_NAME;
            else if (NOTE_CONFLICT_KEY.equals(e.getKey()))
                dataName = TEAM_NOTE_CONFLICT_DATA_NAME;
            else if (NOTE_BASE_KEY.equals(e.getKey()))
                dataName = TEAM_NOTE_LAST_SYNC_DATA_NAME;

            if (dataName != null) {
                SimpleData val = null;
                if (e.getValue() instanceof HierarchyNote) {
                    val = ((HierarchyNote) e.getValue()).getAsData();
                }
                String fullDataName = DataRepository.createDataName(path,
                    dataName);
                data.putValue(fullDataName, val);
            }

        }
    }


    private static final String TEAM_NOTE_DATA_NAME = NOTE_KEY;

    private static final String TEAM_NOTE_LAST_SYNC_DATA_NAME =
            TEAM_NOTE_DATA_NAME + "_Last_Synced_Val";

    private static final String TEAM_NOTE_CONFLICT_DATA_NAME =
            TEAM_NOTE_DATA_NAME + "_Edit_Conflict_Val";

}
