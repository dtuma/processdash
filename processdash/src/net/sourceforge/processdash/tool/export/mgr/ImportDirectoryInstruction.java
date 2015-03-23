// Copyright (C) 2005-2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class ImportDirectoryInstruction extends AbstractInstruction {

    private static final String TAG_NAME = "importDir";

    private static final String DIR_ATTR = "directory";

    private static final String URL_ATTR = "url";

    private static final String PREFIX_ATTR = "prefix";

    public ImportDirectoryInstruction() {
    }

    public ImportDirectoryInstruction(String location, String prefix) {
        if (TeamServerSelector.isUrlFormat(location))
            setURL(location);
        else
            setDirectory(location);
        setPrefix(prefix);
    }

    public ImportDirectoryInstruction(Element e) {
        super(e);
    }

    public String getDirectory() {
        return getAttribute(DIR_ATTR);
    }

    public void setDirectory(String directory) {
        setAttribute(DIR_ATTR, directory);
    }

    public String getURL() {
        return getAttribute(URL_ATTR);
    }

    public void setURL(String url) {
        setAttribute(URL_ATTR, url);
    }

    public String getPrefix() {
        String path = getAttribute(PREFIX_ATTR);
        if (path != null && !path.startsWith("/"))
            path = "/" + path;
        return path;
    }

    public void setPrefix(String path) {
        if (path != null && !path.startsWith("/"))
            path = "/" + path;
        setAttribute(PREFIX_ATTR, path);
    }

    public String getDescription() {
        if (isUrlOnly()) {
            return resource.format(
                "Wizard.Import.Import_URL.Task_Description_FMT", getURL(),
                getPrefix());
        } else {
            return resource.format(
                "Wizard.Import.Import_Directory.Task_Description_FMT",
                getDirectory(), getPrefix());
        }
    }

    public boolean isUrlOnly() {
        return StringUtils.hasValue(getURL())
            && !StringUtils.hasValue(getDirectory());
    }

    public String getXmlTagName() {
        return TAG_NAME;
    }

    public void getAsXML(StringBuffer out) {
        super.getAsXML(out);
    }

    public static boolean matches(Element e) {
        return TAG_NAME.equals(e.getTagName());
    }

    public Object dispatch(ImportInstructionDispatcher dispatcher) {
        return dispatcher.dispatch(this);
    }

}
