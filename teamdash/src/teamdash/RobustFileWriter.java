package teamdash;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RobustFileWriter extends OutputStreamWriter {

    public static final String OUT_PREFIX = RobustFileOutputStream.OUT_PREFIX;

    public static final String BACKUP_PREFIX = RobustFileOutputStream.BACKUP_PREFIX;

    RobustFileOutputStream outStream;

    public RobustFileWriter(String destFile) throws IOException {
        this(new RobustFileOutputStream(destFile));
    }

    public RobustFileWriter(File destFile) throws IOException {
        this(new RobustFileOutputStream(destFile));
    }

    protected RobustFileWriter(RobustFileOutputStream outStream)
            throws IOException {
        super(outStream);
        this.outStream = outStream;
    }

    public RobustFileWriter(String destFile, String encoding)
            throws IOException {
        this(new RobustFileOutputStream(destFile), encoding);
    }

    public RobustFileWriter(File destFile, String encoding) throws IOException {
        this(new RobustFileOutputStream(destFile), encoding);
    }

    protected RobustFileWriter(RobustFileOutputStream outStream, String encoding)
            throws IOException {
        super(outStream, encoding);
        this.outStream = outStream;
    }

    public void abort() throws IOException, IllegalStateException {
        outStream.abort();
    }

    public long getChecksum() {
        return outStream.getChecksum();
    }

}
