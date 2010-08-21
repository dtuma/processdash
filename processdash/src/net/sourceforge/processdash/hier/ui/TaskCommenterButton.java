// Copyright (C) 2007-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNoteEvent;
import net.sourceforge.processdash.hier.HierarchyNoteListener;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.util.HTMLUtils;

public class TaskCommenterButton extends JButton implements ActionListener,
        PropertyChangeListener, HierarchyNoteListener {

    ProcessDashboard context = null;
    ActiveTaskModel taskModel = null;

    Icon commentPresentIcon = null;
    Icon commentErrorIcon = null;
    Icon noCommentIcon = null;

    // Will be shown only when there is a conflict for a comment in the current
    //  hierarchy.
    String conflictPresentTooltip = null;

    // A tooltip informing the user that it's possible to edit comments
    //  by clicking on the TaskCommenterButton
    String editTooltip = null;

    // A tooltip informing the user that there is no comment for any of the task
    //  of the current hierarchy
    String noCommentTooltip = null;

    // We show the deepest comment of the hierarchy as a tooltip.
    String deepestComment = null;

    // Indicates if there is a comment conflict for either the current task or any
    //  of its ancestors.
    boolean commentConflictPresent = false;

    // The node for which there is a comment conflict
    PropertyKey taskInConflict = null;

    // The other person that has made a comment for the task that has a conflicting
    //  comment
    String conflictingCommentAuthor = null;

    Resources resources = null;

    public TaskCommenterButton(ProcessDashboard dash, ActiveTaskModel activeTaskModel) {
        super();
        HierarchyNoteManager.addHierarchyNoteListener(this);
        context = dash;
        taskModel = activeTaskModel;

        PCSH.enableHelpKey(this, "NoteIndicator");

        resources = Resources.getDashBundle("Notes.TaskCommenter");
        editTooltip = resources.getHTML("Edit_Tooltip");
        noCommentTooltip = resources.getHTML("No_Comments_Tooltip");

        commentPresentIcon = DashboardIconFactory.getCommentIcon();
        commentErrorIcon = DashboardIconFactory.getCommentErrorIcon();
        noCommentIcon = DashboardIconFactory.getNoCommentIcon();

        this.setBorder(BorderFactory.createEmptyBorder());
        addActionListener(this);
        taskModel.addPropertyChangeListener(this);
        new ToolTipTimingCustomizer().install(this);
        updateAppearance();
    }

    private void updateAppearance() {
        StringBuffer toolTipText = null;

        deepestComment = null;
        commentConflictPresent = false;
        taskInConflict = null;
        conflictingCommentAuthor = null;

        setCommentTooltipAndConflict(taskModel.getNode());

        if (deepestComment == null) {
            toolTipText = new StringBuffer("<html><div>" + noCommentTooltip);
            setIcon(noCommentIcon);
        }
        else {
            toolTipText = new StringBuffer("<html><div width='300'>");

            if (commentConflictPresent) {
                toolTipText.append(
                        resources.format("Conflict_Tooltip_FMT",
                                         conflictingCommentAuthor,
                                         taskInConflict.path()));
                setIcon(commentErrorIcon);
            }
            else {
                toolTipText.append(deepestComment + "<hr>" + editTooltip);
                setIcon(commentPresentIcon);
            }
        }

        toolTipText.append("</div></html>");
        setToolTipText(toolTipText.toString());
    }

    // Navigate through the task hierarchy from the currentNode to the top. If any
    //  conflicting comments are found, "taskInConflict" and "conflictingCommentAuthor"
    //  will be set accordingly.
    private void setCommentTooltipAndConflict(PropertyKey currentNode) {
        Map<String, HierarchyNote> notes = HierarchyNoteManager.getNotesForPath(
                context.getDataRepository(), currentNode.path());

        if (notes != null) {
            if (deepestComment == null) {
                HierarchyNote note = notes.get(HierarchyNoteManager.NOTE_KEY);
                if (note != null)
                    deepestComment = note.getAsHTML() + getBylineHTML(note);
            }

            if (notes.get(HierarchyNoteManager.NOTE_CONFLICT_KEY) != null) {
                commentConflictPresent = true;
                taskInConflict = currentNode;
                conflictingCommentAuthor =
                    notes.get(HierarchyNoteManager.NOTE_CONFLICT_KEY).getAuthor();
            }
        }

        PropertyKey parent = currentNode.getParent();

        if (parent != null)
            setCommentTooltipAndConflict(parent);
    }

    private String getBylineHTML(HierarchyNote note) {
        Date when = note.getTimestamp();
        String who = note.getAuthor();
        if (when == null || who == null
                || !Settings.getBool(ENABLE_BYLINE_SETTING, false))
            return "";

        String bylineText = resources.format("ByLine_FMT",when, who);
        return "<hr><div " + BYLINE_CSS + ">"
                + HTMLUtils.escapeEntities(bylineText)
                + "</div>";
    }

    public void actionPerformed(ActionEvent e) {
        PropertyKey phaseToShow = (commentConflictPresent) ? taskInConflict
                                    : context.getCurrentPhase();
        HierarchyNoteEditorDialog.showGlobalNoteEditor(context, phaseToShow);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        updateAppearance();
    }

    public void notesChanged(HierarchyNoteEvent e) {
        if (Filter.pathMatches(taskModel.getPath(), e.getPath()))
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    updateAppearance();
                }});
    }


    private static final String ENABLE_BYLINE_SETTING = "comment.showByLine";

    private static final String BYLINE_CSS =
        "style='text-align:right; color:#808080; font-style:italic'";

}
