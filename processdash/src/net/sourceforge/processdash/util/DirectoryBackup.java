// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

public abstract class DirectoryBackup {

    public interface ExtraContentSupplier {
        public void addExtraContentToBackup(ZipOutputStream out) throws IOException;
    }

    protected File srcDirectory;

    protected File destDirectory;

    protected FilenameFilter fileFilter;

    protected MessageFormat backupFilenameFormat;

    protected ExtraContentSupplier extraContentSupplier;

    protected int autoCleanupNumDays = -1;


    public File getSrcDirectory() {
        return srcDirectory;
    }

    public void setSrcDirectory(File srcDirectory) {
        this.srcDirectory = srcDirectory;
    }

    public File getDestDirectory() {
        return destDirectory;
    }

    public void setDestDirectory(File destDirectory) {
        this.destDirectory = destDirectory;
    }

    public FilenameFilter getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(FilenameFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public MessageFormat getBackupFilenameFormat() {
        return backupFilenameFormat;
    }

    public void setBackupFilenameFormat(MessageFormat backupFilenameFormat) {
        this.backupFilenameFormat = backupFilenameFormat;
    }

    public void setBackupFilenameFormat(String backupFilenameFormat) {
        setBackupFilenameFormat(new MessageFormat(backupFilenameFormat));
    }

    public ExtraContentSupplier getExtraContentSupplier() {
        return extraContentSupplier;
    }

    public void setExtraContentSupplier(ExtraContentSupplier extraContentSupplier) {
        this.extraContentSupplier = extraContentSupplier;
    }

    public int getAutoCleanupNumDays() {
        return autoCleanupNumDays;
    }

    public void setAutoCleanupNumDays(int autoCleanupNumDays) {
        this.autoCleanupNumDays = autoCleanupNumDays;
    }

    public File backup(String qualifier) throws IOException {
        validate();

        File destFile = getDestBackupFile(qualifier);
        doBackup(destFile);
        if (autoCleanupNumDays > 0)
            cleanupOldBackups(autoCleanupNumDays);
        return destFile;
    }

    protected void validate() throws FileNotFoundException {
        if (srcDirectory == null)
            throw new NullPointerException("Source directory is null");

        if (!srcDirectory.isDirectory())
            throw new FileNotFoundException("Directory '"
                    + srcDirectory.getPath() + "' does not exist.");

        if (destDirectory == null)
            destDirectory = new File(srcDirectory, "backup");

        if (!destDirectory.isDirectory() && !destDirectory.mkdirs())
            throw new FileNotFoundException("Directory '"
                    + destDirectory.getPath()
                    + "' does not exist, and could not be created.");

        if (fileFilter == null)
            fileFilter = new FilenameFilter(){
                public boolean accept(File dir, String name) {
                    return true;
                }};

        if (backupFilenameFormat == null)
            backupFilenameFormat = new MessageFormat("backup-{0}-{1}.zip");
    }

    protected abstract void doBackup(File destFile) throws IOException;

    public void cleanupOldBackups(int numDays) {
        if (!destDirectory.isDirectory())
            return;

        File[] backupFiles = destDirectory.listFiles();
        if (backupFiles == null || backupFiles.length == 0)
            return;

        Date cutoffDate = new Date(System.currentTimeMillis() - numDays
                * DAY_MILLIS);
        String cutoffStr = DATE_FMT.format(cutoffDate);

        for (int i = 0; i < backupFiles.length; i++) {
            File oneFile = backupFiles[i];
            String filename = oneFile.getName();
            Matcher m = DATE_PATTERN.matcher(filename);
            if (filename.toLowerCase().endsWith(".zip") && m.find()) {
                String fileDate = m.group();
                if (cutoffStr.compareTo(fileDate) > 0)
                    oneFile.delete();
            }
        }
    }

    protected List<String> getFilenamesToBackup() {
        return FileUtils.listRecursively(srcDirectory, fileFilter);
    }

    protected File getDestBackupFile(String qualifier) {
        String now = DATE_FMT.format(new Date());
        Object[] fmtArgs = new Object[] { now, FileUtils.makeSafe(qualifier) };
        String filename = backupFilenameFormat.format(fmtArgs);
        return new File(destDirectory, filename);
    }



    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHHmmss");

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{14}");

    private static final long DAY_MILLIS = 24L /* hours */* 60 /* minutes */
    * 60 /* seconds */* 1000 /* millis */;

}
