// Copyright (C) 1999-2011 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.defects;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberFunction;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.export.impl.DefectXmlConstantsv1;
import net.sourceforge.processdash.tool.export.impl.XmlConstants;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RobustFileWriter;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;


public class DefectLog {

    public interface Listener extends EventListener {
        public void defectUpdated(DefectLog log, Defect d);
    }

    String defectLogFilename = null;
    String dataPrefix = null;
    String dataNamespace = null;
    DataRepository data = null;

    public static final String DEF_INJ_SUFFIX = "/Defects Injected";
    public static final String DEF_REM_SUFFIX = "/Defects Removed";

    /** A boolean setting indicating if the dataset is saved in UTF8 */
    public static final String USE_XML_SETTING = "dataset.usesXmlDefects";

    /** The version at which the dashboard started to support XML defect format */
    public static final String XML_SUPPORT_VERSION = "1.12";


    public DefectLog(String filename, String dataPath, DataRepository data) {
        this.defectLogFilename = filename;
        this.dataPrefix = dataPath + "/";
        this.dataNamespace = null;
        this.data = data;
    }

    /** Find the existing defect in the log with the given ID.
     * 
     * @param id the ID number of the defect to find.
     * @return a matching defect, or null if none was found.
     */
    public synchronized Defect getDefect(String id) {
        Defect defectsRead[] = readDefects();
        int pos = findDefect(defectsRead, id);
        if (pos == -1)
            return null;
        else
            return defectsRead[pos];
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

        fireDefectChanged(d);
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

            fireDefectChanged(d);
        }
    }

    /** Save the defect data.
     *
     * The data is first written to a temporary file.  Upon successful
     * completion, the temporary file is renamed to the actual defect
     * file.
     */
    private void save(Defect [] defects) {
        if (Settings.isReadOnly())
            return;
        else if (Settings.getBool(USE_XML_SETTING, false))
            saveAsXML(defects);
        else {
            if (saveAsTabDelimited(defects) == false) {
                enableXmlStorageFormat();
                saveAsXML(defects);
            }
        }
    }

    private void saveAsXML(Defect [] defects) {
        try {
            RobustFileOutputStream out = new RobustFileOutputStream(
                      defectLogFilename);
            if (defects != null && defects.length > 0) {
                XmlSerializer ser = XMLUtils.getXmlSerializer(true);
                ser.setOutput(out, XmlConstants.ENCODING);
                ser.startDocument(XmlConstants.ENCODING, null);
                ser.startTag(null, "defectLog");
                for (int i = 0; i < defects.length; i++)
                    if (defects[i] != null)
                        defects[i].toXml(ser);
                ser.endTag(null, "defectLog");
                ser.endDocument();
            }
            out.close();

        } catch (IOException e) { System.out.println("IOException: " + e); };
    }

    private boolean saveAsTabDelimited(Defect [] defects) {
        boolean savedSuccessfully = true;
        try {
            File defectFile = new File(defectLogFilename);
            Writer out = new BufferedWriter(new RobustFileWriter(defectFile));

            // write the defect info
            String newLine = System.getProperty("line.separator");
            if (defects != null)
                for (int i = 0;   i < defects.length;   i++)
                    if (defects[i] != null) {
                        if (defects[i].needsXmlSaveFormat())
                            savedSuccessfully = false;
                        out.write(defects[i].toString());
                        out.write(newLine);
                    }

            out.close();
        } catch (IOException e) { System.out.println("IOException: " + e); };
        return savedSuccessfully;
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
        PushbackInputStream pin = null;
        try {
            pin = new PushbackInputStream(
                        new FileInputStream(defectLogFilename));
            int c = pin.read();
            if (c == -1) { // empty file
                ;
            } else if (c == '<') { // xml format
                pin.unread(c);
                results = readDefectsFromXml(pin);
            } else { // tab-delimited format
                pin.unread(c);
                results = readTabDelimitedDefects(pin);
            }
        } catch (FileNotFoundException f) {
            System.out.println("FileNotFoundException: " + f);
        } catch (SAXException i) {
            System.out.println("Invalid XML defect data in file "
                          + defectLogFilename + ": " + XMLUtils.exceptionMessage(i));
        } catch (IOException i) {
            System.out.println("IOException: " + i);
        }

        FileUtils.safelyClose(pin);
        if (results == null)
            results = new Defect[0];
        return results;
    }

    private Defect[] readDefectsFromXml(InputStream in) throws SAXException,
              IOException {
        Element doc = XMLUtils.parse(in).getDocumentElement();
        NodeList nl = doc.getElementsByTagName(DefectXmlConstantsv1.DEFECT_TAG);
        Defect[] result = new Defect[nl.getLength()];
        for (int i = 0;  i < result.length;  i++)
            result[i] = new Defect((Element) nl.item(i));
        return result;
    }

    private Defect[] readTabDelimitedDefects(InputStream in) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(in));
        return getDefects(buf, 0);
    }

    private void incrementDataValue(String dataName, int increment) {
        modifyDataValue(dataName, increment, false);
    }
    private void setDataValue(String dataName, int value) {
        modifyDataValue(dataName, value, true);
    }
    private void modifyDataValue(String dataName, int increment,
            boolean ignoreExistingValue) {
        String prefix = dataPrefix + getDataNamespace();
        dataName = DataRepository.createDataName(prefix, dataName);
        DoubleData val;
        try {
            val = (DoubleData)data.getValue(dataName);
        } catch (ClassCastException cce) {
            return;          // Do nothing - don't overwrite values of other types
        }
        if (val instanceof NumberFunction) {
            return;          // Do nothing - don't overwrite old-style calculations
        } else if (ignoreExistingValue && increment == 0) {
            if (val != null && val.getDouble() != 0)
                data.restoreDefaultValue(dataName);
            return;
        } else if (val == null || ignoreExistingValue) {
            val = new DoubleData(increment);
        } else {
            val = new DoubleData(val.getInteger() + increment);
        }
        val.setEditable(false);
        data.putValue(dataName, val);
    }

    private String getDataNamespace() {
        if (dataNamespace == null) {
            String dataName = DataRepository.createDataName(dataPrefix,
                    "Defect_Data_Namespace");
            SimpleData sd = data.getSimpleValue(dataName);
            if (sd == null)
                dataNamespace = "";
            else
                dataNamespace = sd.format() + "/";
        }
        return dataNamespace;
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
        int old_fix_count, new_fix_count;

        old_phase_injected = old_phase_removed =
            new_phase_injected = new_phase_removed = null;
        old_fix_count = new_fix_count = 0;

            // deleted defect
        if ("DELETE".equals(d.number)) {
            old_phase_injected = d.phase_injected;
            old_phase_removed = d.phase_removed;
            old_fix_count = d.fix_count;

            // new defect
        } else if (d.number == null || "NEW".equals(d.number)) {
            new_phase_injected = d.phase_injected;
            new_phase_removed  = d.phase_removed;
            new_fix_count = d.fix_count;

                                      // assign the defect a unique number
            int maxNum = 0;
            for (int i = defects.length;  i-- > 0; )
                if (defects[i] != null)
                    try {
                        maxNum = Math.max(maxNum, Integer.parseInt(defects[i].number));
                    } catch (NumberFormatException nfe) {}
            d.number = Integer.toString(maxNum + 1);

            // changed defect, or new defect with number already assigned
        } else {
            new_phase_injected = d.phase_injected;
            new_phase_removed = d.phase_removed;
            new_fix_count = d.fix_count;

            int pos = findDefect(defects, d.number);
            if (pos != -1) {
                old_phase_injected = defects[pos].phase_injected;
                old_phase_removed = defects[pos].phase_removed;
                old_fix_count = defects[pos].fix_count;
            }
        }

        int fix_count_delta = new_fix_count - old_fix_count;

        if (old_phase_injected != null &&
            !old_phase_injected.equals(new_phase_injected))
            incrementDataValue(old_phase_injected + DEF_INJ_SUFFIX, -old_fix_count);

        if (new_phase_injected != null &&
            !new_phase_injected.equals(old_phase_injected))
            incrementDataValue(new_phase_injected + DEF_INJ_SUFFIX, new_fix_count);

        if (fix_count_delta != 0 && old_phase_injected != null &&
            old_phase_injected.equals(new_phase_injected))
            incrementDataValue(old_phase_injected + DEF_INJ_SUFFIX, fix_count_delta);

        if (old_phase_removed != null &&
            !old_phase_removed.equals(new_phase_removed))
            incrementDataValue(old_phase_removed + DEF_REM_SUFFIX, -old_fix_count);

        if (new_phase_removed != null &&
            !new_phase_removed.equals(old_phase_removed))
            incrementDataValue(new_phase_removed + DEF_REM_SUFFIX, new_fix_count);

        if (fix_count_delta != 0 && old_phase_removed != null &&
            old_phase_removed.equals(new_phase_removed))
            incrementDataValue(old_phase_removed + DEF_REM_SUFFIX, fix_count_delta);
    }

    /* * Recalculate ALL the data associated with this defect log.
        * This action is appropriate, for example, if massive changes
        * have been made to the defect log entries.
        * @param defects - an array of the new defects in the log.
        */
    void recalculateData(Defect defects[], DashboardContext ctx) {

        class PhaseCounter extends HashMap<String, Integer> {
            public PhaseCounter() {};
            public void increment(String var, int count) {
                Integer i = get(var);
                if (i == null)
                    i = new Integer(count);
                else
                    i = new Integer(count + i.intValue());
                put(var, i);
            }
            public void storeDataValue(String var) {
                Integer i = remove(var);
                setDataValue(var, (i == null ? 0 : i.intValue()));
            }
        }

        PhaseCounter phaseData = new PhaseCounter();

        for (int i = defects.length;   i-- > 0; ) {
            Defect d = defects[i];
            if (d == null) continue;
            phaseData.increment(d.phase_injected + DEF_INJ_SUFFIX, d.fix_count);
            phaseData.increment(d.phase_removed + DEF_REM_SUFFIX, d.fix_count);
        }

        // Iterate over all of the known phases for this defect log and store the
        // associated data elements.
        List defectPhases = DefectUtil.getDefectPhases(dataPrefix, ctx);
        for (Object phaseName : defectPhases) {
            phaseData.storeDataValue(phaseName + DEF_INJ_SUFFIX);
            phaseData.storeDataValue(phaseName + DEF_REM_SUFFIX);
        }
        phaseData.storeDataValue("Before Development" + DEF_INJ_SUFFIX);
        phaseData.storeDataValue("After Development" + DEF_REM_SUFFIX);

        // If data is still present in the phaseData map, it means that some
        // of the defects are using nonstandard phases.  Store the nonstandard
        // counts as well.
        if (!phaseData.isEmpty()) {
            for (String var : new ArrayList<String>(phaseData.keySet()))
                phaseData.storeDataValue(var);
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

    private static List listeners = new LinkedList();

    public static void addDefectLogListener(Listener l) {
        listeners.add(l);
    }
    public static void removeDefectLogListener(Listener l) {
        listeners.remove(l);
    }
    private void fireDefectChanged(Defect d) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener l = (Listener) i.next();
            l.defectUpdated(this, d);
        }
    }

    public static void convertFileToXml(File f) {
        if (f.length() > 0) {
            DefectLog log = new DefectLog(f.getAbsolutePath(), null, null);
            Defect[] defects = log.readDefects();
            log.saveAsXML(defects);
        }
    }
    public static void enableXmlStorageFormat() {
        InternalSettings.set(USE_XML_SETTING, "true");
        DataVersionChecker.registerDataRequirement("pspdash", XML_SUPPORT_VERSION);
    }
}
