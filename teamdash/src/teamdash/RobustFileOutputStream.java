
package teamdash;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

public class RobustFileOutputStream extends OutputStream {

    public static final String OUT_PREFIX = "tttt";
    public static final String BACKUP_PREFIX = OUT_PREFIX + "_bak_";

    boolean origFileExists;
    File outFile, backupFile, destFile;
    OutputStream out;
    Checksum checksum;
    boolean closed = false;
    boolean aborted = false;

    public RobustFileOutputStream(String destFile) throws IOException {
        this(new File(destFile));
    }

    public RobustFileOutputStream(File destFile) throws IOException {
        File parentDir = destFile.getParentFile();
        if (!parentDir.isDirectory())
            parentDir.mkdirs();

        String filename = destFile.getName();
        this.destFile = destFile;
        this.outFile = new File(parentDir, OUT_PREFIX + filename);
        this.backupFile = new File(parentDir, BACKUP_PREFIX + filename);

        if (destFile.isDirectory())
            throw new IOException("Cannot write to file '" + destFile
                    + "' - directory is in the way.");

        origFileExists = destFile.isFile();

        checksum = makeChecksum();
        OutputStream outStream = new FileOutputStream(outFile);
        out = new CheckedOutputStream(outStream, checksum);
    }

    protected Checksum makeChecksum() {
        return new Adler32();
    }

    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void abort() throws IOException, IllegalStateException {
        if (closed)
            throw new IllegalStateException("File already closed");
        else if (aborted)
            return;
        else
            aborted = true;

        out.close();
        outFile.delete();
    }

    public void close() throws IOException {
        if (aborted)
            throw new IllegalStateException("Output already aborted");
        else if (closed)
            return;
        else
            closed = true;

        // close the temporary files
        out.close();

        // get the checksum calculated from writing the file
        long expectedChecksum = checksum.getValue();
        // reread the written file to see if it was written correctly
        long actualChecksum = computeChecksum(outFile, makeChecksum());
        // if the checksums don't match, throw an exception
        if (expectedChecksum != actualChecksum)
            throw new IOException("Error writing file '" + destFile +
                    "' - verification of written data failed.");

        // temporarily move the original file out of the way, into the backup
        if (origFileExists) {
            if (backupFile.exists())
                backupFile.delete();
            if (destFile.renameTo(backupFile) == false)
                throw new IOException("Error writing file '" + destFile +
                        "' - could not backup original file.");
        }

        // rename the output file to the real destination file
        if (outFile.renameTo(destFile) == false) {
            // put the original file back in place
            if (origFileExists) {
                if (backupFile.renameTo(destFile) == false) {
                    System.err.println("Warning - couldn't restore '" +
                            destFile + "' from backup '" + backupFile + "'.");
                }
            }
            throw new IOException("Error writing file '" + destFile + "'.");
        }

        // delete the backup
        if (origFileExists)
            backupFile.delete();
    }

    public long getChecksum() {
        return checksum.getValue();
    }

    private static long computeChecksum(File file, Checksum verify)
            throws IOException {
        InputStream in = new BufferedInputStream(new CheckedInputStream(
                new FileInputStream(file), verify));
        while (in.read() != -1)
            ; // do nothing
        in.close();

        return verify.getValue();
    }

}
