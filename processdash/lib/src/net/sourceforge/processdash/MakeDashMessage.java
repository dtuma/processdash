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

package net.sourceforge.processdash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class MakeDashMessage extends Task {

    private File src;

    private String xml;

    private File dest;

    public void setSrc(File xml) {
        this.src = xml;
    }

    public void addText(String xml) {
        this.xml = xml;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    @Override
    public void execute() throws BuildException {
        checkParams();

        try {
            writeFile();
        } catch (IOException e) {
            throw new BuildException("Cannot write message file", e);
        }
    }

    private void checkParams() {
        if (src == null && (xml == null || xml.length() == 0))
            throw new BuildException("either src attribute must be specified, "
                    + "or nested XML data must be provided.");
        if (src != null && src.isFile() == false)
            throw new BuildException("Cannot find file '" + src.getPath() + "'");
        if (dest == null)
            throw new BuildException("dest attribute must be specified");
        if (dest.getParentFile().isDirectory() == false
                && dest.getParentFile().mkdirs() == false)
            throw new BuildException("Directory '" + dest.getParent()
                    + "' does not exist");

    }

    private void writeFile() throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(dest)));

        zipOut.putNextEntry(new ZipEntry("manifest.xml"));
        zipOut.write((MANIFEST_XML_1 + System.currentTimeMillis()
                + MANIFEST_XML_2).getBytes("UTF-8"));
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry("message.xml"));
        zipOut.write(getXmlData().getBytes("UTF-8"));
        zipOut.closeEntry();

        zipOut.finish();
        zipOut.close();
    }

    private String getXmlData() throws IOException {
        String result;
        if (src != null)
            result = readSrc();
        else
            result = xml;

        result = getProject().replaceProperties(result);

        if (result.trim().startsWith("<?xml") == false) {
            if (result.trim().startsWith("<messages") == false) {
                result = "<messages>\n" + result + "\n</messages>";
            }
            result = XML_HEADER + result;
        }

        return result;
    }

    private String readSrc() throws IOException {
        StringBuilder buf = new StringBuilder();
        Reader in = new InputStreamReader(new BufferedInputStream(
                new FileInputStream(src)), "UTF-8");
        int c;
        while ((c = in.read()) != -1)
            buf.append((char) c);

        return buf.toString();
    }

    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n";

    private static final String MANIFEST_XML_1 = XML_HEADER
            + "<archive type='dashboardDataExport'>\n"
            + "<exported when='@";
    private static final String MANIFEST_XML_2 = "' />\n"
            + "<file name='message.xml' type='messages' version='1' />\n"
            + "</archive>";
}
