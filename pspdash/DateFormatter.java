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

//
// This package formats and parses Dates per the dateFormat & dateTimeFormat
// entries in the Settings.  The first entry in each list becomes the default.
//
package pspdash;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;


public class DateFormatter {

    static protected String delim = "|";
    static protected Date   d;
    static protected Vector dateOnlyFormats = new Vector ();
    static protected Vector dateTimeFormats = new Vector ();

    static protected SimpleDateFormat defaultFormat =
        (SimpleDateFormat)DateFormat.getDateInstance (DateFormat.LONG);

    static protected SimpleDateFormat sdf = new SimpleDateFormat ();


    public static String formatDateTime (Date d) {
        if (dateTimeFormats.size() == 0)
            return defaultFormat.format(d);
        else {
            sdf.applyPattern ((String)dateTimeFormats.elementAt (0));
            return sdf.format(d);
        }
    }

    public static String formatDate (Date d) {
        if (d == null)
            return "";
        if (dateOnlyFormats.size() == 0)
            return defaultFormat.format(d);
        else {
            sdf.applyPattern ((String)dateOnlyFormats.elementAt (0));
            return sdf.format(d);
        }
    }

    public static Date parseDateTime (String s) {
        if (dateTimeFormats.size() == 0)
            try {
                return defaultFormat.parse (s);
            } catch (Exception e) {
                System.err.println("Error parsing default:"+e);
                return null;
            }
        for (int i = 0; i < dateTimeFormats.size(); i++)
            try {
                sdf.applyPattern ((String)dateTimeFormats.elementAt (i));
                return sdf.parse (s);
            } catch (Exception e) {}
        for (int i = 0; i < dateOnlyFormats.size(); i++)
            try {
                sdf.applyPattern ((String)dateOnlyFormats.elementAt (i));
                return sdf.parse (s);
            } catch (Exception e) {}
        return null;
    }

    public static Date parseDate (String s) {
        if (dateOnlyFormats.size() == 0)
            try {
                return defaultFormat.parse (s);
            } catch (Exception e) {
                System.err.println("Error parsing default:"+e);
                return null;
            }
        for (int i = 0; i < dateOnlyFormats.size(); i++)
            try {
                sdf.applyPattern ((String)dateOnlyFormats.elementAt (i));
                return sdf.parse (s);
            } catch (Exception e) {}
        return null;
    }

    static {
        StringTokenizer st;
        String DOString = Settings.getVal("dateFormat");
        st = new StringTokenizer (DOString, delim, false);
        while (st.hasMoreElements())
            dateOnlyFormats.addElement (st.nextToken ());
        String DTString = Settings.getVal("dateTimeFormat");
        st = new StringTokenizer (DTString, delim, false);
        while (st.hasMoreElements())
            dateTimeFormats.addElement (st.nextToken ());
        sdf.setTimeZone(TimeZone.getDefault());
        defaultFormat.setTimeZone(TimeZone.getDefault());
    }

}
