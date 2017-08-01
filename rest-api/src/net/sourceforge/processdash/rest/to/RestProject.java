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

public class RestProject extends JsonMap implements Comparable<RestProject> {

    private JsonDate creationDate;

    public RestProject(String id, String name, String fullName) {
        super("id", id, "name", name, "fullName", fullName);
        set("creationDate", creationDate = new JsonDate(
                Long.parseLong(id, Character.MAX_RADIX)));
    }

    public String getId() {
        return getAttr();
    }

    public String getName() {
        return getAttr();
    }

    public String getFullName() {
        return getAttr();
    }

    public JsonDate getCreationDate() {
        return creationDate;
    }

    public int compareTo(RestProject that) {
        return -this.creationDate.compareTo(that.creationDate);
    }

}
