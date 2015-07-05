// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlamePoint implements Comparable<BlamePoint> {

    public static final BlamePoint INITIAL = new BlamePoint(new Date(0), "");

    private Date timestamp;

    private String author;

    public BlamePoint(Date timestamp, String author) {
        this.timestamp = timestamp;
        this.author = author;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public int compareTo(BlamePoint that) {
        int result = this.timestamp.compareTo(that.timestamp);
        if (result == 0)
            result = this.author.compareTo(that.author);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BlamePoint) {
            BlamePoint that = (BlamePoint) obj;
            return this.author.equals(that.author)
                    && this.timestamp.equals(that.timestamp);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    @Override
    public String toString() {
        return author + " @ " + DATE_FMT.format(timestamp);
    }

    private static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

}
