// Copyright (C) 2009 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.templates.ExtensionManager;

/**
 * This class locates and lists all of the process scripts and tools that are
 * associated with a particular task in the dashboard hierarchy.
 * 
 * @author Tuma
 */
public class ScriptEnumerator {

    private static final String DEFAULT_SOURCE_IMPL =
        DataNameScriptSource.class.getName();

    private static List<ScriptSource> SCRIPT_SOURCES = null;

    private static List<ScriptEnumeratorListener> LISTENERS =
        Collections.synchronizedList(new ArrayList());

    private static final Logger log = Logger.getLogger(ScriptEnumerator.class
            .getName());

    public static void addListener(ScriptEnumeratorListener l) {
        LISTENERS.add(l);
    }

    public static void removeListener(ScriptEnumeratorListener l) {
        LISTENERS.remove(l);
    }

    public static void scriptSourceChanged(ScriptSource source, String path) {
        ScriptEnumeratorEvent evt = new ScriptEnumeratorEvent(
                ScriptEnumerator.class, path);
        ArrayList<ScriptEnumeratorListener> notifyList =
                new ArrayList<ScriptEnumeratorListener>(LISTENERS);
        for (ScriptEnumeratorListener l : notifyList)
            l.scriptChanged(evt);
    }

    public static List<ScriptID> getScripts(DashboardContext ctx, String path) {
        PropertyKey key = ctx.getHierarchy().findClosestKey(path);
        return getScripts(ctx, key);
    }

    public static List<ScriptID> getScripts(DashboardContext ctx,
            PropertyKey path) {
        Vector<ScriptID> result = ctx.getHierarchy().getScriptIDs(path);
        ScriptID defaultScript = null;
        if (result.size() > 1)
            defaultScript = result.remove(0);

        boolean foundNonTemplateScripts = false;
        for (ScriptSource source : getScriptSources(ctx)) {
            List<ScriptID> scripts = source.getScripts(path.path());
            if (scripts != null) {
                for (ScriptID oneScript : scripts) {
                    if (!result.contains(oneScript)) {
                        result.add(oneScript);
                        foundNonTemplateScripts = true;
                    }
                }
            }
        }

        if (foundNonTemplateScripts) {
            Collections.sort(result, SCRIPT_COMPARATOR);
            ScriptID newDefault = result.get(0);
            if (defaultScript == null
                    || SCRIPT_COMPARATOR.compare(newDefault, defaultScript) < 0)
                defaultScript = newDefault;
        }
        if (defaultScript != null)
            result.add(0, defaultScript);

        return result;
    }

    private static List<ScriptSource> getScriptSources(DashboardContext ctx) {
        if (SCRIPT_SOURCES == null) {
            Map<String, ScriptSource> sources = new TreeMap<String, ScriptSource>();
            List extensions = ExtensionManager.getExecutableExtensions(
                "scriptSource", "class", DEFAULT_SOURCE_IMPL, "requires", ctx);
            for (Object ext : extensions) {
                if (ext instanceof ScriptSource) {
                    ScriptSource ss = (ScriptSource) ext;
                    sources.put(ss.getUniqueID(), ss);
                } else if (ext != null) {
                    log.severe("Invalid scriptSource declaration: class '"
                            + ext.getClass().getName()
                            + "' does not implement ScriptSource");
                }
            }
            SCRIPT_SOURCES = Collections.unmodifiableList(
                    new ArrayList<ScriptSource>(sources.values()));
        }
        return SCRIPT_SOURCES;
    }

    private static class ScriptPathComparator implements Comparator<ScriptID> {

        public int compare(ScriptID a, ScriptID b) {
            // Compare scripts ONLY based on the length of their datapath.  The
            // intent is to ensure that scripts for the same hierarchy node
            // are grouped together.  The "stable sorting" guarantee of the
            // Collections.sort method will ensure that multiple scripts for
            // the same node do not change their order relative to each other.
            return len(b.datapath) - len(a.datapath);
        }

        private int len(String s) {
            return (s == null ? 0 : s.length());
        }
    }
    private static final Comparator<ScriptID> SCRIPT_COMPARATOR =
        new ScriptPathComparator();
}
