// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class NameSource {

    private Map<Integer, String> seedNames;

    private Map<String, String> assignedNames;

    public NameSource(String property) {
        // check for a system property naming a seed file. Abort if not set
        String filename = System.getProperty(property);
        if (filename == null)
            return;

        // read names from the given file
        seedNames = new TreeMap<Integer, String>();
        assignedNames = new HashMap<String, String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null) {
                int eqPos = line.indexOf('=');
                if (eqPos == -1)
                    // if the line has no equals sign, it represents a "seed"
                    // name that can be pseudorandomly assigned
                    seedNames.put(line.hashCode(), line);
                else
                    // if the line contains an equals sign, it represents an
                    // explicit name assignment
                    assignedNames.put(line.substring(0, eqPos).toLowerCase(),
                        line.substring(eqPos + 1));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            seedNames = null;
        }
    }

    public void addAssignment(String input, String name) {
        if (assignedNames != null && input != null && name != null)
            assignedNames.put(input.toLowerCase(), name);
    }

    public String getName(String input, String defaultValue) {
        // if this object was not initialized with a seed file, abort
        if (seedNames == null)
            return defaultValue;

        // check to see if a name has already been assigned for this value
        String lower = input.toLowerCase();
        String assigned = assignedNames.get(lower);
        if (assigned != null)
            return assigned;

        // scan the seed values for one whose hashcode is closest to the
        // hashcode of the input value
        String result = defaultValue;
        int hash = lower.hashCode();
        Iterator<Entry<Integer, String>> i = seedNames.entrySet().iterator();
        while (i.hasNext()) {
            Entry<Integer, String> next = i.next();
            result = next.getValue();
            if (next.getKey() > hash)
                break;
        }
        if (result != defaultValue) {
            // record this name assignment
            assignedNames.put(lower, result);
            // remove the name from the seed list, so it is not used again
            i.remove();
        }

        // return the value we found
        return result;
    }

}
