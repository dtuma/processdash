
import pspdash.*;
//import pspdash.data.DataRepository;

import java.io.IOException;

public class selectHier extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String prefix = getPrefix();
        if (prefix == null || prefix.length() == 0) { error(); return; }

        PSPProperties hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(prefix);
        while (key != null) {
            String id = getID(hierarchy, key);
            if (id != null && id.endsWith("Root")) break;
            key = key.getParent();
        }
        if (key == null) { error(); return; }

        prefix = key.path();

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

        printTree(hierarchy, key, 0, prefix);

        out.println("</table>");
        String script = SCRIPT;
        if (getParameter("call") != null)
            script = StringUtils.findAndReplace
                (script, "saveHier", getParameter("call"));
        prefix = StringUtils.findAndReplace(prefix, "\\", "\\\\");
        prefix = StringUtils.findAndReplace(prefix, "'", "\\'");
        script = StringUtils.findAndReplace(script, "PREFIX", prefix);
        out.print(script);
        out.println("</body></html>");
    }

    private String getID(PSPProperties hierarchy, PropertyKey key) {
        return hierarchy.getID(key);
    }

    private void error() throws IOException {
        out.println("<HTML><BODY>");
        out.println("This script must be used from within a team project.");
        out.println("</BODY></HTML>");
    }

    protected void printTree(PSPProperties hierarchy, PropertyKey key,
                             int depth, String rootPath) {
        String id = getID(hierarchy, key);
        if (id == null || id.length() == 0) return;
        if (!id.endsWith("Root") && !id.endsWith("Node")) return;

        out.print("<tr><td align=left nowrap>");
        for (int i = 0;   i < depth;   i++)
            out.print(SPACER);
        out.print(NODE_IMG);

        String relPath = (depth == 0 ? "" :
                          key.path().substring(rootPath.length()+1));
        String name = key.name();

        relPath = HTMLUtils.escapeEntities(relPath);
        name = HTMLUtils.escapeEntities(name);

        out.print("<a href='javascript:doClick(\"" + relPath + "\");'>");
        out.print(name);
        out.println("</a></td></tr>");

        int numChildren = hierarchy.getNumChildren(key);
        for (int i = 0;   i < numChildren;   i++)
            printTree(hierarchy, hierarchy.getChildKey(key, i),
                      depth+1, rootPath);
    }
    private static final String NODE_IMG =
        "<img width=16 height=13 src='/Images/node.png'>";
    private static final String SPACER =
        "<img width=16 height=13 src='/help/Images/spacer.gif'>";
    private static final String SCRIPT =
        "<script>\n" +
        "  function doClick(relPath) {\n" +
        "    if (relPath) {\n"+
        "      rerootParent('PREFIX/' + relPath);\n"+
//      "      self.opener.saveHier(relPath, 'PREFIX/' + relPath);\n"+
        "    } else {\n" +
        "      rerootParent('PREFIX');\n"+
//      "      self.opener.saveHier('', 'PREFIX');\n"+
        "    }\n" +
        "    self.close();\n"+
        "  }\n" +
        "  function rerootParent(fullPath) {\n"+
        "    var url = self.opener.location.pathname;\n"+
        "    var pos = url.lastIndexOf('//');\n"+
        "    if (pos != -1) {\n"+
        "      url = url.substr(pos);\n"+
        "      url = escape(fullPath) + url;\n"+
        "      self.opener.location.pathname = url;\n"+
        "    }\n"+
        "  }\n"+
        "</script>\n";



}
