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

import java.util.Date;

public class RestTask extends JsonMap {

    public RestTask() {}

    public RestTask(String id, String fullName, RestProject project) {
        super("id", id, "fullName", fullName, "project", project);
    }

    public RestTask(String id, String fullName, RestProject project,
            Date completionDate) {
        this(id, fullName, project);
        set("completionDate", completionDate);
    }

    public RestTask(String id, String fullName, RestProject project,
            Date completionDate, Double estimatedTime, Double actualTime) {
        this(id, fullName, project, completionDate);
        set("estimatedTime", estimatedTime);
        set("actualTime", actualTime);
    }

    public String getId() {
        return getAttr();
    }

    public String getFullName() {
        return getAttr();
    }

    public RestProject getProject() {
        return getAttr();
    }

    public JsonDate getCompletionDate() {
        return getAttr();
    }

    public void setCompletionDate(Date completionDate) {
        setAttr(completionDate);
    }

    public Double getEstimatedTime() {
        return getAttr();
    }

    public void setEstimatedTime(Double estimatedTime) {
        setAttr(estimatedTime);
    }

    public Double getActualTime() {
        return getAttr();
    }

    public void setActualTime(Double actualTime) {
        setAttr(actualTime);
    }

    public String getFullPath() {
        return getProject().getFullName() + "/" + getFullName();
    }

}
