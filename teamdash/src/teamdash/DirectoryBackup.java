package teamdash;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.util.FileUtils;

public class DirectoryBackup {

    private File directory;

    private String subdirName;

    private FileFilter filter;

    public DirectoryBackup(File directory, String subdirName, FileFilter filter) {
        this.directory = directory;
        this.subdirName = subdirName;
        this.filter = filter;
    }


    public void cleanupOldBackups(int numDays) {
        File backupDir = new File(directory, subdirName);
        if (!backupDir.isDirectory())
            return;

        File[] backupFiles = backupDir.listFiles();
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


    public void backup(String qualifier) throws IOException {
        File backupDir = new File(directory, subdirName);
        if (!backupDir.isDirectory() && !backupDir.mkdirs())
            throw new IOException("Directory '" + backupDir.getPath()
                    + "' does not exist, and could not be created.");

        File outputZipFile = new File(backupDir, getOutputFilename(qualifier));
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(outputZipFile)));

        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (filter.accept(files[i]))
                backupFile(zipOut, files[i]);
        }

        zipOut.close();
    }


    private String getOutputFilename(String qualifier) {
        String result = "backup-" + DATE_FMT.format(new Date());
        if (qualifier != null)
            result = result + "-" + qualifier;
        result = result + ".zip";
        return result;
    }


    private void backupFile(ZipOutputStream zipOut, File file)
            throws IOException {
        ZipEntry e = new ZipEntry(file.getName());
        e.setTime(file.lastModified());
        zipOut.putNextEntry(e);
        FileUtils.copyFile(file, zipOut);
        zipOut.closeEntry();
    }


    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHHmmss");

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{14}");

    private static final long DAY_MILLIS = 24L /* hours */* 60 /* minutes */
    * 60 /* seconds */* 1000 /* millis */;
}
