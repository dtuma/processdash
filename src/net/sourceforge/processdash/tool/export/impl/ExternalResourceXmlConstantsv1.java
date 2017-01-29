// Copyright (C) 2007-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

public interface ExternalResourceXmlConstantsv1 extends XmlConstants {

    public String ARCHIVE_PATH = "externalResources";

    public String MANIFEST_FILE_NAME = "externalResources.xml";

    public String DOC_ROOT_ELEM = "externalResources";

    public String DATASET_URL = "datasetUrl";

    public String IMPORT_DIR_ELEM = "importedDirectory";

    public String ORIGINAL_PATH = "origPath";

    public String ORIGINAL_URL = "origURL";

    public String NEW_PATH = "newPath";

    public String MCF_ELEM = "mcfMetadata";

    public String MCF_ID = "mcfID";

    public String MCF_VERSION = "mcfVersion";

    public String MCF_SUBDIR = "mcf";

}
