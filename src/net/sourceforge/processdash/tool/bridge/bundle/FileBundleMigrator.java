// Copyright (C) 2021-2023 Tuma Solutions, LLC
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;

import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.quicklauncher.TeamToolsVersionManager;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class FileBundleMigrator {

    public synchronized static void migrate(File directory,
            FileResourceCollectionStrategy strategy, FileBundleMode bundleMode)
            throws IOException, LockFailureException {
        migrate(directory, directory, strategy, bundleMode, true);
    }

    public synchronized static void migrate(File source, File dest,
            FileResourceCollectionStrategy strategy, FileBundleMode bundleMode,
            boolean lockSource) throws IOException, LockFailureException {
        if (!FileBundleUtils.isBundledDir(source))
            new FileBundleMigrator(source, dest, strategy, getDirType(strategy),
                MigrationDirection.Bundle, bundleMode, lockSource).migrate();
    }


    public synchronized static void unmigrate(File directory,
            FileResourceCollectionStrategy strategy)
            throws IOException, LockFailureException {
        unmigrate(directory, directory, strategy, true);
    }

    public synchronized static void unmigrate(File source, File dest,
            FileResourceCollectionStrategy strategy, boolean lockSource)
            throws IOException, LockFailureException {
        FileBundleMode bundleMode = FileBundleUtils.getBundleMode(source);
        if (bundleMode != null)
            new FileBundleMigrator(dest, source, strategy, getDirType(strategy),
                MigrationDirection.Unbundle, bundleMode, lockSource).migrate();
    }


    private static DirType getDirType(FileResourceCollectionStrategy strategy) {
        return (strategy instanceof DashboardInstanceStrategy
                ? DirType.Dashboard
                : DirType.WBS);
    }



    private enum MigrationDirection {
        Bundle, Unbundle
    }



    private enum DirType {

        Dashboard {
            void writeCompatibilityFiles(FileBundleMigrator fbm)
                    throws IOException {
                fbm.writeDashboardCompatibilityFiles();
            }
        },


        WBS {
            void writeCompatibilityFiles(FileBundleMigrator fbm)
                    throws IOException {
                fbm.writeWBSCompatibilityFiles();
            }

            void migrateAdjacent(FileBundleMigrator fbm)
                    throws IOException, LockFailureException {
                // automatically migrate the disseminate subdir if present
                File dataDissDir = new File(fbm.dataDir, "disseminate");
                File bundleDissDir = new File(fbm.bundleDir, "disseminate");
                File source = (fbm.direction == MigrationDirection.Bundle
                        ? dataDissDir : bundleDissDir);
                if (source.isDirectory())
                    new FileBundleMigrator(dataDissDir, bundleDissDir,
                            TeamDataDirStrategy.INSTANCE, Disseminate,
                            fbm.direction, fbm.bundleMode, fbm.lockSourceDir)
                                    .migrate();
            }
        },


        Disseminate;


        void writeCompatibilityFiles(FileBundleMigrator fbm)
                throws IOException {}

        void migrateAdjacent(FileBundleMigrator fbm)
                throws IOException, LockFailureException {}
    };



    private interface Migrator extends BundledWorkingDirectory {

    }



    private File dataDir, bundleDir;

    private boolean isInPlace;

    private FileResourceCollectionStrategy strategy;

    private DirType dirType;

    private MigrationDirection direction;

    private FileBundleMode bundleMode;

    private boolean lockSourceDir;

    private Migrator migrator;

    private FileBundleMigrator(File dataDir, File bundleDir,
            FileResourceCollectionStrategy strategy, DirType dirType,
            MigrationDirection direction, FileBundleMode bundleMode,
            boolean lockSourceDir) {
        this.dataDir = dataDir;
        this.bundleDir = bundleDir;
        this.isInPlace = dataDir.equals(bundleDir);
        this.strategy = strategy;
        this.dirType = dirType;
        this.direction = direction;
        this.bundleMode = bundleMode;
        this.lockSourceDir = lockSourceDir;
    }

    private void migrate() throws IOException, LockFailureException {
        // create a migrator object to do the work
        this.migrator = createMigrator();

        try {
            // lock the data directory to ensure (a) others aren't changing
            // it, and (b) no other migration process is underway
            acquireWriteLock();

            // migrate disseminate directories before WBS directories
            dirType.migrateAdjacent(this);

            if (this.direction == MigrationDirection.Bundle) {
                // migrate the files into the bundle directories
                bundle();

            } else {
                // migrate the files out of the bundle directories
                unbundle();
            }

        } catch (AlreadyMigrated am) {
            // an attempt was made to bundle an already-bundled directory, or
            // unbundle a directory that wasn't bundled. No action is needed

        } finally {
            // allow the migrator object to unwind resources
            File migratorMetadataDir = new File(dataDir, "metadata");
            FileUtils.deleteDirectory(migratorMetadataDir, true);
            migrator.releaseLocks();
        }
    }

    private Migrator createMigrator() {
        if (bundleMode == FileBundleMode.Local)
            return new Local(strategy);
        else if (bundleMode == FileBundleMode.Sync)
            return new Sync(strategy);

        throw new IllegalArgumentException("Unrecognized bundle mode");
    }

    private void bundle() throws IOException, LockFailureException {
        // check bundle mode preconditions
        ensureStartingBundleMode(dataDir, null, migrator.getBundleMode());

        // create the bundle client and related structures (but change no files)
        migrator.prepare();

        // backup the contents of the data directory for recovery purposes
        migrator.doBackup("before_bundle_migration");

        // write new bundles and refs for all source files
        migrator.flushData();

        // remove the now-migrated files from the bundle directory
        if (isInPlace)
            cleanupLegacyFiles();

        // write a minimal set of files into the bundle directory to
        // steer legacy clients away from accessing the data
        dirType.writeCompatibilityFiles(this);
    }

    private void unbundle() throws IOException, LockFailureException {
        // check bundle mode preconditions
        ensureStartingBundleMode(bundleDir, migrator.getBundleMode(), null);

        // remove read-only flags from compatibility files
        makeTargetFilesWriteable();

        // backup the data directory before unbundling
        URL backupFile = migrator.doBackup("before_bundle_unmigration");

        // create the bundle client, and extract all bundles into the data dir
        migrator.prepare();

        // move the now-obsolete bundle/heads directories into backup subdir
        if (isInPlace)
            backupBundleDirs(backupFile, FileBundleConstants.BUNDLE_SUBDIR,
                FileBundleConstants.HEADS_SUBDIR);
    }

    private void ensureStartingBundleMode(File directory,
            FileBundleMode expectedStartingMode,
            FileBundleMode desiredEndingMode) throws IOException {
        try {
            // make absolutely certain the dir has the mode we expect before we
            // begin our operation. (We checked this earlier, but we recheck
            // again now that we've locked the directory.)
            FileBundleUtils.ensureBundleMode(directory, expectedStartingMode);

        } catch (FileBundleModeMismatch fbmm) {
            // if the directory is already using the bundle mode that we desire
            // for our end state, abort with no action
            if (isInPlace && fbmm.getActualMode() == desiredEndingMode)
                throw new AlreadyMigrated();

            // unexpected situation - abort the entire operation
            throw fbmm;
        }
    }

    private void backupBundleDirs(URL backupFile, String... subdirNames) {
        // if the data directory contains bundle-related subdirs after an
        // unbundling operation, clean them up
        File backupDir = new File(dataDir, "backup");
        String prefix = extractBackupPrefix(backupFile);
        for (String oneSubdirName : subdirNames) {
            File oneSubdir = new File(dataDir, oneSubdirName);
            File destDir = new File(backupDir, prefix + oneSubdirName);
            if (oneSubdir.isDirectory())
                oneSubdir.renameTo(destDir);
        }
    }

    private String extractBackupPrefix(URL backupFile) {
        String result = backupFile.toString();
        int beg = result.lastIndexOf('/') + 1;
        int end = result.lastIndexOf('-') + 1;
        return result.substring(beg, end);
    }

    private void acquireWriteLock() throws LockFailureException {
        int retries = strategy instanceof TeamDataDirStrategy ? 5 : 1;
        while (true) {
            try {
                migrator.acquireWriteLock(null, migrator.getClass().getName());
                return;
            } catch (LockFailureException lfe) {
                if (--retries == 0)
                    throw lfe;

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void cleanupLegacyFiles() throws IOException {
        // delete legacy dashboard files from the bundle directory
        FilenameFilter filter = strategy.getFilenameFilter();
        List<String> filenames = FileUtils.listRecursively(bundleDir, filter);
        for (String filename : filenames) {
            File oneFile = new File(bundleDir, filename);
            if (!oneFile.delete())
                oneFile.deleteOnExit();
        }

        // the logic above will have deleted files, but not subdirectories.
        // clean the (now-empty) directories as a separate step
        for (File oneFile : bundleDir.listFiles()) {
            if (oneFile.isDirectory()) {
                if (filter.accept(bundleDir, oneFile.getName() + "/"))
                    FileUtils.deleteDirectory(oneFile, true);
            }
        }
    }

    private void makeTargetFilesWriteable() {
        // find all dataDir files that match the filter, and ensure writability
        FilenameFilter filter = strategy.getFilenameFilter();
        List<String> filenames = FileUtils.listRecursively(dataDir, filter);
        for (String filename : filenames) {
            File oneFile = new File(dataDir, filename);
            oneFile.setWritable(true);
        }
    }

    private void writeDashboardCompatibilityFiles() throws IOException {
        // write a "stub" pspdash.ini file that forces a minimum dashboard
        // software version, to prevent older clients from accessing the data
        writeCompatibilityFile("pspdash.ini", //
            property(DataVersionChecker.SETTING_NAME,
                "pspdash version " + getReqVersion("pspdash")),
            property(BUNDLE_MODE, bundleMode.getName()));

        // write a "dummy" global.dat file, so the Quick Launcher can recognize
        // the directory as one containing a dashboard dataset
        writeCompatibilityFile("global.dat", //
            "#include <bundle-support.txt>", //
            "= This dataset requires functionality added in dashboard version "
                    + getReqVersion("pspdash"));
    }

    private void writeWBSCompatibilityFiles() throws IOException {
        // write a "stub" user-settings.ini file that forces a minimum WBS
        // Editor version, to prevent older clients from accessing the data
        writeCompatibilityFile("user-settings.ini", //
            property(TeamToolsVersionManager.WBS_EDITOR_VERSION_REQUIREMENT,
                getReqVersion("teamToolsB")),
            property(BUNDLE_MODE, bundleMode.getName()));
    }

    private void writeCompatibilityFile(String filename, String... lines)
            throws IOException {
        File outFile = new File(bundleDir, filename);
        Writer out = new OutputStreamWriter(
                FileBundleUtils.outputStream(outFile), "UTF-8");
        for (String line : lines) {
            out.write(line);
            out.write(System.getProperty("line.separator"));
        }
        out.close();
        outFile.setReadOnly();
    }

    private String property(String key, String value) {
        return key + "=" + value;
    }

    private String getReqVersion(String packageId) {
        return bundleMode.getMinVersions().get(packageId);
    }


    private class Local extends BundledWorkingDirectoryLocal
            implements Migrator {

        public Local(FileResourceCollectionStrategy strategy) {
            super(bundleDir, strategy, null);
            setEnableBackgroundFlush(false);
        }

        @Override
        protected void createWorkingDirAndProcessLock(File wdp) {
            this.workingDirectory = dataDir;
        }

        @Override
        protected File getWriteLockDirectory() {
            return lockSourceDir ? dataDir : bundleDir;
        }

        @Override
        protected void ensureBundleMode() throws IOException {
            // skip this check when requested by our superclass, because we
            // expect the mode published to the filesystem to be out of sync
            // with the desired mode until migration is complete
        }

        @Override
        protected void repairCorruptFiles() {}

        @Override
        protected void flushLeftoverDirtyFiles() {}

        @Override
        public void update() throws IllegalStateException, IOException {
            // only update the target directory when unbundling
            if (direction == MigrationDirection.Unbundle)
                super.update();
        }

        @Override
        public boolean flushData() throws LockFailureException, IOException {
            // only flush data when we're performing a bundling operation
            return direction == MigrationDirection.Bundle //
                    && super.flushData();
        }
    }

    private class Sync extends BundledWorkingDirectorySync
            implements Migrator {

        public Sync(FileResourceCollectionStrategy strategy) {
            super(bundleDir, strategy, null);
            setEnableBackgroundFastForward(false);
            this.enforceLocks = true;
        }

        @Override
        protected void createWorkingDirAndProcessLock(File wdp) {
            this.workingDirectory = dataDir;
        }

        @Override
        protected File getWriteLockDirectory() {
            return lockSourceDir ? dataDir : bundleDir;
        }

        @Override
        protected void ensureBundleMode() throws IOException {
            // skip this check when requested by our superclass, because we
            // expect the mode published to the filesystem to be out of sync
            // with the desired mode until migration is complete
        }

        @Override
        protected void repairCorruptFiles() {}

        @Override
        protected void flushLeftoverDirtyFiles() {}

        @Override
        public void update() throws IllegalStateException, IOException {
            // only update the target directory when unbundling
            if (direction == MigrationDirection.Unbundle)
                super.update();
        }

        @Override
        public boolean flushData() throws LockFailureException, IOException {
            // only flush data when we're performing a bundling operation
            return direction == MigrationDirection.Bundle //
                    && super.flushData();
        }
    }

    private class AlreadyMigrated extends IOException {}

    private static final String BUNDLE_MODE = FileBundleUtils.BUNDLE_MODE_PROP;

}
