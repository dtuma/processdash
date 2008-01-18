package teamdash.templates.setup;

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
