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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/**
 * Configurable class for executing commands against subversion.
 * 
 * This class can be subclassed for more sophisticated behavior.
 */
public class SvnExecutor {

    protected String svnCommand = "svn";

    protected List<String> svnOptions;

    protected File baseDirectory;

    protected String baseUrl;

    protected String rootUrl;

    protected String basePrefix;

    protected int numThreads = 10;

    protected Executor executor;

    protected static final Logger logger = Logger.getLogger(SvnExecutor.class
            .getName());


    public SvnExecutor() {}

    public String getSvnCommand() {
        return svnCommand;
    }

    public void setSvnCommand(String svnCommand) {
        this.svnCommand = svnCommand;
    }

    public List<String> getSvnOptions() {
        return svnOptions;
    }

    public void setSvnOptions(List<String> svnOptions) {
        this.svnOptions = svnOptions;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        if (!baseDirectory.isDirectory())
            throw new SvnDiffException.NotWorkingCopy(new FileNotFoundException(
                    baseDirectory.getPath()));

        this.baseDirectory = baseDirectory;
        this.baseUrl = this.rootUrl = this.basePrefix = null;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }



    /** Get the root URL of the repository */
    public String getRootUrl() {
        if (rootUrl == null)
            validateAndLoadBaseInfo();
        return rootUrl;
    }

    /** Get the URL of the base directory */
    public String getBaseUrl() {
        if (baseUrl == null)
            validateAndLoadBaseInfo();
        return baseUrl;
    }

    /** Get the path of the base directory relative to the repository root */
    public String getBasePath() {
        if (basePrefix == null)
            validateAndLoadBaseInfo();
        return basePrefix;
    }



    /**
     * Ensure that the configuration of this object is correct.
     * 
     * @throws SvnDiffException.AppNotFound
     *             if the svn executable could not be found.
     * @throws SvnDiffException.NotWorkingCopy
     *             if the {@link #baseDirectory} has not been configured, or if
     *             it does not point to a svn working copy
     * @throws SvnDiffException
     *             if any other unexpected problem is encountered.
     */
    public void validate() throws SvnDiffException {
        validateAndLoadBaseInfo();
    }


    protected void validateAndLoadBaseInfo() throws SvnDiffException {
        try {
            InputStream in = exec("help");
            while (in.read() != -1)
                ;
        } catch (Exception e) {
            throw new SvnDiffException.AppNotFound(e);
        }

        try {
            Document xml = execXml("info", "--xml");
            this.baseUrl = XMLUtils.xPathStr("/info/entry/url", xml);
            this.rootUrl = XMLUtils.xPathStr("/info/entry/repository/root", xml);

        } catch (Exception e) {
            throw new SvnDiffException.NotWorkingCopy(e);
        }

        if (baseUrl.startsWith(rootUrl))
            basePrefix = baseUrl.substring(rootUrl.length());
        else
            throw new SvnDiffException(
                    "Unexpected relationship between base/root URL");
    }



    /**
     * Execute an "svn" command and return the output as an XML document.
     * 
     * @param args
     *            the arguments to pass to svn
     * @return the output of the svn process, as XML
     * @throws IOException
     *             if an error occurs
     */
    public Document execXml(Object... args) throws IOException {
        InputStream in = exec(args);
        try {
            return XMLUtils.parse(in);
        } catch (SAXException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }


    /**
     * Execute an "svn" command and return the output.
     * 
     * @param args
     *            the arguments to pass to svn
     * @return the output of the svn process
     * @throws IOException
     *             if an error occurs
     */
    public InputStream exec(Object... args) throws IOException {
        List<String> commandLine = new ArrayList<String>();
        commandLine.add(svnCommand);
        commandLine.add("--non-interactive");
        appendArgs(commandLine, svnOptions);
        for (Object o : args)
            appendArgs(commandLine, o);

        logger.fine("About to exec: " + StringUtils.join(commandLine, " "));
        String[] cmd = commandLine.toArray(new String[commandLine.size()]);
        Process proc = Runtime.getRuntime().exec(cmd, null, baseDirectory);
        return new BufferedInputStream(proc.getInputStream());
    }

    private void appendArgs(List<String> commandLine, Object o) {
        if (o instanceof String) {
            commandLine.add((String) o);
        } else if (o instanceof List) {
            commandLine.addAll((List) o);
        } else {
            // ignore other args, for example null
        }
    }



    /**
     * Request the execution of an svn task at some time in the future.
     * 
     * @param task the task to execute
     */
    public void queue(final SvnTask task) {
        synchronized (this) {
            if (executor == null)
                executor = Executors.newFixedThreadPool(numThreads,
                    new DaemonThreadFactory());
        }

        executor.execute(new Runnable() {
            public void run() {
                task.getTaskHelper().execute(SvnExecutor.this);
            }});
    }

    /**
     * Request the execution of a set of svn tasks at some time in the future.
     * 
     * @param tasks the tasks to execute
     */
    public void queueAll(Collection<? extends SvnTask> tasks) {
        for (SvnTask t : tasks)
            queue(t);
    }

    protected static class DaemonThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {
            Thread result = new Thread(r);
            result.setDaemon(true);
            return result;
        }

    }

}
