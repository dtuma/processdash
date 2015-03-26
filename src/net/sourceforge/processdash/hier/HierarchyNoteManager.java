// Copyright (C) 2007-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.InheritedValue;
import net.sourceforge.processdash.hier.HierarchyNote.InvalidNoteSpecification;
import net.sourceforge.processdash.hier.ui.HierarchyNoteFormat;
import net.sourceforge.processdash.hier.ui.PlainTextNoteFormat;
import net.sourceforge.processdash.templates.ExtensionManager;

public class HierarchyNoteManager {

    public static final String NOTE_KEY = "Team_Note";

    public static final String NOTE_BASE_KEY = "Team_Note.Base";

    public static final String NOTE_CONFLICT_KEY = "Team_Note.Conflict";

    // Contains all objects that want to be notified when changes are made to the notes.
    private static ArrayList<HierarchyNoteListener> listeners =
        new ArrayList<HierarchyNoteListener>();

    public static void addHierarchyNoteListener(HierarchyNoteListener l) {
        listeners.add(l);
    }

    public static void removeHierarchyNoteListener(HierarchyNoteListener l) {
        listeners.remove(l);
    }

    private static void notifyListeners(String path) {
        HierarchyNoteEvent e = new HierarchyNoteEvent(
                HierarchyNoteManager.class, path);
        for (HierarchyNoteListener o : new ArrayList<HierarchyNoteListener>(listeners)) {
            o.notesChanged(e);
        }
    }

    private static Logger logger = Logger.getLogger(HierarchyNoteManager.class
            .getName());


    public static Map<String, HierarchyNote> getNotesForPath(DataContext data,
            String path) {
        return getNotesForPath(data, path, true);
    }

    public static Map<String, HierarchyNote> getNotesForPath(DataContext data,
            String path, boolean ignoreBlank) {

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

            if (ignoreBlank && isOnlyIgnorableBlankNote(result))
                return null;

            return result;

        } catch (InvalidNoteSpecification e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check to see if the given noteData map contains just an empty note.
     * 
     * When a user deletes all the text in a note, it becomes indistinguishable
     * from the "no note present" scenario.  We cannot actually delete the
     * note in that case (because doing so would cause us to lose important
     * team synchronization information), but we can still treat the empty
     * note as "blank" from an end-user perspective.  This method returns true
     * in that case.
     * 
     * @param noteData the note data collected for a hierarchy node
     * @return true if the data only contains an ignorable blank note
     */
    private static boolean isOnlyIgnorableBlankNote(
            Map<String, HierarchyNote> noteData) {
        // if the main note for this hierarchy path is empty of all text,
        // treat that as synonymous with "no note present."  (Exception:
        // if a conflict note is present, we can't ignore it.)

        // we cannot ignore exception notes.  If one is present,
        if (noteData.containsKey(NOTE_CONFLICT_KEY))
            return false;

        HierarchyNote note = noteData.get(NOTE_KEY);
        if (note == null)
            return true;

        String content = note.getContent();
        return (content == null || content.trim().length() == 0);
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
                    HierarchyNote note = (HierarchyNote) e.getValue();
                    // When saving the main note, fill in the author/timestamp
                    // data if it is missing
                    if (dataName == TEAM_NOTE_DATA_NAME) {
                        if (note.getTimestamp() == null)
                            note.setTimestamp(new Date());
                        if (note.getAuthor() == null)
                            note.setAuthor(ProcessDashboard.getOwnerName(data));
                    }
                    val = (note).getAsData();
                }
                String fullDataName = DataRepository.createDataName(path,
                    dataName);
                data.putValue(fullDataName, val);
            }
        }

        notifyListeners(path);
    }

    public static String getDefaultNoteFormatID(DataContext data,
            String path) {
        SimpleData val = InheritedValue.get(data, path,
            TEAM_NOTE_FORMAT_DATA_NAME).getSimpleValue();
        if (val != null && val.test())
            return val.format();
        else
            return PlainTextNoteFormat.FORMAT_ID;
    }

    public static HierarchyNoteFormat getDefaultNoteFormat(DataContext data,
            String path) {
        String formatID = getDefaultNoteFormatID(data, path);
        return getNoteFormat(formatID);
    }

    private static final String TEAM_NOTE_DATA_NAME = NOTE_KEY;

    private static final String TEAM_NOTE_LAST_SYNC_DATA_NAME =
            TEAM_NOTE_DATA_NAME + "_Last_Synced_Val";

    private static final String TEAM_NOTE_CONFLICT_DATA_NAME =
            TEAM_NOTE_DATA_NAME + "_Edit_Conflict_Val";

    private static final String TEAM_NOTE_FORMAT_DATA_NAME =
            TEAM_NOTE_DATA_NAME + "_Default_Format";


    public static HierarchyNoteFormat getNoteFormat(String formatID) {
        HierarchyNoteFormat result = getFormatters().get(formatID);
        if (result == null) {
            logger.severe("Unrecognized note format '" + formatID
                    + "' - using plain text instead");
            result = getFormatters().get(PlainTextNoteFormat.FORMAT_ID);
        }
        return result;
    }

    private static Map<String, HierarchyNoteFormat> NOTE_FORMATS = null;

    private static synchronized Map<String, HierarchyNoteFormat> getFormatters() {
        if (NOTE_FORMATS == null) {
            Map<String, HierarchyNoteFormat> result =
                new HashMap<String, HierarchyNoteFormat>();
            List formats = ExtensionManager.getExecutableExtensions(
                NOTE_FORMAT_EXTENSION_TAG, null);
            for (Iterator i = formats.iterator(); i.hasNext();) {
                Object o = (Object) i.next();
                if (o instanceof HierarchyNoteFormat) {
                    HierarchyNoteFormat nf = (HierarchyNoteFormat) o;
                    result.put(nf.getID(), nf);
                }
            }
            NOTE_FORMATS = Collections.synchronizedMap(result);
        }
        return NOTE_FORMATS;
    }

    private static final String NOTE_FORMAT_EXTENSION_TAG = "noteFormat";

}
