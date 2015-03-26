package net.sourceforge.processdash;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.util.FileUtils;

public class ExtractAllBackups {

    private static final boolean CLEANUP_FILES = false;

    public static void main(String[] args) {
        if (args.length == 0)
            printUsageAndExit();
        String dirName = args[0];
        File dir = new File(dirName);
        if (!dir.isDirectory())
            printUsageAndExit();

        try {
            extractAllBackups(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java ExtractAllBackups dirName");
        System.exit(1);
    }

    private static void extractAllBackups(File dir) throws Exception {
        File[] backups = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".zip");
            }
        });
        Arrays.sort(backups);
        for (int i = backups.length; i-- > 0;)
            extractOneBackup(backups, i);
    }

    private static void extractOneBackup(File[] backups, int i)
            throws Exception {
        String backupName = backups[i].getName();
        String baseName = backupName.substring(0, backupName.length() - 4);
        File extractDir = new File(backups[i].getParentFile(), baseName);
        FileUtils.deleteDirectory(extractDir);
        extractDir.mkdir();
        for (int j = backups.length; j-- > i;)
            unzip(extractDir, backups[j]);

        if (CLEANUP_FILES) {
            Matcher m = TIMESTAMP_PAT.matcher(baseName);
            m.find();
            String timestamp = m.group();
            long ts = DATE_FMT.parse(timestamp).getTime();
            File[] extractedFiles = extractDir.listFiles();
            for (int j = 0; j < extractedFiles.length; j++) {
                String name = extractedFiles[j].getName().toLowerCase();
                if (name.endsWith(".dat") || name.endsWith(".def")) {
                    if (extractedFiles[j].lastModified() > ts)
                        extractedFiles[j].delete();
                }
            }
        }
    }

    private static void unzip(File destDir, File zipFile) throws Exception {
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(zipFile)));
        for (ZipEntry e = zipIn.getNextEntry(); e != null; e = zipIn
                .getNextEntry()) {
            File outFile = new File(destDir, e.getName());
            FileUtils.copyFile(zipIn, outFile);
            outFile.setLastModified(e.getTime());
        }
    }


    private static final Pattern TIMESTAMP_PAT = Pattern.compile("\\d+");

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHHmmss");
}
