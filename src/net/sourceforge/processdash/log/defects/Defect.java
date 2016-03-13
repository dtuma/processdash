// Copyright (C) 1998-2016 Tuma Solutions, LLC
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

import java.util.*;
import java.io.IOException;
import java.text.*;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.export.impl.DefectXmlConstantsv1;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class Defect implements Cloneable {

    public static final String UNSPECIFIED = "Unspecified";
    public static final DefectPhase UNSPECIFIED_PHASE = new DefectPhase(
            UNSPECIFIED);

    public Date date;
    public String number, defect_type, phase_injected, phase_removed,
                  fix_time, fix_defect, description;
    public DefectPhase injected, removed;
    public int fix_count = 1;
    public boolean fix_pending;
    public Map<String, String> extra_attrs;

    public Defect() {}

    public Defect(String s) throws ParseException {
        if (s == null) throw new ParseException("Null pointer passed in", 0);
        StringTokenizer tok = new StringTokenizer(s.replace('\u0001','\n'), "\t");
        try {
            number = tok.nextToken();
            defect_type = tok.nextToken();
            phase_injected = tok.nextToken();
            injected = new DefectPhase(phase_injected);
            phase_removed = tok.nextToken();
            removed = new DefectPhase(phase_removed);
            fix_time = tok.nextToken();
            fix_defect = tok.nextToken();
            description = tok.nextToken();
            date = FormatUtil.parseDate(tok.nextToken());
        } catch (NoSuchElementException e) {
            System.out.println("NoSuchElementException: " + e);
            throw new ParseException("Poor defect formatting", 0);
        }
    }

    public Defect(Element xml) {
        extra_attrs = XMLUtils.getAttributesAsMap(xml);
        number = extractXmlAttr(XML.NUM_ATTR);
        defect_type = extractXmlAttr(XML.DEFECT_TYPE_ATTR);
        phase_injected = extractXmlAttr(XML.INJECTED_ATTR);
        injected = extractXmlPhaseAttrs(XML.INJECTED_ATTR, phase_injected);
        phase_removed = extractXmlAttr(XML.REMOVED_ATTR);
        removed = extractXmlPhaseAttrs(XML.REMOVED_ATTR, phase_removed);
        fix_time = extractXmlAttr(XML.FIX_TIME_ATTR);
        fix_defect = extractXmlAttr(XML.FIX_DEFECT_ATTR);
        description = extractXmlAttr(XML.DESCRIPTION_ATTR);
        fix_count = Math.max(0, extractXmlIntAttr(XML.FIX_COUNT_ATTR, 1));
        fix_pending = "true".equals(extractXmlAttr(XML.FIX_PENDING_ATTR));
        date = XMLUtils.parseDate(extractXmlAttr(XML.DATE_ATTR));
    }

    private String extractXmlAttr(String name) {
        String result = extra_attrs.remove(name);
        return (result == null ? "" : result);
    }

    private int extractXmlIntAttr(String name, int defaultVal) {
        try {
            String value = extractXmlAttr(name);
            if (hasValue(value))
                return Integer.parseInt(value);
        } catch (Exception e) {}
        return defaultVal;
    }

    private DefectPhase extractXmlPhaseAttrs(String prefix, String legacyPhase) {
        DefectPhase result = new DefectPhase(legacyPhase);

        String id = extractXmlAttr(prefix + XML.ID_ATTR_SUFFIX);
        if (XMLUtils.hasValue(id)) {
            result.phaseID = id;

            String processName = "";
            String phaseName = extractXmlAttr(prefix + XML.NAME_ATTR_SUFFIX);
            int slashPos = phaseName.indexOf('/');
            if (slashPos != -1) {
                processName = phaseName.substring(0, slashPos);
                phaseName = phaseName.substring(slashPos + 1);
            }
            result.processName = processName;
            result.phaseName = phaseName;
        }

        return result;
    }

    private String token(String s, boolean multiline) {
        if (s == null || s.length() == 0)
            return " ";
        s = StringUtils.canonicalizeNewlines(s);
        s = s.replace('\t', ' ');
        s = s.replace('\n', (multiline ? '\u0001' : ' '));
        return s;
    }

    public float getFixTime() {
        try {
            return getFixTimeOrErr();
        } catch (ParseException e) {
            return 0;
        }
    }

    public float getFixTimeOrErr() throws ParseException {
        if (fixTimeIsEmpty())
            return 0;

        String timeStr = this.fix_time.trim();
        int commaPos = timeStr.lastIndexOf(',');
        if (commaPos == -1) {
            // no comma implies plain java numeric format, eg 1234.5
            try {
                return Float.parseFloat(timeStr);
            } catch (NumberFormatException nfe) {
                throw new ParseException("Unparseable fix time: " + timeStr, 0);
            }

        } else if (commaPos + 2 == timeStr.length()) {
            // European decimal format, eg "1.234,5"
            return COMMA_FIX_TIME_FMT.parse(timeStr).floatValue();

        } else {
            // US decimal format, eg "1,234.5"
            return DECIMAL_FIX_TIME_FMT.parse(timeStr).floatValue();
        }
    }

    public String getLocalizedFixTime() {
        try {
            if (fixTimeIsEmpty())
                return "";
            else
                return LOCAL_FIX_TIME_FMT.format(getFixTimeOrErr());
        } catch (ParseException pe) {
            return fix_time;
        }
    }

    private String getFixTimeXmlStr() {
        try {
            if (fixTimeIsEmpty())
                return "";
            else
                return Float.toString(getFixTimeOrErr());
        } catch (ParseException pe) {
            return fix_time;
        }
    }

    private boolean fixTimeIsEmpty() {
        return fix_time == null || fix_time.trim().length() == 0;
    }

    private static final NumberFormat DECIMAL_FIX_TIME_FMT = NumberFormat
        .getNumberInstance(Locale.US);
    private static final NumberFormat COMMA_FIX_TIME_FMT = NumberFormat
        .getNumberInstance(Locale.FRANCE);
    private static final NumberFormat LOCAL_FIX_TIME_FMT = NumberFormat
        .getNumberInstance();
    static {
        LOCAL_FIX_TIME_FMT.setMaximumFractionDigits(1);
    }

    public boolean needsXmlSaveFormat() {
        return fix_count != 1 || fix_pending //
                || (injected != null && injected.phaseID != null) //
                || (removed != null && removed.phaseID != null);
    }

    public String toString() {
        String tab = "\t";
        String dateStr = "";
        if (date != null) dateStr = XMLUtils.saveDate(date);
        return (token(number, false) + tab +
                token(defect_type, false) + tab +
                token(phase_injected, false) + tab +
                token(phase_removed, false) + tab +
                token(fix_time, false) + tab +
                token(fix_defect, false) + tab +
                token(description, true) + tab +
                token(dateStr, false) + tab);
    }

    public void toXml(XmlSerializer ser) throws IOException {
        ser.startTag(null, XML.DEFECT_TAG);
        ser.attribute(null, XML.NUM_ATTR, xmlToken(number));
        ser.attribute(null, XML.DEFECT_TYPE_ATTR, xmlToken(defect_type));
        ser.attribute(null, XML.INJECTED_ATTR, xmlToken(phase_injected));
        writePhaseAttrs(ser, XML.INJECTED_ATTR, injected);
        ser.attribute(null, XML.REMOVED_ATTR, xmlToken(phase_removed));
        writePhaseAttrs(ser, XML.REMOVED_ATTR, removed);
        ser.attribute(null, XML.FIX_TIME_ATTR, xmlToken(getFixTimeXmlStr()));
        ser.attribute(null, XML.FIX_DEFECT_ATTR, xmlToken(fix_defect));
        ser.attribute(null, XML.DESCRIPTION_ATTR, xmlToken(description));
        if (fix_count != 1)
            ser.attribute(null, XML.FIX_COUNT_ATTR, Integer.toString(fix_count));
        if (fix_pending)
            ser.attribute(null, XML.FIX_PENDING_ATTR, "true");
        if (date != null)
            ser.attribute(null, XML.DATE_ATTR, XMLUtils.saveDate(date));
        if (extra_attrs != null && !extra_attrs.isEmpty())
            for (Map.Entry<String, String> attr : extra_attrs.entrySet())
                ser.attribute(null, attr.getKey(), attr.getValue());
        ser.endTag(null, XML.DEFECT_TAG);
    }

    public void writePhaseAttrs(XmlSerializer ser, String prefix,
            DefectPhase phase) throws IOException {
        if (phase != null && phase.phaseID != null) {
            ser.attribute(null, prefix + XML.ID_ATTR_SUFFIX, phase.phaseID);
            ser.attribute(null, prefix + XML.NAME_ATTR_SUFFIX,
                phase.processName + "/" + phase.phaseName);
        }
    }

    private String xmlToken(String s) {
        if (s == null || " ".equals(s))
            return "";
        else
            return StringUtils.canonicalizeNewlines(s);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Defect) {
            Defect that = (Defect) obj;
            return eq(this.date, that.date)
                        && eq(this.number, that.number)
                        && eq(this.defect_type, that.defect_type)
                        && eq(this.phase_injected, that.phase_injected)
                        && eq(this.injected, that.injected)
                        && eq(this.phase_removed, that.phase_removed)
                        && eq(this.removed, that.removed)
                        && eq(this.fix_time, that.fix_time)
                        && eq(this.fix_defect, that.fix_defect)
                        && eq(this.description, that.description)
                        && (this.fix_count == that.fix_count)
                        && (this.fix_pending == that.fix_pending)
                        && eq(this.extra_attrs, that.extra_attrs);
        }
        return false;
    }

    private boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (!hasValue(a)) return !hasValue(b);
        return a.equals(b);
    }

    private boolean hasValue(Object obj) {
        if (obj == null)
            return false;
        if ("".equals(obj) || " ".equals(obj) || UNSPECIFIED.equals(obj))
            return false;
        if (obj instanceof Collection && ((Collection) obj).isEmpty())
            return false;
        return true;
    }

    public int hashCode() {
        int result = hc(this.date);
        result = (result << 1) ^ hc(this.number);
        result = (result << 1) ^ hc(this.defect_type);
        result = (result << 1) ^ hc(this.phase_injected);
        result = (result << 1) ^ hc(this.injected);
        result = (result << 1) ^ hc(this.phase_removed);
        result = (result << 1) ^ hc(this.removed);
        result = (result << 1) ^ hc(this.fix_time);
        result = (result << 1) ^ hc(this.fix_defect);
        result = (result << 1) ^ hc(this.description);
        result = (result << 1) ^ hc(this.fix_count);
        result = (result << 1) ^ hc(this.fix_pending);
        result = (result << 1) ^ hc(this.extra_attrs);
        return result;
    }

    private int hc(Object a) {
        if (hasValue(a))
            return a.hashCode();
        else
            return 0;
    }

    public Object clone() {
        try {
            Defect result = (Defect) super.clone();
            if (result.extra_attrs != null)
                result.extra_attrs = new HashMap(result.extra_attrs);
            return result;
        } catch (CloneNotSupportedException e) {
            // can't happen?
            return null;
        }
    }

    private interface XML extends DefectXmlConstantsv1 {}

}
