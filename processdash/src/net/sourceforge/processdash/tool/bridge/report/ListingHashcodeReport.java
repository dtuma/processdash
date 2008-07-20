// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;


/**
 * A simple report which prints the listing hashcode for a collection
 * 
 * @see ListingHashcodeCalculator
 */
public class ListingHashcodeReport implements CollectionReport {

    public static final ListingHashcodeReport INSTANCE = new ListingHashcodeReport();

    public String getContentType() {
        return "text/plain; charset=UTF-8";
    }

    public void runReport(ResourceCollection c, List<String> resources,
            OutputStream out) throws IOException {
        long hashcode = ListingHashcodeCalculator.getListingHashcode(c,
            resources);

        Writer w = new OutputStreamWriter(out, "UTF-8");
        w.write(Long.toString(hashcode));
        w.flush();
    }

}
