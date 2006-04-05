package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/** This ant task determines the revision number of an SVN working copy,
 * and saves that number into a property.
 * 
 * Assumptions: the working copy isn't a mixed revision tree.
 * Finds the revision number of every file underneath a given directory.
 * The highest revision number is used as the result.  If any files are
 * locally modified/added/deleted/etc, an asterisk is appended to the end
 * of the result.
 */
public class SvnRevInfo extends Task {

    private File startDir;

    private String destProperty;

    public void setDir(File dir) {
        startDir = dir.getAbsoluteFile();
    }

    public void setProperty(String name) {
        destProperty = name;
    }

    private void validate() throws BuildException {
        if (startDir == null)
            startDir = getProject().getBaseDir();
        if (destProperty == null)
            throw new BuildException("property attribute must be specified.");
    }

    public void execute() throws BuildException {
        validate();

        String result = "unknown";
        try {
            String[] cmd = new String[] { "svn", "status", "-v" };
            Process p = Runtime.getRuntime().exec(cmd, null, startDir);
            result = parseStatus(p.getInputStream());
        } catch (Exception e) {
        }
        getProject().setProperty(destProperty, result);
    }

    private String parseStatus(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                inputStream));

        int revNum = 0;
        boolean localMods = false;
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = SVN_LINE_PATTERN.matcher(line);
            if (!m.find())
                continue;

            localMods |= (MOD_FLAGS.indexOf(m.group(1).charAt(0)) != -1);

            int thisRev = Integer.parseInt(m.group(2));
            revNum = Math.max(revNum, thisRev);
        }
        return revNum + (localMods ? "*" : "");
    }

    private static final String MOD_FLAGS = "ADMRC!~";

    private static final Pattern SVN_LINE_PATTERN = Pattern
            .compile("^(.).....\\s*(\\d+)");
}
