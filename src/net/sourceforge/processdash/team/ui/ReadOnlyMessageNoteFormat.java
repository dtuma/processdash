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

package net.sourceforge.processdash.team.ui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTextPane;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.ui.HierarchyNoteEditor;
import net.sourceforge.processdash.hier.ui.HierarchyNoteFormat;

public class ReadOnlyMessageNoteFormat implements HierarchyNoteFormat {

    private String formatID;

    public void setConfigElement(Element xml, String attrName) {
        this.formatID = xml.getAttribute("formatID");
    }

    public String getAsHTML(HierarchyNote note) {
        return null;
    }

    public HierarchyNoteEditor getEditor(HierarchyNote note,
            HierarchyNote conflict, HierarchyNote base) {
        return new Editor();
    }

    public void replaceHyperlink(HierarchyNote note, String oldUrl,
            String newUrl, String newLinkText) {}

    public String getID() {
        return formatID;
    }

    public class Editor extends JTextPane implements HierarchyNoteEditor {

        Editor() {
            setText("To view or edit task notes for this project, "
                    + "please open the Work Breakdown Structure Editor.");
            setFont(getFont().deriveFont(Font.ITALIC));
            setEditable(false);
        }

        public String getContent() {
            return null;
        }

        public String getFormatID() {
            return formatID;
        }

        public Component getNoteEditorComponent() {
            return this;
        }

        public void addDirtyListener(ChangeListener l) {}

        public void removeDirtyListener(ChangeListener l) {}

        public void setDirty(boolean dirty) {}

        public boolean isDirty() {
            return false;
        }

    }
}
