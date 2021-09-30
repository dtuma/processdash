// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FileBundleTimeFormat {

    private SimpleDateFormat format;

    public FileBundleTimeFormat(String timezone) {
        format = new SimpleDateFormat("yyyyMMdd-HHmmss");
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        format.setLenient(false);
    }

    public String format() {
        return format(System.currentTimeMillis());
    }

    public String format(long time) {
        return format(new Date(time));
    }

    public String format(Date time) {
        return format.format(time);
    }

    public Date parse(String timestamp) {
        try {
            return (Date) format.parse(timestamp);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Bad bundle timestamp '" + timestamp + "'");
        }
    }

}
