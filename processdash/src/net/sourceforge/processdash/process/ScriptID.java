// Copyright (C) 2000-2007 Tuma Solutions, LLC
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


import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.HTMLUtils;


public class ScriptID {

    protected String scriptfile  = null;
    protected String datapath    = null;
    protected String displayname = null;

    public ScriptID (String script, String path, String name) {
        scriptfile = script;
        datapath = path;
        displayname = name;
    }

    public ScriptID (ScriptID base, String path) {
        scriptfile = base.scriptfile;
        datapath = path;
        displayname = base.displayname;
    }

    public ScriptID getForPath(String path) {
        return new ScriptID(this, path);
    }

    public void setScript (String script) {
        scriptfile = script;
    }

    public void setDataPath (String path) {
        datapath = path;
    }

    public void setDisplayName (String name) {
        displayname = name;
    }

    public String getScript () {
        return scriptfile;
    }

    public boolean scriptEquals(ScriptID other) {
        return scriptEquals(other.getScript());
    }

    public boolean scriptEquals(String other) {
        if (scriptfile == null || other == null) return false;
        return stripHash(scriptfile).equals(stripHash(other));
    }

    public String getDataPath () {
        return datapath;
    }

    public String getDisplayName () {
        if (displayname == null && nameResolver != null)
            displayname = nameResolver.lookup(scriptfile);
        return (displayname == null ? scriptfile : displayname);
    }

    public String toString() {
        return getDisplayName();
    }

    public void display() {
        viewScript (scriptfile, datapath);
    }

    protected void viewScript (String theScript, String thePath) {
        String url = getHref(theScript, thePath);
        if (TriggerURI.isTrigger(url))
            TriggerURI.handle(url);
        else if (url != null)
            Browser.launch(url);
    }

    public String getHref() {
        return getHref(scriptfile, datapath);
    }

    protected String getHref (String theScript, String thePath) {
        if (theScript == null || thePath == null) return null;
        String delim = (theScript.startsWith("/") ? "/" : "//");
        return HTMLUtils.urlEncodePath(thePath) + delim + theScript;
    }

    protected String stripHash(String script) {
        int hashPos = script.indexOf('#');
        return (hashPos == -1 ? script : script.substring(0, hashPos));
    }

    public interface NameResolver {
        public String lookup(String name);
    }

    private static NameResolver nameResolver = null;
    public static void setNameResolver(NameResolver r) { nameResolver = r; }

}
