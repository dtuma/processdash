// Copyright (C) 2021-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;

public class FileBundleSpec {

    public String bundleName;

    public ReadableResourceCollection source;

    public FileBundleRetentionGranularity retentionGranularity;

    public List<String> filenames;

    public List<FileBundleID> parents;

    public long timestamp;

    public FileBundleSpec(String bundleName, ReadableResourceCollection src,
            FileBundleRetentionGranularity retentionGranularity) {
        this.bundleName = bundleName;
        this.source = src;
        this.retentionGranularity = retentionGranularity;
        this.filenames = new ArrayList<String>();
        this.parents = Collections.EMPTY_LIST;
        this.timestamp = -1;
    }

}
