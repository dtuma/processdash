// Copyright (C) 2000-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class RepositoryServer extends Thread {

    int port = 2467;
    DataRepository data = null;
    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();

    private static final String RESTORE_DEFAULT_TOKEN =
        DataInterpreter.RESTORE_DEFAULT_TOKEN.saveString();

    private class RepositoryServerThread extends Thread implements DataListener {

        DataRepository data = null;
        Socket clientSocket = null;
        BufferedReader in = null;
        ObjectOutputStream out = null;
        public String dataPath = null;

        public RepositoryServerThread(DataRepository data, Socket clientSocket) {
            this.data = data;
            this.clientSocket = clientSocket;

            try {
                // debug("getting output stream...");
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                // send some ignored data through the streams to test connectivity
                // debug("writing to output stream...");
                out.flush();

                // debug("getting input stream...");
                in = new BufferedReader
                    (new InputStreamReader(clientSocket.getInputStream()));
                // debug("reading from input stream...");
                String ID = in.readLine();
                String requiredTag = in.readLine();
                if (ID.startsWith("/"))
                    dataPath = HTMLUtils.urlDecode(ID);
                else
                    dataPath = data.getPath(ID);
                if (dataPath == null)
                    dataPath = "//anonymous//";

                // debug("Done. got both streams.  Writing settings information.");

                out.writeObject(dataPath);
                if (requiredTag.length() == 0)
                    out.writeBoolean(true);
                else {
                    SimpleData d = data.getSimpleValue
                        (DataRepository.createDataName(dataPath,requiredTag));
                    out.writeBoolean(d != null && d.test());
                }
                out.writeObject(Settings.getSettings());
                out.flush();

            } catch (IOException e) { printError(e); }

            try {
                setName(getName() + "(RepositoryServerThread"+dataPath+")");
            } catch (Exception e) {}
        }

//    private void debug(String msg) {
//      System.out.println("RepositoryServerThread: " + msg);
//    }

        private void printError(Exception e) {
            if (threadIsRunning) {
                System.err.println("Exception: " + e);
                e.printStackTrace(System.err);
            }
        }

        private volatile boolean threadIsRunning = true;

        public void run() {
            String methodName = null;
            String dataName = null;
            String value = null;
            String prefix = null;

            while (threadIsRunning) try {
                // debug("reading from socket...");
                try {
                    methodName = null;
                    methodName = in.readLine();
                } catch (SocketException se) {
                    //if (threadIsRunning) {
                    //  System.err.println ("socket error, dying...");
                    //  printError (se);
                    //}
                    threadIsRunning = false;
                }
                // debug("method is "+methodName);

                                        // quit
                if (methodName == null || methodName.equals("quit"))
                    threadIsRunning = false;

                                          // putValue
                else if (methodName.equals("putValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    value = in.readLine();        // debug("    arg is "+value);
                    if (RESTORE_DEFAULT_TOKEN.equals(value))
                        data.restoreDefaultValue(dataName);
                    else
                        data.userPutValue(dataName,
                                          ValueFactory.create(null, value, null, null));
                }
                                        // removeValue
                else if (methodName.equals("removeValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.removeValue(dataName);
                }
                                        // addDataListener
                else if (methodName.equals("addDataListener")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.addDataListener(dataName, this);
                }
                                        // removeDataListener
                else if (methodName.equals("removeDataListener")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.removeDataListener(dataName, this);
                }

                else if (methodName.equals("maybeCreateValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    value = in.readLine();        // debug("    arg is "+value);
                    prefix = in.readLine();       // debug("    arg is "+prefix);
                    data.maybeCreateValue(dataName, value, prefix);
                }

                else if (methodName.equals("logMessage")) {
                    value = LOG_PREFIX + in.readLine();
                    value = StringUtils.findAndReplace(value, "\u0001", "\n"+LOG_PREFIX);
                    System.out.println(value);
                }

                else
                    System.err.println("RepositoryServerThread: I don't understand " +
                                       methodName);

            } catch (Exception e) { printError(e); }

            cleanup();
        }
        private static final String LOG_PREFIX = "DApplet:";

        public void dataValueChanged(DataEvent e) {
            try {
                synchronized (out) {
                    sendDataEvent(e);
                    out.flush();
                }
            } catch (Exception ex) { printError(ex); }
        }

        public void dataValuesChanged(Vector v) {
            if (v == null || !threadIsRunning) return;

            synchronized (out) {
                for (int i = v.size();  i > 0; )
                    try {
                        sendDataEvent((DataEvent) v.elementAt(--i));
                    } catch (Exception ex) { printError(ex); }

                try {
                    out.flush();
                } catch (Exception ex) { printError(ex); }
            }
        }

        // WARNING - you must manually synchronize on the <code>out</code>
        // object before calling this method.
        private void sendDataEvent(DataEvent e) throws IOException {

            // We used to just send data events verbatim.  But there was one
            // problem with this; when an item's simple value is "null", it
            // has no way to communicate whether that null value is
            // read-only.  Then, when the item is displayed to the user, the
            // null value is editable - a bad thing. So when the value is
            // null, we have to do the legwork to find out whether it is
            // read-only.

            if (e.getValue() == null) {
                // look up the element in the repository, following any
                // applicable data aliases
                SaveableData d = data.getValue(data.getAliasedName(e.getName()));

                // if the resulting item is not null and is not editable, then
                // we instead will create a proxy data event which claims that
                // the element's value is the read only empty string (rather
                // than null).

                if (d != null && !d.isEditable())
                    e = new DataEvent((Repository) e.getSource(), e.getName(),
                                      e.getID(), ImmutableStringData.EMPTY_STRING);
            }
            out.writeObject(e);
        }

        private void cleanup() {
            try {
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException e) { printError(e); }

            data.deleteDataListener(this);
        }

        public void quit() {
            threadIsRunning = false;
            interrupt();
            cleanup();
        }
    }


//  private void debug(String msg) {
//    System.out.println("RepositoryServer: " + msg);
//  }

    private void printError(Exception e) {
        if (serverIsRunning) {
            System.err.println("Exception: " + e);
            e.printStackTrace(System.err);
        }
    }

    public RepositoryServer(DataRepository r, ServerSocket socket) {
        data = r;
        serverSocket = socket;
        if (socket != null)
            port = socket.getLocalPort();
        else
            port = -1;
    }

    public int getPort() {
        return port;
    }

    private volatile boolean serverIsRunning = true;

    public void handle(Socket clientSocket) {
        RepositoryServerThread newServerThread =
            new RepositoryServerThread(data, clientSocket);
        newServerThread.start();
        serverThreads.addElement(newServerThread);
    }

    public void run() {
        Socket clientSocket = null;

        if (serverSocket == null) return;

        while (serverIsRunning) try {
            // debug("accepting...");
            clientSocket = serverSocket.accept();
            // debug("got a connection.");
            handle(clientSocket);
        } catch (IOException e) {
            printError(e);
        }
    }

    public void deletePrefix(String prefix) {
        for (int i = serverThreads.size();   i-- > 0; ) {
            RepositoryServerThread thread =
                (RepositoryServerThread) serverThreads.elementAt(i);
            if (thread.dataPath.equals(prefix)) {
                thread.quit();
                serverThreads.removeElement(thread);
            }
        }
    }

    public synchronized void quit() {
        serverIsRunning = false;
        interrupt();

        for (int i = serverThreads.size();   i-- > 0; )
            ((RepositoryServerThread) serverThreads.elementAt(i)).quit();
    }

}
