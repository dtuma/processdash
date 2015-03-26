// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.XmlPatch;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.util.JAXPUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PatchXmlTask extends MatchingTask {

    private File patchFile;

    private String patchId;

    private File destDir;

    public void setPatchFile(File patchFile) {
        this.patchFile = patchFile;
    }

    public void setPatchId(String patchId) {
        this.patchId = patchId;
    }

    public void setDir(File sourceDir) {
        this.fileset.setDir(sourceDir);
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    @Override
    public void execute() throws BuildException {
        if (patchFile == null)
            throw new BuildException("You must specify a patch file");

        if (!patchFile.isFile())
            throw new BuildException("The file '" + patchFile
                    + "' does not exist");

        if (!destDir.isDirectory())
            throw new BuildException("The destDir '" + destDir
                    + "' is not a directory");

        DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        String[] srcFilenames = ds.getIncludedFiles();
        if (srcFilenames.length == 0)
            throw new BuildException(
                    "You must designate at least one input file.");

        long patchFileDate = patchFile.lastModified();

        Element patches;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(
                    patchFile));
            patches = XMLUtils.parse(in).getDocumentElement();
        } catch (Exception e) {
            throw new BuildException("Could not read '" + patchFile + "'", e);
        }
        if (patchId != null) {
            patches = findElementById(patches, patchId);
            if (patches == null)
                throw new BuildException("The patch file '" + patchFile
                        + "' does not contain a patch with the id '" + patchId
                        + "'");
        }

        for (int j = 0; j < srcFilenames.length; j++) {
            File inputFile = new File(ds.getBasedir(), srcFilenames[j]);
            String baseFilename = inputFile.getName();
            File outputFile = new File(destDir, baseFilename);

            long inputDate = Math.max(inputFile.lastModified(), patchFileDate);
            if (outputFile.lastModified() > inputDate) {
                // file is already up-to-date
                log("File '" + outputFile + "' is up-to-date",
                    Project.MSG_VERBOSE);
                continue;
            }

            Document document;
            try {
                document = JAXPUtils.getDocumentBuilder().parse(inputFile);
            } catch (Exception e) {
                throw new BuildException("Could not parse input file '"
                        + inputFile + "'", e);
            }

            XmlPatch.apply(document, patches);

            try {
                // convert the modified document back into text
                String docText = XMLUtils.getAsText(document);

                // if this was an HTML document, remove the XML header from
                // the top of the file
                if (docText.indexOf("<html") != -1 && docText.startsWith("<?")) {
                    int dirEnd = docText.indexOf("?>");
                    docText = docText.substring(dirEnd + 2);
                }

                // write the file
                OutputStream out = new BufferedOutputStream(
                        new CleanupNewlines(new FileOutputStream(outputFile)));
                out.write(docText.getBytes("UTF-8"));
                out.close();
            } catch (Exception e) {
                throw new BuildException("Could not write to output file '"
                        + outputFile + "'", e);
            }

            log("Wrote '" + outputFile + "'");
        }
    }

    private Element findElementById(Element elem, String id) {
        NodeList childNodes = elem.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node oneChild = childNodes.item(i);
            if (oneChild instanceof Element) {
                Element result = findElementById((Element) oneChild, id);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private static class CleanupNewlines extends FilterOutputStream {

        public CleanupNewlines(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (b == '\r') {
                // do nothing
            } else if (b == '\n') {
                super.write('\r');
                super.write('\n');
            } else {
                super.write(b);
            }
        }

    }

}
