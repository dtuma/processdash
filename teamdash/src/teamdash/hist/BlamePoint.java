// Copyright (C) 2015-2022 Tuma Solutions, LLC
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
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import net.sourceforge.processdash.util.StringUtils;

public class BlamePoint implements Comparable<BlamePoint> {

    public static final BlamePoint INITIAL = new BlamePoint(new Date(0), "");

    private Date timestamp;

    private String author;

    private Set<String> authors;

    public BlamePoint(Date timestamp, String author) {
        this.timestamp = timestamp;
        this.author = author;
    }

    public BlamePoint(Date timestamp, Set<String> authors) {
        this.timestamp = timestamp;
        this.authors = authors;
        this.author = StringUtils.join(authors, ", ");
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public Set<String> getAuthors() {
        return (authors != null ? authors : Collections.singleton(author));
    }

    public void addAuthorsTo(Set<String> dest) {
        if (authors != null)
            dest.addAll(authors);
        else
            dest.add(author);
    }

    @Override
    public int compareTo(BlamePoint that) {
        return this.timestamp.compareTo(that.timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BlamePoint) {
            BlamePoint that = (BlamePoint) obj;
            return this.timestamp.equals(that.timestamp);
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
