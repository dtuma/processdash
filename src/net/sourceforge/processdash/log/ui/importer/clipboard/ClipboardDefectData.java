// Copyright (C) 2010-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.clipboard;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import org.w3c.dom.Element;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.DefectDataBag;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorTokens;
import net.sourceforge.processdash.ui.lib.binding.ErrorValue;
import net.sourceforge.processdash.util.FormatUtil;

public class ClipboardDefectData implements ClipboardDataIDs {

    private static final String DEFECT_DATA_ID = "defects";

    private static final String DEFECTID_PREFIX_ID = "id-prefix";

    private static final String FIX_TIME_UNITS_ID = "fix-time-units";

    private static final String EXTRA_DESCRIPTION_ID = "extra-description-columns";

    private static final String[] ATTR_SRC = {
            // the first several elements in this array are carefully
            // positioned to align with the defect attribute integer constants
            // in the DefectDataBag class and its associated DefectDataBag.ATTRS
            // array. In this array, they record the BoundMap keys that store
            // the user's clipboard column mapping for each defect attribute.
            null, null, "id-column", "type-column", "phase-inj-column",
            "phase-rem-column", "fix-time-column", "fix-defect-column",
            "description-column",
            "date-column",

            // the final elements in this array indicate other attributes that
            // we also need to listen to.
            DEFECTID_PREFIX_ID, FIX_TIME_UNITS_ID, EXTRA_DESCRIPTION_ID,
            RAW_DATA, HAS_HEADER, };

    static Resources resources = Resources
            .getDashBundle("Defects.ImportForm.Clipboard");


    private BoundMap map;

    private Timer recalcTimer;

    public ClipboardDefectData(BoundMap map, Element xml) {
        this.map = map;

        this.recalcTimer = new Timer(50, EventHandler.create(
            ActionListener.class, this, "recalc"));
        this.recalcTimer.setRepeats(false);

        map.addPropertyChangeListener(ATTR_SRC, this, "needsRecalc");

        storeTimeUnitChoices();
    }

    /**
     * The fix time unit selector needs a list of choices to display in its
     * combo box selector. This method writes that list of items into the map.
     */
    private void storeTimeUnitChoices() {
        List choices = new ArrayList();

        Map m = new HashMap();
        m.put("VALUE", MINUTES);
        m.put("DISPLAY", resources.getString("fix-time-units.Minutes"));
        choices.add(m);

        m = new HashMap();
        m.put("VALUE", HOURS);
        m.put("DISPLAY", resources.getString("fix-time-units.Hours"));
        choices.add(m);

        map.put("fix-time-units-choices", choices);
    }

    public void needsRecalc() {
        recalcTimer.restart();
    }

    public void recalc() {
        map.put(DEFECT_DATA_ID, calculateValue());
    }

    private Object calculateValue() {
        Object error = map.getErrorDataForAttr(RAW_DATA);
        if (error != null)
            return error;

        List<List<String>> data = (List<List<String>>) map.get(RAW_DATA);
        return new DefectConverter().convertDefects(data);
    }

    private class DefectConverter {

        /** true if the clipboard data has a header row */
        boolean hasHeaderRow;

        /**
         * An array mapping defect attribute types (from the DefectDataBag
         * constants) into column positions within the clipboard data.
         */
        int[] columnPositions;

        /** a list of columns whose data should be appended to the description */
        List<TabularDataColumn> extraDescriptionColumns;

        /** The string prefix that the user wants appended to each defect ID */
        String idPrefix;

        /** The current date/time */
        Date now;

        /** The user's choice for fix time units (minutes vs hours) */
        Object fixTimeUnits;

        NumberFormat numberFmt;


        public Object convertDefects(List<List<String>> rawDefectData) {
            // Load data into our internal structures that we will need
            // during the converstion process.
            hasHeaderRow = (map.get(HAS_HEADER) == Boolean.TRUE);
            columnPositions = loadColumnPositions();
            extraDescriptionColumns = (List) map.get(EXTRA_DESCRIPTION_ID);
            idPrefix = cleanup((String) map.get(DEFECTID_PREFIX_ID));
            now = new Date();
            fixTimeUnits = map.get(FIX_TIME_UNITS_ID);
            numberFmt = NumberFormat.getNumberInstance();

            // Iterate over the raw data and build a list of defects.
            List result = new ArrayList();
            int i = (hasHeaderRow ? 1 : 0);
            for (; i < rawDefectData.size(); i++) {
                List<String> rawDefect = rawDefectData.get(i);
                Map m = convertDefect(rawDefect);
                if (m != null)
                    result.add(m);
            }

            // Return the defects we found, or an error if none were present.
            if (result.isEmpty())
                return new ErrorValue(map.getResource("No_Mappings_Selected"),
                        ErrorTokens.MISSING_DATA_SEVERITY);
            else
                return result;
        }

        private int[] loadColumnPositions() {
            // for each type of Defect attribute (taken from the constants in
            // the DefectDataBag class)
            int[] result = new int[DefectDataBag.ATTRS.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = -1;

                // for each defect attribute, find the ID (in the bound map)
                // that holds the user's decision about which clipboard
                // column should be mapped to this attribute.
                String id = ATTR_SRC[i];
                if (id == null)
                    continue;

                // Look at the user's selected clipboard column, and read the
                // column position within the clipboard data.
                Object colObj = map.get(id);
                if (colObj instanceof TabularDataColumn) {
                    TabularDataColumn tdc = (TabularDataColumn) colObj;
                    result[i] = tdc.getPos();
                }
            }
            return result;
        }


        private boolean dataWasSuccessfullyExtractedForTheCurrentDefect;

        public Map convertDefect(List<String> rawDefect) {
            Map defect = new HashMap();

            dataWasSuccessfullyExtractedForTheCurrentDefect = false;

            // Read data for each defect attribute, and add it to the result

            String id = cleanup(extract(rawDefect, DefectDataBag.ID));
            if (hasValue(id) && hasValue(idPrefix))
                id = idPrefix + id;
            store(defect, DefectDataBag.ID, id);

            String type = cleanup(extract(rawDefect, DefectDataBag.TYPE));
            store(defect, DefectDataBag.TYPE, type);

            String phase = extract(rawDefect, DefectDataBag.INJECTED);
            store(defect, DefectDataBag.INJECTED, phase);

            phase = extract(rawDefect, DefectDataBag.REMOVED);
            store(defect, DefectDataBag.REMOVED, phase);

            String fixId = cleanup(extract(rawDefect, DefectDataBag.FIX_DEFECT));
            if (hasValue(fixId) && hasValue(idPrefix))
                fixId = idPrefix + fixId;
            store(defect, DefectDataBag.FIX_DEFECT, fixId);

            String fixTime = extract(rawDefect, DefectDataBag.FIX_TIME);
            fixTime = parseFixTime(fixTime);
            store(defect, DefectDataBag.FIX_TIME, fixTime);

            String description = buildDescription(rawDefect);
            store(defect, DefectDataBag.DESCRIPTION, description);

            String dateStr = extract(rawDefect, DefectDataBag.DATE);
            Date date = parseDate(dateStr);
            store(defect, DefectDataBag.DATE, date);

            // if we actually found any data to copy into our result, return
            // the defect we've built. If no data was found (e.g. for an empty
            // row on the clipboard), return null.
            if (dataWasSuccessfullyExtractedForTheCurrentDefect)
                return defect;
            else
                return null;
        }

        /**
         * Extract data from an appropriate clipboard column for a defect
         * attribute
         * 
         * @param rawDefect
         *            the data elements for each column from this row of
         *            clipboard data
         * @param which
         *            a {@link DefectDataBag} attribute constant indicating
         *            which defect attribute we are interested in
         * @return the value from the clipboard row that maps to that attribute
         */
        private String extract(List<String> rawDefect, int which) {
            int pos = columnPositions[which];
            return extractFromPos(rawDefect, pos);
        }

        /**
         * Extract data from a specific clipboard column
         * 
         * @param rawDefect
         *            the data elements for each column from this row of
         *            clipboard data
         * @param pos
         *            the column position to extract data for
         * @return the value in that column, or null if the column has no value.
         */
        private String extractFromPos(List<String> rawDefect, int pos) {
            if (pos < 0 || pos >= rawDefect.size())
                return null;

            String result = rawDefect.get(pos);
            if (result == null)
                return null;
            result = result.trim();
            if (result.length() == 0)
                return null;

            dataWasSuccessfullyExtractedForTheCurrentDefect = true;
            return result;
        }

        /**
         * Write a defect attribute into a defect object, for later use by the
         * {@link DefectDataBag} class.
         * 
         * @param defect
         *            the defect object
         * @param which
         *            a {@link DefectDataBag} attribute constant
         * @param value
         *            the value to store
         */
        private void store(Map defect, int which, Object value) {
            if (value != null) {
                String attrName = DefectDataBag.ATTRS[which];
                defect.put(attrName, value);
            }
        }

        /**
         * Interpret a string as a fix time, apply the user's choice of time
         * units, and return the value that should be stored into the defect.
         */
        private String parseFixTime(String value) {
            if (!hasValue(value))
                return null;
            try {
                double time = numberFmt.parse(value).doubleValue();
                if (HOURS.equals(fixTimeUnits))
                    time *= 60;
                return numberFmt.format(time);
            } catch (Exception e) {
            }
            return null;
        }

        private String buildDescription(List<String> rawDefect) {
            StringBuilder result = new StringBuilder();

            // retrieve the primary description and use it to initialize our
            // result
            String mainDescription = extract(rawDefect,
                DefectDataBag.DESCRIPTION);
            if (mainDescription != null)
                result.append(mainDescription);

            // if the user has asked to append other columns to the defect
            // description, look up each of those values and append it.
            if (extraDescriptionColumns != null) {
                boolean firstValue = true;
                for (TabularDataColumn edc : extraDescriptionColumns) {
                    String value = extractFromPos(rawDefect, edc.getPos());
                    if (hasValue(value)) {
                        // add an extra line break before the extra items
                        if (firstValue) {
                            result.append("\n");
                            firstValue = false;
                        }
                        // put each extra item on a new row.
                        result.append("\n");
                        // if we have a header row, use it to get the name of
                        // this extra item, and append that as a label
                        if (hasHeaderRow)
                            result.append(edc.getName()).append(": ");
                        // finally, append the extra item to the description.
                        result.append(value);
                    }
                }
            }

            return result.toString();
        }

        private Date parseDate(String value) {
            if (value == null)
                return now;

            Date result = parseDateImpl(value);
            return (result == null ? now : result);
        }

        private Date parseDateImpl(String value) {
            try {
                return FormatUtil.parseDate(value.trim());
            } catch (Exception e) {
            }
            try {
                return FormatUtil.parseDateTime(value.trim());
            } catch (Exception e) {
            }
            return null;
        }
    }

    private String cleanup(String s) {
        if (s == null)
            return null;
        return s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
    }

    private boolean hasValue(String s) {
        return (s != null && s.trim().length() > 0);
    }

    private static final String MINUTES = "minutes";

    private static final String HOURS = "hours";

}
