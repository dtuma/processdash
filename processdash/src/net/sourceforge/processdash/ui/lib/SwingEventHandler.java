// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.SwingUtilities;

public class SwingEventHandler {

    public static <T> T create(Class<T> clazz, T delegate) {
        return create(clazz, delegate, true);
    }

    public static <T> T create(Class<T> clazz, T delegate, boolean async) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
            new Class[] { clazz }, new Handler(delegate, async));
    }

    private static class Handler implements InvocationHandler {
        private Object delegate;
        private boolean async;

        public Handler(Object delegate, boolean async) {
            this.delegate = delegate;
            this.async = async;
        }

        public Object invoke(Object proxy, final Method method,
                final Object[] args) throws Throwable {
            MethodInvocation i = new MethodInvocation(delegate, method, args);
            if (async) {
                SwingUtilities.invokeLater(i);
                return null;
            } else {
                SwingUtilities.invokeAndWait(i);
                if (i.t != null)
                    throw i.t;
                return i.result;
            }
        }
    }

    private static class MethodInvocation implements Runnable {
        private Object target;
        private Method m;
        private Object[] args;
        private Object result;
        private Throwable t;
        public MethodInvocation(Object target, Method m, Object[] args) {
            this.target = target;
            this.m = m;
            this.args = args;
            this.t = null;
        }
        public void run() {
            try {
                result = m.invoke(target, args);
            } catch (Throwable t) {
                this.t = t;
            }
        }
    }

}
