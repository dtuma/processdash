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

package net.sourceforge.processdash.util;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;

public class FlexibleDateFormat extends SimpleDateFormat {

    // NOTE: For full applet support, this class needs to run in JRE1.1

    private Vector formatList;
    private Vector timeOnlyList;

    public FlexibleDateFormat(String formats) {
        formatList = new Vector();
        timeOnlyList = new Vector();
        if (formats != null)
            addFormats(formats);
        addDefaultFormats();
    }

    public void addFormats(String formats) {
        StringTokenizer tok = new StringTokenizer(formats, "|");
        while (tok.hasMoreTokens())
            addFormat(tok.nextToken());
    }

    public void addFormat(String format) {
        if (formatList.size() == 0)
            applyPattern(format);
        formatList.addElement(new SimpleDateFormat(format));
    }

    private void addDefaultFormats() {
        int[] FMT = { DateFormat.MEDIUM, DateFormat.SHORT,
                      DateFormat.LONG, DateFormat.FULL };

        // add all date instances
        for (int i = 0; i < FMT.length; i++)
            formatList.addElement(DateFormat.getDateInstance(FMT[i]));

        // add all date/time instances
        for (int i = 0; i < FMT.length; i++)
            for (int j = 0; i < FMT.length; i++)
                formatList.addElement(DateFormat.getDateTimeInstance(FMT[i], FMT[j]));

        // add all time instances
        for (int i = 0; i < FMT.length; i++)
            timeOnlyList.addElement(DateFormat.getTimeInstance(FMT[i]));
    }


    public void setLenient(boolean lenient) {
        super.setLenient(lenient);
        Enumeration e = formatList.elements();
        while (e.hasMoreElements()) {
            DateFormat fmt = (DateFormat) e.nextElement();
            fmt.setLenient(lenient);
        }
    }


    public Date parse(String text, ParsePosition pos) {
        int startPos = pos.getIndex();
        Date result = super.parse(text, pos);
        if (result != null) return result;

        Enumeration e = formatList.elements();
        while (e.hasMoreElements()) {
            DateFormat fmt = (DateFormat) e.nextElement();
            pos.setIndex(startPos);
            result = fmt.parse(text, pos);
            if (result != null) return result;
        }

        e = timeOnlyList.elements();
        while (e.hasMoreElements()) {
            DateFormat fmt = (DateFormat) e.nextElement();
            pos.setIndex(startPos);
            result = fmt.parse(text, pos);
            if (result != null) return fixupDate(result);
        }

        return null;
    }

    private Date fixupDate(Date time) {
        // find out the current date
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // merge the time passed in with the current date.
        c.setTime(time);
        c.set(year, month, day);
        return c.getTime();
    }

}
