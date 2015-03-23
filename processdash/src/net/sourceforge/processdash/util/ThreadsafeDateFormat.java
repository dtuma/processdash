// Copyright (C) 2010-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ThreadsafeDateFormat {

    private DateFormat prototype;

    private ThreadLocal<DateFormat> delegates;

    public ThreadsafeDateFormat(String pattern) {
        this(pattern, false);
    }

    public ThreadsafeDateFormat(String pattern, boolean lenient) {
        this(new SimpleDateFormat(pattern));
        prototype.setLenient(lenient);
    }

    public ThreadsafeDateFormat(DateFormat prototype) {
        this.prototype = prototype;
        this.delegates = new ThreadLocal<DateFormat>();
    }

    public DateFormat getFormat() {
        DateFormat result = delegates.get();
        if (result == null) {
            result = (DateFormat) prototype.clone();
            delegates.set(result);
        }
        return result;
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo,
            FieldPosition fieldPosition) {
        return getFormat().format(date, toAppendTo, fieldPosition);
    }

    public final String format(Date date) {
        return getFormat().format(date);
    }

    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition fieldPosition) {
        return getFormat().format(obj, toAppendTo, fieldPosition);
    }

    public final String format(Object obj) {
        return getFormat().format(obj);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return getFormat().formatToCharacterIterator(obj);
    }

    public NumberFormat getNumberFormat() {
        return getFormat().getNumberFormat();
    }

    public TimeZone getTimeZone() {
        return getFormat().getTimeZone();
    }

    public Date parse(String source, ParsePosition pos) {
        return getFormat().parse(source, pos);
    }

    public Date parse(String source) throws ParseException {
        return getFormat().parse(source);
    }

    public Object parseObject(String source, ParsePosition pos) {
        return getFormat().parseObject(source, pos);
    }

    public Object parseObject(String source) throws ParseException {
        return getFormat().parseObject(source);
    }

    public void setTimeZone(TimeZone zone) {
        getFormat().setTimeZone(zone);
    }

}
