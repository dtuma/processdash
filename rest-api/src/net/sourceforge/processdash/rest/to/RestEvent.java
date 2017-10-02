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

import java.util.concurrent.atomic.AtomicLong;

public class RestEvent extends JsonMap implements Comparable<RestEvent> {

    private static AtomicLong NEXT_ID = new AtomicLong(
            System.currentTimeMillis() / 10000);

    public RestEvent(String type) {
        super("id", NEXT_ID.getAndIncrement(), "type", type);
    }

    public String getType() {
        return getAttr();
    }

    public Long getId() {
        return getAttr();
    }

    public int compareTo(RestEvent that) {
        return this.getId().compareTo(that.getId());
    }

}
