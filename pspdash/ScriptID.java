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

import java.net.URLEncoder;

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
        if (theScript != null) {
            String url = encode(thePath) + "//" + theScript;
            Browser.launch(url);
        }
    }

    protected String encode(String path) {
        String result = URLEncoder.encode(path);
        result = StringUtils.findAndReplace(result, "%2f", "/");
        result = StringUtils.findAndReplace(result, "%2F", "/");
        return result;
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
