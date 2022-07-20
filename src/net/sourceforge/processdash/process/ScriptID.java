// Copyright (C) 2000-2022 Tuma Solutions, LLC
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


import javax.swing.Action;

import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class ScriptID {

    protected String scriptfile  = null;
    protected String datapath    = null;
    protected String displayname = null;
    protected Action editAction  = null;

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

    public void setEditAction (Action action) {
        editAction = action;
    }

    public String getScript () {
        return scriptfile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScriptID) {
            ScriptID that = (ScriptID) obj;
            return eq(this.scriptfile, that.scriptfile)
                && eq(this.datapath, that.datapath);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (scriptfile != null)
            result = scriptfile.hashCode();
        if (datapath != null)
            result = result ^ datapath.hashCode();
        return result;
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
        return getDisplayName(null);
    }

    public String getDisplayName(DisplayNameListener l) {
        if ("".equals(displayname))
            return scriptfile;
        else if (displayname != null)
            return displayname;
        else if (nameResolver == null)
            return scriptfile;

        displayname = nameResolver.lookup(scriptfile, l != null);
        if (displayname != null)
            return (hasValue(displayname) ? displayname : scriptfile);

        NameResolvingWorker w = new NameResolvingWorker(l);
        w.start();
        return scriptfile;
    }

    public boolean displayNamesMatch(ScriptID that) {
        return this.displayname == null || that.displayname == null
                || this.displayname.equals(that.displayname);
    }

    public Action getEditAction () {
        return editAction;
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
        if (theScript.startsWith("http")) return theScript;
        String delim = (theScript.startsWith("/") ? "/" : "//");
        return HTMLUtils.urlEncodePath(thePath) + delim + theScript;
    }

    protected String stripHash(String script) {
        int hashPos = script.indexOf('#');
        return (hashPos == -1 ? script : script.substring(0, hashPos));
    }

    private static boolean eq(String a, String b) {
        if (a == b) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    public interface DisplayNameListener {
        public void displayNameChanged(ScriptID s, String displayName);
    }

    public interface NameResolver {
        /**
         * Look up the display name for a particular URI
         * @param uri the URI or URL to look up
         * @param quickly if true, do not risk time-consuming operations
         * @return a name to display. If no name could be retrieved, returns the
         *     empty string. If quickly was true and retrieving the name could
         *     take time, returns null.
         */
        public String lookup(String uri, boolean quickly);
    }

    private static NameResolver nameResolver = null;
    public static void setNameResolver(NameResolver r) { nameResolver = r; }

    private static boolean hasValue(String s) {
        return StringUtils.hasValue(s);
    }

    private class NameResolvingWorker extends SwingWorker {
        private DisplayNameListener l;
        public NameResolvingWorker(DisplayNameListener l) {
            this.l = l;
        }
        @Override
        public Object construct() {
            return nameResolver.lookup(scriptfile, false);
        }
        @Override
        public void finished() {
            displayname = (String) get();
            if (l != null && hasValue(displayname))
                l.displayNameChanged(ScriptID.this, displayname);
        }
    }

}
