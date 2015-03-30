// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.mcf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.util.NonclosingInputStream;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class MCFManager {

    private static final MCFManager INSTANCE = new MCFManager();

    public static final MCFManager getInstance() {
        return INSTANCE;
    }

    private Map<String, CustomProcessPublisher> mcfPublishers;

    private ContentSource contentSource;

    private MCFManager() {
        this.mcfPublishers = Collections.synchronizedMap(new HashMap());
        this.contentSource = createContentSource();
    }

    private ContentSource createContentSource() {
        File cp = RuntimeUtils.getClasspathFile(MCFManager.class);
        if (cp.isDirectory()) {
            File templatesDir = new File(cp.getParentFile(), "Templates");
            return new FileContentProvider(templatesDir);
        } else {
            return new ClasspathContentProvider();
        }
    }


    public InputStream registerMcf(String jarURL, JarInputStream jarFile)
            throws IOException {

        // parse the XML doc from the jar file input stream
        Document settings;
        try {
            NonclosingInputStream in = new NonclosingInputStream(jarFile);
            settings = XMLUtils.parse(in);
        } catch (SAXException e) {
            throw new IOException("Error parsing settings.xml in " + jarURL, e);
        }

        // ensure this is the XML file for a custom process
        String rootTag = settings.getDocumentElement().getTagName();
        if (!CustomProcess.ROOT_TAG.equals(rootTag))
            return null;

        // create and initialize a publisher for this custom process
        CustomProcess process = new CustomProcess(settings);
        URL extBase = new URL("jar:" + jarURL + "!/");
        CustomProcessPublisher publisher = new CustomProcessPublisher(
                contentSource, extBase);
        publisher.setHeadless(true);
        publisher.loadInfoFromManifest(jarFile.getManifest());
        publisher.publish(process, null);

        // record the publisher in our data structures for later use
        String processID = process.getProcessID();
        mcfPublishers.put(processID, publisher);

        // return an input stream with the contents of the template.xml file
        String templateFilename = "/Templates/" + processID + "-template.xml";
        byte[] templateXmlData = publisher
                .getGeneratedFileContents(templateFilename);
        return new ByteArrayInputStream(templateXmlData);
    }

}
