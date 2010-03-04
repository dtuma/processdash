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

package teamdash.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.util.FileUtils;

public class ClasspathContentProvider implements ContentSource {

    private String contentPrefix;

    public ClasspathContentProvider() {
        this("/Templates");
    }

    public ClasspathContentProvider(String prefix) {
        this.contentPrefix = prefix;
    }

    public byte[] getContent(String context, String uri, boolean raw)
            throws IOException {
        if (!uri.startsWith("/")) {
            URL contextURL = new URL("http://unimportant" + context);
            URL uriURL = new URL(contextURL, uri);
            uri = uriURL.getFile();
        }
        String resName = contentPrefix + uri;
        InputStream in = GenerateProcess.class.getResourceAsStream(resName);
        if (in == null)
            throw new IOException("No such file");
        else
            return FileUtils.slurpContents(in, true);
    }

}
