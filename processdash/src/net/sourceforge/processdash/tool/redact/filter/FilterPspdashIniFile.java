// Copyright (C) 2012 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.redact.LocationMapper;
import net.sourceforge.processdash.util.StringUtils;

public class FilterPspdashIniFile extends AbstractLineBasedFilter {

    public FilterPspdashIniFile() {
        setFilenamePatterns("pspdash.ini", ".pspdash");
    }

    public String getString(String line) {
        if (line == null)
            return null;

        if (isSetting(line, "backup.extraDirectories", "defectImport.",
            "export.data", "export.instructions", "import.directories",
            "navigator.", "pauseButton.historyList", "templates.directory",
            "window.maxWidth", "window.title"))
            return null;

        if (isSetting(line, "import.instructions"))
            return filterImportInstruction(line);

        return line;
    }

    private boolean isSetting(String line, String... settings) {
        for (String s : settings)
            if (line.startsWith(s))
                return true;
        return false;
    }

    private String filterImportInstruction(String line) {
        String[] instructions = line.split("<");
        for (int i = 0; i < instructions.length; i++) {
            String instr = instructions[i];
            instr = replaceXmlAttr(instr, "directory",
                LocationMapper.FILE_MAPPER);
            instr = replaceXmlAttr(instr, "url", LocationMapper.URL_MAPPER);
            instructions[i] = instr;
        }
        line = StringUtils.join(Arrays.asList(instructions), "<");
        return line;
    }

}
