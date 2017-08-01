// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.to;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONAware;

import net.sourceforge.processdash.rest.rs.HttpException;

public class JsonDate extends Date implements JSONAware {

    public JsonDate(long time) {
        super(time);
    }

    public JsonDate(Date date) {
        super(date.getTime());
    }

    public String toJSONString() {
        return "\"" + toString() + "\"";
    }

    @Override
    public String toString() {
        synchronized (DATE_FORMATS) {
            return DATE_FORMATS[0].format(this);
        }
    }


    public static JsonDate valueOf(String s) {
        if (s == null || s.equals("null") || s.trim().length() == 0)
            return null;

        synchronized (DATE_FORMATS) {
            for (DateFormat f : DATE_FORMATS) {
                ParsePosition pos = new ParsePosition(0);
                Date result = f.parse(s, pos);
                if (pos.getIndex() == s.length())
                    return new JsonDate(result);
            }
        }

        throw HttpException.badRequest();
    }


    private static final DateFormat[] DATE_FORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
            new SimpleDateFormat("yyyy-MM-dd") };

    static {
        DATE_FORMATS[1].setTimeZone(TimeZone.getTimeZone("GMT"));
    }

}
