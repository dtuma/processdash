// Copyright (C) 2026 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.PathConvert;

public class CheckClassCount extends PathConvert {

    private int expected = -1;

    private String property = null;

    public void setExpected(int expected) {
        this.expected = expected;
    }

    @Override
    public void setProperty(String p) {
        super.setProperty(p);
        this.property = p;
    }

    @Override
    public void execute() throws BuildException {
        // validate inputs
        if (property == null)
            throw new BuildException("'property' must be set");

        // hardcode the separator, then run PathConvert logic
        setPathSep(";");
        super.execute();

        // get the path that was built and split into a sorted list
        String path = getProject().getProperty(property);
        String[] items = path.split(";");
        Arrays.sort(items);

        // gather the count and list of classes that were found
        int classCount = 0;
        StringBuilder classList = new StringBuilder();
        for (String item : items) {
            // do not count the number of inner classes
            if (item.indexOf("$") == -1) {
                classCount++;
                classList.append("\n    ").append(item);
            }
        }

        // store the list/number of classes found, and a flag for overflow
        getProject().setProperty(property + ".list", classList.toString());
        getProject().setProperty(property + ".count",
            Integer.toString(classCount));
        if (expected > 0 && classCount > expected)
            getProject().setProperty(property + ".overflowing", "true");
    }

}
