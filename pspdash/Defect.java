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

import java.util.*;
import java.text.*;

public class Defect {
    public Date date;
    public String number, defect_type, phase_injected, phase_removed,
                  fix_time, fix_defect, description, dateStr;

    Defect() {}

    Defect(String s) throws ParseException {
        if (s == null) throw new ParseException("Null pointer passed in", 0);
        StringTokenizer tok = new StringTokenizer(s.replace('','\n'), "\t");
        try {
            number = tok.nextToken();
            defect_type = tok.nextToken();
            phase_injected = tok.nextToken();
            phase_removed = tok.nextToken();
            fix_time = tok.nextToken();
            fix_defect = tok.nextToken();
            description = tok.nextToken();
            date = DateFormatter.parseDate(dateStr = tok.nextToken());
        } catch (NoSuchElementException e) {
            System.out.println("NoSuchElementException: " + e);
            throw new ParseException("Poor defect formatting", 0);
        }
    }

    private String token(String s, boolean multiline) {
        if      (s == null) return " ";
        else if (s.length() == 0) return " ";
        else if (multiline) return s.replace('\t', ' ').replace('\n','');
        else return s.replace('\t', ' ').replace('\n',' ');
    }

    public String toString() {
        String tab = "\t";
        if (date != null) dateStr = DateFormatter.formatDate(date);
        return (token(number, false) + tab +
                token(defect_type, false) + tab +
                token(phase_injected, false) + tab +
                token(phase_removed, false) + tab +
                token(fix_time, false) + tab +
                token(fix_defect, false) + tab +
                token(description, true) + tab +
                token(dateStr, false) + tab);
    }

}
