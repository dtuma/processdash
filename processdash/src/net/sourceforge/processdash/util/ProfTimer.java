// Copyright (C) 2001-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <P>
 * The <B>ProfTimer</B> class is used for crude profiling.
 * <P>
 */
public class ProfTimer {

    private Logger logger;

    private Level level;

    private String instanceName;

    private long lastTime;

    public ProfTimer(Class clazz) {
        this(clazz, clazz.getName());
    }

    public ProfTimer(Class clazz, String instanceName) {
        this(Logger.getLogger("profileTimer." + clazz.getName()),
            instanceName);
    }

    public ProfTimer(Logger logger) {
        this(logger, null);
    }

    public ProfTimer(Logger logger, String instanceName) {
        this(logger, instanceName, Level.FINEST);
    }

    public ProfTimer(Logger logger, String instanceName, Level level) {
        this.logger = logger;
        this.instanceName = (instanceName == null ? "" : instanceName + ": ");
        this.level = level;
        this.lastTime = System.currentTimeMillis();

        if (logger.isLoggable(level))
            logger.log(level, this.instanceName + "starting...");
    }

    public void click(String msg) {
        long currTime = System.currentTimeMillis();
        long diff = currTime - lastTime;
        if (logger.isLoggable(level))
            logger.log(level, instanceName + msg + ", took " + diff + " ms.");
        lastTime = currTime;
    }
}
