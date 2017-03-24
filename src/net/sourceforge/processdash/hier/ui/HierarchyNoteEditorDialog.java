// Copyright (C) 2008-2017 Tuma Solutions, LLC
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.ApplicationEventSource;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.DashHierarchy.Event;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNoteEvent;
import net.sourceforge.processdash.hier.HierarchyNoteListener;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.icons.HierarchyNoteIcon;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

public class HierarchyNoteEditorDialog implements DashHierarchy.Listener,
        TreeSelectionListener, ApplicationEventListener, ChangeListener,
        HierarchyNoteListener {

    protected JFrame frame;
    protected JTree tree;
    protected CommentTreeModel treeModel;
    protected JTable table;
    protected JSplitPane splitPane;
    protected JLabel taskPathLabel;
    protected JScrollPane noteEditorScrollPane;
    protected JButton saveButton;
    protected JButton revertButton;

    private boolean changingSelectionProgrammatically = false;

    // If the note currently being showed is in conflict, this member
    //  will contain it's conflicting portion
    private HierarchyNote noteInConflict = null;

    // The node for which the editor is showing comments.
    private PropertyKey nodeCommentShowedByEditor;

    private HierarchyNoteEditor noteEditor;

    private DataRepository data;
    private DashHierarchy useProps;

    // Members related to the dimensions
    private int frameWidth, frameHeight, dividerLocation = -1;

    private static final String DIMENSION_SETTING_NAME = "TaskCommentEditor.dimensions";
    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 300;
    private static final int DEFAULT_SEPARATOR_LOCATION = 300;
    private static final int NOTE_EDITOR_BORDER_TICKNESS = 1;

    // The text to be shown in the taskPathLabel when no task is selected.
    private static final String NO_TASK_SELECTED = " ";

    private Resources resources = null;

    public HierarchyNoteEditorDialog(DashboardContext ctx,
            PropertyKey currentPhase) {
        resources = Resources.getDashBundle("Notes.Editor");

        this.useProps = ctx.getHierarchy();
        this.data = ctx.getData();
        this.useProps.addHierarchyListener(this);

        constructUserInterface();
        setSelectedNode(currentPhase);
        HierarchyNoteManager.addHierarchyNoteListener(this);

        if (ctx instanceof ApplicationEventSource) {
            ApplicationEventSource aes = (ApplicationEventSource) ctx;
            aes.addApplicationEventListener(this);
        }

        frame.setVisible(true);
    }

    private void constructUserInterface() {
        loadCustomDimensions();

        frame = new JFrame(resources.getString("Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose(true);
            }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        PCSH.enableHelpKey(frame, "NoteIndicator");

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                constructTreePanel(), constructEditPanel());
        splitPane.setDividerLocation(dividerLocation);
        setupWindowKeyBindings(splitPane);

        Container panel = frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.CENTER, splitPane);
    }

    private JScrollPane constructTreePanel() {
        // Create the JTreeModel
        treeModel = new CommentTreeModel(useProps);

        // Create the tree
        tree = new JTree(treeModel);
        tree.addTreeSelectionListener(this);
        tree.setCellRenderer(new CommentTreeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setEditable(false);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // Put the tree in a scroller.
        JScrollPane scrollPane = new JScrollPane(tree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    private JPanel constructEditPanel() {
        JPanel retPanel = new JPanel(new BorderLayout());

        // To add some padding
        retPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));

        taskPathLabel = new JLabel();
        taskPathLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        taskPathLabel.setMinimumSize(new Dimension(0, taskPathLabel
                .getPreferredSize().height));
        retPanel.add(BorderLayout.NORTH, taskPathLabel);

        noteEditorScrollPane = new JScrollPane();
        retPanel.add(BorderLayout.CENTER, noteEditorScrollPane);
        noteEditorScrollPane.setBorder(
                BorderFactory.createLineBorder(Color.BLACK, NOTE_EDITOR_BORDER_TICKNESS));

        JPanel buttonsPanel = new JPanel(false);
        revertButton = createButton(buttonsPanel, "Revert", "revertComment", true);
        saveButton = createButton(buttonsPanel, "Save", "saveComment", true);
        createButton(buttonsPanel, "Close", "close");

        retPanel.add(BorderLayout.SOUTH, buttonsPanel);

        return retPanel;
    }

    private void setupWindowKeyBindings(JComponent c) {
        InputMap inputMap = c.getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
            InputEvent.CTRL_DOWN_MASK), "saveAndClose");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
            InputEvent.META_DOWN_MASK), "saveAndClose");
        c.getActionMap().put("saveAndClose", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isDirty())
                    saveComment();
                close();
            }});

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "confirmClose");
        c.getActionMap().put("confirmClose", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmClose(true);
            }});
    }

    /* Before calling this event, a node must absolutely selected */
    public void revertComment() {
        showEditorFor(nodeCommentShowedByEditor);
        updateButtons();
    }

    /* Before calling this event, a node must absolutely selected */
    public void saveComment() {
        String noteContent = noteEditor.getContent();

        HierarchyNote note = new HierarchyNote();
        note.setContent(noteContent, noteEditor.getFormatID());

        Map<String, HierarchyNote> noteData = new HashMap<String, HierarchyNote>();
        noteData.put(HierarchyNoteManager.NOTE_KEY, note);

        if (noteInConflict != null) {
            noteData.put(HierarchyNoteManager.NOTE_BASE_KEY, noteInConflict);
            noteData.put(HierarchyNoteManager.NOTE_CONFLICT_KEY, null);
        }

        HierarchyNoteManager.saveNotesForPath(data,
                nodeCommentShowedByEditor.path(), noteData);
        treeModel.commentStatusChanged(nodeCommentShowedByEditor);
        noteEditor.setDirty(false);
        noteInConflict = null;
        revertComment();
    }

    public void close() {
        confirmClose(true);
    }

    private void confirmClose(boolean showCancel) {
        if (saveRevertOrCancel(showCancel)) {
            saveCustomDimensions();
            frame.setVisible(false);
        }
    }

    /* Before calling this method, a node must absolutely selected */
    private boolean saveRevertOrCancel(boolean showCancel) {
        if (isDirty() && Settings.isReadWrite()) {

            int optionType = showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                        : JOptionPane.YES_NO_OPTION;

            int userChoice = JOptionPane.showConfirmDialog(frame,
                    resources.format("Confirm_Close_Prompt_FMT", nodeCommentShowedByEditor.path()),
                    resources.getString("Confirm_Close_Title"), optionType);

            switch (userChoice) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false; // do nothing and abort.

            case JOptionPane.YES_OPTION:
                // save changes.
                saveComment();
                break;

            case JOptionPane.NO_OPTION:
                revertComment(); // revert changes.
            }
        }
        return true;
    }

    /* Checks to see if the current comment is dirty */
    private boolean isDirty() {
        return (noteEditor != null && noteEditor.isDirty());
    }

    /**
     * Expand the hierarchy so that the given node is visible and selected.
     */
    private void setSelectedNode(PropertyKey path) {
        if (path == null)
            return;
        TreeNode node = (TreeNode)treeModel.getNodeForKey(path);
        if (node == null)
            return;

        TreePath tp = new TreePath(treeModel.getPathToRoot(node));

        changingSelectionProgrammatically = true;
        tree.clearSelection();
        changingSelectionProgrammatically = false;

        tree.scrollPathToVisible(tp);
        tree.addSelectionPath(tp);
    }

    public void showDialogForNode(PropertyKey path) {
        if ((frame.getExtendedState() & JFrame.ICONIFIED) > 0)
            frame.setState(JFrame.NORMAL);
        frame.setVisible(true);
        frame.toFront();

        if (path != null && !path.equals(nodeCommentShowedByEditor)
                && saveRevertOrCancel(true))
            setSelectedNode(path);
    }

    public void hierarchyChanged(Event e) { }

    public void valueChanged(TreeSelectionEvent e) {

     // Fix that allow the selected node to stay selected when the hierarchy
        //  gets modified. As a side effect, a users can't deselect a node.
        if (!changingSelectionProgrammatically && e.getNewLeadSelectionPath() == null
                && nodeCommentShowedByEditor != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setSelectedNode(nodeCommentShowedByEditor);
                }});
            return;
        }

        if (saveRevertOrCancel(false)) {
            PropertyKey selectedNode = getSelectedNode();
            showEditorFor(selectedNode);
            updateButtons();
        }
    }


    public void stateChanged(ChangeEvent e) {
        updateButtons();
    }


    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            if (frame.isVisible())
                saveRevertOrCancel(false);
        }
    }

    public void notesChanged(HierarchyNoteEvent e) {
        final String path = e.getPath();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                treeModel.commentStatusChanged(path);
                if (path.equals(nodeCommentShowedByEditor.path()) && !isDirty())
                    revertComment();
            }});
    }

    private void updateButtons() {
        boolean isDirty = isDirty();
        saveButton.setEnabled(isDirty);
        revertButton.setEnabled(isDirty);
        MacGUIUtils.setDirty(frame, isDirty);
    }

    private PropertyKey getSelectedNode() {
        TreePath selection = tree.getSelectionPath();
        PropertyKey selectedNode = null;
        if (selection != null)
            selectedNode = treeModel.getKeyForNode(selection.getLastPathComponent());

        return selectedNode;
    }

    private void showEditorFor(PropertyKey selectedNode) {
        // Deregistering the dialog from dirty events by the editor we don't want to show
        //  anymore.
        if (this.noteEditor != null)
            this.noteEditor.removeDirtyListener(this);

        noteEditorScrollPane.setViewportView(null);

        // No node selected
        if (selectedNode == null)
            showEmptyEditor();
        else {
            String path = selectedNode.path();
            taskPathLabel.setText(path);

            HierarchyNoteFormat hierarchyNoteFormat = null;
            HierarchyNoteEditor noteEditor = null;

            Map<String, HierarchyNote> notes = HierarchyNoteManager.getNotesForPath(
                    data, path);

            // The selected node as some notes
            if (notes != null) {
                // We get the note format
                String noteFormat = notes.get(HierarchyNoteManager.NOTE_KEY).getFormat();

                hierarchyNoteFormat = HierarchyNoteManager.getNoteFormat(noteFormat);

                noteInConflict = notes.get(HierarchyNoteManager.NOTE_CONFLICT_KEY);

                // We get the editor of the desired note, for its format
                noteEditor = hierarchyNoteFormat.getEditor(
                        notes.get(HierarchyNoteManager.NOTE_KEY),
                        noteInConflict,
                        notes.get(HierarchyNoteManager.NOTE_BASE_KEY));
            }
            else {
                hierarchyNoteFormat =
                    HierarchyNoteManager.getDefaultNoteFormat(data, path);

                noteInConflict = null;

                // Since there's no note for this node, we can get an empty Editor for
                //  the specified note format
                noteEditor = hierarchyNoteFormat.getEditor(null, null, null);
            }

            final Component noteEditorComponent = noteEditor
                    .getNoteEditorComponent();

            noteEditorScrollPane.setViewportView(noteEditorComponent);
            this.noteEditor = noteEditor;
            this.nodeCommentShowedByEditor = selectedNode;
            this.noteEditor.addDirtyListener(this);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    noteEditorComponent.requestFocus();
                }});
        }
    }

    private void showEmptyEditor() {
        taskPathLabel.setText(NO_TASK_SELECTED);

        JTextArea noTaskSelectedInfo = new JTextArea(resources.getString("No_Task_Selected"));
        noTaskSelectedInfo.setEditable(false);
        noteEditorScrollPane.setViewportView(noTaskSelectedInfo);
        this.noteEditor = null;
        this.nodeCommentShowedByEditor = null;
    }

    private void loadCustomDimensions() {
        String setting = Settings.getVal(DIMENSION_SETTING_NAME);
        if (setting != null && setting.length() > 0)
            try {
                StringTokenizer tok = new StringTokenizer(setting, ",");
                frameWidth = Integer.parseInt(tok.nextToken());
                frameHeight = Integer.parseInt(tok.nextToken());
                dividerLocation = Integer.parseInt(tok.nextToken());
            } catch (Exception e) {
            }
        if (dividerLocation == -1) {
            frameWidth = DEFAULT_WIDTH;
            frameHeight = DEFAULT_HEIGHT;
            dividerLocation = DEFAULT_SEPARATOR_LOCATION;
        }
    }

    private void saveCustomDimensions() {
        frameWidth = frame.getSize().width;
        frameHeight = frame.getSize().height;
        dividerLocation = splitPane.getDividerLocation();
        InternalSettings.set(DIMENSION_SETTING_NAME, frameWidth + ","
                + frameHeight + "," + dividerLocation);
    }

    private ActionListener createActionListener(String methodName) {
        return (ActionListener) EventHandler.create(ActionListener.class, this,
                methodName);
    }

    private JButton createButton(Container p, String resKey, String action) {
        return createButton(p, resKey, action, false);
    }
    private JButton createButton(Container p, String resKey, String action,
            boolean hideIfReadOnly) {
        JButton result = new JButton(resources.getString(resKey));
        result.addActionListener(createActionListener(action));
        if (hideIfReadOnly == false || (Settings.isReadWrite()))
            p.add(result);
        return result;
    }

    private class CommentTreeModel extends HierarchyTreeModel {
        public static final int NO_COMMENT = 0;
        public static final int HAS_COMMENT = 1;
        public static final int HAS_CONFLICT = 2;

        private Map<PropertyKey, Integer> statusData = new HashMap<PropertyKey, Integer>();

        public CommentTreeModel(DashHierarchy hierarchy) {
            super(hierarchy);
        }

        public int getCommentStatus(Object node) {
            PropertyKey key = key(node);
            Integer result = statusData.get(key);
            if (result == null) {
                result = calcCommentStatus(key.path());
                statusData.put(key, result);
            }
            return result;
        }

        private Integer calcCommentStatus(String path) {
            Map noteData = HierarchyNoteManager.getNotesForPath(data, path);
            if (noteData == null || noteData.isEmpty())
                return NO_COMMENT;
            else if (noteData.containsKey(HierarchyNoteManager.NOTE_CONFLICT_KEY))
                return HAS_CONFLICT;
            else
                return HAS_COMMENT;
        }

        @Override
        public void reload(TreeNode node) {
            statusData.clear();
            super.reload(node);
        }

        public PropertyKey getKeyForNode(Object htn) {
            return super.key(htn);
        }

        public void commentStatusChanged(PropertyKey key) {
            statusData.remove(key);
            nodeChanged(getNodeForKey(key));
        }

        public void commentStatusChanged(String path) {
            PropertyKey key = tree.findExistingKey(path);
            if (key != null)
                commentStatusChanged(key);
        }

    }

    private class CommentTreeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            Component result = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                        row, hasFocus);

            CommentTreeModel model = (CommentTreeModel) tree.getModel();
            switch (model.getCommentStatus(value)) {
            case CommentTreeModel.NO_COMMENT:
                setIcon(HierarchyNoteIcon.WHITE);
                break;

            case CommentTreeModel.HAS_COMMENT:
                setIcon(HierarchyNoteIcon.YELLOW);
                break;


            case CommentTreeModel.HAS_CONFLICT:
                setIcon(HierarchyNoteIcon.RED);
                break;
            }

            return result;
        }

    }

    private static HierarchyNoteEditorDialog GLOBAL_INSTANCE = null;

    public static void showGlobalNoteEditor(DashboardContext ctx,
            PropertyKey phaseToShow) {
        if (GLOBAL_INSTANCE == null) {
            GLOBAL_INSTANCE = new HierarchyNoteEditorDialog(ctx, phaseToShow);
        } else {
            GLOBAL_INSTANCE.showDialogForNode(phaseToShow);
        }
    }

}
