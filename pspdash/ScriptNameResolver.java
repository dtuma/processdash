// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;


public class ScriptNameResolver implements ScriptID.NameResolver {

    TinyWebServer web;

    private static Hashtable displayNameCache = new Hashtable();

    public ScriptNameResolver(TinyWebServer w) { web = w; }

    public String lookup(String scriptFile) {
        String result = (String) displayNameCache.get(scriptFile);
        if (result != null)
            return (result.length() == 0 ? null : result);

    try {
        String text = new String(web.getRequest("/"+scriptFile, true));

        int beg = text.indexOf("<TITLE>");
        if (beg == -1) return null;
        beg += 7;

        int end = text.indexOf("</TITLE>", beg);
        if (end == -1) return null;

        result = text.substring(beg, end).trim();
    } catch (Exception e) {
        // Do nothing.
    }

    displayNameCache.put(scriptFile, (result == null ? "" : result));
    return result;
    }

    public static void precacheName(String scriptFile, String name) {
        displayNameCache.put(scriptFile, name);
    }
}
