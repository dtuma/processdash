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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsXmlConstants;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

import teamdash.team.TeamMember;

public class VirtualPdashWriter implements ArchiveMetricsXmlConstants {

    public static final String USER_SETTING = "virtualEV";

    public static final String TIME_ZONE_SETTING = "timeZone";

    public static final String MIN_WBS_VERSION = "6.5";


    /** The team project */
    private TeamProject teamProject;

    /** The human-readable location of the team project */
    private String location;

    /** The location of the project, if it is stored on a PDES */
    private URL locationUrl;

    /** The reverse synchronizer. */
    private WBSSynchronizer reverseSynchronizer;


    public VirtualPdashWriter(TeamProject teamProject, String location,
            WBSSynchronizer reverseSynchronizer) {
        this.teamProject = teamProject;
        this.location = location;
        if (location.startsWith("http")) {
            try {
                locationUrl = new URL(location);
            } catch (IOException e) {
            }
        }
        this.reverseSynchronizer = reverseSynchronizer;
    }


    public void writeVirtualPdashFiles(long exportTimestamp)
            throws IOException {
        boolean enabled = "enabled"
                .equals(teamProject.getUserSetting(USER_SETTING));
        if (exportTimestamp <= 0)
            exportTimestamp = System.currentTimeMillis();
        String tzs = teamProject.getUserSetting(TIME_ZONE_SETTING);
        TimeZone tz = TimeZone.getTimeZone(tzs == null ? "GMT" : tzs);

        for (TeamMember m : teamProject.getTeamMemberList().getTeamMembers()) {
            if (isValid(m) && !hasJoined(m)) {
                writeVirtualPdashFile(m, enabled, exportTimestamp, tz);
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


    private void writeVirtualPdashFile(TeamMember m, boolean enabled,
            long exportTimestamp, TimeZone tz) throws IOException {
        // identify the PDASH file where data should be written
        String filename = m.getInitials()
                + WBSFilenameConstants.EXPORT_FILENAME_ENDING;
        File f = new File(teamProject.getStorageDirectory(), filename);

        // if virtual EV is disabled, delete the file if it exists and abort
        if (!enabled) {
            deletePdashFile(f);
            return;
        }

        // if this virtual user already has a PDASH file, get the checksum
        // of its ev.xml file
        long oldChecksum = getEvXmlChecksum(f);

        // open an output stream to write data
        RobustFileOutputStream out = new RobustFileOutputStream(f);
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(out));

        try {
            // write a manifest.xml file into the ZIP
            writeManifest(zipOut, m, exportTimestamp);

            // write an ev.xml file into the ZIP
            long newChecksum = writeEV(zipOut, m, tz);

            if (oldChecksum == newChecksum) {
                // if the exported data has not changed, discard the new PDASH
                // file we've been creating and leave the old one in place
                out.abort();

            } else {
                // close the ZIP, saving changes
                zipOut.finish();
                zipOut.flush();
                zipOut.close();
                flushPdashFile(f);
            }

        } catch (IOException ioe) {
            out.abort();
            throw ioe;
        }
    }


    private long getEvXmlChecksum(File f) throws IOException {
        ZipInputStream zipIn = null;
        try {
            if (f.isFile()) {
                zipIn = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(f)));
                ZipEntry e;
                while ((e = zipIn.getNextEntry()) != null) {
                    if (EV_FILE_NAME.equals(e.getName())) {
                        return FileUtils.computeChecksum(zipIn, new Adler32(),
                            false);
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            FileUtils.safelyClose(zipIn);
        }
        return -1;
    }


    private void deletePdashFile(File f) throws IOException {
        try {
            // delete the file from our team project directory
            f.delete();

            // if this is a bridged team project directory, delete from server
            if (locationUrl != null)
                ResourceBridgeClient.deleteSingleFile(locationUrl, f.getName());

        } catch (LockFailureException e) {
            // can't happen - PDASH files don't require locks
        }
    }


    private void flushPdashFile(File f) throws IOException {
        FileInputStream in = null;
        try {
            // if this is a bridged team project directory, upload to server
            if (locationUrl != null) {
                in = new FileInputStream(f);
                ResourceBridgeClient.uploadSingleFile(locationUrl, f.getName(),
                    in);
            }
        } catch (LockFailureException lfe) {
            // can't happen - PDASH files don't require locks
        } finally {
            FileUtils.safelyClose(in);
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

        // write a <file> entry for ev.xml
        xml.startTag(null, FILE_ELEM);
        xml.attribute(null, FILE_NAME_ATTR, EV_FILE_NAME);
        xml.attribute(null, TYPE_ATTR, FILE_TYPE_EARNED_VALUE);
        xml.attribute(null, VERSION_ATTR, "1");
        xml.endTag(null, FILE_ELEM);

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


    private long writeEV(ZipOutputStream zipOut, TeamMember m, TimeZone tz)
            throws IOException {
        // build and calculate the EV data
        VirtualEVModel evModel = new VirtualEVModel(teamProject, m, tz);
        evModel.recalc();

        // start an entry in the ZIP for ev.xml and write the EV data
        zipOut.putNextEntry(new ZipEntry(EV_FILE_NAME));
        CheckedOutputStream cc = new CheckedOutputStream(zipOut, new Adler32());
        evModel.writeEV(cc);
        cc.flush();
        zipOut.closeEntry();

        // return the checksum of the ev.xml file we just wrote
        return cc.getChecksum().getValue();
    }


    private static final String WBS_EDITOR_PKG_ID = "teamToolsB";

    private static final String EV_FILE_NAME = "ev.xml";

}
