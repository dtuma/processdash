// Copyright (C) 2025 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsXmlConstants;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.team.TeamMember;

public class VirtualPdashWriter implements ArchiveMetricsXmlConstants {

    /** The team project */
    private TeamProject teamProject;

    /** The human-readable location of the team project */
    private String location;

    /** The reverse synchronizer. */
    private WBSSynchronizer reverseSynchronizer;


    public VirtualPdashWriter(TeamProject teamProject, String location,
            WBSSynchronizer reverseSynchronizer) {
        this.teamProject = teamProject;
        this.location = location;
        this.reverseSynchronizer = reverseSynchronizer;
    }


    public void writeVirtualPdashFiles(long exportTimestamp)
            throws IOException {
        if (exportTimestamp <= 0)
            exportTimestamp = System.currentTimeMillis();

        for (TeamMember m : teamProject.getTeamMemberList().getTeamMembers()) {
            if (isValid(m) && !hasJoined(m)) {
                writeVirtualPdashFile(m, exportTimestamp);
            }
        }
    }

    private boolean isValid(TeamMember m) {
        return StringUtils.hasValue(m.getInitials())
                && StringUtils.hasValue(m.getName());
    }

    private boolean hasJoined(TeamMember m) {
        return reverseSynchronizer.hasMemberJoined(m.getInitials());
    }


    private void writeVirtualPdashFile(TeamMember m, long exportTimestamp)
            throws IOException {
        // identify the PDASH file where data should be written
        String filename = m.getInitials()
                + WBSFilenameConstants.EXPORT_FILENAME_ENDING;
        File f = new File(teamProject.getStorageDirectory(), filename);

        // open an output stream to write data
        RobustFileOutputStream out = new RobustFileOutputStream(f);
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(out));

        try {
            // write a manifest.xml file into the ZIP
            writeManifest(zipOut, m, exportTimestamp);

            // close the ZIP
            zipOut.finish();
            zipOut.flush();
            zipOut.close();
        } catch (IOException ioe) {
            out.abort();
            throw ioe;
        }
    }


    private void writeManifest(ZipOutputStream zipOut, TeamMember m,
            long exportTimestamp) throws IOException {
        // start an entry in the ZIP for the manifest
        zipOut.putNextEntry(new ZipEntry(MANIFEST_FILE_NAME));

        // start the XML document and write the root <archive> tag
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(zipOut, ENCODING);
        xml.startDocument(ENCODING, Boolean.TRUE);
        xml.startTag(null, ARCHIVE_ELEM);
        xml.attribute(null, TYPE_ATTR, FILE_TYPE_ARCHIVE);
        xml.attribute(null, VIRTUAL_ATTR, "true");
        xml.attribute(null, SOURCE_ATTR, "wbs");

        // start the <exported> block
        xml.startTag(null, EXPORTED_TAG);
        xml.attribute(null, OWNER_ATTR, m.getName());
        xml.attribute(null, WHEN_ATTR, "@" + exportTimestamp);

        // write the <fromWBS> tag
        xml.startTag(null, FROM_WBS_TAG);
        xml.attribute(null, FROM_DATASET_LOCATION_ATTR, location);
        xml.endTag(null, FROM_WBS_TAG);

        // write the <fromDataset> tag
        xml.startTag(null, FROM_DATASET_TAG);
        xml.attribute(null, FROM_DATASET_ID_ATTR, getVirtualDatasetID(m));
        xml.endTag(null, FROM_DATASET_TAG);

        // write the version number of the WBS Editor
        xml.startTag(null, PACKAGE_ELEM);
        xml.attribute(null, PACKAGE_ID_ATTR, WBS_EDITOR_PKG_ID);
        xml.attribute(null, VERSION_ATTR, WBSDataWriter.getDumpVersion());
        xml.endTag(null, PACKAGE_ELEM);

        // finish the <exported> block
        xml.endTag(null, EXPORTED_TAG);

        // finalize the document and close the ZIP entry
        xml.endTag(null, ARCHIVE_ELEM);
        xml.endDocument();
        zipOut.closeEntry();
    }

    private String getVirtualDatasetID(TeamMember m) throws IOException {
        // compute an identifying string for this pseudo-individual. The
        // approach here will create a single "person" for all occurrences of a
        // given full name within a team dashboard, but will generate distinct
        // person identifiers for different teams
        String id = teamProject.getDatasetID();
        if (!StringUtils.hasValue(id))
            id = teamProject.getProjectID();
        String s = id + ":" + m.getName();

        // use these to compute a name-based UUID
        UUID uuid = UUID.nameUUIDFromBytes(s.toString().getBytes("UTF-8"));
        return uuid.toString();
    }

    private static final String WBS_EDITOR_PKG_ID = "teamToolsB";

}
