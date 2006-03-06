// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.impl;

public interface ArchiveMetricsXmlConstants extends XmlConstants {

    public static final String MANIFEST_FILE_NAME = "manifest.xml";

    public static final String ARCHIVE_ELEM = "archive";

    public static final String TYPE_ATTR = "type";

    public static final String FILE_TYPE_ARCHIVE = "dashboardDataExport";

    public static final String FILE_TYPE_METRICS = "metrics";

    public static final String FILE_TYPE_DEFECTS = "defects";

    public static final String FILE_TYPE_EARNED_VALUE = "earnedValue";

    public static final String EXPORTED_TAG = "exported";

    public static final String OWNER_ATTR = "byOwner";

    public static final String WHEN_ATTR = "when";

    public static final String PACKAGE_ELEM = "withPackage";

    public static final String PACKAGE_ID_ATTR = "packageID";

    public static final String FILE_ELEM = "file";

    public static final String FILE_NAME_ATTR = "name";

    public static final String VERSION_ATTR = "version";

}
