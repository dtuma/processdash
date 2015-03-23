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

package net.sourceforge.processdash.tool.redact;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sourceforge.processdash.util.PatternList;

public class RedactFilterData {

    private ZipFile srcZip;

    private Set<String> filterIDs;

    private Map<Object, Object> helpers;

    public RedactFilterData(ZipFile srcZip, Set<String> filterIDs) {
        this.srcZip = srcZip;
        this.filterIDs = filterIDs;
        this.helpers = new HashMap();
    }

    public ZipFile getSrcZip() {
        return srcZip;
    }

    public BufferedReader getFile(String name) throws IOException {
        return getFile(srcZip.getEntry(name));
    }

    public BufferedReader getFile(ZipEntry e) throws IOException {
        if (e == null)
            return null;
        else
            return new BufferedReader(new InputStreamReader(
                    srcZip.getInputStream(e), "UTF-8"));
    }

    public InputStream getStream(String name) throws IOException {
        return getStream(srcZip.getEntry(name));
    }

    public InputStream getStream(ZipEntry e) throws IOException {
        if (e == null)
            return null;
        else
            return new BufferedInputStream(srcZip.getInputStream(e));
    }

    public List<ZipEntry> getEntries(String... patterns) {
        PatternList p = new PatternList(patterns);
        List<ZipEntry> result = new ArrayList<ZipEntry>();
        Enumeration<? extends ZipEntry> entries = srcZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (p.matches(e.getName()))
                result.add(e);
        }
        return result;
    }


    public boolean isFiltering(String id) {
        return id != null && id.length() > 0 && filterIDs.contains(id);
    }

    public void putHelper(Object key, Object value) {
        helpers.put(key, value);
    }

    public void putHelper(Object helper) {
        Class clazz = helper.getClass();
        helpers.put(clazz, helper);
    }

    public Object getHelper(Object key) {
        return helpers.get(key);
    }

}
