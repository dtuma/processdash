package teamdash.templates.tools;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSEditor;

public class OpenWBSEditor extends TinyCGIBase {


    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        parseFormData();
        String directory = getParameter("directory");
        if (!checkEntryCriteria(directory))
            return;

        WBSEditor editor = getEditor(directory);

        if (parameters.containsKey("team"))
            editor.showTeamListEditor();
        else
            editor.show();

        DashController.printNullDocument(out);
    }

    private boolean checkEntryCriteria(String directory) {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.3")) {
            out.print(JAVA_VERSION_MSG1 + javaVersion + JAVA_VERSION_MSG2);
            return false;
        }

        File dir = new File(directory);
        if (!dir.isDirectory()) {
            out.print(DIR_MISSING_MSG1 + HTMLUtils.escapeEntities(directory) +
                      DIR_MISSING_MSG2);
            return false;
        }

        return true;
    }

    private static Hashtable editors = new Hashtable();

    private WBSEditor getEditor(String directory) {
        WBSEditor result = (WBSEditor) editors.get(directory);
        if (result == null || result.isDisposed()) {
            result = makeEditorForPath(directory);
            editors.put(directory, result);
        }

        return result;
    }

    private WBSEditor makeEditorForPath(String directory) {
        File dir = new File(directory);
        File dumpFile = new File(dir, "projDump.xml");
        WBSEditor result = new WBSEditor
            (new TeamProject(dir, "Team Project"), dumpFile);
        result.setExitOnClose(false);

        return result;
    }


    private static final String JAVA_VERSION_MSG1 =
        "<html><body><h1>Incorrect Java Version</h1>" +
        "Sorry, but the team planning tools require version 1.4 or higher " +
        "of the Java Runtime Environment (JRE).  You are currently running the " +
        "dashboard with version ";
    private static final String JAVA_VERSION_MSG2 =
        " of the JRE.  To use the team planning tools, please upgrade the " +
        "Java Runtime Environment on your computer, restart the dashboard, " +
        "and try again.</body></html>";


    private static final String DIR_MISSING_MSG1 =
        "<html><body><h1>Team Data Directory Missing</h1>" +
        "The team planning tools need access to the team data directory for " +
        "this project.  According to your current project settings, that " +
        "directory is <pre>";
    private static final String DIR_MISSING_MSG2 =
        "</pre>  Unfortunately, this directory does not appear to exist.  " +
        "Check to make certain that the directory is accessible and try " +
        "again.</body></html>";

}
