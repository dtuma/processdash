// Copyright (C) 2014 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.net.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.servlet.RequestDispatcher;

import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.BlockingHttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * This class executes web requests internally for in-JVM clients.
 */
public class LocalConnector extends AbstractConnector {

    static final String PARENT_REQUEST_KEY = LocalConnector.class.getName()
            + ".parentRequest";

    static final String EXTRA_ENVIRONMENT_KEY = LocalConnector.class.getName()
            + ".extraEnvironment";

    private static final Logger LOG = Log.getLogger(LocalConnector.class);

    public LocalConnector() {
        setAcceptors(0);
    }

    public ByteArrayBuffer getResponse(String uri, int port, Map extraEnv)
            throws Exception {
        // get the current, previously active connection if one exists
        AbstractHttpConnection conn = AbstractHttpConnection
                .getCurrentConnection();

        // Identify the effective server name for this request
        Request parentRequest = (conn == null ? null : conn.getRequest());
        String host = (parentRequest == null ? "localhost:" + port //
                : parentRequest.getHeader("Host"));

        // construct an HTTP request for this data
        StringBuilder requestHeader = new StringBuilder();
        requestHeader.append("GET ").append(uri).append(" HTTP/1.0\r\n")
                .append("Host: ").append(host).append("\r\n")
                .append("Connection: close\r\n")
                .append("Content-Length: 0\r\n\r\n");

        // execute the request and retrieve the responses
        ByteArrayBuffer requestBuffer = new ByteArrayBuffer(
                requestHeader.toString(), StringUtil.__ISO_8859_1);
        LocalRequest request = new LocalRequest(requestBuffer, extraEnv,
                parentRequest, false);
        AccessController.doPrivileged(request);
        return request.getResponsesBuffer();
    }


    public Object getConnection() {
        return this;
    }

    @Override
    protected void accept(int acceptorID) throws IOException,
            InterruptedException {
        throw new IOException("Not supported");
    }

    public void open() throws IOException {}

    public void close() throws IOException {}

    public int getLocalPort() {
        return -1;
    }


    private class LocalRequest implements PrivilegedAction<Object> {

        private final ByteArrayBuffer _requestsBuffer;

        private Map _extraEnv;

        private Request _parentRequest;

        private final boolean _keepOpen;

        private volatile ByteArrayBuffer _responsesBuffer;

        private IOException _exception;

        private LocalRequest(ByteArrayBuffer requestsBuffer, Map extraEnv,
                Request parentRequest, boolean keepOpen) {
            _requestsBuffer = requestsBuffer;
            _extraEnv = extraEnv;
            _parentRequest = parentRequest;
            _keepOpen = keepOpen;
        }

        public Object run() {
            LocalEndPoint endPoint = new LocalEndPoint(_requestsBuffer,
                    _parentRequest);

            HttpConnection connection = new HttpConnection(endPoint,
                    getServer());
            Request request = connection.getRequest();
            request.setAttribute(PARENT_REQUEST_KEY, _parentRequest);
            request.setAttribute(EXTRA_ENVIRONMENT_KEY, _extraEnv);
            endPoint.setConnection(connection);
            connectionOpened(connection);

            boolean leaveOpen = _keepOpen;
            try {
                while (endPoint.getIn().length() > 0 && endPoint.isOpen()) {
                    while (true) {
                        final Connection con = endPoint.getConnection();
                        final Connection next = con.handle();
                        if (next != con) {
                            endPoint.setConnection(next);
                            continue;
                        }
                        break;
                    }
                }
            } catch (IOException x) {
                LOG.debug(x);
                leaveOpen = false;
            } catch (Exception x) {
                LOG.warn(x);
                leaveOpen = false;
            } finally {
                if (!leaveOpen)
                    connectionClosed(connection);
                if (_parentRequest != null)
                    connection.resetCurrentConnection(_parentRequest
                            .getConnection());
                _responsesBuffer = endPoint.getOut();
            }


            Throwable t = (Throwable) request
                    .getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            int status = connection.getResponse().getStatus();
            if (t instanceof IOException) {
                _exception = (IOException) t;
            } else if (t != null) {
                _exception = new IOException(t);
            } else if (status == 404) {
                _exception = new FileNotFoundException(request.getRequestURI());
            } else if ((status / 100) != 2) {
                _exception = new IOException("HTTP response code " + status
                        + ": \"" + connection.getResponse().getReason() + "\"");
            }

            return null;
        }

        public ByteArrayBuffer getResponsesBuffer() throws IOException {
            if (_exception != null)
                throw _exception;
            else
                return _responsesBuffer;
        }
    }


    private class HttpConnection extends BlockingHttpConnection {

        public HttpConnection(EndPoint endpoint, Server server) {
            super(LocalConnector.this, endpoint, server);
        }

        public void resetCurrentConnection(AbstractHttpConnection connection) {
            setCurrentConnection(connection);
        }

    }

    private class LocalEndPoint extends ByteArrayEndPoint {

        private Request _parentRequest;

        protected LocalEndPoint(ByteArrayBuffer input, Request parentRequest) {
            super(input.asArray(), 1024);
            _parentRequest = parentRequest;
            setGrowOutput(true);
        }

        @Override
        public void setConnection(Connection connection) {
            if (getConnection() != null && connection != getConnection())
                connectionUpgraded(getConnection(), connection);
            super.setConnection(connection);
        }

        @Override
        public String getLocalAddr() {
            if (_parentRequest != null)
                return _parentRequest.getLocalAddr();
            else
                return super.getLocalAddr();
        }

        @Override
        public String getLocalHost() {
            if (_parentRequest != null)
                return _parentRequest.getLocalName();
            else
                return super.getLocalHost();
        }

        @Override
        public int getLocalPort() {
            if (_parentRequest != null)
                return _parentRequest.getLocalPort();
            else
                return super.getLocalPort();
        }

        @Override
        public String getRemoteAddr() {
            if (_parentRequest != null)
                return _parentRequest.getRemoteAddr();
            else
                return super.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            if (_parentRequest != null)
                return _parentRequest.getRemoteHost();
            else
                return super.getRemoteHost();
        }

        @Override
        public int getRemotePort() {
            if (_parentRequest != null)
                return _parentRequest.getRemotePort();
            else
                return super.getRemotePort();
        }

    }

}
