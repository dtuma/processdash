// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import pspdash.data.DoubleData;
import pspdash.data.DataRepository;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;


public class DefectLog {
    String defectLogFilename = null;
    String dataPrefix = null;
    DataRepository data = null;
    private Defect defectsRead[];
    PSPDashboard parent;


    public static final String defectInjectedSuffix = "/Defects Injected";
    public static final String defectRemovedSuffix = "/Defects Removed";


    DefectLog(String filename, String dataPath, DataRepository data,
              PSPDashboard dash) {
        defectLogFilename = filename;
        this.dataPrefix = dataPath + "/";
        this.data = data;
        this.parent = dash;
    }

    // writeDefect saves the defect data to a temporary file.  After write is
    // complete, the temporary file is renamed to the actual defect file.
    public synchronized void writeDefect(Defect d) {
        Defect defects[] = readDefects();

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

            if (d.number != null) try {
                defects[Integer.parseInt(d.number) - 1] = d;
            } catch (NumberFormatException e) {
                d.number = null;
            }

            // write the defect info to the temporary output files
            int i = 0;
            if (defects != null)
                for (;   i < defects.length;   i++) {
                    out.println(defects[i].toString());
                    backup.println(defects[i].toString());
                }

            if (d.number != null) {
//      updateData(defects, null); //preferred, but doesn't work (yet) w/null
                updateData(defects);
                if (parent.configure_button.defect_frame != null)
                    parent.configure_button.defect_frame.updateDefectLog (this);
            } else {
                d.number = Integer.toString(i + 1);
                out.println(d.toString());
                backup.println(d.toString());
                updateData(defects, d);
                if (parent.configure_button.defect_frame != null)
                    parent.configure_button.defect_frame.updateDefectLog (this, d);
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

    private void getDefects(BufferedReader in, int count) throws IOException {
        String one_defect;
        Defect d;

        try {
            one_defect = in.readLine();
            d = new Defect(one_defect);
            getDefects(in, count+1);
            defectsRead[count] = d;
        } catch (Exception e) {
            defectsRead = new Defect[count];
        }
    }

    public Defect[] readDefects() {
        defectsRead = null;
        try {
            BufferedReader in =
                new BufferedReader(new FileReader(defectLogFilename));
            getDefects(in, 0);
            in.close();
        } catch (FileNotFoundException f) {
            System.out.println("FileNotFoundException: " + f);
        } catch (IOException i) {
            System.out.println("IOException: " + i);
        }
        return defectsRead;
    }

    private void incrementDataValue(String dataName, int increment) {
        dataName = dataPrefix + dataName;
        DoubleData val = (DoubleData)data.getValue(dataName);
        if (val == null)
            val = new DoubleData(increment);
        else
            val = new DoubleData(val.getInteger() + increment);
        val.setEditable(false);
        data.putValue(dataName, val);
    }


    /** Recalculate some defect data.  This action is appropriate when
     * one defect has been changed or added.
     * @param defects is an array of the previous defect log entries.
     * @d is the new or changed defect.  It will be considered changed if
     *    its "number" field matches the number field of some member of the
     *    defects array, new otherwise.
     */
    private void updateData(Defect defects[], Defect d) {
        String old_phase_injected, new_phase_injected;
        String old_phase_removed, new_phase_removed;
        DoubleData val;

        old_phase_injected = old_phase_removed = null;
        try {
            int number = Integer.parseInt(d.number);
            old_phase_injected = defects[number].phase_injected;
            old_phase_removed = defects[number].phase_removed;
        } catch (NumberFormatException e) {
        } catch (IndexOutOfBoundsException e) {}

        new_phase_injected = d.phase_injected;
        new_phase_removed = d.phase_removed;

        if (!new_phase_injected.equals(old_phase_injected)) {
            if (old_phase_injected != null)
                incrementDataValue(old_phase_injected + defectInjectedSuffix, -1);
            incrementDataValue(new_phase_injected + defectInjectedSuffix, 1);
        }

        if (!new_phase_removed.equals(old_phase_removed)) {
            if (old_phase_removed != null)
                incrementDataValue(old_phase_removed + defectRemovedSuffix, -1);
            incrementDataValue(new_phase_removed + defectRemovedSuffix, 1);
        }
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
            phaseData.increment(defects[i].phase_injected + defectInjectedSuffix);
            phaseData.increment(defects[i].phase_removed + defectRemovedSuffix);
        }

        Iterator dataNames = data.getKeys();
        String name, subname;
        int prefixLength = dataPrefix.length();
        DoubleData val;

        while (dataNames.hasNext()) {
            name = (String) dataNames.next();
            if (name.startsWith(dataPrefix)) {
                subname = name.substring(prefixLength);
                if (subname.endsWith(defectInjectedSuffix) ||
                    subname.endsWith(defectRemovedSuffix)) {
                    val = new DoubleData(phaseData.extractValue(subname));
                    val.setEditable(false);
                    data.putValue(name, val);
                }
            }
        }

        dataNames = phaseData.keySet().iterator();
        while (dataNames.hasNext()) {
            subname = (String) dataNames.next();
            name = dataPrefix + subname;
            val = new DoubleData(phaseData.extractValue(subname));
            val.setEditable(false);
            data.putValue(name, val);
        }
    }

}
