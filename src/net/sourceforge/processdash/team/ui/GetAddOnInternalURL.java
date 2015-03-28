// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.net.URL;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

public class GetAddOnInternalURL extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        try {
            String baseResource = (String) env.get("SCRIPT_NAME");
            URL url = TemplateLoader.resolveURL(baseResource);
            if (url == null) {
                baseResource = baseResource + ".link";
                url = TemplateLoader.resolveURL(baseResource);
            }

            String relativeUri = getParameter("relativeUri");
            if (StringUtils.hasValue(relativeUri)) {
                url = new URL(url, relativeUri);
            }

            String absoluteUri = getParameter("absoluteUri");
            if (StringUtils.hasValue(absoluteUri)) {
                String base = url.toString();
                int exclPos = base.indexOf("!/");
                base = base.substring(0, exclPos + 2);
                if (absoluteUri.startsWith("/"))
                    absoluteUri = absoluteUri.substring(1);
                url = new URL(base + absoluteUri);
            }

            url.openStream().close();
            out.print(url.toString());
        } catch (Exception e) {
        }
    }

}
