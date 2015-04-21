// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyPathMapper;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.TaskListMapper;
import net.sourceforge.processdash.util.StringUtils;

@EnabledFor(RedactFilterIDs.TASK_NAMES)
public class FilterDataContainingTaskNames extends AbstractDataStringFilter {

    private HierarchyPathMapper pathNameMapper;

    private TaskListMapper taskListMapper;

    @EnabledFor({ "PROBE_LIST", "Subproject_Path_List" })
    public String scrambleProbeList(String value) {
        if (value == null || value.length() == 0)
            return value;

        List<String> list = StringData.create(value).asList().asList();
        ListData newVal = new ListData();
        for (String elem : list)
            newVal.add(pathNameMapper.getString(elem));
        return newVal.saveString().substring(1);
    }

    @EnabledFor("^Subproject_.*/Hierarchy_Path$")
    public String hashMasterSubprojectPath(String value) {
        return pathNameMapper.getString(value);
    }

    @EnabledFor("^Master_Project_Path$")
    public String hashMasterProjectPath(String value) {
        return pathNameMapper.getString(value);
    }

    @EnabledFor("^Subproject_.*/Short_Name$")
    public String hashMasterSubprojectShortName(String value) {
        return PersonMapper.hashInitials(value);
    }

    @EnabledFor("/Task Lists$")
    public String hashRollupTaskListSpec(String value)
            throws MalformedValueException {
        List<String> taskLists = StringData.create(value).asList().asList();
        ListData newVal = new ListData();
        for (String elem : taskLists) {
            elem = taskListMapper.hashTaskListName(elem);
            newVal.add(elem);
        }
        return newVal.saveString().substring(1);
    }

    @EnabledFor("^Project_Schedule_Name$")
    public String hashTeamProjectScheduleName(String taskListName) {
        return taskListMapper.hashTaskListName(taskListName);
    }

    @EnabledFor("EV_Task_Dependencies$")
    public String hashTaskDependencies(String xml) {
        String[] tags = xml.split("<");
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            tag = RedactFilterUtils.replaceXmlAttr(tag, "name", pathNameMapper);
            tag = RedactFilterUtils.replaceXmlAttr(tag, "taskList",
                taskListMapper);
            tags[i] = tag;
        }

        return StringUtils.join(Arrays.asList(tags), "<");
    }

    @EnabledFor(" To Date Subset Prefix$")
    public String hashToDateSubsetPrefix(String prefix) {
        if (prefix.startsWith("/To Date/"))
            return prefix;
        else
            return pathNameMapper.getString(prefix);
    }

    @EnabledFor("Project_Component_Info")
    public String hashTeamProjectComponentInfo(String xml) {
        String[] tags = xml.split("<");
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            tag = RedactFilterUtils.replaceXmlAttr(tag, "name", pathNameMapper);
            tags[i] = tag;
        }

        return StringUtils.join(Arrays.asList(tags), "<");
    }

}
