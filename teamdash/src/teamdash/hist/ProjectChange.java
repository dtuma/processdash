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
import java.util.Date;

import net.sourceforge.processdash.util.FastDateFormat;

public abstract class ProjectChange {

    private String author;

    private Date timestamp;

    private boolean lastChangeFlag;

    protected ProjectChange(String author, Date timestamp) {
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isLastChangeFlag() {
        return lastChangeFlag;
    }

    public void setLastChangeFlag(boolean lastChange) {
        this.lastChangeFlag = lastChange;
    }

    public String getDisplayDate() {
        return DATE_FMT.format(timestamp);
    }

    public String getDisplayTime() {
        return TIME_FMT.format(timestamp);
    }

    public Date getFollupTimestamp() {
        return (lastChangeFlag ? null : timestamp);
    }

    public abstract String getDescription();

    static FastDateFormat DATE_FMT = FastDateFormat
            .getDateInstance(DateFormat.FULL);

    private static FastDateFormat TIME_FMT = FastDateFormat
            .getTimeInstance(DateFormat.SHORT);

}
