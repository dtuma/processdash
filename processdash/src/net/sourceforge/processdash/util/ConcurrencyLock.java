package net.sourceforge.processdash.util;

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

    /** General exception class indicating failure to obtain the lock */
    public class FailureException extends Exception {
    }

    /** Exception indicating that the lock could not be obtained because
     * some other process owns it, and we were unable to contact them. */
    public class AlreadyLockedException extends FailureException {
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



    private File lockFile = null;

    private FileChannel lockChannel = null;

    private FileLock lock = null;

    private MessageHandler messageHandler = null;

    private Thread shutdownHook = null;


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
     *
     * @throws SentMessageException if someone else already owns this lock, and
     *     we were able to send a message to them.  The exception will include
     *     the response from that owner.
     * @throws AlreadyLockedException if someone else already owns this lock,
     *     but we were unable to contact them.
     * @throws FailureException if the lock could not be obtained for any other
     *     reason.
     */
    public ConcurrencyLock(File lockFile, String message, Listener listener)
            throws SentMessageException, AlreadyLockedException,
            FailureException {

        try {
            this.lockFile = lockFile.getCanonicalFile();

            // Try to acquire a lock on the named lock file.
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock(0, 1, false);

            if (lock != null) {
                // we successfully got the lock.
                if (listener != null)
                    messageHandler = new MessageHandler(listener);
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
            FailureException fe = new FailureException();
            fe.initCause(e);
            throw fe;
        }
    }



    /** Release this concurrency lock.
     */
    public synchronized void unlock() {
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
        if (message == null || message.length() == 0)
            throw new AlreadyLockedException();

        try {

            // Read the information written in the lock file by the other
            // process
            BufferedReader infoIn = new BufferedReader(Channels.newReader(
                    lockChannel.position(1), "UTF-8"));
            String otherHost = infoIn.readLine();
            if (otherHost == null)
                // there is no contact information in the lock file (because
                // the other process didn't provide a Listener).
                throw new AlreadyLockedException();

            int otherPort = Integer.parseInt(infoIn.readLine());
            String otherToken = infoIn.readLine();
            infoIn.close();

            // Check to see if the other process is running on the same
            // computer that we're running on.  If not, don't try to contact it.
            if (!getCurrentHost().equals(otherHost))
                throw new AlreadyLockedException();

            // Try to contact the process that created this lock file, and
            // send it the message
            Socket s = new Socket(LOOPBACK_ADDR, otherPort);
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

            if (otherToken.equals(liveToken))
                throw new SentMessageException(response);
            else
                throw new AlreadyLockedException();

        } catch (FailureException fe) {
            throw fe;
        } catch (Exception exc) {
            // If we reach this point, it means we were UNABLE to contact
            // the process which created the lock file.
            AlreadyLockedException e = new AlreadyLockedException();
            e.initCause(exc);
            throw e;
        }
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

        public MessageHandler(Listener listener) throws IOException {
            super("Concurrency Lock Message Handler for " + lockFile);
            setDaemon(true);

            // listen on a socket for messages from other owners.
            this.serverSocket = ServerSocketChannel.open().socket();
            this.serverSocket.bind(new InetSocketAddress(LOOPBACK_ADDR, 0));
            this.token = Long.toString(System.currentTimeMillis());
            this.listener = listener;

            // write data into the lock file indicating the socket where
            // we are listening.
            Writer out = Channels.newWriter(lockChannel, "UTF-8");
            // write the first bytes of the file (which we have locked, and no
            // one else will be able to read).
            out.write("\n");
            // now write the information about how to contact us.
            out.write(getCurrentHost() + "\n");
            out.write(serverSocket.getLocalPort() + "\n");
            out.write(token + "\n");
            // flush the data we've written out to the file, but DO NOT close
            // the stream.  Closing the stream would release our lock.
            out.flush();
            lockChannel.force(true);

            // start up this thread.
            this.isRunning = true;
            start();
        }

        public void run() {
            while (isRunning) {
                try {
                    Socket s = serverSocket.getChannel().socket().accept();
                    handle(s);
                } catch (Exception e) {
                }
            }

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
