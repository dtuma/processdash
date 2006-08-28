// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


package net.sourceforge.processdash.process;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.net.http.WebServer;


public class ScriptNameResolver implements ScriptID.NameResolver {

    private static final Logger logger = Logger
            .getLogger(ScriptNameResolver.class.getName());

    WebServer web;

    private static Hashtable displayNameCache = new Hashtable();

    public ScriptNameResolver(WebServer w) { web = w; }

    public String lookup(String scriptFile) {
        int hashPos = scriptFile.indexOf('#');
        if (hashPos != -1)
            scriptFile = scriptFile.substring(0, hashPos);

        String result = (String) displayNameCache.get(scriptFile);
        if (result != null)
            return (result.length() == 0 ? null : result);

        try {
            String text = web.getRequestAsString("/"+scriptFile);

            int beg = text.indexOf("<TITLE>");
            if (beg == -1) beg = text.indexOf("<title>");
            if (beg == -1) return null;
            beg += 7;

            int end = text.indexOf("</TITLE>", beg);
            if (end == -1) end = text.indexOf("</title>", beg);
            if (end == -1) return null;

            result = text.substring(beg, end).trim();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve name for '"
                    + scriptFile + "'", e);
        }

        displayNameCache.put(scriptFile, (result == null ? "" : result));
        return result;
    }

    public static void precacheName(String scriptFile, String name) {
        displayNameCache.put(scriptFile, name);
    }
}
