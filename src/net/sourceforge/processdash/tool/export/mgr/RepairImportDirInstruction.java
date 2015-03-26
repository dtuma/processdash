// Copyright (C) 2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;

import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.StringUtils;

/**
 * Information about the Team Data Directory is stored in two locations in the
 * Process Dashboard:
 * <ul>
 * <li>In data repository values for the project itself (editable via the Team
 * Project Parameters and Settings page)</li>
 * <li>In the list of automatic imports</li>
 * </ul>
 * 
 * Most people are unaware of the list of automatic imports. As a result, a team
 * may edit the value on the parameters and settings page and neglect to update
 * the automatic import instruction. This class can repair the import
 * instruction, bringing it into agreement with the project settings.
 * 
 * @author Tuma
 */
public class RepairImportDirInstruction {

    private String projectID;

    private String prefix;

    private String[] locations;

    private String url;

    private String dirPath;

    public RepairImportDirInstruction(String projectID, String prefix,
            String[] locations) {
        this.projectID = projectID;
        this.prefix = prefix;
        this.locations = locations;
    }


    public void run() {
        if (!StringUtils.hasValue(projectID))
            return;

        setupLocations();

        if (dirPath == null && url == null)
            return;

        ImportDirectoryInstruction currentInstr = findMatchingInstruction();

        ImportDirectoryInstruction newInstr = new ImportDirectoryInstruction();
        newInstr.setDirectory(dirPath);
        newInstr.setURL(url);
        newInstr.setPrefix(prefix);

        if (currentInstr == null)
            ImportManager.getInstance().addInstruction(newInstr);
        else if (!instructionIsUpToDate(currentInstr))
            ImportManager.getInstance().changeInstruction(currentInstr,
                newInstr);
    }

    private void setupLocations() {
        String fallbackDirPath = null;
        for (String location : locations) {
            if (!StringUtils.hasValue(location))
                continue;

            if (TeamServerSelector.isUrlFormat(location)) {
                this.url = location;
            } else if (new File(location).isDirectory()) {
                this.dirPath = location;
            } else if (fallbackDirPath == null) {
                fallbackDirPath = location;
            }
        }

        if (this.dirPath == null)
            dirPath = fallbackDirPath;
    }

    private ImportDirectoryInstruction findMatchingInstruction() {
        ImportManager mgr = ImportManager.getInstance();
        for (int i = 0; i < mgr.getInstructionCount(); i++) {
            AbstractInstruction instr = mgr.getInstruction(i);
            if (instr instanceof ImportDirectoryInstruction) {
                ImportDirectoryInstruction importInstr = (ImportDirectoryInstruction) instr;
                if (instructionMatches(importInstr))
                    return importInstr;
            }
        }
        return null;
    }

    private boolean instructionMatches(ImportDirectoryInstruction instr) {
        return (contains(instr.getDirectory(), projectID)
                || contains(instr.getPrefix(), projectID)
                || contains(instr.getURL(), projectID));
    }

    private boolean contains(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.toLowerCase().contains(b.toLowerCase());
    }

    private boolean instructionIsUpToDate(ImportDirectoryInstruction instr) {
        if (StringUtils.hasValue(url) && !url.equals(instr.getURL()))
            return false;
        if (dirPath == null)
            return true;
        for (String location : locations) {
            if (StringUtils.hasValue(location)
                    && location.equals(instr.getDirectory()))
                return true;
        }
        return false;
    }

}
