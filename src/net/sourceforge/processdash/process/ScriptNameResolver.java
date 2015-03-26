// Copyright (C) 2001-2009 Tuma Solutions, LLC
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


package net.sourceforge.processdash.process;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;


public class ScriptNameResolver implements ScriptID.NameResolver {

    private static final Logger logger = Logger
            .getLogger(ScriptNameResolver.class.getName());

    WebServer web;

    private static Hashtable displayNameCache = new Hashtable();

    private static class LookupFailure {
        long failureTimestamp = System.currentTimeMillis();
        public boolean isRecent() {
            long age = System.currentTimeMillis() - failureTimestamp;
            return (age < LOOKUP_RETRY_PERIOD);
        }
    }
    private static final long LOOKUP_RETRY_PERIOD =
        30 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;

    public ScriptNameResolver(WebServer w) { web = w; }

    public String lookup(String scriptFile, boolean quickly) {
        int hashPos = scriptFile.indexOf('#');
        if (hashPos != -1)
            scriptFile = scriptFile.substring(0, hashPos);

        Object cacheItem = displayNameCache.get(scriptFile);
        if (cacheItem instanceof String) {
            return (String) cacheItem;
        } else if (cacheItem instanceof LookupFailure) {
            if (((LookupFailure) cacheItem).isRecent())
                return "";
        }

        if (quickly && scriptFile.startsWith("http"))
            return null;

        String result;
        try {
            String text = getTextContents(scriptFile);
            cacheItem = result = extractTitle(text);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve name for '"
                    + scriptFile + "'", e);
            result = "";
            cacheItem = new LookupFailure();
        }

        displayNameCache.put(scriptFile, cacheItem);
        return result;
    }

    private String getTextContents(String scriptFile) throws IOException {
        if (scriptFile.startsWith("http")) {
            return HTTPUtils.getResponseAsString(new URL(scriptFile)
                    .openConnection());
        } else {
            return web.getRequestAsString("/" + scriptFile);
        }
    }

    private String extractTitle(String html) {
        int beg = html.indexOf("<TITLE>");
        if (beg == -1) beg = html.indexOf("<title>");
        if (beg == -1) return "";
        beg += 7;

        int end = html.indexOf("</TITLE>", beg);
        if (end == -1) end = html.indexOf("</title>", beg);
        if (end == -1) return "";

        return HTMLUtils.unescapeEntities(html.substring(beg, end).trim());
    }

    public static void precacheName(String scriptFile, String name) {
        displayNameCache.put(scriptFile, name);
    }
}
