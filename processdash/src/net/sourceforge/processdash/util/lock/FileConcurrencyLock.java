// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util.lock;

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


public class FileConcurrencyLock implements ConcurrencyLock {

    private File lockFile;

    private String lockToken;

    private String extraInfo;

    private ConcurrencyLockApprover approver = null;

    private FileChannel lockChannel = null;

    private FileLock lock = null;

    private LockWatcher messageHandler = null;

    private boolean listenForLostLock = true;

    private Thread shutdownHook = null;


    private static Logger logger = Logger.getLogger(FileConcurrencyLock.class
            .getName());


    public FileConcurrencyLock(File lockFile) {
        this(lockFile, null);
    }

    public FileConcurrencyLock(File lockFile, String lockToken) {
        if (lockFile == null)
            throw new NullPointerException("lockFile cannot be null");
        if (lockToken == null)
            lockToken = Long.toString(System.currentTimeMillis());

        this.lockFile = lockFile;
        this.lockToken = lockToken;
    }

    public File getLockFile() {
        return lockFile;
    }

    public String getLockToken() {
        return lockToken;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public boolean isListenForLostLock() {
        return listenForLostLock;
    }

    public void setListenForLostLock(boolean listenForLostLock) {
        this.listenForLostLock = listenForLostLock;
    }

    public ConcurrencyLockApprover getApprover() {
        return approver;
    }

    public void setApprover(ConcurrencyLockApprover approver) {
        this.approver = approver;
    }

    public String getLockHash() {
        return Integer.toString(Math.abs(lockFile.hashCode()),
            Character.MAX_RADIX);
    }

    public void acquireLock(String extraInfo) throws LockFailureException {
        acquireLock(null, null, extraInfo);
    }


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
    * @throws SentLockMessageException if someone else already owns this lock, and
    *     we were able to send a message to them.  The exception will include
    *     the response from that owner.
    * @throws AlreadyLockedException if someone else already owns this lock,
    *     but we were unable to contact them.
    * @throws LockFailureException if the lock could not be obtained for any other
    *     reason.
    */
    public synchronized void acquireLock(String message,
            LockMessageHandler listener, String extraInfo)
            throws SentLockMessageException, AlreadyLockedException,
            LockFailureException {

        try {
            // check with our approver first.
            if (approver != null)
                approver.approveLock(this, extraInfo);

            // Try to acquire a lock on the named lock file.
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock(0, 1, false);

            if (lock != null) {
                // we successfully got the lock.
                if (listener != null)
                    messageHandler = new LockWatcher(listener, extraInfo);
                else
                    writeLockMetaData("", 0, lockToken, extraInfo);
                this.extraInfo = extraInfo;
                registerShutdownHook();
            } else {
                // we were unable to get the lock.  Possibly try to contact the
                // owner of the lock.  This will unequivocally throw some sort
                // of FailureException.
                tryToSendMessage(message);
            }

        } catch (LockFailureException fe) {
            throw fe;

        } catch (Exception e) {
            // If we were unable to obtain a lock, throw an appropriate exception
            releaseLock();
            throw new CannotCreateLockException(e);
        }

        logger.log(Level.FINE, "Obtained lock for: {0}", lockFile);
    }

    public synchronized boolean isLocked() {
        return (lock != null);
    }


    public void assertLock() throws LockFailureException {
        assertLock(false);
    }

    private synchronized void assertLock(boolean forceNativeReassert)
            throws LockFailureException {
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
            throw new NotLockedException();
        }

        try {
            FileChannel oldLockChannel = lockChannel;
            FileChannel newLockChannel =
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
            // from us.  Assume ownership of the physical lock file, and sync
            // the extraInfo in this object with the value from that file.
            // (This is important for the use case where a client creates this
            // object and then calls assertLock rather than acquireLock.)
            this.extraInfo = metaData.extraInfo;

            // if we're obtaining the lock for the first time, check with the
            // approver next to make certain we can proceed.  (The approver
            // doesn't get to say whether we keep an existing lock.)
            if (lock == null && approver != null)
                approver.approveLock(this, extraInfo);

            // There doesn't seem to be an API to determine whether we lost
            // the native lock.  The only way to test whether we still own
            // it, is to release and actively reestablish the lock.  But this
            // level of paranoia isn't always desirable, so we only do it if
            // requested.
            if (forceNativeReassert) {
                try {
                    if (lock != null) lock.release();
                    closeChannel(oldLockChannel);
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.log(Level.FINE,
                        "Exception when releasing lock for native reassert", e);
                }
                lock = null;
            }

            if (lock == null) {
                lock = lockChannel.tryLock(0, 1, false);

            } else {
                // for this branch, we're retaining the old lock that was
                // obtained long ago, and still held by this object.  That
                // means that we need to restore the old value of the
                // lockChannel field, and close the new channel that we just
                // opened temporarily.
                this.lockChannel = oldLockChannel;
                closeChannel(newLockChannel);
            }

            if (lock == null) {
                logger.log(Level.FINE, "Lock could not be reestablished: {0}",
                        lockFile);
                throw new AlreadyLockedException(null);
            } else {
                logger.log(Level.FINEST, "Lock is valid: {0}", lockFile);
            }
        } catch (LockFailureException fe) {
            throw fe;
        } catch (Exception e) {
            logger.log(Level.WARNING,
                "Unexpected exception when asserting file lock", e);
            throw new LockFailureException(e);
        }
    }

    private void closeChannel(FileChannel c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "Exception when closing channel", e);
            }
    }


    /** Release this concurrency lock.
     */
    public synchronized void releaseLock() {
        releaseLock(isLocked());
    }

    public synchronized void releaseLock(boolean deleteMetadata) {
        logger.log(Level.FINE, "Unlocking lock: {0}", lockFile);

        if (messageHandler != null) {
            try {
                messageHandler.terminate();
                messageHandler = null;
            } catch (Exception e) {
            }
        }

        if (lock != null) {
            try {
                lock.release();
                lock = null;
            } catch (Exception e) {
            }
        }

        if (lockChannel != null) {
            try {
                lockChannel.close();
                lockChannel = null;
            } catch (Exception e) {
            }
        }

        if (deleteMetadata) {
            try {
                lockFile.delete();
            } catch (Exception e) {
            }
        }

        this.extraInfo = null;

        if (shutdownHook != null) {
            try {
                if (Runtime.getRuntime().removeShutdownHook(shutdownHook))
                    shutdownHook = null;
            } catch (Exception e) {
            }
        }
    }



    private void tryToSendMessage(String message) throws LockFailureException {
        // Check to see if a lock file even exists.  If it doesn't, no other
        // process is holding the lock, so our inability to lock the file is
        // most likely due to an underlying OS-related problem.
        if (lockFile.exists() == false || lockFile.length() < 2)
            throw new CannotCreateLockException();

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
                throw new SentLockMessageException(response);
            else
                throw new AlreadyLockedException(extraInfo);

        } catch (LockFailureException fe) {
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
                releaseLock();
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
    private class LockWatcher extends Thread {

        private ServerSocket serverSocket;

        private String token;

        private LockMessageHandler listener;

        private volatile boolean isRunning;

        public LockWatcher(LockMessageHandler listener, String extraInfo)
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
            boolean nativeReassertNeeded = false;
            setCheckInterval(60);

            while (isRunning) {
                // listen for and handle a message.
                try {
                    Socket s = serverSocket.getChannel().socket().accept();
                    if (isRunning == true) handle(s);
                } catch (Exception e) {}

                if (listenForLostLock) {
                    // periodically check that the lock is still valid.
                    try {
                        long start = System.currentTimeMillis();
                        synchronized (FileConcurrencyLock.this) {
                            if (isRunning == false) break;
                            assertLock(nativeReassertNeeded);
                        }
                        long end = System.currentTimeMillis();
                        logger.log(Level.FINEST, "assertValidity took {0} ms",
                                new Long(end-start));
                        nativeReassertNeeded = false;
                        setCheckInterval(60);
                    } catch (LockUncertainException lue) {
                        // the "lock uncertain" exception indicates that we
                        // were momentarily unable to see the directory
                        // containing the lock file.  This problem is typical
                        // of broken network connectivity - which can also
                        // cause us to lose the native lock.  So when this
                        // problem occurs, we'll try to reassert the native
                        // lock at our next opportunity.
                        nativeReassertNeeded = true;
                        setCheckInterval(20);
                    } catch (Exception e) {
                        logger.log(Level.FINER,
                            "Exception when listening for lost lock", e);
                        try {
                            dispatchMessage(LockMessage.LOCK_LOST_MESSAGE);
                        } catch (Exception e1) {}
                        setCheckInterval(0);
                    }
                }
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
                String response = dispatchMessage(message);
                out.println(token);
                out.println(response);
                out.flush();
            } catch (Exception e) {
            }

            out.close();
            in.close();
            s.close();
        }

        private String dispatchMessage(String message) throws Exception {
            LockMessage msg = new LockMessage(FileConcurrencyLock.this, message);
            String result = listener.handleMessage(msg);
            return result;
        }

        public void terminate() {
            isRunning = false;
            try {
                serverSocket.close();
            } catch (IOException e) {}
        }
    }
}
