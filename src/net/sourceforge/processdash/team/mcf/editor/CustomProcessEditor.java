// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.mcf.editor;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.team.mcf.ClasspathContentProvider;
import net.sourceforge.processdash.team.mcf.CustomProcess;
import net.sourceforge.processdash.team.mcf.CustomProcessPublisher;

public class CustomProcessEditor extends AbstractCustomProcessEditor {

    public static void main(String[] args) {
        new CustomProcessEditor(null, new ClasspathContentProvider());
    }

    ContentSource contentSource;

    public CustomProcessEditor(String prefix, ContentSource contentSource) {
        super(prefix);
        this.contentSource = contentSource;
        frame.setVisible(true);
    }

    protected void publishProcess(CustomProcess process, File destFile)
            throws IOException {
        CustomProcessPublisher.publish(process, destFile, contentSource, null,
            true);
    }

}
