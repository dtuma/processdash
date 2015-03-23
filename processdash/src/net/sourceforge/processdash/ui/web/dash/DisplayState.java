// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.XMLUtils;

public class DisplayState extends TinyCGIBase {


    private XmlSerializer xml;

    @Override
    protected void doGet() throws IOException {
        xml = null;
        String script = (String) env.get("SCRIPT_NAME");
        if (script.contains("getTimingState"))
            writeTimingState();
        else if (script.contains("getHierarchy"))
            writeHierarchy();
        else if (script.contains("getScripts"))
            writeScripts();
        else
            throw new TinyCGIException(404, "Unrecognized request");
    }

    private boolean getFlag(String name, boolean defaultValue) {
        Object value = getParameter(name);
        if (value == null)
            return defaultValue;
        else
            return "true".equals(value) || Boolean.TRUE.equals(value);
    }

    private void startXml() throws IOException {
        out.write("Content-Type: text/xml\r\n\r\n");
        out.flush();

        boolean whitespace = getFlag("whitespace", false);
        xml = XMLUtils.getXmlSerializer(whitespace);
        xml.setOutput(outStream, "UTF-8");
        xml.startDocument("UTF-8", null);
    }

    private void finishXml() throws IOException {
        if (xml != null) {
            xml.endDocument();
            xml = null;
        }
    }

    private void writeTimingState() {
        DashboardContext ctx = getDashboardContext();
        DashboardTimeLog tl = (DashboardTimeLog) ctx.getTimeLog();
        TimeLoggingModel tlm = tl.getTimeLoggingModel();
        String path = tlm.getActiveTaskModel().getPath();
        String isTiming = tlm.isPaused() ? "false" : "true";

        out.write("Content-Type: text/plain\r\n\r\n");
        out.write("activeTask=" + path + "\r\n");
        out.write("isTiming=" + isTiming + "\r\n");
        out.flush();
    }

    private void writeHierarchy() throws IOException {
        Set dataToShow = new HashSet();
        String[] showParam = (String[]) parameters.get("show_ALL");
        if (showParam != null)
            dataToShow.addAll(Arrays.asList(showParam));

        startXml();
        writeHierarchy(xml, getPSPProperties(), PropertyKey.ROOT, dataToShow);
        finishXml();
    }

    private void writeHierarchy(XmlSerializer xml, DashHierarchy hier,
            PropertyKey node, Set dataToShow) throws IOException {
        xml.startTag(null, "node");
        xml.attribute(null, "name", node.name());
        if (dataToShow.contains("path"))
            xml.attribute(null, "path", node.path());
        String id = hier.getID(node);
        if (XMLUtils.hasValue(id) && dataToShow.contains("type"))
            xml.attribute(null, "type", id);

        int numChildren = hier.getNumChildren(node);
        if (dataToShow.contains("leaf"))
            xml.attribute(null, "leaf", Boolean.toString(numChildren == 0));

        if (dataToShow.contains("scripts"))
            writeScripts(node.path(), dataToShow.contains("scriptAncestors"),
                dataToShow.contains("scriptTriggers"));

        for (int i = 0; i < numChildren; i++)
            writeHierarchy(xml, hier, hier.getChildKey(node, i), dataToShow);

        xml.endTag(null, "node");
    }

    private void writeScripts() throws IOException {
        boolean includeAncestors = getFlag("includeAncestors", true);
        boolean includeTriggers = getFlag("includeTriggers", false);

        startXml();
        xml.startTag(null, "scriptList");
        writeScripts(getPrefix(), includeAncestors, includeTriggers);
        xml.endTag(null, "scriptList");
        finishXml();
    }

    private void writeScripts(String path, boolean includeAncestors,
            boolean includeTriggers) throws IOException {
        List<ScriptID> scripts = ScriptEnumerator.getScripts(
            getDashboardContext(), path);
        if (scripts == null || scripts.isEmpty())
            return;

        String defaultHref = scripts.get(0).getHref();
        for (int i = 1; i < scripts.size(); i++) {
            ScriptID oneScript = scripts.get(i);
            String oneHref = oneScript.getHref();

            // skip this script if it belongs to an ancestor, and we
            // were requested not to include ancestor entries
            if (!includeAncestors && !path.equals(oneScript.getDataPath()))
                continue;

            // skip this script if it is a trigger URL, and we were requested
            // not to include trigger entries.
            if (!includeTriggers && TriggerURI.isMandatoryTrigger(oneHref))
                continue;

            // set a flag if this is the default script
            boolean isDefault = false;
            if (defaultHref != null && defaultHref.equals(oneHref)) {
                isDefault = true;
                defaultHref = null;
            }

            // write an XML entry for this script.
            writeScript(xml, oneScript, isDefault, includeAncestors);
        }
    }

    private void writeScript(XmlSerializer xml, ScriptID scriptID,
            boolean isDefault, boolean writePath) throws IOException {
        xml.startTag(null, "script");
        xml.attribute(null, "href", scriptID.getHref());
        xml.attribute(null, "name", scriptID.getDisplayName());
        if (writePath)
            xml.attribute(null, "path", scriptID.getDataPath());
        if (isDefault)
            xml.attribute(null, "default", "true");
        xml.endTag(null, "script");
    }

}
