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

package net.sourceforge.processdash.process;

import static net.sourceforge.processdash.team.TeamDataConstants.PROJECT_ID;
import static net.sourceforge.processdash.team.TeamDataConstants.TEAM_DATA_DIRECTORY;
import static net.sourceforge.processdash.team.TeamDataConstants.TEAM_DATA_DIRECTORY_URL;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.util.XMLUtils;

public class WorkflowInfoFactory {

    public static WorkflowInfo get(File directory) {
        File workflowDump = new File(directory, "workflowDump.xml");

        try {
            if (workflowDump.isFile()) {
                BufferedInputStream in = new BufferedInputStream(
                        new FileInputStream(workflowDump));
                Element xml = XMLUtils.parse(in).getDocumentElement();
                return new WorkflowInfo(xml);
            }
        } catch (Exception e) {
        }

        return null;
    }

    public static WorkflowInfo get(String... locations) {
        ImportDirectory importDir = ImportDirectoryFactory.getInstance().get(
            locations);
        if (importDir == null)
            return null;
        else
            return get(importDir.getDirectory());
    }

    public static WorkflowInfo get(DataContext data) {
        String url = getStringData(data, TEAM_DATA_DIRECTORY_URL);
        String dir = getStringData(data, TEAM_DATA_DIRECTORY);
        return get(url, dir);
    }

    private static String getStringData(DataContext data, String elem) {
        SimpleData val = data.getSimpleValue(elem);
        return (val == null ? null : val.format());
    }

    public static WorkflowInfo get(DataRepository data, String path) {
        StringBuffer prefix = new StringBuffer(path);
        if (data.getInheritableValue(prefix, PROJECT_ID) == null)
            return null;
        else
            return get(data.getSubcontext(prefix.toString()));
    }

}
