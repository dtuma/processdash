// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.io.FileInputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

final class ImportTemplatePermissionDialog {

    static boolean currentlyDisplayingDialog = false;

    static boolean askUserForPermission(JFrame parent,
                                        String templateJarFilename,
                                        String templateDir,
                                        boolean create)
    {
        if (parent == null) return false;
        if (!XMLUtils.hasValue(templateJarFilename) &&
            !XMLUtils.hasValue(templateDir))
            return false;

        synchronized (ImportTemplatePermissionDialog.class) {
            if (currentlyDisplayingDialog) return false;
            currentlyDisplayingDialog = true;
        }

        String templateName = getTemplateName(templateJarFilename);

        StringBuffer text = new StringBuffer();
        text.append
            ("<html><body color='black'>"+
             "<p>The dashboard has received a request to import a new</p>"+
             "<p>add-on process set.  Add-on process sets can contain</p>"+
             "<p>process scripts, forms, and templates, as well as</p>"+
             "<p>program extensions that add functionality to the</p>"+
             "<p>Process Dashboard.</p>" +
             "<p>&nbsp;</p>" +
             "<p>Would you like to import ");

        if (templateJarFilename != null) {
            text.append("the process set<pre>        ");
            if (templateName != null) {
                text.append("&quot;")
                    .append(HTMLUtils.escapeEntities(templateName))
                    .append("&quot; (");
            }
            text.append(HTMLUtils.escapeEntities(templateJarFilename));
            if (templateName != null) text.append(")");
            text.append("</pre>");
        }

        if (templateDir != null) {
            if (templateJarFilename != null)
                text.append("along with other ");
            text.append("process assets in the<pre>        ")
                .append(HTMLUtils.escapeEntities(templateDir))
                .append("</pre>directory?");
        }

        text.append
            (" (These templates will be registered in</p>"+
             "<p>your dashboard configuration file, and will be</p>"+
             "<p>imported in future sessions as well.)</p>" +
             "<p>&nbsp;</p>" +
             "<p>You should answer &quot;Yes&quot; only if you just initiated an</p>" +
             "<p>action to ")
            .append(create ? "create" : "join")
            .append(" a team project. If you have not made</p>"+
                    "<p>such a request, you should click &quot;No&quot;.</p>");

        int answer = JOptionPane.showConfirmDialog
            (parent, text.toString(), "Import Template",
             JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        synchronized (ImportTemplatePermissionDialog.class) {
            currentlyDisplayingDialog = false;
        }

        return (answer == JOptionPane.YES_OPTION);
    }



    private static String getTemplateName(String jarfileName) {
        try {
            JarInputStream jarFile =
                new JarInputStream(new FileInputStream(jarfileName));
            Attributes attrs = jarFile.getManifest().getMainAttributes();
            return attrs.getValue(DashPackage.NAME_ATTRIBUTE);

        } catch (Exception ioe) {
            return null;
        }
    }
}
