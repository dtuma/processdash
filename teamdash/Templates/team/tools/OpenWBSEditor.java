import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import pspdash.DashController;
import pspdash.TinyCGIBase;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSEditor;

public class OpenWBSEditor extends TinyCGIBase {

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        String directory = getParameter("directory");
        WBSEditor editor = getEditor(directory);

        if (parameters.containsKey("team"))
            editor.showTeamListEditor();
        else
            editor.show();

        DashController.printNullDocument(out);
    }

    private static Hashtable editors = new Hashtable();

    private WBSEditor getEditor(String directory) {
        WBSEditor result = (WBSEditor) editors.get(directory);
        if (result == null) {
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
}
