// Copyright (C) 2021-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.FileUtils;

/**
 * An implementation of HeadRefs that stores bundle references by creating
 * single-byte "peg" files in a particular directory.
 */
public class HeadRefsPegFiles implements HeadRefs {

    private File pegFileDirectory;

    private String pegFilePrefix;

    public HeadRefsPegFiles(File pegFileDirectory, String qualifier) {
        this.pegFileDirectory = pegFileDirectory;
        this.pegFilePrefix = "HEAD-" + qualifier + "-";
    }


    @Override
    public FileBundleID getHeadRef(String bundleName) throws IOException {
        return getHeadRefs().get(bundleName);
    }


    @Override
    public Map<String, FileBundleID> getHeadRefs() throws IOException {
        // Get the list of peg files, and sort so newer files are listed later
        List<String> pegFilenames = getPegFilenames();
        Collections.sort(pegFilenames);

        // Extract the bundleID from each peg filename, and add to a result Map
        // If multiple HEADs exist for a given bundle, newer ones will win
        Map<String, FileBundleID> result = new HashMap<String, FileBundleID>();
        for (String onePegFile : pegFilenames) {
            try {
                String token = onePegFile.substring(pegFilePrefix.length());
                FileBundleID bundleID = new FileBundleID(token);
                result.put(bundleID.getBundleName(), bundleID);
            } catch (IllegalArgumentException iae) {
                // if someone created a bogus file in the directory with our
                // prefix, ignore it
            }
        }

        // return the collection of bundle HEADs we found
        return result;
    }


    @Override
    public void storeHeadRef(FileBundleID bundleID) throws IOException {
        storeHeadRefs(Collections.singleton(bundleID));
    }


    @Override
    public void storeHeadRefs(Collection<FileBundleID> headRefs)
            throws IOException {
        // Get the files that we own in the directory
        List<String> pegFilenames = getPegFilenames();

        // iterate over the new HEAD refs to be stored
        for (FileBundleID bundleID : headRefs) {
            // build the name of the peg file we need to create
            String newFilename = pegFilePrefix + bundleID.getToken() + ".txt";
            File newPegFile = new File(pegFileDirectory, newFilename);

            // see if this device has written a file in the past we can reuse
            String reuseFilename = getPegFileToReuse(pegFilenames, bundleID);
            if (reuseFilename != null) {
                // rename the old peg file to the new name
                File fileToReuse = new File(pegFileDirectory, reuseFilename);
                FileUtils.renameFile(fileToReuse, newPegFile);
                pegFilenames.remove(reuseFilename);

            } else {
                // write a new peg file to store the new HEAD ref
                OutputStream out = FileBundleUtils.outputStream(newPegFile);
                out.write('#');
                out.close();
            }
        }
    }

    private String getPegFileToReuse(List<String> pegFilenames,
            FileBundleID newBundleID) {
        // look through the existing peg files for one that was written by this
        // device for this bundle
        String deviceAndBundleNameSuffix = newBundleID.getToken()
                .substring(FileBundleID.TIMESTAMP_LEN) + ".txt";
        for (String onePegFile : pegFilenames) {
            if (onePegFile.endsWith(deviceAndBundleNameSuffix))
                return onePegFile;
        }
        return null;
    }


    @Override
    public void deleteHeadRef(String bundleName) throws IOException {
        // Get the files that we own in the directory
        List<String> pegFilenames = getPegFilenames();

        // delete peg files from the directory that represent old HEADs
        // for the given bundle
        deletePegFilesForBundle(pegFilenames, bundleName);
    }


    private List<String> getPegFilenames() throws IOException {
        // abort if the directory is unavailable (e.g., lost network)
        String[] filenames = pegFileDirectory.list();
        if (filenames == null)
            throw new FileNotFoundException(pegFileDirectory.getPath());

        // get the current list of files that we own in the directory
        List<String> result = new ArrayList<String>(filenames.length);
        for (String oneFilename : filenames) {
            if (oneFilename.startsWith(pegFilePrefix))
                result.add(oneFilename);
        }

        // return the list of files we found
        return result;
    }

    private void deletePegFilesForBundle(List<String> pegFilenames,
            String bundleName) {
        // delete peg files from the directory that represent old HEADs
        // for the given bundle
        String bundleSuffix = "-" + bundleName + ".txt";
        for (Iterator i = pegFilenames.iterator(); i.hasNext();) {
            String onePegFile = (String) i.next();
            if (onePegFile.endsWith(bundleSuffix)) {
                new File(pegFileDirectory, onePegFile).delete();
                i.remove();
            }
        }
    }

}
