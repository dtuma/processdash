// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class LocalizedTemplateResource extends TinyCGIBase {

    @Override
    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {
        this.env = env;

        String target = (String) env.get("SCRIPT_PATH");
        int pos = target.lastIndexOf('.');
        String file = target.substring(0, pos);
        String suffix = target.substring(pos);

        if (Translator.isTranslating()) {
            String language = Locale.getDefault().getLanguage();
            if (serveFile(file, language, suffix, out, false))
                return;
        }

        serveFile(file, "en", suffix, out, true);
    }

    private boolean serveFile(String file, String language, String suffix,
            OutputStream out, boolean failOnError) throws IOException {
        try {
            String uri = file + "_" + language + suffix;
            byte[] data = getRequest(uri, false);
            out.write(data);
            return true;
        } catch (IOException ioe) {
            if (failOnError)
                throw ioe;
            else
                return false;
        }
    }

}
