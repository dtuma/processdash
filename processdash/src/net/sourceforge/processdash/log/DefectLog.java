// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


package net.sourceforge.processdash.log;

import pspdash.PSPDashboard;
import pspdash.data.NumberFunction;
import pspdash.data.DoubleData;
import pspdash.data.DataRepository;

import java.io.*;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Iterator;


public class DefectLog {
    String defectLogFilename = null;
    String dataPrefix = null;
    DataRepository data = null;
    PSPDashboard parent;

    public static final String DEF_INJ_SUFFIX = "/Defects Injected";
    public static final String DEF_REM_SUFFIX = "/Defects Removed";


    public DefectLog(String filename, String dataPath, DataRepository data,
              PSPDashboard dash) {
        defectLogFilename = filename;
        this.dataPrefix = dataPath + "/";
        this.data = data;
        this.parent = dash;
    }

    /** Save data for the given defect to the defect log.
     *
     * @param d a new or changed defect.
     */
    public synchronized void writeDefect(Defect d) {
        Defect defectsRead[] = readDefects();

        // Update data elements in the repository concerning defect counts.
        // This will also assign the defect a number if it needs one.
        updateData(defectsRead, d);

        Defect defects[];
        int pos = findDefect(defectsRead, d.number);
        if (pos == -1) {            // new defect
            int count = defectsRead.length + 1;
            defects = new Defect[count];
            defects[--count] = d;
            while (count-- > 0)
                defects[count] = defectsRead[count];
        } else {
            defects = defectsRead;
            defects[pos] = d;
        }

        save(defects);

        if (parent.configure_button.defect_frame != null)
            parent.configure_button.defect_frame.updateDefectLog (this, d);
    }

    /** Delete the named defect from the defect log.
     *
     * @param defectNumber the id number of the defect to delete.
     */
    public void deleteDefect(String defectNumber) {
        Defect defects[] = readDefects();

        int pos = findDefect(defects, defectNumber);
        if (pos != -1) {
            Defect d = defects[pos];
            defects[pos] = null;
            d.number = "DELETE";
            updateData(defects, d);
            save(defects);

            if (parent.configure_button.defect_frame != null)
                parent.configure_button.defect_frame.updateDefectLog (this);
        }
    }

    /** Save the defect data.
     *
     * The data is first written to a temporary file.  Upon successful
     * completion, the temporary file is renamed to the actual defect
     * file.
     */
    private void save(Defect [] defects) {

        String fileSep = System.getProperty("file.separator");

        File defectFile = new File(defectLogFilename);

        // Create temporary files
        File tempFile = new File(defectFile.getParent()+ fileSep +
                                 "tttt_" + defectFile.getName());
        File backupFile = new File(defectFile.getParent()+ fileSep +
                                 "tttt" + defectFile.getName());
        try {
            PrintWriter out =
                new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));
            PrintWriter backup =
                new PrintWriter(new BufferedWriter(new FileWriter(backupFile)));

            // write the defect info to the temporary output files
            if (defects != null)
                for (int i = 0;   i < defects.length;   i++)
                    if (defects[i] != null) {
                        out.println(defects[i].toString());
                        backup.println(defects[i].toString());
                    }

            // close the temporary output files
            out.close();
            backup.close();

            // rename out to the real datafile
            defectFile.delete();
            tempFile.renameTo(defectFile);

            // delete the backup
            backupFile.delete();
        } catch (IOException e) { System.out.println("IOException: " + e); };
    }

    private Defect[] getDefects(BufferedReader in, int count) throws IOException
    {
        String one_defect = in.readLine();
        if (one_defect == null)
            return new Defect[count];

        Defect [] results = getDefects(in, count+1);
        try {
            results[count] = new Defect(one_defect);
        } catch (ParseException pe) {
            System.out.println("When reading defect log " + defectLogFilename +
                               "for project/task " + dataPrefix +
                               ", error parsing defect: " + one_defect);
            results[count] = null;
        }
        return results;
    }

    private int findDefect(Defect defects[], String defectNumber) {
        if (defectNumber == null)
            return -1;

        for (int i = defects.length;  i-- > 0; )
            if (defects[i] != null && defectNumber.equals(defects[i].number))
                return i;

        return -1;
    }

    public Defect[] readDefects() {
        Defect [] results = null;
        try {
            BufferedReader in =
                new BufferedReader(new FileReader(defectLogFilename));
            results = getDefects(in, 0);
            in.close();
        } catch (FileNotFoundException f) {
            System.out.println("FileNotFoundException: " + f);
        } catch (IOException i) {
            System.out.println("IOException: " + i);
        }
        return results;
    }

    private void incrementDataValue(String dataName, int increment) {
        dataName = DataRepository.createDataName(dataPrefix, dataName);
        DoubleData val;
        try {
            val = (DoubleData)data.getValue(dataName);
        } catch (ClassCastException cce) {
            return;          // Do nothing - don't overwrite values of other types
        }
        if (val == null)
            val = new DoubleData(increment);
        else if (val instanceof NumberFunction)
            return;          // Do nothing - don't overwrite old-style calculations
        else
            val = new DoubleData(val.getInteger() + increment);
        val.setEditable(false);
        data.putValue(dataName, val);
    }


    /** Recalculate some defect data.  This action is appropriate when
     * one defect has been changed or added.
     * @param defects is an array of the previous defect log entries.
     * @d is the new or changed defect.<UL>
     * <LI>It will be considered changed if its "number" field matches
     *     the number field of some member of the defects array.
     * <LI>If its number is null or the string "NEW", it is considered
     *     a new defect, and it is assigned a unique number.
     * <LI>If its number is the string "DELETE", it is considered a
     *     deleted defect.
     * <LI>Otherwise, it is considered to be a new defect whose number
     *     has already been assigned.
     *</UL>*/
    private void updateData(Defect defects[], Defect d) {
        String old_phase_injected, new_phase_injected;
        String old_phase_removed, new_phase_removed;

        old_phase_injected = old_phase_removed =
            new_phase_injected = new_phase_removed = null;

            // deleted defect
        if ("DELETE".equals(d.number)) {
            old_phase_injected = d.phase_injected;
            old_phase_removed = d.phase_removed;

            // new defect
        } else if (d.number == null || "NEW".equals(d.number)) {
            new_phase_injected = d.phase_injected;
            new_phase_removed  = d.phase_removed;

                                      // assign the defect a unique number
            int maxNum = defects.length;
            try {
                for (int i = defects.length;  i-- > 0; )
                    if (defects[i] != null) {
                        maxNum = Integer.parseInt(defects[i].number) + 1;
                        break;
                    }
            } catch (Exception e) { }
            d.number = Integer.toString(maxNum == 0 ? 1 : maxNum);

            // changed defect, or new defect with number already assigned
        } else {
            new_phase_injected = d.phase_injected;
            new_phase_removed = d.phase_removed;

            int pos = findDefect(defects, d.number);
            if (pos != -1) {
                old_phase_injected = defects[pos].phase_injected;
                old_phase_removed = defects[pos].phase_removed;
            }
        }

        if (old_phase_injected != null &&
            !old_phase_injected.equals(new_phase_injected))
            incrementDataValue(old_phase_injected + DEF_INJ_SUFFIX, -1);

        if (new_phase_injected != null &&
            !new_phase_injected.equals(old_phase_injected))
            incrementDataValue(new_phase_injected + DEF_INJ_SUFFIX, 1);

        if (old_phase_removed != null &&
            !old_phase_removed.equals(new_phase_removed))
            incrementDataValue(old_phase_removed + DEF_REM_SUFFIX, -1);

        if (new_phase_removed != null &&
            !new_phase_removed.equals(old_phase_removed))
            incrementDataValue(new_phase_removed + DEF_REM_SUFFIX, 1);
    }

    /** Recalculate ALL the data associated with this defect log.
     * This action is appropriate, for example, if massive changes
     * have been made to the defect log entries.
     * @param defects - an array of the new defects in the log.
     */
    private void updateData(Defect defects[]) {

        class PhaseCounter extends Hashtable {
            public PhaseCounter() {};
            public void increment(String var) {
                Integer i = (Integer)get(var);
                if (i == null)
                    i = new Integer(1);
                else
                    i = new Integer(1 + i.intValue());
                put(var, i);
            }
            public int extractValue(String var) {
                Integer i = (Integer)remove(var);
                return (i == null ? 0 : i.intValue());
            }
        }

        PhaseCounter phaseData = new PhaseCounter();

        for (int i = defects.length;   i-- > 0; ) {
            if (defects[i] == null) continue;
            phaseData.increment(defects[i].phase_injected + DEF_INJ_SUFFIX);
            phaseData.increment(defects[i].phase_removed + DEF_REM_SUFFIX);
        }

        Iterator dataNames = data.getKeys();
        String name, subname;
        int prefixLength = dataPrefix.length();
        DoubleData val;

        while (dataNames.hasNext()) {
            name = (String) dataNames.next();
            if (name.startsWith(dataPrefix)) {
                subname = name.substring(prefixLength);
                if (subname.endsWith(DEF_INJ_SUFFIX) ||
                    subname.endsWith(DEF_REM_SUFFIX)) {

                    Object o = data.getValue(name);
                    if (!(o instanceof DoubleData) || o instanceof NumberFunction)
                                // Don't overwrite calculations, which are
                        continue; // typically summing up values from other places

                    val = new DoubleData(phaseData.extractValue(subname));
                    val.setEditable(false);
                    data.putValue(name, val);
                }
            }
        }

        dataNames = phaseData.keySet().iterator();
        while (dataNames.hasNext()) {
            subname = (String) dataNames.next();
            name = DataRepository.createDataName(dataPrefix, subname);
            val = new DoubleData(phaseData.extractValue(subname));
            val.setEditable(false);
            data.putValue(name, val);
        }
    }

    public void performInternalRename(String oldPrefix, String newPrefix) {
        Defect defects[] = readDefects();
        int oldPrefixLen = oldPrefix.length();
        Defect d;

        for (int i = defects.length;  i-- > 0; ) {
            d = defects[i];
            if (phaseMatches(d.phase_injected, oldPrefix))
                d.phase_injected= newPrefix + d.phase_injected.substring(oldPrefixLen);
            if (phaseMatches(d.phase_removed, oldPrefix))
                d.phase_removed = newPrefix + d.phase_removed.substring(oldPrefixLen);
        }

        save(defects);
    }

    private boolean phaseMatches(String phase, String prefix) {
        if (phase == null) return false;
        return (phase.equals(prefix) || phase.startsWith(prefix + "/"));
    }

    public String getDefectLogFilename() {
        return defectLogFilename;
    }

    public String getDataPrefix() {
        return dataPrefix;
    }

}
