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

public class DataFileEntry {

    private String filename;

    private String key;

    private String value;

    private boolean modified;

    public DataFileEntry(String filename, String key, String value) {
        this.filename = filename;
        this.key = key;
        this.value = value;
        this.modified = false;
    }

    public String getFilename() {
        return filename;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        this.modified = true;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }

}
