// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher;

public class LaunchableDataset {

    public enum Type {
        Team, Personal
    }

    private String name;

    private Type type;

    private String location;

    private String detailsUrl;

    private String owner;

    public LaunchableDataset(String name, Type type, String location,
            String detailsUrl, String owner) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.detailsUrl = detailsUrl;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isTeam() {
        return Type.Team.equals(type);
    }

    public boolean isPersonal() {
        return Type.Personal.equals(type);
    }

    public String getLocation() {
        return location;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("LaunchableDataset[");
        result.append("name='").append(name);
        result.append("', type='").append(type);
        result.append("', location='").append(location);
        if (detailsUrl != null)
            result.append("', details='").append(detailsUrl);
        result.append("', owner='").append(owner);
        result.append("']");
        return result.toString();
    }

}
