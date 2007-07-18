// Copyright (C) 1998-2007 Tuma Solutions, LLC
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


package net.sourceforge.processdash.log.defects;

import java.util.*;
import java.text.*;

import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class Defect implements Cloneable {

    public static final String UNSPECIFIED = "Unspecified";

    public Date date;
    public String number, defect_type, phase_injected, phase_removed,
                  fix_time, fix_defect, description;

    public Defect() {}

    public Defect(String s) throws ParseException {
        if (s == null) throw new ParseException("Null pointer passed in", 0);
        StringTokenizer tok = new StringTokenizer(s.replace('\u0001','\n'), "\t");
        try {
            number = tok.nextToken();
            defect_type = tok.nextToken();
            phase_injected = tok.nextToken();
            phase_removed = tok.nextToken();
            fix_time = tok.nextToken();
            fix_defect = tok.nextToken();
            description = tok.nextToken();
            date = FormatUtil.parseDate(tok.nextToken());
        } catch (NoSuchElementException e) {
            System.out.println("NoSuchElementException: " + e);
            throw new ParseException("Poor defect formatting", 0);
        }
    }

    private String token(String s, boolean multiline) {
        if (s == null || s.length() == 0)
            return " ";
        s = StringUtils.canonicalizeNewlines(s);
        s = s.replace('\t', ' ');
        s = s.replace('\n', (multiline ? '\u0001' : ' '));
        return s;
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

    public boolean equals(Object obj) {
        if (obj instanceof Defect) {
            Defect that = (Defect) obj;
            return eq(this.date, that.date)
                        && eq(this.number, that.number)
                        && eq(this.defect_type, that.defect_type)
                        && eq(this.phase_injected, that.phase_injected)
                        && eq(this.phase_removed, that.phase_removed)
                        && eq(this.fix_time, that.fix_time)
                        && eq(this.fix_defect, that.fix_defect)
                        && eq(this.description, that.description);
        }
        return false;
    }

    private boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (!hasValue(a)) return !hasValue(b);
        return a.equals(b);
    }

    private boolean hasValue(Object obj) {
        return obj != null && !"".equals(obj) && !" ".equals(obj)
                  && !UNSPECIFIED.equals(obj);
    }

    public int hashCode() {
        int result = hc(this.date);
        result = (result << 1) ^ hc(this.number);
        result = (result << 1) ^ hc(this.defect_type);
        result = (result << 1) ^ hc(this.phase_injected);
        result = (result << 1) ^ hc(this.phase_removed);
        result = (result << 1) ^ hc(this.fix_time);
        result = (result << 1) ^ hc(this.fix_defect);
        result = (result << 1) ^ hc(this.description);
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
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // can't happen?
            return null;
        }
    }

}
