// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.rs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;

import net.sourceforge.processdash.rest.service.RestDashContext;

/**
 * Simplified implementation of a JAX-RS-style mechanism for invoking REST
 * endpoints.
 * 
 * JAX-RS is an industry standard platform for implementing REST APIs. But
 * during a first attempt at building a REST API for the Process Dashboard, it
 * was discovered that a near-empty WAR file based on Jersey and Jackson would
 * add a full three seconds to the startup sequence of the application, even
 * though 95% of users will never invoke any of the functionality it contains.
 * 
 * In the interest of performance, this simplified replacement allows
 * registration of various REST endpoints (via an initParam), and dispatches
 * requests to those endpoints based on path-match rules.
 */
public class DispatchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRestInvocation(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRestInvocation(req, resp);
    }

    private void handleRestInvocation(HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {
        initialize(req, resp);
        findAndRunHandler(req, resp);
    }



    private class Handler {

        Method method;

        Object target;

        Set<String> httpMethods;

        Pattern path;

    }


    private List<Handler> handlers;

    private void initialize(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (handlers == null) {
            try {
                RestDashContext.init(req);
                handlers = createHandlers();
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        }
    }

    private List<Handler> createHandlers() throws Exception {
        List<Handler> handlers = new ArrayList<Handler>();
        String[] classNames = getInitParameter("handlers").trim().split("\\s+");
        for (String oneClassName : classNames) {
            Class clazz = Class.forName(oneClassName, true,
                DispatchServlet.class.getClassLoader());
            createHandlersForClass(handlers, clazz);
        }
        return Collections.unmodifiableList(handlers);
    }

    private void createHandlersForClass(List<Handler> handlers,
            Class<? extends Object> clazz) throws Exception {

        // get information about the handler class itself
        Object target = clazz.newInstance();
        Path basePath = clazz.getAnnotation(Path.class);

        // create a handler for each applicable method
        for (Method m : clazz.getMethods()) {
            Handler handler = new Handler();
            handler.method = m;
            handler.target = target;
            handler.httpMethods = getHttpMethods(m);
            if (handler.httpMethods == null)
                // methods with no GET/POST/etc annotation are ignored
                continue;

            // build a string representing the desired path
            StringBuilder path = new StringBuilder();
            if (basePath != null)
                path.append(basePath.value());
            Path methodPath = m.getAnnotation(Path.class);
            if (methodPath != null)
                path.append(methodPath.value());
            if (path.length() == 0)
                continue;

            // construct a regexp for matching the desired path. This simple
            // implementation discards the names of path parameters, and just
            // passes all parameters in by position
            while (true) {
                int beg = path.indexOf("{");
                int end = path.indexOf("}");
                if (beg == -1 || end == -1)
                    break;
                path.replace(beg, end + 1, "([^/]+)");
            }
            handler.path = Pattern.compile(path.toString());

            // add this handler to the list
            handlers.add(handler);
        }
    }

    protected Set<String> getHttpMethods(Method m) {
        Set<String> httpMethods = new HashSet<String>();
        for (Annotation a : m.getAnnotations()) {
            HttpMethod hm = a.annotationType().getAnnotation(HttpMethod.class);
            if (hm != null)
                httpMethods.add(hm.value());
        }
        return httpMethods.isEmpty() ? null
                : Collections.unmodifiableSet(httpMethods);
    }


    private void findAndRunHandler(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        // if the handlers were not created successfully, abort
        if (handlers == null)
            return;

        // look for an appropriate handler, and invoke it
        String httpMethod = req.getMethod().toUpperCase();
        String pathInfo = req.getPathInfo();
        boolean sawPathMatch = false;
        for (Handler handler : handlers) {
            Matcher matcher = handler.path.matcher(pathInfo);
            if (matcher.matches()) {
                sawPathMatch = true;
                if (handler.httpMethods.contains(httpMethod)) {
                    runHandler(req, resp, matcher, handler);
                    return;
                }
            }
        }

        // if no handler was found, send an error response
        resp.sendError(sawPathMatch ? HttpStatus.METHOD_NOT_ALLOWED_405
                : HttpStatus.NOT_FOUND_404);
    }

    private void runHandler(HttpServletRequest req, HttpServletResponse resp,
            Matcher matcher, Handler handler) throws IOException {
        // build a parameter list for the method. This is always the servlet
        // request, followed by the path parameters in the order they appeared
        Object[] args = new Object[matcher.groupCount() + 1];
        args[0] = req;
        for (int i = matcher.groupCount(); i > 0; i--)
            args[i] = matcher.group(i);

        try {
            // invoke the handler and get the resulting object
            Map result = (Map) handler.method.invoke(handler.target, args);

            // send a response to the client
            resp.setContentType("application/json");
            JSONObject.writeJSONString(result, resp.getWriter());

        } catch (Exception e) {
            if (e.getCause() instanceof HttpException) {
                HttpException he = (HttpException) e.getCause();
                resp.sendError(he.getStatusCode());
            } else {
                e.printStackTrace();
                resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        }
    }

}
