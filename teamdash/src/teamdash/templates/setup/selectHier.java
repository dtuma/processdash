

package teamdash.templates.setup;
import java.io.IOException;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class selectHier extends TinyCGIBase {

    protected void writeContents() throws IOException {
        PropertyKey key = getStartingKey();
        if (key == null) { error(); return; }

        String prefix = key.path();
        if ("/".equals(prefix)) prefix = "";

        String prompt = getParameter("prompt");
        if (prompt == null) prompt = "Select a WBS item";
        prompt = HTMLUtils.escapeEntities(prompt);

        out.print("<html><head><title>");
        out.print(prompt);
        out.print("</title>\n" +
                  "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
                  "<style>\n" +
                  "  A:link    { color:black; text-decoration:none }\n" +
                  "  A:visited { color:black; text-decoration:none }\n" +
                  "  A:hover   { color:blue; text-decoration:underline }\n" +
                  "</style><body>\n" +
                  "<b>");
        out.print(prompt);
        out.print("</b><table>\n");

        String rootPrefix = prefix;

        initialize(getPSPProperties(), key, getRootId(), 0, rootPrefix);
        printTree(getPSPProperties(), key, getRootId(), 0, rootPrefix);

        out.println("</table>");
        String script = getScript();
        prefix = StringUtils.findAndReplace(prefix, "\\", "\\\\");
        prefix = StringUtils.findAndReplace(prefix, "'", "\\'");
        script = StringUtils.findAndReplace(script, "PREFIX", prefix);
        out.print(script);
        out.println("</body></html>");
    }

    private String getRootId() {
        return "wbs";
    }

    protected void initialize(DashHierarchy properties, PropertyKey key,
            String rootId, int i, String rootPrefix) {
        // do nothing (subclasses may override)
    }

    protected void printTree(DashHierarchy hierarchy, PropertyKey key,
                             String id, int depth, String rootPath) {
        if (prune(hierarchy, key, id, depth, rootPath)) return;

        out.print("<tr id='");
        out.print(id);
        out.print("'><td align=left nowrap>");
        for (int i = 0;   i < depth;   i++)
            out.print(SPACER);
        out.print(NODE_IMG);

        String relPath = (depth == 0 ? "" :
                          key.path().substring(rootPath.length()+1));
        String name = key.name();
        name = HTMLUtils.escapeEntities(name);

        printLink(rootPath, relPath);
        out.print(name);
        out.println("</a></td></tr>");

        id = id + "-";
        int numChildren = hierarchy.getNumChildren(key);
        for (int i = 0;   i < numChildren;   i++)
            printTree(hierarchy, hierarchy.getChildKey(key, i),
                      id + i, depth+1, rootPath);
    }
    private static final String NODE_IMG =
        "<img width=16 height=13 src='/Images/node.png'>";
    private static final String SPACER =
        "<img width=16 height=13 src='/help/Images/spacer.gif'>";


    protected void printLink(String rootPath, String relPath) {
        relPath = HTMLUtils.escapeEntities(relPath);
        out.print("<a href='javascript:doClick(\"" + relPath + "\");'>");
    }


    protected PropertyKey getStartingKey() {
        return getPSPProperties().findExistingKey(getPrefix());
    }


    protected String getID(DashHierarchy hierarchy, PropertyKey key) {
        return hierarchy.getID(key);
    }

    protected void error() throws IOException {
        out.println("<HTML><BODY>");
        out.println("No project exists with the name");
        out.println(HTMLUtils.escapeEntities(getPrefix()));
        out.println("</BODY></HTML>");
    }

    protected boolean prune(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {
        return prune(hierarchy, key);
    }

    protected boolean prune(DashHierarchy hierarchy, PropertyKey key) {
        return false;
    }

    protected String getScript() {
        String call = getParameter("call");
        if (call == null)
            return DEFAULT_SCRIPT_START + REROOT_PARENT_SCRIPT +
                DEFAULT_SCRIPT_END;
        else
            return DEFAULT_SCRIPT_START +
                StringUtils.findAndReplace(CUSTOM_SCRIPT_END, "call", call);
    }

    protected static final String DEFAULT_SCRIPT_START =
        "<script>\n" +
        "  function doClick(relPath) {\n" +
        "    if (relPath) {\n"+
        "      takeAction('PREFIX', relPath, 'PREFIX/' + relPath);\n"+
        "    } else {\n" +
        "      takeAction('PREFIX', '', 'PREFIX');\n"+
        "    }\n" +
        "    self.close();\n"+
        "  }\n";
    protected static final String REROOT_PARENT_SCRIPT =
        "  function rerootParent(fullPath) {\n"+
        "    var url = self.opener.location.pathname;\n"+
        "    var pos = url.lastIndexOf('//');\n"+
        "    if (pos != -1) {\n"+
        "      url = url.substr(pos);\n"+
        "      url = escape(fullPath) + url;\n"+
        "      self.opener.location.pathname = url;\n"+
        "    }\n"+
        "  }\n";
    protected static final String DEFAULT_SCRIPT_END =
        "  function takeAction(root, relPath, fullPath) {\n"+
        "    rerootParent(fullPath);\n" +
        "  }\n" +
        "</script>\n";
    protected static final String CUSTOM_SCRIPT_END =
        "  function takeAction(root, relPath, fullPath) {\n" +
        "      self.opener.call(root, relPath, fullPath);\n" +
        "  }\n" +
        "</script>\n";

}
