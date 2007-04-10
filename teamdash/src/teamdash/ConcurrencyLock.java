package teamdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConcurrencyLock {

    /** Interface for indicating interest in messages sent by other processes
     * desiring this lock
     */
    public interface Listener {

        /** Called when some other process wanted to obtain a lock, and
         * could not because we own it.
         * 
         * @param message a message from the other process (for example,
         *     describing what they wanted to do with the lock).  This listener
         *     can potentially look at the message and perform some action on
         *     behalf of the other process.
         * @return our response to the message in question; can be null.  The
         *     reponse must not include the carriage return or newline
         *     characters.
         * @throws Exception if we cannot understand the message or respond
         *     to it for some reason.
         */
        public String handleMessage(String message) throws Exception;

    }

    /** A message that is sent to the listener of a lock, if the lock was
     * lost and could not be reclaimed. */
    public static final String LOCK_LOST_MESSAGE = "lockWasLost";


    /** General exception class indicating failure to obtain the lock */
    public class FailureException extends Exception {

        public FailureException() {}

        public FailureException(String message) {
            super(message);
        }

        public FailureException(Throwable cause) {
            super(cause);
        }

    }

    /** Exception indicating that the lock could not be obtained because
     * some other process owns it, and we were unable to contact them. */
    public class AlreadyLockedException extends FailureException {

        private String extraInfo;

        public AlreadyLockedException(String extraInfo) {
            this.extraInfo = extraInfo;
        }

        /** Get the extra info that was written into the lock file by the
         * owner of this lock.  If no extra information was provided by the
         * owner of the lock, returns null.
         */
        public String getExtraInfo() {
            return extraInfo;
        }

    }

    /** Exception indicating that the lock could not be obtained because
     * some other process owns it, but that we <b>were</b> able to contact
     * that other process.
     */
    public class SentMessageException extends FailureException {

        private String response;

        public SentMessageException(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }
    }

    /** Exception thrown if we cannot determine whether a lock is valid.
     * This typically occurs if the lock file is in a network directory that
     * is not currently reachable. */
    public class LockUncertainException extends FailureException {}



    private File lockFile = null;

    private FileChannel lockChannel = null;

    private FileLock lock = null;

    private MessageHandler messageHandler = null;

    private Thread shutdownHook = null;

    private String lockToken = null;


    private static Logger logger = Logger.getLogger(ConcurrencyLock.class
            .getName());


    /** Obtain a concurrency lock for the data in the given directory.
     *
     * @param lockFile a file symbolizing data we want to lock.  Clients must
     *     use their own convention for associating lock files with associated
     *     protected data.  This class will need to create the named lock file
     *     in order to successfully obtain a lock.
     * @param message (optional) a message to send to the owner of the lock.
     *     The message cannot contain carriage return or newline characters.
     *     If this parameter is null, no attempt will be made to contact the
     *     other process.
     * @param listener (optional) a listener who will receive messages from
     *     other processes which want this lock.
     * @param extraInfo (optional) an arbitrary string of text (containing no
     *     carriage return or newline characters) to write into the lock file.
     *     This could be used by clients to identify the owner of the lock in
     *     a human-meaningful way.
     *
     * @throws SentMessageException if someone else already owns this lock, and
     *     we were able to send a message to them.  The exception will include
     *     the response from that owner.
     * @throws AlreadyLockedException if someone else already owns this lock,
     *     but we were unable to contact them.
     * @throws FailureException if the lock could not be obtained for any other
     *     reason.
     */
    public ConcurrencyLock(File lockFile, String message, Listener listener,
            String extraInfo) throws SentMessageException,
            AlreadyLockedException, FailureException {

        try {
            this.lockFile = lockFile.getCanonicalFile();

            // Try to acquire a lock on the named lock file.
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock(0, 1, false);

            if (lock != null) {
                // we successfully got the lock.
                lockToken = Long.toString(System.currentTimeMillis());
                if (listener != null)
                    messageHandler = new MessageHandler(listener, extraInfo);
                else
                    writeLockMetaData("", 0, lockToken, extraInfo);
                registerShutdownHook();
            } else {
                // we were unable to get the lock.  Possibly try to contact the
                // owner of the lock.  This will unequivocally throw some sort
                // of FailureException.
                tryToSendMessage(message);
            }

        } catch (FailureException fe) {
            throw fe;

        } catch (Exception e) {
            // If we were unable to obtain a lock, throw a FailureException
            unlock();
            throw new FailureException(e);
        }

        logger.log(Level.FINE, "Obtained lock for: {0}", lockFile);
    }


    public void assertValidity() throws FailureException {
        if (lock == null || lockFile == null)
            throw new FailureException("This object does not own a lock.");

        if (!lockFile.getParentFile().isDirectory()) {
            // the parent directory of the lock doesn't exist.  This probably
            // means that we have temporarily lost our connection to the
            // network directory where the lock is located, and we don't
            // know for certain whether we've lost the lock.
            logger.log(Level.FINE, "Lock directory does not exist for {0}",
                    lockFile);
            throw new LockUncertainException();
        }

        if (!lockFile.exists()) {
            // the lock file no longer exists. This means that we lost the lock,
            // then someone else already claimed and released the lock.  We
            // have to assume that the information protected by the lock was
            // altered, and we no longer own the canonical version.
            logger.log(Level.FINE, "Lock file no longer exists: {0}", lockFile);
            throw new AlreadyLockedException(null);
        }

        try {
            FileChannel oldLockChannel = lockChannel;
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();

            LockMetaData metaData = readLockMetaData(false);
            if (metaData == null) {
                // the lock file can't be read, or doesn't contain any metadata.
                // this is another indication that someone else has claimed
                // this lock.
                logger.log(Level.FINE, "Lock file could not be read: {0}",
                        lockFile);
                closeChannel(oldLockChannel);
                throw new AlreadyLockedException(null);
            }

            if (!lockToken.equals(metaData.token)) {
                // someone else has claimed the lock, and still owns it.
                logger.log(Level.FINE, "Lock was lost: {0}", lockFile);
                closeChannel(oldLockChannel);
                throw new AlreadyLockedException(metaData.extraInfo);
            }

            // The lock file is still intact, and still contains our lock
            // token.  This is a sign that no one else has taken the lock
            // from us.
            //
            // There doesn't seem to be an API to determine whether we lost
            // the native lock.  The only way to test whether we still own
            // it, is to release and actively reestablish the lock.
            try {
                lock.release();
                closeChannel(oldLockChannel);
            } catch (Exception e) {}
            lock = lockChannel.tryLock(0, 1, false);
            if (lock == null) {
                logger.log(Level.FINE, "Lock could not be reestablished: {0}",
                        lockFile);
                throw new AlreadyLockedException(null);
            } else {
                logger.log(Level.FINEST, "Lock is valid: {0}", lockFile);
            }
        } catch (FailureException fe) {
            throw fe;
        } catch (Exception e) {
            throw new FailureException(e);
        }
    }

    private void closeChannel(FileChannel c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {}
    }


    /** Release this concurrency lock.
     */
    public synchronized void unlock() {
        logger.log(Level.FINE, "Unlocking lock: {0}", lockFile);

        if (messageHandler != null)
            try {
                messageHandler.terminate();
                messageHandler = null;
            } catch (Exception e) {
            }

        if (lock != null)
            try {
                lock.release();
                lock = null;
            } catch (Exception e) {
            }

        if (lockChannel != null)
            try {
                lockChannel.close();
                lockChannel = null;
            } catch (Exception e) {
            }

        if (lockFile != null)
            try {
                lockFile.delete();
                lockFile = null;
            } catch (Exception e) {
            }

        if (shutdownHook != null)
            try {
                if (Runtime.getRuntime().removeShutdownHook(shutdownHook))
                    shutdownHook = null;
            } catch (Exception e) {
            }
    }



    private void tryToSendMessage(String message) throws FailureException {
        String extraInfo = null;
        try {
            LockMetaData metaData = readLockMetaData(true);
            if (metaData == null)
                // there is no contact information in the lock file (because
                // the other process didn't provide a Listener).
                throw new AlreadyLockedException(null);
            else
                extraInfo = metaData.extraInfo;

            // if our caller didn't have a request to send, don't try to
            // contact the owner of the lock.
            if (message == null || message.length() == 0)
                throw new AlreadyLockedException(extraInfo);

            // Check to see if the other process is running on the same
            // computer that we're running on.  If not, don't try to contact it.
            if (!getCurrentHost().equals(metaData.hostName))
                throw new AlreadyLockedException(extraInfo);

            // Try to contact the process that created this lock file, and
            // send it the message
            Socket s = new Socket(LOOPBACK_ADDR, metaData.port);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s
                    .getOutputStream(), "UTF-8"), false);
            out.println(message);
            out.flush();

            // try to read its reponse to our message.
            s.setSoTimeout(4000);
            BufferedReader in = new BufferedReader(new InputStreamReader(s
                    .getInputStream(), "UTF-8"));
            String liveToken = in.readLine();
            String response = in.readLine();

            out.close();
            in.close();
            s.close();

            if (metaData.token.equals(liveToken))
                throw new SentMessageException(response);
            else
                throw new AlreadyLockedException(extraInfo);

        } catch (FailureException fe) {
            throw fe;
        } catch (Exception exc) {
            // If we reach this point, it means we were UNABLE to contact
            // the process which created the lock file.
            AlreadyLockedException e = new AlreadyLockedException(extraInfo);
            e.initCause(exc);
            throw e;
        }
    }



    private void writeLockMetaData(String hostName, int port, String token,
            String extraInfo) throws IOException {
        // write data into the lock file indicating the socket where
        // we are listening.
        Writer out = Channels.newWriter(lockChannel, "UTF-8");
        // write the first byte of the file (which we have locked, and no
        // one else will be able to read).
        out.write("\n");
        // now write the information about how to contact us.
        out.write(hostName + "\n");
        out.write(port + "\n");
        out.write(token + "\n");
        // write extra info, if it is present.
        if (extraInfo != null) {
            extraInfo = extraInfo.replace('\r', ' ').replace('\n', ' ');
            out.write(extraInfo + "\n");
        }

        // flush the data we've written out to the file, but DO NOT close
        // the stream.  Closing the stream would release our lock.
        out.flush();
        lockChannel.force(true);
    }

    private static class LockMetaData {
        String hostName;
        int port;
        String token;
        String extraInfo;
    }

    private LockMetaData readLockMetaData(boolean close) throws IOException {
        LockMetaData result = new LockMetaData();

        // Read the information written in the lock file by the other
        // process
        BufferedReader infoIn = new BufferedReader(Channels.newReader(
                lockChannel.position(1), "UTF-8"));
        result.hostName = infoIn.readLine();
        if (result.hostName == null)
            // there is no contact information in the lock file (because
            // the other process didn't provide a Listener, and was written
            // by an older version of this class).
            return null;

        result.port = Integer.parseInt(infoIn.readLine());
        result.token = infoIn.readLine();
        result.extraInfo = infoIn.readLine();

        if (close)
            infoIn.close();

        return result;
    }



    /** Register a JVM shutdown hook to clean up the files created by this lock.
     */
    private void registerShutdownHook() {
        // destroy the lock when the JVM is closing
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {
            public void run() {
                shutdownHook = null;
                unlock();
            }
        });
    }


    /** Get the IP address of the current host.
     */
    private String getCurrentHost() {
        String currentHost = "127.0.0.1";
        try {
            currentHost = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {
        }
        return currentHost;
    }


    private static InetAddress LOOPBACK_ADDR;
    static {
        try {
            LOOPBACK_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }



    /** Listen for and handle messages from other would-be lock owners.
     */
    private class MessageHandler extends Thread {

        private ServerSocket serverSocket;

        private String token;

        private Listener listener;

        private volatile boolean isRunning;

        public MessageHandler(Listener listener, String extraInfo)
                throws IOException {
            super("Concurrency Lock Message Handler for " + lockFile);
            setDaemon(true);

            // listen on a socket for messages from other owners.
            this.serverSocket = ServerSocketChannel.open().socket();
            this.serverSocket.bind(new InetSocketAddress(LOOPBACK_ADDR, 0));
            this.token = lockToken;
            this.listener = listener;

            writeLockMetaData(getCurrentHost(), serverSocket.getLocalPort(),
                    token, extraInfo);

            // start up this thread.
            this.isRunning = true;
            start();
        }

        public void run() {
            setCheckInterval(60);

            while (isRunning) {
                // listen for and handle a message.
                try {
                    Socket s = serverSocket.getChannel().socket().accept();
                    handle(s);
                } catch (Exception e) {}

                // periodically check to ensure that the lock is still valid.
                try {
                    long start = System.currentTimeMillis();
                    assertValidity();
                    long end = System.currentTimeMillis();
                    logger.log(Level.FINEST, "assertValidity took {0} ms",
                            new Long(end-start));
                    setCheckInterval(60);
                } catch (LockUncertainException lue) {
                    setCheckInterval(20);
                } catch (Exception e) {
                    try {
                        listener.handleMessage(LOCK_LOST_MESSAGE);
                    } catch (Exception e1) {}
                    setCheckInterval(0);
                }
            }

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void setCheckInterval(int seconds) {
            try {
                this.serverSocket.setSoTimeout(seconds * 1000 /*millis*/);
            } catch (Exception e) {}
        }

        private void handle(Socket s) throws Exception {
            BufferedReader in = new BufferedReader(new InputStreamReader(s
                    .getInputStream(), "UTF-8"));
            String message = in.readLine();

            PrintWriter out = new PrintWriter(new OutputStreamWriter(s
                    .getOutputStream(), "UTF-8"));
            try {
                String response = listener.handleMessage(message);
                out.println(token);
                out.println(response);
                out.flush();
            } catch (Exception e) {
            }

            out.close();
            in.close();
            s.close();
        }

        public void terminate() {
            isRunning = false;
            interrupt();
        }
    }
}
