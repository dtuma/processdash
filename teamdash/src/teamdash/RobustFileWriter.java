package teamdash;

import java.io.*;

public class RobustFileWriter extends Writer {

    public static final String OUT_PREFIX    = "tttt";
    public static final String BACKUP_PREFIX = OUT_PREFIX + "_";

    File outFile, backupFile, destFile;
    Writer out, backup;

    public RobustFileWriter(String destFile) throws IOException {
        this(new File(destFile), null);
    }

    public RobustFileWriter(String destFile, String encoding)
        throws IOException {
        this(new File(destFile), encoding);
    }

    public RobustFileWriter(File destFile) throws IOException {
        this(destFile, null);
    }

    public RobustFileWriter(File destFile, String encoding)
        throws IOException
    {
        this(destFile.getParent(), destFile.getName(), encoding);
    }

    public RobustFileWriter(String directory, String filename, String encoding)
        throws IOException
    {
        File parentDir = new File(directory);
        if (!parentDir.isDirectory())
            parentDir.mkdirs();
        destFile   = new File(parentDir, filename);
        outFile    = new File(parentDir, OUT_PREFIX    + filename);
        backupFile = new File(parentDir, BACKUP_PREFIX + filename);
        if (encoding == null) {
            out    = new FileWriter(outFile);
            backup = new FileWriter(backupFile);
        } else {
            out = new OutputStreamWriter
                (new FileOutputStream(outFile), encoding);
            backup = new OutputStreamWriter
                (new FileOutputStream(backupFile), encoding);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        backup.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        out.flush();
        backup.flush();
    }

    public void close() throws IOException {
        // close the temporary files
        out.close();
        backup.close();

        // rename to the real destination file
        destFile.delete();
        outFile.renameTo(destFile);

        // delete the backup
        backupFile.delete();
    }

}
