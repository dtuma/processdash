// Copyright (C) 2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline;

public class GetAzureAccessToken extends Task {

    private Commandline cmdLine;

    private String propertyName;

    private boolean useQuotes;

    private File storage;


    public GetAzureAccessToken() {
        this.cmdLine = new Commandline();
    }

    public void setProperty(String property) {
        this.propertyName = property;
    }

    public void setQuotes(boolean useQuotes) {
        this.useQuotes = useQuotes;
    }

    public void setFile(File storage) {
        this.storage = storage;
    }

    public void setCommand(String line) {
        createArg().setLine(line);
    }

    public Commandline.Argument createArg() {
        return cmdLine.createArgument();
    }



    @Override
    public void execute() throws BuildException {
        // validate parameters
        if (propertyName == null)
            throw new BuildException("No 'property' attr specified");
        if (storage == null)
            throw new BuildException("No 'file' attr specified");

        // if the property has already been set, do nothing
        if (getProject().getProperty(propertyName) != null)
            return;

        // read data from the file
        String accessToken = readAccessTokenFromFile();

        // if there was no access token, refresh the file and try again
        if (accessToken == null) {
            getNewToken();
            accessToken = readAccessTokenFromFile();
        }

        // store the access token in project properties
        if (accessToken != null) {
            if (useQuotes)
                accessToken = "\"" + accessToken + "\"";
            getProject().setProperty(propertyName, accessToken);
        }
    }



    private String readAccessTokenFromFile() {
        BufferedReader in = null;
        String token = null;
        try {
            // if the file is missing or empty, return null
            if (!storage.isFile() || storage.length() == 0)
                return null;

            // read data from the file
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(storage), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                String[] keyVal = readKeyVal(line);
                if (ACCESS_TOKEN.equals(keyVal[0])) {
                    token = keyVal[1];
                } else if (EXPIRES_ON.equals(keyVal[0])) {
                    Date expires = DATE_FMT.parse(keyVal[1]);
                    if (expires.getTime() < System.currentTimeMillis())
                        return null;
                }
            }

            return token;

        } catch (Exception ioe) {
            return null;
        } finally {
            safelyClose(in);
        }
    }

    private String[] readKeyVal(String line) throws IOException {
        int pos = line.indexOf(": ");
        if (pos == -1)
            return new String[] { null, null };
        else
            return new String[] { stripQuotes(line.substring(0, pos)),
                    stripQuotes(line.substring(pos + 2)) };
    }

    private String stripQuotes(String s) {
        s = s.trim();
        if (s.endsWith(","))
            s = s.substring(0, s.length() - 1);
        if (s.startsWith("\""))
            s = s.substring(1, s.length() - 1);
        return s;
    }

    private static void safelyClose(Closeable c) {
        try {
            if (c != null)
                c.close();
        } catch (IOException ioe) {
        }
    }



    private void getNewToken() {
        // get the command line for refreshing the token
        String[] cmd = cmdLine.getCommandline();
        if (cmd.length == 0)
            throw new BuildException("The command line must be specified");

        // create unique property names for storing the result and error output
        String unique = Long.toString(System.currentTimeMillis());
        String resultProp = "azureResult#" + unique;
        String errorProp = "azureError#" + unique;

        // run a process to execute the given command
        log("Getting azure access token");
        ExecTask exec = new ExecTask(this);
        exec.setExecutable(cmd[0]);
        for (int i = 1; i < cmd.length; i++)
            exec.createArg().setValue(cmd[i]);
        exec.setFailonerror(false);
        exec.setResultProperty(resultProp);
        exec.setErrorProperty(errorProp);
        exec.setOutput(storage);
        exec.execute();

        // if the program completed successfully, return normally
        String result = getProject().getProperty(resultProp);
        if ("0".equals(result))
            return;

        // throw a build exception with the error text we received
        String error = getProject().getProperty(errorProp);
        if (error == null || error.trim().length() == 0)
            error = "Could not get azure access token";
        throw new BuildException(error);
    }



    private static final String ACCESS_TOKEN = "accessToken";

    private static final String EXPIRES_ON = "expiresOn";

    private static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSSSSS");

}
