// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import net.sourceforge.processdash.ui.DashboardIconFactory;

public class AddTaskDefaultHandler implements AddTaskHandler {

    private String templateID;

    public void setConfigElement(Element e, String attrName) {
        templateID = e.getAttribute("templateID");
    }

    public List<AddTaskTypeOption> getTaskTypes(String targetParent,
            String activeTask) {
        AddTaskTypeOption task = new AddTaskTypeOption();
        task.icon = DashboardIconFactory.getTaskIcon();
        task.templateID = templateID;
        return Collections.singletonList(task);
    }

    public void finalizeAddedTask(String newTaskPath, AddTaskTypeOption type) {}

}
