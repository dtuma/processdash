// Copyright (C) 2021 Tuma Solutions, LLC
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class BundledWorkingDirectoryLocal extends LocalWorkingDirectory {

    private FileResourceCollection collection;

    private ResourceBundleClient client;

    private static final Logger logger = Logger
            .getLogger(BundledWorkingDirectoryLocal.class.getName());

    public BundledWorkingDirectoryLocal(File targetDirectory,
            FileResourceCollectionStrategy strategy,
            File workingDirectoryParent) {
        super(targetDirectory, strategy, workingDirectoryParent);

        this.collection = new FileResourceCollection(workingDirectory, false,
                strategy, true);
        this.collection.loadFileDataCache(getFileDataCacheFile());
    }


    @Override
    public void prepare() throws IOException {
        // make sure the target and working directories exist
        super.prepare();
        collection.validate();

        // create the client if it hasn't been created already
        if (this.client == null)
            makeBundleClient();

        // prepare the contents of the working directory for use
        repairCorruptFiles();
        update();
    }

    private void makeBundleClient() throws IOException {
        // track locally checked out HEAD refs in metadata/heads.txt
        HeadRefs workingHeads = new HeadRefsPropertiesFileLocking(
                new File(getMetadataDir(), HEADS_FILE));

        // track globally published HEAD refs in target/heads/heads.txt
        File bundleHeadsDir = getSubdir(targetDirectory,
            FileBundleConstants.HEADS_SUBDIR);
        HeadRefs bundleHeads = new HeadRefsPropertiesFileLocking(
                new File(bundleHeadsDir, HEADS_FILE));

        // for WBS/disseminate dirs, track PDASH heads in separate "peg" files
        if (strategy.getFilenameFilter().accept(null, "test-data.pdash")) {
            HeadRefs pdash = new HeadRefsPegFiles(bundleHeadsDir, "PDASH");
            HeadRefs merged = new HeadRefsMerger() //
                    .addPatternedRefs(new PatternList(".*,pdash$"), pdash) //
                    .addDefaultRefs(bundleHeads);
            bundleHeads = merged;
        }

        // create our client object for performing sync up/down operations
        File bundleDir = getSubdir(targetDirectory,
            FileBundleConstants.BUNDLE_SUBDIR);
        this.client = new ResourceBundleClient(strategy, collection,
                workingHeads, bundleDir, bundleHeads);
    }

    private void repairCorruptFiles() throws IOException {
        List<String> corruptFilenames = new ArrayList();
        for (File oneFile : workingDirectory.listFiles()) {
            if (strategy.isFilePossiblyCorrupt(oneFile))
                corruptFilenames.add(oneFile.getName());
        }
        if (!corruptFilenames.isEmpty())
            client.restoreFiles(corruptFilenames);
    }


    @Override
    public File getDirectory() {
        return workingDirectory;
    }


    @Override
    public void update() throws IllegalStateException, IOException {
        discardLocallyCachedFileData();
        for (int numTries = 5; numTries-- > 0;) {
            if (client.syncDown() == false)
                return;
        }

        throw new IOException("Unable to sync down");
    }


    @Override
    public boolean flushData() throws LockFailureException, IOException {
        assertWriteLock();
        discardLocallyCachedFileData();
        for (int numTries = 5; numTries-- > 0;) {
            if (client.syncUp() == false) {
                try {
                    client.saveDefaultExcludedFiles();
                } catch (Exception e) {
                    logger.log(Level.FINE,
                        "Unable to save default excluded files", e);
                }
                collection.saveFileDataCache(getFileDataCacheFile());
                return true;
            }
        }

        return false;
    }


    private void discardLocallyCachedFileData() {
        // To avoid excessive file I/O, the FileResourceCollection class
        // caches certain file information and does not bother refreshing it
        // until a certain amount of time has passed. In some scenarios, we
        // cannot afford that luxury, so we need to recheck the files to
        // see if any have changed.
        collection.recheckAllFileTimestamps();
    }


    public URL doBackup(String qualifier) throws IOException {
        // make a backup of the local working directory.
        return doBackupImpl(workingDirectory, qualifier);
    }


    private File getFileDataCacheFile() {
        return new File(getMetadataDir(), "fileDataCache.xml");
    }

    private File getMetadataDir() {
        return getSubdir(workingDirectory, "metadata");
    }

    private File getSubdir(File base, String subdir) {
        File result = new File(base, subdir);
        result.mkdir();
        return result;
    }


    private static final String HEADS_FILE = "heads.txt";

}
